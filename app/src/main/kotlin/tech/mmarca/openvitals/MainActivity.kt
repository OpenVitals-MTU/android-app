package tech.mmarca.openvitals

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.preferences.AppThemeMode
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.data.repository.HealthRepository
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.navigation.AppNavigation
import tech.mmarca.openvitals.navigation.ExternalRouteImportRequest
import tech.mmarca.openvitals.navigation.Screen
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme
import java.util.Locale

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var preferencesRepository: PreferencesRepository
    @Inject lateinit var healthRepository: HealthRepository
    @Inject lateinit var unitFormatter: UnitFormatter
    @Inject lateinit var dateTimeFormatterProvider: DateTimeFormatterProvider

    private var nextRouteImportRequestId = 0L
    private var routeImportRequest by mutableStateOf<ExternalRouteImportRequest?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        updateRouteImportRequest(intent)

        setContent {
            val appThemeMode by preferencesRepository.appThemeModeFlow.collectAsStateWithLifecycle()
            val appDarkTheme = appThemeMode.isDarkTheme(isSystemInDarkTheme())

            LaunchedEffect(appDarkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        lightScrim = Color.TRANSPARENT,
                        darkScrim = Color.TRANSPARENT,
                        detectDarkMode = { appDarkTheme },
                    ),
                    navigationBarStyle = SystemBarStyle.auto(
                        lightScrim = Color.TRANSPARENT,
                        darkScrim = Color.TRANSPARENT,
                        detectDarkMode = { appDarkTheme },
                    ),
                )
            }

            OpenVitalsTheme(themeMode = appThemeMode) {
                var startDestination by remember {
                    mutableStateOf(
                        if (
                            preferencesRepository.onboardingDone &&
                            healthRepository.availability() == HealthConnectAvailability.AVAILABLE
                        ) {
                            Screen.Dashboard.route
                        } else {
                            Screen.Onboarding.route
                        }
                    )
                }
                val unitSystem by preferencesRepository.unitSystemFlow.collectAsStateWithLifecycle()
                val appLanguage by preferencesRepository.appLanguageFlow.collectAsStateWithLifecycle()

                LaunchedEffect(appLanguage) {
                    AppCompatDelegate.setApplicationLocales(appLanguage.toLocaleListCompat())
                }

                AppNavigation(
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    startDestination = startDestination,
                    routeImportRequest = routeImportRequest,
                    onRouteImportRequestHandled = { requestId ->
                        if (routeImportRequest?.id == requestId) {
                            routeImportRequest = null
                        }
                    },
                    onOnboardingComplete = {
                        preferencesRepository.onboardingDone = true
                        startDestination = Screen.Dashboard.route
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        updateRouteImportRequest(intent)
    }

    private fun updateRouteImportRequest(intent: Intent?) {
        val uri = intent?.routeImportUri() ?: return
        routeImportRequest = ExternalRouteImportRequest(
            id = ++nextRouteImportRequestId,
            uri = uri,
        )
    }
}

private fun Intent.routeImportUri(): Uri? {
    val uri = when (action) {
        Intent.ACTION_VIEW -> data
        Intent.ACTION_SEND -> streamUri()
        Intent.ACTION_SEND_MULTIPLE -> streamUris().firstOrNull { uri ->
            isSupportedRouteImport(uri, type)
        } ?: streamUris().firstOrNull()
        else -> null
    } ?: return null

    return uri.takeIf { isSupportedRouteImport(it, type) }
}

private fun isSupportedRouteImport(uri: Uri, mimeType: String?): Boolean =
    mimeType?.lowercase(Locale.US)?.let { it in RouteImportMimeTypes } == true ||
        RouteImportExtensions.any { extension ->
            uri.toString().lowercase(Locale.US).contains(".$extension")
        }

private fun Intent.streamUri(): Uri? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(Intent.EXTRA_STREAM)
    }

private fun Intent.streamUris(): List<Uri> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java).orEmpty()
    } else {
        @Suppress("DEPRECATION")
        getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM).orEmpty()
    }

private val RouteImportMimeTypes = setOf(
    "application/gpx",
    "application/gpx+xml",
    "application/vnd.google-earth.kml+xml",
    "application/vnd.google-earth.kmz",
    "application/vnd.google-earth.kmz+xml",
    "application/vnd.ant.fit",
    "application/vnd.garmin.fit",
    "application/fit",
    "application/x-fit",
)

private val RouteImportExtensions = setOf("gpx", "kml", "kmz", "fit")

private fun AppThemeMode.isDarkTheme(systemInDarkTheme: Boolean): Boolean =
    when (this) {
        AppThemeMode.SYSTEM -> systemInDarkTheme
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK,
        AppThemeMode.AMOLED -> true
    }
