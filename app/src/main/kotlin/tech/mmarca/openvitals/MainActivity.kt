package tech.mmarca.openvitals

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
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
import tech.mmarca.openvitals.data.model.HealthConnectAvailability
import tech.mmarca.openvitals.data.repository.HealthRepository
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.navigation.AppNavigation
import tech.mmarca.openvitals.navigation.Screen
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var preferencesRepository: PreferencesRepository
    @Inject lateinit var healthRepository: HealthRepository
    @Inject lateinit var unitFormatter: UnitFormatter
    @Inject lateinit var dateTimeFormatterProvider: DateTimeFormatterProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            OpenVitalsTheme {
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
                    onOnboardingComplete = {
                        preferencesRepository.onboardingDone = true
                        startDestination = Screen.Dashboard.route
                    },
                )
            }
        }
    }
}
