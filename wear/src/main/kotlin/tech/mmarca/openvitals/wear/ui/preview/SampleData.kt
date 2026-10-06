package tech.mmarca.openvitals.wear.ui.preview

import tech.mmarca.openvitals.wear.MetricUiState
import tech.mmarca.openvitals.wear.RecordingUiState
import tech.mmarca.openvitals.wear.SettingsUiState
import tech.mmarca.openvitals.wear.SleepStage
import tech.mmarca.openvitals.wear.WearMetric
import tech.mmarca.openvitals.wear.WearUiState
import tech.mmarca.openvitals.wear.features.measure.Measurement
import tech.mmarca.openvitals.wear.features.quicklog.EntryType
import tech.mmarca.openvitals.wear.features.quicklog.LoggedEntry
import tech.mmarca.openvitals.wear.features.recording.ActivityType
import tech.mmarca.openvitals.wear.features.recording.WorkoutField
import tech.mmarca.openvitals.wear.health.WearCapabilities

/**
 * Made-up values for previews and for debug builds, where nothing feeds the
 * screens yet. Never shown in a release build.
 */
object SampleData {
    val metrics = mapOf(
        WearMetric.STEPS to MetricUiState(
            current = 6_482.0,
            goal = 10_000.0,
            today = doubles(0, 0, 0, 0, 0, 0, 120, 840, 1_310, 420, 260, 380, 1_150, 540, 310, 752),
            week = doubles(8_210, 11_034, 5_420, 9_876, 12_310, 7_045, 6_482),
        ),
        WearMetric.HEART_RATE to MetricUiState(
            current = 72.0,
            today = doubles(64, 66, 63, 70, 78, 91, 104, 98, 86, 79, 74, 71, 69, 72, 75, 72),
            week = doubles(68, 71, 66, 70, 74, 69, 73),
        ),
        WearMetric.DISTANCE to MetricUiState(
            current = 4_730.0,
            today = doubles(0, 0, 0, 0, 0, 0, 90, 610, 960, 300, 190, 280, 840, 390, 230, 840),
            week = doubles(6_010, 8_120, 3_950, 7_240, 9_030, 5_160, 4_730),
        ),
        WearMetric.ACTIVE_CALORIES to MetricUiState(
            current = 312.0,
            goal = 500.0,
            today = doubles(0, 0, 0, 0, 0, 0, 6, 41, 63, 20, 13, 18, 55, 26, 15, 55),
            week = doubles(402, 538, 265, 480, 601, 344, 312),
        ),
        WearMetric.FLOORS to MetricUiState(
            current = 9.0,
            goal = 10.0,
            today = doubles(0, 0, 0, 0, 0, 0, 1, 2, 0, 1, 0, 0, 3, 1, 0, 1),
            week = doubles(12, 8, 5, 11, 14, 7, 9),
        ),
        WearMetric.ELEVATION to MetricUiState(
            current = 86.0,
            today = doubles(0, 0, 0, 0, 0, 0, 4, 21, 9, 6, 0, 0, 28, 9, 0, 9),
            week = doubles(112, 74, 40, 96, 131, 62, 86),
        ),
        WearMetric.BLOOD_OXYGEN to MetricUiState(
            current = 97.0,
            today = doubles(97, 96, 98, 97, 95, 96, 97, 98, 97, 97),
            week = doubles(97, 96, 97, 98, 96, 97, 97),
        ),
        WearMetric.VO2_MAX to MetricUiState(
            current = 44.0,
            week = doubles(43, 43, 44, 44, 44, 44, 44),
        ),
        WearMetric.RESTING_HEART_RATE to MetricUiState(
            current = 58.0,
            week = doubles(60, 59, 61, 58, 57, 59, 58),
        ),
        WearMetric.HRV to MetricUiState(
            current = 46.0,
            week = doubles(41, 48, 39, 52, 44, 47, 46),
        ),
        WearMetric.SLEEP to MetricUiState(
            current = 442.0,
            goal = 480.0,
            week = doubles(401, 468, 377, 455, 512, 430, 442),
        ),
        WearMetric.HYDRATION to MetricUiState(
            current = 1_250.0,
            goal = 2_000.0,
            today = doubles(0, 0, 0, 0, 0, 0, 0, 250, 250, 0, 0, 0, 500, 0, 0, 250),
            week = doubles(1_750, 2_250, 1_500, 2_000, 1_250, 1_750, 1_250),
        ),
        WearMetric.WEIGHT to MetricUiState(
            current = 72.4,
            week = listOf(73.1, 72.9, 72.8, 72.6, 72.7, 72.5, 72.4),
        ),
    )

    /** Minutes per stage for a 7:22 night. */
    val sleepStages = mapOf(
        SleepStage.AWAKE to 21.0,
        SleepStage.REM to 94.0,
        SleepStage.LIGHT to 243.0,
        SleepStage.DEEP to 84.0,
    )

    /** A watch with Health Services, a barometer and no single-beat sensor. */
    val capabilities = WearCapabilities(
        probed = true,
        sensorMetrics = WearMetric.entries.toSet() - WearMetric.BLOOD_OXYGEN,
        measurements = setOf(Measurement.HEART_RATE, Measurement.AIR_PRESSURE),
        workouts = ActivityType.entries.associateWith { activity ->
            buildSet {
                add(WorkoutField.HEART_RATE)
                add(WorkoutField.CALORIES)
                if (activity != ActivityType.WORKOUT) {
                    add(WorkoutField.DISTANCE)
                    add(WorkoutField.ELEVATION)
                    add(if (activity == ActivityType.CYCLE) WorkoutField.SPEED else WorkoutField.PACE)
                }
            }
        },
    )

    val entries = listOf(
        LoggedEntry(EntryType.WEIGHT, System.currentTimeMillis() - 3_600_000, 72.4),
        LoggedEntry(EntryType.HEART_RATE, System.currentTimeMillis() - 1_800_000, 64.0),
        LoggedEntry(EntryType.BLOOD_PRESSURE, System.currentTimeMillis() - 600_000, 121.0, 79.0),
    )

    val recording = RecordingUiState(
        elapsedSeconds = 23 * 60 + 41,
        heartRateBpm = 138,
        distanceMeters = 3_240,
        caloriesKcal = 214,
        elevationGainMeters = 38,
        speedMetersPerSecond = 2.28,
        averageHeartRateBpm = 131,
    )

    val settings = SettingsUiState(
        phoneConnected = true,
        sensorAccessGranted = true,
    )

    /** A watch with a barometer and no SpO2 sensor, so one metric is not on offer. */
    val uiState = WearUiState(
        availableMetrics = capabilities.availableMetrics,
        metrics = metrics,
        recording = recording,
        settings = settings,
        capabilities = capabilities,
        sleepStages = sleepStages,
        entries = entries,
    )

    private fun doubles(vararg values: Int): List<Double> = values.map(Int::toDouble)
}
