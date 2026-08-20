package tech.mmarca.openvitals.features.manualentry.activity.recording

import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.comaps.CoMapsGuidanceFeed
import tech.mmarca.openvitals.data.repository.contract.CoMapsNavigationRepository
import tech.mmarca.openvitals.domain.model.CoMapsNavigationSampleRecorder
import tech.mmarca.openvitals.domain.model.CoMapsNavigationSnapshot
import tech.mmarca.openvitals.domain.model.CoMapsNavigationState
import tech.mmarca.openvitals.domain.model.CoMapsRoutePolyline

/**
 * The recording's view of CoMaps guidance: what the turn strip draws, what the
 * route line is drawn from, and what gets banked with the activity.
 *
 * The guidance itself comes from [CoMapsGuidanceFeed], which serves every
 * feature that wants it. This layer asks for the feed while a recording has
 * use for it and applies the recording's own switches to what comes back:
 *
 * - a GPS-route recording that is actually running, with "CoMaps guidance"
 *   switched on in Settings, Activity recording. A gym session has nothing to
 *   navigate, a paused recording is not going anywhere, and a user who never
 *   asked never has their resolver touched;
 * - the pre-start screen, briefly, so a route being set in CoMaps can start
 *   the recording rather than wait to be noticed.
 *
 * Anything else keeping the feed up — guidance on a watch, which is a separate
 * feature with a separate switch — is *not* this layer's business, and
 * [navigation] stays Disabled through it. A wearer following turns on their
 * wrist has not asked the recording screen for anything.
 */
class CoMapsRecordingWatch(
    private val repository: CoMapsNavigationRepository,
    private val feed: CoMapsGuidanceFeed,
    private val scope: CoroutineScope,
    private val isEnabled: () -> Boolean,
    private val isSavingEnabled: () -> Boolean,
    /**
     * True while the pre-start recording screen is on display: the one moment
     * an idle session watches CoMaps, so a route being set can start the
     * recording rather than wait to be noticed.
     */
    private val isPrestartWatchRequested: () -> Boolean = { false },
) {
    private val _navigation =
        MutableStateFlow<CoMapsNavigationState>(CoMapsNavigationState.Disabled)

    /** Guidance as the recording sees it: Disabled unless the recording wants it. */
    val navigation: StateFlow<CoMapsNavigationState> = _navigation.asStateFlow()

    /**
     * The polyline CoMaps is guiding along. Never persisted, and null for a
     * CoMaps predating the geometry contract.
     */
    private val _route = MutableStateFlow<CoMapsRoutePolyline?>(null)
    val route: StateFlow<CoMapsRoutePolyline?> = _route.asStateFlow()

    private val recorder = CoMapsNavigationSampleRecorder()

    /** Whether the recording is one of the reasons the feed is up. */
    private var watching = false

    /**
     * The session the banked samples belong to, and so also the answer to
     * whether anything should be banked at all. When the recording restarts,
     * the samples of the last one are not ours to keep.
     */
    private var sessionStart: Instant? = null

    /**
     * A rebuild can notify twice before the fetch returns, and a provider
     * that cannot answer must not be asked once per fix.
     */
    private var fetchedRouteRevision: Int? = null
    private var fetchingRoute = false

    init {
        feed.guidance.onEach(::publish).launchIn(scope)
    }

    /** Called on every recording state change; asks for the feed, or lets it go. */
    fun sync(recording: ActivityRecordingState) {
        watching = wantsGuidance(recording)

        if (!watching) {
            recorder.reset()
            sessionStart = null
        } else {
            // A restarted recording does not inherit the last one's guidance.
            val startTime = recording.startTime
            if (startTime != null && startTime != sessionStart) {
                recorder.reset()
                sessionStart = startTime
            }
        }

        feed.request(CoMapsGuidanceFeed.Reason.ACTIVITY_RECORDING, watching)
        // The feed may well stay up for somebody else, and its last reading is
        // still sitting there; re-apply this layer's answer to it either way.
        publish(feed.guidance.value)
    }

    private fun wantsGuidance(recording: ActivityRecordingState): Boolean {
        // The cheap questions first: there is nothing to watch for a gym
        // session or a stopped one, and no reason to reach for preferences to
        // find that out.
        val gpsRecordingRunning = recording.isActive &&
            recording.recordingKind == ActivityRecordingKind.GPS_ROUTE
        if (!gpsRecordingRunning && !isPrestartWatchRequested()) return false
        return isEnabled()
    }

    /** The one place a reading becomes state the recording can see. */
    private fun publish(state: CoMapsNavigationState) {
        // Somebody else's reason to watch is not a reason to show anything
        // here, so what the recording does not want becomes Disabled.
        val effective = if (watching) state else CoMapsNavigationState.Disabled
        _navigation.value = effective
        syncRoute(effective)

        // Bank the reading only if a recording is running to bank it for, and
        // only if the user wants it kept. The recorder decides whether this one
        // is worth keeping; most are not.
        if (effective is CoMapsNavigationState.Active &&
            sessionStart != null &&
            isSavingEnabled()
        ) {
            recorder.accept(effective.snapshot)
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
     * Re-opens the feed after a permission dialog, so the panel updates
     * without waiting for CoMaps to say something next.
     */
    fun refresh() {
        feed.refresh()
    }

    /**
     * The guidance banked during this recording, for the activity about to be
     * saved. Empty unless the user asked for it to be kept. Take it BEFORE
     * finishing: the moment the recording goes inactive the recorder is reset
     * — whether or not anything else keeps the feed itself running — and these
     * samples are the only copy.
     */
    fun samples(): List<CoMapsNavigationSnapshot> = recorder.samples
}
