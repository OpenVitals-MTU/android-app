package tech.mmarca.openvitals.devices.wearos

import java.time.Instant
import java.time.ZoneOffset
import tech.mmarca.openvitals.domain.model.SleepMinuteKind
import tech.mmarca.openvitals.domain.model.WearMinuteFlags
import tech.mmarca.openvitals.domain.model.WearSleepMinute

/** The wire row in physical units. Pure. */
object WearOsSleepMinuteMapping {

    fun toDomain(m: WearLinkProtocol.SleepMinute): WearSleepMinute = WearSleepMinute(
        time = Instant.ofEpochMilli(m.epochMillis),
        zoneOffset = ZoneOffset.ofTotalSeconds(m.offsetSeconds),
        kind = when (m.kind) {
            WearLinkProtocol.MinuteKind.RAW -> SleepMinuteKind.RAW
            WearLinkProtocol.MinuteKind.AWAKE -> SleepMinuteKind.AWAKE
            WearLinkProtocol.MinuteKind.UNMEASURABLE -> SleepMinuteKind.UNMEASURABLE
        },
        flags = WearMinuteFlags(m.flags),
        sampleCount = m.sampleCount,
        movement = m.movement,
        heartRate = m.bpm?.toFloat(),
        heartRateSd = m.heartRateSd,
        heartRateSamples = m.heartRateSamples,
        meanG = FloatArray(3) { m.meanMilliG[it] / 1000f },
        sdG = FloatArray(3) { m.sdMilliG[it] / 1000f },
        zAngleMin = m.zAngleMin?.toFloat(),
        zAngleMax = m.zAngleMax?.toFloat(),
        zAngleDelta = m.zAngleDelta,
    )
}
