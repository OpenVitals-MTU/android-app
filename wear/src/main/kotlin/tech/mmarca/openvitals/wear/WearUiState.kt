package tech.mmarca.openvitals.wear

import tech.mmarca.openvitals.wear.features.recording.ActivityType

/** Everything the screens render. A null value means "no reading yet". */
data class WearUiState(
    /**
     * The metrics this watch has a sensor for. Only these are offered as
     * tiles. Every metric until sensor discovery narrows it.
     */
    val availableMetrics: Set<WearMetric> = WearMetric.entries.toSet(),
    /** A metric without an entry renders as "no reading yet". */
    val metrics: Map<WearMetric, MetricUiState> = emptyMap(),
    val recording: RecordingUiState = RecordingUiState(),
    val settings: SettingsUiState = SettingsUiState(),
    val preferences: WearPreferences = WearPreferences(),
)

/** One metric's readings, in the base unit documented on [WearMetric]. */
data class MetricUiState(
    /** Today's total for a cumulative metric, the latest sample otherwise. */
    val current: Double? = null,
    /** A daily goal. Only meaningful for a cumulative metric. */
    val goal: Double? = null,
    /** Today, oldest first: hourly totals for a cumulative metric, samples otherwise. */
    val today: List<Double> = emptyList(),
    /** The last days, oldest first, today last: daily totals or daily averages. */
    val week: List<Double> = emptyList(),
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
