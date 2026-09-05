package tech.mmarca.openvitals.comaps

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.data.repository.contract.CoMapsNavigationRepository
import tech.mmarca.openvitals.domain.model.CoMapsNavigationState

/**
 * What CoMaps is guiding right now: one feed for everyone. Each feature [request]s it
 * by its own [Reason]; the feed runs while anyone asks.
 *
 * Exactly one collector, because a second subscription resets the liveness clock.
 * A poll runs while a route is followed, since CoMaps never clears its routing info.
 */
@Singleton
class CoMapsGuidanceFeed @Inject constructor(
    private val repository: CoMapsNavigationRepository,
) {
    /** Why the feed is up. No reason outranks another. */
    enum class Reason {
        /** A GPS recording is running (or being armed) with the integration on. */
        ACTIVITY_RECORDING,

        /** A paired watch is showing guidance on the wrist. */
        WATCH,
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** A flow, not a set: the askers are on different threads, and start/stop must happen in order. */
    private val reasons = MutableStateFlow<Set<Reason>>(emptySet())

    private val _guidance = MutableStateFlow<CoMapsNavigationState>(CoMapsNavigationState.Disabled)

    /** What CoMaps says, raw. [CoMapsNavigationState.Disabled] whenever nobody asks. */
    val guidance: StateFlow<CoMapsNavigationState> = _guidance.asStateFlow()

    private var watchJob: Job? = null
    private var safetyPollJob: Job? = null

    init {
        reasons.onEach(::sync).launchIn(scope)
    }

    /** Asks for the feed, or lets it go, on behalf of one [Reason]. */
    fun request(reason: Reason, wanted: Boolean) {
        reasons.update { if (wanted) it + reason else it - reason }
    }

    /**
     * Call after a permission dialog closes: a refused observer hears nothing
     * ever again, so the subscription is re-opened.
     */
    fun refresh() {
        repository.onPermissionChanged()
        // Onto the feed's own scope: the jobs below are single-threaded state.
        scope.launch {
            val asking = reasons.value
            if (asking.isEmpty()) return@launch
            stop()
            sync(asking)
        }
    }

    private fun sync(reasons: Set<Reason>) {
        if (reasons.isEmpty()) {
            stop()
            _guidance.value = CoMapsNavigationState.Disabled
            return
        }
        // The feed opens with the current state. The safety poll starts only while
        // a route is being followed; see [syncSafetyPoll].
        if (watchJob == null) {
            watchJob = scope.launch {
                repository.watchLive().collect(::publish)
            }
        }
    }

    private fun publish(state: CoMapsNavigationState) {
        _guidance.value = state
        syncSafetyPoll(state)
    }

    private fun syncSafetyPoll(state: CoMapsNavigationState) {
        val needed = state is CoMapsNavigationState.Active
        if (needed) {
            if (safetyPollJob == null) {
                safetyPollJob = scope.launch {
                    while (isActive) {
                        delay(SafetyPollIntervalMillis)
                        publish(repository.readLive())
                    }
                }
            }
        } else {
            safetyPollJob?.cancel()
            safetyPollJob = null
        }
    }

    private fun stop() {
        watchJob?.cancel()
        watchJob = null
        safetyPollJob?.cancel()
        safetyPollJob = null
    }

    private companion object {
        const val SafetyPollIntervalMillis = 10_000L
    }
}
