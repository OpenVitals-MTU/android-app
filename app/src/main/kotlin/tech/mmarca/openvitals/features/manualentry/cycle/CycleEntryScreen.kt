package tech.mmarca.openvitals.features.manualentry.cycle

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.core.presentation.resolve
import tech.mmarca.openvitals.domain.preferences.UnitQuantity
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.features.manualentry.ManualEntryPickerButton
import tech.mmarca.openvitals.features.manualentry.ManualEntryWritePermissionCallout
import tech.mmarca.openvitals.features.manualentry.localizedDateText
import tech.mmarca.openvitals.features.manualentry.rememberManualEntryWritePermissionRequester
import tech.mmarca.openvitals.ui.components.HealthDatePickerDialog
import tech.mmarca.openvitals.ui.components.OpenVitalsButton
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.OpenVitalsOutlinedButton
import tech.mmarca.openvitals.ui.theme.CycleColor
import tech.mmarca.openvitals.ui.theme.LayoutMetrics
import tech.mmarca.openvitals.ui.theme.Spacing

private val HeaderIconSize: Dp = 22.dp
private val ButtonIconSize: Dp = 18.dp

/** The day log: one day, every observation, saved together. */
@Composable
fun CycleEntryScreen(
    viewModel: CycleEntryViewModel,
    unitFormatter: UnitFormatter,
    onEntrySaved: () -> Unit = {},
    onLeave: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val unitSystem = unitFormatter.unitSystem(UnitQuantity.TEMPERATURE)
    var showDiscardConfirmation by remember { mutableStateOf(false) }

    val requestWritePermissions = rememberManualEntryWritePermissionRequester {
        viewModel.refreshPermission()
    }

    LaunchedEffect(Unit) { viewModel.start(unitSystem) }
    LaunchedEffect(state.saveCompleted) {
        if (state.saveCompleted) {
            viewModel.onSaveCompletedHandled()
            onEntrySaved()
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshPermission() }

    // Back is guarded only when there is something to lose.
    BackHandler(enabled = state.shouldConfirmDiscard) { showDiscardConfirmation = true }

    LazyColumn(contentPadding = PaddingValues(vertical = Spacing.sm)) {
        item {
            CycleEntryCard(
                state = state,
                unitSystem = unitSystem,
                actions = CycleEntryActions(viewModel, unitSystem),
                onSave = { viewModel.save(unitSystem) },
                onRequestWritePermission = { requestWritePermissions.launch(state.writePermissions) },
                modifier = Modifier.padding(horizontal = LayoutMetrics.screenGutter, vertical = Spacing.sm),
            )
        }
    }

    if (showDiscardConfirmation) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirmation = false },
            title = { Text(stringResource(R.string.cycle_entry_discard_title)) },
            text = { Text(stringResource(R.string.cycle_entry_discard_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardConfirmation = false
                        onLeave()
                    },
                ) { Text(stringResource(R.string.cycle_entry_discard_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirmation = false }) {
                    Text(stringResource(R.string.cycle_entry_keep_editing))
                }
            },
        )
    }
}

/** The callbacks the card needs, gathered so the card's signature stays readable. */
internal data class CycleEntryActions(
    val onDateChanged: (LocalDate) -> Unit,
    val onBleeding: (BleedingOption?) -> Unit,
    val onPain: (Int?) -> Unit,
    val onMood: (Int?) -> Unit,
    val onEnergy: (Int?) -> Unit,
    val onToggleMore: () -> Unit,
    val onToggleBiomarkers: () -> Unit,
    val onToggleSymptom: (tech.mmarca.openvitals.domain.cycle.CycleSymptom) -> Unit,
    val onCopyPreviousDay: () -> Unit,
    val onNotes: (String) -> Unit,
    val onHcgTest: (tech.mmarca.openvitals.domain.model.HcgTestResult?) -> Unit,
    val onBbtInput: (String) -> Unit,
    val onBbtLocation: (Int?) -> Unit,
    val onBbtTime: (java.time.LocalTime?) -> Unit,
    val onToggleBbtDisturbance: (tech.mmarca.openvitals.domain.model.BbtDisturbance) -> Unit,
    val onCervicalSensation: (tech.mmarca.openvitals.domain.model.CervicalSensation?) -> Unit,
    val onMucusAppearance: (Int?) -> Unit,
    val onMucusAmount: (Int?) -> Unit,
    val onOvulation: (Int?) -> Unit,
    val onSexualActivity: (Int?) -> Unit,
) {
    constructor(viewModel: CycleEntryViewModel, unitSystem: UnitSystem) : this(
        onDateChanged = { viewModel.updateDate(it, unitSystem) },
        onBleeding = viewModel::setBleeding,
        onPain = viewModel::setPain,
        onMood = viewModel::setMood,
        onEnergy = viewModel::setEnergy,
        onToggleMore = viewModel::toggleMore,
        onToggleBiomarkers = viewModel::toggleBiomarkers,
        onToggleSymptom = viewModel::toggleSymptom,
        onCopyPreviousDay = viewModel::copyPreviousDaySymptoms,
        onNotes = viewModel::setNotes,
        onHcgTest = viewModel::setHcgTest,
        onBbtInput = viewModel::setBbtInput,
        onBbtLocation = viewModel::setBbtLocation,
        onBbtTime = viewModel::setBbtTime,
        onToggleBbtDisturbance = viewModel::toggleBbtDisturbance,
        onCervicalSensation = viewModel::setCervicalSensation,
        onMucusAppearance = viewModel::setMucusAppearance,
        onMucusAmount = viewModel::setMucusAmount,
        onOvulation = viewModel::setOvulation,
        onSexualActivity = viewModel::setSexualActivity,
    )

    companion object {
        /** No-op callbacks, for previews and tests. */
        val None = CycleEntryActions(
            {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
        )
    }
}

@Composable
internal fun CycleEntryCard(
    state: CycleEntryUiState,
    unitSystem: UnitSystem,
    actions: CycleEntryActions,
    onSave: () -> Unit,
    onRequestWritePermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val anyGranted = state.grantedKinds.isNotEmpty()
    val saving = state.isSavingEntry
    val editable = !saving && !state.isLoadingDay
    var showDatePicker by remember { mutableStateOf(false) }
    val form = state.form

    OpenVitalsCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(LayoutMetrics.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            CycleEntryHeader(anyGranted = anyGranted, isCheckingPermission = state.isCheckingPermission, onRequestWritePermission = onRequestWritePermission)

            ManualEntryPickerButton(
                label = stringResource(R.string.manual_entry_date_label),
                value = state.date.localizedDateText(),
                icon = Icons.Outlined.CalendarMonth,
                enabled = editable,
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth(),
            )

            CycleEntryBleedingSection(
                selected = form.bleeding,
                enabled = editable,
                foreignFlowLevel = state.foreignFlowLevel,
                foreignSpotting = state.foreignSpotting,
                onSelect = actions.onBleeding,
            )

            CycleEntryScales(
                form = form,
                enabled = editable,
                onPain = actions.onPain,
                onMood = actions.onMood,
                onEnergy = actions.onEnergy,
            )

            CycleEntryToggle(
                expanded = state.showMore,
                showLabel = stringResource(R.string.cycle_entry_more_show),
                hideLabel = stringResource(R.string.cycle_entry_more_hide),
                onToggle = actions.onToggleMore,
            )
            if (state.showMore) {
                CycleEntrySymptoms(
                    selected = form.symptoms,
                    offered = state.offeredSymptoms,
                    previousDay = state.previousDaySymptoms,
                    enabled = editable,
                    onToggle = actions.onToggleSymptom,
                    onCopyPreviousDay = actions.onCopyPreviousDay,
                )
                CycleEntryNotes(notes = form.notes, enabled = editable, onNotesChanged = actions.onNotes)
                CycleEntryHcgTest(selected = form.hcgTest, enabled = editable, onSelect = actions.onHcgTest)
            }

            CycleEntryToggle(
                expanded = state.showBiomarkers,
                showLabel = stringResource(R.string.cycle_entry_biomarkers_show),
                hideLabel = stringResource(R.string.cycle_entry_biomarkers_hide),
                onToggle = actions.onToggleBiomarkers,
            )
            if (state.showBiomarkers) {
                CycleEntryBiomarkers(form = form, state = state, unitSystem = unitSystem, editable = editable, actions = actions)
            }

            OpenVitalsButton(
                onClick = onSave,
                enabled = editable && !state.isCheckingPermission,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(imageVector = Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(ButtonIconSize))
                Text(text = stringResource(R.string.cycle_entry_save), modifier = Modifier.padding(start = Spacing.sm))
            }

            state.entryError?.let { entryError ->
                Text(
                    text = cycleEntryErrorText(entryError, state.writeError, unitSystem),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }

    if (showDatePicker) {
        HealthDatePickerDialog(
            selectedDate = state.date,
            onDismiss = { showDatePicker = false },
            onConfirm = { date ->
                showDatePicker = false
                actions.onDateChanged(date)
            },
        )
    }
}

@Composable
private fun CycleEntryHeader(
    anyGranted: Boolean,
    isCheckingPermission: Boolean,
    onRequestWritePermission: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Outlined.CalendarMonth,
            contentDescription = null,
            tint = CycleColor,
            modifier = Modifier.size(HeaderIconSize),
        )
        Column(
            modifier = Modifier
                .padding(horizontal = Spacing.md)
                .weight(1f),
        ) {
            Text(text = stringResource(R.string.cycle_entry_title), style = MaterialTheme.typography.titleSmall)
            Text(
                text = stringResource(R.string.cycle_entry_day_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    if (!anyGranted && !isCheckingPermission) {
        ManualEntryWritePermissionCallout(
            body = stringResource(R.string.cycle_entry_permission_needed),
            onGrant = onRequestWritePermission,
        )
    }
}

@Composable
private fun CycleEntryToggle(expanded: Boolean, showLabel: String, hideLabel: String, onToggle: () -> Unit) {
    OpenVitalsOutlinedButton(onClick = onToggle, modifier = Modifier.fillMaxWidth()) {
        Icon(
            imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
            contentDescription = null,
            modifier = Modifier.size(ButtonIconSize),
        )
        Text(text = if (expanded) hideLabel else showLabel, modifier = Modifier.padding(start = Spacing.sm))
    }
}

@Composable
private fun CycleEntryBiomarkers(
    form: CycleDayForm,
    state: CycleEntryUiState,
    unitSystem: UnitSystem,
    editable: Boolean,
    actions: CycleEntryActions,
) {
    CycleEntryBbtSection(
        form = form,
        unitSystem = unitSystem,
        enabled = editable,
        onInputChanged = actions.onBbtInput,
        onLocationSelected = actions.onBbtLocation,
        onTimeSelected = actions.onBbtTime,
        onToggleDisturbance = actions.onToggleBbtDisturbance,
    )
    CycleEntryCervicalFluidSection(
        form = form,
        enabled = editable,
        onSensation = actions.onCervicalSensation,
        onAppearance = actions.onMucusAppearance,
        onAmount = actions.onMucusAmount,
    )
    CycleChipSection(
        label = stringResource(R.string.cycle_entry_section_ovulation),
        options = ovulationOptions(),
        selection = form.ovulationResult,
        enabled = editable,
        onSelect = actions.onOvulation,
    )
    CycleChipSection(
        label = stringResource(R.string.cycle_entry_section_sexual_activity),
        options = protectionOptions(),
        selection = form.sexualActivityProtection,
        enabled = editable,
        onSelect = actions.onSexualActivity,
    )
    if (state.grantedKinds.size < tech.mmarca.openvitals.domain.model.CycleEntryKind.entries.size) {
        Text(
            text = stringResource(R.string.cycle_entry_partial_permission),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun cycleEntryErrorText(
    error: CycleEntryError,
    writeError: tech.mmarca.openvitals.core.presentation.ScreenError?,
    unitSystem: UnitSystem,
): String = when (error) {
    CycleEntryError.NOTHING_TO_SAVE -> stringResource(R.string.cycle_entry_nothing_to_save)
    CycleEntryError.INVALID_VALUE -> {
        val (min, max) = if (unitSystem == UnitSystem.IMPERIAL) "95" to "102.2" else "35" to "39"
        stringResource(R.string.cycle_entry_invalid_bbt, min, max)
    }
    CycleEntryError.MISSING_WRITE_PERMISSION -> stringResource(R.string.cycle_entry_permission_needed)
    CycleEntryError.WRITE_FAILED -> stringResource(
        R.string.cycle_entry_write_failed,
        writeError.resolve() ?: stringResource(R.string.unknown_error),
    )
}
