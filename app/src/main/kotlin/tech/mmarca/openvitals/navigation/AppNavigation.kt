package tech.mmarca.openvitals.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
import tech.mmarca.openvitals.features.activity.ActivityDetailScreen
import tech.mmarca.openvitals.features.activity.ActivityDetailViewModel
import tech.mmarca.openvitals.features.activity.ActiveCaloriesScreen
import tech.mmarca.openvitals.features.activity.ActivityMetric
import tech.mmarca.openvitals.features.activity.ActivityViewModel
import tech.mmarca.openvitals.features.activity.ActivitiesScreen
import tech.mmarca.openvitals.features.activity.ActivitiesViewModel
import tech.mmarca.openvitals.features.activity.CaloriesOutScreen
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
import tech.mmarca.openvitals.features.browse.BrowseScreen
import tech.mmarca.openvitals.features.browse.BrowseViewModel
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
import tech.mmarca.openvitals.features.manualentry.BodyMeasurementEntryScreen
import tech.mmarca.openvitals.features.manualentry.BodyMeasurementEntryViewModel
import tech.mmarca.openvitals.features.manualentry.HydrationEntryScreen
import tech.mmarca.openvitals.features.manualentry.HydrationEntryViewModel
import tech.mmarca.openvitals.features.manualentry.ManualEntryScreen
import tech.mmarca.openvitals.features.manualentry.ManualEntryViewModel
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
import tech.mmarca.openvitals.features.settings.SettingsScreen
import tech.mmarca.openvitals.features.settings.SettingsViewModel
import tech.mmarca.openvitals.features.sleep.SleepDetailScreen
import tech.mmarca.openvitals.features.sleep.SleepDetailViewModel
import tech.mmarca.openvitals.features.sleep.SleepScreen
import tech.mmarca.openvitals.features.sleep.SleepViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
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
    val currentBodyMeasurementType = if (currentRoute == Screen.BodyMeasurementEntry.route) {
        navBackStackEntry?.arguments?.getString(BODY_MEASUREMENT_TYPE_ARG)?.toBodyMeasurementTypeOrNull()
    } else {
        null
    }
    val currentVitalsMeasurementType = if (currentRoute == Screen.VitalsMeasurementEntry.route) {
        navBackStackEntry?.arguments?.getString(VITALS_MEASUREMENT_TYPE_ARG)?.toVitalsMeasurementTypeOrNull()
    } else {
        null
    }
    var dashboardTopBarState by remember { mutableStateOf(TopBarEditState()) }
    var manualEntryTopBarState by remember { mutableStateOf(TopBarEditState()) }

    val showTopBar = currentRoute != Screen.Onboarding.route
    val showBottomBar = currentRoute == Screen.Dashboard.route || currentRoute == Screen.ManualEntry.route
    val canNavigateBack =
        currentRoute != null &&
            currentRoute != Screen.Onboarding.route &&
            currentRoute != Screen.Dashboard.route &&
            currentRoute != Screen.ManualEntry.route &&
            navController.previousBackStackEntry != null

    val topBarTitle = when (currentRoute) {
        Screen.Dashboard.route -> stringResource(R.string.screen_dashboard)
        Screen.ManualEntry.route -> stringResource(R.string.screen_manual_entry)
        Screen.HydrationEntry.route -> stringResource(R.string.screen_hydration_entry)
        Screen.BodyMeasurementEntry.route -> currentBodyMeasurementType
            ?.let { stringResource(it.titleRes()) }
            ?: stringResource(R.string.screen_body_measurement_entry)
        Screen.VitalsMeasurementEntry.route -> currentVitalsMeasurementType
            ?.let { stringResource(it.titleRes()) }
            ?: stringResource(R.string.screen_vitals_measurement_entry)
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
                        val topBarEditState = when (currentRoute) {
                            Screen.Dashboard.route -> dashboardTopBarState
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
                        if (currentRoute != Screen.Settings.route) {
                            IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                                Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.cd_settings))
                            }
                        }
                    },
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                OpenVitalsBottomNavigation(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(Screen.Dashboard.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
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
                    onOpenMetric = { metricId -> navController.navigate(Screen.Metric.createRoute(metricId.name)) },
                    onOpenBrowse = { navController.navigate(Screen.Browse.route) },
                    onEditStateChanged = { isEditing, onToggleEdit ->
                        dashboardTopBarState = TopBarEditState(isEditing, onToggleEdit)
                    },
                )
            }

            composable(Screen.ManualEntry.route) {
                val manualEntryViewModel = hiltViewModel<ManualEntryViewModel>()
                ManualEntryScreen(
                    viewModel = manualEntryViewModel,
                    onOpenHydrationEntry = {
                        navController.navigate(Screen.HydrationEntry.route)
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
                    onOpenActivity = { activityId ->
                        navController.navigate(Screen.ActivityDetail.createRoute(activityId))
                    },
                    onOpenSleepSession = { sleepId ->
                        navController.navigate(Screen.SleepDetail.createRoute(sleepId))
                    },
                )
            }

            composable(Screen.Steps.route) {
                val activityViewModel = hiltViewModel<ActivityViewModel>()
                StepsScreen(
                    viewModel = activityViewModel,
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

            composable(Screen.Heart.route) {
                val heartViewModel = hiltViewModel<HeartViewModel>()
                AverageHeartRateScreen(
                    viewModel = heartViewModel,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                )
            }

            composable(Screen.Body.route) {
                val bodyViewModel = hiltViewModel<BodyViewModel>()
                WeightScreen(
                    viewModel = bodyViewModel,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                )
            }

            composable(Screen.Hydration.route) {
                val hydrationViewModel = hiltViewModel<HydrationViewModel>()
                HydrationScreen(
                    viewModel = hydrationViewModel,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                )
            }

            composable(Screen.Nutrition.route) {
                val nutritionViewModel = hiltViewModel<NutritionViewModel>()
                CaloriesInScreen(
                    viewModel = nutritionViewModel,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                )
            }

            composable(Screen.Mindfulness.route) {
                val mindfulnessViewModel = hiltViewModel<MindfulnessViewModel>()
                MindfulnessScreen(
                    viewModel = mindfulnessViewModel,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                )
            }

            composable(Screen.Cycle.route) {
                val cycleViewModel = hiltViewModel<CycleViewModel>()
                CycleScreen(
                    viewModel = cycleViewModel,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                )
            }

            composable(Screen.Browse.route) {
                val browseViewModel = hiltViewModel<BrowseViewModel>()
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
                val settingsViewModel = hiltViewModel<SettingsViewModel>()
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

@Composable
private fun OpenVitalsBottomNavigation(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
) {
    val destinations = listOf(
        BottomNavigationDestination(
            route = Screen.Dashboard.route,
            labelRes = R.string.bottom_nav_dashboard,
            icon = Icons.Outlined.Dashboard,
        ),
        BottomNavigationDestination(
            route = Screen.ManualEntry.route,
            labelRes = R.string.bottom_nav_add_entry,
            icon = Icons.Outlined.AddCircleOutline,
        ),
    )

    NavigationBar {
        destinations.forEach { destination ->
            NavigationBarItem(
                selected = currentRoute == destination.route,
                onClick = {
                    if (currentRoute != destination.route) {
                        onNavigate(destination.route)
                    }
                },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = null,
                    )
                },
                label = { Text(stringResource(destination.labelRes)) },
            )
        }
    }
}

@Composable
private fun MetricRouteContent(
    metricId: DashboardWidgetId?,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onOpenActivity: (String) -> Unit,
    onOpenSleepSession: (String) -> Unit,
) {
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
            )
        }
        DashboardWidgetId.SLEEP -> {
            val sleepViewModel = hiltViewModel<SleepViewModel>()
            SleepScreen(
                viewModel = sleepViewModel,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                onOpenSleepSession = onOpenSleepSession,
            )
        }
        DashboardWidgetId.HYDRATION -> {
            val hydrationViewModel = hiltViewModel<HydrationViewModel>()
            HydrationScreen(
                viewModel = hydrationViewModel,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
            )
        }
        DashboardWidgetId.MINDFULNESS -> {
            val mindfulnessViewModel = hiltViewModel<MindfulnessViewModel>()
            MindfulnessScreen(
                viewModel = mindfulnessViewModel,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
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
) {
    when (metric) {
        HeartMetric.AVERAGE_HEART_RATE -> AverageHeartRateScreen(viewModel, unitFormatter, dateTimeFormatterProvider)
        HeartMetric.RESTING_HEART_RATE -> RestingHeartRateScreen(viewModel, unitFormatter, dateTimeFormatterProvider)
        HeartMetric.HRV -> HrvScreen(viewModel, unitFormatter, dateTimeFormatterProvider)
        HeartMetric.BLOOD_PRESSURE -> BloodPressureScreen(viewModel, unitFormatter, dateTimeFormatterProvider)
        HeartMetric.SPO2 -> SpO2Screen(viewModel, unitFormatter, dateTimeFormatterProvider)
        HeartMetric.VO2_MAX -> Vo2MaxScreen(viewModel, unitFormatter, dateTimeFormatterProvider)
        HeartMetric.RESPIRATORY_RATE -> RespiratoryRateScreen(viewModel, unitFormatter, dateTimeFormatterProvider)
        HeartMetric.BODY_TEMPERATURE -> BodyTemperatureScreen(viewModel, unitFormatter, dateTimeFormatterProvider)
    }
}

@Composable
private fun BodyMetricRouteScreen(
    metric: BodyMetric,
    viewModel: BodyViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    when (metric) {
        BodyMetric.WEIGHT -> WeightScreen(viewModel, unitFormatter, dateTimeFormatterProvider)
        BodyMetric.HEIGHT -> HeightScreen(viewModel, unitFormatter, dateTimeFormatterProvider)
        BodyMetric.BMI -> BmiScreen(viewModel, unitFormatter, dateTimeFormatterProvider)
        BodyMetric.BODY_FAT -> BodyFatScreen(viewModel, unitFormatter, dateTimeFormatterProvider)
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

private data class TopBarEditState(
    val isEditing: Boolean = false,
    val onToggleEdit: () -> Unit = {},
)

private data class BottomNavigationDestination(
    val route: String,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
)
