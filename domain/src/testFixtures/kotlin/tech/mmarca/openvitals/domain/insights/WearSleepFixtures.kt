package tech.mmarca.openvitals.domain.insights

import java.time.Instant
import java.time.ZoneOffset
import tech.mmarca.openvitals.domain.model.SleepMinuteKind
import tech.mmarca.openvitals.domain.model.WearMinuteFlags
import tech.mmarca.openvitals.domain.model.WearSleepMinute

/** Synthetic Wear OS rows: a worn wrist, a watch on a table, a charger, and how to string them into a night. */
object WearSleepFixtures {

    val zone: ZoneOffset = ZoneOffset.ofHours(2)

    /** A worn minute with a pulse. */
    fun worn(
        time: Instant,
        movement: Float,
        heartRate: Float,
        flags: Int = WearMinuteFlags.HR_RECORDING,
        kind: SleepMinuteKind = SleepMinuteKind.RAW,
        sdG: Float = 0.02f,
        zAngleDelta: Float = if (movement > 0f) 6f else 0.4f,
    ): WearSleepMinute = row(time, kind, flags, movement, heartRate, sdG, zAngleDelta)

    /** A still watch with no pulse: on a table. [heartRateRecording] false means the grant is missing. */
    fun table(time: Instant, heartRateRecording: Boolean = true, charging: Boolean = false, offBody: Boolean = false): WearSleepMinute {
        var flags = 0
        if (heartRateRecording) flags = flags or WearMinuteFlags.HR_RECORDING
        if (charging) flags = flags or WearMinuteFlags.CHARGING
        if (offBody) flags = flags or WearMinuteFlags.OFF_BODY
        // The watch itself calls a pulseless minute unmeasurable only while it records heart rate.
        val kind = if (heartRateRecording || charging || offBody) SleepMinuteKind.UNMEASURABLE else SleepMinuteKind.RAW
        return row(time, kind, flags, movement = 0f, heartRate = null, sdG = 0.002f, zAngleDelta = 0f)
    }

    private fun row(
        time: Instant,
        kind: SleepMinuteKind,
        flags: Int,
        movement: Float,
        heartRate: Float?,
        sdG: Float,
        zAngleDelta: Float,
    ) = WearSleepMinute(
        time = time,
        zoneOffset = zone,
        kind = kind,
        flags = WearMinuteFlags(flags),
        sampleCount = 300,
        movement = movement,
        heartRate = heartRate,
        heartRateSd = if (heartRate == null) null else 1.5f,
        heartRateSamples = if (heartRate == null) 0 else 6,
        meanG = floatArrayOf(0.05f, -0.02f, 0.99f),
        sdG = floatArrayOf(sdG, sdG, sdG),
        zAngleMin = 85f,
        zAngleMax = 85f + zAngleDelta,
        zAngleDelta = zAngleDelta,
    )

    /** A night's heart rate: settles over the first hour, then dips every 90 minutes. */
    fun nightHeartRate(index: Int): Float {
        val settling = if (index < 60) (60 - index) / 8f else 0f
        val cycle = 4f * kotlin.math.sin(2 * Math.PI * index / 90.0).toFloat()
        val jitter = ((index * 7) % 3 - 1) * 0.5f
        return 52f + settling + cycle + jitter
    }

    /** [count] minutes from [start], one per minute, built by [make] from the index and the time. */
    fun minutes(start: Instant, count: Int, make: (index: Int, time: Instant) -> WearSleepMinute): List<WearSleepMinute> =
        List(count) { index -> make(index, start.plusSeconds(60L * index)) }

    /** Thirty restless minutes, [sleepMinutes] still ones with a night's pulse, then an hour of restlessness. */
    fun wornNight(start: Instant, sleepMinutes: Int = 450): List<WearSleepMinute> =
        minutes(start, 30 + sleepMinutes + 60) { index, time ->
            when {
                index < 30 -> worn(time, movement = 8f + (index * 5) % 8, heartRate = 64f)
                index < 30 + sleepMinutes -> worn(time, movement = 0f, heartRate = nightHeartRate(index - 30))
                else -> worn(time, movement = 8f + (index * 5) % 8, heartRate = 64f)
            }
        }
}
