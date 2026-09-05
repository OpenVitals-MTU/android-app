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
 * The recording's view of CoMaps guidance. Asks [CoMapsGuidanceFeed] for
 * the feed while a GPS recording runs with the setting on, or briefly on the
 * pre-start screen. Guidance kept up for a watch is not this layer's
 * business; [navigation] stays Disabled through it.
 */
class CoMapsRecordingWatch(
    private val repository: CoMapsNavigationRepository,
    private val feed: CoMapsGuidanceFeed,
    private val scope: CoroutineScope,
    private val isEnabled: () -> Boolean,
    private val isSavingEnabled: () -> Boolean,
    /** True while the pre-start screen is on display, so a route being set can start the recording. */
    private val isPrestartWatchRequested: () -> Boolean = { false },
) {
    private val _navigation =
        MutableStateFlow<CoMapsNavigationState>(CoMapsNavigationState.Disabled)

    /** Guidance as the recording sees it: Disabled unless the recording wants it. */
    val navigation: StateFlow<CoMapsNavigationState> = _navigation.asStateFlow()

    /** The polyline CoMaps is guiding along. Never persisted. */
    private val _route = MutableStateFlow<CoMapsRoutePolyline?>(null)
    val route: StateFlow<CoMapsRoutePolyline?> = _route.asStateFlow()

    private val recorder = CoMapsNavigationSampleRecorder()

    /** Whether the recording is one of the reasons the feed is up. */
    private var watching = false

    /** The session the banked samples belong to. A restarted recording keeps none. */
    private var sessionStart: Instant? = null

    /** A rebuild can notify twice before the fetch returns. */
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
        // The feed may stay up for somebody else; re-apply this layer's answer.
        publish(feed.guidance.value)
    }

    private fun wantsGuidance(recording: ActivityRecordingState): Boolean {
        // The cheap questions first.
        val gpsRecordingRunning = recording.isActive &&
            recording.recordingKind == ActivityRecordingKind.GPS_ROUTE
        if (!gpsRecordingRunning && !isPrestartWatchRequested()) return false
        return isEnabled()
    }

    /** The one place a reading becomes state the recording can see. */
    private fun publish(state: CoMapsNavigationState) {
        // What the recording does not want becomes Disabled.
        val effective = if (watching) state else CoMapsNavigationState.Disabled
        _navigation.value = effective
        syncRoute(effective)

        // Bank only while a recording runs and the user wants it kept.
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

    /** Re-opens the feed after a permission dialog. */
    fun refresh() {
        feed.refresh()
    }

    /** The guidance banked during this recording. Take it before finishing: the recorder resets. */
    fun samples(): List<CoMapsNavigationSnapshot> = recorder.samples
}
