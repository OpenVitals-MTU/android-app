package tech.mmarca.openvitals.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.domain.cycle.AgeBand
import tech.mmarca.openvitals.domain.cycle.ContextGroup
import tech.mmarca.openvitals.domain.cycle.CycleTrackingProfile
import tech.mmarca.openvitals.domain.cycle.TrackingContext
import tech.mmarca.openvitals.domain.model.CycleReminderConfig
import tech.mmarca.openvitals.domain.model.CycleReminderVisibility
import tech.mmarca.openvitals.features.cycle.ageBandLabelRes
import tech.mmarca.openvitals.features.cycle.trackingContextLabelRes
import tech.mmarca.openvitals.features.manualentry.ManualEntryPickerButton
import tech.mmarca.openvitals.features.manualentry.ManualEntryTimePickerDialog
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.OpenVitalsOutlinedButton
import tech.mmarca.openvitals.ui.components.OptionDropdown
import tech.mmarca.openvitals.ui.theme.LayoutMetrics
import tech.mmarca.openvitals.ui.theme.Spacing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule

/** The contexts, grouped by what they may change. A declaration is a statement, never a diagnosis. */
@Composable
internal fun CycleContextsCard(
    profile: CycleTrackingProfile,
    onToggleContext: (TrackingContext, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    OpenVitalsCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(LayoutMetrics.cardPadding), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(text = stringResource(R.string.cycle_settings_contexts_title), style = MaterialTheme.typography.titleSmall)
            Text(
                text = stringResource(R.string.cycle_settings_contexts_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ContextGroup.entries.forEach { group ->
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    text = stringResource(
                        if (group == ContextGroup.TIMING) R.string.cycle_settings_context_group_timing else R.string.cycle_settings_context_group_observation,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TrackingContext.entries.filter { it.group == group }.forEach { context ->
                    val checked = context in profile.contexts
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggleContext(context, it) })
                            .padding(vertical = Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = checked, onCheckedChange = null)
                        Text(
                            text = stringResource(trackingContextLabelRes(context)),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = Spacing.sm),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Optional. A birth year in the body profile sets the band; the picker is
 * the fallback. Declining is a first-class answer, not a skipped question.
 */
@Composable
internal fun CycleAgeBandCard(
    selected: AgeBand?,
    derived: AgeBand?,
    onSelect: (AgeBand?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val none = stringResource(R.string.cycle_age_band_none)
    val options = listOf<AgeBand?>(null) + AgeBand.entries
    OpenVitalsCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(LayoutMetrics.cardPadding), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(text = stringResource(R.string.cycle_settings_age_band_title), style = MaterialTheme.typography.titleSmall)
            Text(
                text = stringResource(R.string.cycle_settings_age_band_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (derived != null) {
                Text(
                    text = stringResource(R.string.cycle_settings_age_band_from_body_profile, stringResource(ageBandLabelRes(derived))),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(R.string.cycle_settings_age_band_from_body_profile_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }
            Column(modifier = Modifier.selectableGroup()) {
                options.forEach { band ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = selected == band, role = Role.RadioButton, onClick = { onSelect(band) })
                            .padding(vertical = Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = selected == band, onClick = null)
                        Text(
                            text = band?.let { stringResource(ageBandLabelRes(it)) } ?: none,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = Spacing.sm),
                        )
                    }
                }
            }
        }
    }
}

/** The three reminders and what they may say. Off by default. */
@Composable
internal fun CycleRemindersCard(
    state: CycleSettingsUiState,
    viewModel: CycleSettingsViewModel,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onRequestNotificationPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val config = state.reminders
    var editingTime by remember { mutableStateOf(false) }
    OpenVitalsCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(LayoutMetrics.cardPadding), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Text(text = stringResource(R.string.cycle_reminders_title), style = MaterialTheme.typography.titleSmall)
            SettingsSwitchRow(
                title = stringResource(R.string.cycle_reminders_master),
                body = stringResource(R.string.cycle_reminders_master_body),
                checked = config.enabled,
                onCheckedChange = { enabled ->
                    if (enabled && !state.hasNotificationPermission) onRequestNotificationPermission()
                    else viewModel.setRemindersEnabled(enabled)
                },
            )
            if (config.enabled && !state.hasNotificationPermission) {
                OpenVitalsOutlinedButton(onClick = onRequestNotificationPermission) {
                    Text(stringResource(R.string.action_grant_permission))
                }
            }
            if (config.enabled) {
                CycleReminderDetails(
                    config = config,
                    viewModel = viewModel,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    onEditTime = { editingTime = true },
                )
            }
        }
    }
    if (editingTime) {
        ManualEntryTimePickerDialog(
            selectedTime = config.dailyCheckInTime,
            onDismiss = { editingTime = false },
            onConfirm = { time ->
                editingTime = false
                viewModel.setDailyCheckInTime(time)
            },
        )
    }
}

@Composable
private fun CycleReminderDetails(
    config: CycleReminderConfig,
    viewModel: CycleSettingsViewModel,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onEditTime: () -> Unit,
) {
    val checkInTime = dateTimeFormatterProvider.shortTime().format(config.dailyCheckInTime)
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    SettingsSwitchRow(
        title = stringResource(R.string.cycle_reminders_daily_title),
        body = stringResource(R.string.cycle_reminders_daily_body),
        checked = config.dailyCheckInEnabled,
        onCheckedChange = viewModel::setDailyCheckIn,
    )
    if (config.dailyCheckInEnabled) {
        ManualEntryPickerButton(
            label = stringResource(R.string.cycle_reminders_time, checkInTime),
            value = checkInTime,
            icon = Icons.Outlined.Schedule,
            enabled = true,
            onClick = onEditTime,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    SettingsSwitchRow(
        title = stringResource(R.string.cycle_reminders_window_title),
        body = stringResource(R.string.cycle_reminders_window_body),
        checked = config.periodWindowEnabled,
        onCheckedChange = viewModel::setPeriodWindow,
    )
    if (config.periodWindowEnabled) {
        val leadOptions = CycleReminderConfig.LeadDaysRange.toList()
        OptionDropdown(
            label = stringResource(R.string.cycle_reminders_lead_label),
            options = leadOptions,
            selected = config.periodWindowLeadDays,
            optionText = { pluralStringResource(R.plurals.cycle_reminders_lead_days, it, it) },
            enabled = true,
            onSelect = { it?.let(viewModel::setPeriodWindowLeadDays) },
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    SettingsSwitchRow(
        title = stringResource(R.string.cycle_reminders_late_title),
        body = stringResource(R.string.cycle_reminders_late_body),
        checked = config.lateCycleEnabled,
        onCheckedChange = viewModel::setLateCycle,
    )
    if (config.lateCycleEnabled) {
        val graceOptions = CycleReminderConfig.GraceDaysRange.toList()
        OptionDropdown(
            label = stringResource(R.string.cycle_reminders_late_title),
            options = graceOptions,
            selected = config.lateCycleGraceDays,
            optionText = { pluralStringResource(R.plurals.cycle_reminders_grace_days, it, it) },
            enabled = true,
            onSelect = { it?.let(viewModel::setLateCycleGraceDays) },
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    CycleVisibilityChoice(config = config, viewModel = viewModel)
}

@Composable
private fun CycleVisibilityChoice(config: CycleReminderConfig, viewModel: CycleSettingsViewModel) {
    Text(text = stringResource(R.string.cycle_reminders_visibility_title), style = MaterialTheme.typography.labelLarge)
    Column(modifier = Modifier.selectableGroup()) {
        CycleReminderVisibility.entries.forEach { option ->
            val (title, body) = when (option) {
                CycleReminderVisibility.CONCEALED ->
                    R.string.cycle_reminders_visibility_concealed to R.string.cycle_reminders_visibility_concealed_body
                CycleReminderVisibility.DESCRIPTIVE ->
                    R.string.cycle_reminders_visibility_descriptive to R.string.cycle_reminders_visibility_descriptive_body
                CycleReminderVisibility.CUSTOM ->
                    R.string.cycle_reminders_visibility_custom to R.string.cycle_reminders_visibility_custom_body
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(selected = config.visibility == option, role = Role.RadioButton, onClick = { viewModel.setVisibility(option) })
                    .padding(vertical = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = config.visibility == option, onClick = null)
                Column(modifier = Modifier.padding(start = Spacing.sm)) {
                    Text(text = stringResource(title), style = MaterialTheme.typography.bodyMedium)
                    Text(text = stringResource(body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
    if (config.visibility == CycleReminderVisibility.CUSTOM) {
        OutlinedTextField(
            value = config.customTitle,
            onValueChange = viewModel::setCustomTitle,
            label = { Text(stringResource(R.string.cycle_reminders_custom_title)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = config.customBody,
            onValueChange = viewModel::setCustomBody,
            label = { Text(stringResource(R.string.cycle_reminders_custom_body)) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Export and import of the journal file. The outcome of the last action shows under the buttons. */
@Composable
internal fun CycleJournalBackupCard(
    message: CycleBackupMessage?,
    importedDays: Int,
    onExport: () -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OpenVitalsCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(LayoutMetrics.cardPadding), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(text = stringResource(R.string.cycle_settings_backup_title), style = MaterialTheme.typography.titleSmall)
            Text(
                text = stringResource(R.string.cycle_settings_backup_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OpenVitalsOutlinedButton(onClick = onExport, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.cycle_settings_backup_export))
            }
            OpenVitalsOutlinedButton(onClick = onImport, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.cycle_settings_backup_import))
            }
            message?.let {
                Text(
                    text = when (it) {
                        CycleBackupMessage.EXPORTED -> stringResource(R.string.cycle_journal_exported)
                        CycleBackupMessage.IMPORTED -> pluralStringResource(R.plurals.cycle_journal_imported, importedDays, importedDays)
                        CycleBackupMessage.IMPORT_FAILED -> stringResource(R.string.cycle_journal_import_failed)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (it == CycleBackupMessage.IMPORT_FAILED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** The journal's off switch. Health Connect records are not this card's to delete. */
@Composable
internal fun CycleDeleteJournalCard(
    isDeleting: Boolean,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirming by rememberSaveable { mutableStateOf(false) }
    OpenVitalsCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(LayoutMetrics.cardPadding), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(text = stringResource(R.string.cycle_settings_delete_title), style = MaterialTheme.typography.titleSmall)
            Text(
                text = stringResource(R.string.cycle_settings_delete_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OpenVitalsOutlinedButton(
                onClick = { confirming = true },
                enabled = !isDeleting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.cycle_settings_delete_action))
            }
        }
    }
    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text(stringResource(R.string.cycle_settings_delete_confirm_title)) },
            text = { Text(stringResource(R.string.cycle_settings_delete_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirming = false
                        onDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirming = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

/** Recorded and estimated are kept apart, and nothing is a diagnosis. Said once, here. */
@Composable
internal fun CycleAboutEstimatesCard(modifier: Modifier = Modifier) {
    OpenVitalsCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(LayoutMetrics.cardPadding), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(text = stringResource(R.string.cycle_settings_about_title), style = MaterialTheme.typography.titleSmall)
            Text(
                text = stringResource(R.string.cycle_settings_about_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.cycle_widget_privacy_notice),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
