package tech.mmarca.openvitals.features.manualentry.activity.recording

import android.location.Location
import android.location.LocationManager
import io.mockk.every
import io.mockk.mockk
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.features.manualentry.activity.ActivityRecordingSensor

/**
 * What counts as a fix good enough to start a run on, and what the setup screen
 * does with it.
 *
 * [Location] has no JVM implementation, so the fixes are mocked at the four
 * getters [activityGpsFixQuality] actually reads. The age deliberately comes
 * from the wall clock rather than an elapsed-realtime stamp, so no `SystemClock`
 * is touched.
 */
class ActivityRecordingGpsFixTest {

    private fun location(
        accuracyMeters: Float = 5f,
        ageMillis: Long = 0L,
        latitude: Double = 59.4,
        longitude: Double = 24.7,
        provider: String? = LocationManager.GPS_PROVIDER,
    ): Location {
        val fix = mockk<Location>(relaxed = true)
        every { fix.provider } returns provider
        every { fix.hasAccuracy() } returns true
        every { fix.accuracy } returns accuracyMeters
        every { fix.time } returns System.currentTimeMillis() - ageMillis
        every { fix.elapsedRealtimeNanos } returns 0L
        every { fix.latitude } returns latitude
        every { fix.longitude } returns longitude
        return fix
    }

    @Test fun `activityGpsFixQuality - a fresh, accurate fix is precise`() {
        val quality = location().activityGpsFixQuality(now = Instant.now())

        assertTrue(quality.isPrecise)
        assertEquals(5.0, quality.accuracyMeters!!, 0.0)
    }

    @Test fun `activityGpsFixQuality - a fix worse than the required accuracy is not precise`() {
        val quality = location(accuracyMeters = 100f).activityGpsFixQuality(now = Instant.now())

        assertFalse(quality.isPrecise)
    }

    @Test fun `activityGpsFixQuality - a stale fix is not precise, however accurate`() {
        val quality = location(ageMillis = 30_000L).activityGpsFixQuality(now = Instant.now())

        assertFalse(quality.isPrecise)
    }

    @Test fun `activityGpsFixQuality - a fix from before the session started is not precise`() {
        val now = Instant.now()
        val quality = location().activityGpsFixQuality(
            startTime = now.plusSeconds(60),
            now = now,
        )

        assertFalse(quality.isPrecise)
    }

    @Test fun `PreRecordingGpsFixState - withholds the fix without permission or with GPS off`() {
        val quality = location().activityGpsFixQuality(now = Instant.now())

        assertNull(
            PreRecordingGpsFixState(
                hasPrecisePermission = false,
                gpsProviderEnabled = true,
                latestLocation = location(),
                fixQuality = quality,
            ).latestPreciseFix,
        )
        assertNull(
            PreRecordingGpsFixState(
                hasPrecisePermission = true,
                gpsProviderEnabled = false,
                latestLocation = location(),
                fixQuality = quality,
            ).latestPreciseFix,
        )
    }

    @Test fun `PreRecordingGpsFixState - exposes an initial fix once everything lines up`() {
        val state = PreRecordingGpsFixState(
            hasPrecisePermission = true,
            gpsProviderEnabled = true,
            latestLocation = location(),
            fixQuality = location().activityGpsFixQuality(now = Instant.now()),
        )

        assertNotNull(state.latestPreciseFix)
        assertEquals(59.4, state.latestPreciseFix!!.latitude, 0.0)
        assertEquals(5.0, state.fixQuality!!.accuracyMeters!!, 0.0)
    }

    @Test
    fun `setup screen - switched to record without GPS, a run starts at once - no fix, no permission`() {
        // GPS permission held but no fix: without the switch this run cannot start
        // at all, so a run never starts from an unknown position.
        assertFalse(
            "no fix yet",
            activityRecordingStartEnabled(
                baseEnabled = true,
                recordingWithoutGps = false,
                supportsGpsRoute = true,
                hasPrecisePermission = true,
                hasPreciseFix = false,
                hasRequiredSensor = true,
            ),
        )

        // The whole point of the switch: there is nothing to wait for. No fix, and
        // it does not even matter whether the location permission was ever granted.
        assertTrue(
            activityRecordingStartEnabled(
                baseEnabled = true,
                recordingWithoutGps = true,
                supportsGpsRoute = true,
                hasPrecisePermission = false,
                hasPreciseFix = false,
                hasRequiredSensor = false,
            ),
        )

        val action = activityRecordingStartAction(
            supportsStepCounting = true,
            hasActivityRecognitionPermission = true,
            supportsGpsRoute = true,
            recordingWithoutGps = true,
            // Asking for the location permission for a recording that will never
            // use it is exactly what makes people distrust a health app.
            hasPrecisePermission = false,
            hrrTest = false,
            recordingSensor = ActivityRecordingSensor.GPS,
            latestPreciseFix = location(),
            restSecondsText = "",
        )

        assertEquals(
            // A recording that will never look at a location must not carry one.
            ActivityRecordingStartAction.StartRecording(
                initialFix = null,
                restSeconds = 0L,
                withoutGps = true,
            ),
            action,
        )
    }
}
