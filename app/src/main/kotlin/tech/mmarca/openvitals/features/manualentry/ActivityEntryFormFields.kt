package tech.mmarca.openvitals.features.manualentry

import android.text.format.DateFormat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.preferences.UnitSystem
import tech.mmarca.openvitals.ui.components.AutoResizeText
import tech.mmarca.openvitals.ui.components.HealthDatePickerDialog
import tech.mmarca.openvitals.ui.theme.WorkoutColor

@Composable
internal fun ActivityEntryHeader(
    state: ActivityEntryUiState,
    onRequestWritePermission: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.DirectionsRun,
            contentDescription = null,
            tint = WorkoutColor,
            modifier = Modifier.size(22.dp),
        )
        Column(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .weight(1f),
        ) {
            Text(
                text = stringResource(R.string.manual_entry_activity_title),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(
                    if (state.canWrite) {
                        R.string.activity_entry_subtitle
                    } else {
                        R.string.activity_entry_permission_needed
                    }
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!state.canWrite && !state.isCheckingPermission) {
            OutlinedButton(onClick = onRequestWritePermission) {
                Text(stringResource(R.string.action_grant))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ActivityTypeSelector(
    types: List<ActivityEntryType>,
    selectedType: ActivityEntryType,
    onSelectActivityType: (ActivityEntryType) -> Unit,
    errorText: String?,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedTextField(
                value = stringResource(selectedType.labelRes),
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                label = { Text(stringResource(R.string.activity_entry_type_label)) },
                isError = errorText != null,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                types.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(stringResource(type.labelRes)) },
                        onClick = {
                            onSelectActivityType(type)
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
            }
        }
        FieldErrorText(errorText)
    }
}

@Composable
internal fun ActivityStartDateTimeFields(
    state: ActivityEntryUiState,
    enabled: Boolean,
    onStartDateChanged: (String) -> Unit,
    onStartTimeChanged: (String) -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val selectedDate = state.startDateText.toStartDateOrNull() ?: LocalDate.now()
    val selectedTime = state.startTimeText.toStartTimeOrNull() ?: LocalTime.now().withSecond(0).withNano(0)
    val dateError = state.validationErrorText(ActivityEntryField.START_DATE)
    val timeError = state.validationErrorText(ActivityEntryField.START_TIME)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ActivityPickerField(
                label = stringResource(R.string.activity_entry_start_date_label),
                value = selectedDate.localizedDateText(),
                icon = Icons.Outlined.CalendarMonth,
                enabled = enabled,
                isError = dateError != null,
                onClick = { showDatePicker = true },
            )
            FieldErrorText(dateError)
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ActivityPickerField(
                label = stringResource(R.string.activity_entry_start_time_label),
                value = selectedTime.localizedTimeText(),
                icon = Icons.Outlined.Schedule,
                enabled = enabled,
                isError = timeError != null,
                onClick = { showTimePicker = true },
            )
            FieldErrorText(timeError)
        }
    }

    if (showDatePicker) {
        HealthDatePickerDialog(
            selectedDate = selectedDate,
            onDismiss = { showDatePicker = false },
            onConfirm = { date ->
                showDatePicker = false
                onStartDateChanged(DateTimeFormatter.ISO_LOCAL_DATE.format(date))
            },
        )
    }

    if (showTimePicker) {
        ActivityTimePickerDialog(
            selectedTime = selectedTime,
            onDismiss = { showTimePicker = false },
            onConfirm = { time ->
                showTimePicker = false
                onStartTimeChanged(ActivityEntryTimeFormatter.format(time))
            },
        )
    }
}

@Composable
internal fun ActivityPickerField(
    label: String,
    value: String,
    icon: ImageVector,
    enabled: Boolean,
    isError: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        border = BorderStroke(
            width = 1.dp,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp),
            )
            Column(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .weight(1f),
            ) {
                AutoResizeText(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor,
                    maxLines = 1,
                )
                AutoResizeText(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ActivityTimePickerDialog(
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
        title = { Text(stringResource(R.string.activity_entry_select_time)) },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(LocalTime.of(timePickerState.hour, timePickerState.minute)) },
            ) {
                Text(stringResource(R.string.action_select))
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
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}
@Composable
internal fun ActivityMetricInputs(
    state: ActivityEntryUiState,
    unitSystem: UnitSystem,
    enabled: Boolean,
    onDistanceChanged: (String) -> Unit,
    onElevationChanged: (String) -> Unit,
    onActiveCaloriesChanged: (String) -> Unit,
    onTotalCaloriesChanged: (String) -> Unit,
) {
    val distanceError = state.validationErrorText(ActivityEntryField.DISTANCE)
    val elevationError = state.validationErrorText(ActivityEntryField.ELEVATION)
    val activeCaloriesError = state.validationErrorText(ActivityEntryField.ACTIVE_CALORIES)
    val totalCaloriesError = state.validationErrorText(ActivityEntryField.TOTAL_CALORIES)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = state.distanceText,
            onValueChange = onDistanceChanged,
            enabled = enabled && !state.isSavingEntry && state.selectedActivityType.supportsDistance,
            singleLine = true,
            label = {
                Text(
                    stringResource(
                        R.string.activity_entry_distance_label,
                        if (unitSystem == UnitSystem.IMPERIAL) "mi" else "km",
                    )
                )
            },
            isError = distanceError != null,
            supportingText = distanceError?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
            value = state.elevationText,
            onValueChange = onElevationChanged,
            enabled = enabled && !state.isSavingEntry && state.selectedActivityType.supportsElevation,
            singleLine = true,
            label = {
                Text(
                    stringResource(
                        R.string.activity_entry_elevation_label,
                        if (unitSystem == UnitSystem.IMPERIAL) "ft" else "m",
                    )
                )
            },
            isError = elevationError != null,
            supportingText = elevationError?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = state.activeCaloriesText,
            onValueChange = onActiveCaloriesChanged,
            enabled = enabled && !state.isSavingEntry,
            singleLine = true,
            label = { Text(stringResource(R.string.metric_active_calories)) },
            isError = activeCaloriesError != null,
            supportingText = activeCaloriesError?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
            value = state.totalCaloriesText,
            onValueChange = onTotalCaloriesChanged,
            enabled = enabled && !state.isSavingEntry,
            singleLine = true,
            label = { Text(stringResource(R.string.metric_calories_burned)) },
            isError = totalCaloriesError != null,
            supportingText = totalCaloriesError?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
        )
    }
}
