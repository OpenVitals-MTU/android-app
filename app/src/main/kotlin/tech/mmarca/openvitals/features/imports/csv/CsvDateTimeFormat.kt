package tech.mmarca.openvitals.features.imports.csv

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import java.util.Locale

/**
 * Turning a CSV timestamp cell into an instant plus a wall-clock offset.
 * A CSV usually gives only the wall clock, so the zone comes from
 * [CsvTimeZoneMode]. Text that carries its own offset always wins.
 */

/** A timestamp column's shape. Families, not patterns: one file often mixes several. */
enum class CsvDateTimeFormat {
    /** Try every family over the sample and pick the one that parses most rows. */
    AUTO,

    /** `2026-07-01T08:12:00`, optionally with `Z` or `+05:30`. */
    ISO_8601,

    /** Year first: `2026-07-01 08:12:00`, `2026/07/01`. */
    YEAR_FIRST,

    /** Day first: `01/07/2026 08:12`, `01.07.2026`, `01-07-2026`. */
    DAY_FIRST,

    /** Month first: `07/01/2026 08:12` — the US ordering. */
    MONTH_FIRST,

    /** Whole seconds since the Unix epoch. */
    EPOCH_SECONDS,

    /** Whole milliseconds since the Unix epoch. */
    EPOCH_MILLIS,

    /** A pattern the user typed, in `DateTimeFormatter` syntax. */
    CUSTOM,
}

/** Where the wall-clock offset comes from when the text does not carry one. */
enum class CsvTimeZoneMode {
    /** This phone's zone, resolved at the row's date, so 2019 rows get 2019's offset. */
    DEVICE,

    /** The text is already UTC. */
    UTC,

    /** One fixed offset for the whole file. No DST. */
    FIXED_OFFSET,
}

/** The user's timestamp choices for one import. */
data class CsvDateTimeSettings(
    val format: CsvDateTimeFormat = CsvDateTimeFormat.AUTO,
    /** Only read when [format] is [CsvDateTimeFormat.CUSTOM]. */
    val customPattern: String? = null,
    val zone: CsvTimeZoneMode = CsvTimeZoneMode.DEVICE,
    /** Only read when [zone] is [CsvTimeZoneMode.FIXED_OFFSET]. */
    val fixedOffset: ZoneOffset? = null,
)

/** A resolved timestamp: the instant, and the wall-clock offset to record with it. */
data class CsvInstant(
    val utc: Instant,
    /** What the wall clock was offset by. Stored on the Health Connect record. */
    val offset: ZoneOffset,
)

private val YearFirstPatterns = listOf(
    "yyyy-MM-dd HH:mm:ss",
    "yyyy-MM-dd HH:mm",
    "yyyy-MM-dd",
    "yyyy/MM/dd HH:mm:ss",
    "yyyy/MM/dd HH:mm",
    "yyyy/MM/dd",
)

private val DayFirstPatterns = listOf(
    "dd/MM/yyyy HH:mm:ss",
    "dd/MM/yyyy HH:mm",
    "dd/MM/yyyy",
    "dd.MM.yyyy HH:mm:ss",
    "dd.MM.yyyy HH:mm",
    "dd.MM.yyyy",
    "dd-MM-yyyy HH:mm:ss",
    "dd-MM-yyyy HH:mm",
    "dd-MM-yyyy",
)

private val MonthFirstPatterns = listOf(
    "MM/dd/yyyy HH:mm:ss",
    "MM/dd/yyyy HH:mm",
    "MM/dd/yyyy",
    "MM.dd.yyyy HH:mm:ss",
    "MM.dd.yyyy HH:mm",
    "MM.dd.yyyy",
    "MM-dd-yyyy HH:mm:ss",
    "MM-dd-yyyy HH:mm",
    "MM-dd-yyyy",
)

/**
 * The families AUTO considers, in preference order. YEAR_FIRST precedes
 * ISO_8601 so a plain year-first file is not labelled ISO; ISO still wins
 * on a `T` or an offset. A DAY_FIRST/MONTH_FIRST tie is not broken here.
 */
private val AutoCandidates = listOf(
    CsvDateTimeFormat.YEAR_FIRST,
    CsvDateTimeFormat.ISO_8601,
    CsvDateTimeFormat.DAY_FIRST,
    CsvDateTimeFormat.MONTH_FIRST,
    CsvDateTimeFormat.EPOCH_MILLIS,
    CsvDateTimeFormat.EPOCH_SECONDS,
)

/**
 * 1990-01-01 and 2100-01-01 as epoch millis. An epoch format accepting any
 * integer would swallow a step-count column and date everything to 1970.
 */
private const val MinPlausibleEpochMillis = 631_152_000_000L
private const val MaxPlausibleEpochMillis = 4_102_444_800_000L

private fun isPlausibleEpochMillis(millis: Long): Boolean =
    millis in MinPlausibleEpochMillis..MaxPlausibleEpochMillis

private val ExplicitOffsetRegex = Regex("""(?:Z|[+-]\d{2}:?\d{2})$""")

/** Whether [text] carries its own UTC offset, which beats the selected mode. */
fun csvTimestampHasExplicitOffset(text: String): Boolean {
    val trimmed = text.trim()
    // A bare `2026-07-01` has a month, not an offset.
    if ('T' !in trimmed && ' ' !in trimmed) {
        return trimmed.endsWith("Z")
    }
    return ExplicitOffsetRegex.containsMatchIn(trimmed)
}

/**
 * The wall-clock fields of [text] under [format], no zone applied. Null when
 * it does not parse. Parsing straight to a zoned value would bake in the
 * host's current offset across DST boundaries.
 */
fun parseCsvWallClock(
    text: String,
    format: CsvDateTimeFormat,
    customPattern: String? = null,
): LocalDateTime? {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return null

    return when (format) {
        CsvDateTimeFormat.ISO_8601 -> parseIsoWallClock(trimmed)
        CsvDateTimeFormat.EPOCH_SECONDS -> {
            val seconds = trimmed.toLongOrNull() ?: return null
            val millis = seconds * 1000
            if (!isPlausibleEpochMillis(millis)) return null
            LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneOffset.UTC)
        }
        CsvDateTimeFormat.EPOCH_MILLIS -> {
            val millis = trimmed.toLongOrNull() ?: return null
            // 12+ digits keeps AUTO from reading epoch seconds as 1970.
            if (trimmed.replace("-", "").length < 12) return null
            if (!isPlausibleEpochMillis(millis)) return null
            LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneOffset.UTC)
        }
        CsvDateTimeFormat.CUSTOM -> {
            val pattern = customPattern?.trim()
            if (pattern.isNullOrEmpty()) return null
            tryPatterns(trimmed, listOf(pattern))
        }
        CsvDateTimeFormat.YEAR_FIRST -> tryPatterns(trimmed, YearFirstPatterns)
        CsvDateTimeFormat.DAY_FIRST -> tryPatterns(trimmed, DayFirstPatterns)
        CsvDateTimeFormat.MONTH_FIRST -> tryPatterns(trimmed, MonthFirstPatterns)
        CsvDateTimeFormat.AUTO -> {
            for (candidate in AutoCandidates) {
                parseCsvWallClock(trimmed, candidate)?.let { return it }
            }
            null
        }
    }
}

/** Matches Dart's `DateTime.tryParse`: space or `T`, optional offset, bare date. */
private fun parseIsoWallClock(text: String): LocalDateTime? {
    val normalized = if ('T' !in text && ' ' in text) text.replaceFirst(' ', 'T') else text
    runCatching { return OffsetDateTime.parse(normalized).withOffsetSameLocal(ZoneOffset.UTC).toLocalDateTime() }
    runCatching { return LocalDateTime.parse(normalized) }
    runCatching { return LocalDate.parse(normalized).atStartOfDay() }
    return null
}

private fun tryPatterns(text: String, patterns: List<String>): LocalDateTime? {
    for (pattern in patterns) {
        val formatter = runCatching { formatterFor(pattern) }.getOrNull() ?: continue
        // DateTimeFormatter.parse rejects trailing input, so a date pattern does not drop the time.
        runCatching {
            val parsed = formatter.parse(text)
            val date = LocalDate.from(parsed)
            val hour = if (parsed.isSupported(ChronoField.HOUR_OF_DAY)) parsed.get(ChronoField.HOUR_OF_DAY) else 0
            val minute = if (parsed.isSupported(ChronoField.MINUTE_OF_HOUR)) parsed.get(ChronoField.MINUTE_OF_HOUR) else 0
            val second = if (parsed.isSupported(ChronoField.SECOND_OF_MINUTE)) parsed.get(ChronoField.SECOND_OF_MINUTE) else 0
            return date.atTime(hour, minute, second)
        }
    }
    return null
}

private val FormatterCache = mutableMapOf<String, DateTimeFormatter>()

private fun formatterFor(pattern: String): DateTimeFormatter =
    synchronized(FormatterCache) {
        FormatterCache.getOrPut(pattern) {
            DateTimeFormatterBuilder()
                .appendPattern(pattern)
                .toFormatter(Locale.US)
        }
    }

/** Resolves [text] to an instant plus offset, or null. */
fun resolveCsvInstant(text: String, settings: CsvDateTimeSettings): CsvInstant? {
    val trimmed = text.trim()
    val wall = parseCsvWallClock(trimmed, settings.format, settings.customPattern) ?: return null

    // The file's own offset always wins over the selected mode.
    if (csvTimestampHasExplicitOffset(trimmed)) {
        val normalized = if ('T' !in trimmed && ' ' in trimmed) trimmed.replaceFirst(' ', 'T') else trimmed
        runCatching {
            val parsed = OffsetDateTime.parse(normalized)
            return CsvInstant(parsed.toInstant(), parsed.offset)
        }
        runCatching {
            val parsed = ZonedDateTime.parse(normalized)
            return CsvInstant(parsed.toInstant(), parsed.offset)
        }
    }

    // Epoch formats are instants already.
    val isEpoch = settings.format == CsvDateTimeFormat.EPOCH_SECONDS ||
        settings.format == CsvDateTimeFormat.EPOCH_MILLIS ||
        (settings.format == CsvDateTimeFormat.AUTO && trimmed.toLongOrNull() != null)
    if (isEpoch) {
        val utc = wall.toInstant(ZoneOffset.UTC)
        return CsvInstant(utc, offsetForInstant(utc, settings))
    }

    return when (settings.zone) {
        CsvTimeZoneMode.UTC -> CsvInstant(wall.toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
        CsvTimeZoneMode.FIXED_OFFSET -> {
            val offset = settings.fixedOffset ?: ZoneOffset.UTC
            CsvInstant(wall.toInstant(offset), offset)
        }
        CsvTimeZoneMode.DEVICE -> {
        // Resolving against the zone's rules applies the offset that held on that date.
            val zoned = wall.atZone(ZoneId.systemDefault())
            CsvInstant(zoned.toInstant(), zoned.offset)
        }
    }
}

/** The offset to record for an instant that already knows when it is. */
private fun offsetForInstant(utc: Instant, settings: CsvDateTimeSettings): ZoneOffset =
    when (settings.zone) {
        CsvTimeZoneMode.UTC -> ZoneOffset.UTC
        CsvTimeZoneMode.FIXED_OFFSET -> settings.fixedOffset ?: ZoneOffset.UTC
        CsvTimeZoneMode.DEVICE -> ZoneId.systemDefault().rules.getOffset(utc)
    }

/** What [detectCsvDateTimeFormat] concluded from a sample. */
data class CsvDateTimeDetection(
    /** The best family found, or [CsvDateTimeFormat.AUTO] when nothing parsed. */
    val format: CsvDateTimeFormat,
    val matchedRows: Int,
    val totalRows: Int,
    /** Day-first and month-first both parsed every row, so the ordering cannot be inferred. */
    val ambiguousDayMonth: Boolean,
) {
    val matchedNothing: Boolean get() = matchedRows == 0
}

/**
 * Picks the family that parses the most of [samples]. Refuses to guess
 * between `dd/MM` and `MM/dd`; the UI must make the user choose.
 */
fun detectCsvDateTimeFormat(samples: List<String>): CsvDateTimeDetection {
    val values = samples.map { it.trim() }.filter { it.isNotEmpty() }
    if (values.isEmpty()) {
        return CsvDateTimeDetection(
            format = CsvDateTimeFormat.AUTO,
            matchedRows = 0,
            totalRows = 0,
            ambiguousDayMonth = false,
        )
    }

    var best = CsvDateTimeFormat.AUTO
    var bestCount = 0
    val counts = mutableMapOf<CsvDateTimeFormat, Int>()
    for (candidate in AutoCandidates) {
        val count = values.count { parseCsvWallClock(it, candidate) != null }
        counts[candidate] = count
        if (count > bestCount) {
            best = candidate
            bestCount = count
        }
    }

    val dayFirst = counts[CsvDateTimeFormat.DAY_FIRST] ?: 0
    val monthFirst = counts[CsvDateTimeFormat.MONTH_FIRST] ?: 0
    val ambiguous = dayFirst == values.size &&
        monthFirst == values.size &&
        (counts[CsvDateTimeFormat.ISO_8601] ?: 0) < values.size &&
        (counts[CsvDateTimeFormat.YEAR_FIRST] ?: 0) < values.size

    return CsvDateTimeDetection(
        format = best,
        matchedRows = bestCount,
        totalRows = values.size,
        ambiguousDayMonth = ambiguous,
    )
}
