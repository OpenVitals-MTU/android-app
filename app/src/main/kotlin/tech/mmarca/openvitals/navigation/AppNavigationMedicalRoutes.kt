package tech.mmarca.openvitals.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.features.imports.medical.MedicalImportScreen
import tech.mmarca.openvitals.features.medical.MedicalCategoryScreen
import tech.mmarca.openvitals.features.medical.MedicalDocumentsScreen
import tech.mmarca.openvitals.features.medical.MedicalRecordDetailScreen
import tech.mmarca.openvitals.features.medical.MedicalRecordEntryScreen
import tech.mmarca.openvitals.features.medical.MedicalRecordsHomeScreen
import tech.mmarca.openvitals.features.medical.MedicalSourcesScreen

/** The medical records area: the home the tile opens, a category's list, one record, this app's sources, and the import. */
internal fun NavGraphBuilder.medicalRecordsRoutes(
    navController: NavHostController,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    composable(Screen.MedicalRecords.route) {
        MedicalRecordsHomeScreen(
            viewModel = hiltViewModel(),
            exportViewModel = hiltViewModel(),
            onOpenCategory = { category ->
                navController.navigate(Screen.MedicalRecordCategory.createRoute(category)) { launchSingleTop = true }
            },
            onOpenImport = { navController.navigate(Screen.SettingsMedicalImport.createRoute()) { launchSingleTop = true } },
            onOpenSources = { navController.navigate(Screen.MedicalSources.route) { launchSingleTop = true } },
            onOpenDocuments = { navController.navigate(Screen.MedicalDocuments.route) { launchSingleTop = true } },
            onAddRecord = { kind -> navController.navigate(Screen.MedicalRecordEntry.createRoute(kind)) { launchSingleTop = true } },
        )
    }

    composable(
        route = Screen.MedicalRecordEntry.routePattern,
        arguments = listOf(
            navArgument(MEDICAL_ENTRY_KIND_ARG) { type = NavType.StringType },
            navArgument(MEDICAL_ENTRY_ID_ARG) { type = NavType.StringType; nullable = true; defaultValue = null },
        ),
    ) {
        MedicalRecordEntryScreen(viewModel = hiltViewModel(), onDone = { navController.popBackStack() })
    }

    composable(Screen.MedicalSources.route) {
        MedicalSourcesScreen(viewModel = hiltViewModel())
    }

    composable(Screen.MedicalDocuments.route) {
        MedicalDocumentsScreen(viewModel = hiltViewModel())
    }

    // Reached from the records home and from Settings, Import & export.
    composable(
        route = Screen.SettingsMedicalImport.routePattern,
        arguments = listOf(navArgument(MEDICAL_IMPORT_URI_ARG) { type = NavType.StringType; nullable = true; defaultValue = null }),
    ) { entry ->
        MedicalImportScreen(
            onDone = { navController.popBackStack() },
            initialUri = entry.arguments?.getString(MEDICAL_IMPORT_URI_ARG),
        )
    }

    composable(
        route = Screen.MedicalRecordCategory.route,
        arguments = listOf(navArgument(MEDICAL_CATEGORY_ARG) { type = NavType.StringType }),
    ) {
        MedicalCategoryScreen(
            viewModel = hiltViewModel(),
            exportViewModel = hiltViewModel(),
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            onOpenRecord = { ref -> navController.navigate(Screen.MedicalRecordDetail.createRoute(ref)) },
            onAddRecord = { kind -> navController.navigate(Screen.MedicalRecordEntry.createRoute(kind)) { launchSingleTop = true } },
        )
    }

    composable(
        route = Screen.MedicalRecordDetail.route,
        arguments = listOf(MEDICAL_SOURCE_ARG, MEDICAL_TYPE_ARG, MEDICAL_ID_ARG).map { name ->
            navArgument(name) { type = NavType.StringType }
        },
    ) {
        MedicalRecordDetailScreen(
            viewModel = hiltViewModel(),
            exportViewModel = hiltViewModel(),
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            onDeleted = { navController.popBackStack() },
            onEdit = { kind, id -> navController.navigate(Screen.MedicalRecordEntry.createRoute(kind, id)) { launchSingleTop = true } },
        )
    }
}
