package tech.mmarca.openvitals.features.manualentry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.data.model.MindfulnessBackgroundSound
import tech.mmarca.openvitals.data.model.MindfulnessBellSound
import tech.mmarca.openvitals.data.model.MindfulnessSessionWriteRequest
import tech.mmarca.openvitals.data.model.MindfulnessTimerConfig
import tech.mmarca.openvitals.data.repository.MindfulnessRepository
import tech.mmarca.openvitals.data.repository.PreferencesRepository

private const val MinSessionMinutes = 1
private const val MaxSessionMinutes = 24 * 60
private const val TimerTickMillis = 1_000L
private const val BellPreviewMillis = 1_500L
private const val BackgroundPreviewMillis = 2_000L
private const val DefaultSessionTitle = "Meditation"

enum class MindfulnessEntryError {
    INVALID_TIMER,
    INVALID_MANUAL_ENTRY,
    TIMER_TOO_SHORT,
    MISSING_WRITE_PERMISSION,
    UNAVAILABLE,
    WRITE_FAILED,
}

data class MindfulnessBellEvent(
    val id: Long,
    val sound: MindfulnessBellSound,
    val previewMillis: Long? = null,
)

data class MindfulnessBackgroundEvent(
    val id: Long,
    val sound: MindfulnessBackgroundSound,
    val previewMillis: Long,
)

data class MindfulnessEntryUiState(
    val durationMinutesText: String = "",
    val intervalEnabled: Boolean = false,
    val intervalMinutesText: String = "",
    val bellSound: MindfulnessBellSound = MindfulnessBellSound.STRUCK,
    val backgroundSound: MindfulnessBackgroundSound = MindfulnessBackgroundSound.NONE,
    val writePermissions: Set<String> = emptySet(),
    val canWrite: Boolean = false,
    val mindfulnessAvailable: Boolean = true,
    val isCheckingPermission: Boolean = true,
    val isSavingEntry: Boolean = false,
    val isTimerRunning: Boolean = false,
    val isTimerPaused: Boolean = false,
    val timerCompleted: Boolean = false,
    val remainingSeconds: Int = 0,
    val totalSeconds: Int = 0,
    val manualMinutesText: String = "",
    val entryError: MindfulnessEntryError? = null,
    val writeErrorMessage: String? = null,
    val bellEvent: MindfulnessBellEvent? = null,
    val backgroundEvent: MindfulnessBackgroundEvent? = null,
)

@HiltViewModel
class MindfulnessEntryViewModel @Inject constructor(
    private val repository: MindfulnessRepository,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    private var timerJob: Job? = null
    private var timerStart: Instant? = null
    private var completedStart: Instant? = null
    private var completedEnd: Instant? = null
    private var bellEventId = 0L
    private var backgroundEventId = 0L

    private val _uiState = MutableStateFlow(initialState())
    val uiState: StateFlow<MindfulnessEntryUiState> = _uiState.asStateFlow()

    init {
        refreshPermission()
    }

    fun refreshPermission() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isCheckingPermission = true,
                entryError = null,
                writeErrorMessage = null,
            )
            runCatching {
                val available = repository.isMindfulnessAvailable()
                Triple(
                    available,
                    repository.mindfulnessWritePermissions,
                    repository.hasMindfulnessWritePermission(),
                )
            }.onSuccess { (available, permissions, canWrite) ->
                _uiState.value = _uiState.value.copy(
                    isCheckingPermission = false,
                    mindfulnessAvailable = available,
                    writePermissions = permissions,
                    canWrite = canWrite,
                    entryError = if (available) null else MindfulnessEntryError.UNAVAILABLE,
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isCheckingPermission = false,
                    mindfulnessAvailable = false,
                    writePermissions = repository.mindfulnessWritePermissions,
                    canWrite = false,
                    entryError = MindfulnessEntryError.UNAVAILABLE,
                    writeErrorMessage = error.message,
                )
            }
        }
    }

    fun updateDurationMinutes(text: String) {
        updateTimerFields {
            val duration = text.toPositiveIntOrNull()
            copy(
                durationMinutesText = text,
                totalSeconds = duration?.times(60) ?: totalSeconds,
                remainingSeconds = if (duration != null && !isTimerRunning && !timerCompleted) {
                    duration * 60
                } else {
                    remainingSeconds
                },
            )
        }
    }

    fun updateIntervalEnabled(enabled: Boolean) {
        updateTimerFields { copy(intervalEnabled = enabled) }
    }

    fun updateIntervalMinutes(text: String) {
        updateTimerFields { copy(intervalMinutesText = text) }
    }

    fun updateBellSound(sound: MindfulnessBellSound) {
        if (!canUpdateTimerFields()) return
        bellEventId += 1
        _uiState.value = _uiState.value.copy(
            bellSound = sound,
            entryError = null,
            writeErrorMessage = null,
            bellEvent = MindfulnessBellEvent(
                id = bellEventId,
                sound = sound,
                previewMillis = BellPreviewMillis,
            ),
        )
    }

    fun updateBackgroundSound(sound: MindfulnessBackgroundSound) {
        if (!canUpdateTimerFields()) return
        backgroundEventId += 1
        _uiState.value = _uiState.value.copy(
            backgroundSound = sound,
            entryError = null,
            writeErrorMessage = null,
            backgroundEvent = if (sound == MindfulnessBackgroundSound.NONE) {
                null
            } else {
                MindfulnessBackgroundEvent(
                    id = backgroundEventId,
                    sound = sound,
                    previewMillis = BackgroundPreviewMillis,
                )
            },
        )
    }

    fun updateManualMinutes(text: String) {
        _uiState.value = _uiState.value.copy(
            manualMinutesText = text,
            entryError = null,
            writeErrorMessage = null,
        )
    }

    fun startTimer() {
        val config = currentTimerConfigOrNull()
        if (config == null) {
            _uiState.value = _uiState.value.copy(
                entryError = MindfulnessEntryError.INVALID_TIMER,
                writeErrorMessage = null,
            )
            return
        }
        preferencesRepository.setMindfulnessTimerConfig(config)
        timerJob?.cancel()
        timerStart = Instant.now()
        completedStart = null
        completedEnd = null
        _uiState.value = _uiState.value.copy(
            durationMinutesText = config.durationMinutes.toString(),
            intervalEnabled = config.intervalMinutes != null,
            intervalMinutesText = config.intervalMinutes?.toString().orEmpty(),
            bellSound = config.bellSound,
            backgroundSound = config.backgroundSound,
            remainingSeconds = config.durationMinutes * 60,
            totalSeconds = config.durationMinutes * 60,
            isTimerRunning = true,
            isTimerPaused = false,
            timerCompleted = false,
            entryError = null,
            writeErrorMessage = null,
        )
        timerJob = viewModelScope.launch {
            runTimer(config)
        }
    }

    fun stopTimer() {
        if (!_uiState.value.isTimerRunning) return
        timerJob?.cancel()
        timerJob = null

        val state = _uiState.value
        val elapsedSeconds = (state.totalSeconds - state.remainingSeconds).coerceAtLeast(0)
        val start = timerStart ?: Instant.now().minusSeconds(elapsedSeconds.toLong())
        completedStart = start
        completedEnd = start.plusSeconds(elapsedSeconds.toLong())
        _uiState.value = _uiState.value.copy(
            isTimerRunning = false,
            isTimerPaused = true,
            timerCompleted = false,
            entryError = null,
            writeErrorMessage = null,
        )
    }

    fun resumeTimer() {
        val state = _uiState.value
        if (!state.isTimerPaused) return
        val config = currentTimerConfigOrNull()
        if (config == null || state.remainingSeconds <= 0) {
            _uiState.value = state.copy(
                entryError = MindfulnessEntryError.INVALID_TIMER,
                writeErrorMessage = null,
            )
            return
        }
        completedStart = null
        completedEnd = null
        _uiState.value = state.copy(
            isTimerRunning = true,
            isTimerPaused = false,
            timerCompleted = false,
            entryError = null,
            writeErrorMessage = null,
        )
        timerJob = viewModelScope.launch {
            runTimer(config)
        }
    }

    fun discardTimer() {
        timerJob?.cancel()
        timerJob = null
        timerStart = null
        completedStart = null
        completedEnd = null
        val duration = _uiState.value.durationMinutesText.toPositiveIntOrNull() ?: 0
        _uiState.value = _uiState.value.copy(
            isTimerRunning = false,
            isTimerPaused = false,
            timerCompleted = false,
            remainingSeconds = duration * 60,
            totalSeconds = duration * 60,
            entryError = null,
            writeErrorMessage = null,
        )
    }

    fun saveTimerSession() {
        val start = completedStart ?: return
        val end = completedEnd ?: return
        if (Duration.between(start, end).toMinutes() < MinSessionMinutes) {
            _uiState.value = _uiState.value.copy(
                entryError = MindfulnessEntryError.TIMER_TOO_SHORT,
                writeErrorMessage = null,
            )
            return
        }
        writeSession(
            title = currentSessionTitle(),
            start = start,
            end = end,
            onSuccess = {
                val duration = _uiState.value.durationMinutesText.toPositiveIntOrNull() ?: 0
                _uiState.value = _uiState.value.copy(
                    isSavingEntry = false,
                    isTimerPaused = false,
                    timerCompleted = false,
                    remainingSeconds = duration * 60,
                    totalSeconds = duration * 60,
                    entryError = null,
                    writeErrorMessage = null,
                )
                completedStart = null
                completedEnd = null
            },
        )
    }

    fun addManualEntry() {
        val minutes = _uiState.value.manualMinutesText.toPositiveIntOrNull()
        if (minutes == null || minutes !in MinSessionMinutes..MaxSessionMinutes) {
            _uiState.value = _uiState.value.copy(
                entryError = MindfulnessEntryError.INVALID_MANUAL_ENTRY,
                writeErrorMessage = null,
            )
            return
        }
        val end = Instant.now()
        val start = end.minus(Duration.ofMinutes(minutes.toLong()))
        writeSession(
            title = currentSessionTitle(),
            start = start,
            end = end,
            onSuccess = {
                _uiState.value = _uiState.value.copy(
                    isSavingEntry = false,
                    manualMinutesText = "",
                    entryError = null,
                    writeErrorMessage = null,
                )
            },
        )
    }

    private suspend fun runTimer(config: MindfulnessTimerConfig) {
        val totalSeconds = config.durationMinutes * 60
        val intervalSeconds = config.intervalMinutes?.times(60)
        var remainingSeconds = _uiState.value.remainingSeconds.takeIf { it in 1..totalSeconds } ?: totalSeconds
        while (remainingSeconds > 0) {
            delay(TimerTickMillis)
            remainingSeconds -= 1
            val elapsedSeconds = totalSeconds - remainingSeconds
            _uiState.value = _uiState.value.copy(remainingSeconds = remainingSeconds)
            if (
                intervalSeconds != null &&
                remainingSeconds > 0 &&
                elapsedSeconds > 0 &&
                elapsedSeconds % intervalSeconds == 0
            ) {
                emitBell(config.bellSound)
            }
        }

        val start = timerStart ?: Instant.now().minus(Duration.ofMinutes(config.durationMinutes.toLong()))
        completedStart = start
        completedEnd = start.plus(Duration.ofMinutes(config.durationMinutes.toLong()))
        timerJob = null
        _uiState.value = _uiState.value.copy(
            isTimerRunning = false,
            isTimerPaused = false,
            timerCompleted = true,
            remainingSeconds = 0,
        )
        emitBell(config.bellSound)
    }

    private fun writeSession(
        title: String,
        start: Instant,
        end: Instant,
        onSuccess: () -> Unit,
    ) {
        val current = _uiState.value
        when {
            !current.mindfulnessAvailable -> {
                _uiState.value = current.copy(
                    entryError = MindfulnessEntryError.UNAVAILABLE,
                    writeErrorMessage = null,
                )
                return
            }
            !current.canWrite -> {
                _uiState.value = current.copy(
                    entryError = MindfulnessEntryError.MISSING_WRITE_PERMISSION,
                    writeErrorMessage = null,
                )
                return
            }
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSavingEntry = true,
                entryError = null,
                writeErrorMessage = null,
            )
            runCatching {
                repository.writeMindfulnessSessionEntry(
                    MindfulnessSessionWriteRequest(
                        title = title,
                        startTime = start,
                        endTime = end,
                    )
                )
            }.onSuccess {
                onSuccess()
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isSavingEntry = false,
                    entryError = MindfulnessEntryError.WRITE_FAILED,
                    writeErrorMessage = error.message,
                )
            }
        }
    }

    private fun updateTimerFields(update: MindfulnessEntryUiState.() -> MindfulnessEntryUiState) {
        if (_uiState.value.isTimerRunning || _uiState.value.isTimerPaused || _uiState.value.timerCompleted) return
        _uiState.value = _uiState.value.update().copy(
            entryError = null,
            writeErrorMessage = null,
        )
    }

    private fun currentTimerConfigOrNull(): MindfulnessTimerConfig? {
        val duration = _uiState.value.durationMinutesText.toPositiveIntOrNull()
            ?.takeIf { it in MinSessionMinutes..MaxSessionMinutes }
            ?: return null
        val interval = if (_uiState.value.intervalEnabled) {
            _uiState.value.intervalMinutesText.toPositiveIntOrNull()
                ?.takeIf { it in MinSessionMinutes until duration }
                ?: return null
        } else {
            null
        }
        return MindfulnessTimerConfig(
            durationMinutes = duration,
            intervalMinutes = interval,
            bellSound = _uiState.value.bellSound,
            backgroundSound = _uiState.value.backgroundSound,
        )
    }

    private fun currentSessionTitle(): String = DefaultSessionTitle

    private fun emitBell(sound: MindfulnessBellSound) {
        bellEventId += 1
        _uiState.value = _uiState.value.copy(
            bellEvent = MindfulnessBellEvent(bellEventId, sound),
        )
    }

    private fun initialState(): MindfulnessEntryUiState {
        val config = preferencesRepository.mindfulnessTimerConfig()
        return MindfulnessEntryUiState(
            durationMinutesText = config.durationMinutes.toString(),
            intervalEnabled = config.intervalMinutes != null,
            intervalMinutesText = config.intervalMinutes?.toString().orEmpty(),
            bellSound = config.bellSound,
            backgroundSound = config.backgroundSound,
            remainingSeconds = config.durationMinutes * 60,
            totalSeconds = config.durationMinutes * 60,
        )
    }

    override fun onCleared() {
        timerJob?.cancel()
        super.onCleared()
    }

    private fun canUpdateTimerFields(): Boolean {
        val state = _uiState.value
        return !state.isTimerRunning && !state.isTimerPaused && !state.timerCompleted
    }
}

private fun String.toPositiveIntOrNull(): Int? =
    trim().toIntOrNull()?.takeIf { it > 0 }
