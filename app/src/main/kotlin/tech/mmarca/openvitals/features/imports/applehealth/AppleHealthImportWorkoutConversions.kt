package tech.mmarca.openvitals.features.imports.applehealth

import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseRoute
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.units.kilocalories
import androidx.health.connect.client.units.meters
import java.time.Duration
import kotlin.math.roundToLong
import tech.mmarca.openvitals.core.geo.haversineMeters

internal fun AppleHealthImportConverter.convertWorkouts(
    workouts: List<AppleWorkout>,
    overlapCandidates: List<AppleWorkoutOverlapCandidate>,
    overlapCandidateLimitReached: Boolean = false,
): List<ConvertedAppleRecord> =
    workouts.flatMap { workout ->
        val start = workout.startDate ?: return@flatMap emptyList<ConvertedAppleRecord>().also {
            invalid(workout.workoutActivityType, "Workout is missing startDate.", null)
        }
        val end = workout.endDate ?: return@flatMap emptyList<ConvertedAppleRecord>().also {
            invalid(workout.workoutActivityType, "Workout is missing endDate.", start.instant.toString())
        }
        val interval = interval(start, end)
        val fingerprint = buildStableClientRecordId("workout", workout.stableParts())
        val exerciseRoute = workout.toSynthesizedExerciseRoute(interval)
        if (workout.routes.isNotEmpty() && exerciseRoute == null) {
            skipped(
                appleType = workout.workoutActivityType,
                reasonCode = "route_skipped",
                detail = "Workout route file did not contain enough valid geometry points.",
                timeRange = AppleImportTimeRange(interval.start.instant, interval.end.instant).toString(),
            )
        }
        val session = ExerciseSessionRecord(
            startTime = interval.start.instant,
            startZoneOffset = interval.start.offset,
            endTime = interval.end.instant,
            endZoneOffset = interval.end.offset,
            metadata = appleMetadata("ExerciseSessionRecord", fingerprint),
            exerciseType = workout.workoutActivityType.toExerciseType(),
            title = workout.workoutActivityType.removePrefix("HKWorkoutActivityType").ifBlank { "Apple Health workout" },
            exerciseRoute = exerciseRoute,
        )
        val convertedSession = ConvertedAppleRecord(
            appleType = workout.workoutActivityType,
            targetType = "ExerciseSessionRecord",
            fingerprint = fingerprint,
            recordType = ExerciseSessionRecord::class,
            record = session,
            sourceTimeRange = AppleImportTimeRange(interval.start.instant, interval.end.instant),
            unit = workout.durationUnit,
            value = workout.duration?.toString(),
        )
        buildList {
            add(convertedSession)
            markConverted(workout.workoutActivityType)
            if (!overlapCandidateLimitReached && !overlapCandidates.hasOverlapping(workout, AppleDistanceTypes)) {
                workout.totalDistance
                    ?.toMeters(workout.totalDistanceUnit)
                    ?.takeIf { it > 0.0 }
                    ?.let { meters ->
                        val distanceFingerprint = buildStableClientRecordId("workout_distance", workout.stableParts() + "|distance")
                        val distanceRecord = ConvertedAppleRecord(
                            appleType = workout.workoutActivityType,
                            targetType = "DistanceRecord",
                            fingerprint = distanceFingerprint,
                            recordType = DistanceRecord::class,
                            record = DistanceRecord(
                                startTime = interval.start.instant,
                                startZoneOffset = interval.start.offset,
                                endTime = interval.end.instant,
                                endZoneOffset = interval.end.offset,
                                distance = meters.meters,
                                metadata = appleMetadata("DistanceRecord", distanceFingerprint),
                            ),
                            sourceTimeRange = AppleImportTimeRange(interval.start.instant, interval.end.instant),
                            unit = workout.totalDistanceUnit,
                            value = workout.totalDistance.toString(),
                        )
                        add(distanceRecord)
                        markConverted(workout.workoutActivityType)
                    }
            }
            if (!overlapCandidateLimitReached && !overlapCandidates.hasOverlapping(workout, setOf(AppleActiveEnergyBurned))) {
                workout.totalEnergyBurned
                    ?.toKilocalories(workout.totalEnergyBurnedUnit)
                    ?.takeIf { it > 0.0 }
                    ?.let { kcal ->
                        val energyFingerprint = buildStableClientRecordId("workout_active_calories", workout.stableParts() + "|energy")
                        val energyRecord = ConvertedAppleRecord(
                            appleType = workout.workoutActivityType,
                            targetType = "ActiveCaloriesBurnedRecord",
                            fingerprint = energyFingerprint,
                            recordType = ActiveCaloriesBurnedRecord::class,
                            record = ActiveCaloriesBurnedRecord(
                                startTime = interval.start.instant,
                                startZoneOffset = interval.start.offset,
                                endTime = interval.end.instant,
                                endZoneOffset = interval.end.offset,
                                energy = kcal.kilocalories,
                                metadata = appleMetadata("ActiveCaloriesBurnedRecord", energyFingerprint),
                            ),
                            sourceTimeRange = AppleImportTimeRange(interval.start.instant, interval.end.instant),
                            unit = workout.totalEnergyBurnedUnit,
                            value = workout.totalEnergyBurned.toString(),
                        )
                        add(energyRecord)
                        markConverted(workout.workoutActivityType)
                    }
            }
        }
    }

private fun AppleWorkout.toSynthesizedExerciseRoute(interval: AppleInterval): ExerciseRoute? {
    // routes are already deduplicated by path at parse time (AppleHealthImportParser.toWorkout).
    val points = routes
        .flatMap { it.points }
        .filter { it.latitude in -90.0..90.0 && it.longitude in -180.0..180.0 }
    if (points.size < MinWorkoutRoutePoints) return null

    val cumulativeDistances = points.runningRouteDistances()
    val totalDistanceMeters = cumulativeDistances.lastOrNull() ?: 0.0
    // Offsets are computed at millisecond resolution: Health Connect stores route location times
    // with millisecond precision, so sub-millisecond spacing collapses consecutive points to the
    // same timestamp and violates ExerciseRoute's strictly-increasing-time requirement when the
    // session is read back (breaking duplicate detection and session reads).
    val durationMillis = Duration.between(interval.start.instant, interval.end.instant)
        .toMillis()
        .coerceAtLeast(points.size.toLong() + 1L)
    val lastOffsetMillis = (durationMillis - 1L).coerceAtLeast(points.lastIndex.toLong())
    var previousOffsetMillis = -1L

    val locations = points.mapIndexed { index, point ->
        val progress = if (totalDistanceMeters > 0.0) {
            cumulativeDistances[index] / totalDistanceMeters
        } else {
            index.toDouble() / points.lastIndex.toDouble()
        }
        val requestedOffset = (lastOffsetMillis * progress).roundToLong()
        val minOffset = previousOffsetMillis + 1L
        val maxOffset = lastOffsetMillis - (points.lastIndex - index)
        val offset = requestedOffset.coerceIn(minOffset, maxOffset)
        previousOffsetMillis = offset
        ExerciseRoute.Location(
            time = interval.start.instant.plusMillis(offset),
            latitude = point.latitude,
            longitude = point.longitude,
            horizontalAccuracy = point.horizontalAccuracyMeters?.meters,
            verticalAccuracy = point.verticalAccuracyMeters?.meters,
            altitude = point.altitudeMeters?.meters,
        )
    }

    return ExerciseRoute(locations)
}

private fun List<AppleWorkoutRoutePoint>.runningRouteDistances(): List<Double> {
    var distance = 0.0
    return mapIndexed { index, point ->
        if (index > 0) {
            distance += this[index - 1].distanceMetersTo(point)
        }
        distance
    }
}

private fun AppleWorkoutRoutePoint.distanceMetersTo(other: AppleWorkoutRoutePoint): Double =
    haversineMeters(latitude, longitude, other.latitude, other.longitude)

private const val MinWorkoutRoutePoints = 2
