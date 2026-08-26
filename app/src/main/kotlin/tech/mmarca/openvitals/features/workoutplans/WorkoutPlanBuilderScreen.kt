package tech.mmarca.openvitals.features.workoutplans

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.ui.theme.Spacing
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.features.manualentry.activity.ActivityPickerField
import tech.mmarca.openvitals.features.manualentry.activity.ActivityTimePickerDialog
import tech.mmarca.openvitals.features.manualentry.activity.FieldErrorText
import tech.mmarca.openvitals.features.manualentry.activity.localizedDateText
import tech.mmarca.openvitals.features.manualentry.activity.localizedTimeText
import tech.mmarca.openvitals.healthconnect.HealthConnectFeature
import tech.mmarca.openvitals.ui.components.HealthDatePickerDialog
import tech.mmarca.openvitals.ui.components.OpenVitalsButton
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.OpenVitalsOutlinedButton
import tech.mmarca.openvitals.ui.components.OpenVitalsTextButton
import tech.mmarca.openvitals.ui.components.ScreenErrorContent
import tech.mmarca.openvitals.ui.components.WithHealthConnectFeatureScreen
import tech.mmarca.openvitals.ui.components.rememberHealthConnectPermissionLauncher

@Composable
fun WorkoutPlanBuilderScreen(
    onSaved: (String) -> Unit,
    onClose: () -> Unit,
    viewModel: WorkoutPlanBuilderViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDiscardDialog by remember { mutableStateOf(false) }
    var pickerBlockId by remember { mutableStateOf<String?>(null) }
    val permissionLauncher = rememberHealthConnectPermissionLauncher(onResult = viewModel::clearError)

    LaunchedEffect(state.savedPlanId) {
        state.savedPlanId?.let(onSaved)
    }
    BackHandler(enabled = state.isDirty && !state.saveCompleted) {
        showDiscardDialog = true
    }

    WithHealthConnectFeatureScreen(
        feature = HealthConnectFeature.WORKOUT_PLANS,
        isLoading = state.isLoading,
        modifier = Modifier.fillMaxSize(),
    ) { _ ->
        when {
            state.isLoading -> Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
            }
            !state.isAvailable -> Text(
                text = stringResource(R.string.workout_plan_unavailable_on_device),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(Spacing.lg),
            )
            state.error == ScreenError.NotFound -> ScreenErrorContent(screenError = state.error)
            else -> WorkoutPlanBuilderContent(
                state = state,
                viewModel = viewModel,
                onGrantPermissions = { permissionLauncher.launch(state.writePermissions) },
                onOpenPicker = { pickerBlockId = it },
            )
        }
    }

    pickerBlockId?.let { blockId ->
        WorkoutPlanStepPickerSheet(
            onPick = { choice ->
                viewModel.addStep(blockId, choice)
                pickerBlockId = null
            },
            onPickRest = {
                viewModel.addRestStep(blockId)
                pickerBlockId = null
            },
            onDismiss = { pickerBlockId = null },
        )
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(R.string.workout_plan_discard_title)) },
            text = { Text(stringResource(R.string.workout_plan_discard_body)) },
            confirmButton = {
                OpenVitalsTextButton(
                    onClick = {
                        showDiscardDialog = false
                        onClose()
                    },
                ) {
                    Text(stringResource(R.string.action_discard))
                }
            },
            dismissButton = {
                OpenVitalsTextButton(onClick = { showDiscardDialog = false }) {
                    Text(stringResource(R.string.workout_plan_keep_editing))
                }
            },
        )
    }
}

@Composable
private fun WorkoutPlanBuilderContent(
    state: WorkoutPlanBuilderUiState,
    viewModel: WorkoutPlanBuilderViewModel,
    onGrantPermissions: () -> Unit,
    onOpenPicker: (String) -> Unit,
) {
    val enabled = state.canEdit
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        if (!state.isOwnedByApp) {
            item(key = "read-only") {
                Text(
                    text = stringResource(R.string.workout_plan_read_only_other_app),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        state.error?.let { error ->
            item(key = "error") {
                Column {
                    ScreenErrorContent(screenError = error)
                    if (error == ScreenError.PermissionDenied && state.writePermissions.isNotEmpty()) {
                        OpenVitalsOutlinedButton(onClick = onGrantPermissions, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.action_grant_permission))
                        }
                    }
                }
            }
        }
        item(key = "session") {
            WorkoutPlanSessionCard(state = state, viewModel = viewModel, enabled = enabled)
        }
        state.errorFor(WorkoutPlanValidationErrorKind.NO_BLOCKS)?.let { error ->
            item(key = "no-blocks") { FieldErrorText(error.message()) }
        }
        state.errorFor(WorkoutPlanValidationErrorKind.NO_ACTIVE_STEP)?.let { error ->
            item(key = "no-active") { FieldErrorText(error.message()) }
        }
        itemsIndexedWithKey(state.form.blocks) { index, block ->
            WorkoutPlanBlockCard(
                index = index,
                block = block,
                state = state,
                enabled = enabled,
                canMoveUp = index > 0,
                canMoveDown = index < state.form.blocks.lastIndex,
                onNameChanged = { viewModel.updateBlockName(block.id, it) },
                onRoundsChanged = { viewModel.updateBlockRounds(block.id, it) },
                onMoveBlock = { viewModel.moveBlock(block.id, it) },
                onRemoveBlock = { viewModel.removeBlock(block.id) },
                onAddExercise = { onOpenPicker(block.id) },
                onAddRest = { viewModel.addRestStep(block.id) },
                onStepGoalTypeChanged = { stepId, goalType -> viewModel.updateStepGoalType(block.id, stepId, goalType) },
                onStepGoalValueChanged = { stepId, text -> viewModel.updateStepGoalValue(block.id, stepId, text) },
                onStepDescriptionChanged = { stepId, text -> viewModel.updateStepDescription(block.id, stepId, text) },
                onMoveStep = { from, to -> viewModel.moveStep(block.id, from, to) },
                onRemoveStep = { stepId -> viewModel.removeStep(block.id, stepId) },
            )
        }
        item(key = "add-block") {
            OpenVitalsOutlinedButton(
                onClick = viewModel::addBlock,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(imageVector = Icons.Outlined.Add, contentDescription = null)
                Text(
                    text = stringResource(R.string.workout_plan_add_block),
                    modifier = Modifier.padding(start = Spacing.sm),
                )
            }
        }
        item(key = "save") {
            OpenVitalsButton(
                onClick = viewModel::save,
                enabled = enabled && (state.isDirty || !state.isEditing),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.action_save))
            }
        }
    }
}

private inline fun <T : Any> androidx.compose.foundation.lazy.LazyListScope.itemsIndexedWithKey(
    items: List<T>,
    crossinline content: @Composable (Int, T) -> Unit,
) {
    items.forEachIndexed { index, item ->
        item(key = (item as? WorkoutPlanBlockInput)?.id ?: index) { content(index, item) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkoutPlanSessionCard(
    state: WorkoutPlanBuilderUiState,
    viewModel: WorkoutPlanBuilderViewModel,
    enabled: Boolean,
) {
    val form = state.form
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var sessionTypeExpanded by remember { mutableStateOf(false) }
    val selectedDate = form.startDateText.toWorkoutPlanDateOrNull() ?: LocalDate.now()
    val selectedTime = form.startTimeText.toWorkoutPlanTimeOrNull() ?: LocalTime.now().withSecond(0).withNano(0)
    val titleError = state.errorFor(WorkoutPlanValidationErrorKind.TITLE_REQUIRED)
    val dateError = state.errorFor(WorkoutPlanValidationErrorKind.START_DATE_INVALID)
    val timeError = state.errorFor(WorkoutPlanValidationErrorKind.START_TIME_INVALID)
    val durationError = state.errorFor(WorkoutPlanValidationErrorKind.DURATION_INVALID)

    OpenVitalsCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            OutlinedTextField(
                value = form.titleText,
                onValueChange = viewModel::updateTitle,
                enabled = enabled,
                singleLine = true,
                isError = titleError != null,
                label = { Text(stringResource(R.string.activity_entry_title_label)) },
                modifier = Modifier.fillMaxWidth(),
            )
            FieldErrorText(titleError?.message())

            ExposedDropdownMenuBox(
                expanded = sessionTypeExpanded && enabled,
                onExpandedChange = { sessionTypeExpanded = enabled && it },
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = sessionTypeLabel(form.sessionExerciseType),
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    enabled = enabled,
                    label = { Text(stringResource(R.string.workout_plan_session_type_label)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sessionTypeExpanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = sessionTypeExpanded && enabled,
                    onDismissRequest = { sessionTypeExpanded = false },
                ) {
                    val options = (WorkoutPlanSessionTypes + form.sessionExerciseType).distinct()
                    options.forEach { exerciseType ->
                        DropdownMenuItem(
                            text = { Text(sessionTypeLabel(exerciseType)) },
                            onClick = {
                                viewModel.updateSessionType(exerciseType)
                                sessionTypeExpanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    ActivityPickerField(
                        label = stringResource(R.string.activity_entry_start_date_label),
                        value = selectedDate.localizedDateText(),
                        icon = Icons.Outlined.CalendarMonth,
                        enabled = enabled,
                        isError = dateError != null,
                        onClick = { showDatePicker = true },
                    )
                    FieldErrorText(dateError?.message())
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    ActivityPickerField(
                        label = stringResource(R.string.activity_entry_start_time_label),
                        value = selectedTime.localizedTimeText(),
                        icon = Icons.Outlined.Schedule,
                        enabled = enabled,
                        isError = timeError != null,
                        onClick = { showTimePicker = true },
                    )
                    FieldErrorText(timeError?.message())
                }
            }

            OutlinedTextField(
                value = form.durationMinutesText,
                onValueChange = viewModel::updateDurationMinutes,
                enabled = enabled,
                singleLine = true,
                isError = durationError != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                label = { Text(stringResource(R.string.activity_entry_duration_label)) },
                modifier = Modifier.fillMaxWidth(),
            )
            FieldErrorText(durationError?.message())

            OutlinedTextField(
                value = form.notesText,
                onValueChange = viewModel::updateNotes,
                enabled = enabled,
                minLines = 2,
                label = { Text(stringResource(R.string.activity_entry_notes_label)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (showDatePicker) {
        HealthDatePickerDialog(
            selectedDate = selectedDate,
            onDismiss = { showDatePicker = false },
            onConfirm = { date ->
                showDatePicker = false
                viewModel.updateStartDate(DateTimeFormatter.ISO_LOCAL_DATE.format(date))
            },
        )
    }
    if (showTimePicker) {
        ActivityTimePickerDialog(
            selectedTime = selectedTime,
            onDismiss = { showTimePicker = false },
            onConfirm = { time ->
                showTimePicker = false
                viewModel.updateStartTime(WorkoutPlanTimeFormatter.format(time))
            },
        )
    }
}
