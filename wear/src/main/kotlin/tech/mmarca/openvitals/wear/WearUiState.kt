package tech.mmarca.openvitals.wear

import tech.mmarca.openvitals.wear.features.recording.ActivityType

/** Everything the screens render. A null value means "no reading yet". */
data class WearUiState(
    val vitals: VitalsUiState = VitalsUiState(),
    val recording: RecordingUiState = RecordingUiState(),
    val settings: SettingsUiState = SettingsUiState(),
    val preferences: WearPreferences = WearPreferences(),
)

/**
 * Display settings the watch does not own: the phone app is where they are
 * chosen, and a later sync writes them here. Readings stay metric; only
 * their display changes.
 */
data class WearPreferences(
    val unitSystem: UnitSystem = UnitSystem.METRIC,
)

/** Same two values as the phone app's `UnitSystem`. */
enum class UnitSystem {
    METRIC,
    IMPERIAL,
}

data class VitalsUiState(
    val heartRateBpm: Int? = null,
    /** Recent heart-rate readings, oldest first. */
    val heartRateSamples: List<Int> = emptyList(),
    val steps: Int? = null,
    val stepGoal: Int = 10_000,
    val distanceMeters: Int? = null,
    val activeCalories: Int? = null,
)

data class RecordingUiState(
    val activityType: ActivityType = ActivityType.WALK,
    val elapsedSeconds: Long = 0,
    val heartRateBpm: Int? = null,
    val distanceMeters: Int? = null,
    val paused: Boolean = false,
)

data class SettingsUiState(
    val phoneConnected: Boolean = false,
    val sensorAccessGranted: Boolean = false,
)
