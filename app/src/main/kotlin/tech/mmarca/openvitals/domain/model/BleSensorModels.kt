package tech.mmarca.openvitals.domain.model

import java.time.Instant

/**
 * What a registered Bluetooth device is. A [SENSOR] streams live GATT values
 * and owns capabilities. A [WATCH] holds FIT files pulled over GFDI. A
 * [BIKE_COMPUTER] does both. File sync keys off [BleSensorDevice.isGarminGfdi],
 * the live role off a non-empty [BleSensorDevice.capabilities].
 */
enum class BleDeviceKind(
    /** Persisted form, so a rename cannot orphan stored devices. */
    val storageName: String,
) {
    SENSOR("SENSOR"),
    WATCH("WATCH"),
    BIKE_COMPUTER("BIKE_COMPUTER"),
    ;

    companion object {
        fun fromStorage(value: String): BleDeviceKind? =
            entries.firstOrNull { it.storageName == value }
    }
}

/**
 * Which integration owns a [BleDeviceKind.WATCH]. Garmin speaks GFDI; WearOS
 * is a live heart-rate source whose data arrives via Health Connect. Null
 * for a sensor, and for a Garmin watch stored before this field existed.
 */
enum class DeviceIntegration(val storageName: String) {
    GARMIN("GARMIN"),
    WEAROS("WEAROS"),
    ;

    companion object {
        fun fromStorage(value: String): DeviceIntegration? =
            entries.firstOrNull { it.storageName == value }
    }
}

enum class BleSensorCapability {
    HEART_RATE,
    CYCLING_CADENCE,
    CYCLING_POWER,
    CYCLING_SPEED_DISTANCE,
    RUNNING_SPEED_CADENCE,
}

enum class BleConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
}

data class BleSensorDevice(
    val id: String,
    val displayName: String,
    val address: String,
    val bluetoothName: String?,
    val capabilities: Set<BleSensorCapability>,
    val enabled: Boolean,
    val wheelCircumferenceMm: Int?,
    val batteryPercent: Int? = null,
    val batteryUpdatedAt: Instant? = null,
    val addedAt: Instant,
    /** Defaulted so devices stored before this field existed keep their meaning. */
    val kind: BleDeviceKind = BleDeviceKind.SENSOR,
    /** The owning integration for a watch. Null means legacy Garmin. */
    val integration: DeviceIntegration? = null,
    /** When recorded files were last pulled. Null for a sensor or a never-synced watch. */
    val lastSyncedAt: Instant? = null,
) {
    /** Literally a watch, not a bike computer, so an Edge never gets watch-only UI. */
    val isWatch: Boolean
        get() = kind == BleDeviceKind.WATCH

    /** A Garmin Edge: a GFDI sync device that is also a candidate live sensor. */
    val isBikeComputer: Boolean
        get() = kind == BleDeviceKind.BIKE_COMPUTER

    /** Driven over GFDI: a Garmin watch or bike computer, never WearOS. The file-sync concept. */
    val isGarminGfdi: Boolean
        get() = (kind == BleDeviceKind.WATCH || kind == BleDeviceKind.BIKE_COMPUTER) &&
            integration != DeviceIntegration.WEAROS

    /** A Garmin watch. For file-sync eligibility use [isGarminGfdi]. */
    val isGarminWatch: Boolean
        get() = isWatch && integration != DeviceIntegration.WEAROS

    /** A WearOS watch: live heart rate over BLE, recorded data via Health Connect. */
    val isWearosWatch: Boolean
        get() = isWatch && integration == DeviceIntegration.WEAROS

    /**
     * Can hold live capabilities and join a recording: a sensor, a bike
     * computer, or a watch the user also added through the Sensors path.
     * A sync-only watch registers with no capabilities.
     */
    val isLiveSensorCapable: Boolean
        get() = kind == BleDeviceKind.SENSOR ||
            kind == BleDeviceKind.BIKE_COMPUTER ||
            (kind == BleDeviceKind.WATCH && capabilities.isNotEmpty())

    fun normalized(): BleSensorDevice =
        copy(
            displayName = displayName.trim().ifBlank { bluetoothName.orEmpty().ifBlank { address } },
            wheelCircumferenceMm = wheelCircumferenceMm?.coerceIn(
                DefaultWheelCircumferenceMm,
                MaxWheelCircumferenceMm,
            ),
            batteryPercent = batteryPercent?.coerceIn(0, 100),
        )

    companion object {
        const val DefaultWheelCircumferenceMm = 2_100
        const val MaxWheelCircumferenceMm = 3_000
    }
}

data class BleDeviceConnectionStatus(
    val deviceId: String,
    val displayName: String,
    val address: String,
    val status: BleConnectionStatus,
    val capabilities: Set<BleSensorCapability>,
    val batteryPercent: Int? = null,
)

data class BleRecordingMetrics(
    val heartRateBpm: Long? = null,
    val cyclingCadenceRpm: Long? = null,
    val powerWatts: Double? = null,
    val cyclingSpeedMetersPerSecond: Double? = null,
    val runningSpeedMetersPerSecond: Double? = null,
    val runningCadenceRpm: Long? = null,
    val heartRateNoSignal: Boolean = false,
    val deviceStatuses: List<BleDeviceConnectionStatus> = emptyList(),
)

data class BleHeartRateSample(
    val time: Instant,
    val beatsPerMinute: Long,
)

data class BlePowerSample(
    val time: Instant,
    val watts: Double,
)

data class BleCyclingCadenceSample(
    val time: Instant,
    val rpm: Long,
)

data class BleSpeedSample(
    val time: Instant,
    val metersPerSecond: Double,
    val isRunning: Boolean,
)

data class BleStepsCadenceSample(
    val time: Instant,
    val stepsPerMinute: Long,
)

data class BleRecordingSampleBuffer(
    val heartRateSamples: List<BleHeartRateSample> = emptyList(),
    val powerSamples: List<BlePowerSample> = emptyList(),
    val cyclingCadenceSamples: List<BleCyclingCadenceSample> = emptyList(),
    val speedSamples: List<BleSpeedSample> = emptyList(),
    val stepsCadenceSamples: List<BleStepsCadenceSample> = emptyList(),
) {
    fun isEmpty(): Boolean =
        heartRateSamples.isEmpty() &&
            powerSamples.isEmpty() &&
            cyclingCadenceSamples.isEmpty() &&
            speedSamples.isEmpty() &&
            stepsCadenceSamples.isEmpty()

    /**
     * The span the samples cover, or null. The session must contain it:
     * Health Connect stacks out-of-bounds samples onto the closing instant.
     */
    fun firstSampleTime(): Instant? = sampleTimes().minOrNull()

    fun lastSampleTime(): Instant? = sampleTimes().maxOrNull()

    private fun sampleTimes(): List<Instant> =
        heartRateSamples.map { it.time } +
            powerSamples.map { it.time } +
            cyclingCadenceSamples.map { it.time } +
            speedSamples.map { it.time } +
            stepsCadenceSamples.map { it.time }

    fun averageHeartRateBpm(): Long? =
        heartRateSamples.takeIf { it.isNotEmpty() }?.map { it.beatsPerMinute }?.average()?.toLong()

    fun averagePowerWatts(): Double? =
        powerSamples.takeIf { it.isNotEmpty() }?.map { it.watts }?.average()

    fun withHeartRateSample(time: Instant, bpm: Long): BleRecordingSampleBuffer =
        copy(heartRateSamples = heartRateSamples + BleHeartRateSample(time, bpm))

    fun withPowerSample(time: Instant, watts: Double): BleRecordingSampleBuffer =
        copy(powerSamples = powerSamples + BlePowerSample(time, watts))

    fun withCyclingCadenceSample(time: Instant, rpm: Long): BleRecordingSampleBuffer =
        copy(cyclingCadenceSamples = cyclingCadenceSamples + BleCyclingCadenceSample(time, rpm))

    fun withSpeedSample(time: Instant, metersPerSecond: Double, isRunning: Boolean): BleRecordingSampleBuffer =
        copy(speedSamples = speedSamples + BleSpeedSample(time, metersPerSecond, isRunning))

    fun withStepsCadenceSample(time: Instant, stepsPerMinute: Long): BleRecordingSampleBuffer =
        copy(stepsCadenceSamples = stepsCadenceSamples + BleStepsCadenceSample(time, stepsPerMinute))

    fun trimmed(maxSamplesPerSeries: Int = MaxSamplesPerSeries): BleRecordingSampleBuffer =
        copy(
            heartRateSamples = heartRateSamples.takeLast(maxSamplesPerSeries),
            powerSamples = powerSamples.takeLast(maxSamplesPerSeries),
            cyclingCadenceSamples = cyclingCadenceSamples.takeLast(maxSamplesPerSeries),
            speedSamples = speedSamples.takeLast(maxSamplesPerSeries),
            stepsCadenceSamples = stepsCadenceSamples.takeLast(maxSamplesPerSeries),
        )

    companion object {
        // About 6 hours at 1 Hz; a safety cap when finishing a recording.
        const val MaxSamplesPerSeries = 21_600
    }
}

data class BleDiscoveredDevice(
    val address: String,
    val name: String?,
    val rssi: Int?,
    val suggestedCapabilities: Set<BleSensorCapability>,
    /**
     * The advertisement carried a member service a classifier recognised:
     * a file-sync watch to onboard. The advertised service, not a GATT UUID,
     * which no advertisement carries.
     */
    val advertisesSyncService: Boolean = false,
)
