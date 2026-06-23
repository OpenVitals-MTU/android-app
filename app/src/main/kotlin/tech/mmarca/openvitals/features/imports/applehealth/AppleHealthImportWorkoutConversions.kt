package tech.mmarca.openvitals.features.imports.applehealth

import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.units.kilocalories
import androidx.health.connect.client.units.meters

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
        val session = ExerciseSessionRecord(
            startTime = interval.start.instant,
            startZoneOffset = interval.start.offset,
            endTime = interval.end.instant,
            endZoneOffset = interval.end.offset,
            metadata = appleMetadata("ExerciseSessionRecord", fingerprint),
            exerciseType = workout.workoutActivityType.toExerciseType(),
            title = workout.workoutActivityType.removePrefix("HKWorkoutActivityType").ifBlank { "Apple Health workout" },
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
        markConverted(workout.workoutActivityType)
        buildList {
            add(convertedSession)
            if (!overlapCandidateLimitReached && !overlapCandidates.hasOverlapping(workout, AppleDistanceTypes)) {
                workout.totalDistance
                    ?.toMeters(workout.totalDistanceUnit)
                    ?.takeIf { it > 0.0 }
                    ?.let { meters ->
                        val distanceFingerprint = buildStableClientRecordId("workout_distance", workout.stableParts() + "|distance")
                        add(
                            ConvertedAppleRecord(
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
                            ),
                        )
                    }
            }
            if (!overlapCandidateLimitReached && !overlapCandidates.hasOverlapping(workout, setOf(AppleActiveEnergyBurned))) {
                workout.totalEnergyBurned
                    ?.toKilocalories(workout.totalEnergyBurnedUnit)
                    ?.takeIf { it > 0.0 }
                    ?.let { kcal ->
                        val energyFingerprint = buildStableClientRecordId("workout_active_calories", workout.stableParts() + "|energy")
                        add(
                            ConvertedAppleRecord(
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
                            ),
                        )
                    }
            }
        }
    }
