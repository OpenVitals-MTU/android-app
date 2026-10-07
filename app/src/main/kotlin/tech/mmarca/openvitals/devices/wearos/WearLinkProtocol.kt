package tech.mmarca.openvitals.devices.wearos

import java.util.UUID

/**
 * The line protocol between OpenVitals on the phone and the OpenVitals Wear OS
 * app, over one RFCOMM socket per request. Every line is UTF-8, space-separated
 * fields, `\n`-terminated. The phone sends one request line; the watch answers
 * with zero or more data lines and one terminating line, then both sides close.
 *
 * This file is identical in `:app` (`devices/wearos`) and `:wear`, except for
 * the package line; `WearOsLinkParityTest` on the phone side fails when the two
 * copies differ. Change both or neither.
 *
 * Requests:
 * - `PING` → `PONG`. Liveness only.
 * - `HR_SINCE <epochMillis> <limit>` → up to `limit` `HR` lines, oldest first,
 *   each newer than `epochMillis` (exclusive), then `END <count> <more>`.
 *
 * Reply lines:
 * - `HR <epochMillis> <bpm>` — one heart rate sample.
 * - `END <count> <more>` — `count` data lines were sent; `more` is `1` when the
 *   limit cut the reply short and another request from the last sample's time
 *   would return more.
 *
 * A line the receiver cannot parse is skipped, never fatal: an older peer may
 * send something newer.
 */
object WearLinkProtocol {

    /** Bumped when a change is not backward compatible. Informational for now. */
    const val VERSION: Int = 1

    /** The RFCOMM service record the watch listens on and the phone connects to. */
    val SERVICE_UUID: UUID = UUID.fromString("4838d728-6e5a-4b95-a29d-a60032338301")

    const val PING: String = "PING"
    const val PONG: String = "PONG"
    const val HEART_RATE_SINCE: String = "HR_SINCE"
    const val HEART_RATE: String = "HR"
    const val END: String = "END"

    /** The most samples one `HR_SINCE` reply carries. The phone pages past it. */
    const val MAX_SAMPLES_PER_REQUEST: Int = 2000

    data class HeartRateRequest(val sinceEpochMillis: Long, val limit: Int)

    data class HeartRateSample(val epochMillis: Long, val bpm: Int)

    data class End(val count: Int, val more: Boolean)

    fun formatHeartRateRequest(sinceEpochMillis: Long, limit: Int): String =
        "$HEART_RATE_SINCE $sinceEpochMillis ${limit.coerceIn(1, MAX_SAMPLES_PER_REQUEST)}"

    /** Null unless [line] is a well-formed `HR_SINCE` request. The limit is clamped. */
    fun parseHeartRateRequest(line: String): HeartRateRequest? {
        val fields = fields(line)
        if (fields.size != 3 || fields[0] != HEART_RATE_SINCE) return null
        val since = fields[1].toLongOrNull() ?: return null
        val limit = fields[2].toIntOrNull() ?: return null
        if (since < 0 || limit < 1) return null
        return HeartRateRequest(since, limit.coerceAtMost(MAX_SAMPLES_PER_REQUEST))
    }

    fun formatSample(epochMillis: Long, bpm: Int): String = "$HEART_RATE $epochMillis $bpm"

    /** Null unless [line] is a well-formed `HR` line with a plausible rate. */
    fun parseSample(line: String): HeartRateSample? {
        val fields = fields(line)
        if (fields.size != 3 || fields[0] != HEART_RATE) return null
        val at = fields[1].toLongOrNull() ?: return null
        val bpm = fields[2].toIntOrNull() ?: return null
        if (at <= 0 || bpm !in MIN_BPM..MAX_BPM) return null
        return HeartRateSample(at, bpm)
    }

    fun formatEnd(count: Int, more: Boolean): String = "$END $count ${if (more) 1 else 0}"

    /** Null unless [line] is a well-formed `END` line. */
    fun parseEnd(line: String): End? {
        val fields = fields(line)
        if (fields.size != 3 || fields[0] != END) return null
        val count = fields[1].toIntOrNull() ?: return null
        val more = when (fields[2]) {
            "0" -> false
            "1" -> true
            else -> return null
        }
        if (count < 0) return null
        return End(count, more)
    }

    /** True when [line] is the ping request. */
    fun isPing(line: String): Boolean = line.trim() == PING

    private fun fields(line: String): List<String> = line.trim().split(' ').filter { it.isNotEmpty() }

    /** Outside this range the sensor is reporting noise, not a heart. */
    const val MIN_BPM: Int = 25
    const val MAX_BPM: Int = 250
}
