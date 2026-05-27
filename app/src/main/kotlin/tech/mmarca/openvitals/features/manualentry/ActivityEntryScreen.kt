package tech.mmarca.openvitals.features.manualentry

import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.preferences.UnitSystem
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.features.activity.RoutePreview
import tech.mmarca.openvitals.ui.components.HealthDatePickerDialog
import tech.mmarca.openvitals.ui.theme.WorkoutColor

@Composable
fun ActivityEntryScreen(
    viewModel: ActivityEntryViewModel,
    unitFormatter: UnitFormatter,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingSourceAction by remember { mutableStateOf<ActivityEntrySourceAction?>(null) }
    val importRouteFile = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            viewModel.importRouteFile(uri, unitFormatter.unitSystem())
        }
    }
    fun performSourceAction(action: ActivityEntrySourceAction) {
        when (action) {
            ActivityEntrySourceAction.MANUAL -> viewModel.startManualEntry()
            ActivityEntrySourceAction.IMPORT_ROUTE_FILE -> importRouteFile.launch(RouteImportMimeTypes)
        }
    }
    val requestWritePermissions = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) {
        viewModel.refreshPermission()
        pendingSourceAction?.let { action ->
            pendingSourceAction = null
            performSourceAction(action)
        }
    }
    fun performSourceActionAfterPermission(action: ActivityEntrySourceAction) {
        if (state.canWrite) {
            performSourceAction(action)
        } else {
            pendingSourceAction = action
            requestWritePermissions.launch(state.writePermissions)
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshPermission()
    }

    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
        item {
            if (state.mode == ActivityEntryMode.CHOOSE_SOURCE) {
                ActivityEntrySourceCard(
                    state = state,
                    onStartManualEntry = {
                        performSourceActionAfterPermission(ActivityEntrySourceAction.MANUAL)
                    },
                    onImportRouteFile = {
                        performSourceActionAfterPermission(ActivityEntrySourceAction.IMPORT_ROUTE_FILE)
                    },
                    onRequestWritePermission = {
                        requestWritePermissions.launch(state.writePermissions)
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            } else {
                ActivityEntryCard(
                    state = state,
                    unitFormatter = unitFormatter,
                    onSelectActivityType = viewModel::selectActivityType,
                    onTitleChanged = viewModel::updateTitle,
                    onNotesChanged = viewModel::updateNotes,
                    onStartDateChanged = viewModel::updateStartDate,
                    onStartTimeChanged = viewModel::updateStartTime,
                    onDurationChanged = viewModel::updateDurationMinutes,
                    onDistanceChanged = viewModel::updateDistance,
                    onElevationChanged = viewModel::updateElevation,
                    onActiveCaloriesChanged = viewModel::updateActiveCalories,
                    onTotalCaloriesChanged = viewModel::updateTotalCalories,
                    onClearRoute = viewModel::clearImportedRoute,
                    onChooseSource = viewModel::chooseSource,
                    onRequestWritePermission = {
                        requestWritePermissions.launch(state.writePermissions)
                    },
                    onAddEntry = {
                        viewModel.addEntry(unitFormatter.unitSystem())
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }
}

private enum class ActivityEntrySourceAction {
    MANUAL,
    IMPORT_ROUTE_FILE,
}

@Composable
private fun ActivityEntrySourceCard(
    state: ActivityEntryUiState,
    onStartManualEntry: () -> Unit,
    onImportRouteFile: () -> Unit,
    onRequestWritePermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ActivityEntryHeader(
                state = state,
                onRequestWritePermission = onRequestWritePermission,
            )

            Text(
                text = stringResource(R.string.activity_entry_source_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Button(
                onClick = onStartManualEntry,
                enabled = !state.isCheckingPermission && !state.isImportingRoute && !state.isSavingEntry,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(R.string.activity_entry_create_manual),
                    modifier = Modifier.padding(start = 6.dp),
                )
            }

            OutlinedButton(
                onClick = onImportRouteFile,
                enabled = !state.isCheckingPermission && !state.isImportingRoute && !state.isSavingEntry,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Outlined.FolderOpen,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(R.string.activity_entry_import_route_file),
                    modifier = Modifier.padding(start = 6.dp),
                )
            }

            state.entryError?.let { error ->
                Text(
                    text = activityEntryErrorText(error, state.detailMessage),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun ActivityEntryCard(
    state: ActivityEntryUiState,
    unitFormatter: UnitFormatter,
    onSelectActivityType: (ActivityEntryType) -> Unit,
    onTitleChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit,
    onStartDateChanged: (String) -> Unit,
    onStartTimeChanged: (String) -> Unit,
    onDurationChanged: (String) -> Unit,
    onDistanceChanged: (String) -> Unit,
    onElevationChanged: (String) -> Unit,
    onActiveCaloriesChanged: (String) -> Unit,
    onTotalCaloriesChanged: (String) -> Unit,
    onClearRoute: () -> Unit,
    onChooseSource: () -> Unit,
    onRequestWritePermission: () -> Unit,
    onAddEntry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val enabled = state.canWrite && !state.isSavingEntry && !state.isCheckingPermission && !state.isImportingRoute
    val durationError = state.validationErrorText(ActivityEntryField.DURATION)
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ActivityEntryHeader(
                state = state,
                onRequestWritePermission = onRequestWritePermission,
            )

            OutlinedButton(
                onClick = onChooseSource,
                enabled = !state.isSavingEntry && !state.isImportingRoute,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.activity_entry_choose_another_source))
            }

            ActivityTypeSelector(
                types = if (state.importedRoute == null) {
                    state.activityTypes
                } else {
                    state.activityTypes.filter { it.supportsGpsRoute }
                },
                selectedType = state.selectedActivityType,
                onSelectActivityType = onSelectActivityType,
                errorText = state.validationErrorText(ActivityEntryField.ACTIVITY_TYPE),
            )

            OutlinedTextField(
                value = state.titleText,
                onValueChange = onTitleChanged,
                enabled = !state.isSavingEntry,
                singleLine = true,
                label = { Text(stringResource(R.string.activity_entry_title_label)) },
                modifier = Modifier.fillMaxWidth(),
            )

            ActivityStartDateTimeFields(
                state = state,
                enabled = !state.isSavingEntry,
                onStartDateChanged = onStartDateChanged,
                onStartTimeChanged = onStartTimeChanged,
            )

            OutlinedTextField(
                value = state.durationMinutesText,
                onValueChange = onDurationChanged,
                enabled = !state.isSavingEntry,
                singleLine = true,
                label = { Text(stringResource(R.string.activity_entry_duration_label)) },
                isError = durationError != null,
                supportingText = durationError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            ActivityMetricInputs(
                state = state,
                unitSystem = unitFormatter.unitSystem(),
                onDistanceChanged = onDistanceChanged,
                onElevationChanged = onElevationChanged,
                onActiveCaloriesChanged = onActiveCaloriesChanged,
                onTotalCaloriesChanged = onTotalCaloriesChanged,
            )

            OutlinedTextField(
                value = state.notesText,
                onValueChange = onNotesChanged,
                enabled = !state.isSavingEntry,
                minLines = 2,
                label = { Text(stringResource(R.string.activity_entry_notes_label)) },
                modifier = Modifier.fillMaxWidth(),
            )

            ImportedActivityRouteSection(
                state = state,
                unitFormatter = unitFormatter,
                onClearRoute = onClearRoute,
            )

            Button(
                onClick = onAddEntry,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(R.string.activity_entry_add),
                    modifier = Modifier.padding(start = 6.dp),
                )
            }

            state.entryError?.let { error ->
                Text(
                    text = activityEntryErrorText(error, state.detailMessage),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun ActivityEntryHeader(
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
private fun ActivityTypeSelector(
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
private fun ActivityStartDateTimeFields(
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
private fun ActivityPickerField(
    label: String,
    value: String,
    icon: ImageVector,
    enabled: Boolean,
    isError: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor = if (isError) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
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
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActivityTimePickerDialog(
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
                onClick = {
                    onConfirm(LocalTime.of(timePickerState.hour, timePickerState.minute))
                },
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
private fun ActivityMetricInputs(
    state: ActivityEntryUiState,
    unitSystem: UnitSystem,
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
            enabled = !state.isSavingEntry && state.selectedActivityType.supportsDistance,
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
            enabled = !state.isSavingEntry && state.selectedActivityType.supportsElevation,
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
            enabled = !state.isSavingEntry,
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
            enabled = !state.isSavingEntry,
            singleLine = true,
            label = { Text(stringResource(R.string.metric_calories_burned)) },
            isError = totalCaloriesError != null,
            supportingText = totalCaloriesError?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ImportedActivityRouteSection(
    state: ActivityEntryUiState,
    unitFormatter: UnitFormatter,
    onClearRoute: () -> Unit,
) {
    val route = state.importedRoute ?: return

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.activity_entry_imported_route),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            OutlinedButton(
                onClick = onClearRoute,
                enabled = !state.isSavingEntry,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RoutePreview(
                    points = route.points,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                )
                Text(
                    text = stringResource(
                        R.string.activity_entry_route_summary,
                        route.name
                            ?: route.fileName
                            ?: stringResource(R.string.activity_entry_imported_route),
                        unitFormatter.distance(route.distanceMeters).text,
                        unitFormatter.elevation(route.elevationGainedMeters).text,
                        route.points.size,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun FieldErrorText(errorText: String?) {
    if (errorText == null) return
    Text(
        text = errorText,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}

@Composable
private fun ActivityEntryUiState.validationErrorText(field: ActivityEntryField): String? =
    validationErrors.firstOrNull { it.field == field }?.validationMessage()

@Composable
private fun ActivityEntryValidationError.validationMessage(): String = stringResource(
    when (this) {
        ActivityEntryValidationError.ACTIVITY_TYPE_DOES_NOT_SUPPORT_ROUTE ->
            R.string.activity_entry_error_activity_type_route
        ActivityEntryValidationError.START_DATE_INVALID -> R.string.activity_entry_error_start_date
        ActivityEntryValidationError.START_TIME_INVALID -> R.string.activity_entry_error_start_time
        ActivityEntryValidationError.START_TIME_AFTER_ROUTE_START ->
            R.string.activity_entry_error_start_time_after_route
        ActivityEntryValidationError.DURATION_INVALID -> R.string.activity_entry_error_duration
        ActivityEntryValidationError.DISTANCE_INVALID -> R.string.activity_entry_error_distance
        ActivityEntryValidationError.DISTANCE_UNSUPPORTED -> R.string.activity_entry_error_distance_unsupported
        ActivityEntryValidationError.ELEVATION_INVALID -> R.string.activity_entry_error_elevation
        ActivityEntryValidationError.ELEVATION_UNSUPPORTED -> R.string.activity_entry_error_elevation_unsupported
        ActivityEntryValidationError.ACTIVE_CALORIES_INVALID -> R.string.activity_entry_error_active_calories
        ActivityEntryValidationError.TOTAL_CALORIES_INVALID -> R.string.activity_entry_error_total_calories
        ActivityEntryValidationError.TOTAL_CALORIES_BELOW_ACTIVE ->
            R.string.activity_entry_error_total_calories_below_active
    }
)

@Composable
private fun LocalDate.localizedDateText(): String =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        .withLocale(LocalLocale.current.platformLocale)
        .format(this)

@Composable
private fun LocalTime.localizedTimeText(): String =
    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
        .withLocale(LocalLocale.current.platformLocale)
        .format(this)

private fun String.toStartDateOrNull(): LocalDate? =
    runCatching { LocalDate.parse(trim()) }.getOrNull()

private fun String.toStartTimeOrNull(): LocalTime? =
    runCatching { LocalTime.parse(trim(), ActivityEntryTimeFormatter) }.getOrNull()

@Composable
private fun activityEntryErrorText(
    error: ActivityEntryError,
    message: String?,
): String = when (error) {
    ActivityEntryError.INVALID_VALUE -> stringResource(R.string.activity_entry_invalid_value)
    ActivityEntryError.MISSING_WRITE_PERMISSION -> stringResource(R.string.activity_entry_permission_needed)
    ActivityEntryError.ROUTE_IMPORT_FAILED -> stringResource(
        R.string.activity_entry_route_import_failed,
        message ?: stringResource(R.string.unknown_error),
    )
    ActivityEntryError.WRITE_FAILED -> stringResource(
        R.string.activity_entry_write_failed,
        message ?: stringResource(R.string.unknown_error),
    )
}

private val RouteImportMimeTypes = arrayOf(
    "application/gpx+xml",
    "application/vnd.google-earth.kml+xml",
    "application/vnd.google-earth.kmz",
    "application/xml",
    "text/xml",
    "application/zip",
    "application/x-zip-compressed",
    "application/octet-stream",
)

private val ActivityEntryTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("H:mm")
