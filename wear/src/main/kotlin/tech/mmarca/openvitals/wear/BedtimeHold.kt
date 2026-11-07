package tech.mmarca.openvitals.wear

/**
 * Whether the sleep recorder keeps the processor awake, and why not when it
 * does not.
 *
 * The Watch8's accelerometer cannot wake the processor and buffers 48
 * seconds of readings; the sensor hub ignores the heart rate's report
 * latency while the arm is still, so a sleeping watch wakes every few
 * minutes and loses most of each minute's movement. Holding the processor
 * awake is the only setup that recorded whole minutes on that watch. It
 * costs battery, so it runs only while the wearer has bedtime mode on, the
 * watch is on the wrist, and it is off the charger (which keeps the
 * processor awake by itself).
 */
enum class BedtimeHold {
    /** Bedtime mode is off: the recorder takes what the watch's own wake-ups bring. */
    OFF,

    /** Bedtime mode is on and the watch is worn: every minute is recorded whole. */
    HOLDING,

    /** Bedtime mode is on but the watch is off the wrist. */
    PAUSED_OFF_WRIST,

    /** Bedtime mode is on but the watch is charging. */
    PAUSED_CHARGING,
    ;

    companion object {
        fun decide(bedtime: Boolean, worn: Boolean, charging: Boolean): BedtimeHold = when {
            !bedtime -> OFF
            charging -> PAUSED_CHARGING
            !worn -> PAUSED_OFF_WRIST
            else -> HOLDING
        }
    }
}
