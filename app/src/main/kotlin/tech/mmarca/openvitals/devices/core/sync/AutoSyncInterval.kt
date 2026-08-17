package tech.mmarca.openvitals.devices.core.sync

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * How often a device is synced without being asked, or [OFF] when it is only
 * ever synced by hand.
 *
 * Device-agnostic like [DeviceSyncPhase]: the choice is about how often the
 * app reaches for a device, not about any one protocol.
 *
 * The offered intervals are deliberately few and coarse. Each run wakes the
 * radio at both ends and spends watch battery, so the useful question is
 * "roughly how fresh do you want the data", not "how many minutes". Nothing
 * below 30 minutes is offered: the watch closes a monitoring file every 15
 * minutes at best, so a faster schedule mostly buys empty syncs, and
 * Android's own floor for periodic work is 15 minutes anyway.
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

        /**
         * The interval stored as [minutes], or [OFF] for 0, null, and any
         * value this build no longer offers — a schedule the app cannot honour
         * must read as off rather than as a silently different one.
         */
        fun fromMinutes(minutes: Int?): AutoSyncInterval =
            entries.firstOrNull { it != OFF && it.minutes == minutes } ?: OFF
    }
}
