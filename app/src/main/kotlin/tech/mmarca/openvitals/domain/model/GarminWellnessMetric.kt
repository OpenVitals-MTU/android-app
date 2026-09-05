package tech.mmarca.openvitals.domain.model

import java.time.Instant

/**
 * The metrics in `garmin_wellness_samples`: watch-only series Health
 * Connect has no type for. Stored names match the Flutter drift rows.
 */
enum class GarminWellnessMetric(val storageName: String) {
    STRESS("stress"),
    BODY_ENERGY("body_energy"),

    /** Garmin intensity minutes — the running daily totals, in minutes. */
    MODERATE_MINUTES("moderate_minutes"),
    VIGOROUS_MINUTES("vigorous_minutes"),

    /**
     * From the metrics file. VO2 max lives in Health Connect. Recovery time
     * in minutes, readiness 0..100, training loads on Garmin's own scale.
     */
    RECOVERY_TIME("recovery_time"),
    TRAINING_READINESS("training_readiness"),
    TRAINING_LOAD_ACUTE("training_load_acute"),
    TRAINING_LOAD_CHRONIC("training_load_chronic"),

    /** The watch's own sleep score, 0..100, at the session start. */
    SLEEP_SCORE("sleep_score"),
    SLEEP_AWAKENINGS("sleep_awakenings"),

    /** How long the watch counted the sleeper awake, in seconds. */
    SLEEP_AWAKE_SECONDS("sleep_awake_seconds"),

    /** Garmin's undocumented "sleep pressure", stored raw. */
    SLEEP_PRESSURE("sleep_pressure"),

    /** Sleep Coach, in minutes: the usual need, and what the night called for. */
    SLEEP_NEED_NORMAL_MINUTES("sleep_need_normal_minutes"),
    SLEEP_NEED_MINUTES("sleep_need_minutes"),
    ;

    companion object {
        fun fromStorage(value: String): GarminWellnessMetric? =
            entries.firstOrNull { it.storageName == value }
    }
}

/** One watch-only reading, stored raw in [GarminWellnessMetric]'s own units. */
data class GarminWellnessSample(
    val metric: GarminWellnessMetric,
    val time: Instant,
    val value: Long,
)
