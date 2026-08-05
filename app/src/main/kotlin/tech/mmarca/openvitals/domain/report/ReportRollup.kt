package tech.mmarca.openvitals.domain.report

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import tech.mmarca.openvitals.domain.model.ReportDailyValue
import tech.mmarca.openvitals.domain.model.ReportGranularity
import tech.mmarca.openvitals.domain.model.ReportMetricSummary
import tech.mmarca.openvitals.domain.model.ReportPoint
import tech.mmarca.openvitals.domain.model.ReportValueKind
import tech.mmarca.openvitals.domain.preferences.ActivityWeekMode

/**
 * The pure calendar arithmetic between daily reads and report rows: bucket the
 * days, combine per [ReportValueKind], fold the extremes, and never invent a
 * bucket the data doesn't have.
 */
object ReportRollup {

    /**
     * The calendar bucket [date] belongs to. Weekly buckets follow the user's
     * week preference: Monday-aligned calendar weeks, or rolling 7-day blocks
     * anchored at [rangeStart] for LAST_7_DAYS.
     */
    fun bucketStart(
        date: LocalDate,
        granularity: ReportGranularity,
        weekMode: ActivityWeekMode,
        rangeStart: LocalDate,
    ): LocalDate = when (granularity) {
        ReportGranularity.DAILY -> date
        ReportGranularity.WEEKLY -> when (weekMode) {
            ActivityWeekMode.MONDAY_TO_SUNDAY ->
                date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            ActivityWeekMode.LAST_7_DAYS ->
                rangeStart.plusDays(ChronoUnit.DAYS.between(rangeStart, date) / 7 * 7)
        }
        ReportGranularity.MONTHLY -> date.withDayOfMonth(1)
    }

    private fun bucketEnd(
        start: LocalDate,
        granularity: ReportGranularity,
    ): LocalDate = when (granularity) {
        ReportGranularity.DAILY -> start
        ReportGranularity.WEEKLY -> start.plusDays(6)
        ReportGranularity.MONTHLY -> start.with(TemporalAdjusters.lastDayOfMonth())
    }

    /**
     * Daily values → one [ReportPoint] per calendar bucket that has data.
     * SUM metrics add up; AVERAGE metrics mean out. Bucket min/max fold the
     * daily extremes (a day's own min/max where the source has one, its value
     * otherwise). Bucket edges are clamped to the range so edge buckets show
     * the dates they truly cover. Gap buckets are omitted, never zero-filled.
     */
    fun rollup(
        daily: List<ReportDailyValue>,
        valueKind: ReportValueKind,
        granularity: ReportGranularity,
        weekMode: ActivityWeekMode,
        rangeStart: LocalDate,
        rangeEnd: LocalDate,
    ): List<ReportPoint> =
        daily
            .filter { !it.date.isBefore(rangeStart) && !it.date.isAfter(rangeEnd) }
            .groupBy { bucketStart(it.date, granularity, weekMode, rangeStart) }
            .toSortedMap()
            .map { (start, members) ->
                val values = members.map { it.value }
                val secondaries = members.mapNotNull { it.secondaryValue }
                ReportPoint(
                    bucketStart = maxOf(start, rangeStart),
                    bucketEnd = minOf(bucketEnd(start, granularity), rangeEnd),
                    value = combine(values, valueKind),
                    min = members.minOf { it.min ?: it.value },
                    max = members.maxOf { it.max ?: it.value },
                    daysWithData = members.size,
                    secondaryValue = secondaries.takeIf { it.isNotEmpty() }?.let { combine(it, valueKind) },
                    secondaryMin = secondaries.minOrNull(),
                    secondaryMax = secondaries.maxOrNull(),
                )
            }

    /**
     * The stats row, from DAILY values so bucketing never changes what the
     * numbers mean. Null when the range has no data at all.
     */
    fun summarize(
        daily: List<ReportDailyValue>,
        valueKind: ReportValueKind,
    ): ReportMetricSummary? {
        if (daily.isEmpty()) return null
        val sorted = daily.sortedBy { it.date }
        val values = sorted.map { it.value }
        val secondaries = sorted.mapNotNull { it.secondaryValue }
        return ReportMetricSummary(
            average = values.average(),
            min = sorted.minOf { it.min ?: it.value },
            max = sorted.maxOf { it.max ?: it.value },
            total = values.sum().takeIf { valueKind == ReportValueKind.SUM },
            daysWithData = sorted.size,
            secondaryAverage = secondaries.takeIf { it.isNotEmpty() }?.average(),
            secondaryMin = secondaries.minOrNull(),
            secondaryMax = secondaries.maxOrNull(),
            changeOverRange = (values.last() - values.first()).takeIf { sorted.size >= 2 },
        )
    }

    private fun combine(values: List<Double>, valueKind: ReportValueKind): Double =
        when (valueKind) {
            ReportValueKind.SUM -> values.sum()
            ReportValueKind.AVERAGE -> values.average()
        }
}
