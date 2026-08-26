package tech.mmarca.openvitals.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.preferences.AppThemeMode
import tech.mmarca.openvitals.domain.model.BodyMeasurementType
import tech.mmarca.openvitals.domain.model.VitalsMeasurementType
import tech.mmarca.openvitals.features.manualentry.ManualEntryScreen
import tech.mmarca.openvitals.features.manualentry.ManualEntryViewModel
import tech.mmarca.openvitals.healthconnect.HealthConnectFeature
import tech.mmarca.openvitals.ui.components.WithHealthConnectFeatureScreen
import tech.mmarca.openvitals.features.manualentry.activity.ActivityEntryScreen
import tech.mmarca.openvitals.features.manualentry.activity.ActivityEntryViewModel
import tech.mmarca.openvitals.features.manualentry.body.BodyMeasurementEntryScreen
import tech.mmarca.openvitals.features.manualentry.body.BodyMeasurementEntryViewModel
import tech.mmarca.openvitals.features.manualentry.cycle.CycleEntryScreen
import tech.mmarca.openvitals.features.manualentry.cycle.CycleEntryViewModel
import tech.mmarca.openvitals.features.manualentry.hydration.HydrationEntryScreen
import tech.mmarca.openvitals.features.manualentry.hydration.HydrationEntryViewModel
import tech.mmarca.openvitals.features.manualentry.mindfulness.MindfulnessEntryScreen
import tech.mmarca.openvitals.features.manualentry.mindfulness.MindfulnessEntryViewModel
import tech.mmarca.openvitals.features.manualentry.nutrition.CarbsEntryScreen
import tech.mmarca.openvitals.features.manualentry.nutrition.CarbsEntryViewModel
import tech.mmarca.openvitals.features.manualentry.vitals.VitalsMeasurementEntryScreen
import tech.mmarca.openvitals.features.manualentry.vitals.VitalsMeasurementEntryViewModel
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.mmarca.openvitals.features.workoutplans.WorkoutPlanBuilderScreen
import tech.mmarca.openvitals.features.workoutplans.WorkoutPlanListScreen

internal fun NavGraphBuilder.manualEntryRoutes(
    navController: NavHostController,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    appThemeMode: AppThemeMode,
    routeImportRequest: ExternalRouteImportRequest?,
    onRouteImportRequestHandled: (Long) -> Unit,
    onManualEntryEditStateChanged: (Boolean, () -> Unit) -> Unit,
    onActivityEntryTitleChanged: (Int?) -> Unit,
    onActivityEntryEditStateChanged: (Boolean, Boolean, () -> Unit) -> Unit,
    onActivityEntryFocusModeChanged: (Boolean) -> Unit,
    onActivityRecordingOutdoorModeStateChanged: (Boolean, Boolean, () -> Unit) -> Unit,
    onEntrySaved: () -> Unit,
    onEntrySavedAndPopBack: () -> Unit,
    onActivityEntrySaved: () -> Unit,
) {
    composable(Screen.ManualEntry.route) {
        val manualEntryViewModel = hiltViewModel<ManualEntryViewModel>()
        WithHealthConnectFeatureScreen(
            feature = HealthConnectFeature.MANUAL_ENTRY,
            showInlineSyncBanner = false,
        ) { _ ->
            ManualEntryScreen(
                viewModel = manualEntryViewModel,
                onOpenHydrationEntry = {
                    navController.navigate(Screen.HydrationEntry.route)
                },
                onOpenCarbsEntry = {
                    navController.navigate(Screen.CarbsEntry.route)
                },
                onOpenActivityEntry = {
                    navController.navigate(Screen.ActivityEntry.createRoute())
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
                onOpenCycleEntry = {
                    navController.navigate(Screen.CycleEntry.route)
                },
                onEditStateChanged = onManualEntryEditStateChanged,
                onOpenWorkoutPlans = {
                    navController.navigate(Screen.WorkoutPlans.route)
                },
            )
        }
    }

    composable(Screen.WorkoutPlans.route) {
        WorkoutPlanListScreen(
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            onOpenBuilder = { planId ->
                navController.navigate(Screen.WorkoutPlanBuilder.createRoute(planId))
            },
            onStartPlan = { planId ->
                navController.navigate(
                    Screen.ActivityEntry.createRoute(mode = Screen.ActivityEntryMode.RECORD, planId = planId),
                )
            },
            onLogPlan = { planId ->
                navController.navigate(Screen.ActivityEntry.createRoute(planId = planId))
            },
        )
    }

    composable(
        route = Screen.WorkoutPlanBuilder.route,
        arguments = listOf(
            navArgument(WORKOUT_PLAN_ID_ARG) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
        ),
    ) {
        WorkoutPlanBuilderScreen(
            onSaved = { savedId ->
                navController.previousBackStackEntry?.savedStateHandle?.set(WORKOUT_PLAN_SAVED_RESULT, savedId)
                navController.popBackStack()
            },
            onClose = { navController.popBackStack() },
        )
    }

    composable(Screen.HydrationEntry.route) {
        val hydrationViewModel = hiltViewModel<HydrationEntryViewModel>()
        HydrationEntryScreen(
            viewModel = hydrationViewModel,
            unitFormatter = unitFormatter,
            onEntrySaved = onEntrySaved,
        )
    }

    composable(
        route = Screen.HydrationEntryLogDrink.route,
        arguments = listOf(navArgument(HYDRATION_DRINK_ID_ARG) { type = NavType.StringType }),
    ) { backStackEntry ->
        val hydrationViewModel = hiltViewModel<HydrationEntryViewModel>()
        HydrationEntryScreen(
            viewModel = hydrationViewModel,
            unitFormatter = unitFormatter,
            initialLogDrinkId = backStackEntry.arguments?.getString(HYDRATION_DRINK_ID_ARG),
            onEntrySaved = onEntrySavedAndPopBack,
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
            onEntrySaved = onEntrySavedAndPopBack,
        )
    }

    composable(Screen.CarbsEntry.route) {
        val carbsEntryViewModel = hiltViewModel<CarbsEntryViewModel>()
        CarbsEntryScreen(
            viewModel = carbsEntryViewModel,
            unitFormatter = unitFormatter,
            onEntrySaved = onEntrySaved,
        )
    }

    composable(
        route = Screen.ActivityEntry.route,
        arguments = listOf(
            navArgument(ACTIVITY_ENTRY_MODE_ARG) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
            navArgument(ACTIVITY_ENTRY_PLAN_ID_ARG) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
            navArgument(ACTIVITY_ENTRY_TYPE_ARG) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
        ),
    ) { backStackEntry ->
        val activityEntryViewModel = hiltViewModel<ActivityEntryViewModel>()
        val savedWorkoutPlanId by backStackEntry.savedStateHandle
            .getStateFlow<String?>(WORKOUT_PLAN_SAVED_RESULT, null)
            .collectAsStateWithLifecycle()
        ActivityEntryScreen(
            viewModel = activityEntryViewModel,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            savedWorkoutPlanId = savedWorkoutPlanId,
            onSavedWorkoutPlanHandled = { backStackEntry.savedStateHandle[WORKOUT_PLAN_SAVED_RESULT] = null },
            onOpenWorkoutPlans = { navController.navigate(Screen.WorkoutPlans.route) },
            onOpenWorkoutPlanBuilder = { planId ->
                navController.navigate(Screen.WorkoutPlanBuilder.createRoute(planId))
            },
            pendingRouteImportUri = routeImportRequest?.uri,
            pendingRouteImportRequestId = routeImportRequest?.id,
            onPendingRouteImportHandled = onRouteImportRequestHandled,
            onEntrySaved = onActivityEntrySaved,
            onActivityRecordingTitleChanged = onActivityEntryTitleChanged,
            onActivityRecordingEditStateChanged = onActivityEntryEditStateChanged,
            onActivityRecordingFocusModeChanged = onActivityEntryFocusModeChanged,
            onActivityRecordingOutdoorModeStateChanged = onActivityRecordingOutdoorModeStateChanged,
            appThemeMode = appThemeMode,
        )
    }

    composable(
        route = Screen.ActivityEntryEdit.route,
        arguments = listOf(navArgument(ACTIVITY_ENTRY_ID_ARG) { type = NavType.StringType }),
    ) { backStackEntry ->
        val activityEntryViewModel = hiltViewModel<ActivityEntryViewModel>()
        val savedWorkoutPlanId by backStackEntry.savedStateHandle
            .getStateFlow<String?>(WORKOUT_PLAN_SAVED_RESULT, null)
            .collectAsStateWithLifecycle()
        ActivityEntryScreen(
            viewModel = activityEntryViewModel,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            savedWorkoutPlanId = savedWorkoutPlanId,
            onSavedWorkoutPlanHandled = { backStackEntry.savedStateHandle[WORKOUT_PLAN_SAVED_RESULT] = null },
            onOpenWorkoutPlans = { navController.navigate(Screen.WorkoutPlans.route) },
            onOpenWorkoutPlanBuilder = { planId ->
                navController.navigate(Screen.WorkoutPlanBuilder.createRoute(planId))
            },
            onEntrySaved = onEntrySavedAndPopBack,
            onActivityRecordingTitleChanged = onActivityEntryTitleChanged,
            onActivityRecordingEditStateChanged = onActivityEntryEditStateChanged,
            onActivityRecordingFocusModeChanged = onActivityEntryFocusModeChanged,
            onActivityRecordingOutdoorModeStateChanged = onActivityRecordingOutdoorModeStateChanged,
            appThemeMode = appThemeMode,
        )
    }

    composable(Screen.MindfulnessEntry.route) {
        val mindfulnessEntryViewModel = hiltViewModel<MindfulnessEntryViewModel>()
        MindfulnessEntryScreen(
            viewModel = mindfulnessEntryViewModel,
            onEntrySaved = onEntrySaved,
        )
    }

    composable(
        route = Screen.MindfulnessEntryEdit.route,
        arguments = listOf(navArgument(MINDFULNESS_ENTRY_ID_ARG) { type = NavType.StringType }),
    ) {
        val mindfulnessEntryViewModel = hiltViewModel<MindfulnessEntryViewModel>()
        MindfulnessEntryScreen(
            viewModel = mindfulnessEntryViewModel,
            onEntrySaved = onEntrySavedAndPopBack,
        )
    }

    composable(Screen.CycleEntry.route) {
        val cycleEntryViewModel = hiltViewModel<CycleEntryViewModel>()
        CycleEntryScreen(
            viewModel = cycleEntryViewModel,
            unitFormatter = unitFormatter,
            onEntrySaved = onEntrySaved,
        )
    }

    composable(
        route = Screen.CycleEntryEdit.route,
        arguments = listOf(
            navArgument(CYCLE_ENTRY_KIND_ARG) { type = NavType.StringType },
            navArgument(CYCLE_ENTRY_ID_ARG) { type = NavType.StringType },
        ),
    ) {
        val cycleEntryViewModel = hiltViewModel<CycleEntryViewModel>()
        CycleEntryScreen(
            viewModel = cycleEntryViewModel,
            unitFormatter = unitFormatter,
            onEntrySaved = onEntrySavedAndPopBack,
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
            onEntrySaved = onEntrySaved,
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
            onEntrySaved = onEntrySavedAndPopBack,
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
            onEntrySaved = onEntrySaved,
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
            onEntrySaved = onEntrySavedAndPopBack,
        )
    }
}
