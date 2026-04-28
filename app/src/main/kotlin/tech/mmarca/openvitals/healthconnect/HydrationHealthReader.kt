package tech.mmarca.openvitals.healthconnect

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import tech.mmarca.openvitals.data.model.DailyHydration
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

internal class HydrationHealthReader(
    private val support: HealthConnectReaderSupport,
) {
    suspend fun readHydrationLiters(date: LocalDate): Double? {
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant()
        return support.withNullableLogging("readHydrationLiters[$date][$start..$end]") {
            val aggregateLiters = support.client().aggregate(
                AggregateRequest(
                    metrics = setOf(HydrationRecord.VOLUME_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                )
            )[HydrationRecord.VOLUME_TOTAL]?.inLiters
            aggregateLiters?.takeIf { it > 0.0 }
                ?: support.client().readHydrationRecordsByDate(start, end, zone).values.sum().takeIf { it > 0.0 }
                ?: aggregateLiters
        }
    }

    suspend fun readTodayHydrationLiters(): Double? = readHydrationLiters(LocalDate.now())

    suspend fun readDailyHydration(startDate: LocalDate, endDate: LocalDate): List<DailyHydration> {
        val zone = ZoneId.systemDefault()
        val start = startDate.atStartOfDay(zone).toInstant()
        val end = endDate.plusDays(1).atStartOfDay(zone).toInstant()
        return support.withLogging("readDailyHydration[$start..$end]", emptyList()) {
            val aggregateBuckets = support.client().aggregateGroupByDuration(
                AggregateGroupByDurationRequest(
                    metrics = setOf(HydrationRecord.VOLUME_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    timeRangeSlicer = Duration.ofDays(1),
                )
            ).map { bucket ->
                DailyHydration(
                    date = bucket.startTime.atZone(zone).toLocalDate(),
                    liters = bucket.result[HydrationRecord.VOLUME_TOTAL]?.inLiters ?: 0.0,
                )
            }
            val hydrationByDate = if (aggregateBuckets.any { it.liters > 0.0 }) {
                aggregateBuckets.associate { it.date to it.liters }
            } else {
                support.client().readHydrationRecordsByDate(start, end, zone)
            }
            dailyHydrationSeries(startDate, endDate, hydrationByDate)
        }
    }
}

internal suspend fun HealthConnectClient.readHydrationRecordsByDate(
    start: Instant,
    end: Instant,
    zone: ZoneId,
): Map<LocalDate, Double> =
    readRecordsPaged(
        recordType = HydrationRecord::class,
        timeRangeFilter = TimeRangeFilter.between(start, end),
        ascendingOrder = true,
    )
        .groupBy { record -> record.startTime.atZone(zone).toLocalDate() }
        .mapValues { (_, records) -> records.sumOf { it.volume.inLiters } }

internal fun dailyHydrationSeries(
    startDate: LocalDate,
    endDate: LocalDate,
    hydrationByDate: Map<LocalDate, Double>,
): List<DailyHydration> =
    generateSequence(startDate) { date ->
        date.plusDays(1).takeUnless { it.isAfter(endDate) }
    }.map { date ->
        DailyHydration(
            date = date,
            liters = hydrationByDate[date] ?: 0.0,
        )
    }.toList()
