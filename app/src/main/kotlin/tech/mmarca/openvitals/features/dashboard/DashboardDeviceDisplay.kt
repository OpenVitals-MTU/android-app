package tech.mmarca.openvitals.features.dashboard

import java.time.Instant
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.devices.garmin.GarminRealtimeState
import tech.mmarca.openvitals.domain.insights.MetricDailyGoalKey
import tech.mmarca.openvitals.domain.model.BleConnectionStatus
import tech.mmarca.openvitals.domain.model.BleDeviceConnectionStatus
import tech.mmarca.openvitals.domain.model.BleSensorDevice

/** The watch tile: the most recently synced watch, others behind a count. Bike computers excluded. */
internal fun List<BleSensorDevice>.toWatchWidgetDisplay(
    syncingDeviceId: String? = null,
    live: GarminRealtimeState = GarminRealtimeState(),
    now: Instant = Instant.now(),
): WatchWidgetDisplay? {
    val watches = filter { it.isWatch }
    // The watch being synced owns the tile; failing that, the most recently synced.
    val primary = watches.firstOrNull { it.id == syncingDeviceId }
        ?: watches.maxByOrNull { it.lastSyncedAt?.toEpochMilli() ?: Long.MIN_VALUE }
        ?: return null
    return WatchWidgetDisplay(
        deviceId = primary.id,
        name = primary.displayName,
        batteryPercent = primary.batteryPercent,
        lastSyncedAt = primary.lastSyncedAt,
        additionalCount = watches.size - 1,
        isSyncing = primary.id == syncingDeviceId,
        // Only one watch can hold a link, so the live values belong to it or nothing.
        liveHeartRateBpm = live.freshHeartRate(now),
        liveSteps = live.freshSteps(now),
    )
}

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
