package tech.mmarca.openvitals.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import tech.mmarca.openvitals.data.repository.ActivityRepository
import tech.mmarca.openvitals.data.repository.BodyRepository
import tech.mmarca.openvitals.data.repository.HeartRepository
import tech.mmarca.openvitals.data.repository.HealthRepository
import tech.mmarca.openvitals.data.repository.HydrationRepository
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.data.repository.SleepRepository
import tech.mmarca.openvitals.features.activity.ActivityScreen
import tech.mmarca.openvitals.features.activity.ActivityViewModel
import tech.mmarca.openvitals.features.activity.ActivitiesScreen
import tech.mmarca.openvitals.features.activity.ActivitiesViewModel
import tech.mmarca.openvitals.features.body.BodyScreen
import tech.mmarca.openvitals.features.body.BodyViewModel
import tech.mmarca.openvitals.features.browse.BrowseScreen
import tech.mmarca.openvitals.features.browse.BrowseViewModel
import tech.mmarca.openvitals.features.dashboard.DashboardScreen
import tech.mmarca.openvitals.features.dashboard.DashboardViewModel
import tech.mmarca.openvitals.features.heart.HeartScreen
import tech.mmarca.openvitals.features.heart.HeartViewModel
import tech.mmarca.openvitals.features.hydration.HydrationScreen
import tech.mmarca.openvitals.features.hydration.HydrationViewModel
import tech.mmarca.openvitals.features.onboarding.OnboardingScreen
import tech.mmarca.openvitals.features.onboarding.OnboardingViewModel
import tech.mmarca.openvitals.features.settings.SettingsScreen
import tech.mmarca.openvitals.features.settings.SettingsViewModel
import tech.mmarca.openvitals.features.sleep.SleepScreen
import tech.mmarca.openvitals.features.sleep.SleepViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    repository: HealthRepository,
    activityRepository: ActivityRepository,
    sleepRepository: SleepRepository,
    heartRepository: HeartRepository,
    bodyRepository: BodyRepository,
    hydrationRepository: HydrationRepository,
    preferencesRepository: PreferencesRepository,
    startDestination: String,
    onOnboardingComplete: () -> Unit = {},
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    val showTopBar = currentRoute != Screen.Onboarding.route
    val canNavigateBack =
        currentRoute != null &&
            currentRoute != Screen.Onboarding.route &&
            currentRoute != Screen.Dashboard.route &&
            navController.previousBackStackEntry != null

    val topBarTitle = when (currentRoute) {
        Screen.Dashboard.route -> "Dashboard"
        Screen.Steps.route -> "Steps"
        Screen.Activity.route -> "Activities"
        Screen.Sleep.route -> "Sleep"
        Screen.Heart.route -> "Heart"
        Screen.Body.route -> "Body"
        Screen.Hydration.route -> "Hydration"
        Screen.Browse.route -> "Browse"
        Screen.Settings.route -> "Settings"
        else -> ""
    }

    Scaffold(
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = { Text(topBarTitle) },
                    navigationIcon = {
                        if (canNavigateBack) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                    contentDescription = "Back",
                                )
                            }
                        }
                    },
                    actions = {
                        if (currentRoute != Screen.Settings.route) {
                            IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                                Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                            }
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Screen.Onboarding.route) {
                val onboardingViewModel = remember(repository) { OnboardingViewModel(repository) }
                OnboardingScreen(
                    viewModel = onboardingViewModel,
                    onOnboardingComplete = {
                        onOnboardingComplete()
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    },
                )
            }

            composable(Screen.Dashboard.route) {
                val dashboardViewModel = remember(repository) { DashboardViewModel(repository, preferencesRepository) }
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    onGrantPermissions = { navController.navigate(Screen.Settings.route) },
                    onOpenSteps = { navController.navigate(Screen.Steps.route) },
                    onOpenActivities = { navController.navigate(Screen.Activity.route) },
                    onOpenSleep = { navController.navigate(Screen.Sleep.route) },
                    onOpenHeart = { navController.navigate(Screen.Heart.route) },
                    onOpenBody = { navController.navigate(Screen.Body.route) },
                    onOpenHydration = { navController.navigate(Screen.Hydration.route) },
                    onOpenBrowse = { navController.navigate(Screen.Browse.route) },
                )
            }

            composable(Screen.Steps.route) {
                val activityViewModel = remember(activityRepository) { ActivityViewModel(activityRepository) }
                ActivityScreen(viewModel = activityViewModel)
            }

            composable(Screen.Activity.route) {
                val activitiesViewModel = remember(activityRepository) { ActivitiesViewModel(activityRepository) }
                ActivitiesScreen(viewModel = activitiesViewModel)
            }

            composable(Screen.Sleep.route) {
                val sleepViewModel = remember(sleepRepository) { SleepViewModel(sleepRepository) }
                SleepScreen(viewModel = sleepViewModel)
            }

            composable(Screen.Heart.route) {
                val heartViewModel = remember(heartRepository) { HeartViewModel(heartRepository) }
                HeartScreen(viewModel = heartViewModel)
            }

            composable(Screen.Body.route) {
                val bodyViewModel = remember(bodyRepository) { BodyViewModel(bodyRepository) }
                BodyScreen(viewModel = bodyViewModel)
            }

            composable(Screen.Hydration.route) {
                val hydrationViewModel = remember(hydrationRepository) { HydrationViewModel(hydrationRepository) }
                HydrationScreen(viewModel = hydrationViewModel)
            }

            composable(Screen.Browse.route) {
                val browseViewModel = remember(activityRepository, sleepRepository, bodyRepository) {
                    BrowseViewModel(activityRepository, sleepRepository, bodyRepository)
                }
                BrowseScreen(viewModel = browseViewModel)
            }

            composable(Screen.Settings.route) {
                val settingsViewModel = remember(repository) { SettingsViewModel(repository) }
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
