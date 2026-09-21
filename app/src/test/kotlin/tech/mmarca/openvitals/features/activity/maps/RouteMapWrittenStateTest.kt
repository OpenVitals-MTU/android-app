package tech.mmarca.openvitals.features.activity.maps

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test
import tech.mmarca.openvitals.domain.model.ExerciseRoutePoint

/** Each 3 degree compass step used to hand the map the whole recorded track again. */
class RouteMapWrittenStateTest {

    private val start: Instant = Instant.parse("2026-07-04T10:00:00Z")

    private fun point(second: Long) = ExerciseRoutePoint(
        time = start.plusSeconds(second),
        latitude = 59.43 + second * 0.0001,
        longitude = 24.75,
        altitudeMeters = null,
        horizontalAccuracyMeters = null,
        verticalAccuracyMeters = null,
    )

    private val everything = RouteMapChanges(track = true, position = true)
    private val nothing = RouteMapChanges(track = false, position = false)
    private val positionOnly = RouteMapChanges(track = false, position = true)

    @Test
    fun `the first pass writes everything`() {
        val written = RouteMapWrittenState()

        assertEquals(everything, written.changesFor(emptyList(), emptyList(), null, null))
    }

    @Test
    fun `a compass step rewrites the position marker only`() {
        val written = RouteMapWrittenState()
        val track = listOf(point(0), point(1))
        written.changesFor(track, emptyList(), point(1), 90f)

        assertEquals(positionOnly, written.changesFor(track, emptyList(), point(1), 93f))
        assertEquals(nothing, written.changesFor(track, emptyList(), point(1), 93f))
    }

    @Test
    fun `a new fix rewrites the track`() {
        val written = RouteMapWrittenState()
        val track = listOf(point(0), point(1))
        written.changesFor(track, emptyList(), point(1), 90f)

        assertEquals(everything, written.changesFor(track + point(2), emptyList(), point(2), 90f))
    }

    @Test
    fun `a new break in the same track rewrites the track`() {
        val written = RouteMapWrittenState()
        val track = listOf(point(0), point(1), point(2))
        written.changesFor(track, emptyList(), point(2), null)

        assertEquals(
            RouteMapChanges(track = true, position = false),
            written.changesFor(track, listOf(2), point(2), null),
        )
    }

    @Test
    fun `a reset map is written in full again`() {
        val written = RouteMapWrittenState()
        val track = listOf(point(0), point(1))
        written.changesFor(track, emptyList(), point(1), 90f)

        written.reset()

        assertEquals(everything, written.changesFor(track, emptyList(), point(1), 90f))
    }
}
