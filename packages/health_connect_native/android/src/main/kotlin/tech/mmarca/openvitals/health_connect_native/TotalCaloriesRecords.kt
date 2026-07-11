package tech.mmarca.openvitals.health_connect_native

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Duration
import java.time.Instant

/**
 * Ported from the native OpenVitals app (`healthconnect/TotalCaloriesRecords.kt`).
 * Shared calorie-estimation helpers (used by Nutrition + Activity readers).
 * Uses the Pigeon [CaloriesBurnedSourceMsg] enum as the source tag.
 */
internal data class CaloriesBurned(val kcal: Double, val source: CaloriesBurnedSourceMsg)

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
): CaloriesBurned? =
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
): CaloriesBurned? =
  totalCaloriesRecordedOrEstimated(
    recordedTotalCaloriesKcal = recordedTotalCaloriesKcal,
    estimatedTotalCaloriesKcal = activeCaloriesKcal?.let { active ->
      bmrKcalPerDay?.let { bmr -> active + basalCaloriesForInterval(bmr, start, end) }
    },
  )

private fun totalCaloriesRecordedOrEstimated(
  recordedTotalCaloriesKcal: Double?,
  estimatedTotalCaloriesKcal: Double?,
): CaloriesBurned? =
  when {
    recordedTotalCaloriesKcal != null ->
      CaloriesBurned(recordedTotalCaloriesKcal, CaloriesBurnedSourceMsg.RECORDED_TOTAL)
    estimatedTotalCaloriesKcal != null ->
      CaloriesBurned(estimatedTotalCaloriesKcal, CaloriesBurnedSourceMsg.ESTIMATED_ACTIVE_AND_BMR)
    else -> null
  }

internal fun basalCaloriesForInterval(bmrKcalPerDay: Double, start: Instant, end: Instant): Double {
  if (!end.isAfter(start)) return 0.0
  return bmrKcalPerDay * Duration.between(start, end).toMillis().toDouble() /
    Duration.ofDays(1).toMillis().toDouble()
}
