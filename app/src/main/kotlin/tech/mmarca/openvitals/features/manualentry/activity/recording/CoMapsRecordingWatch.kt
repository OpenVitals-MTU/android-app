package tech.mmarca.openvitals.features.manualentry.activity.recording

import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.data.repository.contract.CoMapsNavigationRepository
import tech.mmarca.openvitals.domain.model.CoMapsNavigationSampleRecorder
import tech.mmarca.openvitals.domain.model.CoMapsNavigationSnapshot
import tech.mmarca.openvitals.domain.model.CoMapsNavigationState
import tech.mmarca.openvitals.domain.model.CoMapsRoutePolyline

/**
 * The CoMaps guidance watch, alive exactly as long as it should be.
 *
 * CoMaps is **observed**, not polled: its `NavigationService` calls
 * `notifyChange` on the provider URI at every location fix while it guides, so
 * guidance arrives as fast as CoMaps makes it and a phone that is navigating
 * nowhere is never queried at all. The watch only runs while a GPS-route
 * recording is actively running and the user has switched the integration on:
 * a gym session has nothing to navigate, a paused recording is not going
 * anywhere, and a user who never asked never has their resolver touched.
 *
 * One poll stays, because the feed has a blind spot: CoMaps notifies on CHANGE
 * only. Nothing fires when a route ends — that path just calls `stopSelf()` —
 * and CoMaps never clears the routing info its provider reads from, so the
 * last row keeps being served afterwards. Without an occasional ask, the panel
 * would hold the final turn of a finished route forever. The poll runs only
 * while a route is actually being followed, which is the only thing it can
 * detect; a route *starting* needs no poll, the observer hears it. This
 * matters because querying the provider STARTS the CoMaps process when it is
 * not running — polling regardless woke a map app every ten seconds for the
 * whole of every recording that was not navigating anywhere.
 */
class CoMapsRecordingWatch(
    private val repository: CoMapsNavigationRepository,
    private val scope: CoroutineScope,
    private val isEnabled: () -> Boolean,
    private val isSavingEnabled: () -> Boolean,
) {
    private val _navigation =
        MutableStateFlow<CoMapsNavigationState>(CoMapsNavigationState.Disabled)
    val navigation: StateFlow<CoMapsNavigationState> = _navigation.asStateFlow()

    /**
     * The polyline CoMaps is guiding along. Never persisted, and null for a
     * CoMaps predating the geometry contract.
     */
    private val _route = MutableStateFlow<CoMapsRoutePolyline?>(null)
    val route: StateFlow<CoMapsRoutePolyline?> = _route.asStateFlow()

    private var watchJob: Job? = null
    private var safetyPollJob: Job? = null
    private val recorder = CoMapsNavigationSampleRecorder()

    /**
     * The session the banked samples belong to. When the recording restarts,
     * the samples of the last one are not ours to keep.
     */
    private var sessionStart: Instant? = null

    /**
     * A rebuild can notify twice before the fetch returns, and a provider
     * that cannot answer must not be asked once per fix.
     */
    private var fetchedRouteRevision: Int? = null
    private var fetchingRoute = false

    /** Called on every recording state change; starts or stops the watch to match. */
    fun sync(recording: ActivityRecordingState) {
        if (!wantsGuidance(recording)) {
            stop()
            recorder.reset()
            sessionStart = null
            if (_navigation.value !is CoMapsNavigationState.Disabled || _route.value != null) {
                _navigation.value = CoMapsNavigationState.Disabled
                _route.value = null
            }
            return
        }

        // A restarted recording does not inherit the last one's guidance.
        val startTime = recording.startTime
        if (startTime != null && startTime != sessionStart) {
            recorder.reset()
            sessionStart = startTime
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

    private fun wantsGuidance(recording: ActivityRecordingState): Boolean {
        // The cheap questions first: there is nothing to poll for a gym
        // session or a stopped one, and no reason to reach for preferences to
        // find that out.
        if (!recording.isActive ||
            recording.recordingKind != ActivityRecordingKind.GPS_ROUTE
        ) {
            return false
        }
        return isEnabled()
    }

    /** The one place a reading becomes state, from the feed or the safety poll. */
    private fun publish(state: CoMapsNavigationState) {
        _navigation.value = state
        syncSafetyPoll(state)
        syncRoute(state)

        // Bank the reading only if the user wants it kept. The recorder
        // decides whether this one is worth keeping; most are not.
        if (state is CoMapsNavigationState.Active && isSavingEnabled()) {
            recorder.accept(state.snapshot)
        }
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

    private fun syncRoute(state: CoMapsNavigationState) {
        val revision = (state as? CoMapsNavigationState.Active)?.routeRevision
        if (state !is CoMapsNavigationState.Active || revision == null) {
            fetchedRouteRevision = null
            if (_route.value != null) _route.value = null
            return
        }
        if (revision == fetchedRouteRevision || fetchingRoute) return
        fetchingRoute = true
        scope.launch {
            try {
                val geometry = repository.readRouteGeometry(revision) ?: return@launch
                fetchedRouteRevision = revision
                _route.value = CoMapsRoutePolyline(
                    revision = geometry.revision,
                    points = geometry.points,
                    destination = state.destination,
                    destinationName = state.destinationName,
                )
            } finally {
                fetchingRoute = false
            }
        }
    }

    /**
     * Grants aside, reads again so the panel updates without waiting for the
     * feed or the safety poll.
     */
    fun refresh() {
        scope.launch { publish(repository.readLive()) }
    }

    /**
     * The guidance banked during this recording, for the activity about to be
     * saved. Empty unless the user asked for it to be kept. Take it BEFORE
     * finishing: the moment the recording goes inactive this watch tears
     * itself down and resets the recorder, and these samples are the only copy.
     */
    fun samples(): List<CoMapsNavigationSnapshot> = recorder.samples

    private fun stop() {
        watchJob?.cancel()
        watchJob = null
        safetyPollJob?.cancel()
        safetyPollJob = null
        fetchedRouteRevision = null
        fetchingRoute = false
    }

    private companion object {
        const val SafetyPollIntervalMillis = 10_000L
    }
}
