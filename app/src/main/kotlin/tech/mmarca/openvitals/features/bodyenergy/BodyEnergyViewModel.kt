package tech.mmarca.openvitals.features.bodyenergy

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.performance.DefaultDispatcherProvider
import tech.mmarca.openvitals.core.performance.DispatcherProvider
import tech.mmarca.openvitals.core.performance.LoadCoordinator
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.PeriodSelection
import tech.mmarca.openvitals.core.period.PeriodSelectionDriver
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.period.WeekPeriodMode
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.core.presentation.toScreenError
import tech.mmarca.openvitals.data.repository.contract.BodyEnergyCalibrationPreferences
import tech.mmarca.openvitals.data.repository.contract.BodyProfilePreferences
import tech.mmarca.openvitals.data.repository.contract.BodyEnergyRepository
import tech.mmarca.openvitals.data.repository.contract.BodyEnergyTimelineQuery
import tech.mmarca.openvitals.data.repository.contract.BodyEnergyTimelineResult
import tech.mmarca.openvitals.data.sync.BodyEnergyChainSyncService
import tech.mmarca.openvitals.domain.model.RefreshMode
import tech.mmarca.openvitals.domain.preferences.BodyEnergyCalibration
import tech.mmarca.openvitals.domain.preferences.BodyProfile

@Immutable
data class BodyEnergyUiState(
    val isLoading: Boolean = true,
    val selectedRange: TimeRange = TimeRange.DAY,
    val selectedDate: LocalDate = LocalDate.now(),
    val result: BodyEnergyTimelineResult? = null,
    val display: BodyEnergyDisplayState = BodyEnergyDisplayState(),
    val calibration: BodyEnergyCalibration = BodyEnergyCalibration.Automatic,
    /** Only the birth year is read: v11 derives the zone ladder from age. */
    val bodyProfile: BodyProfile = BodyProfile(),
    val error: ScreenError? = null,
)

@HiltViewModel
class BodyEnergyViewModel @Inject constructor(
    private val repository: BodyEnergyRepository,
    private val calibrationPreferences: BodyEnergyCalibrationPreferences,
    private val bodyProfilePreferences: BodyProfilePreferences,
    private val chainSyncService: BodyEnergyChainSyncService,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider,
) : ViewModel() {

    private val periodDriver = PeriodSelectionDriver(
        initialRange = TimeRange.DAY,
        initialWeekPeriodMode = WeekPeriodMode.MONDAY_TO_SUNDAY,
        onRangeSelected = {},
    )
    private val loadCoordinator = LoadCoordinator()
    private val _uiState = MutableStateFlow(
        BodyEnergyUiState(
            selectedRange = TimeRange.DAY,
            selectedDate = periodDriver.selection.selectedDate,
            calibration = calibrationPreferences.bodyEnergyCalibration(),
            bodyProfile = bodyProfilePreferences.bodyProfile(),
        )
    )
    val uiState: StateFlow<BodyEnergyUiState> = _uiState.asStateFlow()

    init {
        observeCalibration()
        observeBodyProfile()
        observeChainRebuilt()
        load()
    }

    private fun observeCalibration() {
        viewModelScope.launch {
            calibrationPreferences.bodyEnergyCalibrationFlow.drop(1).collect { calibration ->
                // Zones change what a bucket means. The gains the watch learner nudges do not.
                val zonesChanged =
                    calibration.zoneSignature() != _uiState.value.calibration.zoneSignature()
                _uiState.value = _uiState.value.copy(calibration = calibration)
                if (zonesChanged) load()
            }
        }
    }

    private fun observeChainRebuilt() {
        viewModelScope.launch {
            // The rebuild dropped today's row; a normal load recomputes it on the new chain.
            chainSyncService.chainRebuilt.collect { load() }
        }
    }

    private fun observeBodyProfile() {
        viewModelScope.launch {
            bodyProfilePreferences.bodyProfileFlow.drop(1).collect { profile ->
                _uiState.value = _uiState.value.copy(bodyProfile = profile)
                load(RefreshMode.FORCE)
            }
        }
    }

    /** Commits the zone ladder and the birth year. The profile is written first: zones derive from age. */
    fun completeSetup(calibration: BodyEnergyCalibration, birthYear: Int?) {
        if (birthYear != null) {
            bodyProfilePreferences.setBodyProfile(
                bodyProfilePreferences.bodyProfile().copy(birthYear = birthYear)
            )
        }
        calibrationPreferences.setBodyEnergyCalibration(calibration.copy(setupCompleted = true))
        load(RefreshMode.FORCE)
    }

    /** Returns the gains to neutral and forgets the watch readings. */
    fun resetPersonalTuning() {
        val current = calibrationPreferences.bodyEnergyCalibration()
        calibrationPreferences.setBodyEnergyCalibration(
            current.copy(
                sleepChargeGain = 1.0,
                activityDrainGain = 1.0,
                basalDrainGain = 1.0,
                stressDrainGain = 1.0,
                watchObservationCount = 0,
                setupCompleted = true,
            )
        )
        load(RefreshMode.FORCE)
    }

    fun selectRange(range: TimeRange) {
        if (range != TimeRange.DAY) return
    }

    fun previousPeriod() {
        applyPeriodSelection(periodDriver.previousPeriod())
        load()
    }

    fun nextPeriod() {
        periodDriver.nextPeriod()?.let { selection ->
            applyPeriodSelection(selection)
            load()
        }
    }

    fun selectDate(date: LocalDate) {
        applyPeriodSelection(periodDriver.selectDate(date))
        load()
    }

    fun selectDay(date: LocalDate) {
        applyPeriodSelection(periodDriver.selectDay(date))
        load()
    }

    fun resumeCurrentPeriod(refreshCurrent: Boolean = false) {
        val selection = periodDriver.resumeCurrentPeriod()
        if (selection == null) {
            if (refreshCurrent) load(RefreshMode.FORCE)
            return
        }
        applyPeriodSelection(selection)
        load()
    }

    fun refresh() {
        load(RefreshMode.FORCE)
    }

    private fun load(refreshMode: RefreshMode = RefreshMode.NORMAL) {
        val selection = periodDriver.selection
        val period = DatePeriod(selection.selectedDate, selection.selectedDate)
        loadCoordinator.launch(viewModelScope) load@{
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
            )
            runCatching {
                repository.loadTimeline(
                    BodyEnergyTimelineQuery(
                        period = period,
                        range = TimeRange.DAY,
                        refreshMode = refreshMode,
                    )
                )
            }.onSuccess { result ->
                if (!isCurrent) return@load
                // The mapper walks the whole day's timeline. Keep it off Main.
                val display = withContext(dispatchers.default) { result.toBodyEnergyDisplayState() }
                if (!isCurrent) return@load
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    result = result,
                    display = display,
                    error = null,
                )
                // After the foreground load: Health Connect serializes reads.
                warmChain()
            }.onFailure { error ->
                if (!isCurrent) return@load
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = error.toScreenError(R.string.screen_error_load_body_energy),
                )
            }
        }
    }

    /** Best-effort, and its own throttle decides whether anything happens. */
    private fun warmChain() {
        viewModelScope.launch {
            runCatching { chainSyncService.syncAll() }
        }
    }

    private fun applyPeriodSelection(selection: PeriodSelection) {
        _uiState.value = _uiState.value.copy(
            selectedRange = TimeRange.DAY,
            selectedDate = selection.selectedDate,
        )
    }
}
