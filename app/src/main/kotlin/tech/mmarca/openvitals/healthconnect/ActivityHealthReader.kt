package tech.mmarca.openvitals.healthconnect

import android.util.Log
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.CyclingPedalingCadenceRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ElevationGainedRecord
import androidx.health.connect.client.records.ExerciseCompletionGoal
import androidx.health.connect.client.records.ExerciseLap
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.PlannedExerciseBlock
import androidx.health.connect.client.records.ExerciseRoute
import androidx.health.connect.client.records.ExerciseSegment
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.PlannedExerciseSessionRecord
import androidx.health.connect.client.records.PlannedExerciseStep
import androidx.health.connect.client.records.PowerRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.StepsCadenceRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WheelchairPushesRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Power
import androidx.health.connect.client.units.Velocity
import androidx.health.connect.client.units.kilocalories
import androidx.health.connect.client.units.meters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tech.mmarca.openvitals.domain.model.ActivityExerciseSegmentWrite
import tech.mmarca.openvitals.domain.model.ActivityPauseInterval
import tech.mmarca.openvitals.domain.model.ActivityProgressPoint
import tech.mmarca.openvitals.domain.model.ActivityCadenceKind
import tech.mmarca.openvitals.domain.model.ActivityCadenceSample
import tech.mmarca.openvitals.domain.model.ActivityWriteRequest
import tech.mmarca.openvitals.domain.model.SpeedSample
import tech.mmarca.openvitals.domain.model.BleRecordingSampleBuffer
import tech.mmarca.openvitals.domain.model.CaloriesBurnedSource
import tech.mmarca.openvitals.domain.model.CaloriesBurnedValue
import tech.mmarca.openvitals.domain.model.DailySteps
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.domain.model.ExerciseRoutePoint
import tech.mmarca.openvitals.domain.model.PlannedExerciseBlockData
import tech.mmarca.openvitals.domain.model.PlannedExerciseCompletion
import tech.mmarca.openvitals.domain.model.PlannedExerciseData
import tech.mmarca.openvitals.domain.model.PlannedExerciseStepData
import tech.mmarca.openvitals.domain.model.PlannedExerciseWriteRequest
import tech.mmarca.openvitals.domain.model.deduplicateExerciseSessions
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlin.reflect.KClass

private const val DailyStepsMaxQueryDays = 366L

internal class ActivityHealthReader(
    private val support: HealthConnectReaderSupport,
    private val appPackageName: String,
) {
    suspend fun readSteps(date: LocalDate): Long {
        val (start, end) = support.dayRange(date)
        return support.withLogging("readSteps[$date][$start..$end]", 0L) {
            support.client().aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                )
            )[StepsRecord.COUNT_TOTAL] ?: 0L
        }
    }

    suspend fun readTodaySteps(): Long = readSteps(LocalDate.now())

    suspend fun readDailySteps(
        startDate: LocalDate,
        endDate: LocalDate,
        includeSteps: Boolean = true,
        includeDistance: Boolean = true,
        includeWheelchairPushes: Boolean = false,
        includeFloors: Boolean = false,
        includeActiveCalories: Boolean = false,
        includeElevation: Boolean = false,
    ): List<DailySteps> =
        dailyStepDateChunks(startDate, endDate).flatMap { (chunkStart, chunkEnd) ->
            readDailyStepsChunk(
                startDate = chunkStart,
                endDate = chunkEnd,
                includeSteps = includeSteps,
                includeDistance = includeDistance,
                includeWheelchairPushes = includeWheelchairPushes,
                includeFloors = includeFloors,
                includeActiveCalories = includeActiveCalories,
                includeElevation = includeElevation,
            )
        }

    private suspend fun readDailyStepsChunk(
        startDate: LocalDate,
        endDate: LocalDate,
        includeSteps: Boolean,
        includeDistance: Boolean,
        includeWheelchairPushes: Boolean,
        includeFloors: Boolean,
        includeActiveCalories: Boolean,
        includeElevation: Boolean,
    ): List<DailySteps> {
        val zone = ZoneId.systemDefault()
        val start = startDate.atStartOfDay(zone).toInstant()
        val end = endDate.plusDays(1).atStartOfDay(zone).toInstant()
        return support.withLogging("readDailySteps[$start..$end]", emptyList()) {
            val metrics = buildSet {
                if (includeSteps) add(StepsRecord.COUNT_TOTAL)
                if (includeDistance) add(DistanceRecord.DISTANCE_TOTAL)
                if (includeWheelchairPushes) add(WheelchairPushesRecord.COUNT_TOTAL)
                if (includeFloors) add(FloorsClimbedRecord.FLOORS_CLIMBED_TOTAL)
                if (includeActiveCalories) add(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL)
                if (includeElevation) add(ElevationGainedRecord.ELEVATION_GAINED_TOTAL)
            }
            support.client().aggregateGroupByDuration(
                AggregateGroupByDurationRequest(
                    metrics = metrics,
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    timeRangeSlicer = Duration.ofDays(1),
                )
            ).map { bucket ->
                DailySteps(
                    date = bucket.startTime.atZone(zone).toLocalDate(),
                    steps = if (includeSteps) {
                        bucket.result[StepsRecord.COUNT_TOTAL] ?: 0L
                    } else {
                        0L
                    },
                    distanceMeters = if (includeDistance) {
                        bucket.result[DistanceRecord.DISTANCE_TOTAL]?.inMeters ?: 0.0
                    } else {
                        0.0
                    },
                    wheelchairPushes = if (includeWheelchairPushes) {
                        bucket.result[WheelchairPushesRecord.COUNT_TOTAL] ?: 0L
                    } else {
                        null
                    },
                    floorsClimbed = if (includeFloors) {
                        bucket.result[FloorsClimbedRecord.FLOORS_CLIMBED_TOTAL]?.toInt() ?: 0
                    } else {
                        null
                    },
                    activeCaloriesKcal = if (includeActiveCalories) {
                        bucket.result[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories ?: 0.0
                    } else {
                        null
                    },
                    elevationGainedMeters = if (includeElevation) {
                        bucket.result[ElevationGainedRecord.ELEVATION_GAINED_TOTAL]?.inMeters ?: 0.0
                    } else {
                        null
                    },
                )
            }
        }
    }

    suspend fun readWheelchairPushes(date: LocalDate): Long {
        val (start, end) = support.dayRange(date)
        return support.withLogging("readWheelchairPushes[$date][$start..$end]", 0L) {
            support.client().aggregate(
                AggregateRequest(
                    metrics = setOf(WheelchairPushesRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                )
            )[WheelchairPushesRecord.COUNT_TOTAL] ?: 0L
        }
    }

    suspend fun readFloorsClimbed(date: LocalDate): Int {
        val (start, end) = support.dayRange(date)
        return support.withLogging("readFloorsClimbed[$date][$start..$end]", 0) {
            support.client().aggregate(
                AggregateRequest(
                    metrics = setOf(FloorsClimbedRecord.FLOORS_CLIMBED_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                )
            )[FloorsClimbedRecord.FLOORS_CLIMBED_TOTAL]?.toInt() ?: 0
        }
    }

    suspend fun readElevationGained(date: LocalDate): Double {
        val (start, end) = support.dayRange(date)
        return support.withLogging("readElevationGained[$date][$start..$end]", 0.0) {
            support.client().aggregate(
                AggregateRequest(
                    metrics = setOf(ElevationGainedRecord.ELEVATION_GAINED_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                )
            )[ElevationGainedRecord.ELEVATION_GAINED_TOTAL]?.inMeters ?: 0.0
        }
    }

    suspend fun readActivityProgress(
        date: LocalDate,
        includeSteps: Boolean = true,
        includeDistance: Boolean,
        includeCalories: Boolean,
        includeActiveCalories: Boolean,
        includeCaloriesEstimate: Boolean = false,
        includeWheelchairPushes: Boolean,
        includeFloors: Boolean,
        includeElevation: Boolean,
    ): List<ActivityProgressPoint> {
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant()
        val end = if (date == LocalDate.now()) Instant.now() else date.plusDays(1).atStartOfDay(zone).toInstant()
        return support.withLogging("readActivityProgress[$date][$start..$end]", emptyList()) {
            val client = support.client()
            val includeEstimatedCalories = includeCalories && includeCaloriesEstimate
            val bmrKcalPerDay = if (includeEstimatedCalories) {
                client.readLatestBmrKcalPerDayBefore(end)
            } else {
                null
            }
            val metrics = buildSet {
                if (includeSteps) add(StepsRecord.COUNT_TOTAL)
                if (includeDistance) add(DistanceRecord.DISTANCE_TOTAL)
                if (includeCalories) add(TotalCaloriesBurnedRecord.ENERGY_TOTAL)
                if (includeActiveCalories || includeEstimatedCalories) {
                    add(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL)
                }
                if (includeWheelchairPushes) add(WheelchairPushesRecord.COUNT_TOTAL)
                if (includeFloors) add(FloorsClimbedRecord.FLOORS_CLIMBED_TOTAL)
                if (includeElevation) add(ElevationGainedRecord.ELEVATION_GAINED_TOTAL)
            }
            var cumulativeSteps = 0L
            var cumulativeDistance = 0.0
            var cumulativeCalories = 0.0
            var cumulativeActiveCalories = 0.0
            var hasActiveCaloriesData = false
            var cumulativeWheelchairPushes = 0L
            var cumulativeFloors = 0
            var cumulativeElevation = 0.0

            val buckets = support.client().aggregateGroupByDuration(
                AggregateGroupByDurationRequest(
                    metrics = metrics,
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    timeRangeSlicer = Duration.ofHours(1),
                )
            )
            val hasRecordedTotalCaloriesData = includeCalories &&
                buckets.any { bucket -> bucket.result[TotalCaloriesBurnedRecord.ENERGY_TOTAL] != null }
            buckets.map { bucket ->
                if (includeSteps) {
                    cumulativeSteps += bucket.result[StepsRecord.COUNT_TOTAL] ?: 0L
                }
                if (includeDistance) {
                    cumulativeDistance += bucket.result[DistanceRecord.DISTANCE_TOTAL]?.inMeters ?: 0.0
                }
                if (includeCalories) {
                    cumulativeCalories += bucket.result[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories ?: 0.0
                }
                if (includeActiveCalories || includeEstimatedCalories) {
                    val activeCalories = bucket.result[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]
                    hasActiveCaloriesData = hasActiveCaloriesData || activeCalories != null
                    cumulativeActiveCalories += activeCalories?.inKilocalories ?: 0.0
                }
                if (includeWheelchairPushes) {
                    cumulativeWheelchairPushes += bucket.result[WheelchairPushesRecord.COUNT_TOTAL] ?: 0L
                }
                if (includeFloors) {
                    cumulativeFloors += bucket.result[FloorsClimbedRecord.FLOORS_CLIMBED_TOTAL]?.toInt() ?: 0
                }
                if (includeElevation) {
                    cumulativeElevation += bucket.result[ElevationGainedRecord.ELEVATION_GAINED_TOTAL]?.inMeters ?: 0.0
                }
                val totalCaloriesBurnedKcal = if (includeCalories) {
                    totalCaloriesRecordedOrIntervalEstimated(
                        recordedTotalCaloriesKcal = if (hasRecordedTotalCaloriesData) cumulativeCalories else null,
                        activeCaloriesKcal = if (includeEstimatedCalories && hasActiveCaloriesData) {
                            cumulativeActiveCalories
                        } else {
                            null
                        },
                        bmrKcalPerDay = bmrKcalPerDay,
                        start = start,
                        end = bucket.endTime,
                    )?.kcal
                } else {
                    null
                }
                ActivityProgressPoint(
                    time = bucket.endTime,
                    totalSteps = cumulativeSteps,
                    totalDistanceMeters = if (includeDistance) cumulativeDistance else null,
                    totalCaloriesBurnedKcal = totalCaloriesBurnedKcal,
                    totalActiveCaloriesKcal = if (includeActiveCalories) cumulativeActiveCalories else null,
                    totalWheelchairPushes = if (includeWheelchairPushes) cumulativeWheelchairPushes else null,
                    totalFloorsClimbed = if (includeFloors) cumulativeFloors else null,
                    totalElevationGainedMeters = if (includeElevation) cumulativeElevation else null,
                )
            }
        }
    }

    suspend fun readDistanceMeters(date: LocalDate): Double {
        val (start, end) = support.dayRange(date)
        return support.withLogging("readDistanceMeters[$date][$start..$end]", 0.0) {
            support.client().aggregate(
                AggregateRequest(
                    metrics = setOf(DistanceRecord.DISTANCE_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                )
            )[DistanceRecord.DISTANCE_TOTAL]?.inMeters ?: 0.0
        }
    }

    suspend fun readTodayDistanceMeters(): Double = readDistanceMeters(LocalDate.now())

    suspend fun readCaloriesBurned(
        date: LocalDate,
        includeEstimatedCalories: Boolean = false,
    ): CaloriesBurnedValue? {
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant()
        val end = if (date == LocalDate.now()) Instant.now() else date.plusDays(1).atStartOfDay(zone).toInstant()
        return support.withNullableLogging("readCaloriesBurned[$date][$start..$end]") {
            val client = support.client()
            val metrics = buildSet {
                add(TotalCaloriesBurnedRecord.ENERGY_TOTAL)
                if (includeEstimatedCalories) add(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL)
            }

            val aggregate = client.aggregate(
                AggregateRequest(
                    metrics = metrics,
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                )
            )
            val recordedTotalCaloriesKcal = aggregate[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories
            totalCaloriesRecordedOrDailyEstimated(
                recordedTotalCaloriesKcal = recordedTotalCaloriesKcal,
                activeCaloriesKcal = if (includeEstimatedCalories && recordedTotalCaloriesKcal == null) {
                    aggregate[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories
                } else {
                    null
                },
                bmrKcalPerDay = if (includeEstimatedCalories && recordedTotalCaloriesKcal == null) {
                    client.readLatestBmrKcalPerDayBefore(end)
                } else {
                    null
                },
            )
        }
    }

    suspend fun readCaloriesKcal(
        date: LocalDate,
        includeEstimatedCalories: Boolean = false,
    ): Double? = readCaloriesBurned(date, includeEstimatedCalories)?.kcal

    suspend fun readTodayCaloriesKcal(includeEstimatedCalories: Boolean = false): Double? =
        readCaloriesKcal(LocalDate.now(), includeEstimatedCalories)

    suspend fun readLatestWorkout(date: LocalDate): ExerciseData? {
        val (start, end) = support.dayRange(date)
        return support.withNullableLogging("readLatestWorkout[$date][$start..$end]") {
            support.client().readRecordsPaged(
                recordType = ExerciseSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = false,
                pageSize = 1,
                maxRecords = 1,
            ).firstOrNull()?.toExerciseData(appPackageName = appPackageName)
        }
    }

    suspend fun readLatestWorkout(): ExerciseData? =
        support.withNullableLogging("readLatestWorkout") {
            support.client().readRecordsPaged(
                recordType = ExerciseSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.before(Instant.now()),
                ascendingOrder = false,
                pageSize = 1,
                maxRecords = 1,
            ).firstOrNull()?.toExerciseData(appPackageName = appPackageName)
        }

    suspend fun readExerciseSessions(start: Instant, end: Instant): List<ExerciseData> =
        support.withLogging("readExerciseSessions[$start..$end]", emptyList()) {
            val sessions = support.client().readRecordsPaged(
                recordType = ExerciseSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = false,
                pageSize = 50,
            ).map { it.toExerciseData(appPackageName = appPackageName) }

            deduplicateExerciseSessions(sessions)
        }

    suspend fun readExerciseSession(
        id: String,
        includeSteps: Boolean,
        includeDistance: Boolean,
        includeTotalCalories: Boolean,
        includeActiveCalories: Boolean,
        includeTotalCaloriesEstimate: Boolean = false,
        includeWheelchairPushes: Boolean,
        includeFloors: Boolean,
        includeElevation: Boolean,
        includeSpeed: Boolean,
        includePower: Boolean,
        includeStepsCadence: Boolean,
        includeCyclingCadence: Boolean,
        includeHeartRate: Boolean = false,
    ): ExerciseData? =
        support.withNullableLogging("readExerciseSession[$id]") {
            val record = support.client().readRecord(ExerciseSessionRecord::class, id).record
            val metrics = buildSet {
                if (includeSteps) add(StepsRecord.COUNT_TOTAL)
                if (includeDistance) add(DistanceRecord.DISTANCE_TOTAL)
                if (includeTotalCalories) add(TotalCaloriesBurnedRecord.ENERGY_TOTAL)
                if (includeActiveCalories || includeTotalCaloriesEstimate) add(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL)
                if (includeWheelchairPushes) add(WheelchairPushesRecord.COUNT_TOTAL)
                if (includeFloors) add(FloorsClimbedRecord.FLOORS_CLIMBED_TOTAL)
                if (includeElevation) add(ElevationGainedRecord.ELEVATION_GAINED_TOTAL)
                if (includeSpeed) add(SpeedRecord.SPEED_AVG)
                if (includePower) add(PowerRecord.POWER_AVG)
                if (includeStepsCadence) add(StepsCadenceRecord.RATE_AVG)
                if (includeCyclingCadence) add(CyclingPedalingCadenceRecord.RPM_AVG)
                if (includeHeartRate) add(HeartRateRecord.BPM_AVG)
            }
            val aggregate = if (metrics.isEmpty()) {
                null
            } else {
                runCatching {
                    support.client().aggregate(
                        AggregateRequest(
                            metrics = metrics,
                            timeRangeFilter = TimeRangeFilter.between(record.startTime, record.endTime),
                        )
                    )
                }.onFailure {
                    if (HealthConnectRateLimitBackoff.isRateLimitFailure(it)) {
                        throw it
                    }
                    Log.e(TAG, "Failed readExerciseSession aggregate ${support.diagnosticsSummary()}", it)
                }.getOrNull()
            }
            val totalCaloriesMetricKcal = if (includeTotalCalories && aggregate != null) {
                aggregate[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories
            } else {
                null
            }
            val shouldEstimateTotalCalories = includeTotalCalories &&
                includeTotalCaloriesEstimate &&
                totalCaloriesMetricKcal == null
            val activeCaloriesMetricKcal = if ((includeActiveCalories || shouldEstimateTotalCalories) && aggregate != null) {
                aggregate[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories
            } else {
                null
            }
            val totalCalories = if (includeTotalCalories && aggregate != null) {
                totalCaloriesRecordedOrIntervalEstimated(
                    recordedTotalCaloriesKcal = totalCaloriesMetricKcal,
                    activeCaloriesKcal = if (shouldEstimateTotalCalories) activeCaloriesMetricKcal else null,
                    bmrKcalPerDay = if (shouldEstimateTotalCalories) {
                        support.client().readLatestBmrKcalPerDayBefore(record.endTime)
                    } else {
                        null
                    },
                    start = record.startTime,
                    end = record.endTime,
                )
            } else {
                null
            }

            record.toExerciseData(
                steps = if (includeSteps && aggregate != null) aggregate[StepsRecord.COUNT_TOTAL] ?: 0L else null,
                totalDistanceMeters = if (includeDistance && aggregate != null) {
                    aggregate[DistanceRecord.DISTANCE_TOTAL]?.inMeters ?: 0.0
                } else {
                    null
                },
                totalCaloriesKcal = totalCalories?.kcal,
                totalCaloriesSource = totalCalories?.source ?: CaloriesBurnedSource.NO_DATA,
                activeCaloriesKcal = if (includeActiveCalories && aggregate != null) {
                    activeCaloriesMetricKcal ?: 0.0
                } else {
                    null
                },
                wheelchairPushes = if (includeWheelchairPushes && aggregate != null) {
                    aggregate[WheelchairPushesRecord.COUNT_TOTAL] ?: 0L
                } else {
                    null
                },
                floorsClimbed = if (includeFloors && aggregate != null) {
                    aggregate[FloorsClimbedRecord.FLOORS_CLIMBED_TOTAL]?.toInt() ?: 0
                } else {
                    null
                },
                elevationGainedMeters = if (includeElevation && aggregate != null) {
                    aggregate[ElevationGainedRecord.ELEVATION_GAINED_TOTAL]?.inMeters ?: 0.0
                } else {
                    null
                },
                averageSpeedMetersPerSecond = if (includeSpeed && aggregate != null) {
                    aggregate[SpeedRecord.SPEED_AVG]?.inMetersPerSecond
                } else {
                    null
                },
                averagePowerWatts = if (includePower && aggregate != null) {
                    aggregate[PowerRecord.POWER_AVG]?.inWatts
                } else {
                    null
                },
                averageStepsCadenceRate = if (includeStepsCadence && aggregate != null) {
                    aggregate[StepsCadenceRecord.RATE_AVG]
                } else {
                    null
                },
                averageCyclingCadenceRpm = if (includeCyclingCadence && aggregate != null) {
                    aggregate[CyclingPedalingCadenceRecord.RPM_AVG]
                } else {
                    null
                },
                averageHeartRateBpm = if (includeHeartRate && aggregate != null) {
                    aggregate[HeartRateRecord.BPM_AVG]?.toLong()
                } else {
                    null
                },
                appPackageName = appPackageName,
            )
        }

    suspend fun readSpeedSamples(start: Instant, end: Instant): List<SpeedSample> =
        support.withLogging("readSpeedSamples[$start..$end]", emptyList()) {
            support.client().readRecordsPaged(
                recordType = SpeedRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = true,
                pageSize = 500,
            ).flatMap { record ->
                val source = record.metadata.dataOrigin.packageName
                record.samples.map { sample ->
                    SpeedSample(
                        time = sample.time,
                        metersPerSecond = sample.speed.inMetersPerSecond,
                        source = source,
                    )
                }
            }
        }

    suspend fun readActivityCadenceSamples(start: Instant, end: Instant): List<ActivityCadenceSample> =
        support.withLogging("readActivityCadenceSamples[$start..$end]", emptyList()) {
            val cyclingSamples = support.client().readRecordsPaged(
                recordType = CyclingPedalingCadenceRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = true,
                pageSize = 500,
            ).flatMap { record ->
                val source = record.metadata.dataOrigin.packageName
                record.samples.map { sample ->
                    ActivityCadenceSample(
                        time = sample.time,
                        rate = sample.revolutionsPerMinute,
                        kind = ActivityCadenceKind.CYCLING,
                        source = source,
                    )
                }
            }
            val stepsSamples = support.client().readRecordsPaged(
                recordType = StepsCadenceRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = true,
                pageSize = 500,
            ).flatMap { record ->
                val source = record.metadata.dataOrigin.packageName
                record.samples.map { sample ->
                    ActivityCadenceSample(
                        time = sample.time,
                        rate = sample.rate,
                        kind = ActivityCadenceKind.STEPS,
                        source = source,
                    )
                }
            }
            (cyclingSamples + stepsSamples).sortedBy { it.time }
        }

    suspend fun readPlannedExerciseSessions(start: Instant, end: Instant): List<PlannedExerciseData> =
        support.withLogging("readPlannedExerciseSessions[$start..$end]", emptyList()) {
            support.client().readRecordsPaged(
                recordType = PlannedExerciseSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = true,
                pageSize = 100,
            ).map { record ->
                PlannedExerciseData(
                    id = record.metadata.id,
                    title = record.title,
                    exerciseType = record.exerciseType,
                    startTime = record.startTime,
                    endTime = record.endTime,
                    hasExplicitTime = record.hasExplicitTime,
                    completedExerciseSessionId = record.completedExerciseSessionId,
                    notes = record.notes,
                    blockCount = record.blocks.size,
                    source = record.metadata.dataOrigin.packageName,
                    blocks = record.blocks.map { it.toPlannedExerciseBlockData() },
                )
            }
        }

    suspend fun writePlannedExerciseSession(request: PlannedExerciseWriteRequest): String = withContext(Dispatchers.IO) {
        request.id?.let { existingId ->
            support.client().deleteRecords(
                recordType = PlannedExerciseSessionRecord::class,
                recordIdsList = listOf(existingId),
                clientRecordIdsList = emptyList(),
            )
        }
        val zone = ZoneId.systemDefault()
        val record = PlannedExerciseSessionRecord(
            startTime = request.startTime,
            startZoneOffset = zone.rules.getOffset(request.startTime),
            endTime = request.endTime,
            endZoneOffset = zone.rules.getOffset(request.endTime),
            metadata = Metadata.manualEntry(
                clientRecordId = "openvitals_planned_activity_${request.startTime.toEpochMilli()}_${UUID.randomUUID()}",
                device = Device(type = Device.TYPE_PHONE),
            ),
            blocks = request.blocks.map { it.toPlannedExerciseBlock() },
            exerciseType = request.exerciseType,
            title = request.title?.trim()?.takeIf { it.isNotBlank() },
            notes = request.notes?.trim()?.takeIf { it.isNotBlank() },
        )
        support.client()
            .insertRecords(listOf(record))
            .recordIdsList
            .firstOrNull()
            ?: record.metadata.clientRecordId.orEmpty()
    }

    suspend fun writeActivityEntry(request: ActivityWriteRequest): String = withContext(Dispatchers.IO) {
        validateActivityWriteRequest(request)

        val zone = ZoneId.systemDefault()
        val sessionClientRecordId = "openvitals_activity_${request.startTime.toEpochMilli()}_${UUID.randomUUID()}"
        val sessionMetadata = Metadata.manualEntry(
            clientRecordId = sessionClientRecordId,
            device = Device(type = Device.TYPE_PHONE),
        )
        val exerciseSegments = request.toExerciseSegments()
        val exerciseLaps = request.toExerciseLaps()
        val session = request.toExerciseSessionRecord(sessionMetadata, exerciseSegments, zone)
        val extraRecords = request.toManualActivityMetricRecords(zone)

        Log.d(
            TAG,
            "Writing activity entry type=${request.exerciseType} " +
                "hasRoute=${request.routePoints.isNotEmpty()} pauses=${request.pauseIntervals.size} " +
                "segments=${exerciseSegments.size} laps=${exerciseLaps.size} " +
                "extras=${extraRecords.size} ${support.diagnosticsSummary()}",
        )
        support.client()
            .insertRecords(listOf(session) + extraRecords)
            .recordIdsList
            .firstOrNull()
            ?: sessionClientRecordId
    }

    suspend fun updateActivityEntry(id: String, request: ActivityWriteRequest) = withContext(Dispatchers.IO) {
        validateActivityWriteRequest(request)

        val existing = support.client().readRecord(ExerciseSessionRecord::class, id).record
        existing.requireOpenVitalsOrigin(appPackageName)

        val zone = ZoneId.systemDefault()
        val exerciseSegments = request.toExerciseSegments()
        val exerciseLaps = request.toExerciseLaps()
        val session = request.toExerciseSessionRecord(
            metadata = Metadata.manualEntryWithId(
                id = id,
                device = existing.metadata.device ?: Device(type = Device.TYPE_PHONE),
            ),
            exerciseSegments = exerciseSegments,
            zone = zone,
        )
        val extraRecords = request.toManualActivityMetricRecords(zone)

        Log.d(
            TAG,
            "Updating activity entry type=${request.exerciseType} " +
                "hasRoute=${request.routePoints.isNotEmpty()} pauses=${request.pauseIntervals.size} " +
                "segments=${exerciseSegments.size} laps=${exerciseLaps.size} " +
                "extras=${extraRecords.size} ${support.diagnosticsSummary()}",
        )
        support.client().updateRecords(listOf(session))
        deleteManualActivityMetricRecords(existing.startTime, existing.endTime)
        if (extraRecords.isNotEmpty()) {
            support.client().insertRecords(extraRecords)
        }
    }

    suspend fun deleteActivityEntry(id: String) = withContext(Dispatchers.IO) {
        val existing = support.client().readRecord(ExerciseSessionRecord::class, id).record
        existing.requireOpenVitalsOrigin(appPackageName)

        Log.d(TAG, "Deleting activity entry ${support.diagnosticsSummary()}")
        deleteManualActivityMetricRecords(existing.startTime, existing.endTime)
        support.client().deleteRecords(
            recordType = ExerciseSessionRecord::class,
            recordIdsList = listOf(existing.metadata.id),
            clientRecordIdsList = emptyList(),
        )
    }

    private fun ActivityWriteRequest.toExerciseSessionRecord(
        metadata: Metadata,
        exerciseSegments: List<ExerciseSegment>,
        zone: ZoneId,
    ): ExerciseSessionRecord =
        ExerciseSessionRecord(
            startTime = startTime,
            startZoneOffset = zone.rules.getOffset(startTime),
            endTime = endTime,
            endZoneOffset = zone.rules.getOffset(endTime),
            metadata = metadata,
            exerciseType = exerciseType,
            title = title?.trim()?.takeIf { it.isNotBlank() },
            notes = notes?.trim()?.takeIf { it.isNotBlank() },
            segments = exerciseSegments,
            laps = toExerciseLaps(),
            exerciseRoute = routePoints.toExerciseRouteOrNull(),
            plannedExerciseSessionId = plannedExerciseSessionId,
        )

    private fun ActivityWriteRequest.toManualActivityMetricRecords(zone: ZoneId): List<Record> {
        val startOffset = zone.rules.getOffset(startTime)
        val endOffset = zone.rules.getOffset(endTime)
        return buildList {
            distanceMeters?.let { meters ->
                add(
                    DistanceRecord(
                        startTime = startTime,
                        startZoneOffset = startOffset,
                        endTime = endTime,
                        endZoneOffset = endOffset,
                        distance = meters.meters,
                        metadata = manualActivityMetricMetadata("distance", startTime),
                    )
                )
            }
            elevationGainedMeters?.let { meters ->
                add(
                    ElevationGainedRecord(
                        startTime = startTime,
                        startZoneOffset = startOffset,
                        endTime = endTime,
                        endZoneOffset = endOffset,
                        elevation = meters.meters,
                        metadata = manualActivityMetricMetadata("elevation", startTime),
                    )
                )
            }
            activeCaloriesKcal?.let { kcal ->
                add(
                    ActiveCaloriesBurnedRecord(
                        startTime = startTime,
                        startZoneOffset = startOffset,
                        endTime = endTime,
                        endZoneOffset = endOffset,
                        energy = kcal.kilocalories,
                        metadata = manualActivityMetricMetadata("active_calories", startTime),
                    )
                )
            }
            totalCaloriesKcal?.let { kcal ->
                add(
                    TotalCaloriesBurnedRecord(
                        startTime = startTime,
                        startZoneOffset = startOffset,
                        endTime = endTime,
                        endZoneOffset = endOffset,
                        energy = kcal.kilocalories,
                        metadata = manualActivityMetricMetadata("total_calories", startTime),
                    )
                )
            }
            stepsCount?.let { steps ->
                add(
                    StepsRecord(
                        startTime = startTime,
                        startZoneOffset = startOffset,
                        endTime = endTime,
                        endZoneOffset = endOffset,
                        count = steps,
                        metadata = manualActivityMetricMetadata("steps", startTime),
                    )
                )
            }
            addAll(bleSamples.toManualActivitySensorRecords(startTime, endTime, zone))
        }
    }

    private fun BleRecordingSampleBuffer.toManualActivitySensorRecords(
        startTime: Instant,
        endTime: Instant,
        zone: ZoneId,
    ): List<Record> {
        if (isEmpty()) return emptyList()
        val startOffset = zone.rules.getOffset(startTime)
        val endOffset = zone.rules.getOffset(endTime)
        return buildList {
            if (heartRateSamples.isNotEmpty()) {
                add(
                    HeartRateRecord(
                        startTime = startTime,
                        startZoneOffset = startOffset,
                        endTime = endTime,
                        endZoneOffset = endOffset,
                        samples = heartRateSamples.map {
                            HeartRateRecord.Sample(
                                time = it.time.coerceInSession(startTime, endTime),
                                beatsPerMinute = it.beatsPerMinute,
                            )
                        },
                        metadata = manualActivityMetricMetadata("heart_rate", startTime),
                    )
                )
            }
            if (powerSamples.isNotEmpty()) {
                add(
                    PowerRecord(
                        startTime = startTime,
                        startZoneOffset = startOffset,
                        endTime = endTime,
                        endZoneOffset = endOffset,
                        samples = powerSamples.map { sample ->
                            PowerRecord.Sample(
                                time = sample.time.coerceInSession(startTime, endTime),
                                power = Power.watts(sample.watts),
                            )
                        },
                        metadata = manualActivityMetricMetadata("power", startTime),
                    )
                )
            }
            if (cyclingCadenceSamples.isNotEmpty()) {
                add(
                    CyclingPedalingCadenceRecord(
                        startTime = startTime,
                        startZoneOffset = startOffset,
                        endTime = endTime,
                        endZoneOffset = endOffset,
                        samples = cyclingCadenceSamples.map {
                            CyclingPedalingCadenceRecord.Sample(
                                time = it.time.coerceInSession(startTime, endTime),
                                revolutionsPerMinute = it.rpm.toDouble(),
                            )
                        },
                        metadata = manualActivityMetricMetadata("cycling_cadence", startTime),
                    )
                )
            }
            val cyclingSpeedSamples = speedSamples.filterNot { it.isRunning }
            if (cyclingSpeedSamples.isNotEmpty()) {
                add(
                    SpeedRecord(
                        startTime = startTime,
                        startZoneOffset = startOffset,
                        endTime = endTime,
                        endZoneOffset = endOffset,
                        samples = cyclingSpeedSamples.map { sample ->
                            SpeedRecord.Sample(
                                time = sample.time.coerceInSession(startTime, endTime),
                                speed = Velocity.metersPerSecond(sample.metersPerSecond),
                            )
                        },
                        metadata = manualActivityMetricMetadata("speed", startTime),
                    )
                )
            }
            val runningSpeedSamples = speedSamples.filter { it.isRunning }
            if (runningSpeedSamples.isNotEmpty()) {
                add(
                    SpeedRecord(
                        startTime = startTime,
                        startZoneOffset = startOffset,
                        endTime = endTime,
                        endZoneOffset = endOffset,
                        samples = runningSpeedSamples.map { sample ->
                            SpeedRecord.Sample(
                                time = sample.time.coerceInSession(startTime, endTime),
                                speed = Velocity.metersPerSecond(sample.metersPerSecond),
                            )
                        },
                        metadata = manualActivityMetricMetadata("running_speed", startTime),
                    )
                )
            }
            if (stepsCadenceSamples.isNotEmpty()) {
                add(
                    StepsCadenceRecord(
                        startTime = startTime,
                        startZoneOffset = startOffset,
                        endTime = endTime,
                        endZoneOffset = endOffset,
                        samples = stepsCadenceSamples.map {
                            StepsCadenceRecord.Sample(
                                time = it.time.coerceInSession(startTime, endTime),
                                rate = it.stepsPerMinute.toDouble(),
                            )
                        },
                        metadata = manualActivityMetricMetadata("steps_cadence", startTime),
                    )
                )
            }
        }
    }

    private fun Instant.coerceInSession(startTime: Instant, endTime: Instant): Instant =
        when {
            isBefore(startTime) -> startTime
            isAfter(endTime) -> endTime
            else -> this
        }

    private suspend fun deleteManualActivityMetricRecords(start: Instant, end: Instant) {
        deleteManualActivityMetricRecords(StepsRecord::class, "steps", start, end)
        deleteManualActivityMetricRecords(DistanceRecord::class, "distance", start, end)
        deleteManualActivityMetricRecords(ElevationGainedRecord::class, "elevation", start, end)
        deleteManualActivityMetricRecords(ActiveCaloriesBurnedRecord::class, "active_calories", start, end)
        deleteManualActivityMetricRecords(TotalCaloriesBurnedRecord::class, "total_calories", start, end)
        deleteManualActivityMetricRecords(HeartRateRecord::class, "heart_rate", start, end)
        deleteManualActivityMetricRecords(PowerRecord::class, "power", start, end)
        deleteManualActivityMetricRecords(CyclingPedalingCadenceRecord::class, "cycling_cadence", start, end)
        deleteManualActivityMetricRecords(SpeedRecord::class, "speed", start, end)
        deleteManualActivityMetricRecords(SpeedRecord::class, "running_speed", start, end)
        deleteManualActivityMetricRecords(StepsCadenceRecord::class, "steps_cadence", start, end)
    }

    private suspend fun <T : Record> deleteManualActivityMetricRecords(
        recordType: KClass<T>,
        kind: String,
        start: Instant,
        end: Instant,
    ) {
        val recordIds = support.client().readRecordsPaged(
            recordType = recordType,
            timeRangeFilter = TimeRangeFilter.between(start, end),
            ascendingOrder = true,
        ).filter { record ->
            record.metadata.dataOrigin.packageName == appPackageName &&
                record.metadata.clientRecordId?.startsWith("openvitals_activity_${kind}_") == true
        }.map { record -> record.metadata.id }

        if (recordIds.isNotEmpty()) {
            support.client().deleteRecords(
                recordType = recordType,
                recordIdsList = recordIds,
                clientRecordIdsList = emptyList(),
            )
        }
    }

    private fun manualActivityMetricMetadata(kind: String, startTime: Instant): Metadata =
        Metadata.manualEntry(
            clientRecordId = "openvitals_activity_${kind}_${startTime.toEpochMilli()}_${UUID.randomUUID()}",
            device = Device(type = Device.TYPE_PHONE),
        )

    private fun validateActivityWriteRequest(request: ActivityWriteRequest) {
        require(request.startTime.isBefore(request.endTime)) { "Activity start must be before end." }
        request.distanceMeters?.let { require(it > 0.0 && it <= MaxActivityDistanceMeters) { "Distance must be greater than 0 m." } }
        request.elevationGainedMeters?.let { require(it >= 0.0 && it <= MaxActivityElevationMeters) { "Elevation gain is out of range." } }
        request.activeCaloriesKcal?.let { require(it > 0.0 && it <= MaxActivityCaloriesKcal) { "Active calories are out of range." } }
        request.totalCaloriesKcal?.let { require(it > 0.0 && it <= MaxActivityCaloriesKcal) { "Total calories are out of range." } }
        request.stepsCount?.let { require(it > 0L && it <= MaxActivitySteps) { "Steps are out of range." } }
        val sortedPauses = request.pauseIntervals.sortedBy { it.startTime }
        sortedPauses.forEach { interval ->
            require(interval.startTime.isBefore(interval.endTime)) { "Pause start must be before pause end." }
            require(!interval.startTime.isBefore(request.startTime) && !interval.endTime.isAfter(request.endTime)) {
                "Pause intervals must be inside the activity time range."
            }
        }
        sortedPauses.zipWithNext { previous, next ->
            require(!previous.endTime.isAfter(next.startTime)) { "Pause intervals must not overlap." }
        }
        require(request.routePoints.isEmpty() || request.routePoints.size >= MinRoutePointCount) {
            "Route must contain at least $MinRoutePointCount points."
        }
        request.routePoints.forEach { point ->
            require(!point.time.isBefore(request.startTime) && point.time.isBefore(request.endTime)) {
                "Route points must be inside the activity time range."
            }
        }
        val sortedSegments = request.exerciseSegments.sortedBy { it.startTime }
        sortedSegments.forEach { segment ->
            require(segment.startTime.isBefore(segment.endTime)) { "Segment start must be before segment end." }
            require(!segment.startTime.isBefore(request.startTime) && !segment.endTime.isAfter(request.endTime)) {
                "Exercise segments must be inside the activity time range."
            }
            require(segment.repetitions >= 0) { "Segment repetitions must not be negative." }
            require(segment.setIndex == null || segment.setIndex >= 0) { "Segment set index must not be negative." }
        }
        sortedSegments.zipWithNext { previous, next ->
            require(!previous.endTime.isAfter(next.startTime)) { "Exercise segments must not overlap." }
        }
        val sortedLaps = request.laps.sortedBy { it.startTime }
        sortedLaps.forEach { lap ->
            require(lap.startTime.isBefore(lap.endTime)) { "Lap start must be before lap end." }
            require(!lap.startTime.isBefore(request.startTime) && !lap.endTime.isAfter(request.endTime)) {
                "Exercise laps must be inside the activity time range."
            }
            lap.lengthMeters?.let { meters ->
                require(meters >= 0.0 && meters <= MaxActivityDistanceMeters) { "Lap length is out of range." }
            }
        }
        sortedLaps.zipWithNext { previous, next ->
            require(!previous.endTime.isAfter(next.startTime)) { "Exercise laps must not overlap." }
        }
    }

    private fun List<ExerciseRoutePoint>.toExerciseRouteOrNull(): ExerciseRoute? {
        if (isEmpty()) return null
        val route = sortedBy { it.time }.map { point ->
            ExerciseRoute.Location(
                time = point.time,
                latitude = point.latitude,
                longitude = point.longitude,
                horizontalAccuracy = point.horizontalAccuracyMeters?.meters,
                verticalAccuracy = point.verticalAccuracyMeters?.meters,
                altitude = point.altitudeMeters?.meters,
            )
        }
        return ExerciseRoute(route)
    }

    private companion object {
        private const val TAG = "HealthConnectManager"
        private const val MinRoutePointCount = 2
        private const val MaxActivityDistanceMeters = 1_000_000.0
        private const val MaxActivityElevationMeters = 1_000_000.0
        private const val MaxActivityCaloriesKcal = 1_000_000.0
        private const val MaxActivitySteps = 1_000_000L
    }
}

internal fun dailyStepDateChunks(
    startDate: LocalDate,
    endDate: LocalDate,
    maxDays: Long = DailyStepsMaxQueryDays,
): List<Pair<LocalDate, LocalDate>> {
    if (endDate.isBefore(startDate) || maxDays <= 0L) return emptyList()

    val chunks = mutableListOf<Pair<LocalDate, LocalDate>>()
    var chunkStart = startDate
    while (!chunkStart.isAfter(endDate)) {
        val chunkEnd = minOf(chunkStart.plusDays(maxDays - 1), endDate)
        chunks += chunkStart to chunkEnd
        chunkStart = chunkEnd.plusDays(1)
    }
    return chunks
}

internal fun PlannedExerciseBlock.toPlannedExerciseBlockData(): PlannedExerciseBlockData =
    PlannedExerciseBlockData(
        repetitions = repetitions,
        description = description,
        steps = steps.map { it.toPlannedExerciseStepData() },
    )

internal fun PlannedExerciseStep.toPlannedExerciseStepData(): PlannedExerciseStepData =
    PlannedExerciseStepData(
        exerciseType = exerciseType,
        exercisePhase = exercisePhase,
        description = description,
        completion = completionGoal.toPlannedExerciseCompletion(),
    )

internal fun PlannedExerciseBlockData.toPlannedExerciseBlock(): PlannedExerciseBlock =
    PlannedExerciseBlock(
        repetitions = repetitions,
        description = description,
        steps = steps.map { it.toPlannedExerciseStep() },
    )

internal fun PlannedExerciseStepData.toPlannedExerciseStep(): PlannedExerciseStep =
    PlannedExerciseStep(
        exerciseType = exerciseType,
        exercisePhase = exercisePhase,
        description = description,
        completionGoal = completion.toExerciseCompletionGoal(),
        performanceTargets = emptyList(),
    )

private fun ExerciseCompletionGoal.toPlannedExerciseCompletion(): PlannedExerciseCompletion =
    when (this) {
        is ExerciseCompletionGoal.RepetitionsGoal ->
            PlannedExerciseCompletion.Repetitions(repetitions)
        is ExerciseCompletionGoal.DurationGoal ->
            PlannedExerciseCompletion.DurationSeconds(duration.seconds)
        ExerciseCompletionGoal.ManualCompletion ->
            PlannedExerciseCompletion.Manual
        else ->
            PlannedExerciseCompletion.Unknown
    }

private fun PlannedExerciseCompletion.toExerciseCompletionGoal(): ExerciseCompletionGoal =
    when (this) {
        is PlannedExerciseCompletion.Repetitions ->
            ExerciseCompletionGoal.RepetitionsGoal(repetitions)
        is PlannedExerciseCompletion.DurationSeconds ->
            ExerciseCompletionGoal.DurationGoal(Duration.ofSeconds(seconds.coerceAtLeast(1L)))
        PlannedExerciseCompletion.Manual ->
            ExerciseCompletionGoal.ManualCompletion
        PlannedExerciseCompletion.Unknown ->
            ExerciseCompletionGoal.UnknownGoal
    }

internal fun ActivityWriteRequest.toExerciseSegments(): List<ExerciseSegment> {
    if (exerciseSegments.isNotEmpty()) {
        return exerciseSegments
            .sortedBy { it.startTime }
            .map { it.toExerciseSegment() }
    }

    val activeSegmentType = exerciseType.toActiveExerciseSegmentType()
    val sortedPauses = pauseIntervals.sortedBy { it.startTime }
    return buildList {
        var activeStart = startTime
        sortedPauses.forEach { pause ->
            if (activeStart.isBefore(pause.startTime)) {
                add(
                    ExerciseSegment(
                        startTime = activeStart,
                        endTime = pause.startTime,
                        segmentType = activeSegmentType,
                    )
                )
            }
            add(
                ExerciseSegment(
                    startTime = pause.startTime,
                    endTime = pause.endTime,
                    segmentType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_PAUSE,
                )
            )
            if (activeStart.isBefore(pause.endTime)) {
                activeStart = pause.endTime
            }
        }
        if (activeStart.isBefore(endTime)) {
            add(
                ExerciseSegment(
                    startTime = activeStart,
                    endTime = endTime,
                    segmentType = activeSegmentType,
                )
            )
        }
    }
}

internal fun ActivityWriteRequest.toExerciseLaps(): List<ExerciseLap> =
    laps
        .sortedBy { it.startTime }
        .map { lap ->
            ExerciseLap(
                startTime = lap.startTime,
                endTime = lap.endTime,
                length = lap.lengthMeters?.meters,
            )
        }

private fun ActivityExerciseSegmentWrite.toExerciseSegment(): ExerciseSegment =
    ExerciseSegment(
        startTime = startTime,
        endTime = endTime,
        segmentType = segmentType,
        repetitions = repetitions,
        setIndex = setIndex,
    )

private fun Int.toActiveExerciseSegmentType(): Int =
    when (this) {
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING -> ExerciseSegment.EXERCISE_SEGMENT_TYPE_BIKING
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY -> ExerciseSegment.EXERCISE_SEGMENT_TYPE_BIKING_STATIONARY
        ExerciseSessionRecord.EXERCISE_TYPE_ELLIPTICAL -> ExerciseSegment.EXERCISE_SEGMENT_TYPE_ELLIPTICAL
        ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING ->
            ExerciseSegment.EXERCISE_SEGMENT_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING
        ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS -> ExerciseSegment.EXERCISE_SEGMENT_TYPE_OTHER_WORKOUT
        ExerciseSessionRecord.EXERCISE_TYPE_GYMNASTICS -> ExerciseSegment.EXERCISE_SEGMENT_TYPE_OTHER_WORKOUT
        ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT -> ExerciseSegment.EXERCISE_SEGMENT_TYPE_OTHER_WORKOUT
        ExerciseSessionRecord.EXERCISE_TYPE_PILATES -> ExerciseSegment.EXERCISE_SEGMENT_TYPE_PILATES
        ExerciseSessionRecord.EXERCISE_TYPE_ROWING_MACHINE -> ExerciseSegment.EXERCISE_SEGMENT_TYPE_ROWING_MACHINE
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING -> ExerciseSegment.EXERCISE_SEGMENT_TYPE_RUNNING
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL -> ExerciseSegment.EXERCISE_SEGMENT_TYPE_RUNNING_TREADMILL
        ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING -> ExerciseSegment.EXERCISE_SEGMENT_TYPE_STAIR_CLIMBING
        ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING_MACHINE -> ExerciseSegment.EXERCISE_SEGMENT_TYPE_STAIR_CLIMBING_MACHINE
        ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER -> ExerciseSegment.EXERCISE_SEGMENT_TYPE_SWIMMING_OPEN_WATER
        ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL -> ExerciseSegment.EXERCISE_SEGMENT_TYPE_SWIMMING_POOL
        ExerciseSessionRecord.EXERCISE_TYPE_WALKING -> ExerciseSegment.EXERCISE_SEGMENT_TYPE_WALKING
        ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING -> ExerciseSegment.EXERCISE_SEGMENT_TYPE_WEIGHTLIFTING
        ExerciseSessionRecord.EXERCISE_TYPE_WHEELCHAIR -> ExerciseSegment.EXERCISE_SEGMENT_TYPE_WHEELCHAIR
        ExerciseSessionRecord.EXERCISE_TYPE_YOGA -> ExerciseSegment.EXERCISE_SEGMENT_TYPE_YOGA
        else -> ExerciseSegment.EXERCISE_SEGMENT_TYPE_OTHER_WORKOUT
    }
