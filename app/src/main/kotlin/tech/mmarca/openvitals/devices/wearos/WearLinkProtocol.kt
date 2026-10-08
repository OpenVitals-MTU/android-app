package tech.mmarca.openvitals.devices.wearos

import java.util.Locale
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
 * - `SM_SINCE <epochMillis> <limit>` → up to `limit` `SM` lines, oldest first,
 *   each newer than `epochMillis` (exclusive), then `END <count> <more>`.
 *
 * Reply lines:
 * - `HR <epochMillis> <bpm>` — one heart rate sample.
 * - `SM <epochMillis> <kind> <movement> <bpm> <offsetSeconds>` — one sleep
 *   minute, the estimator's input on the phone: `kind` is `R` (raw: movement
 *   and heart rate, for the phone to classify), `A` (the watch saw the wearer
 *   awake) or `U` (not worn, or no signal); `movement` is the minute's
 *   movement count with one decimal, zero when still; `bpm` is the minute's
 *   mean heart rate or `-`; `offsetSeconds` is the watch's UTC offset, which
 *   decides the night the minute belongs to.
 * - `END <count> <more>` — `count` data lines were sent; `more` is `1` when the
 *   limit cut the reply short and another request from the last line's time
 *   would return more.
 *
 * A line the receiver cannot parse is skipped, never fatal: an older peer may
 * send something newer.
 */
object WearLinkProtocol {

    /** Bumped when a change is not backward compatible. Informational for now. */
    const val VERSION: Int = 2

    /** The RFCOMM service record the watch listens on and the phone connects to. */
    val SERVICE_UUID: UUID = UUID.fromString("4838d728-6e5a-4b95-a29d-a60032338301")

    const val PING: String = "PING"
    const val PONG: String = "PONG"
    const val HEART_RATE_SINCE: String = "HR_SINCE"
    const val HEART_RATE: String = "HR"
    const val SLEEP_MINUTES_SINCE: String = "SM_SINCE"
    const val SLEEP_MINUTE: String = "SM"
    const val END: String = "END"

    /** The most samples one `HR_SINCE` reply carries. The phone pages past it. */
    const val MAX_SAMPLES_PER_REQUEST: Int = 2000

    /** The most minutes one `SM_SINCE` reply carries: a day and a half. The phone pages past it. */
    const val MAX_MINUTES_PER_REQUEST: Int = 2000

    data class HeartRateRequest(val sinceEpochMillis: Long, val limit: Int)

    data class HeartRateSample(val epochMillis: Long, val bpm: Int)

    data class SleepMinutesRequest(val sinceEpochMillis: Long, val limit: Int)

    /** What the watch said about one minute. The code is what goes on the wire. */
    enum class MinuteKind(val code: String) {
        RAW("R"),
        AWAKE("A"),
        UNMEASURABLE("U"),
        ;

        companion object {
            fun fromCode(code: String): MinuteKind? = entries.firstOrNull { it.code == code }
        }
    }

    /** One minute of sleep input. [bpm] is null when the minute carried no heart rate. */
    data class SleepMinute(
        val epochMillis: Long,
        val kind: MinuteKind,
        val movement: Float,
        val bpm: Int?,
        val offsetSeconds: Int,
    )

    data class End(val count: Int, val more: Boolean)

    fun formatHeartRateRequest(sinceEpochMillis: Long, limit: Int): String =
        "$HEART_RATE_SINCE $sinceEpochMillis ${limit.coerceIn(1, MAX_SAMPLES_PER_REQUEST)}"

    /** Null unless [line] is a well-formed `HR_SINCE` request. The limit is clamped. */
    fun parseHeartRateRequest(line: String): HeartRateRequest? =
        parseSinceRequest(line, HEART_RATE_SINCE, MAX_SAMPLES_PER_REQUEST)?.let { (since, limit) ->
            HeartRateRequest(since, limit)
        }

    fun formatSleepMinutesRequest(sinceEpochMillis: Long, limit: Int): String =
        "$SLEEP_MINUTES_SINCE $sinceEpochMillis ${limit.coerceIn(1, MAX_MINUTES_PER_REQUEST)}"

    /** Null unless [line] is a well-formed `SM_SINCE` request. The limit is clamped. */
    fun parseSleepMinutesRequest(line: String): SleepMinutesRequest? =
        parseSinceRequest(line, SLEEP_MINUTES_SINCE, MAX_MINUTES_PER_REQUEST)?.let { (since, limit) ->
            SleepMinutesRequest(since, limit)
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

    fun formatSleepMinute(minute: SleepMinute): String =
        "$SLEEP_MINUTE ${minute.epochMillis} ${minute.kind.code} " +
            "${String.format(Locale.ROOT, "%.1f", minute.movement.coerceAtLeast(0f))} " +
            "${minute.bpm?.toString() ?: NO_VALUE} ${minute.offsetSeconds}"

    /** Null unless [line] is a well-formed `SM` line. A rate outside the plausible range reads as none. */
    fun parseSleepMinute(line: String): SleepMinute? {
        val fields = fields(line)
        if (fields.size != 6 || fields[0] != SLEEP_MINUTE) return null
        val at = fields[1].toLongOrNull() ?: return null
        val kind = MinuteKind.fromCode(fields[2]) ?: return null
        val movement = fields[3].toFloatOrNull() ?: return null
        val bpm = if (fields[4] == NO_VALUE) null else fields[4].toIntOrNull() ?: return null
        val offset = fields[5].toIntOrNull() ?: return null
        if (at <= 0 || !movement.isFinite() || movement < 0f || offset !in -MAX_OFFSET_SECONDS..MAX_OFFSET_SECONDS) return null
        return SleepMinute(at, kind, movement, bpm?.takeIf { it in MIN_BPM..MAX_BPM }, offset)
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

    private fun parseSinceRequest(line: String, word: String, maxLimit: Int): Pair<Long, Int>? {
        val fields = fields(line)
        if (fields.size != 3 || fields[0] != word) return null
        val since = fields[1].toLongOrNull() ?: return null
        val limit = fields[2].toIntOrNull() ?: return null
        if (since < 0 || limit < 1) return null
        return since to limit.coerceAtMost(maxLimit)
    }

    private fun fields(line: String): List<String> = line.trim().split(' ').filter { it.isNotEmpty() }

    private const val NO_VALUE = "-"

    /** Eighteen hours: the widest UTC offset there is. */
    private const val MAX_OFFSET_SECONDS = 18 * 3600

    /** Outside this range the sensor is reporting noise, not a heart. */
    const val MIN_BPM: Int = 25
    const val MAX_BPM: Int = 250
}
