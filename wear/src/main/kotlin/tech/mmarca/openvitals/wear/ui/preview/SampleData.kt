package tech.mmarca.openvitals.wear.ui.preview

import tech.mmarca.openvitals.wear.MetricUiState
import tech.mmarca.openvitals.wear.RecordingUiState
import tech.mmarca.openvitals.wear.SettingsUiState
import tech.mmarca.openvitals.wear.WearMetric
import tech.mmarca.openvitals.wear.WearUiState

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
    )

    val recording = RecordingUiState(
        elapsedSeconds = 23 * 60 + 41,
        heartRateBpm = 138,
        distanceMeters = 3_240,
    )

    val settings = SettingsUiState(
        phoneConnected = true,
        sensorAccessGranted = true,
    )

    /** A watch with a barometer and no SpO2 sensor, so one metric is not on offer. */
    val uiState = WearUiState(
        availableMetrics = WearMetric.entries.toSet() - WearMetric.BLOOD_OXYGEN,
        metrics = metrics,
        recording = recording,
        settings = settings,
    )

    private fun doubles(vararg values: Int): List<Double> = values.map(Int::toDouble)
}
