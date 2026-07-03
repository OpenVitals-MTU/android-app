package tech.mmarca.openvitals.domain.insights

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.domain.model.CaffeineDailyStat
import tech.mmarca.openvitals.domain.model.CaffeineDistributionSlice
import tech.mmarca.openvitals.domain.model.CaffeineEntry
import tech.mmarca.openvitals.domain.model.CaffeineEntryInsight
import tech.mmarca.openvitals.domain.model.CaffeineInsights
import tech.mmarca.openvitals.domain.model.CaffeinePoint
import tech.mmarca.openvitals.domain.model.CaffeineSourceCategory
import tech.mmarca.openvitals.domain.model.CaffeineTimeBucket
import tech.mmarca.openvitals.domain.model.CaffeineTimeOfDayBucket
import tech.mmarca.openvitals.domain.model.displayLabel
import tech.mmarca.openvitals.domain.preferences.CaffeinePreferences

object CaffeineInsightCalculator {
    private const val CurvePastHours = 24L
    private const val CurveFutureHours = 18L
    private const val CurveStepMinutes = 30L
    private const val ContributionStepMinutes = 20L
    private const val ForecastLimitHours = 168L
    private val MilligramsEpsilon = 0.01

    fun build(
        entries: List<CaffeineEntry>,
        period: DatePeriod,
        preferences: CaffeinePreferences,
        now: Instant = Instant.now(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): CaffeineInsights {
        val normalizedPreferences = preferences.normalized()
        val periodEntries = entries.filter { entry ->
            val date = entry.startTime.atZone(zone).toLocalDate()
            date in period.start..period.end
        }
        val today = now.atZone(zone).toLocalDate()
        val todayEntries = periodEntries.filter { it.startTime.atZone(zone).toLocalDate() == today }
        val currentMg = activeCaffeineMg(entries, now, normalizedPreferences)
        val bedtimeInstant = bedtimeInstant(today, normalizedPreferences.bedtime, zone)
        val bedtimeMg = activeCaffeineMg(entries, bedtimeInstant, normalizedPreferences)
        val dailyStats = dailyStats(entries, period, normalizedPreferences, zone)
        val periodTotal = periodEntries.sumOf { it.caffeineMg }
        val periodDays = dailyStats.size.coerceAtLeast(1)
        val loggedDays = dailyStats.count { it.totalMg > 0.0 }
        val entryInsights = periodEntries
            .sortedByDescending { it.startTime }
            .map { entry ->
                val peak = peakContribution(entry, normalizedPreferences)
                val catalogMatch = CaffeineHealthDrinkCatalog.match(entry)
                CaffeineEntryInsight(
                    entry = entry,
                    currentContributionMg = contributionMg(entry, now, normalizedPreferences),
                    peakTime = peak.time,
                    peakMg = peak.valueMg,
                    contributionPoints = contributionCurve(entry, normalizedPreferences),
                    inferredCategory = catalogMatch?.item?.category ?: inferCategory(entry.name),
                    catalogMatch = catalogMatch,
                )
            }

        return CaffeineInsights(
            currentMg = currentMg,
            todayTotalMg = todayEntries.sumOf { it.caffeineMg },
            periodTotalMg = periodTotal,
            periodAverageMg = periodTotal / periodDays,
            loggedDays = loggedDays,
            peakDay = dailyStats.maxByOrNull { it.totalMg }?.takeIf { it.totalMg > 0.0 },
            safeNights = dailyStats.count { it.safeForSleep },
            totalNights = dailyStats.size,
            safeSleepStreak = safeSleepStreak(dailyStats, today),
            bedtimeMg = bedtimeMg,
            sleepThresholdMg = normalizedPreferences.sleepThresholdMg,
            bedtime = normalizedPreferences.bedtime,
            timeToThresholdMinutes = timeUntilBelowThreshold(
                entries = entries,
                from = now,
                thresholdMg = normalizedPreferences.sleepThresholdMg.toDouble(),
                preferences = normalizedPreferences,
            ),
            curvePoints = caffeineCurve(entries, now, normalizedPreferences),
            dailyStats = dailyStats,
            entryInsights = entryInsights,
            sourceTotals = distribution(periodEntries) { it.source.ifBlank { "Unknown source" } },
            itemTotals = distribution(periodEntries) { it.name?.takeIf(String::isNotBlank) ?: "Caffeine entry" },
            categoryTotals = distribution(periodEntries) { CaffeineHealthDrinkCatalog.categoryFor(it).displayLabel },
            timeBuckets = timeBuckets(periodEntries, zone),
        )
    }

    fun activeCaffeineMg(
        entries: List<CaffeineEntry>,
        at: Instant,
        preferences: CaffeinePreferences,
    ): Double = entries.sumOf { contributionMg(it, at, preferences) }.zeroFloor()

    fun contributionMg(
        entry: CaffeineEntry,
        at: Instant,
        preferences: CaffeinePreferences,
    ): Double {
        if (entry.caffeineMg <= 0.0 || at.isBefore(entry.startTime)) return 0.0
        val durationMinutes = entry.modelingDurationMinutes()
        val dosePerMinute = entry.caffeineMg / durationMinutes
        var total = 0.0
        for (minute in 0 until durationMinutes) {
            val doseTime = entry.startTime.plus(Duration.ofMinutes(minute.toLong()))
            if (at.isBefore(doseTime)) continue
            val elapsedMinutes = Duration.between(doseTime, at).toMinutes().coerceAtLeast(0)
            total += dosePerMinute * absorbedRemainingFraction(elapsedMinutes.toDouble(), preferences)
        }
        return total.zeroFloor()
    }

    fun peakContribution(
        entry: CaffeineEntry,
        preferences: CaffeinePreferences,
    ): CaffeinePoint {
        var best = CaffeinePoint(entry.startTime, 0.0)
        val scanUntilMinutes = (preferences.effectiveHalfLifeMinutes * 4L)
            .coerceAtLeast(12 * 60L)
            .coerceAtMost(ForecastLimitHours * 60L)
        var minute = 0L
        while (minute <= scanUntilMinutes) {
            val time = entry.startTime.plus(Duration.ofMinutes(minute))
            val value = contributionMg(entry, time, preferences)
            if (value > best.valueMg) best = CaffeinePoint(time, value)
            minute += 5L
        }
        return best
    }

    private fun contributionCurve(
        entry: CaffeineEntry,
        preferences: CaffeinePreferences,
    ): List<CaffeinePoint> {
        val endMinutes = (preferences.effectiveHalfLifeMinutes * 5L)
            .coerceAtLeast(12 * 60L)
            .coerceAtMost(ForecastLimitHours * 60L)
        return (0..endMinutes step ContributionStepMinutes).map { minute ->
            val time = entry.startTime.plus(Duration.ofMinutes(minute))
            CaffeinePoint(time, contributionMg(entry, time, preferences))
        }
    }

    private fun caffeineCurve(
        entries: List<CaffeineEntry>,
        now: Instant,
        preferences: CaffeinePreferences,
    ): List<CaffeinePoint> {
        val start = now.minus(Duration.ofHours(CurvePastHours))
        val end = now.plus(Duration.ofHours(CurveFutureHours))
        return generateSequence(start) { time ->
            time.plus(Duration.ofMinutes(CurveStepMinutes)).takeIf { !it.isAfter(end) }
        }.map { time ->
            CaffeinePoint(time, activeCaffeineMg(entries, time, preferences))
        }.toList()
    }

    private fun dailyStats(
        entries: List<CaffeineEntry>,
        period: DatePeriod,
        preferences: CaffeinePreferences,
        zone: ZoneId,
    ): List<CaffeineDailyStat> =
        generateSequence(period.start) { date ->
            date.plusDays(1).takeIf { !it.isAfter(period.end) }
        }.map { date ->
            val total = entries.sumOf { entry ->
                if (entry.startTime.atZone(zone).toLocalDate() == date) entry.caffeineMg else 0.0
            }
            val bedtime = bedtimeInstant(date, preferences.bedtime, zone)
            val bedtimeMg = activeCaffeineMg(entries, bedtime, preferences)
            CaffeineDailyStat(
                date = date,
                totalMg = total,
                bedtimeMg = bedtimeMg,
                safeForSleep = bedtimeMg <= preferences.sleepThresholdMg,
            )
        }.toList()

    private fun timeUntilBelowThreshold(
        entries: List<CaffeineEntry>,
        from: Instant,
        thresholdMg: Double,
        preferences: CaffeinePreferences,
    ): Long? {
        if (activeCaffeineMg(entries, from, preferences) <= thresholdMg) return 0
        val limit = from.plus(Duration.ofHours(ForecastLimitHours))
        var low = from
        var high = limit
        if (activeCaffeineMg(entries, high, preferences) > thresholdMg) return null
        repeat(32) {
            val mid = low.plusMillis(Duration.between(low, high).toMillis() / 2L)
            if (activeCaffeineMg(entries, mid, preferences) > thresholdMg) {
                low = mid
            } else {
                high = mid
            }
        }
        return Duration.between(from, high).toMinutes().coerceAtLeast(0)
    }

    private fun safeSleepStreak(stats: List<CaffeineDailyStat>, today: LocalDate): Int {
        var streak = 0
        stats
            .filter { !it.date.isAfter(today) }
            .sortedByDescending { it.date }
            .forEach { stat ->
                if (!stat.safeForSleep) return streak
                streak += 1
            }
        return streak
    }

    private fun distribution(
        entries: List<CaffeineEntry>,
        labelFor: (CaffeineEntry) -> String,
    ): List<CaffeineDistributionSlice> =
        entries
            .groupBy(labelFor)
            .map { (label, values) -> CaffeineDistributionSlice(label, values.sumOf { it.caffeineMg }) }
            .filter { it.valueMg > 0.0 }
            .sortedByDescending { it.valueMg }

    private fun timeBuckets(
        entries: List<CaffeineEntry>,
        zone: ZoneId,
    ): List<CaffeineTimeBucket> =
        CaffeineTimeOfDayBucket.entries.map { bucket ->
            CaffeineTimeBucket(
                bucket = bucket,
                valueMg = entries
                    .filter { it.startTime.atZone(zone).toLocalTime().bucket() == bucket }
                    .sumOf { it.caffeineMg },
            )
        }

    private fun absorbedRemainingFraction(
        elapsedMinutes: Double,
        preferences: CaffeinePreferences,
    ): Double {
        if (elapsedMinutes < 0.0) return 0.0
        val ka = ln(10.0) / preferences.absorptionMinutes.coerceAtLeast(1)
        val ke = ln(2.0) / preferences.effectiveHalfLifeMinutes.coerceAtLeast(1)
        val fraction = if (abs(ka - ke) < 0.000001) {
            ka * elapsedMinutes * exp(-ke * elapsedMinutes)
        } else {
            (ka / (ka - ke)) * (exp(-ke * elapsedMinutes) - exp(-ka * elapsedMinutes))
        }
        return fraction.coerceAtLeast(0.0)
    }

    private fun bedtimeInstant(date: LocalDate, bedtime: LocalTime, zone: ZoneId): Instant =
        date.atTime(bedtime).atZone(zone).toInstant()

    private fun CaffeineEntry.modelingDurationMinutes(): Int {
        val minutes = Duration.between(startTime, endTime).toMinutes()
        return if (minutes >= 1L) {
            minutes.coerceAtMost(24 * 60L).toInt()
        } else {
            CaffeinePreferences.DefaultConsumptionDurationMinutes
        }
    }

    fun inferCategory(name: String?): CaffeineSourceCategory =
        CaffeineHealthDrinkCatalog.matchName(name)?.item?.category
            ?: CaffeineHealthDrinkCatalog.inferGenericCategory(name)

    private fun LocalTime.bucket(): CaffeineTimeOfDayBucket = when (hour) {
        in 5..11 -> CaffeineTimeOfDayBucket.MORNING
        in 12..14 -> CaffeineTimeOfDayBucket.AFTERNOON
        in 15..18 -> CaffeineTimeOfDayBucket.EVENING
        else -> CaffeineTimeOfDayBucket.NIGHT
    }

    private fun Double.zeroFloor(): Double =
        if (this < MilligramsEpsilon) 0.0 else this

    private operator fun ClosedRange<LocalDate>.contains(date: LocalDate): Boolean =
        !date.isBefore(start) && !date.isAfter(endInclusive)
}
