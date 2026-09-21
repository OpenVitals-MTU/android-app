package tech.mmarca.openvitals.features.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.diagnostics.CrashReportEmailActivity
import tech.mmarca.openvitals.core.diagnostics.PrivacySafeDebugLogExporter
import tech.mmarca.openvitals.core.diagnostics.shareDebugDiagnosticsLog
import tech.mmarca.openvitals.healthconnect.openHealthConnectPermissionSettings
import tech.mmarca.openvitals.ui.components.FullScreenLoading

/**
 * The Settings root and the sections that share [SettingsViewModel]: Health Connect, Vitals
 * and Diagnostics. Every other section has a screen and a ViewModel of its own.
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    section: SettingsSection? = null,
    onOpenSection: (SettingsSection) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val unableToOpenPermissions = stringResource(R.string.onboarding_unable_open_permissions)
    val debugLogsSaved = stringResource(R.string.settings_debug_logs_saved)
    val debugLogsSaveFailed = stringResource(R.string.settings_debug_logs_save_failed)
    val debugLogsShareFailed = stringResource(R.string.settings_debug_logs_share_failed)
    val privacyPolicyUrl = stringResource(R.string.settings_privacy_policy_url)
    val discussionUrl = stringResource(R.string.settings_support_discussion_url)
    val supportUrl = stringResource(R.string.settings_support_url)
    val openManualPermissionSettings = {
        if (!openHealthConnectPermissionSettings(context)) {
            Toast.makeText(
                context,
                unableToOpenPermissions,
                Toast.LENGTH_SHORT,
            ).show()
        }
    }
    fun openExternalUrl(url: String) {
        runCatching {
            context.startActivity(
                android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse(url),
                ),
            )
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
            openExternalUrl(privacyPolicyUrl)
        },
        onOpenIssues = {
            context.startActivity(CrashReportEmailActivity.createIssueReportIntent(context))
        },
        onOpenDiscussion = {
            openExternalUrl(discussionUrl)
        },
        onOpenSupport = {
            openExternalUrl(supportUrl)
        },
        onSaveDebugLogs = {
            debugLogSaver.launch("openvitals-diagnostics-logs.txt")
        },
        onShareDebugLogs = {
            coroutineScope.launch {
                runCatching {
                    context.shareDebugDiagnosticsLog()
                }.onFailure {
                    Toast.makeText(context, debugLogsShareFailed, Toast.LENGTH_SHORT).show()
                }
            }
        },
        onOpenManualPermissionSettings = openManualPermissionSettings,
        onGrantPermissions = requestAllPermissions::launch,
    )

    SettingsSectionList {
        settingsScreenContent(
            section = section,
            state = state,
            viewModel = viewModel,
            actions = actions,
        )
    }
}

/** The column every settings section renders into: full width on a phone, capped on a tablet. */
@Composable
internal fun SettingsSectionList(content: LazyListScope.() -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 920.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
            content = content,
        )
    }
}
