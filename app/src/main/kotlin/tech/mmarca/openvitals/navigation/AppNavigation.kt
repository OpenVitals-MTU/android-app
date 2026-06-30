package tech.mmarca.openvitals.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
import tech.mmarca.openvitals.features.activity.ActivitiesScreen
import tech.mmarca.openvitals.features.activity.ActivitiesViewModel
import tech.mmarca.openvitals.features.activity.CaloriesScreen
import tech.mmarca.openvitals.features.activity.CaloriesViewModel
import tech.mmarca.openvitals.features.activity.CardioLoadDetailScreen
import tech.mmarca.openvitals.features.body.BodyScreen
import tech.mmarca.openvitals.features.body.BodyViewModel
import tech.mmarca.openvitals.features.bodyenergy.BodyEnergyDetailsScreen
import tech.mmarca.openvitals.features.bodyenergy.BodyEnergyViewModel
import tech.mmarca.openvitals.features.dashboard.DashboardScreen
import tech.mmarca.openvitals.features.dashboard.DashboardViewModel
import tech.mmarca.openvitals.features.dashboard.DashboardWidgetId
import tech.mmarca.openvitals.features.heart.HeartViewModel
import tech.mmarca.openvitals.features.heart.HeartVitalsOverviewScreen
import tech.mmarca.openvitals.domain.preferences.AppThemeMode
import tech.mmarca.openvitals.domain.model.BodyMeasurementType
import tech.mmarca.openvitals.domain.model.VitalsMeasurementType
import tech.mmarca.openvitals.features.manualentry.body.titleRes
import tech.mmarca.openvitals.features.manualentry.activity.recording.ActivityRecordingOutdoorModeToggle
import tech.mmarca.openvitals.features.manualentry.vitals.titleRes
import tech.mmarca.openvitals.features.nutrition.NutritionScreen
import tech.mmarca.openvitals.features.nutrition.NutritionViewModel
import tech.mmarca.openvitals.features.onboarding.OnboardingScreen
import tech.mmarca.openvitals.features.onboarding.OnboardingViewModel
import tech.mmarca.openvitals.features.readiness.DailyReadinessScreen
import tech.mmarca.openvitals.features.readiness.DailyReadinessViewModel
import tech.mmarca.openvitals.features.readiness.StressDetailsScreen
import tech.mmarca.openvitals.features.readiness.TrainingReadinessDetailsScreen
import tech.mmarca.openvitals.features.recovery.RecoveryViewModel
import tech.mmarca.openvitals.features.recovery.SleepEfficiencyDetailScreen
import tech.mmarca.openvitals.features.recovery.SleepScoreDetailScreen
import tech.mmarca.openvitals.features.sleep.SleepDetailScreen
import tech.mmarca.openvitals.features.sleep.SleepDetailViewModel
import tech.mmarca.openvitals.features.sleep.SleepScreen
import tech.mmarca.openvitals.features.sleep.SleepViewModel
import tech.mmarca.openvitals.ui.components.MetricAction
import tech.mmarca.openvitals.ui.components.OpenVitalsAdaptiveScaffold
import tech.mmarca.openvitals.ui.components.OpenVitalsNavigationDestination
import tech.mmarca.openvitals.ui.components.OpenVitalsIconButton
import tech.mmarca.openvitals.ui.components.HealthConnectNewPermissionsPrompt
import tech.mmarca.openvitals.ui.components.PrivacyReconsentPrompt

internal const val CardioLoadDetailRoute = "activity/cardio_load"
internal const val SleepEfficiencyDetailRoute = "recovery/sleep_efficiency"
internal const val SleepScoreDetailRoute = "recovery/sleep_score"

@Composable
fun AppNavigation(
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    startDestination: String,
    appThemeMode: AppThemeMode = AppThemeMode.SYSTEM,
    routeImportRequest: ExternalRouteImportRequest? = null,
    externalNavigationRoute: String? = null,
    onRouteImportRequestHandled: (Long) -> Unit = {},
    onExternalNavigationHandled: () -> Unit = {},
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
    var metricSectionTopBarState by remember { mutableStateOf<TopBarEditState?>(null) }
    var activityEntryTopBarTitleRes by remember { mutableStateOf<Int?>(null) }
    var activityEntryTopBarEditState by remember { mutableStateOf<TopBarEditState?>(null) }
    var activityRecordingOutdoorTopBarState by remember { mutableStateOf<TopBarOutdoorModeState?>(null) }
    var isActivityRecordingFocusMode by remember { mutableStateOf(false) }
    var dashboardRefreshRequest by remember { mutableIntStateOf(0) }

    fun markDashboardDirty() {
        dashboardRefreshRequest += 1
    }

    fun markDashboardDirtyAndPopBack() {
        markDashboardDirty()
        navController.popBackStack()
    }

    fun navigateToDashboardAfterActivitySave() {
        navController.navigate(Screen.Dashboard.route) {
            popUpTo(Screen.Dashboard.route)
            launchSingleTop = true
            restoreState = true
        }
    }

    fun finishActivityEntrySave() {
        markDashboardDirty()
        val previousRoute = navController.previousBackStackEntry?.destination?.route
        if (
            previousRoute == Screen.Dashboard.route ||
            previousRoute == Screen.Activity.route ||
            previousRoute == Screen.Metric.route
        ) {
            if (navController.popBackStack()) return
        }
        navigateToDashboardAfterActivitySave()
    }

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
            Screen.CarbsEntry.route,
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
    val settingsRoutes = remember {
        setOf(
            Screen.Settings.route,
            Screen.SettingsDisplay.route,
            Screen.SettingsActivities.route,
            Screen.SettingsSensors.route,
            Screen.SettingsCalories.route,
            Screen.SettingsSleep.route,
            Screen.SettingsBodyEnergy.route,
            Screen.SettingsCycle.route,
            Screen.SettingsDataImport.route,
            Screen.SettingsHealthConnect.route,
            Screen.SettingsPermissions.route,
            Screen.SettingsDebugDiagnostics.route,
        )
    }

    val isActivityEntryRoute =
        currentRoute == Screen.ActivityEntry.route ||
            currentRoute == Screen.ActivityEntryEdit.route
    val isActivityRecordingFocusRoute = isActivityEntryRoute && isActivityRecordingFocusMode
    val showTopBar = currentRoute != null &&
        currentRoute != Screen.Onboarding.route &&
        !isActivityRecordingFocusRoute
    val isTaskRoute = currentRoute?.let { it in taskRoutes } == true
    val isSettingsRoute = currentRoute?.let { it in settingsRoutes } == true
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

    val metricSectionRoutes = remember {
        setOf(
            Screen.Metric.route,
            Screen.HeartVitals.route,
            Screen.Activity.route,
            Screen.Sleep.route,
            Screen.Body.route,
        )
    }

    LaunchedEffect(currentRoute) {
        if (currentRoute !in metricSectionRoutes) {
            metricSectionTopBarState = null
        }
    }

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

    LaunchedEffect(isActivityEntryRoute) {
        if (!isActivityEntryRoute) {
            isActivityRecordingFocusMode = false
        }
    }

    LaunchedEffect(externalNavigationRoute, currentRoute) {
        if (
            externalNavigationRoute != null &&
            currentRoute != null &&
            currentRoute != Screen.Onboarding.route
        ) {
            navController.navigate(externalNavigationRoute) {
                launchSingleTop = true
            }
            onExternalNavigationHandled()
        }
    }

    val topBarTitle = when (currentRoute) {
        Screen.Dashboard.route -> stringResource(R.string.app_name)
        Screen.DailyReadiness.route -> stringResource(R.string.screen_daily_readiness)
        Screen.StressDetails.route -> stringResource(R.string.screen_stress_tracking)
        Screen.BodyEnergyDetails.route -> stringResource(R.string.screen_body_energy)
        Screen.TrainingReadinessDetails.route -> stringResource(R.string.screen_training_readiness)
        CardioLoadDetailRoute -> stringResource(R.string.metric_cardio_load)
        SleepEfficiencyDetailRoute -> stringResource(R.string.recovery_sleep_efficiency)
        SleepScoreDetailRoute -> stringResource(R.string.recovery_sleep_score)
        Screen.ManualEntry.route -> stringResource(R.string.screen_manual_entry)
        Screen.HydrationEntry.route -> stringResource(R.string.screen_hydration_entry)
        Screen.HydrationEntryEdit.route -> stringResource(R.string.screen_hydration_entry)
        Screen.CarbsEntry.route -> stringResource(R.string.screen_carbs_entry)
        Screen.ActivityEntry.route -> activityEntryTopBarTitleRes
            ?.let { stringResource(it) }
            ?: stringResource(R.string.screen_activity_entry)
        Screen.ActivityEntryEdit.route -> activityEntryTopBarTitleRes
            ?.let { stringResource(it) }
            ?: stringResource(R.string.screen_activity_entry)
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
        Screen.Nutrition.route -> stringResource(R.string.screen_nutrition)
        Screen.Body.route -> stringResource(R.string.screen_body)
        Screen.HeartVitals.route -> stringResource(R.string.screen_heart_vitals)
        Screen.Activity.route -> stringResource(R.string.screen_activities)
        Screen.ActivityDetail.route -> stringResource(R.string.screen_activity_detail)
        Screen.Sleep.route -> stringResource(R.string.screen_sleep)
        Screen.SleepDetail.route -> stringResource(R.string.screen_sleep_detail)
        Screen.Metric.route -> currentMetricId?.let { stringResource(metricTitleRes(it)) }.orEmpty()
        Screen.Settings.route -> stringResource(R.string.screen_settings)
        Screen.SettingsDisplay.route -> stringResource(R.string.settings_display_group_title)
        Screen.SettingsActivities.route -> stringResource(R.string.settings_activities_group_title)
        Screen.SettingsSensors.route -> stringResource(R.string.settings_sensors_group_title)
        Screen.SettingsCalories.route -> stringResource(R.string.settings_calories_group_title)
        Screen.SettingsSleep.route -> stringResource(R.string.settings_sleep_group_title)
        Screen.SettingsBodyEnergy.route -> stringResource(R.string.settings_body_energy_group_title)
        Screen.SettingsCycle.route -> stringResource(R.string.settings_cycle_group_title)
        Screen.SettingsDataImport.route -> stringResource(R.string.settings_data_import_group_title)
        Screen.SettingsHealthConnect.route -> stringResource(R.string.settings_health_connect_group_title)
        Screen.SettingsPermissions.route -> stringResource(R.string.settings_permissions_group_title)
        Screen.SettingsDebugDiagnostics.route -> stringResource(R.string.settings_debug_diagnostics_group_title)
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
                in metricSectionRoutes -> metricSectionTopBarState
                Screen.ActivityEntry.route,
                Screen.ActivityEntryEdit.route -> activityEntryTopBarEditState
                else -> null
            }
            val isActivityRecordingRoute =
                currentRoute == Screen.ActivityEntry.route ||
                    currentRoute == Screen.ActivityEntryEdit.route
            if (isActivityRecordingRoute) {
                activityRecordingOutdoorTopBarState?.let { outdoorState ->
                    ActivityRecordingOutdoorModeToggle(
                        enabled = outdoorState.enabled,
                        onEnabledChange = { outdoorState.onToggle() },
                        appThemeMode = appThemeMode,
                    )
                }
            }
            if (topBarEditState != null) {
                val isActivityRecordingEditState = isActivityRecordingRoute
                OpenVitalsIconButton(onClick = topBarEditState.onToggleEdit) {
                    Icon(
                        imageVector = if (isActivityRecordingEditState && topBarEditState.isEditing) {
                            Icons.Outlined.Check
                        } else {
                            Icons.Outlined.Edit
                        },
                        contentDescription = stringResource(
                            when {
                                isActivityRecordingEditState && topBarEditState.isEditing ->
                                    R.string.cd_finish_recording_dashboard_editing
                                isActivityRecordingEditState -> R.string.cd_edit_recording_dashboard
                                currentRoute == Screen.Dashboard.route && topBarEditState.isEditing ->
                                    R.string.cd_finish_dashboard_editing
                                currentRoute == Screen.Dashboard.route -> R.string.cd_edit_dashboard
                                currentRoute in metricSectionRoutes && topBarEditState.isEditing ->
                                    R.string.cd_finish_metric_section_editing
                                currentRoute in metricSectionRoutes -> R.string.cd_edit_metric_sections
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
            if (currentRoute == Screen.Dashboard.route) {
                OpenVitalsIconButton(
                    onClick = {
                        navController.navigate(Screen.DailyReadiness.route) {
                            launchSingleTop = true
                        }
                    },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SelfImprovement,
                        contentDescription = stringResource(R.string.cd_daily_readiness),
                    )
                }
                OpenVitalsIconButton(
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
                OpenVitalsIconButton(
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
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                            launchSingleTop = true
                        }
                        onOnboardingComplete()
                    },
                )
            }

            composable(Screen.Dashboard.route) {
                val dashboardViewModel = hiltViewModel<DashboardViewModel>()
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    refreshRequest = dashboardRefreshRequest,
                    onOpenMetric = { metricId ->
                        when (metricId) {
                            DashboardWidgetId.CALORIES_OUT,
                            DashboardWidgetId.ACTIVE_CALORIES,
                            DashboardWidgetId.BMR -> navController.navigate(Screen.Calories.route)
                            DashboardWidgetId.CALORIES_IN,
                            DashboardWidgetId.PROTEIN,
                            DashboardWidgetId.CARBS,
                            DashboardWidgetId.FAT -> navController.navigate(Screen.Nutrition.route)
                            DashboardWidgetId.WEIGHT,
                            DashboardWidgetId.HEIGHT,
                            DashboardWidgetId.BMI,
                            DashboardWidgetId.FFMI,
                            DashboardWidgetId.BODY_FAT,
                            DashboardWidgetId.LEAN_MASS,
                            DashboardWidgetId.BONE_MASS,
                            DashboardWidgetId.BODY_WATER_MASS -> navController.navigate(Screen.Body.route)
                            DashboardWidgetId.WORKOUT -> navController.navigate(Screen.Activity.route)
                            DashboardWidgetId.SLEEP -> navController.navigate(Screen.Sleep.route)
                            DashboardWidgetId.BODY_ENERGY -> {
                                navController.navigate(Screen.BodyEnergyDetails.createRoute(java.time.LocalDate.now().toString()))
                            }
                            DashboardWidgetId.WEEKLY_CARDIO_LOAD,
                            DashboardWidgetId.CARDIO_LOAD -> navController.navigate(CardioLoadDetailRoute)
                            else -> if (metricId.isHeartVitalsMetric()) {
                                navController.navigate(Screen.HeartVitals.route)
                            } else {
                                navController.navigate(Screen.Metric.createRoute(metricId.name))
                            }
                        }
                    },
                    onOpenActivities = {
                        navController.navigate(Screen.Activity.route)
                    },
                    onOpenActivity = { activityId ->
                        navController.navigate(Screen.ActivityDetail.createRoute(activityId))
                    },
                    onEditActivity = { activityId ->
                        navController.navigate(Screen.ActivityEntryEdit.createRoute(activityId))
                    },
                    onOpenLog = { navController.navigate(Screen.ManualEntry.route) },
                    onStartActivity = {
                        navController.navigate(Screen.ActivityEntry.route)
                    },
                )
            }

            composable(Screen.DailyReadiness.route) {
                val dailyReadinessViewModel = hiltViewModel<DailyReadinessViewModel>()
                DailyReadinessScreen(
                    viewModel = dailyReadinessViewModel,
                    onOpenBodyEnergyDetails = { date ->
                        navController.navigate(Screen.BodyEnergyDetails.createRoute(date.toString()))
                    },
                    onOpenTrainingReadinessDetails = { date ->
                        navController.navigate(Screen.TrainingReadinessDetails.createRoute(date.toString()))
                    },
                    onOpenStressDetails = { date ->
                        navController.navigate(Screen.StressDetails.createRoute(date.toString()))
                    },
                )
            }

            composable(
                route = Screen.BodyEnergyDetails.route,
                arguments = listOf(navArgument(BODY_ENERGY_DATE_ARG) { type = NavType.StringType }),
            ) { entry ->
                val bodyEnergyViewModel = hiltViewModel<BodyEnergyViewModel>()
                val selectedDate = entry.arguments
                    ?.getString(BODY_ENERGY_DATE_ARG)
                    ?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() }
                    ?: java.time.LocalDate.now()
                BodyEnergyDetailsScreen(
                    viewModel = bodyEnergyViewModel,
                    selectedDate = selectedDate,
                )
            }

            composable(
                route = Screen.TrainingReadinessDetails.route,
                arguments = listOf(navArgument(TRAINING_READINESS_DATE_ARG) { type = NavType.StringType }),
            ) { entry ->
                val dailyReadinessViewModel = hiltViewModel<DailyReadinessViewModel>()
                val selectedDate = entry.arguments
                    ?.getString(TRAINING_READINESS_DATE_ARG)
                    ?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() }
                    ?: java.time.LocalDate.now()
                TrainingReadinessDetailsScreen(
                    viewModel = dailyReadinessViewModel,
                    selectedDate = selectedDate,
                )
            }

            composable(
                route = Screen.StressDetails.route,
                arguments = listOf(navArgument(STRESS_DATE_ARG) { type = NavType.StringType }),
            ) { entry ->
                val dailyReadinessViewModel = hiltViewModel<DailyReadinessViewModel>()
                val selectedDate = entry.arguments
                    ?.getString(STRESS_DATE_ARG)
                    ?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() }
                    ?: java.time.LocalDate.now()
                StressDetailsScreen(
                    viewModel = dailyReadinessViewModel,
                    selectedDate = selectedDate,
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

            manualEntryRoutes(
                navController = navController,
                unitFormatter = unitFormatter,
                appThemeMode = appThemeMode,
                routeImportRequest = routeImportRequest,
                onRouteImportRequestHandled = onRouteImportRequestHandled,
                onManualEntryEditStateChanged = { isEditing, onToggleEdit ->
                    manualEntryTopBarState = TopBarEditState(isEditing, onToggleEdit)
                },
                onActivityEntryTitleChanged = { titleRes ->
                    activityEntryTopBarTitleRes = titleRes
                },
                onActivityEntryEditStateChanged = { isAvailable, isEditing, onToggleEdit ->
                    activityEntryTopBarEditState = if (isAvailable) {
                        TopBarEditState(isEditing, onToggleEdit)
                    } else {
                        null
                    }
                },
                onActivityEntryFocusModeChanged = { isActivityRecordingFocusMode = it },
                onActivityRecordingOutdoorModeStateChanged = { isAvailable, enabled, onToggle ->
                    activityRecordingOutdoorTopBarState = if (isAvailable) {
                        TopBarOutdoorModeState(enabled, onToggle)
                    } else {
                        null
                    }
                },
                onEntrySaved = ::markDashboardDirty,
                onEntrySavedAndPopBack = ::markDashboardDirtyAndPopBack,
                onActivityEntrySaved = ::finishActivityEntrySave,
            )

            composable(Screen.HeartVitals.route) {
                val heartViewModel = hiltViewModel<HeartViewModel>()
                HeartVitalsOverviewScreen(
                    viewModel = heartViewModel,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    onOpenMetric = { metric ->
                        navController.navigate(Screen.Metric.createRoute(metric.toDashboardWidgetId().name))
                    },
                    onSectionEditStateChanged = { isEditing, onToggleEdit ->
                        metricSectionTopBarState = TopBarEditState(isEditing, onToggleEdit)
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
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    onOpenMetric = { targetMetricId ->
                        when (targetMetricId) {
                            DashboardWidgetId.CALORIES_OUT,
                            DashboardWidgetId.ACTIVE_CALORIES,
                            DashboardWidgetId.BMR -> navController.navigate(Screen.Calories.route)
                            DashboardWidgetId.CALORIES_IN,
                            DashboardWidgetId.PROTEIN,
                            DashboardWidgetId.CARBS,
                            DashboardWidgetId.FAT -> navController.navigate(Screen.Nutrition.route)
                            DashboardWidgetId.WEIGHT,
                            DashboardWidgetId.HEIGHT,
                            DashboardWidgetId.BMI,
                            DashboardWidgetId.FFMI,
                            DashboardWidgetId.BODY_FAT,
                            DashboardWidgetId.LEAN_MASS,
                            DashboardWidgetId.BONE_MASS,
                            DashboardWidgetId.BODY_WATER_MASS -> navController.navigate(Screen.Body.route)
                            DashboardWidgetId.SLEEP -> navController.navigate(Screen.Sleep.route)
                            DashboardWidgetId.BODY_ENERGY -> {
                                navController.navigate(Screen.BodyEnergyDetails.createRoute(java.time.LocalDate.now().toString()))
                            }
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
                    onSectionEditStateChanged = { isEditing, onToggleEdit ->
                        metricSectionTopBarState = TopBarEditState(isEditing, onToggleEdit)
                    },
                )
            }

            composable(Screen.Calories.route) {
                val caloriesViewModel = hiltViewModel<CaloriesViewModel>()
                CaloriesScreen(
                    viewModel = caloriesViewModel,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    onSectionEditStateChanged = { isEditing, onToggleEdit ->
                        metricSectionTopBarState = TopBarEditState(isEditing, onToggleEdit)
                    },
                )
            }

            composable(Screen.Nutrition.route) {
                val nutritionViewModel = hiltViewModel<NutritionViewModel>()
                NutritionScreen(
                    viewModel = nutritionViewModel,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    onSectionEditStateChanged = { isEditing, onToggleEdit ->
                        metricSectionTopBarState = TopBarEditState(isEditing, onToggleEdit)
                    },
                )
            }

            composable(Screen.Body.route) {
                val bodyViewModel = hiltViewModel<BodyViewModel>()
                BodyScreen(
                    viewModel = bodyViewModel,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    onEditBodyMeasurement = { type, entryId ->
                        navController.navigate(Screen.BodyMeasurementEntryEdit.createRoute(type.name, entryId))
                    },
                    onSectionEditStateChanged = { isEditing, onToggleEdit ->
                        metricSectionTopBarState = TopBarEditState(isEditing, onToggleEdit)
                    },
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
                    onSectionEditStateChanged = { isEditing, onToggleEdit ->
                        metricSectionTopBarState = TopBarEditState(isEditing, onToggleEdit)
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
                    onDeleteActivity = {
                        markDashboardDirty()
                        navController.popBackStack()
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
                    onSectionEditStateChanged = { isEditing, onToggleEdit ->
                        metricSectionTopBarState = TopBarEditState(isEditing, onToggleEdit)
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

            settingsRoutes(navController)
        }
    }
    HealthConnectNewPermissionsPrompt()
    PrivacyReconsentPrompt()
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
        DashboardWidgetId.CARBS -> Screen.CarbsEntry.route
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

private fun metricTitleRes(metricId: DashboardWidgetId): Int =
    when (metricId) {
        DashboardWidgetId.STEPS -> R.string.metric_steps
        DashboardWidgetId.DISTANCE -> R.string.metric_distance
        DashboardWidgetId.CALORIES_OUT -> R.string.screen_calories
        DashboardWidgetId.ACTIVE_CALORIES -> R.string.screen_calories
        DashboardWidgetId.FLOORS -> R.string.metric_floors_climbed
        DashboardWidgetId.ELEVATION -> R.string.metric_elevation
        DashboardWidgetId.WHEELCHAIR_PUSHES -> R.string.metric_wheelchair_pushes
        DashboardWidgetId.WORKOUT -> R.string.metric_workout
        DashboardWidgetId.SLEEP -> R.string.metric_sleep
        DashboardWidgetId.BODY_ENERGY -> R.string.metric_body_energy
        DashboardWidgetId.HYDRATION -> R.string.metric_hydration
        DashboardWidgetId.CALORIES_IN -> R.string.screen_nutrition
        DashboardWidgetId.PROTEIN -> R.string.screen_nutrition
        DashboardWidgetId.CARBS -> R.string.screen_nutrition
        DashboardWidgetId.FAT -> R.string.screen_nutrition
        DashboardWidgetId.WEIGHT -> R.string.screen_body
        DashboardWidgetId.HEIGHT -> R.string.screen_body
        DashboardWidgetId.BMI -> R.string.screen_body
        DashboardWidgetId.FFMI -> R.string.screen_body
        DashboardWidgetId.BODY_FAT -> R.string.screen_body
        DashboardWidgetId.LEAN_MASS -> R.string.screen_body
        DashboardWidgetId.BMR -> R.string.screen_calories
        DashboardWidgetId.BONE_MASS -> R.string.screen_body
        DashboardWidgetId.BODY_WATER_MASS -> R.string.screen_body
        DashboardWidgetId.AVG_HEART_RATE -> R.string.metric_avg_heart_rate
        DashboardWidgetId.RESTING_HEART_RATE -> R.string.metric_resting_heart_rate
        DashboardWidgetId.HRV -> R.string.metric_hrv
        DashboardWidgetId.BLOOD_PRESSURE -> R.string.metric_blood_pressure
        DashboardWidgetId.SPO2 -> R.string.metric_spo2
        DashboardWidgetId.VO2_MAX -> R.string.metric_vo2_max
        DashboardWidgetId.RESPIRATORY_RATE -> R.string.metric_respiratory_rate
        DashboardWidgetId.BODY_TEMPERATURE -> R.string.metric_body_temp
        DashboardWidgetId.BLOOD_GLUCOSE -> R.string.metric_blood_glucose
        DashboardWidgetId.SKIN_TEMPERATURE -> R.string.metric_skin_temperature
        DashboardWidgetId.WEEKLY_CARDIO_LOAD -> R.string.metric_weekly_cardio_load
        DashboardWidgetId.CARDIO_LOAD -> R.string.metric_weekly_cardio_load
        DashboardWidgetId.MINDFULNESS -> R.string.metric_mindfulness
        DashboardWidgetId.CYCLE -> R.string.metric_cycle
    }

private data class TopBarEditState(
    val isEditing: Boolean = false,
    val onToggleEdit: () -> Unit = {},
)

private data class TopBarOutdoorModeState(
    val enabled: Boolean = false,
    val onToggle: () -> Unit = {},
)
