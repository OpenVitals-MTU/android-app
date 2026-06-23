package tech.mmarca.openvitals.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.BodyMeasurementType
import tech.mmarca.openvitals.domain.model.VitalsMeasurementType
import tech.mmarca.openvitals.features.manualentry.ManualEntryScreen
import tech.mmarca.openvitals.features.manualentry.ManualEntryViewModel
import tech.mmarca.openvitals.features.manualentry.activity.ActivityEntryScreen
import tech.mmarca.openvitals.features.manualentry.activity.ActivityEntryViewModel
import tech.mmarca.openvitals.features.manualentry.body.BodyMeasurementEntryScreen
import tech.mmarca.openvitals.features.manualentry.body.BodyMeasurementEntryViewModel
import tech.mmarca.openvitals.features.manualentry.hydration.HydrationEntryScreen
import tech.mmarca.openvitals.features.manualentry.hydration.HydrationEntryViewModel
import tech.mmarca.openvitals.features.manualentry.mindfulness.MindfulnessEntryScreen
import tech.mmarca.openvitals.features.manualentry.mindfulness.MindfulnessEntryViewModel
import tech.mmarca.openvitals.features.manualentry.vitals.VitalsMeasurementEntryScreen
import tech.mmarca.openvitals.features.manualentry.vitals.VitalsMeasurementEntryViewModel

internal fun NavGraphBuilder.manualEntryRoutes(
    navController: NavHostController,
    unitFormatter: UnitFormatter,
    routeImportRequest: ExternalRouteImportRequest?,
    onRouteImportRequestHandled: (Long) -> Unit,
    onManualEntryEditStateChanged: (Boolean, () -> Unit) -> Unit,
    onEntrySaved: () -> Unit,
    onEntrySavedAndPopBack: () -> Unit,
    onActivityEntrySaved: () -> Unit,
) {
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
            onEditStateChanged = onManualEntryEditStateChanged,
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

    composable(Screen.ActivityEntry.route) {
        val activityEntryViewModel = hiltViewModel<ActivityEntryViewModel>()
        ActivityEntryScreen(
            viewModel = activityEntryViewModel,
            unitFormatter = unitFormatter,
            pendingRouteImportUri = routeImportRequest?.uri,
            pendingRouteImportRequestId = routeImportRequest?.id,
            onPendingRouteImportHandled = onRouteImportRequestHandled,
            onEntrySaved = onActivityEntrySaved,
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
            onEntrySaved = onEntrySavedAndPopBack,
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
