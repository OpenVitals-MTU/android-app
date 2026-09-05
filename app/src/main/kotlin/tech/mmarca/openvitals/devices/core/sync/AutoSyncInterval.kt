package tech.mmarca.openvitals.devices.core.sync

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * How often a device is synced unasked, or [OFF]. Few, coarse intervals:
 * each run spends battery at both ends, and nothing below 30 minutes buys
 * more than empty syncs.
 */
enum class AutoSyncInterval(
    /** Minutes between runs; 0 for [OFF]. This is what is persisted. */
    val minutes: Int,
) {
    OFF(0),
    EVERY_30_MINUTES(30),
    HOURLY(60),
    EVERY_2_HOURS(120),
    ;

    val isOn: Boolean get() = this != OFF

    val duration: Duration get() = minutes.minutes

    companion object {

        /** The interval for [minutes]; [OFF] for 0, null, and any value this build no longer offers. */
        fun fromMinutes(minutes: Int?): AutoSyncInterval =
            entries.firstOrNull { it != OFF && it.minutes == minutes } ?: OFF
    }
}
