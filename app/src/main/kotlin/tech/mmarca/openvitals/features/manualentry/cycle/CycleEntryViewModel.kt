package tech.mmarca.openvitals.features.manualentry.cycle

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.core.presentation.toScreenError
import tech.mmarca.openvitals.data.repository.contract.CycleJournalRepository
import tech.mmarca.openvitals.data.repository.contract.CyclePreferences
import tech.mmarca.openvitals.data.repository.contract.CycleRepository
import tech.mmarca.openvitals.domain.cycle.CycleSymptom
import tech.mmarca.openvitals.domain.cycle.ObservationCatalog
import tech.mmarca.openvitals.domain.model.BbtDisturbance
import tech.mmarca.openvitals.domain.model.CervicalSensation
import tech.mmarca.openvitals.domain.model.CycleEntryKind
import tech.mmarca.openvitals.domain.model.HcgTestResult
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.features.cycle.reminders.CycleReminderSettings
import tech.mmarca.openvitals.features.homewidgets.HomeWidgetRefreshScheduler
import tech.mmarca.openvitals.navigation.CYCLE_ENTRY_DATE_ARG
import tech.mmarca.openvitals.navigation.CYCLE_ENTRY_PRESET_ARG
import tech.mmarca.openvitals.navigation.CYCLE_ENTRY_SECTION_ARG
import tech.mmarca.openvitals.navigation.CycleEntryPreset

enum class CycleEntryError {
    NOTHING_TO_SAVE,
    INVALID_VALUE,
    MISSING_WRITE_PERMISSION,
    WRITE_FAILED,
}

/**
 * The day log. One day, every observation, saved together. [form] is what the
 * user typed; [loadedForm] is what the day held when it opened.
 */
@Immutable
data class CycleEntryUiState(
    val date: LocalDate = LocalDate.now(),
    /** The one section to show. Null shows them all. */
    val section: CycleEntrySection? = null,
    val form: CycleDayForm = CycleDayForm(),
    val loadedForm: CycleDayForm = CycleDayForm(),
    val offeredSymptoms: List<CycleSymptom> = ObservationCatalog.symptomsFor(emptySet()),
    val previousDaySymptoms: Set<CycleSymptom> = emptySet(),
    /** The heaviest flow another app recorded that day. Shown, never edited. */
    val foreignFlowLevel: Int? = null,
    val foreignSpotting: Boolean = false,
    val showMore: Boolean = false,
    val showBiomarkers: Boolean = false,
    val writePermissions: Set<String> = emptySet(),
    val grantedKinds: Set<CycleEntryKind> = emptySet(),
    val isCheckingPermission: Boolean = true,
    val isLoadingDay: Boolean = true,
    val isSavingEntry: Boolean = false,
    val saveCompleted: Boolean = false,
    val entryError: CycleEntryError? = null,
    val writeError: ScreenError? = null,
) {
    val hasChanges: Boolean
        get() = form != loadedForm

    /** True when an unsaved day log would be lost. */
    val shouldConfirmDiscard: Boolean
        get() = hasChanges && !isSavingEntry

    /** True when what this screen can write needs a Health Connect permission that is missing. */
    val lacksWritePermission: Boolean
        get() = when (val shown = section) {
            null -> grantedKinds.isEmpty()
            else -> shown.writeKinds.any { it !in grantedKinds }
        }
}

@HiltViewModel
class CycleEntryViewModel @Inject constructor(
    private val repository: CycleRepository,
    private val journal: CycleJournalRepository,
    private val preferences: CyclePreferences,
    private val reminders: CycleReminderSettings,
    savedStateHandle: SavedStateHandle,
    private val homeWidgetRefreshScheduler: HomeWidgetRefreshScheduler? = null,
) : ViewModel() {
    constructor(
        repository: CycleRepository,
        journal: CycleJournalRepository,
        preferences: CyclePreferences,
        reminders: CycleReminderSettings,
    ) : this(repository, journal, preferences, reminders, SavedStateHandle())

    private val requestedDate: LocalDate? =
        savedStateHandle.get<String>(CYCLE_ENTRY_DATE_ARG)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    /** Applied once, to the first day loaded, and only where nothing is recorded yet. */
    private var pendingPreset: String? = savedStateHandle[CYCLE_ENTRY_PRESET_ARG]

    private val _uiState = MutableStateFlow(
        CycleEntryUiState(
            date = requestedDate?.coerceAtMost(LocalDate.now()) ?: LocalDate.now(),
            section = CycleEntrySection.fromRoute(savedStateHandle[CYCLE_ENTRY_SECTION_ARG]),
        ),
    )
    val uiState: StateFlow<CycleEntryUiState> = _uiState.asStateFlow()

    private var dayLoaded = false

    fun start(unitSystem: UnitSystem = UnitSystem.METRIC) {
        refreshPermission()
        if (!dayLoaded) {
            dayLoaded = true
            loadDay(_uiState.value.date, unitSystem)
        }
    }

    fun refreshPermission() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCheckingPermission = true, entryError = null, writeError = null)
            runCatching {
                val kinds = CycleEntryKind.entries.toSet()
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

    /** Switches the day. Unsaved changes of the previous day are dropped; the screen asks first. */
    fun updateDate(date: LocalDate, unitSystem: UnitSystem = UnitSystem.METRIC) {
        loadDay(minOf(date, LocalDate.now()), unitSystem)
    }

    fun setBleeding(option: BleedingOption?) = updateForm { copy(bleeding = option) }

    fun setPain(level: Int?) = updateForm { copy(pain = level) }

    fun setMood(level: Int?) = updateForm { copy(mood = level) }

    fun setEnergy(level: Int?) = updateForm { copy(energy = level) }

    fun toggleSymptom(symptom: CycleSymptom) =
        updateForm { copy(symptoms = if (symptom in symptoms) symptoms - symptom else symptoms + symptom) }

    fun copyPreviousDaySymptoms() =
        updateForm { copy(symptoms = symptoms + _uiState.value.previousDaySymptoms) }

    fun setNotes(text: String) = updateForm { copy(notes = text) }

    fun toggleMore() = update { copy(showMore = !showMore) }

    fun toggleBiomarkers() = update { copy(showBiomarkers = !showBiomarkers) }

    fun setBbtInput(text: String) = updateForm { copy(bbtInputText = text) }

    fun setBbtLocation(location: Int?) = updateForm { copy(bbtLocation = location) }

    fun setBbtTime(time: LocalTime?) = updateForm { copy(bbtTime = time?.withSecond(0)?.withNano(0)) }

    fun toggleBbtDisturbance(disturbance: BbtDisturbance) = updateForm {
        copy(bbtDisturbances = if (disturbance in bbtDisturbances) bbtDisturbances - disturbance else bbtDisturbances + disturbance)
    }

    fun setCervicalSensation(sensation: CervicalSensation?) = updateForm { copy(cervicalSensation = sensation) }

    fun setMucusAppearance(appearance: Int?) = updateForm { copy(mucusAppearance = appearance) }

    fun setMucusAmount(amount: Int?) = updateForm { copy(mucusAmount = amount) }

    fun setOvulation(result: Int?) = updateForm { copy(ovulationResult = result) }

    fun setHcgTest(result: HcgTestResult?) = updateForm { copy(hcgTest = result) }

    fun setSexualActivity(protection: Int?) = updateForm { copy(sexualActivityProtection = protection) }

    fun save(unitSystem: UnitSystem = UnitSystem.METRIC) {
        val current = _uiState.value
        val form = current.form
        if (form.isEmpty && current.loadedForm.isEmpty) {
            _uiState.value = current.copy(entryError = CycleEntryError.NOTHING_TO_SAVE, writeError = null)
            return
        }
        val celsius = form.bbtCelsius(unitSystem)
        if (form.bbtInputText.isNotBlank() && (celsius == null || celsius !in CycleDayForm.MinBbtCelsius..CycleDayForm.MaxBbtCelsius)) {
            _uiState.value = current.copy(entryError = CycleEntryError.INVALID_VALUE, writeError = null)
            return
        }
        val changedKinds = form.changedKinds(current.loadedForm, unitSystem)
        if (changedKinds.any { it !in current.grantedKinds }) {
            _uiState.value = current.copy(entryError = CycleEntryError.MISSING_WRITE_PERMISSION, writeError = null)
            return
        }

        viewModelScope.launch {
            _uiState.value = current.copy(isSavingEntry = true, entryError = null, writeError = null)
            runCatching {
                repository.saveDayLog(current.date, form.toWrite(current.date, unitSystem))
            }.onSuccess {
                // A logged day silences today's reminder and may move the estimate; the widget shows both.
                reminders.applyStoredConfig()
                homeWidgetRefreshScheduler?.refreshNow()
                _uiState.value = _uiState.value.copy(
                    isSavingEntry = false,
                    loadedForm = form,
                    saveCompleted = true,
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
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

    private fun loadDay(date: LocalDate, unitSystem: UnitSystem) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(date = date, isLoadingDay = true, entryError = null, writeError = null)
            runCatching {
                val log = repository.loadDayLog(date)
                val previous = journal.entry(date.minusDays(1))?.symptoms.orEmpty()
                Triple(CycleDayForm.fromLog(log, unitSystem), log, previous)
            }.onSuccess { (loaded, log, previous) ->
                if (_uiState.value.date != date) return@onSuccess
                val catalog = ObservationCatalog.symptomsFor(preferences.cycleTrackingProfile().contexts)
                val preset = pendingPreset.also { pendingPreset = null }
                val form = when {
                    preset == CycleEntryPreset.PERIOD_START && loaded.bleeding == null && log.foreignFlowLevel == null ->
                        loaded.copy(bleeding = BleedingOption.LIGHT)
                    else -> loaded
                }
                _uiState.value = _uiState.value.copy(
                    form = form,
                    loadedForm = loaded,
                    offeredSymptoms = (catalog + form.symptoms).distinct(),
                    previousDaySymptoms = previous,
                    foreignFlowLevel = log.foreignFlowLevel,
                    foreignSpotting = log.foreignSpotting,
                    showMore = _uiState.value.showMore || form.symptoms.isNotEmpty() || form.notes.isNotBlank() || form.hcgTest != null,
                    showBiomarkers = _uiState.value.showBiomarkers || form.bbtInputText.isNotBlank() ||
                        form.hasCervicalMucus || form.cervicalSensation != null || form.ovulationResult != null ||
                        form.sexualActivityProtection != null,
                    isLoadingDay = false,
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoadingDay = false,
                    entryError = CycleEntryError.WRITE_FAILED,
                    writeError = error.toScreenError(),
                )
            }
        }
    }

    private inline fun updateForm(transform: CycleDayForm.() -> CycleDayForm) =
        update { copy(form = form.transform()) }

    private inline fun update(transform: CycleEntryUiState.() -> CycleEntryUiState) {
        _uiState.value = _uiState.value.transform().copy(saveCompleted = false, entryError = null, writeError = null)
    }
}
