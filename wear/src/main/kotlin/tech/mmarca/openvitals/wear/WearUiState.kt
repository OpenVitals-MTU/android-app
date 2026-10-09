package tech.mmarca.openvitals.wear

import tech.mmarca.openvitals.wear.features.quicklog.LoggedEntry
import tech.mmarca.openvitals.wear.features.recording.ActivityType
import tech.mmarca.openvitals.wear.health.WearCapabilities

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
    /** What the watch can measure; gates measurement buttons and workouts. */
    val capabilities: WearCapabilities = WearCapabilities(),
    /** Last night's minutes per stage, as synced from the phone. */
    val sleepStages: Map<SleepStage, Double> = emptyMap(),
    /** Quick logs and spot measurements made on this watch, oldest first. */
    val entries: List<LoggedEntry> = emptyList(),
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
    /** Top of the heart rate zones. The phone will know the user's age; until then a fixed guess. */
    val maxHeartRate: Int = 190,
    /** Daily water goal in millilitres. */
    val waterGoalMl: Int = 2_000,
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
    val caloriesKcal: Int? = null,
    val elevationGainMeters: Int? = null,
    val speedMetersPerSecond: Double? = null,
    val averageHeartRateBpm: Int? = null,
    val paused: Boolean = false,
)

data class SettingsUiState(
    val phoneConnected: Boolean = false,
    val sensorAccessGranted: Boolean = false,
)
