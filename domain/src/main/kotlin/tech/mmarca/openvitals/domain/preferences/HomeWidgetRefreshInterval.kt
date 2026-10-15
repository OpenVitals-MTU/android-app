package tech.mmarca.openvitals.domain.preferences

/**
 * How often the home widgets re-read Health Connect in the background.
 * Few, coarse steps: every run wakes the process, and WorkManager runs
 * nothing under 15 minutes anyway.
 */
enum class HomeWidgetRefreshInterval(
    /** Minutes between runs. This is what is persisted. */
    val minutes: Int,
) {
    EVERY_15_MINUTES(15),
    EVERY_30_MINUTES(30),
    HOURLY(60),
    EVERY_2_HOURS(120),
    ;

    companion object {
        val DEFAULT: HomeWidgetRefreshInterval = EVERY_30_MINUTES

        /** The interval for [minutes]; [DEFAULT] for null and any value this build no longer offers. */
        fun fromMinutes(minutes: Int?): HomeWidgetRefreshInterval =
            entries.firstOrNull { it.minutes == minutes } ?: DEFAULT
    }
}
