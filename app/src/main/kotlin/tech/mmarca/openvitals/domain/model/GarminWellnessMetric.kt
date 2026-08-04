package tech.mmarca.openvitals.domain.model

import java.time.Instant

/**
 * The metrics the `garmin_wellness_samples` table can hold — watch-only
 * wellness series that Health Connect has no type for, so that table is their
 * system of record, not a cache.
 *
 * The stored name is explicit so renaming a Kotlin identifier cannot orphan
 * rows. The strings match the Flutter build's drift rows exactly: phase 5's
 * preserved drift file imports 1:1 into the Room table.
 */
enum class GarminWellnessMetric(val storageName: String) {
    STRESS("stress"),
    BODY_ENERGY("body_energy"),

    /** Garmin intensity minutes — the running daily totals, in minutes. */
    MODERATE_MINUTES("moderate_minutes"),
    VIGOROUS_MINUTES("vigorous_minutes"),

    /**
     * From the metrics file. Health Connect holds VO2 max, so it is NOT here;
     * these are the estimates it has no type for.
     *
     * [RECOVERY_TIME] is in minutes, [TRAINING_READINESS] is 0..100, and the
     * two training loads are Garmin's own unitless scale.
     */
    RECOVERY_TIME("recovery_time"),
    TRAINING_READINESS("training_readiness"),
    TRAINING_LOAD_ACUTE("training_load_acute"),
    TRAINING_LOAD_CHRONIC("training_load_chronic"),

    /**
     * The watch's own sleep score for a night, 0..100, timestamped at the
     * session start. Distinct from anything the app derives from stages.
     */
    SLEEP_SCORE("sleep_score"),
    SLEEP_AWAKENINGS("sleep_awakenings"),

    /**
     * How long the watch itself counted the sleeper awake, in SECONDS.
     *
     * The number to compare our stage-derived total against — they have
     * disagreed by nearly an hour on a real night.
     */
    SLEEP_AWAKE_SECONDS("sleep_awake_seconds"),

    /** Garmin's undocumented "sleep pressure", stored raw. */
    SLEEP_PRESSURE("sleep_pressure"),

    /**
     * Sleep Coach, in minutes: the usual nightly need, and what the night's
     * strain actually called for.
     */
    SLEEP_NEED_NORMAL_MINUTES("sleep_need_normal_minutes"),
    SLEEP_NEED_MINUTES("sleep_need_minutes"),
    ;

    companion object {
        fun fromStorage(value: String): GarminWellnessMetric? =
            entries.firstOrNull { it.storageName == value }
    }
}

/**
 * One watch-only wellness reading: stress 0..100, Body Battery 0..100, sleep
 * score 0..100, minutes/seconds counters, … — stored raw, uninterpreted, per
 * [GarminWellnessMetric]'s own units.
 */
data class GarminWellnessSample(
    val metric: GarminWellnessMetric,
    val time: Instant,
    val value: Long,
)
