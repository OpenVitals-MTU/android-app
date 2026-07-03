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
import tech.mmarca.openvitals.features.dashboard.DashboardWidgetId
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.preferences.AppThemeMode
import tech.mmarca.openvitals.domain.preferences.isDarkTheme
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.data.repository.contract.HealthRepository
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.ui.components.AppLockGate
import tech.mmarca.openvitals.navigation.AppNavigation
import tech.mmarca.openvitals.navigation.ExternalRouteImportRequest
import tech.mmarca.openvitals.navigation.EXTRA_OPENVITALS_ROUTE
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
    private var externalNavigationRoute by mutableStateOf<String?>(null)
    private var startDestination by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (redirectHealthConnectOnboardingIfNeeded(intent)) {
            return
        }
        enableEdgeToEdge()
        updateRouteImportRequest(intent)
        updateExternalNavigationRoute(intent)

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
                val resolvedStartDestination = startDestination ?: run {
                    if (
                        preferencesRepository.onboardingDone &&
                        healthRepository.availability() == HealthConnectAvailability.AVAILABLE
                    ) {
                        Screen.Dashboard.route
                    } else {
                        Screen.Onboarding.route
                    }
                }.also { destination ->
                    if (startDestination == null) {
                        startDestination = destination
                    }
                }
                val unitSystem by preferencesRepository.unitSystemFlow.collectAsStateWithLifecycle()
                val appLanguage by preferencesRepository.appLanguageFlow.collectAsStateWithLifecycle()

                LaunchedEffect(appLanguage) {
                    AppCompatDelegate.setApplicationLocales(appLanguage.toLocaleListCompat())
                }

                AppLockGate(enabled = preferencesRepository.appLockEnabled) {
                    AppNavigation(
                        unitFormatter = unitFormatter,
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        startDestination = resolvedStartDestination,
                        appThemeMode = appThemeMode,
                        routeImportRequest = routeImportRequest,
                        externalNavigationRoute = externalNavigationRoute,
                        onRouteImportRequestHandled = { requestId ->
                            if (routeImportRequest?.id == requestId) {
                                routeImportRequest = null
                            }
                        },
                        onExternalNavigationHandled = {
                            externalNavigationRoute = null
                        },
                        onOnboardingComplete = {
                            preferencesRepository.onboardingDone = true
                            startDestination = Screen.Dashboard.route
                        },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (redirectHealthConnectOnboardingIfNeeded(intent)) {
            return
        }
        updateRouteImportRequest(intent)
        updateExternalNavigationRoute(intent)
    }

    private fun redirectHealthConnectOnboardingIfNeeded(intent: Intent?): Boolean {
        if (intent?.isHealthConnectOnboardingIntent() != true) return false
        startActivity(
            Intent(this, HealthConnectOnboardingActivity::class.java).apply {
                action = intent.action
                data = intent.data
                putExtras(intent)
            },
        )
        finish()
        return true
    }

    private fun updateRouteImportRequest(intent: Intent?) {
        val uri = intent?.routeImportUri() ?: return
        routeImportRequest = ExternalRouteImportRequest(
            id = ++nextRouteImportRequestId,
            uri = uri,
        )
    }

    private fun updateExternalNavigationRoute(intent: Intent?) {
        externalNavigationRoute = intent?.openVitalsRoute()
    }
}

private fun Intent.isHealthConnectOnboardingIntent(): Boolean {
    val action = action ?: return false
    return action == "androidx.health.ACTION_SHOW_ONBOARDING" ||
        action == "android.health.connect.action.SHOW_ONBOARDING"
}

private fun Intent.openVitalsRoute(): String? =
    getStringExtra(EXTRA_OPENVITALS_ROUTE)?.takeIf(::isSupportedOpenVitalsRoute)

private fun isSupportedOpenVitalsRoute(route: String): Boolean {
    if (route == Screen.Dashboard.route) return true
    if (route == Screen.DailyReadiness.route) return true
    if (route == Screen.ActivityEntry.route) return true
    if (route == Screen.HydrationEntry.route) return true
    val hydrationDrinkLogPrefix = "manual_entry/hydration/log/"
    if (route.startsWith(hydrationDrinkLogPrefix)) {
        return Uri.decode(route.removePrefix(hydrationDrinkLogPrefix)).isNotBlank()
    }
    if (route.startsWith("daily_readiness/body_energy/")) return true
    val metricPrefix = "metric/"
    if (!route.startsWith(metricPrefix)) return false
    val metricId = Uri.decode(route.removePrefix(metricPrefix))
    return runCatching { DashboardWidgetId.valueOf(metricId) }.isSuccess
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
