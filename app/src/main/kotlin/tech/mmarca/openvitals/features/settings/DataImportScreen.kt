package tech.mmarca.openvitals.features.settings

import android.content.ClipData
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.stringResource
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.performance.offMainIo
import tech.mmarca.openvitals.features.imports.applehealth.AppleHealthImportCategory
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.FitImportMimeTypes
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.RouteImportMimeTypes
import tech.mmarca.openvitals.ui.components.ConfirmLeaveWhileImporting
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.theme.LayoutMetrics

/**
 * Data transfer: the Apple Health export, the bulk route and FIT imports, and the way
 * on to the CSV import and the report builder.
 *
 * Its own screen and ViewModel, as Watches and Sync with another phone already are.
 */
@Composable
fun DataImportScreen(
    viewModel: DataImportViewModel,
    onImportRouteFileSelected: (android.net.Uri) -> Unit = {},
    onImportFitFileSelected: (android.net.Uri) -> Unit = {},
    onRouteFilesImported: () -> Unit = {},
    onOpenCsvImport: () -> Unit = {},
    onOpenReportExport: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    val reportCopied = stringResource(R.string.settings_apple_health_import_report_copied)
    val errorCopied = stringResource(R.string.settings_apple_health_import_error_copied)
    val reportSaved = stringResource(R.string.settings_apple_health_import_report_saved)
    val reportSaveFailed = stringResource(R.string.settings_apple_health_import_report_save_failed)

    // A bulk import that is still running must not be walked away from by accident.
    ConfirmLeaveWhileImporting(importing = state.isImportingRouteFiles || state.isScanningFitFolder)

    fun copyImportText(text: String, message: String) {
        coroutineScope.launch {
            clipboard.setClipEntry(ClipData.newPlainText("OpenVitals", text).toClipEntry())
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    // Permissions are granted in Health Connect, so they are re-read on the way back.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    LaunchedEffect(state.routeImportResult) {
        if ((state.routeImportResult?.importedFiles ?: 0) > 0) {
            onRouteFilesImported()
        }
    }

    val requestDataImportPermissions = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) { viewModel.onPermissionsResult() }

    val requestRouteImportPermissions = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) { viewModel.onPermissionsResult() }

    val appleHealthExportPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(viewModel::analyzeAppleHealthExport) }

    val fitFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(onImportFitFileSelected) }

    // A folder: `OpenDocumentTree` grants the whole tree, walked for FIT files.
    val fitFolderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { treeUri ->
        // Null is the user backing out.
        treeUri?.let(viewModel::importFitFolder)
    }

    val routeFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(onImportRouteFileSelected) }

    val routeFilesPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris -> if (uris.isNotEmpty()) viewModel.importRouteFiles(uris) }

    val reportSaver = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri ->
        if (uri != null) {
            val reportText = state.appleHealthImportResult?.shareableReportText
                ?: state.appleHealthImportError.orEmpty()
            coroutineScope.launch {
                offMainIo {
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        output.write(reportText.toByteArray())
                    } ?: error("Unable to open destination.")
                }.fold(
                    onSuccess = { Toast.makeText(context, reportSaved, Toast.LENGTH_SHORT).show() },
                    onFailure = { Toast.makeText(context, reportSaveFailed, Toast.LENGTH_SHORT).show() },
                )
            }
        }
    }

    val actions = DataImportActions(
        onGrantDataImportPermissions = {
            requestDataImportPermissions.launch(state.missingDataImportWritePermissions)
        },
        onGrantRouteImportPermissions = {
            requestRouteImportPermissions.launch(state.missingRouteImportWritePermissions)
        },
        onPickAppleHealthExport = { appleHealthExportPicker.launch(AppleHealthExportMimeTypes) },
        onToggleAppleHealthCategory = viewModel::setAppleHealthImportCategorySelected,
        onImportSelectedAppleHealth = viewModel::importSelectedAppleHealthExport,
        onCopyReport = { reportText -> copyImportText(reportText, reportCopied) },
        onCopyError = { errorText -> copyImportText(errorText, errorCopied) },
        onSaveReport = { reportSaver.launch("openvitals-apple-health-import-report.txt") },
        onPickRouteFile = { routeFilePicker.launch(RouteImportMimeTypes) },
        onPickRouteFiles = { routeFilesPicker.launch(RouteImportMimeTypes) },
        onPickFitFile = { fitFilePicker.launch(FitImportMimeTypes) },
        onPickFitFolder = { fitFolderPicker.launch(null) },
        onOpenCsvImport = onOpenCsvImport,
        onOpenReportExport = onOpenReportExport,
    )

    LazyColumn(contentPadding = PaddingValues(vertical = LayoutMetrics.screenGutter)) {
        dataImportCards(state, actions)
    }
}

/** What the Data transfer cards can ask for. Same shape as [SettingsScreenActions]. */
@Immutable
data class DataImportActions(
    val onGrantDataImportPermissions: () -> Unit,
    val onGrantRouteImportPermissions: () -> Unit,
    val onPickAppleHealthExport: () -> Unit,
    val onToggleAppleHealthCategory: (AppleHealthImportCategory, Boolean) -> Unit,
    val onImportSelectedAppleHealth: () -> Unit,
    val onCopyReport: (String) -> Unit,
    val onCopyError: (String) -> Unit,
    val onSaveReport: () -> Unit,
    val onPickRouteFile: () -> Unit,
    val onPickRouteFiles: () -> Unit,
    val onPickFitFile: () -> Unit,
    val onPickFitFolder: () -> Unit,
    val onOpenCsvImport: () -> Unit,
    val onOpenReportExport: () -> Unit,
)

private fun LazyListScope.dataImportCards(
    state: DataImportUiState,
    actions: DataImportActions,
) {
    item { SectionHeader(stringResource(SettingsSection.DATA_IMPORT.titleRes)) }
    item {
        AppleHealthImportCard(
            availability = state.availability,
            importPermissions = state.dataImportWritePermissions,
            grantedPermissions = state.grantedPermissions,
            isAnalyzing = state.isAnalyzingAppleHealth,
            isImporting = state.isImportingAppleHealth,
            analysisProgress = state.appleHealthAnalysisProgress,
            analysis = state.appleHealthImportAnalysis,
            selectedCategories = state.selectedAppleHealthImportCategories,
            progress = state.appleHealthImportProgress,
            result = state.appleHealthImportResult,
            error = state.appleHealthImportError,
            permissionDenied = state.appleHealthImportPermissionDenied,
            onGrantPermissions = {
                actions.onGrantDataImportPermissions()
            },
            onImport = { actions.onPickAppleHealthExport() },
            onToggleCategory = actions.onToggleAppleHealthCategory,
            onImportSelected = actions.onImportSelectedAppleHealth,
            onCopyReport = actions.onCopyReport,
            onCopyError = actions.onCopyError,
            onSaveReport = actions.onSaveReport,
            modifier = Modifier.padding(horizontal = LayoutMetrics.screenGutter),
        )
    }
    item { SettingsCardSpacer() }
    item {
        RouteImportCard(
            availability = state.availability,
            importPermissions = state.routeImportWritePermissions,
            grantedPermissions = state.grantedPermissions,
            // One bulk importer serves both cards; each shows only its own run.
            isImporting = state.isImportingRouteFiles,
            progress = state.routeImportProgress.takeIf { state.routeImportSource == RouteBulkImportSource.ROUTE_FILES },
            result = state.routeImportResult.takeIf { state.routeImportSource == RouteBulkImportSource.ROUTE_FILES },
            error = state.routeImportError.takeIf { state.routeImportSource == RouteBulkImportSource.ROUTE_FILES },
            onGrantPermissions = {
                actions.onGrantRouteImportPermissions()
            },
            onImportSingle = actions.onPickRouteFile,
            onImportBulk = actions.onPickRouteFiles,
            modifier = Modifier.padding(horizontal = LayoutMetrics.screenGutter),
        )
    }
    item { SettingsCardSpacer() }
    item {
        FitImportCard(
            availability = state.availability,
            importPermissions = state.routeImportWritePermissions,
            grantedPermissions = state.grantedPermissions,
            isScanning = state.isScanningFitFolder,
            folderHadNoFitFiles = state.fitFolderHadNoFitFiles,
            truncatedAt = state.fitFolderTruncatedAt,
            scanError = state.fitFolderScanError,
            isImporting = state.isImportingRouteFiles,
            progress = state.routeImportProgress.takeIf { state.routeImportSource == RouteBulkImportSource.FIT_FOLDER },
            result = state.routeImportResult.takeIf { state.routeImportSource == RouteBulkImportSource.FIT_FOLDER },
            error = state.routeImportError.takeIf { state.routeImportSource == RouteBulkImportSource.FIT_FOLDER },
            onGrantPermissions = {
                actions.onGrantRouteImportPermissions()
            },
            onImport = actions.onPickFitFile,
            onImportFolder = actions.onPickFitFolder,
            modifier = Modifier.padding(horizontal = LayoutMetrics.screenGutter),
        )
    }
    item { SettingsCardSpacer() }
    item {
        CsvImportCard(
            onOpenCsvImport = actions.onOpenCsvImport,
            modifier = Modifier.padding(horizontal = LayoutMetrics.screenGutter),
        )
    }
    item { SettingsCardSpacer() }
    item {
        ReportExportCard(
            onOpenReportExport = actions.onOpenReportExport,
            modifier = Modifier.padding(horizontal = LayoutMetrics.screenGutter),
        )
    }
}
