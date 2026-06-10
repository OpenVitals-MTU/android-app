package tech.mmarca.openvitals.healthconnect

import androidx.health.connect.client.records.ExerciseRouteResult
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.MindfulnessSessionRecord
import androidx.health.connect.client.records.SleepSessionRecord
import tech.mmarca.openvitals.domain.model.CaloriesBurnedSource
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.domain.model.ExerciseDeviceData
import tech.mmarca.openvitals.domain.model.ExerciseLapData
import tech.mmarca.openvitals.domain.model.ExerciseRouteData
import tech.mmarca.openvitals.domain.model.ExerciseRoutePoint
import tech.mmarca.openvitals.domain.model.ExerciseRouteStatus
import tech.mmarca.openvitals.domain.model.ExerciseSegmentData
import tech.mmarca.openvitals.domain.model.MindfulnessSession
import tech.mmarca.openvitals.domain.model.SleepData
import tech.mmarca.openvitals.domain.model.SleepDeviceData
import tech.mmarca.openvitals.domain.model.SleepStage
import tech.mmarca.openvitals.domain.model.sleepDurationMsFromStages

internal fun ExerciseSessionRecord.toExerciseData(
    steps: Long? = null,
    totalDistanceMeters: Double? = null,
    totalCaloriesKcal: Double? = null,
    totalCaloriesSource: CaloriesBurnedSource = if (totalCaloriesKcal != null) {
        CaloriesBurnedSource.RECORDED_TOTAL
    } else {
        CaloriesBurnedSource.NO_DATA
    },
    activeCaloriesKcal: Double? = null,
    floorsClimbed: Int? = null,
    elevationGainedMeters: Double? = null,
    wheelchairPushes: Long? = null,
    averageSpeedMetersPerSecond: Double? = null,
    averagePowerWatts: Double? = null,
    averageStepsCadenceRate: Double? = null,
    averageCyclingCadenceRpm: Double? = null,
    appPackageName: String? = null,
) = ExerciseData(
    id = metadata.id,
    title = title,
    exerciseType = exerciseType,
    startTime = startTime,
    endTime = endTime,
    durationMs = endTime.toEpochMilli() - startTime.toEpochMilli(),
    source = metadata.dataOrigin.packageName,
    totalDistanceMeters = totalDistanceMeters,
    totalCaloriesKcal = totalCaloriesKcal,
    totalCaloriesSource = totalCaloriesSource,
    activeCaloriesKcal = activeCaloriesKcal,
    steps = steps,
    wheelchairPushes = wheelchairPushes,
    averageSpeedMetersPerSecond = averageSpeedMetersPerSecond,
    averagePowerWatts = averagePowerWatts,
    averageStepsCadenceRate = averageStepsCadenceRate,
    averageCyclingCadenceRpm = averageCyclingCadenceRpm,
    floorsClimbed = floorsClimbed,
    elevationGainedMeters = elevationGainedMeters,
    notes = notes,
    startZoneOffset = startZoneOffset,
    endZoneOffset = endZoneOffset,
    lastModifiedTime = metadata.lastModifiedTime,
    clientRecordId = metadata.clientRecordId,
    clientRecordVersion = metadata.clientRecordVersion,
    recordingMethod = metadata.recordingMethod,
    device = metadata.device?.let { device ->
        ExerciseDeviceData(
            type = device.type,
            manufacturer = device.manufacturer,
            model = device.model,
        )
    },
    plannedExerciseSessionId = plannedExerciseSessionId,
    segments = segments.map { segment ->
        ExerciseSegmentData(
            startTime = segment.startTime,
            endTime = segment.endTime,
            segmentType = segment.segmentType,
            repetitions = segment.repetitions,
            setIndex = segment.setIndex,
        )
    },
    laps = laps.map { lap ->
        ExerciseLapData(
            startTime = lap.startTime,
            endTime = lap.endTime,
            lengthMeters = lap.length?.inMeters,
        )
    },
    route = exerciseRouteResult.toExerciseRouteData(),
    isOpenVitalsEntry = appPackageName?.let { isOpenVitalsRecord(metadata.dataOrigin.packageName, it) } ?: false,
)

private fun ExerciseRouteResult.toExerciseRouteData(): ExerciseRouteData = when (this) {
    is ExerciseRouteResult.Data -> ExerciseRouteData(
        status = ExerciseRouteStatus.DATA,
        points = exerciseRoute.route.map { point ->
            ExerciseRoutePoint(
                time = point.time,
                latitude = point.latitude,
                longitude = point.longitude,
                altitudeMeters = point.altitude?.inMeters,
                horizontalAccuracyMeters = point.horizontalAccuracy?.inMeters,
                verticalAccuracyMeters = point.verticalAccuracy?.inMeters,
            )
        },
    )
    is ExerciseRouteResult.ConsentRequired -> ExerciseRouteData(status = ExerciseRouteStatus.CONSENT_REQUIRED)
    is ExerciseRouteResult.NoData -> ExerciseRouteData(status = ExerciseRouteStatus.NO_DATA)
    else -> ExerciseRouteData(status = ExerciseRouteStatus.NO_DATA)
}

internal fun SleepSessionRecord.toSleepData(): SleepData {
    val mappedStages = stages.map { stage ->
        SleepStage(
            startTime = stage.startTime,
            endTime = stage.endTime,
            stageType = stage.stage,
        )
    }
    val spanDurationMs = endTime.toEpochMilli() - startTime.toEpochMilli()

    return SleepData(
        id = metadata.id,
        title = title,
        startTime = startTime,
        endTime = endTime,
        durationMs = sleepDurationMsFromStages(mappedStages, spanDurationMs),
        source = metadata.dataOrigin.packageName,
        notes = notes,
        startZoneOffset = startZoneOffset,
        endZoneOffset = endZoneOffset,
        lastModifiedTime = metadata.lastModifiedTime,
        clientRecordId = metadata.clientRecordId,
        clientRecordVersion = metadata.clientRecordVersion,
        recordingMethod = metadata.recordingMethod,
        device = metadata.device?.let { device ->
            SleepDeviceData(
                type = device.type,
                manufacturer = device.manufacturer,
                model = device.model,
            )
        },
        stages = mappedStages,
    )
}

internal fun MindfulnessSessionRecord.toMindfulnessSession(appPackageName: String? = null) = MindfulnessSession(
    id = metadata.id,
    title = title,
    startTime = startTime,
    endTime = endTime,
    durationMs = endTime.toEpochMilli() - startTime.toEpochMilli(),
    source = metadata.dataOrigin.packageName,
    isOpenVitalsEntry = appPackageName?.let { isOpenVitalsRecord(metadata.dataOrigin.packageName, it) } ?: false,
)
