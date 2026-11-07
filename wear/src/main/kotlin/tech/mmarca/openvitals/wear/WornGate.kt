package tech.mmarca.openvitals.wear

/**
 * Whether the heart rate sensor runs, from the off-body sensor. Its LEDs
 * cost the most power of all the recorders, and off the wrist they measure
 * nothing.
 *
 * Off the wrist for [OFF_DELAY_MILLIS], and the sensor stops: the off-body
 * sensor reports a short "off" now and then while the watch is worn. Back
 * on the wrist, and it starts at once.
 *
 * On some watches the off-body sensor may itself depend on the heart rate
 * sensor, so with it stopped no "on" would ever come. A wake-up (the screen
 * turns on, the charger is pulled) therefore runs the sensor again for
 * [OFF_DELAY_MILLIS]; if no "on" comes in that time, it stops again.
 *
 * A watch without an off-body sensor never reports "off", so its heart
 * rate always runs. Pure, so it is testable without a device.
 */
class WornGate {

    /** When the watch was last reported off the wrist, or the last wake-up since; null while worn. */
    private var offSince: Long? = null

    fun onWorn(now: Long, worn: Boolean) {
        offSince = if (worn) null else offSince ?: now
    }

    /** The screen turned on or the charger was pulled: the wearer may have put the watch on. */
    fun onWakeUp(now: Long) {
        if (offSince != null) offSince = now
    }

    fun heartRateWanted(now: Long): Boolean {
        val since = offSince ?: return true
        return now - since < OFF_DELAY_MILLIS
    }

    /** When [heartRateWanted] turns false unless something is reported first; null when it will not. */
    fun nextChangeAt(): Long? = offSince?.plus(OFF_DELAY_MILLIS)

    companion object {
        /** Longer than the off-body sensor's short false "off", short enough to save the LEDs. */
        const val OFF_DELAY_MILLIS = 60_000L
    }
}
