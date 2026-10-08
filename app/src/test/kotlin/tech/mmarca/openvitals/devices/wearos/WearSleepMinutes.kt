package tech.mmarca.openvitals.devices.wearos

import java.time.Instant
import java.time.ZoneOffset
import tech.mmarca.openvitals.domain.model.SleepMinuteKind
import tech.mmarca.openvitals.domain.model.WearMinuteFlags
import tech.mmarca.openvitals.domain.model.WearSleepMinute

/** Builders for worn, well-recorded minutes, so tests only name what they vary. */
object WearSleepMinutes {

    fun minute(
        time: Instant,
        movement: Float,
        heartRate: Float?,
        zone: ZoneOffset,
        kind: SleepMinuteKind = SleepMinuteKind.RAW,
        flags: Int = WearMinuteFlags.HR_RECORDING,
        sdG: Float = 0.003f,
        zAngleDelta: Float? = 0.3f,
    ): WearSleepMinute = WearSleepMinute(
        time = time,
        zoneOffset = zone,
        kind = kind,
        flags = WearMinuteFlags(flags),
        sampleCount = 300,
        movement = movement,
        heartRate = heartRate,
        heartRateSd = if (heartRate == null) null else 1.5f,
        heartRateSamples = if (heartRate == null) 0 else 6,
        meanG = floatArrayOf(0f, 0f, 1f),
        sdG = floatArrayOf(sdG, sdG, sdG),
        zAngleMin = 88f,
        zAngleMax = 90f,
        zAngleDelta = zAngleDelta,
    )
}
