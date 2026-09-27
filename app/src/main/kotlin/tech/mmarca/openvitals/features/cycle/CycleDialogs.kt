package tech.mmarca.openvitals.features.cycle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.domain.cycle.CycleExclusionReason
import tech.mmarca.openvitals.domain.cycle.LongitudinalCycleItem
import tech.mmarca.openvitals.ui.theme.LayoutMetrics
import tech.mmarca.openvitals.ui.theme.Spacing

private const val BackfillWindowDays = 92L

/** Keep a cycle in the history but out of the estimate, with an optional reason. */
@Composable
internal fun CycleExclusionDialog(
    cycle: LongitudinalCycleItem,
    onDismiss: () -> Unit,
    onConfirm: (excluded: Boolean, reason: CycleExclusionReason?) -> Unit,
) {
    var excluded by remember { mutableStateOf(cycle.isExcluded) }
    var reason by remember { mutableStateOf(cycle.exclusionReason ?: CycleExclusionReason.ILLNESS) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.cycle_exclusion_dialog_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Text(
                    text = stringResource(R.string.cycle_exclusion_dialog_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = stringResource(R.string.cycle_exclusion_switch), style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = excluded, onCheckedChange = { excluded = it })
                }
                if (excluded) {
                    Text(text = stringResource(R.string.cycle_exclusion_reason_label), style = MaterialTheme.typography.titleSmall)
                    Column(modifier = Modifier.selectableGroup()) {
                        CycleExclusionReason.entries.forEach { option ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectable(selected = reason == option, role = Role.RadioButton, onClick = { reason = option })
                                    .padding(vertical = Spacing.xs),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(selected = reason == option, onClick = null)
                                Text(
                                    text = stringResource(exclusionReasonLabelRes(option)),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(start = Spacing.sm),
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(excluded, if (excluded) reason else null) }) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

/** A past period start, within the last three months and not on an existing start. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CycleBackfillDialog(
    existingStarts: Set<LocalDate>,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val today = LocalDate.now()
    val earliest = today.minusDays(BackfillWindowDays)
    val selectable = remember(existingStarts) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val date = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()
                return !date.isAfter(today) && !date.isBefore(earliest) && date !in existingStarts
            }

            override fun isSelectableYear(year: Int): Boolean = year in earliest.year..today.year
        }
    }
    val state = rememberDatePickerState(
        initialSelectedDateMillis = today.minusDays(BackfillWindowDays / 2).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = selectable,
    )
    val selected = state.selectedDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { selected?.let(onConfirm) }, enabled = selected != null) {
                Text(stringResource(R.string.cycle_backfill_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(
                text = stringResource(R.string.cycle_backfill_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = LayoutMetrics.screenGutter, end = LayoutMetrics.screenGutter, top = LayoutMetrics.screenGutter),
            )
            Text(
                text = stringResource(R.string.cycle_backfill_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = LayoutMetrics.screenGutter),
            )
            DatePicker(state = state, title = null)
        }
    }
}

/** Asks before a swipe removes an observation. A day log is many fields at once; a record is one. */
@Composable
internal fun CycleDeleteObservationDialog(
    observation: CycleObservation,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val body = if (observation.kind != null) {
        stringResource(R.string.cycle_delete_record_body, observation.title)
    } else {
        val date = observation.dayLogDate?.let { dateTimeFormatterProvider.mediumDate().format(it) }.orEmpty()
        stringResource(R.string.cycle_delete_day_log_body, date)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.cycle_delete_observation_title)) },
        text = { Text(body) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Text(stringResource(R.string.action_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
