package tech.mmarca.openvitals.domain.cycle

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** The three overnight signals the cycle screen lines up against the cycle day. */
enum class NightlySignal {
    RESTING_HEART_RATE,
    HRV_RMSSD,
    RESPIRATORY_RATE,
}

/** One reading of one signal. [source] is the app or device that wrote it. */
data class NightSample(val time: Instant, val value: Double, val source: String)

/** A sleep session as the start and end the night is cut by. */
data class NightWindow(val start: Instant, val end: Instant)

/**
 * One night: the cycle day it wakes into, its value, and how many readings the value rests on.
 * [isDailyValue] marks a value taken from the device's once-a-day record on the wake-up day
 * instead of from readings inside the night; it is shown as such, never as a night value.
 */
data class NightlyPoint(
    val cycleDay: Int,
    val date: LocalDate,
    val value: Double,
    val sampleCount: Int,
    val isDailyValue: Boolean = false,
)

/**
 * One signal over the current cycle. [points] is empty when the signal is missing for
 * the cycle. All points come from [source]; [otherSources] names the sources that also
 * wrote this signal in the cycle and were left out, never blended in.
 */
data class NightlySeries(
    val signal: NightlySignal,
    val source: String?,
    val points: List<NightlyPoint>,
    val otherSources: Set<String> = emptySet(),
    /** The read failed for want of a permission: "not allowed", as opposed to "no data". */
    val permissionDenied: Boolean = false,
) {
    val isMissing: Boolean
        get() = points.isEmpty()

    /** Points that are the device's daily value, not a reading from the night. */
    val dailyValueCount: Int
        get() = points.count { it.isDailyValue }
}

/**
 * The nightly signals of the current cycle. [nightCount] is the number of nights with a
 * sleep session: without any, no signal can be placed on a night.
 */
data class NightlyVitals(
    val cycleStart: LocalDate,
    val nightCount: Int,
    val series: List<NightlySeries>,
) {
    val hasAnySignal: Boolean
        get() = series.any { !it.isMissing }

    val missingSignals: List<NightlySignal>
        get() = series.filter { it.isMissing }.map { it.signal }
}

/**
 * Turns raw samples into one value per night and cycle day. Information only: nothing
 * here feeds an estimate, a phase or a fertility statement.
 *
 * A night is a sleep session. Its cycle day is the day the session ends on (the day you
 * wake up), counted from the cycle start as day 1. Health Connect sources often split one
 * night into pieces; pieces less than [MAX_PIECE_GAP_HOURS] hours apart are one night, and of
 * several nights ending on one day the longest is the night, the rest are naps. The night's
 * value is the **median** of the readings between its first start and last end (so a short
 * waking between two pieces is inside it). Studies aggregate differently (Oura ring studies use
 * 5-minute segments overnight, one WHOOP study the last slow-wave sleep cycle, another
 * all non-wake time of the main sleep), so absolute values do not compare across them.
 *
 * Resting pulse is the exception to "inside the night": many devices write one
 * `RestingHeartRateRecord` a day, stamped outside the sleep window. For the signals in
 * [DAILY_FALLBACK] a night with no reading inside it takes that source's readings on the
 * wake-up day instead, flagged [NightlyPoint.isDailyValue] so the card labels it.
 *
 * Sources are never mixed. Devices disagree on the absolute level, so a series takes one
 * source for the whole cycle, the one that covers the most nights (then the most
 * readings, then the name), and drops the nights only another source recorded.
 */
object NightlyVitalsCalculator {

    const val MAX_PIECE_GAP_HOURS = 3L

    /** Signals whose device value is often a once-a-day record outside the sleep window. */
    val DAILY_FALLBACK: Set<NightlySignal> = setOf(NightlySignal.RESTING_HEART_RATE)

    fun build(
        cycleStart: LocalDate,
        today: LocalDate,
        sleep: List<NightWindow>,
        samples: Map<NightlySignal, List<NightSample>>,
        zone: ZoneId = ZoneId.systemDefault(),
        permissionDenied: Set<NightlySignal> = emptySet(),
    ): NightlyVitals {
        val nights = nights(cycleStart, today, sleep, zone)
        return NightlyVitals(
            cycleStart = cycleStart,
            nightCount = nights.size,
            series = NightlySignal.entries.map { signal ->
                series(signal, cycleStart, nights, samples[signal].orEmpty(), zone)
                    .copy(permissionDenied = signal in permissionDenied)
            },
        )
    }

    /** The night of each wake-up date inside the cycle, in date order. */
    internal fun nights(
        cycleStart: LocalDate,
        today: LocalDate,
        sleep: List<NightWindow>,
        zone: ZoneId,
    ): List<Pair<LocalDate, NightWindow>> =
        mergePieces(sleep.filter { it.end > it.start })
            .groupBy { it.end.atZone(zone).toLocalDate() }
            .filterKeys { it >= cycleStart && it <= today }
            .mapValues { (_, windows) -> windows.maxBy { it.lengthMillis() } }
            .toList()
            .sortedBy { it.first }

    /** Sessions closer than [MAX_PIECE_GAP_HOURS] hours are one night split by the source. */
    private fun mergePieces(windows: List<NightWindow>): List<NightWindow> {
        val maxGapMillis = MAX_PIECE_GAP_HOURS * 3_600_000L
        val merged = mutableListOf<NightWindow>()
        for (window in windows.sortedBy { it.start }) {
            val last = merged.lastOrNull()
            if (last != null && window.start.toEpochMilli() - last.end.toEpochMilli() <= maxGapMillis) {
                merged[merged.lastIndex] = NightWindow(last.start, maxOf(last.end, window.end))
            } else {
                merged += window
            }
        }
        return merged
    }

    private fun NightWindow.lengthMillis(): Long = end.toEpochMilli() - start.toEpochMilli()

    /** One night's readings per source: inside the session, and (resting pulse) the wake-up day's own. */
    private class NightReadings(
        val date: LocalDate,
        val inside: Map<String, List<NightSample>>,
        val daily: Map<String, List<NightSample>>,
    ) {
        fun of(source: String): List<NightSample> = inside[source] ?: daily[source].orEmpty()
    }

    private fun series(
        signal: NightlySignal,
        cycleStart: LocalDate,
        nights: List<Pair<LocalDate, NightWindow>>,
        samples: List<NightSample>,
        zone: ZoneId,
    ): NightlySeries {
        val fallback = signal in DAILY_FALLBACK
        val perNight = nights.map { (date, window) ->
            val inside = samples.filter { it.time >= window.start && it.time <= window.end }.groupBy { it.source }
            val daily = if (fallback) {
                samples.filter { it.time.atZone(zone).toLocalDate() == date }
                    .groupBy { it.source }
                    .filterKeys { it !in inside }
            } else {
                emptyMap()
            }
            NightReadings(date, inside, daily)
        }
        val allSources = perNight.flatMap { it.inside.keys + it.daily.keys }.toSet()
        if (allSources.isEmpty()) return NightlySeries(signal, source = null, points = emptyList())
        val chosen = allSources.sortedWith(
            compareByDescending<String> { source -> perNight.count { it.of(source).isNotEmpty() } }
                .thenByDescending { source -> perNight.sumOf { it.of(source).size } }
                .thenBy { it },
        ).first()
        val points = perNight.mapNotNull { night ->
            val readings = night.of(chosen)
            if (readings.isEmpty()) return@mapNotNull null
            NightlyPoint(
                cycleDay = ChronoUnit.DAYS.between(cycleStart, night.date).toInt() + 1,
                date = night.date,
                value = median(readings.map { it.value }),
                sampleCount = readings.size,
                isDailyValue = chosen !in night.inside,
            )
        }
        return NightlySeries(signal, source = chosen, points = points, otherSources = allSources - chosen)
    }

    /** The middle value; the mean of the two middle values for an even count. */
    internal fun median(values: List<Double>): Double {
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2.0
    }
}

/** A sourced statement shown in the nightly vitals' details. Its text is `cycle_nightly_evidence_<id>`. */
data class NightlyEvidence(val id: String, val source: CycleSource)

/**
 * What the details section says and who says it. Every figure in the strings was read
 * from the paper's abstract or full text, not remembered. The gain from adding these
 * signals to temperature is small and the signals are confounded by sleep, alcohol and
 * hormonal contraception; the copy says so.
 */
object NightlyVitalsEvidence {
    val ALL: List<NightlyEvidence> = listOf(
        NightlyEvidence("goodale_pulse", CycleSources.GOODALE_2019),
        NightlyEvidence("jasinski_days", CycleSources.JASINSKI_2024),
        NightlyEvidence("lin_phase", CycleSources.LIN_MCPHASES_2026),
        NightlyEvidence("de_jager_hrv", CycleSources.DE_JAGER_2026),
        NightlyEvidence("gonzalez_sleep", CycleSources.GONZALEZ_2026),
        NightlyEvidence("pietila_alcohol", CycleSources.PIETILA_2018),
        NightlyEvidence("shi_small_gain", CycleSources.SHI_2026),
    )
}
