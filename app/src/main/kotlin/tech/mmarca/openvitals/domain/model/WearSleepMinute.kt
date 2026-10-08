package tech.mmarca.openvitals.domain.model

import java.time.Instant
import java.time.ZoneOffset

/** What the watch could tell about one minute, instantaneous evidence only. */
data class WearMinuteFlags(val bits: Int) {
    val isCharging: Boolean get() = bits and CHARGING != 0
    val isOffBody: Boolean get() = bits and OFF_BODY != 0
    val isScreenOn: Boolean get() = bits and SCREEN_ON != 0
    val heartRateLostContact: Boolean get() = bits and HR_NO_CONTACT != 0
    val heartRateRecording: Boolean get() = bits and HR_RECORDING != 0
    val isSparse: Boolean get() = bits and SPARSE != 0

    companion object {
        const val CHARGING = 1
        const val OFF_BODY = 2
        const val SCREEN_ON = 4
        const val HR_NO_CONTACT = 8
        const val HR_RECORDING = 16
        const val SPARSE = 32
        val NONE = WearMinuteFlags(0)
    }
}

/**
 * One minute the OpenVitals Wear OS app recorded, in physical units: the
 * sleep pipeline's input. `docs/engineering/sleep-minute-features.md`
 * defines every field; the wire form is `WearLinkProtocol.SleepMinute`.
 */
data class WearSleepMinute(
    val time: Instant,
    val zoneOffset: ZoneOffset,
    val kind: SleepMinuteKind,
    val flags: WearMinuteFlags,
    /** Accelerometer samples in the minute. */
    val sampleCount: Int,
    /** The actigraphy count on the estimator's scale; zero when still. */
    val movement: Float,
    /** Mean beats per minute, or null when the minute carried no sample. */
    val heartRate: Float?,
    /** Standard deviation of the minute's heart rate samples, or null. */
    val heartRateSd: Float?,
    val heartRateSamples: Int,
    /** Per-axis mean acceleration x, y, z in g. */
    val meanG: FloatArray,
    /** Per-axis standard deviation of acceleration x, y, z in g. */
    val sdG: FloatArray,
    /** Lowest and highest five-second z-angle in the minute, degrees, or null. */
    val zAngleMin: Float?,
    val zAngleMax: Float?,
    /** Mean absolute change between successive five-second z-angles, degrees, or null. */
    val zAngleDelta: Float?,
) {
    /** The estimator's narrower input. */
    fun toSleepMinute(): SleepMinute = SleepMinute(time, kind, movement, heartRate)

    override fun equals(other: Any?): Boolean =
        other is WearSleepMinute && time == other.time && zoneOffset == other.zoneOffset && kind == other.kind &&
            flags == other.flags && sampleCount == other.sampleCount && movement == other.movement &&
            heartRate == other.heartRate && heartRateSd == other.heartRateSd && heartRateSamples == other.heartRateSamples &&
            meanG.contentEquals(other.meanG) && sdG.contentEquals(other.sdG) &&
            zAngleMin == other.zAngleMin && zAngleMax == other.zAngleMax && zAngleDelta == other.zAngleDelta

    override fun hashCode(): Int = time.hashCode()
}
