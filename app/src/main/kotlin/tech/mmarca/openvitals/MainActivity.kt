package tech.mmarca.openvitals

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.data.model.HealthConnectAvailability
import tech.mmarca.openvitals.navigation.AppNavigation
import tech.mmarca.openvitals.navigation.Screen
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as OpenVitalsApp

        setContent {
            OpenVitalsTheme {
                var startDestination by remember {
                    mutableStateOf(
                        if (
                            app.preferencesRepository.onboardingDone &&
                            app.healthRepository.availability() == HealthConnectAvailability.AVAILABLE
                        ) {
                            Screen.Dashboard.route
                        } else {
                            Screen.Onboarding.route
                        }
                    )
                }
                val unitSystem by app.preferencesRepository.unitSystemFlow.collectAsStateWithLifecycle()
                val appLanguage by app.preferencesRepository.appLanguageFlow.collectAsStateWithLifecycle()
                val unitFormatter = remember(unitSystem) {
                    UnitFormatter(unitSystemProvider = { unitSystem })
                }

                LaunchedEffect(appLanguage) {
                    AppCompatDelegate.setApplicationLocales(appLanguage.toLocaleListCompat())
                }

                AppNavigation(
                    repository = app.healthRepository,
                    activityRepository = app.activityRepository,
                    sleepRepository = app.sleepRepository,
                    heartRepository = app.heartRepository,
                    bodyRepository = app.bodyRepository,
                    hydrationRepository = app.hydrationRepository,
                    nutritionRepository = app.nutritionRepository,
                    mindfulnessRepository = app.mindfulnessRepository,
                    vitalsRepository = app.vitalsRepository,
                    cycleRepository = app.cycleRepository,
                    preferencesRepository = app.preferencesRepository,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = app.dateTimeFormatterProvider,
                    startDestination = startDestination,
                    onOnboardingComplete = {
                        app.preferencesRepository.onboardingDone = true
                        startDestination = Screen.Dashboard.route
                    },
                )
            }
        }
    }
}
