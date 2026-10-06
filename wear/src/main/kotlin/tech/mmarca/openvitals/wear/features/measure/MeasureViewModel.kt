package tech.mmarca.openvitals.wear.features.measure

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.wear.features.quicklog.EntryLog
import tech.mmarca.openvitals.wear.features.quicklog.EntryType
import tech.mmarca.openvitals.wear.features.quicklog.LoggedEntry

sealed interface MeasureUiState {
    /** Instructions and a start button. */
    data object Ready : MeasureUiState

    data class Running(
        /** Share of the maximum duration that has passed. */
        val progress: Float,
        /** The latest live value, such as the current pulse. Null before the first one. */
        val live: Double?,
        /** The sensor is not delivering yet. */
        val waiting: Boolean,
        /** The watch reports it is not on a wrist. */
        val offBody: Boolean,
    ) : MeasureUiState

    data class Done(val value: Double, val secondary: Double?) : MeasureUiState

    data class Failed(val error: MeasureError) : MeasureUiState
}

/**
 * Runs one spot measurement for the measurement screen and keeps its result
 * in the entry log. Leaving the screen clears the ViewModel, which cancels
 * the run and releases the sensor.
 */
class MeasureViewModel(
    private val measurement: Measurement,
    private val events: (Measurement) -> Flow<MeasureEvent>,
    private val onResult: (Measurement, Double) -> Unit,
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val _state = MutableStateFlow<MeasureUiState>(MeasureUiState.Ready)
    val state: StateFlow<MeasureUiState> = _state.asStateFlow()

    private var run: Job? = null

    fun start() {
        run?.cancel()
        run = viewModelScope.launch {
            val startedAt = now()
            val durationMillis = measurement.durationSeconds * 1000f
            var running = MeasureUiState.Running(progress = 0f, live = null, waiting = true, offBody = false)
            _state.value = running
            val ticker = launch {
                while (true) {
                    delay(TickMillis)
                    running = running.copy(progress = ((now() - startedAt) / durationMillis).coerceIn(0f, 1f))
                    _state.value = running
                }
            }
            try {
                events(measurement).collect { event ->
                    when (event) {
                        is MeasureEvent.Live -> running = running.copy(live = event.value, waiting = false, offBody = false)
                        is MeasureEvent.Waiting -> running = running.copy(waiting = true, offBody = event.offBody)
                        is MeasureEvent.Result -> {
                            ticker.cancel()
                            _state.value = MeasureUiState.Done(event.value, event.secondary)
                            onResult(measurement, event.value)
                        }
                    }
                    if (_state.value is MeasureUiState.Running) _state.value = running
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: MeasureException) {
                _state.value = MeasureUiState.Failed(e.error)
            } catch (e: SecurityException) {
                _state.value = MeasureUiState.Failed(MeasureError.PERMISSION_DENIED)
            } catch (e: Exception) {
                _state.value = MeasureUiState.Failed(MeasureError.FAILED)
            } finally {
                ticker.cancel()
            }
        }
    }

    /** For failures found before the run starts, such as a refused permission. */
    fun fail(error: MeasureError) {
        run?.cancel()
        _state.value = MeasureUiState.Failed(error)
    }

    companion object {
        private const val TickMillis = 200L

        fun factory(measurement: Measurement, application: Application) = viewModelFactory {
            initializer {
                val runner = MeasurementRunner(application)
                val entryLog = EntryLog(application)
                MeasureViewModel(
                    measurement = measurement,
                    events = runner::run,
                    onResult = { measured, value ->
                        measured.entryType?.let { type ->
                            entryLog.add(LoggedEntry(type, System.currentTimeMillis(), value))
                        }
                    },
                )
            }
        }
    }
}

/** Where a result is kept. Air pressure is a reading of the moment and is not logged. */
private val Measurement.entryType: EntryType?
    get() = when (this) {
        Measurement.HEART_RATE -> EntryType.HEART_RATE
        Measurement.HRV -> EntryType.HRV
        Measurement.AIR_PRESSURE -> null
    }
