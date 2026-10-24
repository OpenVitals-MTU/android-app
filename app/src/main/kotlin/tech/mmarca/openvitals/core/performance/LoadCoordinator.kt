package tech.mmarca.openvitals.core.performance

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

class LoadCoordinator {
    private var activeJob: Job? = null
    private var activeRequestId: Long = 0L

    fun launch(
        scope: CoroutineScope,
        block: suspend LoadScope.() -> Unit,
    ) {
        val requestId = ++activeRequestId
        activeJob?.cancel()
        activeJob = scope.launch {
            yield()
            LoadScope(requestId, ::isCurrent).block()
        }
    }

    /** Cancels the load in flight, if any, and makes any answer still on its way stale. */
    fun cancel() {
        activeRequestId++
        activeJob?.cancel()
        activeJob = null
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
