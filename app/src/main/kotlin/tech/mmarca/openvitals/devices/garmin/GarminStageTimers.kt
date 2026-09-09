package tech.mmarca.openvitals.devices.garmin

import kotlin.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** The reasons a stalled stage ends a sync with. One place, so logs and tests agree. */
internal object GarminSyncTimeout {
    fun filter(after: Duration): String =
        "Garmin filter response timed out after ${after.inWholeSeconds} seconds."

    fun directory(after: Duration): String =
        "Garmin directory response timed out after ${after.inWholeSeconds} seconds."

    fun download(label: String, after: Duration): String =
        "Garmin download response for $label timed out after ${after.inWholeSeconds} seconds."

    fun transfer(label: String, after: Duration): String =
        "Garmin $label transfer stalled for ${after.inWholeSeconds} seconds."

    fun whole(after: Duration): String =
        "Garmin file sync timed out after ${after.inWholeMinutes} minutes."
}

/**
 * The session's two timers: one stage timer, re-armed as each protocol step
 * starts, and one grace timer for an empty sync. Expiry runs under the
 * session's [mutex], like a frame, so it never races frame handling.
 */
internal class GarminStageTimers(
    private val scope: CoroutineScope,
    private val mutex: Mutex,
) {
    private var stageJob: Job? = null
    private var graceJob: Job? = null

    /** Whether the grace timer is still counting. */
    val graceArmed: Boolean get() = graceJob != null

    /** Replaces any running stage timer. */
    fun armStage(after: Duration, onExpiry: suspend () -> Unit) {
        stageJob?.cancel()
        stageJob = scope.launch {
            delay(after)
            mutex.withLock {
                stageJob = null
                onExpiry()
            }
        }
    }

    fun cancelStage() {
        stageJob?.cancel()
        stageJob = null
    }

    fun armGrace(after: Duration, onExpiry: suspend () -> Unit) {
        graceJob?.cancel()
        graceJob = scope.launch {
            delay(after)
            // Cleared before the lock: what fires inside must not cancel this job under itself.
            graceJob = null
            mutex.withLock { onExpiry() }
        }
    }

    fun cancelGrace() {
        graceJob?.cancel()
        graceJob = null
    }

    fun cancelAll() {
        cancelStage()
        cancelGrace()
    }
}
