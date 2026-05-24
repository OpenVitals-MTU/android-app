package tech.mmarca.openvitals.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
import tech.mmarca.openvitals.core.period.PeriodRangePreferenceKey
import tech.mmarca.openvitals.core.period.TimeRange
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
import tech.mmarca.openvitals.features.activity.ActivityMetric
import tech.mmarca.openvitals.features.activity.ActivityScreen
import tech.mmarca.openvitals.features.activity.ActivityViewModel
import tech.mmarca.openvitals.features.activity.ActivitiesScreen
import tech.mmarca.openvitals.features.activity.ActivitiesViewModel
import tech.mmarca.openvitals.features.body.BodyMetric
import tech.mmarca.openvitals.features.body.BodyScreen
import tech.mmarca.openvitals.features.body.BodyViewModel
import tech.mmarca.openvitals.features.browse.BrowseScreen
import tech.mmarca.openvitals.features.browse.BrowseViewModel
import tech.mmarca.openvitals.features.cycle.CycleScreen
import tech.mmarca.openvitals.features.cycle.CycleViewModel
import tech.mmarca.openvitals.features.dashboard.DashboardScreen
import tech.mmarca.openvitals.features.dashboard.DashboardViewModel
import tech.mmarca.openvitals.features.dashboard.DashboardWidgetId
import tech.mmarca.openvitals.features.heart.HeartMetric
import tech.mmarca.openvitals.features.heart.HeartScreen
import tech.mmarca.openvitals.features.heart.HeartViewModel
import tech.mmarca.openvitals.features.hydration.HydrationScreen
import tech.mmarca.openvitals.features.hydration.HydrationViewModel
import tech.mmarca.openvitals.features.mindfulness.MindfulnessScreen
import tech.mmarca.openvitals.features.mindfulness.MindfulnessViewModel
import tech.mmarca.openvitals.features.nutrition.NutritionMetric
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
    val currentMetricId = if (currentRoute == Screen.Metric.route) {
        navBackStackEntry?.arguments?.getString(METRIC_ID_ARG)?.toDashboardWidgetIdOrNull()
    } else {
        null
    }
    var dashboardTopBarState by remember { mutableStateOf(DashboardTopBarState()) }

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
        Screen.Metric.route -> currentMetricId?.let { stringResource(metricTitleRes(it)) }.orEmpty()
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
                        if (currentRoute == Screen.Dashboard.route) {
                            IconButton(onClick = dashboardTopBarState.onToggleEdit) {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = stringResource(
                                        if (dashboardTopBarState.isEditing) {
                                            R.string.cd_finish_dashboard_editing
                                        } else {
                                            R.string.cd_edit_dashboard
                                        }
                                    ),
                                    tint = if (dashboardTopBarState.isEditing) {
                                        androidx.compose.material3.MaterialTheme.colorScheme.primary
                                    } else {
                                        androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                            }
                        }
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
                    onOpenMetric = { metricId -> navController.navigate(Screen.Metric.createRoute(metricId.name)) },
                    onOpenBrowse = { navController.navigate(Screen.Browse.route) },
                    onEditStateChanged = { isEditing, onToggleEdit ->
                        dashboardTopBarState = DashboardTopBarState(isEditing, onToggleEdit)
                    },
                )
            }

            composable(
                route = Screen.Metric.route,
                arguments = listOf(navArgument(METRIC_ID_ARG) { type = NavType.StringType }),
            ) { backStackEntry ->
                val metricId = backStackEntry.arguments
                    ?.getString(METRIC_ID_ARG)
                    ?.toDashboardWidgetIdOrNull()
                MetricRouteContent(
                    metricId = metricId,
                    activityRepository = activityRepository,
                    sleepRepository = sleepRepository,
                    heartRepository = heartRepository,
                    bodyRepository = bodyRepository,
                    hydrationRepository = hydrationRepository,
                    nutritionRepository = nutritionRepository,
                    mindfulnessRepository = mindfulnessRepository,
                    vitalsRepository = vitalsRepository,
                    cycleRepository = cycleRepository,
                    preferencesRepository = preferencesRepository,
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

            composable(Screen.Steps.route) {
                val activityViewModel = appViewModel {
                    ActivityViewModel(
                        repository = activityRepository,
                        initialRange = preferencesRepository.timeRangeFor(PeriodRangePreferenceKey.STEPS),
                        onRangeSelected = preferencesRepository.rangeSaver(PeriodRangePreferenceKey.STEPS),
                    )
                }
                ActivityScreen(
                    viewModel = activityViewModel,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                )
            }

            composable(Screen.Activity.route) {
                val activitiesViewModel = appViewModel {
                    ActivitiesViewModel(
                        repository = activityRepository,
                        initialRange = preferencesRepository.timeRangeFor(PeriodRangePreferenceKey.ACTIVITIES),
                        onRangeSelected = preferencesRepository.rangeSaver(PeriodRangePreferenceKey.ACTIVITIES),
                    )
                }
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
                val sleepViewModel = appViewModel {
                    SleepViewModel(
                        repository = sleepRepository,
                        initialRange = preferencesRepository.timeRangeFor(PeriodRangePreferenceKey.SLEEP),
                        initialSleepRangeMode = preferencesRepository.sleepRangeMode,
                        sleepRangeModeFlow = preferencesRepository.sleepRangeModeFlow,
                        onRangeSelected = preferencesRepository.rangeSaver(PeriodRangePreferenceKey.SLEEP),
                    )
                }
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
                    HeartViewModel(
                        repository = heartRepository,
                        vitalsRepository = vitalsRepository,
                        initialRange = preferencesRepository.timeRangeFor(PeriodRangePreferenceKey.HEART),
                        onRangeSelected = preferencesRepository.rangeSaver(PeriodRangePreferenceKey.HEART),
                    )
                }
                HeartScreen(
                    viewModel = heartViewModel,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                )
            }

            composable(Screen.Body.route) {
                val bodyViewModel = appViewModel {
                    BodyViewModel(
                        repository = bodyRepository,
                        initialRange = preferencesRepository.timeRangeFor(PeriodRangePreferenceKey.BODY),
                        onRangeSelected = preferencesRepository.rangeSaver(PeriodRangePreferenceKey.BODY),
                    )
                }
                BodyScreen(
                    viewModel = bodyViewModel,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                )
            }

            composable(Screen.Hydration.route) {
                val hydrationViewModel = appViewModel {
                    HydrationViewModel(
                        repository = hydrationRepository,
                        initialRange = preferencesRepository.timeRangeFor(PeriodRangePreferenceKey.HYDRATION),
                        initialDailyGoalLiters = preferencesRepository.hydrationDailyGoalLiters,
                        onRangeSelected = preferencesRepository.rangeSaver(PeriodRangePreferenceKey.HYDRATION),
                        onDailyGoalChanged = { goal -> preferencesRepository.hydrationDailyGoalLiters = goal },
                    )
                }
                HydrationScreen(
                    viewModel = hydrationViewModel,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                )
            }

            composable(Screen.Nutrition.route) {
                val nutritionViewModel = appViewModel {
                    NutritionViewModel(
                        repository = nutritionRepository,
                        initialRange = preferencesRepository.timeRangeFor(PeriodRangePreferenceKey.NUTRITION),
                        onRangeSelected = preferencesRepository.rangeSaver(PeriodRangePreferenceKey.NUTRITION),
                    )
                }
                NutritionScreen(
                    viewModel = nutritionViewModel,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                )
            }

            composable(Screen.Mindfulness.route) {
                val mindfulnessViewModel = appViewModel {
                    MindfulnessViewModel(
                        repository = mindfulnessRepository,
                        initialRange = preferencesRepository.timeRangeFor(PeriodRangePreferenceKey.MINDFULNESS),
                        onRangeSelected = preferencesRepository.rangeSaver(PeriodRangePreferenceKey.MINDFULNESS),
                    )
                }
                MindfulnessScreen(
                    viewModel = mindfulnessViewModel,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                )
            }

            composable(Screen.Cycle.route) {
                val cycleViewModel = appViewModel {
                    CycleViewModel(
                        repository = cycleRepository,
                        initialRange = preferencesRepository.timeRangeFor(PeriodRangePreferenceKey.CYCLE),
                        onRangeSelected = preferencesRepository.rangeSaver(PeriodRangePreferenceKey.CYCLE),
                    )
                }
                CycleScreen(
                    viewModel = cycleViewModel,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                )
            }

            composable(Screen.Browse.route) {
                val browseViewModel = appViewModel {
                    BrowseViewModel(
                        activityRepository = activityRepository,
                        sleepRepository = sleepRepository,
                        bodyRepository = bodyRepository,
                        initialRange = preferencesRepository.timeRangeFor(PeriodRangePreferenceKey.BROWSE),
                        onRangeSelected = preferencesRepository.rangeSaver(PeriodRangePreferenceKey.BROWSE),
                    )
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
private fun MetricRouteContent(
    metricId: DashboardWidgetId?,
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
    onOpenActivity: (String) -> Unit,
    onOpenSleepSession: (String) -> Unit,
) {
    metricId?.toActivityMetricOrNull()?.let { activityMetric ->
        val activityViewModel = appViewModel {
            ActivityViewModel(
                repository = activityRepository,
                initialRange = preferencesRepository.timeRangeFor(PeriodRangePreferenceKey.STEPS),
                onRangeSelected = preferencesRepository.rangeSaver(PeriodRangePreferenceKey.STEPS),
            )
        }
        ActivityScreen(
            viewModel = activityViewModel,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            metric = activityMetric,
        )
        return
    }

    metricId?.toHeartMetricOrNull()?.let { heartMetric ->
        val heartViewModel = appViewModel {
            HeartViewModel(
                repository = heartRepository,
                vitalsRepository = vitalsRepository,
                initialRange = preferencesRepository.timeRangeFor(PeriodRangePreferenceKey.HEART),
                onRangeSelected = preferencesRepository.rangeSaver(PeriodRangePreferenceKey.HEART),
            )
        }
        HeartScreen(
            viewModel = heartViewModel,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            metric = heartMetric,
        )
        return
    }

    metricId?.toBodyMetricOrNull()?.let { bodyMetric ->
        val bodyViewModel = appViewModel {
            BodyViewModel(
                repository = bodyRepository,
                initialRange = preferencesRepository.timeRangeFor(PeriodRangePreferenceKey.BODY),
                onRangeSelected = preferencesRepository.rangeSaver(PeriodRangePreferenceKey.BODY),
            )
        }
        BodyScreen(
            viewModel = bodyViewModel,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            metric = bodyMetric,
        )
        return
    }

    metricId?.toNutritionMetricOrNull()?.let { nutritionMetric ->
        val nutritionViewModel = appViewModel {
            NutritionViewModel(
                repository = nutritionRepository,
                initialRange = preferencesRepository.timeRangeFor(PeriodRangePreferenceKey.NUTRITION),
                onRangeSelected = preferencesRepository.rangeSaver(PeriodRangePreferenceKey.NUTRITION),
            )
        }
        NutritionScreen(
            viewModel = nutritionViewModel,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            metric = nutritionMetric,
        )
        return
    }

    when (metricId) {
        DashboardWidgetId.WORKOUT -> {
            val activitiesViewModel = appViewModel {
                ActivitiesViewModel(
                    repository = activityRepository,
                    initialRange = preferencesRepository.timeRangeFor(PeriodRangePreferenceKey.ACTIVITIES),
                    onRangeSelected = preferencesRepository.rangeSaver(PeriodRangePreferenceKey.ACTIVITIES),
                )
            }
            ActivitiesScreen(
                viewModel = activitiesViewModel,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                onOpenActivity = onOpenActivity,
            )
        }
        DashboardWidgetId.SLEEP -> {
            val sleepViewModel = appViewModel {
                SleepViewModel(
                    repository = sleepRepository,
                    initialRange = preferencesRepository.timeRangeFor(PeriodRangePreferenceKey.SLEEP),
                    initialSleepRangeMode = preferencesRepository.sleepRangeMode,
                    sleepRangeModeFlow = preferencesRepository.sleepRangeModeFlow,
                    onRangeSelected = preferencesRepository.rangeSaver(PeriodRangePreferenceKey.SLEEP),
                )
            }
            SleepScreen(
                viewModel = sleepViewModel,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                onOpenSleepSession = onOpenSleepSession,
            )
        }
        DashboardWidgetId.HYDRATION -> {
            val hydrationViewModel = appViewModel {
                HydrationViewModel(
                    repository = hydrationRepository,
                    initialRange = preferencesRepository.timeRangeFor(PeriodRangePreferenceKey.HYDRATION),
                    initialDailyGoalLiters = preferencesRepository.hydrationDailyGoalLiters,
                    onRangeSelected = preferencesRepository.rangeSaver(PeriodRangePreferenceKey.HYDRATION),
                    onDailyGoalChanged = { goal -> preferencesRepository.hydrationDailyGoalLiters = goal },
                )
            }
            HydrationScreen(
                viewModel = hydrationViewModel,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
            )
        }
        DashboardWidgetId.MINDFULNESS -> {
            val mindfulnessViewModel = appViewModel {
                MindfulnessViewModel(
                    repository = mindfulnessRepository,
                    initialRange = preferencesRepository.timeRangeFor(PeriodRangePreferenceKey.MINDFULNESS),
                    onRangeSelected = preferencesRepository.rangeSaver(PeriodRangePreferenceKey.MINDFULNESS),
                )
            }
            MindfulnessScreen(
                viewModel = mindfulnessViewModel,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
            )
        }
        DashboardWidgetId.CYCLE -> {
            val cycleViewModel = appViewModel {
                CycleViewModel(
                    repository = cycleRepository,
                    initialRange = preferencesRepository.timeRangeFor(PeriodRangePreferenceKey.CYCLE),
                    onRangeSelected = preferencesRepository.rangeSaver(PeriodRangePreferenceKey.CYCLE),
                )
            }
            CycleScreen(
                viewModel = cycleViewModel,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
            )
        }
        else -> {
            Text(
                text = stringResource(R.string.unknown_error),
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

private fun PreferencesRepository.rangeSaver(key: PeriodRangePreferenceKey): (TimeRange) -> Unit =
    { range -> setTimeRangeFor(key, range) }

private fun String.toDashboardWidgetIdOrNull(): DashboardWidgetId? =
    runCatching { DashboardWidgetId.valueOf(this) }.getOrNull()

private fun DashboardWidgetId.toActivityMetricOrNull(): ActivityMetric? =
    when (this) {
        DashboardWidgetId.STEPS -> ActivityMetric.STEPS
        DashboardWidgetId.DISTANCE -> ActivityMetric.DISTANCE
        DashboardWidgetId.CALORIES_OUT -> ActivityMetric.CALORIES_BURNED
        DashboardWidgetId.ACTIVE_CALORIES -> ActivityMetric.ACTIVE_CALORIES
        DashboardWidgetId.FLOORS -> ActivityMetric.FLOORS
        DashboardWidgetId.ELEVATION -> ActivityMetric.ELEVATION
        else -> null
    }

private fun DashboardWidgetId.toHeartMetricOrNull(): HeartMetric? =
    when (this) {
        DashboardWidgetId.AVG_HEART_RATE -> HeartMetric.AVERAGE_HEART_RATE
        DashboardWidgetId.RESTING_HEART_RATE -> HeartMetric.RESTING_HEART_RATE
        DashboardWidgetId.HRV -> HeartMetric.HRV
        DashboardWidgetId.BLOOD_PRESSURE -> HeartMetric.BLOOD_PRESSURE
        DashboardWidgetId.SPO2 -> HeartMetric.SPO2
        DashboardWidgetId.VO2_MAX -> HeartMetric.VO2_MAX
        DashboardWidgetId.RESPIRATORY_RATE -> HeartMetric.RESPIRATORY_RATE
        DashboardWidgetId.BODY_TEMPERATURE -> HeartMetric.BODY_TEMPERATURE
        else -> null
    }

private fun DashboardWidgetId.toBodyMetricOrNull(): BodyMetric? =
    when (this) {
        DashboardWidgetId.WEIGHT -> BodyMetric.WEIGHT
        DashboardWidgetId.HEIGHT -> BodyMetric.HEIGHT
        DashboardWidgetId.BMI -> BodyMetric.BMI
        DashboardWidgetId.BODY_FAT -> BodyMetric.BODY_FAT
        DashboardWidgetId.LEAN_MASS -> BodyMetric.LEAN_MASS
        DashboardWidgetId.BMR -> BodyMetric.BMR
        DashboardWidgetId.BONE_MASS -> BodyMetric.BONE_MASS
        else -> null
    }

private fun DashboardWidgetId.toNutritionMetricOrNull(): NutritionMetric? =
    when (this) {
        DashboardWidgetId.CALORIES_IN -> NutritionMetric.CALORIES_IN
        DashboardWidgetId.PROTEIN -> NutritionMetric.PROTEIN
        DashboardWidgetId.CARBS -> NutritionMetric.CARBS
        DashboardWidgetId.FAT -> NutritionMetric.FAT
        else -> null
    }

private fun metricTitleRes(metricId: DashboardWidgetId): Int =
    when (metricId) {
        DashboardWidgetId.STEPS -> R.string.metric_steps
        DashboardWidgetId.DISTANCE -> R.string.metric_distance
        DashboardWidgetId.CALORIES_OUT -> R.string.metric_calories_out
        DashboardWidgetId.ACTIVE_CALORIES -> R.string.metric_active_calories
        DashboardWidgetId.FLOORS -> R.string.metric_floors_climbed
        DashboardWidgetId.ELEVATION -> R.string.metric_elevation
        DashboardWidgetId.WORKOUT -> R.string.metric_workout
        DashboardWidgetId.SLEEP -> R.string.metric_sleep
        DashboardWidgetId.HYDRATION -> R.string.metric_hydration
        DashboardWidgetId.CALORIES_IN -> R.string.metric_calories_in
        DashboardWidgetId.PROTEIN -> R.string.metric_protein
        DashboardWidgetId.CARBS -> R.string.metric_carbs
        DashboardWidgetId.FAT -> R.string.metric_fat
        DashboardWidgetId.WEIGHT -> R.string.metric_weight
        DashboardWidgetId.HEIGHT -> R.string.metric_height
        DashboardWidgetId.BMI -> R.string.metric_bmi
        DashboardWidgetId.BODY_FAT -> R.string.metric_body_fat
        DashboardWidgetId.LEAN_MASS -> R.string.metric_lean_mass
        DashboardWidgetId.BMR -> R.string.metric_bmr
        DashboardWidgetId.BONE_MASS -> R.string.metric_bone_mass
        DashboardWidgetId.AVG_HEART_RATE -> R.string.metric_avg_heart_rate
        DashboardWidgetId.RESTING_HEART_RATE -> R.string.metric_resting_heart_rate
        DashboardWidgetId.HRV -> R.string.metric_hrv
        DashboardWidgetId.BLOOD_PRESSURE -> R.string.metric_blood_pressure
        DashboardWidgetId.SPO2 -> R.string.metric_spo2
        DashboardWidgetId.VO2_MAX -> R.string.metric_vo2_max
        DashboardWidgetId.RESPIRATORY_RATE -> R.string.metric_respiratory_rate
        DashboardWidgetId.BODY_TEMPERATURE -> R.string.metric_body_temp
        DashboardWidgetId.MINDFULNESS -> R.string.metric_mindfulness
        DashboardWidgetId.CYCLE -> R.string.metric_cycle
        DashboardWidgetId.BROWSE -> R.string.metric_browse
    }

private data class DashboardTopBarState(
    val isEditing: Boolean = false,
    val onToggleEdit: () -> Unit = {},
)

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
