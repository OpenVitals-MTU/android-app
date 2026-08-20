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
 * What CoMaps is guiding right now — the one feed, for everyone who wants it.
 *
 * The bottom of four layers, and the only one that touches CoMaps:
 *
 * 1. **this** — reads the guidance;
 * 2. activity recording — shows it on the recording screen, banks it with the
 *    activity, draws the route, all under its own Settings switch;
 * 3. guidance on a watch — puts it on a wrist, under its own per-watch switch;
 * 4. the vendor that owns that wrist — today Garmin, which has no turn-by-turn
 *    channel and so sends a notification instead. A Samsung or a Wear OS watch
 *    would send something else entirely, and belongs at that layer, not here.
 *
 * Layers 2 and 3 are independent features that happen to read the same thing.
 * Either can want guidance without the other; each [request]s it by its own
 * [Reason] and the feed runs while anyone is asking. Neither can see the
 * other's switches, and nothing here knows what a watch is.
 *
 * **Exactly one collector, deliberately.** CoMaps is observed, not polled: its
 * `NavigationService` calls `notifyChange` on the provider URI at every
 * location fix while it guides, so guidance arrives as fast as CoMaps makes it
 * and a phone navigating nowhere is never queried. The repository holds the
 * liveness clock that feed and safety poll share, and a second concurrent
 * subscription would reset that clock under the first — the two would then
 * take turns calling a live route finished. So the subscription lives here,
 * once, and readers read [guidance].
 *
 * One poll stays, because the feed has a blind spot: CoMaps notifies on CHANGE
 * only. Nothing fires when a route ends — that path just calls `stopSelf()` —
 * and CoMaps never clears the routing info its provider reads from, so the
 * last row keeps being served afterwards. Without an occasional ask, a
 * finished turn would be held forever. The poll runs only while a route is
 * actually being followed, which is the only thing it can detect; a route
 * *starting* needs no poll, the observer hears it. This matters because
 * querying the provider STARTS the CoMaps process when it is not running.
 */
@Singleton
class CoMapsGuidanceFeed @Inject constructor(
    private val repository: CoMapsNavigationRepository,
) {
    /**
     * Why the feed is up. Each is a feature with its own switch, and no
     * reason outranks another: the feed runs while any is asking and stops
     * when the last one lets go.
     */
    enum class Reason {
        /** A GPS recording is running (or being armed) with the integration on. */
        ACTIVITY_RECORDING,

        /** A paired watch is showing guidance on the wrist. */
        WATCH,
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /**
     * A flow rather than a set because the askers are on different threads —
     * the recording controller on Main, a watch relay on its own — and the
     * start/stop it drives must happen in one place, in order.
     */
    private val reasons = MutableStateFlow<Set<Reason>>(emptySet())

    private val _guidance = MutableStateFlow<CoMapsNavigationState>(CoMapsNavigationState.Disabled)

    /**
     * What CoMaps says, raw: [CoMapsNavigationState.Disabled] whenever nobody
     * is asking. Readers apply their own switches to it — this one answers
     * only to whether anybody wants the feed at all.
     */
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
     * Call after a permission dialog closes: the package probe may now answer
     * differently, and the subscription is re-opened rather than merely re-read.
     *
     * Registering the observer needs the grant that just arrived. An attempt
     * made without it was refused, and a refused observer hears nothing ever
     * again — so a wearer who switched guidance on and granted afterwards
     * would have had a dead feed until the next app start. Re-opening reads
     * the current state on its own, so there is nothing else to ask for.
     */
    fun refresh() {
        repository.onPermissionChanged()
        // Onto the feed's own scope, like every other start and stop: the jobs
        // below are single-threaded state and the callers are screens.
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
        // The feed opens with the current state, so there is no separate first
        // read to make here — and the safety poll is NOT started here either:
        // it only exists to catch a route ending, so it runs only while one is
        // being followed (see [syncSafetyPoll]).
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
