package tech.mmarca.openvitals.domain.preferences

import org.junit.Assert.assertEquals
import org.junit.Test

class ActivityRecordingPreferencesTest {

    @Test fun `defaults match practical recording settings`() {
        val preferences = ActivityRecordingPreferences()

        assertEquals(true, preferences.autoIdleEnabled)
        assertEquals(10, preferences.autoIdleTimeoutSeconds)
        assertEquals(30, preferences.requiredGpsAccuracyMeters)
        assertEquals(200, preferences.routeGapMeters)
        assertEquals(true, preferences.barometerClimbEnabled)
        assertEquals(null, preferences.recordingDistanceIntervalMeters)
        assertEquals(500, preferences.recordingTimeIntervalMillis)
        assertEquals(false, preferences.voiceAnnouncementsEnabled)
        assertEquals(5, preferences.voiceAnnouncementTimeIntervalMinutes)
        assertEquals(1_000, preferences.voiceAnnouncementDistanceIntervalMeters)
    }

    @Test fun `normalized coerces timeout and closest practical options`() {
        val normalized = ActivityRecordingPreferences(
            autoIdleTimeoutSeconds = 2,
            requiredGpsAccuracyMeters = 42,
            routeGapMeters = 410,
            recordingDistanceIntervalMeters = 12,
            recordingTimeIntervalMillis = 800,
            voiceAnnouncementTimeIntervalMinutes = 7,
            voiceAnnouncementDistanceIntervalMeters = 700,
        ).normalized()

        assertEquals(5, normalized.autoIdleTimeoutSeconds)
        assertEquals(50, normalized.requiredGpsAccuracyMeters)
        assertEquals(500, normalized.routeGapMeters)
        assertEquals(10, normalized.recordingDistanceIntervalMeters)
        assertEquals(1_000, normalized.recordingTimeIntervalMillis)
        assertEquals(5, normalized.voiceAnnouncementTimeIntervalMinutes)
        assertEquals(500, normalized.voiceAnnouncementDistanceIntervalMeters)
    }

    @Test fun `normalized keeps disabled route gap disabled`() {
        val normalized = ActivityRecordingPreferences(routeGapMeters = null).normalized()

        assertEquals(null, normalized.routeGapMeters)
    }
}
