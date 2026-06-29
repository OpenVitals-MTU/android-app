package tech.mmarca.openvitals.features.settings

import android.content.ClipData
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.diagnostics.PrivacySafeDebugLogExporter
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.healthconnect.openHealthConnectPermissionSettings
import tech.mmarca.openvitals.ui.components.FullScreenLoading

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    section: SettingsSection? = null,
    onOpenSection: (SettingsSection) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    val unableToOpenPermissions = stringResource(R.string.onboarding_unable_open_permissions)
    val reportCopied = stringResource(R.string.settings_apple_health_import_report_copied)
    val reportSaved = stringResource(R.string.settings_apple_health_import_report_saved)
    val reportSaveFailed = stringResource(R.string.settings_apple_health_import_report_save_failed)
    val debugLogsSaved = stringResource(R.string.settings_debug_logs_saved)
    val debugLogsSaveFailed = stringResource(R.string.settings_debug_logs_save_failed)
    val cacheClearedMessage = stringResource(R.string.settings_clear_local_cache_done)
    val privacyPolicyUrl = stringResource(R.string.settings_privacy_policy_url)
    val openManualPermissionSettings = {
        if (!openHealthConnectPermissionSettings(context)) {
            Toast.makeText(
                context,
                unableToOpenPermissions,
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
    }

    val requestAllPermissions = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        viewModel.onPermissionsResult(granted)
    }

    val requestCyclePermissions = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        viewModel.onPermissionsResult(granted)
    }

    val requestDataImportPermissions = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        viewModel.onPermissionsResult(granted)
    }

    val appleHealthExportPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            viewModel.importAppleHealthExport(uri)
        }
    }

    val offlineMapPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            viewModel.importOfflineMap(uri)
        }
    }

    val appleHealthReportSaver = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri ->
        if (uri != null) {
            runCatching {
                val reportText = state.appleHealthImportResult?.shareableReportText.orEmpty()
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(reportText.toByteArray())
                } ?: error("Unable to open destination.")
            }.fold(
                onSuccess = {
                    Toast.makeText(context, reportSaved, Toast.LENGTH_SHORT).show()
                },
                onFailure = {
                    Toast.makeText(context, reportSaveFailed, Toast.LENGTH_SHORT).show()
                },
            )
        }
    }

    val debugLogSaver = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        PrivacySafeDebugLogExporter.writeCurrentProcessLogcat(context, output)
                    } ?: error("Unable to open destination.")
                }.fold(
                    onSuccess = {
                        Toast.makeText(context, debugLogsSaved, Toast.LENGTH_SHORT).show()
                    },
                    onFailure = {
                        Toast.makeText(context, debugLogsSaveFailed, Toast.LENGTH_SHORT).show()
                    },
                )
            }
        }
    }

    if (state.isLoading) {
        FullScreenLoading()
        return
    }

    val actions = SettingsScreenActions(
        onOpenSection = onOpenSection,
        onOpenPrivacyPolicy = {
            runCatching {
                context.startActivity(
                    android.content.Intent(
                        android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse(privacyPolicyUrl),
                    ),
                )
            }
        },
        onGrantCyclePermissions = {
            if (state.availability == HealthConnectAvailability.AVAILABLE) {
                requestCyclePermissions.launch(state.cyclePermissions - state.grantedPermissions)
            }
        },
        onGrantDataImportPermissions = {
            requestDataImportPermissions.launch(state.missingDataImportWritePermissions)
        },
        onImportAppleHealth = {
            appleHealthExportPicker.launch(AppleHealthExportMimeTypes)
        },
        onImportOfflineMap = {
            offlineMapPicker.launch(OfflineMapMimeTypes)
        },
        onCopyAppleHealthReport = { reportText ->
            coroutineScope.launch {
                clipboard.setClipEntry(
                    ClipData.newPlainText("OpenVitals", reportText).toClipEntry()
                )
                Toast.makeText(context, reportCopied, Toast.LENGTH_SHORT).show()
            }
        },
        onSaveAppleHealthReport = {
            appleHealthReportSaver.launch("openvitals-apple-health-import-report.txt")
        },
        onSaveDebugLogs = {
            debugLogSaver.launch("openvitals-debug-logs.txt")
        },
        onOpenManualPermissionSettings = openManualPermissionSettings,
        onGrantPermissions = requestAllPermissions::launch,
        onClearCache = {
            viewModel.clearCachedSummaries()
            Toast.makeText(
                context,
                cacheClearedMessage,
                Toast.LENGTH_SHORT,
            ).show()
        },
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 920.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            settingsScreenContent(
                section = section,
                state = state,
                viewModel = viewModel,
                actions = actions,
            )
        }
    }
}
