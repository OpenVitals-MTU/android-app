package tech.mmarca.openvitals.features.manualentry.activity.recording

import android.location.Location
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import java.time.Instant
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import tech.mmarca.openvitals.domain.model.ExerciseRoutePoint
import tech.mmarca.openvitals.domain.preferences.ActivityRecordingPreferences

/**
 * The live GPS accumulator: every number the recording screen shows while you
 * are moving — distance, elevation, speed, the route and its breaks, and the
 * auto-idle clock — is folded here, one fix at a time.
 *
 * It used to live inside the controller's `acceptConvertedLocation`, welded to
 * the Android location stack, and so had no tests at all. These are those tests.
 *
 * `Location.distanceBetween` is a platform static with no JVM implementation, so
 * it is stubbed here with the same WGS84 geodesy Android uses: a milli-degree of
 * latitude at the equator is the meridian arc — 110.574 m, not the 111.19 m
 * "mean degree" you get from a sphere. The arithmetic under test is the fold,
 * not the ellipsoid.
 */
class ActivityRecordingAcceptedLocationTest {

    private val start: Instant = Instant.parse("2026-01-01T10:00:00Z")

    private val metersPerMilliDegreeLat = 110.574

    @Before fun setUp() {
        mockkStatic(Location::class)
        every {
            Location.distanceBetween(any(), any(), any(), any(), any())
        } answers {
            val results = arg<FloatArray>(4)
            results[0] = wgs84DistanceMeters(
                startLatitude = arg(0),
                startLongitude = arg(1),
                endLatitude = arg(2),
                endLongitude = arg(3),
            ).toFloat()
        }
    }

    @After fun tearDown() {
        unmockkStatic(Location::class)
    }

    private fun point(
        seconds: Long,
        latMilliDegrees: Double = 0.0,
        altitudeMeters: Double? = null,
        accuracyMeters: Double? = 5.0,
    ): ExerciseRoutePoint = ExerciseRoutePoint(
        time = start.plusSeconds(seconds),
        latitude = latMilliDegrees * 0.001,
        longitude = 0.0,
        altitudeMeters = altitudeMeters,
        horizontalAccuracyMeters = accuracyMeters,
        verticalAccuracyMeters = null,
    )

    /**
     * Distance and time gates opened up, so a test only fights the gate it means
     * to. `routeGapMeters = null` means "never break the route".
     */
    private val preferences = ActivityRecordingPreferences(
        recordingDistanceIntervalMeters = 5,
        recordingTimeIntervalMillis = 0,
        routeGapMeters = null,
        autoIdleEnabled = false,
    )

    private fun recording(
        points: List<ExerciseRoutePoint> = emptyList(),
        autoIdleEnabled: Boolean = false,
        autoIdleTimeoutMillis: Long = 10_000L,
        lastMovementAt: Instant? = null,
        distanceMeters: Double = 0.0,
        maxSpeedMetersPerSecond: Double = 0.0,
    ): ActivityRecordingState = ActivityRecordingState(
        status = ActivityRecordingStatus.RECORDING,
        recordingKind = ActivityRecordingKind.GPS_ROUTE,
        startTime = start,
        points = points,
        distanceMeters = distanceMeters,
        maxSpeedMetersPerSecond = maxSpeedMetersPerSecond,
        autoIdleEnabled = autoIdleEnabled,
        autoIdleTimeoutMillis = autoIdleTimeoutMillis,
        lastMovementAt = lastMovementAt,
    )

    private fun accept(
        state: ActivityRecordingState,
        p: ExerciseRoutePoint,
        accuracyMeters: Double = 5.0,
        prefs: ActivityRecordingPreferences = preferences,
    ): ActivityRecordingState = state.withAcceptedLocation(
        point = p,
        accuracyMeters = accuracyMeters,
        recordingPreferences = prefs,
    )

    @Test fun `the first fix opens the route but banks no distance or speed`() {
        val result = accept(recording(), point(seconds = 0))

        assertEquals(1, result.points.size)
        assertEquals(0.0, result.distanceMeters, 0.0)
        assertEquals(0.0, result.currentSpeedMetersPerSecond, 0.0)
        assertEquals(ActivityGpsStatus.FIX, result.gpsStatus)
        // There is no leg yet, so there is nothing to be the max of.
        assertEquals(0.0, result.maxSpeedMetersPerSecond, 0.0)
    }

    @Test fun `a second fix accumulates distance, climb and speed`() {
        val first = accept(recording(), point(seconds = 0, altitudeMeters = 100.0))
        val result = accept(
            first,
            point(seconds = 10, latMilliDegrees = 1.0, altitudeMeters = 110.0),
        )

        assertEquals(2, result.points.size)
        assertEquals(metersPerMilliDegreeLat, result.distanceMeters, 0.5)
        assertEquals(10.0, result.elevationGainedMeters, 0.001)
        assertEquals(0.0, result.elevationLostMeters, 0.0)
        // ~110.6 m in 10 s.
        assertEquals(11.06, result.currentSpeedMetersPerSecond, 0.05)
        assertEquals(11.06, result.maxSpeedMetersPerSecond, 0.05)
    }

    @Test fun `a descent accumulates loss, not gain`() {
        val first = accept(recording(), point(seconds = 0, altitudeMeters = 100.0))
        val result = accept(
            first,
            point(seconds = 10, latMilliDegrees = 1.0, altitudeMeters = 92.0),
        )

        assertEquals(0.0, result.elevationGainedMeters, 0.0)
        assertEquals(8.0, result.elevationLostMeters, 0.001)
    }

    @Test fun `max speed is a high-water mark, not the latest speed`() {
        val first = accept(recording(), point(seconds = 0))
        val fast = accept(first, point(seconds = 10, latMilliDegrees = 1.0))
        // Half the ground in the same time: slower, but the peak must survive.
        val slow = accept(fast, point(seconds = 20, latMilliDegrees = 1.5))

        assertTrue(slow.currentSpeedMetersPerSecond < fast.currentSpeedMetersPerSecond)
        assertEquals(fast.maxSpeedMetersPerSecond, slow.maxSpeedMetersPerSecond, 0.001)
    }

    @Test fun `a fix that does not advance the clock is dropped`() {
        val first = accept(recording(), point(seconds = 10, latMilliDegrees = 1.0))
        val result = accept(first, point(seconds = 10, latMilliDegrees = 2.0))

        assertEquals("route must not grow", 1, result.points.size)
        assertEquals(0.0, result.distanceMeters, 0.0)
        assertEquals(1, result.droppedPointCount)
    }

    @Test fun `a fix inside the minimum sample distance is shown but not banked`() {
        val first = accept(recording(), point(seconds = 0))
        // 0.01 milli-degrees ~ 1.1 m, under the 5 m interval.
        val near = point(seconds = 10, latMilliDegrees = 0.01)
        val result = accept(first, near)

        assertEquals("route must not grow", 1, result.points.size)
        assertEquals(0.0, result.distanceMeters, 0.0)
        // ...but the live marker still follows the fix, and it is not a "drop".
        assertEquals(near, result.latestUiPoint)
        assertEquals(0, result.droppedPointCount)
        assertEquals(near.time, result.lastLocationTime)
    }

    @Test fun `a gap wider than routeGapMeters breaks the route and banks no distance`() {
        val gapped = ActivityRecordingPreferences(
            recordingDistanceIntervalMeters = 5,
            recordingTimeIntervalMillis = 0,
            routeGapMeters = 50,
            autoIdleEnabled = false,
        )
        val first = accept(recording(), point(seconds = 0), prefs = gapped)
        // ~111 m, well past the 50 m gap.
        val result = accept(
            first,
            point(seconds = 600, latMilliDegrees = 1.0),
            prefs = gapped,
        )

        assertEquals("the point still joins the route", 2, result.points.size)
        assertEquals("the line breaks before it", listOf(1), result.routeBreakIndexes)
        // The whole point of the break: a tunnel is not 111 m of running.
        assertEquals(0.0, result.distanceMeters, 0.0)
        assertEquals(0.0, result.currentSpeedMetersPerSecond, 0.0)
    }

    @Test fun `an implausible jump is dropped rather than banked`() {
        val first = accept(recording(), point(seconds = 0))
        // ~111 m in 1 s = 111 m/s, far past any plausible speed, and far past the
        // combined accuracy of the two fixes.
        val result = accept(first, point(seconds = 1, latMilliDegrees = 1.0))

        assertEquals(1, result.points.size)
        assertEquals(0.0, result.distanceMeters, 0.0)
        assertEquals(1, result.droppedPointCount)
    }

    @Test fun `auto-idle charges only the stretch beyond the timeout`() {
        val idlePreferences = preferences.copy(autoIdleEnabled = true)
        val first = accept(
            recording(autoIdleEnabled = true, lastMovementAt = start),
            point(seconds = 0),
            prefs = idlePreferences,
        )
        // Moves again 30 s later, with a 10 s idle timeout: 20 s of that was idle,
        // not 30.
        val result = accept(
            first,
            point(seconds = 30, latMilliDegrees = 1.0),
            prefs = idlePreferences,
        )

        assertEquals(20_000L, result.totalIdleMillis)
        assertEquals(start.plusSeconds(30), result.lastMovementAt)
    }

    @Test fun `moving again inside the timeout accrues no idle at all`() {
        val idlePreferences = preferences.copy(autoIdleEnabled = true)
        val first = accept(
            recording(autoIdleEnabled = true, lastMovementAt = start),
            point(seconds = 0),
            prefs = idlePreferences,
        )
        val result = accept(
            first,
            point(seconds = 8, latMilliDegrees = 1.0),
            prefs = idlePreferences,
        )

        assertEquals(0L, result.totalIdleMillis)
    }

    @Test fun `a route break does not stop the auto-idle clock`() {
        // Regression guard: the break path must leave lastMovementAt alone, so the
        // stationary stretch that CAUSED the gap is still charged as idle when the
        // next real leg lands.
        val gapped = ActivityRecordingPreferences(
            recordingDistanceIntervalMeters = 5,
            recordingTimeIntervalMillis = 0,
            routeGapMeters = 50,
            autoIdleEnabled = true,
        )
        val first = accept(
            recording(autoIdleEnabled = true, lastMovementAt = start),
            point(seconds = 0),
            prefs = gapped,
        )
        val broken = accept(first, point(seconds = 600, latMilliDegrees = 1.0), prefs = gapped)

        assertEquals(listOf(1), broken.routeBreakIndexes)
        assertEquals("the break banks nothing itself", 0L, broken.totalIdleMillis)
        assertEquals("the idle clock keeps running", start, broken.lastMovementAt)
    }
}

private const val WgsSemiMajorAxisMeters = 6_378_137.0
private const val WgsEccentricitySquared = 0.00669437999014

/**
 * The WGS84 geodesic distance over the short legs these tests use — the same
 * ellipsoid `Location.distanceBetween` measures on, via the local radii of
 * curvature rather than the full Vincenty inverse.
 */
private fun wgs84DistanceMeters(
    startLatitude: Double,
    startLongitude: Double,
    endLatitude: Double,
    endLongitude: Double,
): Double {
    val meanLatitudeRadians = Math.toRadians((startLatitude + endLatitude) / 2.0)
    val sinLatitude = sin(meanLatitudeRadians)
    val w = sqrt(1.0 - WgsEccentricitySquared * sinLatitude * sinLatitude)
    val meridianRadius = WgsSemiMajorAxisMeters * (1.0 - WgsEccentricitySquared) / (w * w * w)
    val normalRadius = WgsSemiMajorAxisMeters / w
    val northing = meridianRadius * Math.toRadians(endLatitude - startLatitude)
    val easting = normalRadius * cos(meanLatitudeRadians) * Math.toRadians(endLongitude - startLongitude)
    return hypot(easting, northing)
}
