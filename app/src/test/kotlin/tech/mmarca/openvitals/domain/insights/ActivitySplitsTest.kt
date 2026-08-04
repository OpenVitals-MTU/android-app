package tech.mmarca.openvitals.domain.insights

import androidx.health.connect.client.records.ExerciseSegment
import androidx.health.connect.client.records.ExerciseSessionRecord
import java.time.Duration
import java.time.Instant
import kotlin.math.PI
import kotlin.math.roundToLong
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.core.geo.haversineMeters
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.domain.model.ExerciseLapData
import tech.mmarca.openvitals.domain.model.ExerciseRouteData
import tech.mmarca.openvitals.domain.model.ExerciseRoutePoint
import tech.mmarca.openvitals.domain.model.ExerciseRouteStatus
import tech.mmarca.openvitals.domain.model.ExerciseSegmentData
import tech.mmarca.openvitals.domain.model.HeartRateSample
import tech.mmarca.openvitals.domain.model.SpeedSample

/**
 * `buildActivitySplits` is arithmetic, so it is tested like arithmetic:
 * known geometry in, exact numbers out.
 */
class ActivitySplitsTest {

    private val start: Instant = Instant.parse("2026-07-10T08:00:00Z")

    private fun at(seconds: Number): Instant =
        start.plusNanos((seconds.toDouble() * 1e9).roundToLong())

    private fun point(
        atSeconds: Number,
        eastMeters: Double,
        altitudeMeters: Double? = null,
    ): ExerciseRoutePoint =
        ExerciseRoutePoint(
            time = at(atSeconds),
            latitude = 0.0,
            longitude = eastMeters / MetersPerDegreeAtEquator,
            altitudeMeters = altitudeMeters,
            horizontalAccuracyMeters = null,
            verticalAccuracyMeters = null,
        )

    private fun workout(
        totalDistanceMeters: Double? = null,
        durationMs: Long? = null,
        routePoints: List<ExerciseRoutePoint> = emptyList(),
        laps: List<ExerciseLapData> = emptyList(),
        segments: List<ExerciseSegmentData> = emptyList(),
        endTime: Instant? = null,
        exerciseType: Int = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
    ): ExerciseData {
        val end = endTime ?: at(600)
        return ExerciseData(
            id = "w1",
            title = "Run",
            exerciseType = exerciseType,
            startTime = start,
            endTime = end,
            durationMs = durationMs ?: Duration.between(start, end).toMillis(),
            source = "test",
            totalDistanceMeters = totalDistanceMeters,
            laps = laps,
            segments = segments,
            route = if (routePoints.isEmpty()) {
                ExerciseRouteData()
            } else {
                ExerciseRouteData(status = ExerciseRouteStatus.DATA, points = routePoints)
            },
        )
    }

    private fun hr(atSeconds: Number, bpm: Long): HeartRateSample =
        HeartRateSample(time = at(atSeconds), beatsPerMinute = bpm, source = "test")

    private fun speed(atSeconds: Number, metersPerSecond: Double): SpeedSample =
        SpeedSample(time = at(atSeconds), metersPerSecond = metersPerSecond, source = "test")

    private fun pause(startSeconds: Number, endSeconds: Number): ExerciseSegmentData =
        ExerciseSegmentData(
            startTime = at(startSeconds),
            endTime = at(endSeconds),
            segmentType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_PAUSE,
            repetitions = 0,
        )

    private fun compute(
        workout: ExerciseData,
        heartRates: List<HeartRateSample> = emptyList(),
        speeds: List<SpeedSample> = emptyList(),
        splitDistanceMeters: Double = 1000.0,
    ): ActivitySplits =
        buildActivitySplits(
            workout = workout,
            routePoints = workout.route.points,
            speedSamples = speeds,
            heartRateSamples = heartRates,
            splitDistanceMeters = splitDistanceMeters,
        )

    private fun elapsedSeconds(split: ActivitySplit): Double = split.elapsedMs / 1000.0

    /**
     * Seconds since the workout start. Interpolated crossings land on
     * sub-millisecond boundaries (the haversine of a "1000 m" fixture segment is
     * 1000 m to about eleven decimal places, not exactly), so split times are
     * asserted with a tolerance rather than by Instant equality.
     */
    private fun secondsFromStart(time: Instant): Double =
        Duration.between(start, time).toNanos() / 1_000_000_000.0

    // --- an activity that does not travel has no splits ---

    @Test
    fun `a strength session with GPS drift is not cut into laps`() {
        // The bug, from a real session: a phone left on the bench picked up 1.2 km
        // of GPS drift over 36 minutes. Health Connect recorded it faithfully, the
        // old "does it have any distance?" gate said yes, and a lifting session was
        // duly cut into a "1.0 km" split and a "181 m (partial)" one, both at a
        // 30:29 min/km pace. The distance was real data; the splits were nonsense.
        val splits = compute(
            workout(
                exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
                totalDistanceMeters = 1200.0,
                endTime = at(2160), // 36 minutes
            ),
        )

        assertTrue(splits.splits.isEmpty())
        assertTrue(splits.isEmpty)
    }

    @Test
    fun `neither is a strength session carrying a route it never meant to record`() {
        val splits = compute(
            workout(
                exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
                // ~1.2 km of "movement" — exactly the drift a bench-side phone logs.
                routePoints = listOf(point(0, 0.0), point(2160, 1200.0)),
            ),
        )
        assertTrue(splits.splits.isEmpty())
    }

    @Test
    fun `a run with the same distance IS cut, so the gate is on the KIND`() {
        val splits = compute(
            workout(
                exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
                totalDistanceMeters = 1200.0,
                endTime = at(2160),
            ),
        )
        assertTrue(splits.splits.isNotEmpty())
    }

    // --- the equator-line fixture ---

    @Test
    fun `the fixture really does put the requested number of meters between fixes`() {
        // Everything below reads distance off this; if the fixture lies, the
        // whole file lies.
        val a = point(0, 0.0)
        val b = point(10, 1000.0)
        assertEquals(
            1000.0,
            haversineMeters(a.latitude, a.longitude, b.latitude, b.longitude),
            0.01,
        )
    }

    // --- source priority ---

    @Test
    fun `device laps win over a route, and are NOT re-cut to the split distance`() {
        // A track session: 400 m laps, recorded by the watch. A 1 km splitter
        // would produce ~1.6 splits from the same route — the device's laps must
        // survive untouched.
        val workout = workout(
            totalDistanceMeters = 1600.0,
            endTime = at(480),
            routePoints = (0..16).map { point(it * 30, it * 100.0) },
            laps = (0 until 4).map { lap ->
                ExerciseLapData(
                    startTime = at(lap * 120),
                    endTime = at((lap + 1) * 120),
                    lengthMeters = 400.0,
                )
            },
        )

        val result = compute(workout)

        assertEquals(SplitSource.DEVICE_LAPS, result.source)
        assertEquals(4, result.splits.size)
        result.splits.forEach { assertEquals(400.0, it.distanceMeters, 0.001) }
        assertEquals(listOf(1, 2, 3, 4), result.splits.map { it.index })
        // An uneven lap is not a partial one.
        result.splits.forEach { assertFalse(it.isPartial) }
    }

    @Test
    fun `uneven device laps keep their own lengths`() {
        val workout = workout(
            totalDistanceMeters = 1500.0,
            endTime = at(600),
            laps = listOf(
                ExerciseLapData(startTime = at(0), endTime = at(200), lengthMeters = 800.0),
                ExerciseLapData(startTime = at(200), endTime = at(600), lengthMeters = 700.0),
            ),
        )

        val result = compute(workout)

        assertEquals(SplitSource.DEVICE_LAPS, result.source)
        assertEquals(listOf(800.0, 700.0), result.splits.map { it.distanceMeters })
    }

    @Test
    fun `a route beats speed samples`() {
        val workout = workout(
            totalDistanceMeters = 2000.0,
            endTime = at(600),
            routePoints = (0..20).map { point(it * 30, it * 100.0) },
        )

        val result = compute(
            workout,
            speeds = (0..10).map { speed(it * 60, 3.0) },
        )

        assertEquals(SplitSource.ROUTE, result.source)
    }

    // --- route splits ---

    @Test
    fun `cut at exactly the right distance, with the crossing time INTERPOLATED between fixes`() {
        // A deliberately hostile sample rate: one fix every 200 s. The runner
        // covers 900 m in the first 200 s, then slows hard — the 1 km boundary
        // falls just 100 m into a 1000 m segment that takes 600 s.
        //
        //   fix 0:   t=0 s      0 m
        //   fix 1:   t=200 s    900 m
        //   fix 2:   t=800 s    1900 m
        //
        // The 1 km mark is 10% of the way along the second segment, so it is
        // crossed at t = 200 + 0.10 * 600 = 260 s.
        //
        // SNAPPING to the next fix would call split 1 "1000 m in 800 s" — a
        // 13:20 min/km pace for a kilometre actually run in 4:20. That is the
        // whole reason for interpolating.
        val workout = workout(
            totalDistanceMeters = 1900.0,
            endTime = at(800),
            routePoints = listOf(point(0, 0.0), point(200, 900.0), point(800, 1900.0)),
        )

        val result = compute(workout)

        assertEquals(SplitSource.ROUTE, result.source)
        assertEquals(2, result.splits.size)

        val first = result.splits.first()
        assertEquals(1000.0, first.distanceMeters, 0.01)
        assertEquals(260.0, elapsedSeconds(first), 0.5)
        assertEquals(260.0, secondsFromStart(first.endTime), 0.05)
        assertFalse(first.isPartial)
        // Not the snapped-to-the-next-fix answer.
        assertTrue(kotlin.math.abs(elapsedSeconds(first) - 800.0) > 1.0)

        val second = result.splits[1]
        assertEquals(900.0, second.distanceMeters, 0.01)
        assertEquals(260.0, secondsFromStart(second.startTime), 0.05)
        assertEquals(540.0, elapsedSeconds(second), 0.5)
        assertTrue(second.isPartial)
    }

    @Test
    fun `several boundaries inside one long segment are each interpolated`() {
        // Two fixes, 3 km apart, 300 s apart: constant 10 m/s. Splits must land
        // at 100 s, 200 s, 300 s.
        val workout = workout(
            totalDistanceMeters = 3000.0,
            endTime = at(300),
            routePoints = listOf(point(0, 0.0), point(300, 3000.0)),
        )

        val result = compute(workout)

        assertEquals(3, result.splits.size)
        assertEquals(100.0, secondsFromStart(result.splits[0].endTime), 0.05)
        assertEquals(200.0, secondsFromStart(result.splits[1].endTime), 0.05)
        assertEquals(300.0, secondsFromStart(result.splits[2].endTime), 0.05)
        result.splits.forEach { assertEquals(1000.0, it.distanceMeters, 0.01) }
        result.splits.forEach { assertFalse(it.isPartial) }
    }

    @Test
    fun `a custom split distance (5 km, the cyclist case) is honoured`() {
        val workout = workout(
            totalDistanceMeters = 12000.0,
            endTime = at(1200),
            routePoints = listOf(point(0, 0.0), point(1200, 12000.0)),
        )

        val result = compute(workout, splitDistanceMeters = 5000.0)

        assertEquals(3, result.splits.size)
        assertEquals(5000.0, result.splits[0].distanceMeters, 0.01)
        assertEquals(5000.0, result.splits[1].distanceMeters, 0.01)
        assertEquals(2000.0, result.splits[2].distanceMeters, 0.01)
        assertEquals(listOf(false, false, true), result.splits.map { it.isPartial })
    }

    @Test
    fun `a trailing partial split is kept and flagged`() {
        val workout = workout(
            totalDistanceMeters = 1400.0,
            endTime = at(420),
            routePoints = listOf(point(0, 0.0), point(420, 1400.0)),
        )

        val result = compute(workout)

        assertEquals(2, result.splits.size)
        assertTrue(result.splits.last().isPartial)
        assertEquals(400.0, result.splits.last().distanceMeters, 0.01)
        assertEquals(120.0, elapsedSeconds(result.splits.last()), 0.5)
    }

    @Test
    fun `elevation gain and loss come from the route altitudes`() {
        // Up 20 m over the first km, back down 8 m over the second.
        val workout = workout(
            totalDistanceMeters = 2000.0,
            endTime = at(600),
            routePoints = listOf(
                point(0, 0.0, altitudeMeters = 100.0),
                point(150, 500.0, altitudeMeters = 110.0),
                point(300, 1000.0, altitudeMeters = 120.0),
                point(450, 1500.0, altitudeMeters = 116.0),
                point(600, 2000.0, altitudeMeters = 112.0),
            ),
        )

        val result = compute(workout)

        assertEquals(20.0, result.splits[0].elevationGainMeters!!, 0.01)
        assertEquals(0.0, result.splits[0].elevationLossMeters!!, 0.01)
        assertEquals(0.0, result.splits[1].elevationGainMeters!!, 0.01)
        assertEquals(8.0, result.splits[1].elevationLossMeters!!, 0.01)
    }

    @Test
    fun `altitude at a mid-segment boundary is interpolated, and no interior fix is lost from the next split`() {
        // Boundary at 1 km falls halfway between the 750 m fix (110 m) and the
        // 1250 m fix (130 m) -> 120 m. Split 1 gains 20 m, split 2 gains 10 m.
        val workout = workout(
            totalDistanceMeters = 2000.0,
            endTime = at(600),
            routePoints = listOf(
                point(0, 0.0, altitudeMeters = 100.0),
                point(225, 750.0, altitudeMeters = 110.0),
                point(375, 1250.0, altitudeMeters = 130.0),
                point(600, 2000.0, altitudeMeters = 130.0),
            ),
        )

        val result = compute(workout)

        assertEquals(20.0, result.splits[0].elevationGainMeters!!, 0.05)
        // 120 m at the boundary -> 130 m at the 1250 m fix -> flat to the end.
        assertEquals(10.0, result.splits[1].elevationGainMeters!!, 0.05)
        assertEquals(0.0, result.splits[1].elevationLossMeters!!, 0.05)
    }

    @Test
    fun `a route without altitudes reports null elevation, not zero`() {
        val workout = workout(
            totalDistanceMeters = 2000.0,
            endTime = at(600),
            routePoints = listOf(point(0, 0.0), point(600, 2000.0)),
        )

        val result = compute(workout)

        assertNull(result.splits.first().elevationGainMeters)
        assertNull(result.splits.first().elevationLossMeters)
    }

    // --- speed-sample splits (the treadmill case) ---

    @Test
    fun `integrate v dt to cut at the right times, with no route at all`() {
        // A treadmill at a constant 4 m/s: the 1 km mark falls at 250 s, 2 km at
        // 500 s. Samples every 100 s, so both boundaries are mid-sample and must
        // be interpolated.
        val workout = workout(
            totalDistanceMeters = 2400.0,
            endTime = at(600),
        )

        val result = compute(
            workout,
            speeds = (0..6).map { speed(it * 100, 4.0) },
        )

        assertEquals(SplitSource.SPEED_SAMPLES, result.source)
        assertEquals(3, result.splits.size)
        assertEquals(250.0, secondsFromStart(result.splits[0].endTime), 0.05)
        assertEquals(500.0, secondsFromStart(result.splits[1].endTime), 0.05)
        assertEquals(1000.0, result.splits[0].distanceMeters, 0.01)
        assertEquals(1000.0, result.splits[1].distanceMeters, 0.01)
        assertEquals(400.0, result.splits[2].distanceMeters, 0.01)
        assertTrue(result.splits[2].isPartial)
        // No route -> no elevation claim.
        assertNull(result.splits[0].elevationGainMeters)
        assertNull(result.splits[0].elevationLossMeters)
    }

    @Test
    fun `a changing belt speed integrates trapezoidally`() {
        // 0-200 s at 5 m/s (1000 m) then 200-400 s at 2.5 m/s (500 m).
        // The 1 km boundary lands exactly on the 200 s sample.
        val workout = workout(
            totalDistanceMeters = 1500.0,
            endTime = at(400),
        )

        val result = compute(
            workout,
            speeds = listOf(
                speed(0, 5.0),
                speed(100, 5.0),
                speed(200, 5.0),
                speed(300, 2.5),
                speed(400, 2.5),
            ),
        )

        assertEquals(SplitSource.SPEED_SAMPLES, result.source)
        assertEquals(2, result.splits.size)
        assertEquals(1000.0, result.splits[0].distanceMeters, 0.01)
        assertEquals(200.0, secondsFromStart(result.splits[0].endTime), 0.05)
        // The 200-300 s ramp averages 3.75 m/s = 375 m, plus 250 m to the end.
        assertEquals(625.0, result.splits[1].distanceMeters, 0.01)
        assertTrue(result.splits[1].isPartial)
    }

    // --- the estimated fallback ---

    @Test
    fun `every estimated split shares the activity average pace, and the source says so`() {
        // 5 km in 25 min, nothing else: no laps, no route, no speed samples.
        val workout = workout(
            totalDistanceMeters = 5000.0,
            endTime = at(1500),
        )

        val result = compute(workout)

        assertEquals(SplitSource.ESTIMATED, result.source)
        assertEquals(5, result.splits.size)
        for (split in result.splits) {
            assertEquals(1000.0, split.distanceMeters, 0.001)
            assertEquals(300.0, elapsedSeconds(split), 0.001)
            // Identical pace on every row: the honest, useless answer.
            assertEquals(0.3, split.paceSecondsPerMeter!!, 1e-6)
            assertEquals(0.0, split.paceDeltaSeconds!!, 1e-6)
            assertNull(split.elevationGainMeters)
        }
        assertFalse(result.splits.last().isPartial)
    }

    @Test
    fun `an odd total distance still yields a flagged trailing partial`() {
        val workout = workout(
            totalDistanceMeters = 2500.0,
            endTime = at(1000),
        )

        val result = compute(workout)

        assertEquals(SplitSource.ESTIMATED, result.source)
        assertEquals(listOf(1000.0, 1000.0, 500.0), result.splits.map { it.distanceMeters })
        assertEquals(listOf(false, false, true), result.splits.map { it.isPartial })
        // Evenly spread in time: 400 s + 400 s + 200 s.
        assertEquals(400.0, elapsedSeconds(result.splits[0]), 0.001)
        assertEquals(200.0, elapsedSeconds(result.splits[2]), 0.001)
    }

    @Test
    fun `a single speed sample cannot be integrated, so it falls back to estimated`() {
        val workout = workout(
            totalDistanceMeters = 2000.0,
            endTime = at(600),
        )

        val result = compute(workout, speeds = listOf(speed(10, 3.5)))

        assertEquals(SplitSource.ESTIMATED, result.source)
        assertEquals(2, result.splits.size)
    }

    // --- average heart rate ---

    @Test
    fun `average heart rate covers only the samples inside the split window`() {
        // Two 1 km splits, each 300 s.
        val workout = workout(
            totalDistanceMeters = 2000.0,
            endTime = at(600),
            routePoints = listOf(point(0, 0.0), point(600, 2000.0)),
        )

        val result = compute(
            workout,
            heartRates = listOf(
                hr(10, 100), // split 1
                hr(150, 110), // split 1
                hr(290, 120), // split 1
                hr(310, 160), // split 2
                hr(500, 170), // split 2
            ),
        )

        assertEquals(110.0, result.splits[0].averageHeartRateBpm!!, 1e-9) // (100+110+120)/3
        assertEquals(165.0, result.splits[1].averageHeartRateBpm!!, 1e-9) // (160+170)/2
    }

    @Test
    fun `the split window is half-open - a sample exactly on the boundary belongs to the NEXT split, never to both`() {
        // The estimated source cuts on exact times (300 s / 600 s), so the
        // boundary can be probed precisely — unlike an interpolated GPS
        // crossing, which lands a hair either side of the round number.
        val workout = workout(
            totalDistanceMeters = 2000.0,
            endTime = at(600),
        )

        val result = compute(
            workout,
            heartRates = listOf(
                hr(0, 100), // split 1: the start IS included
                hr(300, 160), // exactly on the boundary -> split 2
                hr(400, 170), // split 2
            ),
        )

        assertEquals(SplitSource.ESTIMATED, result.source)
        assertEquals(100.0, result.splits[0].averageHeartRateBpm!!, 1e-9)
        assertEquals(165.0, result.splits[1].averageHeartRateBpm!!, 1e-9) // (160+170)/2
    }

    @Test
    fun `average heart rate is null, not zero, when no sample falls inside the split`() {
        val workout = workout(
            totalDistanceMeters = 2000.0,
            endTime = at(600),
            routePoints = listOf(point(0, 0.0), point(600, 2000.0)),
        )

        val result = compute(workout, heartRates = listOf(hr(10, 130)))

        assertEquals(130.0, result.splits[0].averageHeartRateBpm!!, 1e-9)
        assertNull(result.splits[1].averageHeartRateBpm)
    }

    @Test
    fun `unsorted heart-rate samples are still bucketed correctly`() {
        val workout = workout(
            totalDistanceMeters = 2000.0,
            endTime = at(600),
            routePoints = listOf(point(0, 0.0), point(600, 2000.0)),
        )

        val result = compute(
            workout,
            heartRates = listOf(hr(500, 170), hr(0, 100), hr(400, 150), hr(100, 120)),
        )

        assertEquals(110.0, result.splits[0].averageHeartRateBpm!!, 1e-9) // (100+120)/2
        assertEquals(160.0, result.splits[1].averageHeartRateBpm!!, 1e-9) // (150+170)/2
    }

    // --- paceDeltaSeconds ---

    @Test
    fun `pace delta is negative for a faster split and positive for a slower one`() {
        // 2 km in 600 s -> the activity averages 300 s/km. The first km takes
        // 240 s (60 s faster), the second 360 s (60 s slower).
        val workout = workout(
            totalDistanceMeters = 2000.0,
            endTime = at(600),
            routePoints = listOf(point(0, 0.0), point(240, 1000.0), point(600, 2000.0)),
        )

        val result = compute(workout)

        assertEquals(-60.0, result.splits[0].paceDeltaSeconds!!, 0.5)
        assertEquals(60.0, result.splits[1].paceDeltaSeconds!!, 0.5)
        // Same numbers, re-expressed per mile at the display boundary.
        assertEquals(-96.56, result.splits[0].paceDeltaSecondsPerUnit(1609.344)!!, 0.5)
    }

    @Test
    fun `pace delta measures against the ACTIVITY average, not the mean of the split paces`() {
        // 1 km at 300 s/km, then a 100 m sprint at 200 s/km. The mean of the two
        // split paces is 250 s/km; the activity's real average is
        // (300 + 20) s / 1.1 km = 290.9 s/km. A short partial cannot skew the
        // baseline.
        val workout = workout(
            totalDistanceMeters = 1100.0,
            endTime = at(320),
            routePoints = listOf(point(0, 0.0), point(300, 1000.0), point(320, 1100.0)),
        )

        val result = compute(workout)

        assertEquals(9.09, result.splits[0].paceDeltaSeconds!!, 0.5)
        assertEquals(-90.9, result.splits[1].paceDeltaSeconds!!, 0.5)
    }

    // --- no distance ---

    @Test
    fun `a session with no distance, no route and no speed has no splits`() {
        val workout = workout(endTime = at(3600))

        val result = compute(workout)

        assertTrue(result.splits.isEmpty())
        assertTrue(result.isEmpty)
    }

    @Test
    fun `a zero total distance has no splits`() {
        val workout = workout(totalDistanceMeters = 0.0, endTime = at(1800))

        assertTrue(compute(workout).splits.isEmpty())
    }

    @Test
    fun `heart-rate samples alone do not conjure splits`() {
        val workout = workout(endTime = at(1800))

        val result = compute(
            workout,
            heartRates = listOf(hr(0, 100), hr(600, 140), hr(1200, 150)),
        )

        assertTrue(result.splits.isEmpty())
    }

    // --- degenerate input does not crash or divide by zero ---

    @Test
    fun `a single route point`() {
        val workout = workout(
            totalDistanceMeters = 1000.0,
            endTime = at(300),
            routePoints = listOf(point(0, 0.0)),
        )

        val result = compute(workout)

        // One fix carries no distance: fall through to the estimated source.
        assertEquals(SplitSource.ESTIMATED, result.source)
        assertEquals(1, result.splits.size)
    }

    @Test
    fun `duplicated route points (zero-length segments)`() {
        val workout = workout(
            totalDistanceMeters = 1000.0,
            endTime = at(300),
            routePoints = listOf(
                point(0, 0.0),
                point(60, 0.0),
                point(120, 0.0),
                point(300, 1000.0),
            ),
        )

        val result = compute(workout)

        assertEquals(SplitSource.ROUTE, result.source)
        assertEquals(1, result.splits.size)
        assertEquals(1000.0, result.splits.first().distanceMeters, 0.01)
    }

    @Test
    fun `zero duration`() {
        val workout = workout(
            totalDistanceMeters = 1000.0,
            durationMs = 0L,
            endTime = start,
        )

        val result = compute(workout)

        assertEquals(1, result.splits.size)
        val split = result.splits.first()
        assertEquals(0L, split.elapsedMs)
        // Pace is undefined, not infinite, and not zero.
        assertNull(split.paceSecondsPerMeter)
        assertNull(split.paceDeltaSeconds)
    }

    @Test
    fun `a route whose fixes all share one timestamp`() {
        val workout = workout(
            totalDistanceMeters = 2000.0,
            endTime = start,
            durationMs = 0L,
            routePoints = listOf(point(0, 0.0), point(0, 1000.0), point(0, 2000.0)),
        )

        val result = compute(workout)

        assertEquals(SplitSource.ROUTE, result.source)
        assertEquals(2, result.splits.size)
        assertEquals(0L, result.splits.first().elapsedMs)
        assertNull(result.splits.first().paceDeltaSeconds)
    }

    @Test
    fun `unsorted route points and speed samples are sorted first`() {
        val unsorted = listOf(
            point(600, 2000.0),
            point(0, 0.0),
            point(300, 1000.0),
        )
        val workout = workout(
            totalDistanceMeters = 2000.0,
            endTime = at(600),
            routePoints = unsorted,
        )

        val result = compute(workout)

        assertEquals(SplitSource.ROUTE, result.source)
        assertEquals(2, result.splits.size)
        assertEquals(300.0, secondsFromStart(result.splits[0].endTime), 0.05)
        assertEquals(600.0, secondsFromStart(result.splits[1].endTime), 0.05)
    }

    @Test
    fun `a zero or negative split distance falls back to 1 km rather than looping forever`() {
        val workout = workout(
            totalDistanceMeters = 3000.0,
            endTime = at(900),
            routePoints = listOf(point(0, 0.0), point(900, 3000.0)),
        )

        assertEquals(3, compute(workout, splitDistanceMeters = 0.0).splits.size)
        assertEquals(3, compute(workout, splitDistanceMeters = -5.0).splits.size)
        assertEquals(3, compute(workout, splitDistanceMeters = Double.NaN).splits.size)
    }

    @Test
    fun `an absurdly small split distance is capped instead of building a million rows`() {
        val workout = workout(
            totalDistanceMeters = 10000.0,
            endTime = at(3000),
            routePoints = listOf(point(0, 0.0), point(3000, 10000.0)),
        )

        val result = compute(workout, splitDistanceMeters = 0.01)

        assertTrue(result.splits.size <= 500)
    }

    @Test
    fun `a lap that ends before it starts is discarded`() {
        val workout = workout(
            totalDistanceMeters = 1000.0,
            endTime = at(300),
            laps = listOf(
                ExerciseLapData(startTime = at(200), endTime = at(100), lengthMeters = 400.0),
            ),
        )

        val result = compute(workout)

        // The only lap was nonsense -> fall through to the next usable source.
        assertEquals(SplitSource.ESTIMATED, result.source)
        assertEquals(1, result.splits.size)
    }

    @Test
    fun `a lap with no recorded length borrows the route distance`() {
        val workout = workout(
            totalDistanceMeters = 2000.0,
            endTime = at(600),
            routePoints = listOf(point(0, 0.0), point(300, 1200.0), point(600, 2000.0)),
            laps = listOf(
                ExerciseLapData(startTime = at(0), endTime = at(300), lengthMeters = null),
                ExerciseLapData(startTime = at(300), endTime = at(600), lengthMeters = null),
            ),
        )

        val result = compute(workout)

        assertEquals(SplitSource.DEVICE_LAPS, result.source)
        assertEquals(1200.0, result.splits[0].distanceMeters, 0.5)
        assertEquals(800.0, result.splits[1].distanceMeters, 0.5)
    }

    // --- paused recordings ---

    @Test
    fun `a pause inside a split does not count as time spent covering it`() {
        // The ride this comes from: 21 minutes wall-clock with a 10½-minute pause
        // in it, 1421 m ridden. It reported a 4.1 km/h first kilometre and a
        // 4.9 km/h average, because the pause sat inside that kilometre's window.
        // The rider was doing about 12.
        //
        // 1000 m covered in 900 s of window, 600 s of which was paused: five
        // minutes of riding, not fifteen.
        val workout = workout(
            totalDistanceMeters = 1000.0,
            endTime = at(900),
            routePoints = listOf(
                point(0, 0.0),
                point(60, 100.0),
                // The pause: the recording logs nothing across it, so the next fix is
                // ten minutes later and barely any further on.
                point(660, 120.0),
                point(900, 1000.0),
            ),
            segments = listOf(pause(60, 660)),
        )

        val split = compute(workout).splits.single()

        assertEquals(1000.0, split.distanceMeters, 1.0)
        assertEquals(300_000L, split.elapsedMs)
        // 1000 m in 300 s = 12 km/h, not the 4 km/h the wall clock claimed.
        assertEquals(0.3, split.paceSecondsPerMeter!!, 0.001)
    }

    @Test
    fun `a pause outside a split leaves it alone`() {
        val workout = workout(
            totalDistanceMeters = 1000.0,
            endTime = at(900),
            routePoints = listOf(
                point(0, 0.0),
                point(300, 1000.0),
                point(900, 1000.0),
            ),
            // Paused AFTER the kilometre was done.
            segments = listOf(pause(300, 900)),
        )

        val split = compute(workout).splits.first()

        assertEquals(300_000L, split.elapsedMs)
    }

    @Test
    fun `a split cannot come out negative however the pauses overlap`() {
        // A source app that writes overlapping pause segments, or one longer than
        // the split it sits in, must not produce a negative time and an inverted
        // pace.
        val workout = workout(
            totalDistanceMeters = 1000.0,
            endTime = at(600),
            routePoints = listOf(point(0, 0.0), point(600, 1000.0)),
            segments = listOf(pause(0, 600), pause(0, 600)),
        )

        val split = compute(workout).splits.single()

        assertEquals(0L, split.elapsedMs)
        // No time means no pace at all, rather than an infinite one.
        assertNull(split.paceSecondsPerMeter)
    }

    // --- estimated source is exempt from pause subtraction ---

    @Test
    fun `estimated splits already spread over moving time do not subtract the pause again`() {
        // 2 km with a 300 s pause in a 900 s session: moving time is 600 s, so
        // each estimated kilometre claims 300 s — subtracting the pause from one
        // of those fictional windows again would double-count it.
        val workout = workout(
            totalDistanceMeters = 2000.0,
            endTime = at(900),
            segments = listOf(pause(100, 400)),
        )

        val result = compute(workout)

        assertEquals(SplitSource.ESTIMATED, result.source)
        assertEquals(2, result.splits.size)
        assertEquals(300.0, elapsedSeconds(result.splits[0]), 0.001)
        assertEquals(300.0, elapsedSeconds(result.splits[1]), 0.001)
    }

    private companion object {
        /**
         * A latitude line: at the equator, moving EAST by `meters` keeps latitude 0
         * and makes the haversine distance exactly `meters`, so a route's geometry
         * can be stated in meters directly.
         *
         * This must use the SAME earth radius as `haversineMeters` (6 371 000 m, a
         * sphere) — the WGS84 equatorial radius would put the fixture 0.11% out and
         * quietly turn every "1000 m" below into 998.9 m.
         */
        const val MetersPerDegreeAtEquator = 6371000.0 * PI / 180.0
    }
}
