package tech.mmarca.openvitals.healthconnect

import android.util.Log
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ElevationGainedRecord
import androidx.health.connect.client.records.ExerciseRoute
import androidx.health.connect.client.records.ExerciseSegment
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.kilocalories
import androidx.health.connect.client.units.meters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tech.mmarca.openvitals.data.model.ActivityPauseInterval
import tech.mmarca.openvitals.data.model.ActivityProgressPoint
import tech.mmarca.openvitals.data.model.ActivityWriteRequest
import tech.mmarca.openvitals.data.model.DailySteps
import tech.mmarca.openvitals.data.model.ExerciseData
import tech.mmarca.openvitals.data.model.ExerciseRoutePoint
import tech.mmarca.openvitals.data.model.StepProgressPoint
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

internal class ActivityHealthReader(
    private val support: HealthConnectReaderSupport,
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
        includeFloors: Boolean = false,
        includeActiveCalories: Boolean = false,
        includeElevation: Boolean = false,
    ): List<DailySteps> {
        val zone = ZoneId.systemDefault()
        val start = startDate.atStartOfDay(zone).toInstant()
        val end = endDate.plusDays(1).atStartOfDay(zone).toInstant()
        return support.withLogging("readDailySteps[$start..$end]", emptyList()) {
            val metrics = buildSet {
                add(StepsRecord.COUNT_TOTAL)
                add(DistanceRecord.DISTANCE_TOTAL)
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
                    steps = bucket.result[StepsRecord.COUNT_TOTAL] ?: 0L,
                    distanceMeters = bucket.result[DistanceRecord.DISTANCE_TOTAL]?.inMeters ?: 0.0,
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

    suspend fun readStepProgress(date: LocalDate): List<StepProgressPoint> {
        val (start, end) = support.dayRange(date)
        return support.withLogging("readStepProgress[$date][$start..$end]", emptyList()) {
            var runningTotal = 0L
            support.client().readRecordsPaged(
                recordType = StepsRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = true,
            ).map { record ->
                runningTotal += record.count
                StepProgressPoint(
                    time = record.endTime,
                    totalSteps = runningTotal,
                )
            }
        }
    }

    suspend fun readActivityProgress(
        date: LocalDate,
        includeDistance: Boolean,
        includeCalories: Boolean,
        includeActiveCalories: Boolean,
        includeFloors: Boolean,
        includeElevation: Boolean,
    ): List<ActivityProgressPoint> {
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant()
        val end = if (date == LocalDate.now()) Instant.now() else date.plusDays(1).atStartOfDay(zone).toInstant()
        val metrics = buildSet {
            add(StepsRecord.COUNT_TOTAL)
            if (includeDistance) add(DistanceRecord.DISTANCE_TOTAL)
            if (includeCalories) add(TotalCaloriesBurnedRecord.ENERGY_TOTAL)
            if (includeActiveCalories) add(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL)
            if (includeFloors) add(FloorsClimbedRecord.FLOORS_CLIMBED_TOTAL)
            if (includeElevation) add(ElevationGainedRecord.ELEVATION_GAINED_TOTAL)
        }
        return support.withLogging("readActivityProgress[$date][$start..$end]", emptyList()) {
            var cumulativeSteps = 0L
            var cumulativeDistance = 0.0
            var cumulativeCalories = 0.0
            var cumulativeActiveCalories = 0.0
            var cumulativeFloors = 0
            var cumulativeElevation = 0.0

            support.client().aggregateGroupByDuration(
                AggregateGroupByDurationRequest(
                    metrics = metrics,
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    timeRangeSlicer = Duration.ofHours(1),
                )
            ).map { bucket ->
                cumulativeSteps += bucket.result[StepsRecord.COUNT_TOTAL] ?: 0L
                if (includeDistance) {
                    cumulativeDistance += bucket.result[DistanceRecord.DISTANCE_TOTAL]?.inMeters ?: 0.0
                }
                if (includeCalories) {
                    cumulativeCalories += bucket.result[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories ?: 0.0
                }
                if (includeActiveCalories) {
                    cumulativeActiveCalories += bucket.result[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories ?: 0.0
                }
                if (includeFloors) {
                    cumulativeFloors += bucket.result[FloorsClimbedRecord.FLOORS_CLIMBED_TOTAL]?.toInt() ?: 0
                }
                if (includeElevation) {
                    cumulativeElevation += bucket.result[ElevationGainedRecord.ELEVATION_GAINED_TOTAL]?.inMeters ?: 0.0
                }
                ActivityProgressPoint(
                    time = bucket.endTime,
                    totalSteps = cumulativeSteps,
                    totalDistanceMeters = if (includeDistance) cumulativeDistance else null,
                    totalCaloriesBurnedKcal = if (includeCalories) cumulativeCalories else null,
                    totalActiveCaloriesKcal = if (includeActiveCalories) cumulativeActiveCalories else null,
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

    suspend fun readCaloriesKcal(date: LocalDate): Double? {
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant()
        return support.withNullableLogging("readCaloriesKcal[$date][$start..$end]") {
            support.client().aggregate(
                AggregateRequest(
                    metrics = setOf(TotalCaloriesBurnedRecord.ENERGY_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                )
            )[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories
        }
    }

    suspend fun readTodayCaloriesKcal(): Double? = readCaloriesKcal(LocalDate.now())

    suspend fun readLatestWorkout(date: LocalDate): ExerciseData? {
        val (start, end) = support.dayRange(date)
        return support.withNullableLogging("readLatestWorkout[$date][$start..$end]") {
            support.client().readRecordsPaged(
                recordType = ExerciseSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = false,
                pageSize = 1,
                maxRecords = 1,
            ).firstOrNull()?.toExerciseData()
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
            ).firstOrNull()?.toExerciseData()
        }

    suspend fun readExerciseSessions(start: Instant, end: Instant): List<ExerciseData> =
        support.withLogging("readExerciseSessions[$start..$end]", emptyList()) {
            support.client().readRecordsPaged(
                recordType = ExerciseSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = false,
                pageSize = 50,
            ).map { it.toExerciseData() }
        }

    suspend fun readExerciseSession(
        id: String,
        includeSteps: Boolean,
        includeDistance: Boolean,
        includeTotalCalories: Boolean,
        includeActiveCalories: Boolean,
        includeFloors: Boolean,
        includeElevation: Boolean,
    ): ExerciseData? =
        support.withNullableLogging("readExerciseSession[$id]") {
            val record = support.client().readRecord(ExerciseSessionRecord::class, id).record
            val metrics = buildSet {
                if (includeSteps) add(StepsRecord.COUNT_TOTAL)
                if (includeDistance) add(DistanceRecord.DISTANCE_TOTAL)
                if (includeTotalCalories) add(TotalCaloriesBurnedRecord.ENERGY_TOTAL)
                if (includeActiveCalories) add(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL)
                if (includeFloors) add(FloorsClimbedRecord.FLOORS_CLIMBED_TOTAL)
                if (includeElevation) add(ElevationGainedRecord.ELEVATION_GAINED_TOTAL)
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
                    Log.e(TAG, "Failed readExerciseSession aggregate id=$id ${support.diagnosticsSummary()}", it)
                }.getOrNull()
            }

            record.toExerciseData(
                steps = if (includeSteps && aggregate != null) aggregate[StepsRecord.COUNT_TOTAL] ?: 0L else null,
                totalDistanceMeters = if (includeDistance && aggregate != null) {
                    aggregate[DistanceRecord.DISTANCE_TOTAL]?.inMeters ?: 0.0
                } else {
                    null
                },
                totalCaloriesKcal = if (includeTotalCalories && aggregate != null) {
                    aggregate[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories ?: 0.0
                } else {
                    null
                },
                activeCaloriesKcal = if (includeActiveCalories && aggregate != null) {
                    aggregate[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories ?: 0.0
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
            )
        }

    suspend fun writeActivityEntry(request: ActivityWriteRequest): String = withContext(Dispatchers.IO) {
        validateActivityWriteRequest(request)

        val zone = ZoneId.systemDefault()
        val sessionClientRecordId = "openvitals_activity_${request.startTime.toEpochMilli()}_${UUID.randomUUID()}"
        val sessionMetadata = Metadata.manualEntry(
            clientRecordId = sessionClientRecordId,
            device = Device(type = Device.TYPE_PHONE),
        )
        val startOffset = zone.rules.getOffset(request.startTime)
        val endOffset = zone.rules.getOffset(request.endTime)
        val exerciseSegments = request.toExerciseSegments()
        val session = ExerciseSessionRecord(
            startTime = request.startTime,
            startZoneOffset = startOffset,
            endTime = request.endTime,
            endZoneOffset = endOffset,
            metadata = sessionMetadata,
            exerciseType = request.exerciseType,
            title = request.title?.trim()?.takeIf { it.isNotBlank() },
            notes = request.notes?.trim()?.takeIf { it.isNotBlank() },
            segments = exerciseSegments,
            exerciseRoute = request.routePoints.toExerciseRouteOrNull(),
        )

        val extraRecords = buildList<Record> {
            request.distanceMeters?.let { meters ->
                add(
                    DistanceRecord(
                        startTime = request.startTime,
                        startZoneOffset = startOffset,
                        endTime = request.endTime,
                        endZoneOffset = endOffset,
                        distance = meters.meters,
                        metadata = manualActivityMetricMetadata("distance", request.startTime),
                    )
                )
            }
            request.elevationGainedMeters?.let { meters ->
                add(
                    ElevationGainedRecord(
                        startTime = request.startTime,
                        startZoneOffset = startOffset,
                        endTime = request.endTime,
                        endZoneOffset = endOffset,
                        elevation = meters.meters,
                        metadata = manualActivityMetricMetadata("elevation", request.startTime),
                    )
                )
            }
            request.activeCaloriesKcal?.let { kcal ->
                add(
                    ActiveCaloriesBurnedRecord(
                        startTime = request.startTime,
                        startZoneOffset = startOffset,
                        endTime = request.endTime,
                        endZoneOffset = endOffset,
                        energy = kcal.kilocalories,
                        metadata = manualActivityMetricMetadata("active_calories", request.startTime),
                    )
                )
            }
            request.totalCaloriesKcal?.let { kcal ->
                add(
                    TotalCaloriesBurnedRecord(
                        startTime = request.startTime,
                        startZoneOffset = startOffset,
                        endTime = request.endTime,
                        endZoneOffset = endOffset,
                        energy = kcal.kilocalories,
                        metadata = manualActivityMetricMetadata("total_calories", request.startTime),
                    )
                )
            }
        }

        Log.d(
            TAG,
            "Writing activity entry type=${request.exerciseType} " +
                "routePoints=${request.routePoints.size} pauses=${request.pauseIntervals.size} " +
                "segments=${exerciseSegments.size} extras=${extraRecords.size} ${support.diagnosticsSummary()}",
        )
        support.client().insertRecords(listOf(session) + extraRecords)
        sessionClientRecordId
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
    }
}

internal fun ActivityWriteRequest.toExerciseSegments(): List<ExerciseSegment> {
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

private fun Int.toActiveExerciseSegmentType(): Int =
    when (this) {
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING -> ExerciseSegment.EXERCISE_SEGMENT_TYPE_BIKING
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY -> ExerciseSegment.EXERCISE_SEGMENT_TYPE_BIKING_STATIONARY
        ExerciseSessionRecord.EXERCISE_TYPE_ELLIPTICAL -> ExerciseSegment.EXERCISE_SEGMENT_TYPE_ELLIPTICAL
        ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING ->
            ExerciseSegment.EXERCISE_SEGMENT_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING
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
