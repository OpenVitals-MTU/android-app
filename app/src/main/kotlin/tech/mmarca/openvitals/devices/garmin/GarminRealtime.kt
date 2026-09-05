package tech.mmarca.openvitals.devices.garmin

import java.time.Instant

/**
 * Garmin's live-streaming services by ML service code. Each is its own
 * channel: open it and the watch pushes small payloads. Not the FIT
 * stream. Codes from Gadgetbridge (AGPLv3); only the parsed ones are listed.
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
 * Parses the live channels. Every parser returns null on a short or
 * sentinel packet: one odd reading must never take the link down.
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
        // Zero means no reading, not a stopped heart.
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
