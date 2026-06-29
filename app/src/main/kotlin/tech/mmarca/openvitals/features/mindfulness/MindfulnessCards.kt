package tech.mmarca.openvitals.features.mindfulness

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.MindfulnessReminderConfig
import tech.mmarca.openvitals.domain.model.MindfulnessSession
import tech.mmarca.openvitals.ui.components.MetricCard
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.OpenVitalsIconButton
import tech.mmarca.openvitals.ui.components.OpenVitalsOutlinedButton
import tech.mmarca.openvitals.ui.components.OpenVitalsTextButton
import tech.mmarca.openvitals.ui.components.SourceChip
import tech.mmarca.openvitals.ui.components.SwipeToDeleteEntryRow
import tech.mmarca.openvitals.ui.theme.MindfulnessColor
import java.time.LocalTime
import java.time.ZoneId

@Composable
internal fun MindfulnessSummary(
    display: MindfulnessDisplayState,
    subtitle: String,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
) {
    val total = unitFormatter.minutes(display.summary.totalMinutes)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MetricCard(
            title = stringResource(R.string.metric_total_mindfulness),
            value = total.value,
            unit = total.unit,
            icon = Icons.Outlined.SelfImprovement,
            accentColor = MindfulnessColor,
            subtitle = subtitle,
            modifier = Modifier.weight(1f),
        )
        MetricCard(
            title = stringResource(R.string.section_sessions),
            value = unitFormatter.count(display.summary.sessionCount),
            unit = stringResource(R.string.unit_total),
            icon = Icons.Outlined.SelfImprovement,
            accentColor = MindfulnessColor,
            subtitle = stringResource(R.string.period_selected),
            modifier = Modifier.weight(1f),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MindfulnessReminderCard(
    config: MindfulnessReminderConfig,
    hasNotificationPermission: Boolean,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onToggleReminders: (Boolean) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onSelectReminderTime: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
) {
    val normalized = config.normalized()
    var editingTime by remember { mutableStateOf(false) }
    val reminderTime = dateTimeFormatterProvider.shortTime().format(normalized.reminderTime)
    val body = when {
        normalized.enabled && !hasNotificationPermission -> {
            stringResource(R.string.mindfulness_reminders_permission_needed)
        }
        normalized.enabled -> {
            stringResource(R.string.mindfulness_reminders_summary_on, reminderTime)
        }
        else -> stringResource(R.string.mindfulness_reminders_summary_off)
    }

    OpenVitalsCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = null,
                    tint = MindfulnessColor,
                    modifier = Modifier.size(22.dp),
                )
                Column(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .weight(1f),
                ) {
                    Text(
                        text = stringResource(R.string.mindfulness_reminders_title),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = body,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = normalized.enabled,
                    onCheckedChange = { enabled ->
                        if (enabled && !hasNotificationPermission) {
                            onRequestNotificationPermission()
                        } else {
                            onToggleReminders(enabled)
                        }
                    },
                )
            }

            if (normalized.enabled && !hasNotificationPermission) {
                OpenVitalsOutlinedButton(onClick = onRequestNotificationPermission) {
                    Text(stringResource(R.string.action_grant_permission))
                }
            }

            if (normalized.enabled) {
                MindfulnessReminderTimeRow(
                    label = stringResource(R.string.mindfulness_reminders_time),
                    value = reminderTime,
                    onClick = { editingTime = true },
                )
                Text(
                    text = stringResource(R.string.mindfulness_reminders_goal_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (editingTime) {
        MindfulnessReminderTimePickerDialog(
            title = stringResource(R.string.mindfulness_reminders_time),
            selectedTime = normalized.reminderTime,
            onDismiss = { editingTime = false },
            onConfirm = { time ->
                editingTime = false
                onSelectReminderTime(time)
            },
        )
    }
}

@Composable
private fun MindfulnessReminderTimeRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Schedule,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Column(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .weight(1f),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        OpenVitalsTextButton(onClick = onClick) {
            Text(stringResource(R.string.action_select))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MindfulnessReminderTimePickerDialog(
    title: String,
    selectedTime: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit,
) {
    val context = LocalContext.current
    val timePickerState = rememberTimePickerState(
        initialHour = selectedTime.hour,
        initialMinute = selectedTime.minute,
        is24Hour = DateFormat.is24HourFormat(context),
    )

    TimePickerDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        confirmButton = {
            OpenVitalsTextButton(
                onClick = {
                    onConfirm(LocalTime.of(timePickerState.hour, timePickerState.minute))
                },
            ) {
                Text(stringResource(R.string.action_select))
            }
        },
        dismissButton = {
            OpenVitalsTextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    ) {
        TimePicker(
            state = timePickerState,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}

@Composable
internal fun MindfulnessSessionRow(
    session: MindfulnessSession,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    if (onDelete != null) {
        SwipeToDeleteEntryRow(
            onDelete = onDelete,
            modifier = modifier,
        ) {
            MindfulnessSessionRowContent(
                session = session,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                onEdit = onEdit,
            )
        }
    } else {
        MindfulnessSessionRowContent(
            session = session,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            onEdit = onEdit,
            modifier = modifier,
        )
    }
}

@Composable
private fun MindfulnessSessionRowContent(
    session: MindfulnessSession,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onEdit: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
    val start = session.startTime.atZone(zone)
    val dateFormatter = dateTimeFormatterProvider.mediumDate()
    val timeFormatter = dateTimeFormatterProvider.shortTime()

    OpenVitalsCard(
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.SelfImprovement,
                contentDescription = null,
                tint = MindfulnessColor,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.title ?: stringResource(R.string.metric_mindfulness),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = "${dateFormatter.format(start)}  ·  ${timeFormatter.format(start)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = unitFormatter.duration(session.durationMs),
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(Modifier.height(4.dp))
                SourceChip(source = session.source)
            }
            if (onEdit != null) {
                Spacer(Modifier.width(8.dp))
                OpenVitalsIconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = stringResource(R.string.cd_edit_entry),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

internal fun MindfulnessSession.editAction(onEditMindfulnessSession: (String) -> Unit): (() -> Unit)? =
    if (isOpenVitalsEntry && id.isNotBlank()) {
        { onEditMindfulnessSession(id) }
    } else {
        null
    }

internal fun MindfulnessSession.deleteAction(onDeleteMindfulnessSession: (String) -> Unit): (() -> Unit)? =
    if (isOpenVitalsEntry && id.isNotBlank()) {
        { onDeleteMindfulnessSession(id) }
    } else {
        null
    }
