package tech.mmarca.openvitals.core.performance

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

class LoadCoordinator {
    private var activeJob: Job? = null
    private var activeRequestId: Long = 0L

    /** The last load was cut short by [cancel] and none has started since. */
    var interrupted: Boolean = false
        private set

    fun launch(
        scope: CoroutineScope,
        block: suspend LoadScope.() -> Unit,
    ) {
        val requestId = ++activeRequestId
        interrupted = false
        activeJob?.cancel()
        activeJob = scope.launch {
            yield()
            LoadScope(requestId, ::isCurrent).block()
        }
    }

    /**
     * Stops the load in flight, for a screen that left view. Its reads give way to the next
     * screen's: they share two read slots, and Health Connect answers aggregates one at a time.
     * The stopped load is no longer current, so it publishes nothing, not even its cancellation.
     *
     * True when a load was cut short, as [interrupted] records: the screen then loads again
     * when it is back.
     */
    fun cancel(): Boolean {
        val running = activeJob?.isActive == true
        // Stale first: on an immediate dispatcher the job's `finally` runs inside cancel().
        ++activeRequestId
        activeJob?.cancel()
        activeJob = null
        if (running) interrupted = true
        return running
    }

    private fun isCurrent(requestId: Long): Boolean =
        requestId == activeRequestId

    class LoadScope internal constructor(
        private val requestId: Long,
        private val isCurrentRequest: (Long) -> Boolean,
    ) {
        val isCurrent: Boolean
            get() = isCurrentRequest(requestId)
    }
}
