package tech.mmarca.openvitals.features.devicesync

import android.content.ClipData
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.features.devicesync.protocol.SyncPhase
import tech.mmarca.openvitals.ui.components.OpenVitalsOutlinedButton
import tech.mmarca.openvitals.ui.theme.Spacing

/** Step 4 — how far back to sync. */
@Composable
internal fun DeviceSyncRangeStep(
    state: DeviceSyncState,
    onSetRange: (SyncRange) -> Unit,
    onNext: () -> Unit,
) {
    LazyColumn {
        item {
            DeviceSyncHero(
                icon = Icons.Outlined.History,
                title = stringResource(R.string.device_sync_range_heading),
                body = stringResource(R.string.device_sync_range_body),
            )
        }
        items(SyncRange.entries) { range ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = state.range == range, onClick = { onSetRange(range) })
                Text(
                    text = stringResource(range.labelRes()),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        item {
            DeviceSyncBottomButton(
                label = stringResource(R.string.device_sync_next),
                onClick = onNext,
            )
        }
    }
}

private fun SyncRange.labelRes(): Int = when (this) {
    SyncRange.DAYS_30 -> R.string.device_sync_range_30
    SyncRange.MONTHS_6 -> R.string.device_sync_range_6mo
    SyncRange.YEAR_1 -> R.string.device_sync_range_1y
    SyncRange.ALL -> R.string.device_sync_range_all
}

/** Step 5 — which record categories to sync. */
@Composable
internal fun DeviceSyncTypesStep(
    state: DeviceSyncState,
    onToggleType: (String) -> Unit,
    onStartSync: () -> Unit,
) {
    LazyColumn {
        item {
            DeviceSyncHero(
                icon = Icons.Outlined.Checklist,
                title = stringResource(R.string.device_sync_types_heading),
                body = stringResource(R.string.device_sync_types_body),
            )
        }
        // Only categories with at least one type this device supports.
        items(
            DeviceSyncCategory.entries.filter { category ->
                category.types.any { it in state.availableTypes }
            },
        ) { category ->
            val types = category.types.filter { it in state.availableTypes }
            val allOn = types.all { it in state.selectedTypes }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = allOn,
                    onCheckedChange = {
                        types.forEach { type ->
                            if (allOn == (type in state.selectedTypes)) onToggleType(type)
                        }
                    },
                )
                Text(
                    text = stringResource(category.labelRes()),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        item {
            DeviceSyncBottomButton(
                label = stringResource(
                    R.string.device_sync_start_sync,
                    state.selectedTypes.size,
                ),
                enabled = state.selectedTypes.isNotEmpty(),
                onClick = onStartSync,
            )
        }
        state.error?.let { error ->
            item { DeviceSyncBanner(deviceSyncErrorText(error), isError = true) }
        }
    }
}

private fun DeviceSyncCategory.labelRes(): Int = when (this) {
    DeviceSyncCategory.ACTIVITY -> R.string.device_sync_category_activity
    DeviceSyncCategory.WORKOUTS -> R.string.device_sync_category_workouts
    DeviceSyncCategory.HEART -> R.string.device_sync_category_heart
    DeviceSyncCategory.SLEEP -> R.string.device_sync_category_sleep
    DeviceSyncCategory.BODY -> R.string.device_sync_category_body
    DeviceSyncCategory.VITALS -> R.string.device_sync_category_vitals
    DeviceSyncCategory.NUTRITION -> R.string.device_sync_category_nutrition
    DeviceSyncCategory.HYDRATION -> R.string.device_sync_category_hydration
    DeviceSyncCategory.MINDFULNESS -> R.string.device_sync_category_mindfulness
    DeviceSyncCategory.CYCLE -> R.string.device_sync_category_cycle
}

/** Step 6 — the live transfer. */
@Composable
internal fun DeviceSyncProgressStep(state: DeviceSyncState, onCancel: () -> Unit) {
    val progress = state.progress
    val phaseLabel = stringResource(
        when (progress?.phase) {
            SyncPhase.WRITING, SyncPhase.COMPLETE -> R.string.device_sync_phase_writing
            SyncPhase.EXCHANGING -> R.string.device_sync_phase_exchanging
            else -> R.string.device_sync_phase_handshake
        },
    )
    LazyColumn {
        item {
            DeviceSyncHero(
                icon = Icons.Outlined.Sync,
                title = stringResource(R.string.device_sync_progress_heading),
                body = phaseLabel,
            )
        }
        item {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
            )
        }
        item {
            DeviceSyncStatRow(
                stringResource(R.string.device_sync_sent),
                progress?.itemsSent ?: 0,
            )
        }
        item {
            DeviceSyncStatRow(
                stringResource(R.string.device_sync_received),
                progress?.itemsReceived ?: 0,
            )
        }
        item {
            DeviceSyncStatRow(
                stringResource(R.string.device_sync_written),
                progress?.itemsWritten ?: 0,
            )
        }
        item { DeviceSyncCancelButton(onCancel) }
    }
}

/** Step 7 — the outcome: a merged report, or the failure and a way out. */
@Composable
internal fun DeviceSyncReportStep(state: DeviceSyncState, onDone: () -> Unit) {
    val report = state.report
    // A failed (report == null) or aborted (present but not completed) session
    // must NOT render the success checkmark + "merged N" — show the failure.
    if (report == null || !report.completed) {
        LazyColumn {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(56.dp),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.device_sync_error_heading),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.error?.let { deviceSyncErrorText(it) }
                            ?: stringResource(R.string.device_sync_aborted),
                        textAlign = TextAlign.Center,
                    )
                    // The abort reason is a technical artifact (English by
                    // design, like the report file) but it is the ONE line that
                    // tells a bug report apart from a shrug — show it.
                    report?.abortReason?.let { reason ->
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        Text(
                            text = stringResource(R.string.device_sync_abort_reason, reason),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            // Partial progress: how far it got before dying is itself
            // diagnostic ("12,000 records then stopped" vs "nothing at all").
            if (report != null) {
                item {
                    DeviceSyncStatRow(stringResource(R.string.device_sync_sent), report.itemsSent)
                }
                item {
                    DeviceSyncStatRow(
                        stringResource(R.string.device_sync_received),
                        report.itemsReceived,
                    )
                }
                item {
                    DeviceSyncStatRow(stringResource(R.string.device_sync_imported), report.imported)
                }
            }
            if (state.reportText.isNotEmpty()) {
                item { DeviceSyncReportActions(state.reportText) }
            }
            item {
                DeviceSyncBottomButton(
                    label = stringResource(R.string.device_sync_done),
                    onClick = onDone,
                )
            }
        }
        return
    }

    LazyColumn {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Icons.Outlined.TaskAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.device_sync_report_heading, report.imported),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
            }
        }
        item {
            DeviceSyncStatRow(stringResource(R.string.device_sync_imported), report.imported)
        }
        item {
            DeviceSyncStatRow(
                stringResource(R.string.device_sync_duplicates),
                report.duplicateSkipped,
            )
        }
        items(report.typeSummaries) { summary ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = summary.recordType,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "+${summary.imported}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (state.reportText.isNotEmpty()) {
            item { DeviceSyncReportActions(state.reportText) }
        }
        item {
            DeviceSyncBottomButton(
                label = stringResource(R.string.device_sync_done),
                onClick = onDone,
            )
        }
    }
}

/**
 * Copy/Share for a sync report's text. Shared by the success report, the
 * failure screen (a partial report is exactly what a bug report needs), and
 * the role step's "last sync report" affordance.
 */
@Composable
internal fun DeviceSyncReportActions(reportText: String) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        OpenVitalsOutlinedButton(
            onClick = {
                scope.launch {
                    clipboard.setClipEntry(
                        ClipData.newPlainText("OpenVitals", reportText).toClipEntry(),
                    )
                }
            },
            modifier = Modifier.weight(1f),
        ) {
            Icon(
                imageVector = Icons.Outlined.ContentCopy,
                contentDescription = null,
                modifier = Modifier.padding(end = 6.dp),
            )
            Text(stringResource(R.string.device_sync_copy_report))
        }
        Spacer(modifier = Modifier.width(10.dp))
        // Resolved in composition, not in the click: reading a
        // resource off the Context inside the lambda misses a
        // locale change until the screen is rebuilt.
        val chooserTitle = stringResource(R.string.device_sync_share_report_chooser_title)
        OpenVitalsOutlinedButton(
            onClick = {
                val send = Intent(Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, reportText)
                context.startActivity(Intent.createChooser(send, chooserTitle))
            },
            modifier = Modifier.weight(1f),
        ) {
            Icon(
                imageVector = Icons.Outlined.Share,
                contentDescription = null,
                modifier = Modifier.padding(end = 6.dp),
            )
            Text(stringResource(R.string.device_sync_share_report))
        }
    }
}
