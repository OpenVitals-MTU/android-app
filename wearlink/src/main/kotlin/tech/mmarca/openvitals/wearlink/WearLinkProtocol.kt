package tech.mmarca.openvitals.wearlink

import java.util.UUID

/**
 * The line protocol between OpenVitals on the phone and the OpenVitals Wear OS
 * app, over one RFCOMM socket per request. Every line is UTF-8, space-separated
 * fields, `\n`-terminated. The phone sends one request line; the watch answers
 * with zero or more data lines and one terminating line, then both sides close.
 *
 * One source for both apps: `:wearlink` is compiled into `:app` and `:wear`.
 *
 * Every connection opens with one hello from the phone and one reply from the
 * watch; requests follow only after `OK`:
 * - `HELLO <version> <caps> <token> <name...>` — the phone's protocol version,
 *   its capabilities (`hr,sm` or `-`), the secret it holds for this watch
 *   (`WearLinkToken`) and its Bluetooth name, the rest of the line.
 * - `OK <version> <caps> <name...>` — the watch knows that token for this phone.
 * - `PENDING` — the watch holds no token for this phone yet and is asking the
 *   wearer; the phone tries again later.
 * - `UNAUTHORIZED <mismatch|blocked>` — the stored token differs (a possible
 *   impersonation, or a reinstalled phone) or the phone is blocked.
 * - `VERSION <min> <max>` — the phone's version is outside what the watch speaks.
 * - `ERROR <bad_hello|bad_request|busy|internal>` — the watch could not serve.
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
 * - `SM <epochMillis> <kind> <offsetSeconds> <flags> <n> <mv10> <bpm|-> <hsd10|->
 *   <hn> <mx> <my> <mz> <sx> <sy> <sz> <zmin|-> <zmax|-> <zd10|->` — one sleep
 *   minute, the sleep pipeline's input on the phone, every field an integer
 *   and `-` meaning none. `docs/engineering/sleep-minute-features.md` defines
 *   each: `kind` is `R` (raw), `A` (awake) or `U` (unmeasurable); `flags` the
 *   bit field below; `n` the accelerometer samples; `mv10` the movement count
 *   times ten; `bpm`, `hsd10` and `hn` the minute's heart rate mean, standard
 *   deviation times ten and sample count; `mx..sz` the per-axis mean and
 *   standard deviation in milli-g; `zmin`, `zmax` the five-second z-angle
 *   extremes in degrees and `zd10` its mean change times ten.
 * - `END <count> <more>` — `count` data lines were sent; `more` is `1` when the
 *   limit cut the reply short and another request from the last line's time
 *   would return more.
 *
 * A line the receiver cannot parse is skipped, never fatal: an older peer may
 * send something newer.
 */
object WearLinkProtocol {

    /** Bumped when a change is not backward compatible; the hello negotiates it. */
    const val VERSION: Int = 4

    /** The oldest version this side still speaks. */
    const val MIN_VERSION: Int = 4

    /** The RFCOMM service record the watch listens on and the phone connects to. */
    val SERVICE_UUID: UUID = UUID.fromString("4838d728-6e5a-4b95-a29d-a60032338301")

    const val HELLO: String = "HELLO"
    const val OK: String = "OK"
    const val PENDING: String = "PENDING"
    const val UNAUTHORIZED: String = "UNAUTHORIZED"
    const val VERSION_WORD: String = "VERSION"
    const val ERROR: String = "ERROR"
    const val PING: String = "PING"
    const val PONG: String = "PONG"
    const val HEART_RATE_SINCE: String = "HR_SINCE"
    const val HEART_RATE: String = "HR"
    const val SLEEP_MINUTES_SINCE: String = "SM_SINCE"
    const val SLEEP_MINUTE: String = "SM"
    const val END: String = "END"

    /** Longer than this a line is a protocol violation; the reader closes rather than buffers. */
    const val MAX_LINE_LENGTH: Int = 512

    /** A device name longer than this is clipped on the wire. */
    const val MAX_NAME_LENGTH: Int = 48

    const val CAP_HEART_RATE: String = "hr"
    const val CAP_SLEEP_MINUTES: String = "sm"
    const val NO_CAPABILITIES: String = "-"

    /** The phone's opening line. */
    data class Hello(val version: Int, val capabilities: Set<String>, val token: String, val name: String)

    enum class UnauthorizedReason(val word: String) {
        MISMATCH("mismatch"),
        BLOCKED("blocked"),
        ;

        companion object {
            fun fromWord(word: String): UnauthorizedReason? = entries.firstOrNull { it.word == word }
        }
    }

    enum class ErrorCode(val word: String) {
        BAD_HELLO("bad_hello"),
        BAD_REQUEST("bad_request"),
        BUSY("busy"),
        INTERNAL("internal"),
        ;

        companion object {
            fun fromWord(word: String): ErrorCode? = entries.firstOrNull { it.word == word }
        }
    }

    /** The watch's answer to a hello. */
    sealed class HelloReply {
        data class Ok(val version: Int, val capabilities: Set<String>, val name: String) : HelloReply()
        data object Pending : HelloReply()
        data class Unauthorized(val reason: UnauthorizedReason) : HelloReply()
        data class VersionMismatch(val min: Int, val max: Int) : HelloReply()
        data class Error(val code: ErrorCode) : HelloReply()
    }

    /** A request after the hello. */
    sealed class Request {
        data object Ping : Request()
        data class HeartRateSince(val sinceEpochMillis: Long, val limit: Int) : Request()
        data class SleepMinutesSince(val sinceEpochMillis: Long, val limit: Int) : Request()
    }

    fun formatHello(hello: Hello): String =
        "$HELLO ${hello.version} ${formatCapabilities(hello.capabilities)} ${hello.token} ${cleanName(hello.name)}"

    /** Null unless [line] is a well-formed hello with a well-formed token. */
    fun parseHello(line: String): Hello? {
        val fields = fields(line)
        if (fields.size < 4 || fields[0] != HELLO) return null
        val version = fields[1].toIntOrNull() ?: return null
        val capabilities = parseCapabilities(fields[2]) ?: return null
        val token = fields[3]
        if (version < 1 || !WearLinkToken.isWellFormed(token)) return null
        return Hello(version, capabilities, token, cleanName(fields.drop(4).joinToString(" ")))
    }

    fun formatHelloReply(reply: HelloReply): String = when (reply) {
        is HelloReply.Ok -> "$OK ${reply.version} ${formatCapabilities(reply.capabilities)} ${cleanName(reply.name)}"
        HelloReply.Pending -> PENDING
        is HelloReply.Unauthorized -> "$UNAUTHORIZED ${reply.reason.word}"
        is HelloReply.VersionMismatch -> "$VERSION_WORD ${reply.min} ${reply.max}"
        is HelloReply.Error -> "$ERROR ${reply.code.word}"
    }

    /** Null unless [line] is a well-formed hello reply. */
    fun parseHelloReply(line: String): HelloReply? {
        val fields = fields(line)
        if (fields.isEmpty()) return null
        return when (fields[0]) {
            OK -> {
                if (fields.size < 3) return null
                val version = fields[1].toIntOrNull() ?: return null
                val capabilities = parseCapabilities(fields[2]) ?: return null
                HelloReply.Ok(version, capabilities, cleanName(fields.drop(3).joinToString(" ")))
            }
            PENDING -> if (fields.size == 1) HelloReply.Pending else null
            UNAUTHORIZED -> if (fields.size == 2) UnauthorizedReason.fromWord(fields[1])?.let(HelloReply::Unauthorized) else null
            VERSION_WORD -> {
                if (fields.size != 3) return null
                val min = fields[1].toIntOrNull() ?: return null
                val max = fields[2].toIntOrNull() ?: return null
                if (min < 1 || max < min) null else HelloReply.VersionMismatch(min, max)
            }
            ERROR -> if (fields.size == 2) ErrorCode.fromWord(fields[1])?.let(HelloReply::Error) else null
            else -> null
        }
    }

    fun formatError(code: ErrorCode): String = "$ERROR ${code.word}"

    fun formatRequest(request: Request): String = when (request) {
        Request.Ping -> PING
        is Request.HeartRateSince -> formatHeartRateRequest(request.sinceEpochMillis, request.limit)
        is Request.SleepMinutesSince -> formatSleepMinutesRequest(request.sinceEpochMillis, request.limit)
    }

    /** Null unless [line] is a well-formed request. */
    fun parseRequest(line: String): Request? {
        if (isPing(line)) return Request.Ping
        parseHeartRateRequest(line)?.let { return Request.HeartRateSince(it.sinceEpochMillis, it.limit) }
        parseSleepMinutesRequest(line)?.let { return Request.SleepMinutesSince(it.sinceEpochMillis, it.limit) }
        return null
    }

    private fun formatCapabilities(capabilities: Set<String>): String =
        if (capabilities.isEmpty()) NO_CAPABILITIES else capabilities.sorted().joinToString(",")

    private fun parseCapabilities(field: String): Set<String>? {
        if (field == NO_CAPABILITIES) return emptySet()
        val words = field.split(',')
        if (words.any { word -> word.isEmpty() || word.any { !it.isLetterOrDigit() && it != '_' } }) return null
        return words.toSet()
    }

    /** Printable characters only, every run of whitespace or control characters one space, at most [MAX_NAME_LENGTH]. */
    fun cleanName(name: String): String =
        name.map { if (it.isWhitespace() || it.code < 0x20 || it.code == 0x7f) ' ' else it }
            .joinToString("")
            .split(' ')
            .filter { it.isNotEmpty() }
            .joinToString(" ")
            .take(MAX_NAME_LENGTH)

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

    /** Bits of the `flags` field. */
    const val FLAG_CHARGING: Int = 1
    const val FLAG_OFF_BODY: Int = 2
    const val FLAG_SCREEN_ON: Int = 4
    const val FLAG_HR_NO_CONTACT: Int = 8
    const val FLAG_HR_RECORDING: Int = 16
    const val FLAG_SPARSE: Int = 32

    /**
     * One minute of sleep input, as the wire carries it: integers, with the
     * tenths kept in the name. Null is `-`. [meanMilliG] and [sdMilliG] hold
     * x, y, z.
     */
    data class SleepMinute(
        val epochMillis: Long,
        val kind: MinuteKind,
        val offsetSeconds: Int,
        val flags: Int,
        val sampleCount: Int,
        val movement10: Int,
        val bpm: Int?,
        val heartRateSd10: Int?,
        val heartRateSamples: Int,
        val meanMilliG: IntArray,
        val sdMilliG: IntArray,
        val zAngleMin: Int?,
        val zAngleMax: Int?,
        val zAngleDelta10: Int?,
    ) {
        val movement: Float get() = movement10 / 10f
        val heartRateSd: Float? get() = heartRateSd10?.let { it / 10f }
        val zAngleDelta: Float? get() = zAngleDelta10?.let { it / 10f }

        fun hasFlag(flag: Int): Boolean = flags and flag != 0

        override fun equals(other: Any?): Boolean = other is SleepMinute && formatSleepMinute(this) == formatSleepMinute(other)

        override fun hashCode(): Int = formatSleepMinute(this).hashCode()
    }

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

    fun formatSleepMinute(m: SleepMinute): String = buildString(96) {
        append(SLEEP_MINUTE).append(' ').append(m.epochMillis).append(' ').append(m.kind.code).append(' ')
        append(m.offsetSeconds).append(' ').append(m.flags).append(' ').append(m.sampleCount).append(' ')
        append(m.movement10).append(' ').append(m.bpm ?: NO_VALUE).append(' ').append(m.heartRateSd10 ?: NO_VALUE).append(' ')
        append(m.heartRateSamples)
        for (axis in 0 until 3) append(' ').append(m.meanMilliG[axis])
        for (axis in 0 until 3) append(' ').append(m.sdMilliG[axis])
        append(' ').append(m.zAngleMin ?: NO_VALUE).append(' ').append(m.zAngleMax ?: NO_VALUE)
        append(' ').append(m.zAngleDelta10 ?: NO_VALUE)
    }

    /** Null unless [line] is a well-formed `SM` line. A rate outside the plausible range reads as none. */
    fun parseSleepMinute(line: String): SleepMinute? {
        val f = fields(line)
        if (f.size != SLEEP_MINUTE_FIELDS || f[0] != SLEEP_MINUTE) return null
        val at = f[1].toLongOrNull() ?: return null
        val kind = MinuteKind.fromCode(f[2]) ?: return null
        val offset = f[3].toIntOrNull() ?: return null
        val flags = f[4].toIntOrNull() ?: return null
        val n = f[5].toIntOrNull() ?: return null
        val movement10 = f[6].toIntOrNull() ?: return null
        val bpm = optional(f[7]) ?: return null
        val heartRateSd10 = optional(f[8]) ?: return null
        val hn = f[9].toIntOrNull() ?: return null
        val mean = IntArray(3)
        val sd = IntArray(3)
        for (axis in 0 until 3) {
            mean[axis] = f[10 + axis].toIntOrNull() ?: return null
            sd[axis] = f[13 + axis].toIntOrNull() ?: return null
        }
        val zMin = optional(f[16]) ?: return null
        val zMax = optional(f[17]) ?: return null
        val zDelta10 = optional(f[18]) ?: return null
        if (at <= 0 || offset !in -MAX_OFFSET_SECONDS..MAX_OFFSET_SECONDS || flags < 0 || n < 0 || movement10 < 0 || hn < 0) return null
        return SleepMinute(
            epochMillis = at,
            kind = kind,
            offsetSeconds = offset,
            flags = flags,
            sampleCount = n,
            movement10 = movement10,
            bpm = bpm.value?.takeIf { it in MIN_BPM..MAX_BPM },
            heartRateSd10 = heartRateSd10.value,
            heartRateSamples = hn,
            meanMilliG = mean,
            sdMilliG = sd,
            zAngleMin = zMin.value,
            zAngleMax = zMax.value,
            zAngleDelta10 = zDelta10.value,
        )
    }

    /** An integer or `-`. Null for anything else, so the caller can reject the line. */
    private class Optional(val value: Int?)

    private fun optional(field: String): Optional? =
        if (field == NO_VALUE) Optional(null) else field.toIntOrNull()?.let { Optional(it) }

    private const val SLEEP_MINUTE_FIELDS = 19

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
