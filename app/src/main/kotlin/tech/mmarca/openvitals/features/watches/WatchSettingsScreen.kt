package tech.mmarca.openvitals.features.watches

import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.time.Duration
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.devices.garmin.GarminEntryKind
import tech.mmarca.openvitals.devices.garmin.GarminSettingsEntry
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.theme.Spacing

/**
 * One screen of the watch's own settings, rendered from what the watch sent.
 *
 * The app defines none of this. Titles, option lists and the order of rows
 * all come from the watch, already translated into the locale it was handed —
 * so this screen's whole job is to turn declared controls into real ones and
 * to be honest when it cannot.
 *
 * Port of the Flutter build's `watch_settings_screen.dart`. Two adaptations:
 * the top bar here is the route table's static title, so the watch's own
 * screen title renders as a heading inside the content; and change results
 * (refused / unanswered) show as inline notices rather than snackbars — this
 * scaffold owns no snackbar host.
 */
@Composable
fun WatchSettingsScreen(
    viewModel: WatchSettingsViewModel,
    onOpenSubscreen: (deviceId: String, screenId: Int) -> Unit,
    onClose: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                WatchSettingsEvent.CLOSE_SCREEN -> onClose()
            }
        }
    }

    // Walking back out of a subscreen re-reads THIS one: what happened in
    // there changes what belongs here — "Add Alarm" creates the alarm this
    // list does not show yet, and a delete removed a row it still does.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onResumed()
    }

    when {
        state.loading -> Message(
            text = stringResource(R.string.settings_watch_settings_reading),
            busy = true,
        )

        state.failed -> Message(
            text = stringResource(R.string.settings_watch_settings_unreachable),
            retryLabel = stringResource(R.string.settings_watch_settings_retry),
            onRetry = viewModel::refresh,
        )

        state.isEmpty -> Message(
            text = stringResource(R.string.settings_watch_settings_empty),
            retryLabel = stringResource(R.string.settings_watch_settings_retry),
            onRetry = viewModel::refresh,
        )

        else -> {
            val screen = state.screen ?: return
            // Blank rows are dropped: an alarm list reserves one per slot and
            // leaves the unused ones with no title at all, which drew twenty
            // empty cards under a single real alarm.
            val rows = screen.entries.filterNot { it.isBlank }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = Spacing.sm,
                    bottom = Spacing.xxl,
                ),
            ) {
                item(key = "header") {
                    ScreenHeader(
                        watchTitle = screen.title,
                        hasState = screen.hasState,
                        notice = state.notice,
                    )
                }
                items(rows, key = { it.id }) { entry ->
                    EntryRow(
                        entry = entry,
                        busy = entry.id in state.busyEntryIds,
                        onToggle = { value -> viewModel.setSwitch(entry.id, value) },
                        onOpenSubscreen = { subId ->
                            onOpenSubscreen(viewModel.deviceId, subId)
                        },
                        onChooseOption = { index -> viewModel.chooseOption(entry.id, index) },
                        onPickTime = { hour, minute ->
                            viewModel.setTime(entry.id, hour, minute)
                        },
                        onRunAction = { viewModel.runAction(entry.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ScreenHeader(
    watchTitle: String?,
    hasState: Boolean,
    notice: WatchSettingsNotice?,
) {
    Column(modifier = Modifier.padding(start = Spacing.lg, end = Spacing.lg, bottom = Spacing.md)) {
        if (!watchTitle.isNullOrBlank()) {
            // The watch's own name for this screen — the top bar carries the
            // app's static title, so the watch's word for it lives here.
            Text(
                text = watchTitle,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = Spacing.xs),
            )
        }
        Text(
            text = stringResource(R.string.settings_watch_settings_from_watch),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!hasState) {
            Text(
                text = stringResource(R.string.settings_watch_settings_no_state),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = Spacing.md),
            )
        }
        notice?.let {
            Text(
                text = when (it) {
                    WatchSettingsNotice.REFUSED ->
                        stringResource(R.string.settings_watch_settings_refused)

                    WatchSettingsNotice.UNANSWERED ->
                        stringResource(R.string.settings_watch_settings_unanswered)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = Spacing.md),
            )
        }
    }
}

@Composable
private fun EntryRow(
    entry: GarminSettingsEntry,
    busy: Boolean,
    onToggle: (Boolean) -> Unit,
    onOpenSubscreen: (Int) -> Unit,
    onChooseOption: (Int) -> Unit,
    onPickTime: (hour: Int, minute: Int) -> Unit,
    onRunAction: () -> Unit,
) {
    var showOptions by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showConfirmAction by remember { mutableStateOf(false) }

    val title = entry.title.orEmpty()

    when (entry.kind) {
        GarminEntryKind.TOGGLE -> RowCard(
            title = title,
            summary = entry.summary,
            enabled = !busy,
            // Disabled while in flight rather than optimistic: the value
            // shown is the watch's, and a switch that flips before the watch
            // agrees would be reporting a request as a result.
            onClick = if (busy) null else ({ onToggle(!(entry.switchedOn ?: false)) }),
            trailing = {
                if (busy) {
                    RowSpinner()
                } else {
                    Switch(
                        checked = entry.switchedOn ?: false,
                        onCheckedChange = { value -> onToggle(value) },
                    )
                }
            },
        )

        GarminEntryKind.SUBSCREEN -> RowCard(
            title = title,
            summary = entry.summary,
            enabled = true,
            onClick = { entry.subscreenId?.let(onOpenSubscreen) },
            trailing = { Chevron() },
        )

        GarminEntryKind.OPTIONS -> {
            // Only offered when the watch actually sent choices. An empty
            // list here would open a dialog with nothing in it.
            val canChoose = entry.options.isNotEmpty() && !busy
            RowCard(
                title = title,
                summary = entry.summary,
                enabled = canChoose,
                onClick = if (canChoose) ({ showOptions = true }) else null,
                trailing = { if (busy) RowSpinner() else Chevron() },
            )
        }

        GarminEntryKind.TIME -> RowCard(
            title = title,
            summary = entry.summary,
            enabled = !busy,
            onClick = if (busy) null else ({ showTimePicker = true }),
            trailing = {
                if (busy) {
                    RowSpinner()
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
        )

        GarminEntryKind.NUMBER -> {
            // The watch bounds these itself and does not send the bounds, so
            // a picker here could offer a value it will refuse. Left readable
            // until the limits are known.
            val value = listOfNotNull(
                entry.summary?.takeIf { it.isNotEmpty() },
                entry.unit?.takeIf { it.isNotEmpty() },
            ).joinToString(" ")
            RowCard(
                title = title,
                summary = value.ifEmpty { null },
                enabled = false,
                onClick = null,
            )
        }

        GarminEntryKind.ACTION -> RowCard(
            // A button the WATCH put here, run under the watch's own label.
            // Asked first, always: an action row has no value to inspect
            // beforehand and no way to undo afterwards.
            title = title,
            summary = null,
            titleColor = MaterialTheme.colorScheme.error,
            enabled = !busy,
            onClick = if (busy) null else ({ showConfirmAction = true }),
            trailing = { if (busy) RowSpinner() },
        )

        GarminEntryKind.INERT -> RowCard(
            // Present on the watch, not actionable from a phone. Dimmed
            // rather than dropped, so the screen still matches what is on the
            // wrist.
            title = title,
            summary = entry.summary,
            titleColor = MaterialTheme.colorScheme.onSurfaceVariant,
            enabled = false,
            onClick = null,
        )
    }

    if (showOptions) {
        OptionsDialog(
            entry = entry,
            onDismiss = { showOptions = false },
            onChosen = { index ->
                showOptions = false
                onChooseOption(index)
            },
        )
    }
    if (showTimePicker) {
        EntryTimePickerDialog(
            title = entry.title,
            current = entry.time,
            onDismiss = { showTimePicker = false },
            onConfirm = { hour, minute ->
                showTimePicker = false
                onPickTime(hour, minute)
            },
        )
    }
    if (showConfirmAction) {
        ConfirmActionDialog(
            title = title,
            onDismiss = { showConfirmAction = false },
            onConfirm = {
                showConfirmAction = false
                onRunAction()
            },
        )
    }
}

@Composable
private fun RowCard(
    title: String,
    summary: String?,
    enabled: Boolean,
    onClick: (() -> Unit)?,
    titleColor: Color = Color.Unspecified,
    trailing: (@Composable () -> Unit)? = null,
) {
    OpenVitalsCard(
        modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xs),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .let { base ->
                    if (onClick != null && enabled) base.clickable(onClick = onClick) else base
                }
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = titleColor,
                )
                if (!summary.isNullOrEmpty()) {
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (trailing != null) {
                Spacer(modifier = Modifier.width(Spacing.md))
                trailing()
            }
        }
    }
}

@Composable
private fun RowSpinner() {
    CircularProgressIndicator(
        modifier = Modifier.size(Spacing.xl),
        strokeWidth = WatchProgressStroke,
    )
}

@Composable
private fun Chevron() {
    Icon(
        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Offers the options the WATCH sent, marking the chosen one by POSITION. */
@Composable
private fun OptionsDialog(
    entry: GarminSettingsEntry,
    onDismiss: () -> Unit,
    onChosen: (Int) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(entry.title.orEmpty()) },
        text = {
            Column {
                for (option in entry.options) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onChosen(option.index) }
                            .padding(vertical = WatchSettingRowPadding),
                    ) {
                        // By POSITION, which is what the watch actually
                        // reports. Matching the summary text against the
                        // titles held up until a screen arrived with an empty
                        // summary, and then nothing looked selected at all.
                        Icon(
                            imageVector = if (option.index == entry.selectedIndex) {
                                Icons.Outlined.RadioButtonChecked
                            } else {
                                Icons.Outlined.RadioButtonUnchecked
                            },
                            contentDescription = null,
                            modifier = Modifier.size(Spacing.xl),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.width(Spacing.md))
                        Text(
                            text = option.title,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

/**
 * Opened at the watch's OWN time. Starting from "now" meant every edit began
 * at the wrong number, so nudging an alarm by ten minutes actually reset it
 * to whenever you happened to open the picker.
 */
@Composable
private fun EntryTimePickerDialog(
    title: String?,
    current: Duration?,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit,
) {
    val context = LocalContext.current
    val initialMinutes = current?.inWholeMinutes
    val timePickerState = rememberTimePickerState(
        initialHour = (initialMinutes?.let { it / 60 % 24 } ?: 0L).toInt(),
        initialMinute = (initialMinutes?.let { it % 60 } ?: 0L).toInt(),
        is24Hour = DateFormat.is24HourFormat(context),
    )
    TimePickerDialog(
        onDismissRequest = onDismiss,
        title = { Text(title.orEmpty()) },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(timePickerState.hour, timePickerState.minute) },
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    ) {
        TimePicker(
            state = timePickerState,
            modifier = Modifier.padding(horizontal = Spacing.xxl),
        )
    }
}

/**
 * Runs an action row, after asking — under the watch's own wording: this app
 * does not know what the row does beyond what the watch called it, and
 * inventing a friendlier description would be claiming knowledge it does not
 * have.
 */
@Composable
private fun ConfirmActionDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.settings_watch_settings_confirm_action, title))
        },
        text = { Text(stringResource(R.string.settings_watch_settings_confirm_action_body)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text(title)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
private fun Message(
    text: String,
    busy: Boolean = false,
    retryLabel: String? = null,
    onRetry: (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.xxl),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (busy) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(bottom = Spacing.lg),
                )
            }
            Text(
                text = text,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (onRetry != null) {
                FilledTonalButton(
                    onClick = onRetry,
                    modifier = Modifier.padding(top = Spacing.md),
                ) {
                    Text(retryLabel.orEmpty())
                }
            }
        }
    }
}

/** The small in-row progress spinner's stroke. */
private val WatchProgressStroke = 2.dp
/** Vertical padding of one setting row. */
private val WatchSettingRowPadding = 10.dp
