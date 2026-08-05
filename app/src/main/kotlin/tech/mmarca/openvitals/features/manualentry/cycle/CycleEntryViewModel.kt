package tech.mmarca.openvitals.features.manualentry.cycle

import tech.mmarca.openvitals.features.manualentry.*
import tech.mmarca.openvitals.features.manualentry.activity.*
import tech.mmarca.openvitals.features.manualentry.activity.recording.*
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.*
import tech.mmarca.openvitals.features.manualentry.body.*
import tech.mmarca.openvitals.features.manualentry.hydration.*
import tech.mmarca.openvitals.features.manualentry.mindfulness.*
import tech.mmarca.openvitals.features.manualentry.vitals.*

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.core.presentation.toScreenError
import tech.mmarca.openvitals.data.repository.contract.CycleRepository
import tech.mmarca.openvitals.domain.model.CycleEntryKind
import tech.mmarca.openvitals.domain.model.CycleEntryWriteRequest
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.navigation.CYCLE_ENTRY_ID_ARG
import tech.mmarca.openvitals.navigation.CYCLE_ENTRY_KIND_ARG

enum class CycleEntryError {
    NOTHING_TO_SAVE,
    INVALID_VALUE,
    MISSING_WRITE_PERMISSION,
    WRITE_FAILED,
}

@Immutable
data class CycleEntryUiState(
    val date: LocalDate = LocalDate.now(),
    val flowSelection: Int? = null,
    val spottingLogged: Boolean = false,
    val sexualActivitySelection: Int? = null,
    val ovulationSelection: Int? = null,
    val mucusAppearance: Int? = null,
    val mucusSensation: Int? = null,
    val bbtInputText: String = "",
    val bbtLocation: Int? = null,
    val writePermissions: Set<String> = emptySet(),
    val grantedKinds: Set<CycleEntryKind> = emptySet(),
    val isCheckingPermission: Boolean = true,
    val isSavingEntry: Boolean = false,
    val editKind: CycleEntryKind? = null,
    val editRecordId: String? = null,
    val editTime: Instant? = null,
    val saveCompleted: Boolean = false,
    val entryError: CycleEntryError? = null,
    val writeError: ScreenError? = null,
) {
    val isEditMode: Boolean
        get() = editRecordId != null

    val filledKinds: Set<CycleEntryKind>
        get() = buildSet {
            if (flowSelection != null) add(CycleEntryKind.MENSTRUATION_FLOW)
            if (spottingLogged) add(CycleEntryKind.SPOTTING)
            if (sexualActivitySelection != null) add(CycleEntryKind.SEXUAL_ACTIVITY)
            if (ovulationSelection != null) add(CycleEntryKind.OVULATION_TEST)
            if (mucusAppearance != null || mucusSensation != null) add(CycleEntryKind.CERVICAL_MUCUS)
            if (bbtInputText.isNotBlank()) add(CycleEntryKind.BASAL_BODY_TEMPERATURE)
        }
}

@HiltViewModel
class CycleEntryViewModel @Inject constructor(
    private val repository: CycleRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    constructor(repository: CycleRepository) : this(repository, SavedStateHandle())

    private val editKind: CycleEntryKind? =
        savedStateHandle.get<String>(CYCLE_ENTRY_KIND_ARG)?.toCycleEntryKindOrNull()
    private val editRecordId: String? = savedStateHandle[CYCLE_ENTRY_ID_ARG]

    private val _uiState = MutableStateFlow(
        CycleEntryUiState(editKind = editKind, editRecordId = editRecordId)
    )
    val uiState: StateFlow<CycleEntryUiState> = _uiState.asStateFlow()

    private var editEntryLoaded = false

    fun start(unitSystem: UnitSystem = UnitSystem.METRIC) {
        refreshPermission()
        if (editRecordId != null && !editEntryLoaded) {
            loadEditEntry(unitSystem)
        }
    }

    fun refreshPermission() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isCheckingPermission = true,
                entryError = null,
                writeError = null,
            )
            runCatching {
                val kinds = editKind?.let(::setOf) ?: CycleEntryKind.entries.toSet()
                val granted = kinds.filterTo(mutableSetOf()) { repository.hasCycleWritePermission(it) }
                val permissions = kinds.flatMapTo(mutableSetOf()) { repository.cycleWritePermissions(it) }
                permissions to granted
            }.onSuccess { (writePermissions, grantedKinds) ->
                _uiState.value = _uiState.value.copy(
                    isCheckingPermission = false,
                    writePermissions = writePermissions,
                    grantedKinds = grantedKinds,
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isCheckingPermission = false,
                    grantedKinds = emptySet(),
                    entryError = CycleEntryError.WRITE_FAILED,
                    writeError = error.toScreenError(),
                )
            }
        }
    }

    fun updateDate(date: LocalDate) {
        update { copy(date = minOf(date, LocalDate.now())) }
    }

    fun selectFlow(flow: Int?) = update { copy(flowSelection = flow) }

    fun toggleSpotting() = update { copy(spottingLogged = !spottingLogged) }

    fun selectSexualActivity(protection: Int?) = update { copy(sexualActivitySelection = protection) }

    fun selectOvulation(result: Int?) = update { copy(ovulationSelection = result) }

    fun selectMucusAppearance(appearance: Int?) = update { copy(mucusAppearance = appearance) }

    fun selectMucusSensation(sensation: Int?) = update { copy(mucusSensation = sensation) }

    fun updateBbtInput(text: String) = update { copy(bbtInputText = text) }

    fun selectBbtLocation(location: Int?) = update { copy(bbtLocation = location) }

    fun updateEntryTime(time: Instant) {
        update { copy(editTime = time.coerceAtMost(Instant.now())) }
    }

    fun save(unitSystem: UnitSystem = UnitSystem.METRIC) {
        val current = _uiState.value
        val kinds = if (current.isEditMode) setOfNotNull(current.editKind) else current.filledKinds
        if (kinds.isEmpty()) {
            _uiState.value = current.copy(entryError = CycleEntryError.NOTHING_TO_SAVE, writeError = null)
            return
        }
        if (kinds.any { it !in current.grantedKinds }) {
            _uiState.value = current.copy(
                entryError = CycleEntryError.MISSING_WRITE_PERMISSION,
                writeError = null,
            )
            return
        }
        val bbtCelsius = current.bbtCelsiusOrNull(unitSystem)
        if (CycleEntryKind.BASAL_BODY_TEMPERATURE in kinds &&
            (bbtCelsius == null || bbtCelsius !in MinBbtCelsius..MaxBbtCelsius)
        ) {
            _uiState.value = current.copy(entryError = CycleEntryError.INVALID_VALUE, writeError = null)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSavingEntry = true, entryError = null, writeError = null)
            val time = current.entryInstant()
            var firstError: Throwable? = null
            val savedKinds = mutableSetOf<CycleEntryKind>()

            for (kind in kinds) {
                val request = current.toWriteRequest(kind, time, bbtCelsius)
                runCatching {
                    val recordId = current.editRecordId
                    if (recordId == null) {
                        repository.writeCycleEntry(request)
                    } else {
                        repository.updateCycleEntry(recordId, request)
                    }
                }.onSuccess {
                    savedKinds.add(kind)
                }.onFailure { error ->
                    if (firstError == null) firstError = error
                }
            }

            val error = firstError
            _uiState.value = if (error == null) {
                val cleared = if (_uiState.value.isEditMode) {
                    _uiState.value
                } else {
                    _uiState.value.clearSections(savedKinds)
                }
                cleared.copy(isSavingEntry = false, saveCompleted = true, entryError = null, writeError = null)
            } else {
                val cleared = if (_uiState.value.isEditMode) {
                    _uiState.value
                } else {
                    _uiState.value.clearSections(savedKinds)
                }
                cleared.copy(
                    isSavingEntry = false,
                    entryError = CycleEntryError.WRITE_FAILED,
                    writeError = error.toScreenError(),
                )
            }
        }
    }

    fun onSaveCompletedHandled() {
        _uiState.value = _uiState.value.copy(saveCompleted = false)
    }

    private fun loadEditEntry(unitSystem: UnitSystem) {
        val kind = editKind ?: return
        val recordId = editRecordId ?: return
        editEntryLoaded = true
        viewModelScope.launch {
            runCatching {
                repository.loadCycleEntry(kind, recordId)
            }.onSuccess { entry ->
                if (entry == null || !entry.isOpenVitalsEntry) {
                    _uiState.value = _uiState.value.copy(
                        entryError = CycleEntryError.WRITE_FAILED,
                        writeError = ScreenError.Message("Only OpenVitals entries can be edited."),
                    )
                    return@onSuccess
                }
                _uiState.value = _uiState.value.copy(
                    flowSelection = entry.flow,
                    spottingLogged = kind == CycleEntryKind.SPOTTING,
                    sexualActivitySelection = entry.protectionUsed,
                    ovulationSelection = entry.ovulationTestResult,
                    mucusAppearance = entry.mucusAppearance,
                    mucusSensation = entry.mucusSensation,
                    bbtInputText = entry.temperatureCelsius?.toBbtInput(unitSystem).orEmpty(),
                    bbtLocation = entry.measurementLocation,
                    editTime = entry.time.coerceAtMost(Instant.now()),
                    entryError = null,
                    writeError = null,
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    entryError = CycleEntryError.WRITE_FAILED,
                    writeError = error.toScreenError(),
                )
            }
        }
    }

    private inline fun update(transform: CycleEntryUiState.() -> CycleEntryUiState) {
        _uiState.value = _uiState.value.transform().copy(
            saveCompleted = false,
            entryError = null,
            writeError = null,
        )
    }
}

fun String.toCycleEntryKindOrNull(): CycleEntryKind? =
    CycleEntryKind.entries.firstOrNull { it.name == this }

private fun CycleEntryUiState.entryInstant(): Instant {
    if (isEditMode) return editTime?.coerceAtMost(Instant.now()) ?: Instant.now()
    val zone = ZoneId.systemDefault()
    return if (date == LocalDate.now()) {
        Instant.now()
    } else {
        date.atTime(LocalTime.NOON).atZone(zone).toInstant()
    }
}

private fun CycleEntryUiState.toWriteRequest(
    kind: CycleEntryKind,
    time: Instant,
    bbtCelsius: Double?,
): CycleEntryWriteRequest = when (kind) {
    CycleEntryKind.MENSTRUATION_FLOW -> CycleEntryWriteRequest(kind, time, flow = flowSelection)
    CycleEntryKind.SPOTTING -> CycleEntryWriteRequest(kind, time)
    CycleEntryKind.SEXUAL_ACTIVITY -> CycleEntryWriteRequest(kind, time, protectionUsed = sexualActivitySelection)
    CycleEntryKind.OVULATION_TEST -> CycleEntryWriteRequest(kind, time, ovulationTestResult = ovulationSelection)
    CycleEntryKind.CERVICAL_MUCUS -> CycleEntryWriteRequest(
        kind,
        time,
        mucusAppearance = mucusAppearance,
        mucusSensation = mucusSensation,
    )
    CycleEntryKind.BASAL_BODY_TEMPERATURE -> CycleEntryWriteRequest(
        kind,
        time,
        temperatureCelsius = bbtCelsius,
        measurementLocation = bbtLocation,
    )
}

private fun CycleEntryUiState.clearSections(kinds: Set<CycleEntryKind>): CycleEntryUiState {
    var state = this
    for (kind in kinds) {
        state = when (kind) {
            CycleEntryKind.MENSTRUATION_FLOW -> state.copy(flowSelection = null)
            CycleEntryKind.SPOTTING -> state.copy(spottingLogged = false)
            CycleEntryKind.SEXUAL_ACTIVITY -> state.copy(sexualActivitySelection = null)
            CycleEntryKind.OVULATION_TEST -> state.copy(ovulationSelection = null)
            CycleEntryKind.CERVICAL_MUCUS -> state.copy(mucusAppearance = null, mucusSensation = null)
            CycleEntryKind.BASAL_BODY_TEMPERATURE -> state.copy(bbtInputText = "", bbtLocation = null)
        }
    }
    return state
}

private fun CycleEntryUiState.bbtCelsiusOrNull(unitSystem: UnitSystem): Double? {
    if (bbtInputText.isBlank()) return null
    val value = bbtInputText.replace(',', '.').toDoubleOrNull() ?: return null
    return if (unitSystem == UnitSystem.IMPERIAL) {
        (value - FahrenheitFreezingPoint) / FahrenheitPerCelsius
    } else {
        value
    }
}

private fun Double.toBbtInput(unitSystem: UnitSystem): String {
    val display = if (unitSystem == UnitSystem.IMPERIAL) {
        this * FahrenheitPerCelsius + FahrenheitFreezingPoint
    } else {
        this
    }
    return "%.2f".format(java.util.Locale.US, display).trimEnd('0').trimEnd('.')
}

internal const val MinBbtCelsius = 35.0
internal const val MaxBbtCelsius = 39.0
private const val FahrenheitFreezingPoint = 32.0
private const val FahrenheitPerCelsius = 1.8
