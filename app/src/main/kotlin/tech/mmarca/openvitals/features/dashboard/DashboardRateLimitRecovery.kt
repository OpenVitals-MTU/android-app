package tech.mmarca.openvitals.features.dashboard

import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Asks for the degraded tiles again once Health Connect's backoff ends.
 *
 * A tile whose read was refused shows nothing, and nothing used to reload it:
 * the next resume or pull sent the whole dashboard again. This waits for the
 * deadline the readers publish, plus a little jitter so every screen does not
 * fire on the same millisecond, and then reloads only what is missing. A
 * deadline that moves while it waits restarts the wait; a reload that is
 * refused again publishes a new deadline, and the loop climbs the backoff
 * ladder with it.
 */
internal class DashboardRateLimitRecovery(
    private val deadlines: Flow<Long>,
    /** Whether a partial reload is worth it now: degraded tiles, no load in flight, app in front. */
    private val canReload: () -> Boolean,
    private val reload: () -> Unit,
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val jitterMillis: () -> Long = { Random.nextLong(MaxJitterMillis) },
) {
    fun start(scope: CoroutineScope): Job = scope.launch {
        deadlines.collectLatest { deadline ->
            if (deadline <= 0L) return@collectLatest
            delay((deadline - nowMillis()).coerceAtLeast(0L) + jitterMillis())
            // A load already running re-reads everything; a background app reloads on its return.
            if (canReload()) reload()
        }
    }

    companion object {
        const val MaxJitterMillis = 5_000L
    }
}
