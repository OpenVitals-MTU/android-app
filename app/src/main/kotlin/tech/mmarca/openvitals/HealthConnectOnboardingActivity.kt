package tech.mmarca.openvitals

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.domain.preferences.AppThemeMode
import tech.mmarca.openvitals.features.onboarding.OnboardingScreen
import tech.mmarca.openvitals.features.onboarding.OnboardingViewModel
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

@AndroidEntryPoint
class HealthConnectOnboardingActivity : AppCompatActivity() {

    @Inject lateinit var preferencesRepository: PreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
                val viewModel = hiltViewModel<OnboardingViewModel>()
                OnboardingScreen(
                    viewModel = viewModel,
                    onOnboardingComplete = {
                        startActivity(
                            Intent(this, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            },
                        )
                        finish()
                    },
                )
            }
        }
    }
}

private fun AppThemeMode.isDarkTheme(systemInDarkTheme: Boolean): Boolean =
    when (this) {
        AppThemeMode.SYSTEM -> systemInDarkTheme
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK,
        AppThemeMode.AMOLED -> true
    }
