package tech.mmarca.openvitals.wear.ui.preview

import tech.mmarca.openvitals.wear.RecordingUiState
import tech.mmarca.openvitals.wear.SettingsUiState
import tech.mmarca.openvitals.wear.VitalsUiState
import tech.mmarca.openvitals.wear.WearUiState

/**
 * Made-up values for previews and for debug builds, where nothing feeds the
 * screens yet. Never shown in a release build.
 */
object SampleData {
    val vitals = VitalsUiState(
        heartRateBpm = 72,
        heartRateSamples = listOf(64, 66, 63, 70, 78, 91, 104, 98, 86, 79, 74, 71, 69, 72, 75, 72),
        steps = 6_482,
        stepGoal = 10_000,
        distanceMeters = 4_730,
        activeCalories = 312,
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

    val uiState = WearUiState(vitals = vitals, recording = recording, settings = settings)
}
