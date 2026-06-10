package tech.mmarca.openvitals.healthconnect

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.time.TimeRangeFilter
import tech.mmarca.openvitals.domain.model.CaloriesBurnedSource
import tech.mmarca.openvitals.domain.model.CaloriesBurnedValue
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

internal suspend fun HealthConnectClient.hasTotalCaloriesBurnedRecords(
    start: Instant,
    end: Instant,
): Boolean =
    readRecordsPaged(
        recordType = TotalCaloriesBurnedRecord::class,
        timeRangeFilter = TimeRangeFilter.between(start, end),
        pageSize = 1,
        maxRecords = 1,
    ).isNotEmpty()

internal suspend fun HealthConnectClient.readTotalCaloriesBurnedRecordDates(
    startDate: LocalDate,
    endDate: LocalDate,
    zone: ZoneId,
): Set<LocalDate> {
    if (endDate.isBefore(startDate)) return emptySet()

    val start = startDate.atStartOfDay(zone).toInstant()
    val end = endDate.plusDays(1).atStartOfDay(zone).toInstant()
    return readRecordsPaged(
        recordType = TotalCaloriesBurnedRecord::class,
        timeRangeFilter = TimeRangeFilter.between(start, end),
    ).totalCaloriesBurnedRecordDates(startDate, endDate, zone)
}

internal fun List<TotalCaloriesBurnedRecord>.totalCaloriesBurnedRecordDates(
    startDate: LocalDate,
    endDate: LocalDate,
    zone: ZoneId,
): Set<LocalDate> = buildSet {
    if (endDate.isBefore(startDate)) return@buildSet

    this@totalCaloriesBurnedRecordDates.forEach { record ->
        val firstDate = maxOf(record.startTime.atZone(zone).toLocalDate(), startDate)
        val lastDate = minOf(record.endTime.minusNanos(1).atZone(zone).toLocalDate(), endDate)
        generateSequence(firstDate) { date ->
            date.plusDays(1).takeUnless { it.isAfter(lastDate) }
        }.forEach(::add)
    }
}

internal suspend fun HealthConnectClient.readLatestBmrKcalPerDayBefore(end: Instant): Double? =
    readRecordsPaged(
        recordType = BasalMetabolicRateRecord::class,
        timeRangeFilter = TimeRangeFilter.before(end),
        ascendingOrder = false,
        pageSize = 1,
        maxRecords = 1,
    ).firstOrNull()?.basalMetabolicRate?.inKilocaloriesPerDay

internal fun totalCaloriesRecordedOrDailyEstimated(
    recordedTotalCaloriesKcal: Double?,
    activeCaloriesKcal: Double?,
    bmrKcalPerDay: Double?,
): CaloriesBurnedValue? =
    totalCaloriesRecordedOrEstimated(
        recordedTotalCaloriesKcal = recordedTotalCaloriesKcal,
        estimatedTotalCaloriesKcal = activeCaloriesKcal?.let { active ->
            bmrKcalPerDay?.let { bmr -> active + bmr }
        },
    )

internal fun totalCaloriesRecordedOrIntervalEstimated(
    recordedTotalCaloriesKcal: Double?,
    activeCaloriesKcal: Double?,
    bmrKcalPerDay: Double?,
    start: Instant,
    end: Instant,
): CaloriesBurnedValue? =
    totalCaloriesRecordedOrEstimated(
        recordedTotalCaloriesKcal = recordedTotalCaloriesKcal,
        estimatedTotalCaloriesKcal = activeCaloriesKcal?.let { active ->
            bmrKcalPerDay?.let { bmr -> active + basalCaloriesForInterval(bmr, start, end) }
        },
    )

private fun totalCaloriesRecordedOrEstimated(
    recordedTotalCaloriesKcal: Double?,
    estimatedTotalCaloriesKcal: Double?,
): CaloriesBurnedValue? =
    when {
        recordedTotalCaloriesKcal != null -> CaloriesBurnedValue(
            kcal = recordedTotalCaloriesKcal,
            source = CaloriesBurnedSource.RECORDED_TOTAL,
        )
        estimatedTotalCaloriesKcal != null -> CaloriesBurnedValue(
            kcal = estimatedTotalCaloriesKcal,
            source = CaloriesBurnedSource.ESTIMATED_ACTIVE_AND_BMR,
        )
        else -> null
    }

internal fun basalCaloriesForInterval(
    bmrKcalPerDay: Double,
    start: Instant,
    end: Instant,
): Double {
    if (!end.isAfter(start)) return 0.0
    return bmrKcalPerDay * Duration.between(start, end).toMillis().toDouble() / Duration.ofDays(1).toMillis().toDouble()
}
