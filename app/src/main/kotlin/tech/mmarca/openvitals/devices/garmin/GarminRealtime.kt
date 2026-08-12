package tech.mmarca.openvitals.devices.garmin

import java.time.Instant

/**
 * Garmin's live-streaming services, by their ML service code.
 *
 * Each is a channel of its own on the multi-link transport: open the service
 * and the watch pushes small fixed payloads as the values change, until it is
 * closed again. This is NOT the FIT stream (5011/5012) — that carries the
 * watch's capabilities and file-shaped records; live readings ride here.
 *
 * Codes from Gadgetbridge's `CommunicatorV2.Service` (AGPLv3). Only the ones
 * this app parses are listed; the watch has more (accelerometer, calories,
 * intensity, stress, Body Battery), each cheap to add once something wants
 * them.
 */
enum class GarminRealtimeService(val code: Int) {
    HEART_RATE(6),
    STEPS(7),
    HRV(12),
    SPO2(19),
    RESPIRATION(21),
}

/** One live reading pushed by the watch. */
sealed class GarminRealtimeReading {
    /** Beats per minute, plus the watch's own resting figure when it sends one. */
    data class HeartRate(val bpm: Int, val restingBpm: Int?) : GarminRealtimeReading()

    /** Today's cumulative step count and the watch's goal. */
    data class Steps(val steps: Int, val goal: Int) : GarminRealtimeReading()

    /** One beat-to-beat interval, in milliseconds. */
    data class Hrv(val rrIntervalMillis: Int) : GarminRealtimeReading()

    data class SpO2(val percent: Int, val measuredAt: Instant?) : GarminRealtimeReading()

    data class Respiration(val breathsPerMinute: Int) : GarminRealtimeReading()
}

/**
 * Parses the payloads of [GarminRealtimeService] channels.
 *
 * Every parser returns null rather than throwing on a short or
 * sentinel-valued packet: these arrive unsolicited, on a link held for hours,
 * and one odd reading must never take the link down. The watch signals "I do
 * not know" with out-of-range values rather than by staying quiet — a
 * respiration of -2, an SpO2 of -1 — so those are dropped here instead of
 * being shown as real measurements.
 */
object GarminRealtimeParser {

    fun parse(service: GarminRealtimeService, payload: ByteArray): GarminRealtimeReading? =
        when (service) {
            GarminRealtimeService.HEART_RATE -> parseHeartRate(payload)
            GarminRealtimeService.STEPS -> parseSteps(payload)
            GarminRealtimeService.HRV -> parseHrv(payload)
            GarminRealtimeService.SPO2 -> parseSpO2(payload)
            GarminRealtimeService.RESPIRATION -> parseRespiration(payload)
        }

    fun serviceFor(code: Int): GarminRealtimeService? =
        GarminRealtimeService.entries.firstOrNull { it.code == code }

    /** `[type][bpm][resting][ff ff]` — type appears to be 3 for a live sample. */
    private fun parseHeartRate(payload: ByteArray): GarminRealtimeReading? {
        if (payload.size < 2) return null
        val bpm = payload[1].toInt() and 0xFF
        // Zero is the watch saying it has no reading (off the wrist, or still
        // settling), not a heart that stopped.
        if (bpm <= 0) return null
        val resting = if (payload.size >= 3) (payload[2].toInt() and 0xFF) else 0
        return GarminRealtimeReading.HeartRate(
            bpm = bpm,
            restingBpm = resting.takeIf { it > 0 },
        )
    }

    /** `[u32 steps][u32 goal]` — the day's running total, not a delta. */
    private fun parseSteps(payload: ByteArray): GarminRealtimeReading? {
        if (payload.size < 8) return null
        val reader = GarminByteReader(payload)
        val steps = reader.readInt()
        val goal = reader.readInt()
        if (steps < 0) return null
        return GarminRealtimeReading.Steps(steps = steps.toInt(), goal = goal.toInt())
    }

    /** `[u16 rr]` in milliseconds. */
    private fun parseHrv(payload: ByteArray): GarminRealtimeReading? {
        if (payload.size < 2) return null
        val rr = GarminByteReader(payload).readShort()
        if (rr <= 0) return null
        return GarminRealtimeReading.Hrv(rrIntervalMillis = rr)
    }

    /** `[spo2][u32 garminTimestamp]`; -1 marks "unknown", and then the stamp is junk. */
    private fun parseSpO2(payload: ByteArray): GarminRealtimeReading? {
        if (payload.isEmpty()) return null
        val percent = payload[0].toInt() // signed on purpose: -1 is the sentinel
        if (percent !in 1..100) return null
        val measuredAt = if (payload.size >= 5) {
            val stamp = GarminByteReader(payload.copyOfRange(1, payload.size)).readInt()
            if (stamp > 0) GarminTime.toInstant(stamp) else null
        } else {
            null
        }
        return GarminRealtimeReading.SpO2(percent = percent, measuredAt = measuredAt)
    }

    /** `[breathsPerMinute]`, signed — negative (usually -2) means unknown. */
    private fun parseRespiration(payload: ByteArray): GarminRealtimeReading? {
        if (payload.isEmpty()) return null
        val breaths = payload[0].toInt()
        if (breaths <= 0) return null
        return GarminRealtimeReading.Respiration(breathsPerMinute = breaths)
    }
}
