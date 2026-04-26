package tech.mmarca.openvitals

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.navigation.AppNavigation
import tech.mmarca.openvitals.navigation.Screen
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as OpenVitalsApp

        setContent {
            OpenVitalsTheme {
                var startDestination by remember {
                    mutableStateOf(
                        if (app.preferencesRepository.onboardingDone) Screen.Dashboard.route
                        else Screen.Onboarding.route
                    )
                }
                val unitSystem by app.preferencesRepository.unitSystemFlow.collectAsState()
                val unitFormatter = remember(unitSystem) {
                    UnitFormatter(unitSystemProvider = { unitSystem })
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
