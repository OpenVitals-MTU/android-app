package tech.mmarca.openvitals.features.dashboard

import java.time.Instant
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.domain.insights.MetricDailyGoalKey
import tech.mmarca.openvitals.domain.model.BleConnectionStatus
import tech.mmarca.openvitals.domain.model.BleDeviceConnectionStatus
import tech.mmarca.openvitals.domain.model.BleSensorDevice

/**
 * The paired BLE devices the Sensors & devices screen lists. Watches are
 * excluded: they live under Settings and skewed the lowest-battery figure.
 */
internal fun List<BleSensorDevice>.toDashboardSensorStatus(
    connectionStatuses: List<BleDeviceConnectionStatus>,
): DashboardSensorStatus {
    val statusesById = connectionStatuses.associateBy { it.deviceId }
    val statusesByAddress = connectionStatuses.associateBy { it.address }
    return DashboardSensorStatus(
        devices = filter { it.isLiveSensorCapable }.map { device ->
            val liveStatus = statusesById[device.id] ?: statusesByAddress[device.address]
            DashboardSensorDeviceStatus(
                id = device.id,
                displayName = device.displayName,
                enabled = device.enabled,
                connectionStatus = liveStatus?.status ?: BleConnectionStatus.DISCONNECTED,
                batteryPercent = liveStatus?.batteryPercent ?: device.batteryPercent,
            )
        },
    )
}

internal fun PreferencesRepository.dashboardDailyGoals(): DashboardDailyGoals =
    DashboardDailyGoals(
        steps = dailyGoalFor(MetricDailyGoalKey.STEPS),
        distanceMeters = dailyGoalFor(MetricDailyGoalKey.DISTANCE_METERS),
        caloriesOutKcal = dailyGoalFor(MetricDailyGoalKey.CALORIES_OUT_KCAL),
        activeCaloriesKcal = dailyGoalFor(MetricDailyGoalKey.ACTIVE_CALORIES_KCAL),
        floors = dailyGoalFor(MetricDailyGoalKey.FLOORS),
        elevationMeters = dailyGoalFor(MetricDailyGoalKey.ELEVATION_METERS),
        wheelchairPushes = dailyGoalFor(MetricDailyGoalKey.WHEELCHAIR_PUSHES),
        sleepHours = dailyGoalFor(MetricDailyGoalKey.SLEEP_HOURS),
        hydrationLiters = hydrationDailyGoalLiters,
        caloriesInKcal = dailyGoalFor(MetricDailyGoalKey.CALORIES_IN_KCAL),
        proteinGrams = dailyGoalFor(MetricDailyGoalKey.PROTEIN_GRAMS),
        carbsGrams = dailyGoalFor(MetricDailyGoalKey.CARBS_GRAMS),
        fatGrams = dailyGoalFor(MetricDailyGoalKey.FAT_GRAMS),
        mindfulnessMinutes = dailyGoalFor(MetricDailyGoalKey.MINDFULNESS_MINUTES),
    )
