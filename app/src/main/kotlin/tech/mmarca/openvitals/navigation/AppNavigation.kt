package tech.mmarca.openvitals.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.features.achievements.AchievementsScreen
import tech.mmarca.openvitals.features.achievements.AchievementsViewModel
import tech.mmarca.openvitals.features.activity.ActivityDetailScreen
import tech.mmarca.openvitals.features.activity.ActivityDetailViewModel
import tech.mmarca.openvitals.features.activity.ActivityOverviewViewModel
import tech.mmarca.openvitals.features.activity.ActiveCaloriesScreen
import tech.mmarca.openvitals.features.activity.ActivityMetric
import tech.mmarca.openvitals.features.activity.ActivityViewModel
import tech.mmarca.openvitals.features.activity.ActivitiesScreen
import tech.mmarca.openvitals.features.activity.ActivitiesViewModel
import tech.mmarca.openvitals.features.activity.CaloriesOutScreen
import tech.mmarca.openvitals.features.activity.CaloriesScreen
import tech.mmarca.openvitals.features.activity.CaloriesViewModel
import tech.mmarca.openvitals.features.activity.CardioLoadDetailScreen
import tech.mmarca.openvitals.features.activity.DistanceScreen
import tech.mmarca.openvitals.features.activity.ElevationScreen
import tech.mmarca.openvitals.features.activity.FloorsScreen
import tech.mmarca.openvitals.features.activity.StepsScreen
import tech.mmarca.openvitals.features.body.BmiScreen
import tech.mmarca.openvitals.features.body.BmrScreen
import tech.mmarca.openvitals.features.body.BodyFatScreen
import tech.mmarca.openvitals.features.body.BodyMetric
import tech.mmarca.openvitals.features.body.BodyViewModel
import tech.mmarca.openvitals.features.body.BoneMassScreen
import tech.mmarca.openvitals.features.body.HeightScreen
import tech.mmarca.openvitals.features.body.LeanMassScreen
import tech.mmarca.openvitals.features.body.WeightScreen
import tech.mmarca.openvitals.features.cycle.CycleScreen
import tech.mmarca.openvitals.features.cycle.CycleViewModel
import tech.mmarca.openvitals.features.dashboard.DashboardScreen
import tech.mmarca.openvitals.features.dashboard.DashboardViewModel
import tech.mmarca.openvitals.features.dashboard.DashboardWidgetId
import tech.mmarca.openvitals.features.heart.AverageHeartRateScreen
import tech.mmarca.openvitals.features.heart.BloodPressureScreen
import tech.mmarca.openvitals.features.heart.BodyTemperatureScreen
import tech.mmarca.openvitals.features.heart.HeartMetric
import tech.mmarca.openvitals.features.heart.HeartViewModel
import tech.mmarca.openvitals.features.heart.HrvScreen
import tech.mmarca.openvitals.features.heart.RespiratoryRateScreen
import tech.mmarca.openvitals.features.heart.RestingHeartRateScreen
import tech.mmarca.openvitals.features.heart.SpO2Screen
import tech.mmarca.openvitals.features.heart.Vo2MaxScreen
import tech.mmarca.openvitals.features.hydration.HydrationScreen
import tech.mmarca.openvitals.features.hydration.HydrationViewModel
import tech.mmarca.openvitals.data.model.BodyMeasurementType
import tech.mmarca.openvitals.data.model.VitalsMeasurementType
import tech.mmarca.openvitals.features.manualentry.ActivityEntryScreen
import tech.mmarca.openvitals.features.manualentry.ActivityEntryViewModel
import tech.mmarca.openvitals.features.manualentry.BodyMeasurementEntryScreen
import tech.mmarca.openvitals.features.manualentry.BodyMeasurementEntryViewModel
import tech.mmarca.openvitals.features.manualentry.HydrationEntryScreen
import tech.mmarca.openvitals.features.manualentry.HydrationEntryViewModel
import tech.mmarca.openvitals.features.manualentry.ManualEntryScreen
import tech.mmarca.openvitals.features.manualentry.ManualEntryViewModel
import tech.mmarca.openvitals.features.manualentry.MindfulnessEntryScreen
import tech.mmarca.openvitals.features.manualentry.MindfulnessEntryViewModel
import tech.mmarca.openvitals.features.manualentry.VitalsMeasurementEntryScreen
import tech.mmarca.openvitals.features.manualentry.VitalsMeasurementEntryViewModel
import tech.mmarca.openvitals.features.manualentry.titleRes
import tech.mmarca.openvitals.features.mindfulness.MindfulnessScreen
import tech.mmarca.openvitals.features.mindfulness.MindfulnessViewModel
import tech.mmarca.openvitals.features.nutrition.CaloriesInScreen
import tech.mmarca.openvitals.features.nutrition.CarbsScreen
import tech.mmarca.openvitals.features.nutrition.FatScreen
import tech.mmarca.openvitals.features.nutrition.NutritionMetric
import tech.mmarca.openvitals.features.nutrition.NutritionViewModel
import tech.mmarca.openvitals.features.nutrition.ProteinScreen
import tech.mmarca.openvitals.features.onboarding.OnboardingScreen
import tech.mmarca.openvitals.features.onboarding.OnboardingViewModel
import tech.mmarca.openvitals.features.recovery.RecoveryViewModel
import tech.mmarca.openvitals.features.recovery.SleepEfficiencyDetailScreen
import tech.mmarca.openvitals.features.recovery.SleepScoreDetailScreen
import tech.mmarca.openvitals.features.settings.SettingsScreen
import tech.mmarca.openvitals.features.settings.SettingsViewModel
import tech.mmarca.openvitals.features.sleep.SleepDetailScreen
import tech.mmarca.openvitals.features.sleep.SleepDetailViewModel
import tech.mmarca.openvitals.features.sleep.SleepScreen
import tech.mmarca.openvitals.features.sleep.SleepViewModel
import tech.mmarca.openvitals.ui.components.MetricAction
import tech.mmarca.openvitals.ui.components.OpenVitalsAdaptiveScaffold
import tech.mmarca.openvitals.ui.components.OpenVitalsNavigationDestination

private const val CardioLoadDetailRoute = "activity/cardio_load"
private const val SleepEfficiencyDetailRoute = "recovery/sleep_efficiency"
private const val SleepScoreDetailRoute = "recovery/sleep_score"

@Composable
fun AppNavigation(
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    startDestination: String,
    routeImportRequest: ExternalRouteImportRequest? = null,
    onRouteImportRequestHandled: (Long) -> Unit = {},
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
    val currentBodyMeasurementType = if (
        currentRoute == Screen.BodyMeasurementEntry.route ||
        currentRoute == Screen.BodyMeasurementEntryEdit.route
    ) {
        navBackStackEntry?.arguments?.getString(BODY_MEASUREMENT_TYPE_ARG)?.toBodyMeasurementTypeOrNull()
    } else {
        null
    }
    val currentVitalsMeasurementType = if (
        currentRoute == Screen.VitalsMeasurementEntry.route ||
        currentRoute == Screen.VitalsMeasurementEntryEdit.route
    ) {
        navBackStackEntry?.arguments?.getString(VITALS_MEASUREMENT_TYPE_ARG)?.toVitalsMeasurementTypeOrNull()
    } else {
        null
    }
    var manualEntryTopBarState by remember { mutableStateOf(TopBarEditState()) }

    val topLevelDestinations = remember {
        listOf(
            OpenVitalsNavigationDestination(
                route = Screen.Dashboard.route,
                labelRes = R.string.bottom_nav_dashboard,
                icon = Icons.Outlined.Dashboard,
            ),
        )
    }
    val topLevelRoutes = remember {
        setOf(
            Screen.Dashboard.route,
        )
    }
    val taskRoutes = remember {
        setOf(
            Screen.ManualEntry.route,
            Screen.HydrationEntry.route,
            Screen.HydrationEntryEdit.route,
            Screen.ActivityEntry.route,
            Screen.ActivityEntryEdit.route,
            Screen.MindfulnessEntry.route,
            Screen.MindfulnessEntryEdit.route,
            Screen.BodyMeasurementEntry.route,
            Screen.BodyMeasurementEntryEdit.route,
            Screen.VitalsMeasurementEntry.route,
            Screen.VitalsMeasurementEntryEdit.route,
        )
    }

    val showTopBar = currentRoute != null && currentRoute != Screen.Onboarding.route
    val isTaskRoute = currentRoute?.let { it in taskRoutes } == true
    val showNavigation =
        topLevelDestinations.size > 1 && currentRoute?.let { it in topLevelRoutes } == true
    val canNavigateBack =
        currentRoute != null &&
            currentRoute != Screen.Onboarding.route &&
            currentRoute !in topLevelRoutes &&
            navController.previousBackStackEntry != null
    val navigationSelectedRoute = currentRoute?.takeIf { it in topLevelRoutes }
    val addEntryAction = addEntryActionForCurrentRoute(
        currentRoute = currentRoute,
        currentMetricId = currentMetricId,
        onNavigate = { route -> navController.navigate(route) },
    )

    LaunchedEffect(routeImportRequest?.id, currentRoute) {
        if (
            routeImportRequest != null &&
            currentRoute != null &&
            currentRoute != Screen.Onboarding.route
        ) {
            navController.navigate(Screen.ActivityEntry.route) {
                launchSingleTop = true
            }
        }
    }

    val topBarTitle = when (currentRoute) {
        Screen.Dashboard.route -> stringResource(R.string.app_name)
        CardioLoadDetailRoute -> stringResource(R.string.metric_cardio_load)
        SleepEfficiencyDetailRoute -> stringResource(R.string.recovery_sleep_efficiency)
        SleepScoreDetailRoute -> stringResource(R.string.recovery_sleep_score)
        Screen.ManualEntry.route -> stringResource(R.string.screen_manual_entry)
        Screen.HydrationEntry.route -> stringResource(R.string.screen_hydration_entry)
        Screen.HydrationEntryEdit.route -> stringResource(R.string.screen_hydration_entry)
        Screen.ActivityEntry.route -> stringResource(R.string.screen_activity_entry)
        Screen.ActivityEntryEdit.route -> stringResource(R.string.screen_activity_entry)
        Screen.MindfulnessEntry.route -> stringResource(R.string.screen_mindfulness_entry)
        Screen.MindfulnessEntryEdit.route -> stringResource(R.string.screen_mindfulness_entry)
        Screen.BodyMeasurementEntry.route,
        Screen.BodyMeasurementEntryEdit.route -> currentBodyMeasurementType
            ?.let { stringResource(it.titleRes()) }
            ?: stringResource(R.string.screen_body_measurement_entry)
        Screen.VitalsMeasurementEntry.route,
        Screen.VitalsMeasurementEntryEdit.route -> currentVitalsMeasurementType
            ?.let { stringResource(it.titleRes()) }
            ?: stringResource(R.string.screen_vitals_measurement_entry)
        Screen.Calories.route -> stringResource(R.string.screen_calories)
        Screen.Activity.route -> stringResource(R.string.screen_activities)
        Screen.ActivityDetail.route -> stringResource(R.string.screen_activity_detail)
        Screen.Sleep.route -> stringResource(R.string.screen_sleep)
        Screen.SleepDetail.route -> stringResource(R.string.screen_sleep_detail)
        Screen.Metric.route -> currentMetricId?.let { stringResource(metricTitleRes(it)) }.orEmpty()
        Screen.Settings.route -> stringResource(R.string.screen_settings)
        Screen.Achievements.route -> stringResource(R.string.screen_achievements)
        else -> ""
    }

    OpenVitalsAdaptiveScaffold(
        title = topBarTitle,
        navigationDestinations = topLevelDestinations,
        currentRoute = navigationSelectedRoute,
        showTopBar = showTopBar,
        showNavigation = showNavigation,
        canNavigateBack = canNavigateBack,
        onNavigateBack = { navController.popBackStack() },
        onNavigate = { route ->
            if (route in topLevelRoutes) {
                navController.navigate(route) {
                    popUpTo(Screen.Dashboard.route) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        },
        navigationIcon = Icons.AutoMirrored.Outlined.ArrowBack,
        navigationContentDescription = stringResource(R.string.cd_back),
        action = addEntryAction,
        topBarActions = {
            val topBarEditState = when (currentRoute) {
                Screen.ManualEntry.route -> manualEntryTopBarState
                else -> null
            }
            if (topBarEditState != null) {
                IconButton(onClick = topBarEditState.onToggleEdit) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = stringResource(
                            when {
                                currentRoute == Screen.Dashboard.route && topBarEditState.isEditing ->
                                    R.string.cd_finish_dashboard_editing
                                currentRoute == Screen.Dashboard.route -> R.string.cd_edit_dashboard
                                topBarEditState.isEditing -> R.string.cd_finish_manual_entry_editing
                                else -> R.string.cd_edit_manual_entry_widgets
                            }
                        ),
                        tint = if (topBarEditState.isEditing) {
                            androidx.compose.material3.MaterialTheme.colorScheme.primary
                        } else {
                            androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
            if (showTopBar && !isTaskRoute && currentRoute != Screen.Achievements.route) {
                IconButton(
                    onClick = {
                        navController.navigate(Screen.Achievements.route) {
                            launchSingleTop = true
                        }
                    },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.WorkspacePremium,
                        contentDescription = stringResource(R.string.cd_achievements),
                    )
                }
            }
            if (showTopBar && !isTaskRoute && currentRoute != Screen.Settings.route) {
                IconButton(
                    onClick = {
                        navController.navigate(Screen.Settings.route) {
                            launchSingleTop = true
                        }
                    },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = stringResource(R.string.cd_settings),
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Screen.Onboarding.route) {
                val onboardingViewModel = hiltViewModel<OnboardingViewModel>()
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
                val dashboardViewModel = hiltViewModel<DashboardViewModel>()
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    onGrantPermissions = { navController.navigate(Screen.Settings.route) },
                    onOpenMetric = { metricId ->
                        when (metricId) {
                            DashboardWidgetId.CALORIES_OUT,
                            DashboardWidgetId.ACTIVE_CALORIES,
                            DashboardWidgetId.BMR -> navController.navigate(Screen.Calories.route)
                            DashboardWidgetId.WORKOUT -> navController.navigate(Screen.Activity.route)
                            DashboardWidgetId.SLEEP -> navController.navigate(Screen.Sleep.route)
                            DashboardWidgetId.WEEKLY_CARDIO_LOAD,
                            DashboardWidgetId.CARDIO_LOAD -> navController.navigate(CardioLoadDetailRoute)
                            else -> navController.navigate(Screen.Metric.createRoute(metricId.name))
                        }
                    },
                    onOpenActivities = {
                        navController.navigate(Screen.Activity.route)
                    },
                    onOpenActivity = { activityId ->
                        navController.navigate(Screen.ActivityDetail.createRoute(activityId))
                    },
                    onOpenLog = { navController.navigate(Screen.ManualEntry.route) },
                    onStartActivity = {
                        navController.navigate(Screen.ActivityEntry.route)
                    },
                )
            }

            composable(CardioLoadDetailRoute) {
                val activityOverviewViewModel = hiltViewModel<ActivityOverviewViewModel>()
                CardioLoadDetailScreen(
                    viewModel = activityOverviewViewModel,
                    unitFormatter = unitFormatter,
                )
            }

            composable(SleepEfficiencyDetailRoute) {
                val recoveryViewModel = hiltViewModel<RecoveryViewModel>()
                SleepEfficiencyDetailScreen(
                    viewModel = recoveryViewModel,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                )
            }

            composable(SleepScoreDetailRoute) {
                val recoveryViewModel = hiltViewModel<RecoveryViewModel>()
                SleepScoreDetailScreen(
                    viewModel = recoveryViewModel,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                )
            }

            composable(Screen.ManualEntry.route) {
                val manualEntryViewModel = hiltViewModel<ManualEntryViewModel>()
                ManualEntryScreen(
                    viewModel = manualEntryViewModel,
                    onOpenHydrationEntry = {
                        navController.navigate(Screen.HydrationEntry.route)
                    },
                    onOpenActivityEntry = {
                        navController.navigate(Screen.ActivityEntry.route)
                    },
                    onOpenMindfulnessEntry = {
                        navController.navigate(Screen.MindfulnessEntry.route)
                    },
                    onOpenBodyMeasurementEntry = { type ->
                        navController.navigate(Screen.BodyMeasurementEntry.createRoute(type.name))
                    },
                    onOpenVitalsMeasurementEntry = { type ->
                        navController.navigate(Screen.VitalsMeasurementEntry.createRoute(type.name))
                    },
                    onEditStateChanged = { isEditing, onToggleEdit ->
                        manualEntryTopBarState = TopBarEditState(isEditing, onToggleEdit)
                    },
                )
            }

            composable(Screen.HydrationEntry.route) {
                val hydrationViewModel = hiltViewModel<HydrationEntryViewModel>()
                HydrationEntryScreen(
                    viewModel = hydrationViewModel,
                    unitFormatter = unitFormatter,
                )
            }

            composable(
                route = Screen.HydrationEntryEdit.route,
                arguments = listOf(navArgument(HYDRATION_ENTRY_ID_ARG) { type = NavType.StringType }),
            ) {
                val hydrationViewModel = hiltViewModel<HydrationEntryViewModel>()
                HydrationEntryScreen(
                    viewModel = hydrationViewModel,
                    unitFormatter = unitFormatter,
                    onEntrySaved = { navController.popBackStack() },
                )
            }

            composable(Screen.ActivityEntry.route) {
                val activityEntryViewModel = hiltViewModel<ActivityEntryViewModel>()
                ActivityEntryScreen(
                    viewModel = activityEntryViewModel,
                    unitFormatter = unitFormatter,
                    pendingRouteImportUri = routeImportRequest?.uri,
                    pendingRouteImportRequestId = routeImportRequest?.id,
                    onPendingRouteImportHandled = onRouteImportRequestHandled,
                    onEntrySaved = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Dashboard.route)
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }

            composable(
                route = Screen.ActivityEntryEdit.route,
                arguments = listOf(navArgument(ACTIVITY_ENTRY_ID_ARG) { type = NavType.StringType }),
            ) {
                val activityEntryViewModel = hiltViewModel<ActivityEntryViewModel>()
                ActivityEntryScreen(
                    viewModel = activityEntryViewModel,
                    unitFormatter = unitFormatter,
                    onEntrySaved = { navController.popBackStack() },
                )
            }

            composable(Screen.MindfulnessEntry.route) {
                val mindfulnessEntryViewModel = hiltViewModel<MindfulnessEntryViewModel>()
                MindfulnessEntryScreen(viewModel = mindfulnessEntryViewModel)
            }

            composable(
                route = Screen.MindfulnessEntryEdit.route,
                arguments = listOf(navArgument(MINDFULNESS_ENTRY_ID_ARG) { type = NavType.StringType }),
            ) {
                val mindfulnessEntryViewModel = hiltViewModel<MindfulnessEntryViewModel>()
                MindfulnessEntryScreen(
                    viewModel = mindfulnessEntryViewModel,
                    onEntrySaved = { navController.popBackStack() },
                )
            }

            composable(
                route = Screen.BodyMeasurementEntry.route,
                arguments = listOf(navArgument(BODY_MEASUREMENT_TYPE_ARG) { type = NavType.StringType }),
            ) { backStackEntry ->
                val type = backStackEntry.arguments
                    ?.getString(BODY_MEASUREMENT_TYPE_ARG)
                    ?.toBodyMeasurementTypeOrNull()
                    ?: BodyMeasurementType.WEIGHT
                val bodyMeasurementViewModel = hiltViewModel<BodyMeasurementEntryViewModel>()
                BodyMeasurementEntryScreen(
                    type = type,
                    viewModel = bodyMeasurementViewModel,
                    unitFormatter = unitFormatter,
                )
            }

            composable(
                route = Screen.BodyMeasurementEntryEdit.route,
                arguments = listOf(
                    navArgument(BODY_MEASUREMENT_TYPE_ARG) { type = NavType.StringType },
                    navArgument(BODY_ENTRY_ID_ARG) { type = NavType.StringType },
                ),
            ) { backStackEntry ->
                val type = backStackEntry.arguments
                    ?.getString(BODY_MEASUREMENT_TYPE_ARG)
                    ?.toBodyMeasurementTypeOrNull()
                    ?: BodyMeasurementType.WEIGHT
                val bodyMeasurementViewModel = hiltViewModel<BodyMeasurementEntryViewModel>()
                BodyMeasurementEntryScreen(
                    type = type,
                    viewModel = bodyMeasurementViewModel,
                    unitFormatter = unitFormatter,
                    onEntrySaved = { navController.popBackStack() },
                )
            }

            composable(
                route = Screen.VitalsMeasurementEntry.route,
                arguments = listOf(navArgument(VITALS_MEASUREMENT_TYPE_ARG) { type = NavType.StringType }),
            ) { backStackEntry ->
                val type = backStackEntry.arguments
                    ?.getString(VITALS_MEASUREMENT_TYPE_ARG)
                    ?.toVitalsMeasurementTypeOrNull()
                    ?: VitalsMeasurementType.BLOOD_PRESSURE
                val vitalsMeasurementViewModel = hiltViewModel<VitalsMeasurementEntryViewModel>()
                VitalsMeasurementEntryScreen(
                    type = type,
                    viewModel = vitalsMeasurementViewModel,
                    unitFormatter = unitFormatter,
                )
            }

            composable(
                route = Screen.VitalsMeasurementEntryEdit.route,
                arguments = listOf(
                    navArgument(VITALS_MEASUREMENT_TYPE_ARG) { type = NavType.StringType },
                    navArgument(VITALS_ENTRY_ID_ARG) { type = NavType.StringType },
                ),
            ) { backStackEntry ->
                val type = backStackEntry.arguments
                    ?.getString(VITALS_MEASUREMENT_TYPE_ARG)
                    ?.toVitalsMeasurementTypeOrNull()
                    ?: VitalsMeasurementType.BLOOD_PRESSURE
                val vitalsMeasurementViewModel = hiltViewModel<VitalsMeasurementEntryViewModel>()
                VitalsMeasurementEntryScreen(
                    type = type,
                    viewModel = vitalsMeasurementViewModel,
                    unitFormatter = unitFormatter,
                    onEntrySaved = { navController.popBackStack() },
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
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    onOpenMetric = { targetMetricId ->
                        when (targetMetricId) {
                            DashboardWidgetId.CALORIES_OUT,
                            DashboardWidgetId.ACTIVE_CALORIES,
                            DashboardWidgetId.BMR -> navController.navigate(Screen.Calories.route)
                            DashboardWidgetId.SLEEP -> navController.navigate(Screen.Sleep.route)
                            DashboardWidgetId.WEEKLY_CARDIO_LOAD,
                            DashboardWidgetId.CARDIO_LOAD -> navController.navigate(CardioLoadDetailRoute)
                            else -> navController.navigate(Screen.Metric.createRoute(targetMetricId.name))
                        }
                    },
                    onOpenCardioLoad = {
                        navController.navigate(CardioLoadDetailRoute)
                    },
                    onOpenActivity = { activityId ->
                        navController.navigate(Screen.ActivityDetail.createRoute(activityId))
                    },
                    onEditActivity = { activityId ->
                        navController.navigate(Screen.ActivityEntryEdit.createRoute(activityId))
                    },
                    onOpenSleepSession = { sleepId ->
                        navController.navigate(Screen.SleepDetail.createRoute(sleepId))
                    },
                    onOpenSleepScore = {
                        navController.navigate(SleepScoreDetailRoute)
                    },
                    onOpenSleepEfficiency = {
                        navController.navigate(SleepEfficiencyDetailRoute)
                    },
                    onEditHydrationEntry = { entryId ->
                        navController.navigate(Screen.HydrationEntryEdit.createRoute(entryId))
                    },
                    onEditMindfulnessSession = { entryId ->
                        navController.navigate(Screen.MindfulnessEntryEdit.createRoute(entryId))
                    },
                    onEditBodyMeasurement = { type, entryId ->
                        navController.navigate(Screen.BodyMeasurementEntryEdit.createRoute(type.name, entryId))
                    },
                    onEditVitalsMeasurement = { type, entryId ->
                        navController.navigate(Screen.VitalsMeasurementEntryEdit.createRoute(type.name, entryId))
                    },
                )
            }

            composable(Screen.Calories.route) {
                val caloriesViewModel = hiltViewModel<CaloriesViewModel>()
                CaloriesScreen(
                    viewModel = caloriesViewModel,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                )
            }

            composable(Screen.Activity.route) {
                val activitiesViewModel = hiltViewModel<ActivitiesViewModel>()
                ActivitiesScreen(
                    viewModel = activitiesViewModel,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    onOpenActivity = { activityId ->
                        navController.navigate(Screen.ActivityDetail.createRoute(activityId))
                    },
                    onEditActivity = { activityId ->
                        navController.navigate(Screen.ActivityEntryEdit.createRoute(activityId))
                    },
                    onOpenCardioLoad = {
                        navController.navigate(CardioLoadDetailRoute)
                    },
                    onOpenSteps = {
                        navController.navigate(Screen.Metric.createRoute(DashboardWidgetId.STEPS.name))
                    },
                    onOpenDistance = {
                        navController.navigate(Screen.Metric.createRoute(DashboardWidgetId.DISTANCE.name))
                    },
                    onOpenEnergyBurned = {
                        navController.navigate(Screen.Calories.route)
                    },
                    onOpenHrv = {
                        navController.navigate(Screen.Metric.createRoute(DashboardWidgetId.HRV.name))
                    },
                )
            }

            composable(
                route = Screen.ActivityDetail.route,
                arguments = listOf(navArgument(ACTIVITY_DETAIL_ID_ARG) { type = NavType.StringType }),
            ) {
                val activityDetailViewModel = hiltViewModel<ActivityDetailViewModel>()
                ActivityDetailScreen(
                    viewModel = activityDetailViewModel,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    onEditActivity = { activityId ->
                        navController.navigate(Screen.ActivityEntryEdit.createRoute(activityId))
                    },
                )
            }

            composable(Screen.Sleep.route) {
                val sleepViewModel = hiltViewModel<SleepViewModel>()
                SleepScreen(
                    viewModel = sleepViewModel,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    onOpenSleepSession = { sleepId ->
                        navController.navigate(Screen.SleepDetail.createRoute(sleepId))
                    },
                    onOpenSleepScore = {
                        navController.navigate(SleepScoreDetailRoute)
                    },
                    onOpenSleepEfficiency = {
                        navController.navigate(SleepEfficiencyDetailRoute)
                    },
                )
            }

            composable(
                route = Screen.SleepDetail.route,
                arguments = listOf(navArgument(SLEEP_DETAIL_ID_ARG) { type = NavType.StringType }),
            ) {
                val sleepDetailViewModel = hiltViewModel<SleepDetailViewModel>()
                SleepDetailScreen(
                    viewModel = sleepDetailViewModel,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                )
            }

            composable(Screen.Achievements.route) {
                val achievementsViewModel = hiltViewModel<AchievementsViewModel>()
                AchievementsScreen(
                    viewModel = achievementsViewModel,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                )
            }

            composable(Screen.Settings.route) {
                val settingsViewModel = hiltViewModel<SettingsViewModel>()
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

private fun addEntryActionForCurrentRoute(
    currentRoute: String?,
    currentMetricId: DashboardWidgetId?,
    onNavigate: (String) -> Unit,
): MetricAction? {
    val destinationRoute = when {
        currentRoute == Screen.Activity.route -> Screen.ActivityEntry.route
        currentRoute == Screen.Metric.route -> currentMetricId?.entryRoute()
        else -> null
    } ?: return null

    return MetricAction(
        labelRes = R.string.action_add,
        icon = Icons.Outlined.Add,
        onClick = { onNavigate(destinationRoute) },
    )
}

private fun DashboardWidgetId.entryRoute(): String? =
    when (this) {
        DashboardWidgetId.HYDRATION -> Screen.HydrationEntry.route
        DashboardWidgetId.MINDFULNESS -> Screen.MindfulnessEntry.route
        DashboardWidgetId.WEIGHT -> Screen.BodyMeasurementEntry.createRoute(BodyMeasurementType.WEIGHT.name)
        DashboardWidgetId.HEIGHT -> Screen.BodyMeasurementEntry.createRoute(BodyMeasurementType.HEIGHT.name)
        DashboardWidgetId.BODY_FAT -> Screen.BodyMeasurementEntry.createRoute(BodyMeasurementType.BODY_FAT.name)
        DashboardWidgetId.WORKOUT -> Screen.ActivityEntry.route
        DashboardWidgetId.BLOOD_PRESSURE ->
            Screen.VitalsMeasurementEntry.createRoute(VitalsMeasurementType.BLOOD_PRESSURE.name)
        DashboardWidgetId.SPO2 -> Screen.VitalsMeasurementEntry.createRoute(VitalsMeasurementType.SPO2.name)
        DashboardWidgetId.RESPIRATORY_RATE ->
            Screen.VitalsMeasurementEntry.createRoute(VitalsMeasurementType.RESPIRATORY_RATE.name)
        DashboardWidgetId.BODY_TEMPERATURE ->
            Screen.VitalsMeasurementEntry.createRoute(VitalsMeasurementType.BODY_TEMPERATURE.name)
        else -> null
    }

private fun DashboardWidgetId.isCaloriesDetailMetric(): Boolean =
    this == DashboardWidgetId.CALORIES_OUT ||
        this == DashboardWidgetId.ACTIVE_CALORIES ||
        this == DashboardWidgetId.BMR

@Composable
private fun MetricRouteContent(
    metricId: DashboardWidgetId?,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onOpenMetric: (DashboardWidgetId) -> Unit,
    onOpenCardioLoad: () -> Unit,
    onOpenActivity: (String) -> Unit,
    onEditActivity: (String) -> Unit,
    onOpenSleepSession: (String) -> Unit,
    onOpenSleepScore: () -> Unit,
    onOpenSleepEfficiency: () -> Unit,
    onEditHydrationEntry: (String) -> Unit,
    onEditMindfulnessSession: (String) -> Unit,
    onEditBodyMeasurement: (BodyMeasurementType, String) -> Unit,
    onEditVitalsMeasurement: (VitalsMeasurementType, String) -> Unit,
) {
    if (metricId?.isCaloriesDetailMetric() == true) {
        val caloriesViewModel = hiltViewModel<CaloriesViewModel>()
        CaloriesScreen(
            viewModel = caloriesViewModel,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
        )
        return
    }

    metricId?.toActivityMetricOrNull()?.let { activityMetric ->
        val activityViewModel = hiltViewModel<ActivityViewModel>()
        ActivityMetricRouteScreen(
            metric = activityMetric,
            viewModel = activityViewModel,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
        )
        return
    }

    metricId?.toHeartMetricOrNull()?.let { heartMetric ->
        val heartViewModel = hiltViewModel<HeartViewModel>()
        HeartMetricRouteScreen(
            metric = heartMetric,
            viewModel = heartViewModel,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            onEditVitalsMeasurement = onEditVitalsMeasurement,
        )
        return
    }

    metricId?.toBodyMetricOrNull()?.let { bodyMetric ->
        val bodyViewModel = hiltViewModel<BodyViewModel>()
        BodyMetricRouteScreen(
            metric = bodyMetric,
            viewModel = bodyViewModel,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            onEditBodyMeasurement = onEditBodyMeasurement,
        )
        return
    }

    metricId?.toNutritionMetricOrNull()?.let { nutritionMetric ->
        val nutritionViewModel = hiltViewModel<NutritionViewModel>()
        NutritionMetricRouteScreen(
            metric = nutritionMetric,
            viewModel = nutritionViewModel,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
        )
        return
    }

    when (metricId) {
        DashboardWidgetId.WORKOUT -> {
            val activitiesViewModel = hiltViewModel<ActivitiesViewModel>()
            ActivitiesScreen(
                viewModel = activitiesViewModel,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                onOpenActivity = onOpenActivity,
                onEditActivity = onEditActivity,
                onOpenCardioLoad = onOpenCardioLoad,
                onOpenSteps = { onOpenMetric(DashboardWidgetId.STEPS) },
                onOpenDistance = { onOpenMetric(DashboardWidgetId.DISTANCE) },
                onOpenEnergyBurned = { onOpenMetric(DashboardWidgetId.CALORIES_OUT) },
                onOpenHrv = { onOpenMetric(DashboardWidgetId.HRV) },
            )
        }
        DashboardWidgetId.SLEEP -> {
            val sleepViewModel = hiltViewModel<SleepViewModel>()
            SleepScreen(
                viewModel = sleepViewModel,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                onOpenSleepSession = onOpenSleepSession,
                onOpenSleepScore = onOpenSleepScore,
                onOpenSleepEfficiency = onOpenSleepEfficiency,
            )
        }
        DashboardWidgetId.HYDRATION -> {
            val hydrationViewModel = hiltViewModel<HydrationViewModel>()
            HydrationScreen(
                viewModel = hydrationViewModel,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                onEditHydrationEntry = onEditHydrationEntry,
            )
        }
        DashboardWidgetId.MINDFULNESS -> {
            val mindfulnessViewModel = hiltViewModel<MindfulnessViewModel>()
            MindfulnessScreen(
                viewModel = mindfulnessViewModel,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                onEditMindfulnessSession = onEditMindfulnessSession,
            )
        }
        DashboardWidgetId.CYCLE -> {
            val cycleViewModel = hiltViewModel<CycleViewModel>()
            CycleScreen(
                viewModel = cycleViewModel,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
            )
        }
        DashboardWidgetId.WEEKLY_CARDIO_LOAD,
        DashboardWidgetId.CARDIO_LOAD -> {
            val activityOverviewViewModel = hiltViewModel<ActivityOverviewViewModel>()
            CardioLoadDetailScreen(
                viewModel = activityOverviewViewModel,
                unitFormatter = unitFormatter,
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

@Composable
private fun ActivityMetricRouteScreen(
    metric: ActivityMetric,
    viewModel: ActivityViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    when (metric) {
        ActivityMetric.STEPS -> StepsScreen(viewModel, unitFormatter, dateTimeFormatterProvider)
        ActivityMetric.DISTANCE -> DistanceScreen(viewModel, unitFormatter, dateTimeFormatterProvider)
        ActivityMetric.CALORIES_BURNED -> CaloriesOutScreen(viewModel, unitFormatter, dateTimeFormatterProvider)
        ActivityMetric.ACTIVE_CALORIES -> ActiveCaloriesScreen(viewModel, unitFormatter, dateTimeFormatterProvider)
        ActivityMetric.FLOORS -> FloorsScreen(viewModel, unitFormatter, dateTimeFormatterProvider)
        ActivityMetric.ELEVATION -> ElevationScreen(viewModel, unitFormatter, dateTimeFormatterProvider)
    }
}

@Composable
private fun HeartMetricRouteScreen(
    metric: HeartMetric,
    viewModel: HeartViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onEditVitalsMeasurement: (VitalsMeasurementType, String) -> Unit,
) {
    when (metric) {
        HeartMetric.AVERAGE_HEART_RATE -> AverageHeartRateScreen(viewModel, unitFormatter, dateTimeFormatterProvider)
        HeartMetric.RESTING_HEART_RATE -> RestingHeartRateScreen(viewModel, unitFormatter, dateTimeFormatterProvider)
        HeartMetric.HRV -> HrvScreen(viewModel, unitFormatter, dateTimeFormatterProvider)
        HeartMetric.BLOOD_PRESSURE -> BloodPressureScreen(
            viewModel,
            unitFormatter,
            dateTimeFormatterProvider,
            onEditVitalsMeasurement,
        )
        HeartMetric.SPO2 -> SpO2Screen(viewModel, unitFormatter, dateTimeFormatterProvider, onEditVitalsMeasurement)
        HeartMetric.VO2_MAX -> Vo2MaxScreen(viewModel, unitFormatter, dateTimeFormatterProvider)
        HeartMetric.RESPIRATORY_RATE -> RespiratoryRateScreen(
            viewModel,
            unitFormatter,
            dateTimeFormatterProvider,
            onEditVitalsMeasurement,
        )
        HeartMetric.BODY_TEMPERATURE -> BodyTemperatureScreen(
            viewModel,
            unitFormatter,
            dateTimeFormatterProvider,
            onEditVitalsMeasurement,
        )
    }
}

@Composable
private fun BodyMetricRouteScreen(
    metric: BodyMetric,
    viewModel: BodyViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onEditBodyMeasurement: (BodyMeasurementType, String) -> Unit,
) {
    when (metric) {
        BodyMetric.WEIGHT -> WeightScreen(viewModel, unitFormatter, dateTimeFormatterProvider, onEditBodyMeasurement)
        BodyMetric.HEIGHT -> HeightScreen(viewModel, unitFormatter, dateTimeFormatterProvider, onEditBodyMeasurement)
        BodyMetric.BMI -> BmiScreen(viewModel, unitFormatter, dateTimeFormatterProvider, onEditBodyMeasurement)
        BodyMetric.BODY_FAT -> BodyFatScreen(viewModel, unitFormatter, dateTimeFormatterProvider, onEditBodyMeasurement)
        BodyMetric.LEAN_MASS -> LeanMassScreen(viewModel, unitFormatter, dateTimeFormatterProvider)
        BodyMetric.BMR -> BmrScreen(viewModel, unitFormatter, dateTimeFormatterProvider)
        BodyMetric.BONE_MASS -> BoneMassScreen(viewModel, unitFormatter, dateTimeFormatterProvider)
    }
}

@Composable
private fun NutritionMetricRouteScreen(
    metric: NutritionMetric,
    viewModel: NutritionViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    when (metric) {
        NutritionMetric.CALORIES_IN -> CaloriesInScreen(viewModel, unitFormatter, dateTimeFormatterProvider)
        NutritionMetric.PROTEIN -> ProteinScreen(viewModel, unitFormatter, dateTimeFormatterProvider)
        NutritionMetric.CARBS -> CarbsScreen(viewModel, unitFormatter, dateTimeFormatterProvider)
        NutritionMetric.FAT -> FatScreen(viewModel, unitFormatter, dateTimeFormatterProvider)
    }
}

private fun String.toDashboardWidgetIdOrNull(): DashboardWidgetId? =
    runCatching { DashboardWidgetId.valueOf(this) }.getOrNull()

private fun String.toBodyMeasurementTypeOrNull(): BodyMeasurementType? =
    runCatching { BodyMeasurementType.valueOf(this) }.getOrNull()

private fun String.toVitalsMeasurementTypeOrNull(): VitalsMeasurementType? =
    runCatching { VitalsMeasurementType.valueOf(this) }.getOrNull()

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
        DashboardWidgetId.CALORIES_OUT -> R.string.screen_calories
        DashboardWidgetId.ACTIVE_CALORIES -> R.string.screen_calories
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
        DashboardWidgetId.BMR -> R.string.screen_calories
        DashboardWidgetId.BONE_MASS -> R.string.metric_bone_mass
        DashboardWidgetId.AVG_HEART_RATE -> R.string.metric_avg_heart_rate
        DashboardWidgetId.RESTING_HEART_RATE -> R.string.metric_resting_heart_rate
        DashboardWidgetId.HRV -> R.string.metric_hrv
        DashboardWidgetId.BLOOD_PRESSURE -> R.string.metric_blood_pressure
        DashboardWidgetId.SPO2 -> R.string.metric_spo2
        DashboardWidgetId.VO2_MAX -> R.string.metric_vo2_max
        DashboardWidgetId.RESPIRATORY_RATE -> R.string.metric_respiratory_rate
        DashboardWidgetId.BODY_TEMPERATURE -> R.string.metric_body_temp
        DashboardWidgetId.WEEKLY_CARDIO_LOAD -> R.string.metric_weekly_cardio_load
        DashboardWidgetId.CARDIO_LOAD -> R.string.metric_weekly_cardio_load
        DashboardWidgetId.MINDFULNESS -> R.string.metric_mindfulness
        DashboardWidgetId.CYCLE -> R.string.metric_cycle
    }

private data class TopBarEditState(
    val isEditing: Boolean = false,
    val onToggleEdit: () -> Unit = {},
)
