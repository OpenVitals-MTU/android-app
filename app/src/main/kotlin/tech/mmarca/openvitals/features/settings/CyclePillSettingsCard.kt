package tech.mmarca.openvitals.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.domain.model.PillPlan
import tech.mmarca.openvitals.features.cycle.pillStatusText
import tech.mmarca.openvitals.features.manualentry.ManualEntryPickerButton
import tech.mmarca.openvitals.features.manualentry.ManualEntryTimePickerDialog
import tech.mmarca.openvitals.features.manualentry.localizedDateText
import tech.mmarca.openvitals.ui.components.HealthDatePickerDialog
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.OpenVitalsIconButton
import tech.mmarca.openvitals.ui.components.OpenVitalsOutlinedButton
import tech.mmarca.openvitals.ui.theme.LayoutMetrics
import tech.mmarca.openvitals.ui.theme.Spacing
import java.time.LocalDate

/** The pill scheme: taking days, pause days, a pack's first day, and the daily reminder. */
@Composable
internal fun CyclePillCard(
    state: CycleSettingsUiState,
    viewModel: CycleSettingsViewModel,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onRequestNotificationPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val plan = state.pill
    var editingStart by remember { mutableStateOf(false) }
    var editingTime by remember { mutableStateOf(false) }
    OpenVitalsCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(LayoutMetrics.cardPadding), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Text(text = stringResource(R.string.cycle_pill_title), style = MaterialTheme.typography.titleSmall)
            SettingsSwitchRow(
                title = stringResource(R.string.cycle_pill_track),
                body = stringResource(R.string.cycle_pill_track_body),
                checked = plan.enabled,
                onCheckedChange = viewModel::setPillEnabled,
            )
            if (plan.enabled) {
                CyclePillScheme(
                    plan = plan,
                    viewModel = viewModel,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    onEditStart = { editingStart = true },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingsSwitchRow(
                    title = stringResource(R.string.cycle_pill_reminder),
                    body = stringResource(R.string.cycle_pill_reminder_body),
                    checked = plan.reminderEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled && !state.hasNotificationPermission) onRequestNotificationPermission()
                        viewModel.setPillReminder(enabled)
                    },
                )
                if (plan.reminderEnabled) {
                    if (!state.hasNotificationPermission) {
                        OpenVitalsOutlinedButton(onClick = onRequestNotificationPermission) {
                            Text(stringResource(R.string.action_grant_permission))
                        }
                    }
                    val time = dateTimeFormatterProvider.shortTime().format(plan.reminderTime)
                    ManualEntryPickerButton(
                        label = stringResource(R.string.cycle_reminders_time, time),
                        value = time,
                        icon = Icons.Outlined.Schedule,
                        enabled = true,
                        onClick = { editingTime = true },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
    if (editingStart) {
        HealthDatePickerDialog(
            selectedDate = plan.packStart ?: LocalDate.now(),
            onDismiss = { editingStart = false },
            onConfirm = { date ->
                editingStart = false
                viewModel.setPillPackStart(date)
            },
        )
    }
    if (editingTime) {
        ManualEntryTimePickerDialog(
            selectedTime = plan.reminderTime,
            onDismiss = { editingTime = false },
            onConfirm = { time ->
                editingTime = false
                viewModel.setPillReminderTime(time)
            },
        )
    }
}

@Composable
private fun CyclePillScheme(
    plan: PillPlan,
    viewModel: CycleSettingsViewModel,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onEditStart: () -> Unit,
) {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    PillCountRow(
        label = stringResource(R.string.cycle_pill_active_days),
        value = plan.activeDays,
        range = PillPlan.ActiveDaysRange,
        onChange = viewModel::setPillActiveDays,
    )
    PillCountRow(
        label = stringResource(R.string.cycle_pill_pause_days),
        value = plan.pauseDays,
        range = PillPlan.PauseDaysRange,
        onChange = viewModel::setPillPauseDays,
    )
    Text(
        text = stringResource(R.string.cycle_pill_scheme_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    ManualEntryPickerButton(
        label = stringResource(R.string.cycle_pill_pack_start),
        value = plan.packStart?.localizedDateText().orEmpty(),
        icon = Icons.Outlined.CalendarMonth,
        enabled = true,
        onClick = onEditStart,
        modifier = Modifier.fillMaxWidth(),
    )
    val status = pillStatusText(plan, LocalDate.now(), dateTimeFormatterProvider)
    if (status.isNotEmpty()) {
        Text(text = status, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PillCountRow(label: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        OpenVitalsIconButton(onClick = { onChange(value - 1) }, enabled = value > range.first) {
            Icon(
                imageVector = Icons.Outlined.RemoveCircleOutline,
                contentDescription = stringResource(R.string.cd_decrease_setting, label),
            )
        }
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = Spacing.sm),
        )
        OpenVitalsIconButton(onClick = { onChange(value + 1) }, enabled = value < range.last) {
            Icon(
                imageVector = Icons.Outlined.AddCircleOutline,
                contentDescription = stringResource(R.string.cd_increase_setting, label),
            )
        }
    }
}
