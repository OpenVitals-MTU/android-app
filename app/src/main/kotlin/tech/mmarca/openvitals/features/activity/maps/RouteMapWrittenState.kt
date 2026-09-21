package tech.mmarca.openvitals.features.activity.maps

import tech.mmarca.openvitals.domain.model.ExerciseRoutePoint

/** The parts of a live route map that a render pass has to rewrite. */
internal data class RouteMapChanges(
    /** The recorded line and its start and end markers. */
    val track: Boolean,
    /** The dot or chevron at the current fix. */
    val position: Boolean,
)

/**
 * Remembers what the map last drew. The compass turns several times a second
 * and the track grows once per fix, so only the changed part is rewritten.
 */
internal class RouteMapWrittenState {
    private var drawn = false
    private var points: List<ExerciseRoutePoint> = emptyList()
    private var routeBreakIndexes: List<Int> = emptyList()
    private var currentPoint: ExerciseRoutePoint? = null
    private var headingDegrees: Float? = null

    /** For a map whose style or layers were dropped: the next pass rewrites everything. */
    fun reset() {
        drawn = false
    }

    /** Returns what changed since the last call, and remembers the new values. */
    fun changesFor(
        points: List<ExerciseRoutePoint>,
        routeBreakIndexes: List<Int>,
        currentPoint: ExerciseRoutePoint?,
        headingDegrees: Float?,
    ): RouteMapChanges {
        // By identity: a recording hands over a new list per fix, and comparing
        // two long tracks point by point would cost what the rewrite costs.
        val track = !drawn || points !== this.points || routeBreakIndexes != this.routeBreakIndexes
        val position = !drawn || currentPoint != this.currentPoint || headingDegrees != this.headingDegrees
        drawn = true
        this.points = points
        this.routeBreakIndexes = routeBreakIndexes
        this.currentPoint = currentPoint
        this.headingDegrees = headingDegrees
        return RouteMapChanges(track = track, position = position)
    }
}
