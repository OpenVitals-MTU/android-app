package tech.mmarca.openvitals.features.manualentry.activity.routeimport

import tech.mmarca.openvitals.features.manualentry.*
import tech.mmarca.openvitals.features.manualentry.activity.*
import tech.mmarca.openvitals.features.manualentry.activity.recording.*
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.*
import tech.mmarca.openvitals.features.manualentry.body.*
import tech.mmarca.openvitals.features.manualentry.hydration.*
import tech.mmarca.openvitals.features.manualentry.mindfulness.*
import tech.mmarca.openvitals.features.manualentry.vitals.*



import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.ActivityPauseInterval
import tech.mmarca.openvitals.domain.model.ExerciseRoutePoint
import tech.mmarca.openvitals.domain.preferences.UnitSystem

/** The moving time once pauses are taken out, and the averages computed over it. */
class ActivityRouteSectionTest {

    @Test fun `routeMovingDurationMs is the full span when nothing was paused`() {
        assertEquals(
            Duration.ofMinutes(30).toMillis(),
            routeMovingDurationMs(route(), emptyList()),
        )
    }

    @Test fun `routeMovingDurationMs subtracts every pause`() {
        val pauses = listOf(
            ActivityPauseInterval(
                startTime = start.plus(Duration.ofMinutes(5)),
                endTime = start.plus(Duration.ofMinutes(9)),
            ),
            ActivityPauseInterval(
                startTime = start.plus(Duration.ofMinutes(20)),
                endTime = start.plus(Duration.ofMinutes(21)),
            ),
        )

        assertEquals(
            Duration.ofMinutes(25).toMillis(),
            routeMovingDurationMs(route(), pauses),
        )
    }

    @Test fun `routeMovingDurationMs never goes negative when the pauses exceed the span`() {
        val pauses = listOf(
            ActivityPauseInterval(
                startTime = start,
                endTime = start.plus(Duration.ofHours(5)),
            ),
        )

        assertEquals(0L, routeMovingDurationMs(route(), pauses))
    }

    @Test fun `routeAverageMetrics is null when the route has no moving time left`() {
        val metrics = routeAverageMetrics(
            route = route(duration = Duration.ZERO),
            pauseIntervals = emptyList(),
            unitFormatter = formatter(),
        )

        assertNull(metrics)
    }

    @Test fun `routeAverageMetrics reports pace and speed over the moving time only`() {
        // 5 km in 30 min = 6:00 /km. Pausing 10 min leaves 20 min -> 4:00 /km.
        val moving = routeAverageMetrics(
            route = route(),
            pauseIntervals = listOf(
                ActivityPauseInterval(
                    startTime = start.plus(Duration.ofMinutes(5)),
                    endTime = start.plus(Duration.ofMinutes(15)),
                ),
            ),
            unitFormatter = formatter(),
        )

        assertNotNull(moving)
        assertTrue(requireNotNull(moving).averagePace.contains("4:00"))

        val unpaused = routeAverageMetrics(
            route = route(),
            pauseIntervals = emptyList(),
            unitFormatter = formatter(),
        )

        assertNotNull(unpaused)
        assertTrue(requireNotNull(unpaused).averagePace.contains("6:00"))
    }

    private fun point(minute: Long): ExerciseRoutePoint =
        ExerciseRoutePoint(
            time = start.plus(Duration.ofMinutes(minute)),
            latitude = 59.0 + minute * 0.001,
            longitude = 24.0,
            altitudeMeters = 10.0,
            horizontalAccuracyMeters = null,
            verticalAccuracyMeters = null,
        )

    private fun route(
        duration: Duration = Duration.ofMinutes(30),
        distanceMeters: Double = 5000.0,
    ): RouteFileImport =
        RouteFileImport(
            fileName = "morning.gpx",
            name = "Morning run",
            points = listOf(point(0), point(10), point(20)),
            distanceMeters = distanceMeters,
            elevationGainedMeters = 42.0,
            startTime = start,
            endTime = start.plus(duration),
        )

    private fun formatter(system: UnitSystem = UnitSystem.METRIC): UnitFormatter =
        UnitFormatter(unitSystemProvider = { system })

    private companion object {
        private val start: Instant = Instant.parse("2026-07-09T08:00:00Z")
    }
}
