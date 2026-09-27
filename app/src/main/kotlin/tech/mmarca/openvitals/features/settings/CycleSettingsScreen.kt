package tech.mmarca.openvitals.features.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.performance.offMainIo
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.features.cycle.CycleJournalExport
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.readBytesBounded
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.theme.LayoutMetrics

/** Cycle: the declared contexts, the age band, the reminders, and what the estimates are. */
@Composable
fun CycleSettingsScreen(
    viewModel: CycleSettingsViewModel,
    dateTimeFormatterProvider: DateTimeFormatterProvider = DateTimeFormatterProvider(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.onNotificationPermissionResult(granted)
    }

    val context = LocalContext.current
    val fileScope = rememberCoroutineScope()
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(CycleJournalExport.MimeType)) { uri ->
        if (uri != null) {
            fileScope.launch {
                val text = viewModel.exportJson()
                offMainIo {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
                }.onSuccess { viewModel.onExported() }
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            fileScope.launch {
                val text = offMainIo {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        input.readBytesBounded(CycleJournalExport.ImportMaxBytes, "Journal file too large").decodeToString()
                    }
                }.getOrNull()
                // A file that is too large or unreadable fails like one that holds no journal.
                viewModel.importJson(text.orEmpty())
            }
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    val gutter = Modifier.padding(horizontal = LayoutMetrics.screenGutter)
    SettingsSectionList {
        item { SectionHeader(stringResource(SettingsSection.CYCLE.titleRes)) }
        item {
            CycleContextsCard(
                profile = state.profile,
                onToggleContext = viewModel::setContext,
                modifier = gutter,
            )
        }
        item { SettingsCardSpacer() }
        item {
            CycleAgeBandCard(
                selected = state.profile.ageBand,
                derived = state.derivedAgeBand,
                onSelect = viewModel::setAgeBand,
                modifier = gutter,
            )
        }
        item { SettingsCardSpacer() }
        item {
            CycleRemindersCard(
                state = state,
                viewModel = viewModel,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                onRequestNotificationPermission = {
                    viewModel.requestEnableAfterPermission()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.onNotificationPermissionResult(granted = true)
                    }
                },
                modifier = gutter,
            )
        }
        item { SettingsCardSpacer() }
        item {
            CycleJournalBackupCard(
                message = state.backupMessage,
                importedDays = state.importedDays,
                onExport = { exportLauncher.launch(CycleJournalExport.FileName) },
                onImport = { importLauncher.launch(arrayOf(CycleJournalExport.MimeType, "text/plain", "*/*")) },
                modifier = gutter,
            )
        }
        item { SettingsCardSpacer() }
        item { CycleAboutEstimatesCard(modifier = gutter) }
        item { SettingsCardSpacer() }
        item {
            CycleDeleteJournalCard(
                isDeleting = state.isDeletingJournal,
                onDelete = viewModel::deleteCycleJournal,
                modifier = gutter,
            )
        }
    }
}
