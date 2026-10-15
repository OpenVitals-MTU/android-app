package tech.mmarca.openvitals.domain.preferences

import org.junit.Assert.assertEquals
import org.junit.Test

class ActivityRecordingPreferencesTest {

    @Test fun `defaults match practical recording settings`() {
        val preferences = ActivityRecordingPreferences()

        assertEquals(true, preferences.autoIdleEnabled)
        assertEquals(10, preferences.autoIdleTimeoutSeconds)
        assertEquals(false, preferences.keepScreenOnDuringRecording)
        assertEquals(30, preferences.requiredGpsAccuracyMeters)
        assertEquals(200, preferences.routeGapMeters)
        assertEquals(true, preferences.barometerClimbEnabled)
        assertEquals(null, preferences.recordingDistanceIntervalMeters)
        assertEquals(500, preferences.recordingTimeIntervalMillis)
        assertEquals(false, preferences.voiceAnnouncementsEnabled)
        assertEquals(5, preferences.voiceAnnouncementTimeIntervalMinutes)
        assertEquals(1_000, preferences.voiceAnnouncementDistanceIntervalMeters)
        assertEquals(true, preferences.restTimerBellEnabled)
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

    @Test fun `dashboard layout normalization keeps unique fields within template capacity`() {
        val normalized = ActivityRecordingDashboardLayout(
            template = ActivityRecordingDashboardTemplate.TWO_BY_FOUR,
            fields = listOf(
                ActivityRecordingDashboardField.HEART_RATE,
                ActivityRecordingDashboardField.HEART_RATE,
                ActivityRecordingDashboardField.CADENCE,
                ActivityRecordingDashboardField.SPEED,
                ActivityRecordingDashboardField.DISTANCE,
                ActivityRecordingDashboardField.DURATION,
                ActivityRecordingDashboardField.MOVING_TIME,
                ActivityRecordingDashboardField.AVERAGE_SPEED,
                ActivityRecordingDashboardField.MAX_SPEED,
            ),
        ).normalized()

        assertEquals(ActivityRecordingDashboardTemplate.LARGE_TOP, normalized.template)
        assertEquals(24, normalized.capacity)
        assertEquals(8, normalized.fields.size)
        assertEquals(
            listOf(
                ActivityRecordingDashboardField.HEART_RATE,
                ActivityRecordingDashboardField.CADENCE,
                ActivityRecordingDashboardField.SPEED,
                ActivityRecordingDashboardField.DISTANCE,
                ActivityRecordingDashboardField.DURATION,
                ActivityRecordingDashboardField.MOVING_TIME,
                ActivityRecordingDashboardField.AVERAGE_SPEED,
                ActivityRecordingDashboardField.MAX_SPEED,
            ),
            normalized.fields,
        )
    }

    @Test fun `dashboard layout normalization preserves resized fields that fit`() {
        val normalized = ActivityRecordingDashboardLayout(
            template = ActivityRecordingDashboardTemplate.TWO_BY_FOUR,
            fields = listOf(
                ActivityRecordingDashboardField.HEART_RATE,
                ActivityRecordingDashboardField.CADENCE,
                ActivityRecordingDashboardField.SPEED,
                ActivityRecordingDashboardField.DISTANCE,
            ),
            sizes = mapOf(
                ActivityRecordingDashboardField.HEART_RATE to ActivityRecordingDashboardItemSize.LARGE,
                ActivityRecordingDashboardField.CADENCE to ActivityRecordingDashboardItemSize.WIDE,
            ),
        ).normalized()

        assertEquals(
            listOf(
                ActivityRecordingDashboardField.HEART_RATE,
                ActivityRecordingDashboardField.CADENCE,
                ActivityRecordingDashboardField.SPEED,
                ActivityRecordingDashboardField.DISTANCE,
            ),
            normalized.fields,
        )
        assertEquals(ActivityRecordingDashboardItemSize.LARGE, normalized.sizes[ActivityRecordingDashboardField.HEART_RATE])
        assertEquals(ActivityRecordingDashboardItemSize.WIDE, normalized.sizes[ActivityRecordingDashboardField.CADENCE])
        assertEquals(ActivityRecordingDashboardItemSize.SMALL, normalized.sizes[ActivityRecordingDashboardField.SPEED])
    }

    @Test fun `dashboard layout normalization supports tall resized fields`() {
        val normalized = ActivityRecordingDashboardLayout(
            fields = listOf(
                ActivityRecordingDashboardField.HEART_RATE,
                ActivityRecordingDashboardField.CADENCE,
                ActivityRecordingDashboardField.SPEED,
                ActivityRecordingDashboardField.DISTANCE,
            ),
            sizes = mapOf(
                ActivityRecordingDashboardField.HEART_RATE to ActivityRecordingDashboardItemSize(
                    columnSpan = 3,
                    rowSpan = 3,
                ),
                ActivityRecordingDashboardField.CADENCE to ActivityRecordingDashboardItemSize(
                    columnSpan = 1,
                    rowSpan = 3,
                ),
                ActivityRecordingDashboardField.SPEED to ActivityRecordingDashboardItemSize(
                    columnSpan = 2,
                    rowSpan = 3,
                ),
                ActivityRecordingDashboardField.DISTANCE to ActivityRecordingDashboardItemSize.SMALL,
            ),
        ).normalized()

        assertEquals(
            ActivityRecordingDashboardItemSize(columnSpan = 3, rowSpan = 3),
            normalized.sizes[ActivityRecordingDashboardField.HEART_RATE],
        )
        assertEquals(
            ActivityRecordingDashboardItemSize(columnSpan = 1, rowSpan = 3),
            normalized.sizes[ActivityRecordingDashboardField.CADENCE],
        )
        assertEquals(
            ActivityRecordingDashboardItemSize(columnSpan = 2, rowSpan = 3),
            normalized.sizes[ActivityRecordingDashboardField.SPEED],
        )
        assertEquals(ActivityRecordingDashboardItemSize.SMALL, normalized.sizes[ActivityRecordingDashboardField.DISTANCE])
    }

    @Test fun `dashboard layout normalization drops lowest right resized fields that cannot fit`() {
        val normalized = ActivityRecordingDashboardLayout(
            fields = listOf(
                ActivityRecordingDashboardField.HEART_RATE,
                ActivityRecordingDashboardField.CADENCE,
                ActivityRecordingDashboardField.SPEED,
                ActivityRecordingDashboardField.DISTANCE,
                ActivityRecordingDashboardField.DURATION,
            ),
            sizes = mapOf(
                ActivityRecordingDashboardField.HEART_RATE to ActivityRecordingDashboardItemSize(
                    columnSpan = 4,
                    rowSpan = 2,
                ),
                ActivityRecordingDashboardField.CADENCE to ActivityRecordingDashboardItemSize(
                    columnSpan = 4,
                    rowSpan = 2,
                ),
                ActivityRecordingDashboardField.SPEED to ActivityRecordingDashboardItemSize(
                    columnSpan = 4,
                    rowSpan = 2,
                ),
                ActivityRecordingDashboardField.DISTANCE to ActivityRecordingDashboardItemSize(
                    columnSpan = 4,
                    rowSpan = 2,
                ),
                ActivityRecordingDashboardField.DURATION to ActivityRecordingDashboardItemSize(
                    columnSpan = 4,
                    rowSpan = 2,
                ),
            ),
        ).normalized()

        assertEquals(
            listOf(
                ActivityRecordingDashboardField.HEART_RATE,
                ActivityRecordingDashboardField.CADENCE,
                ActivityRecordingDashboardField.SPEED,
            ),
            normalized.fields,
        )
    }
}
