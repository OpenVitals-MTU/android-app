package tech.mmarca.openvitals.features.manualentry.cycle

import tech.mmarca.openvitals.features.manualentry.*
import tech.mmarca.openvitals.features.manualentry.activity.*
import tech.mmarca.openvitals.features.manualentry.activity.recording.*
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.*
import tech.mmarca.openvitals.features.manualentry.body.*
import tech.mmarca.openvitals.features.manualentry.hydration.*
import tech.mmarca.openvitals.features.manualentry.mindfulness.*
import tech.mmarca.openvitals.features.manualentry.vitals.*

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.features.manualentry.ManualEntryWritePermissionCallout
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.core.presentation.resolve
import tech.mmarca.openvitals.domain.model.CycleEntryKind
import tech.mmarca.openvitals.domain.preferences.UnitQuantity
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.ui.components.HealthDatePickerDialog
import tech.mmarca.openvitals.ui.components.OpenVitalsButton
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.theme.CycleColor

@Composable
fun CycleEntryScreen(
    viewModel: CycleEntryViewModel,
    unitFormatter: UnitFormatter,
    onEntrySaved: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val unitSystem = unitFormatter.unitSystem(UnitQuantity.TEMPERATURE)

    val requestWritePermissions = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) {
        viewModel.refreshPermission()
    }

    LaunchedEffect(Unit) {
        viewModel.start(unitSystem)
    }
    LaunchedEffect(state.saveCompleted) {
        if (state.saveCompleted) {
            viewModel.onSaveCompletedHandled()
            onEntrySaved()
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshPermission()
    }

    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
        item {
            CycleEntryCard(
                state = state,
                unitSystem = unitSystem,
                onDateChanged = viewModel::updateDate,
                onEntryTimeChanged = viewModel::updateEntryTime,
                onSelectSection = viewModel::selectSection,
                onSelectFlow = viewModel::selectFlow,
                onToggleSpotting = viewModel::toggleSpotting,
                onSelectSexualActivity = viewModel::selectSexualActivity,
                onSelectOvulation = viewModel::selectOvulation,
                onSelectMucusAppearance = viewModel::selectMucusAppearance,
                onSelectMucusSensation = viewModel::selectMucusSensation,
                onBbtInputChanged = viewModel::updateBbtInput,
                onSelectBbtLocation = viewModel::selectBbtLocation,
                onSave = { viewModel.save(unitSystem) },
                onRequestWritePermission = {
                    requestWritePermissions.launch(state.writePermissions)
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CycleEntryCard(
    state: CycleEntryUiState,
    unitSystem: UnitSystem,
    onDateChanged: (java.time.LocalDate) -> Unit,
    onEntryTimeChanged: (java.time.Instant) -> Unit,
    onSelectSection: (CycleEntryKind) -> Unit,
    onSelectFlow: (Int?) -> Unit,
    onToggleSpotting: () -> Unit,
    onSelectSexualActivity: (Int?) -> Unit,
    onSelectOvulation: (Int?) -> Unit,
    onSelectMucusAppearance: (Int?) -> Unit,
    onSelectMucusSensation: (Int?) -> Unit,
    onBbtInputChanged: (String) -> Unit,
    onSelectBbtLocation: (Int?) -> Unit,
    onSave: () -> Unit,
    onRequestWritePermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val anyGranted = state.grantedKinds.isNotEmpty()
    val saving = state.isSavingEntry
    val saveEnabled = anyGranted && !saving && !state.isCheckingPermission
    var showDatePicker by remember { mutableStateOf(false) }

    OpenVitalsCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    tint = CycleColor,
                    modifier = Modifier.size(22.dp),
                )
                Column(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .weight(1f),
                ) {
                    Text(
                        text = stringResource(R.string.metric_cycle),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = stringResource(R.string.cycle_entry_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (!anyGranted && !state.isCheckingPermission) {
                ManualEntryWritePermissionCallout(
                    body = stringResource(R.string.cycle_entry_permission_needed),
                    onGrant = onRequestWritePermission,
                )
            }

            if (state.isEditMode) {
                ManualEntryTimestampFields(
                    timestamp = state.editTime,
                    enabled = !saving,
                    onTimestampChanged = onEntryTimeChanged,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                ManualEntryPickerButton(
                    label = stringResource(R.string.manual_entry_date_label),
                    value = state.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                    icon = Icons.Outlined.CalendarMonth,
                    enabled = !saving,
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (!state.isEditMode) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CycleEntryKind.entries.forEach { kind ->
                        FilterChip(
                            selected = state.selectedSection == kind,
                            onClick = { onSelectSection(kind) },
                            label = { Text(stringResource(kind.categoryLabelRes())) },
                            enabled = !saving,
                        )
                    }
                }
            }

            val sections = setOf(state.activeSection)

            if (CycleEntryKind.MENSTRUATION_FLOW in sections) {
                CycleChipSection(
                    label = stringResource(R.string.cycle_entry_section_flow),
                    options = flowOptions(),
                    selection = state.flowSelection,
                    enabled = !saving && CycleEntryKind.MENSTRUATION_FLOW in state.grantedKinds,
                    onSelect = onSelectFlow,
                )
            }
            if (CycleEntryKind.SPOTTING in sections) {
                CycleToggleSection(
                    label = stringResource(R.string.cycle_observation_intermenstrual_bleeding),
                    chipLabel = stringResource(R.string.cycle_entry_section_spotting),
                    logged = state.spottingLogged,
                    enabled = !saving && CycleEntryKind.SPOTTING in state.grantedKinds,
                    onToggle = onToggleSpotting,
                )
            }
            if (CycleEntryKind.SEXUAL_ACTIVITY in sections) {
                CycleChipSection(
                    label = stringResource(R.string.cycle_entry_section_sexual_activity),
                    options = protectionOptions(),
                    selection = state.sexualActivitySelection,
                    enabled = !saving && CycleEntryKind.SEXUAL_ACTIVITY in state.grantedKinds,
                    onSelect = onSelectSexualActivity,
                )
            }
            if (CycleEntryKind.OVULATION_TEST in sections) {
                CycleChipSection(
                    label = stringResource(R.string.cycle_entry_section_ovulation),
                    options = ovulationOptions(),
                    selection = state.ovulationSelection,
                    enabled = !saving && CycleEntryKind.OVULATION_TEST in state.grantedKinds,
                    onSelect = onSelectOvulation,
                )
            }
            if (CycleEntryKind.CERVICAL_MUCUS in sections) {
                CycleChipSection(
                    label = stringResource(R.string.cycle_entry_section_mucus_appearance),
                    options = mucusAppearanceOptions(),
                    selection = state.mucusAppearance,
                    enabled = !saving && CycleEntryKind.CERVICAL_MUCUS in state.grantedKinds,
                    onSelect = onSelectMucusAppearance,
                )
                CycleChipSection(
                    label = stringResource(R.string.cycle_entry_section_mucus_sensation),
                    options = mucusSensationOptions(),
                    selection = state.mucusSensation,
                    enabled = !saving && CycleEntryKind.CERVICAL_MUCUS in state.grantedKinds,
                    onSelect = onSelectMucusSensation,
                )
            }
            if (CycleEntryKind.BASAL_BODY_TEMPERATURE in sections) {
                CycleBbtSection(
                    label = stringResource(
                        R.string.cycle_entry_section_bbt,
                        if (unitSystem == UnitSystem.IMPERIAL) "deg F" else "deg C",
                    ),
                    inputText = state.bbtInputText,
                    location = state.bbtLocation,
                    enabled = !saving && CycleEntryKind.BASAL_BODY_TEMPERATURE in state.grantedKinds,
                    onInputChanged = onBbtInputChanged,
                    onLocationSelected = onSelectBbtLocation,
                )
            }

            OpenVitalsButton(
                onClick = onSave,
                enabled = saveEnabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = if (state.isEditMode) Icons.Outlined.Check else Icons.Outlined.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = if (state.isEditMode) {
                        stringResource(R.string.action_save)
                    } else {
                        stringResource(R.string.cycle_log_action)
                    },
                    modifier = Modifier.padding(start = 6.dp),
                )
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
                onDateChanged(date)
            },
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

private fun CycleEntryKind.categoryLabelRes(): Int = when (this) {
    CycleEntryKind.MENSTRUATION_FLOW -> R.string.cycle_entry_section_flow
    CycleEntryKind.SPOTTING -> R.string.cycle_observation_intermenstrual_bleeding
    CycleEntryKind.SEXUAL_ACTIVITY -> R.string.cycle_observation_sexual_activity
    CycleEntryKind.OVULATION_TEST -> R.string.cycle_observation_ovulation_test
    CycleEntryKind.CERVICAL_MUCUS -> R.string.cycle_observation_cervical_mucus
    CycleEntryKind.BASAL_BODY_TEMPERATURE -> R.string.cycle_observation_basal_body_temperature
}
