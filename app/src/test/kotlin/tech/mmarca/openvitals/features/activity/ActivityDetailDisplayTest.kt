package tech.mmarca.openvitals.features.activity

import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.insights.ActivitySplit
import tech.mmarca.openvitals.domain.insights.ActivitySplits
import tech.mmarca.openvitals.domain.insights.SplitSource
import tech.mmarca.openvitals.domain.model.ExerciseRouteData
import tech.mmarca.openvitals.domain.model.ExerciseRoutePoint
import tech.mmarca.openvitals.domain.model.ExerciseRouteStatus
import tech.mmarca.openvitals.domain.model.SpeedSample

/**
 * The detail-card derivations as pure functions: the pace scale, the height profile,
 * and the speed trace rebuilt from the splits.
 */
class ActivityDetailDisplayTest {

    private val start: Instant = Instant.parse("2026-03-02T07:00:00Z")

    // region the pace scale

    @Test fun `the pace scale is the slowest and fastest split, per kilometre`() {
        val splits = ActivitySplits(
            source = SplitSource.ROUTE,
            splits = listOf(
                // 1 km in 5:00, then 1 km in 6:00.
                split(1, 1_000.0, Duration.ofMinutes(5)),
                split(2, 1_000.0, Duration.ofMinutes(6)),
            ),
        )

        assertEquals(300.0, fastestSplitPaceSeconds(splits)!!, 0.0001)
        assertEquals(360.0, slowestSplitPaceSeconds(splits)!!, 0.0001)
    }

    @Test fun `a split with no distance leaves the scale unset, not zeroed`() {
        val splits = ActivitySplits(
            source = SplitSource.ESTIMATED,
            splits = listOf(split(1, 0.0, Duration.ofMinutes(5))),
        )

        assertNull(fastestSplitPaceSeconds(splits))
        assertNull(slowestSplitPaceSeconds(splits))
    }

    // endregion

    // region the elevation profile
    // Health Connect has no elevation series, so the profile is drawn from the route altitudes.

    @Test fun `the elevation profile comes from the route altitudes, oldest first`() {
        val samples = elevationProfile(route(listOf(120.0, 145.5, 132.0)))

        assertEquals(3, samples.size)
        assertEquals(listOf(120.0, 145.5, 132.0), samples.map { it.meters })
        assertTrue(samples.first().time.isBefore(samples.last().time))
    }

    @Test fun `the elevation profile skips the points the device gave no height for`() {
        // A fix with no altitude must not read as sea level and draw a cliff.
        val samples = elevationProfile(route(listOf(120.0, null, 132.0)))

        assertEquals(listOf(120.0, 132.0), samples.map { it.meters })
    }

    @Test fun `one height is not an elevation profile`() {
        // A single point draws no line, so the screen renders nothing.
        assertTrue(elevationProfile(route(listOf(120.0, null, null))).isEmpty())
    }

    @Test fun `a route with no altitude at all has no elevation profile`() {
        assertTrue(elevationProfile(route(listOf(null, null))).isEmpty())
    }

    @Test fun `an activity with no route has no elevation profile`() {
        assertTrue(elevationProfile(ExerciseRouteData()).isEmpty())
    }

    // endregion

    // region speed rebuilt from the splits
    // Most watches write a route and a distance but no SpeedRecord, so the speed card never appeared.

    /** 1 km in 5:00 = 3.33 m/s, then 1 km in 6:00 = 2.78 m/s. */
    private fun twoSplits() = ActivitySplits(
        source = SplitSource.ROUTE,
        splits = consecutive(
            listOf(
                1_000.0 to Duration.ofMinutes(5),
                1_000.0 to Duration.ofMinutes(6),
            )
        ),
    )

    @Test fun `a split holds its speed across its window - the trace is a step`() {
        val trace = splitSpeedTrace(recordedSpeed = emptyList(), splits = twoSplits())!!

        // Two points per split, at the same height. A split's speed is an average over a window.
        assertEquals(4, trace.samples.size)
        assertEquals(2, trace.splitCount)
        assertEquals(1_000.0 / 300.0, trace.samples[0].metersPerSecond, 0.001)
        assertEquals(1_000.0 / 300.0, trace.samples[1].metersPerSecond, 0.001)
        assertEquals(1_000.0 / 360.0, trace.samples[2].metersPerSecond, 0.001)
        assertEquals(1_000.0 / 360.0, trace.samples[3].metersPerSecond, 0.001)
        // It steps at the boundary: one instant carrying both speeds.
        assertEquals(trace.samples[1].time, trace.samples[2].time)
        assertTrue(trace.samples.first().time.isBefore(trace.samples.last().time))
    }

    @Test fun `the average is distance over time, NOT the mean of the plotted points`() {
        val trace = splitSpeedTrace(recordedSpeed = emptyList(), splits = twoSplits())!!

        // 2 km in 11:00. The mean of two equal-distance splits' speeds is arithmetic (3.06 m/s);
        // the truth is harmonic (3.03 m/s), and the header shows the truth.
        assertEquals(2_000.0 / 660.0, trace.averageMetersPerSecond, 0.0001)
        val arithmetic = (1_000.0 / 300.0 + 1_000.0 / 360.0) / 2.0
        assertTrue(trace.averageMetersPerSecond < arithmetic)
    }

    @Test fun `a recorded trace wins - a measurement beats a reconstruction`() {
        // Two speed cards on one screen must not disagree.
        val trace = splitSpeedTrace(
            recordedSpeed = listOf(
                SpeedSample(time = start, metersPerSecond = 3.1, source = "test")
            ),
            splits = twoSplits(),
        )

        assertNull(trace)
    }

    @Test fun `estimated splits draw NOTHING - they are flat by construction`() {
        // The estimated source spreads distance evenly, so every split carries the average pace.
        // A line through them would claim a metronomic pace nobody measured.
        val trace = splitSpeedTrace(
            recordedSpeed = emptyList(),
            splits = ActivitySplits(
                source = SplitSource.ESTIMATED,
                splits = consecutive(
                    listOf(
                        1_000.0 to Duration.ofMinutes(5),
                        1_000.0 to Duration.ofMinutes(5),
                    )
                ),
            ),
        )

        assertNull(trace)
    }

    @Test fun `one split is an average, not a trace`() {
        // The header already states this number. A chart adds only the suggestion it was measured.
        val trace = splitSpeedTrace(
            recordedSpeed = emptyList(),
            splits = ActivitySplits(
                source = SplitSource.ROUTE,
                splits = consecutive(listOf(1_000.0 to Duration.ofMinutes(5))),
            ),
        )

        assertNull(trace)
    }

    @Test fun `device laps get a trace too`() {
        val trace = splitSpeedTrace(
            recordedSpeed = emptyList(),
            splits = ActivitySplits(
                source = SplitSource.DEVICE_LAPS,
                splits = consecutive(
                    listOf(
                        400.0 to Duration.ofSeconds(90),
                        400.0 to Duration.ofSeconds(95),
                    )
                ),
            ),
        )

        assertEquals(2, trace!!.splitCount)
    }

    @Test fun `a lap with no distance or no time is skipped, not drawn at zero`() {
        // A paused lap has no speed, and no speed is not zero speed.
        val trace = splitSpeedTrace(
            recordedSpeed = emptyList(),
            splits = ActivitySplits(
                source = SplitSource.DEVICE_LAPS,
                splits = consecutive(
                    listOf(
                        400.0 to Duration.ofSeconds(90),
                        0.0 to Duration.ofSeconds(30),
                        400.0 to Duration.ofSeconds(95),
                    )
                ),
            ),
        )!!

        assertEquals(2, trace.splitCount)
        assertEquals(4, trace.samples.size)
        // The skipped lap is out of the average too: 800 m over 185 s, not 215 s.
        assertEquals(800.0 / 185.0, trace.averageMetersPerSecond, 0.0001)
    }

    // endregion

    /** A route whose points carry [altitudes], null where the device gave none. */
    private fun route(altitudes: List<Double?>) = ExerciseRouteData(
        status = ExerciseRouteStatus.DATA,
        points = altitudes.mapIndexed { index, altitude ->
            ExerciseRoutePoint(
                time = start.plus(Duration.ofMinutes(index.toLong())),
                latitude = 59.43 + index * 0.001,
                longitude = 24.75,
                altitudeMeters = altitude,
                horizontalAccuracyMeters = null,
                verticalAccuracyMeters = null,
            )
        },
    )

    private fun split(index: Int, meters: Double, elapsed: Duration) = ActivitySplit(
        index = index,
        distanceMeters = meters,
        elapsedMs = elapsed.toMillis(),
        startTime = start,
        endTime = start.plus(elapsed),
        isPartial = false,
    )

    /** Consecutive splits, each starting where the last ended. [split] stacks them at the session start. */
    private fun consecutive(legs: List<Pair<Double, Duration>>): List<ActivitySplit> {
        var legStart = start
        return legs.mapIndexed { index, (meters, elapsed) ->
            ActivitySplit(
                index = index + 1,
                distanceMeters = meters,
                elapsedMs = elapsed.toMillis(),
                startTime = legStart,
                endTime = legStart.plus(elapsed),
                isPartial = false,
            ).also { legStart = legStart.plus(elapsed) }
        }
    }
}
