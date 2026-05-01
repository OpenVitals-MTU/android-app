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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.data.repository.ActivityRepository
import tech.mmarca.openvitals.data.repository.BodyRepository
import tech.mmarca.openvitals.data.repository.CycleRepository
import tech.mmarca.openvitals.data.repository.HeartRepository
import tech.mmarca.openvitals.data.repository.HealthRepository
import tech.mmarca.openvitals.data.repository.HydrationRepository
import tech.mmarca.openvitals.data.repository.MindfulnessRepository
import tech.mmarca.openvitals.data.repository.NutritionRepository
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.data.repository.SleepRepository
import tech.mmarca.openvitals.data.repository.VitalsRepository
import tech.mmarca.openvitals.features.activity.ActivityDetailScreen
import tech.mmarca.openvitals.features.activity.ActivityDetailViewModel
import tech.mmarca.openvitals.features.activity.ActivityScreen
import tech.mmarca.openvitals.features.activity.ActivityViewModel
import tech.mmarca.openvitals.features.activity.ActivitiesScreen
import tech.mmarca.openvitals.features.activity.ActivitiesViewModel
import tech.mmarca.openvitals.features.body.BodyScreen
import tech.mmarca.openvitals.features.body.BodyViewModel
import tech.mmarca.openvitals.features.browse.BrowseScreen
import tech.mmarca.openvitals.features.browse.BrowseViewModel
import tech.mmarca.openvitals.features.cycle.CycleScreen
import tech.mmarca.openvitals.features.cycle.CycleViewModel
import tech.mmarca.openvitals.features.dashboard.DashboardScreen
import tech.mmarca.openvitals.features.dashboard.DashboardViewModel
import tech.mmarca.openvitals.features.heart.HeartScreen
import tech.mmarca.openvitals.features.heart.HeartViewModel
import tech.mmarca.openvitals.features.hydration.HydrationScreen
import tech.mmarca.openvitals.features.hydration.HydrationViewModel
import tech.mmarca.openvitals.features.mindfulness.MindfulnessScreen
import tech.mmarca.openvitals.features.mindfulness.MindfulnessViewModel
import tech.mmarca.openvitals.features.nutrition.NutritionScreen
import tech.mmarca.openvitals.features.nutrition.NutritionViewModel
import tech.mmarca.openvitals.features.onboarding.OnboardingScreen
import tech.mmarca.openvitals.features.onboarding.OnboardingViewModel
import tech.mmarca.openvitals.features.settings.SettingsScreen
import tech.mmarca.openvitals.features.settings.SettingsViewModel
import tech.mmarca.openvitals.features.sleep.SleepDetailScreen
import tech.mmarca.openvitals.features.sleep.SleepDetailViewModel
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
    nutritionRepository: NutritionRepository,
    mindfulnessRepository: MindfulnessRepository,
    vitalsRepository: VitalsRepository,
    cycleRepository: CycleRepository,
    preferencesRepository: PreferencesRepository,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
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
        Screen.Dashboard.route -> stringResource(R.string.screen_dashboard)
        Screen.Steps.route -> stringResource(R.string.screen_steps)
        Screen.Activity.route -> stringResource(R.string.screen_activities)
        Screen.ActivityDetail.route -> stringResource(R.string.screen_activity_detail)
        Screen.Sleep.route -> stringResource(R.string.screen_sleep)
        Screen.SleepDetail.route -> stringResource(R.string.screen_sleep_detail)
        Screen.Heart.route -> stringResource(R.string.screen_heart_vitals)
        Screen.Body.route -> stringResource(R.string.screen_body)
        Screen.Hydration.route -> stringResource(R.string.screen_hydration)
        Screen.Nutrition.route -> stringResource(R.string.screen_nutrition)
        Screen.Mindfulness.route -> stringResource(R.string.screen_mindfulness)
        Screen.Cycle.route -> stringResource(R.string.screen_cycle)
        Screen.Browse.route -> stringResource(R.string.screen_browse)
        Screen.Settings.route -> stringResource(R.string.screen_settings)
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
                                    contentDescription = stringResource(R.string.cd_back),
                                )
                            }
                        }
                    },
                    actions = {
                        if (currentRoute != Screen.Settings.route) {
                            IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                                Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.cd_settings))
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
                val onboardingViewModel = appViewModel {
                    OnboardingViewModel(repository, preferencesRepository)
                }
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
                val dashboardViewModel = appViewModel {
                    DashboardViewModel(repository, preferencesRepository)
                }
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    onGrantPermissions = { navController.navigate(Screen.Settings.route) },
                    onOpenSteps = { navController.navigate(Screen.Steps.route) },
                    onOpenActivities = { navController.navigate(Screen.Activity.route) },
                    onOpenSleep = { navController.navigate(Screen.Sleep.route) },
                    onOpenHeart = { navController.navigate(Screen.Heart.route) },
                    onOpenBody = { navController.navigate(Screen.Body.route) },
                    onOpenHydration = { navController.navigate(Screen.Hydration.route) },
                    onOpenNutrition = { navController.navigate(Screen.Nutrition.route) },
                    onOpenMindfulness = { navController.navigate(Screen.Mindfulness.route) },
                    onOpenCycle = { navController.navigate(Screen.Cycle.route) },
                    onOpenBrowse = { navController.navigate(Screen.Browse.route) },
                )
            }

            composable(Screen.Steps.route) {
                val activityViewModel = appViewModel { ActivityViewModel(activityRepository) }
                ActivityScreen(
                    viewModel = activityViewModel,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                )
            }

            composable(Screen.Activity.route) {
                val activitiesViewModel = appViewModel { ActivitiesViewModel(activityRepository) }
                ActivitiesScreen(
                    viewModel = activitiesViewModel,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    onOpenActivity = { activityId ->
                        navController.navigate(Screen.ActivityDetail.createRoute(activityId))
                    },
                )
            }

            composable(
                route = Screen.ActivityDetail.route,
                arguments = listOf(navArgument(ACTIVITY_DETAIL_ID_ARG) { type = NavType.StringType }),
            ) { backStackEntry ->
                val activityId = backStackEntry.arguments?.getString(ACTIVITY_DETAIL_ID_ARG).orEmpty()
                val activityDetailViewModel = appViewModel {
                    ActivityDetailViewModel(activityRepository, activityId)
                }
                ActivityDetailScreen(
                    viewModel = activityDetailViewModel,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                )
            }

            composable(Screen.Sleep.route) {
                val sleepViewModel = appViewModel { SleepViewModel(sleepRepository) }
                SleepScreen(
                    viewModel = sleepViewModel,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    onOpenSleepSession = { sleepId ->
                        navController.navigate(Screen.SleepDetail.createRoute(sleepId))
                    },
                )
            }

            composable(
                route = Screen.SleepDetail.route,
                arguments = listOf(navArgument(SLEEP_DETAIL_ID_ARG) { type = NavType.StringType }),
            ) { backStackEntry ->
                val sleepId = backStackEntry.arguments?.getString(SLEEP_DETAIL_ID_ARG).orEmpty()
                val sleepDetailViewModel = appViewModel {
                    SleepDetailViewModel(sleepRepository, sleepId)
                }
                SleepDetailScreen(
                    viewModel = sleepDetailViewModel,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                )
            }

            composable(Screen.Heart.route) {
                val heartViewModel = appViewModel {
                    HeartViewModel(heartRepository, vitalsRepository)
                }
                HeartScreen(
                    viewModel = heartViewModel,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                )
            }

            composable(Screen.Body.route) {
                val bodyViewModel = appViewModel { BodyViewModel(bodyRepository) }
                BodyScreen(
                    viewModel = bodyViewModel,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                )
            }

            composable(Screen.Hydration.route) {
                val hydrationViewModel = appViewModel { HydrationViewModel(hydrationRepository) }
                HydrationScreen(
                    viewModel = hydrationViewModel,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                )
            }

            composable(Screen.Nutrition.route) {
                val nutritionViewModel = appViewModel { NutritionViewModel(nutritionRepository) }
                NutritionScreen(
                    viewModel = nutritionViewModel,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                )
            }

            composable(Screen.Mindfulness.route) {
                val mindfulnessViewModel = appViewModel {
                    MindfulnessViewModel(mindfulnessRepository)
                }
                MindfulnessScreen(
                    viewModel = mindfulnessViewModel,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                )
            }

            composable(Screen.Cycle.route) {
                val cycleViewModel = appViewModel {
                    CycleViewModel(cycleRepository)
                }
                CycleScreen(
                    viewModel = cycleViewModel,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                )
            }

            composable(Screen.Browse.route) {
                val browseViewModel = appViewModel {
                    BrowseViewModel(activityRepository, sleepRepository, bodyRepository)
                }
                BrowseScreen(
                    viewModel = browseViewModel,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    onOpenActivity = { activityId ->
                        navController.navigate(Screen.ActivityDetail.createRoute(activityId))
                    },
                    onOpenSleepSession = { sleepId ->
                        navController.navigate(Screen.SleepDetail.createRoute(sleepId))
                    },
                )
            }

            composable(Screen.Settings.route) {
                val settingsViewModel = appViewModel {
                    SettingsViewModel(repository, preferencesRepository)
                }
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

@Composable
private inline fun <reified VM : ViewModel> appViewModel(
    noinline create: () -> VM,
): VM = viewModel(factory = OpenVitalsViewModelFactory(create))

private class OpenVitalsViewModelFactory<VM : ViewModel>(
    private val create: () -> VM,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val viewModel = create()
        require(modelClass.isInstance(viewModel)) {
            "Expected ${modelClass.name}, but created ${viewModel::class.java.name}"
        }
        return viewModel as T
    }
}
