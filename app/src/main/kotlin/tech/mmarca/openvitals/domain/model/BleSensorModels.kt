package tech.mmarca.openvitals.domain.model

import java.time.Instant

/**
 * What a registered Bluetooth device IS, which decides how the app talks to it.
 *
 * A [SENSOR] streams live values over standard GATT services while a recording
 * runs (heart-rate strap, power meter) and owns [BleSensorCapability]s. A
 * [BIKE_COMPUTER] (Garmin Edge) can broadcast the same live standard-GATT
 * sensor values (heart rate, speed/cadence, power) into a recording, so it is
 * live-capable too.
 *
 * [WATCH] exists only as **parsing tolerance**: the app no longer links to
 * watches (watch data arrives through Health Connect, e.g. via Gadgetbridge),
 * but registries written by the retired watch integration — including the
 * Flutter-era JSON the data migrator copies over verbatim — still contain
 * `"kind": "WATCH"` entries. Those must decode without crashing and are
 * ignored everywhere live sensors are listed or connected (see
 * [BleSensorDevice.isLiveSensorCapable]); they are never created anew.
 */
enum class BleDeviceKind(
    /**
     * Persisted form, so renaming the Kotlin identifier can't orphan stored
     * devices. The Flutter build wrote these exact strings to the registry.
     */
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
 * Which retired watch integration wrote a stored [BleDeviceKind.WATCH] entry.
 *
 * Parsing tolerance only, like [BleDeviceKind.WATCH] itself: the app no longer
 * drives any watch protocol, but stored registries (and the migrated Flutter
 * JSON) carry these values and must keep decoding losslessly. Null for a plain
 * sensor, and for a Garmin watch stored before this field existed.
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
    /**
     * Defaulted rather than required so every existing call site — and every
     * device already in storage, written before this field existed — keeps
     * meaning what it meant.
     */
    val kind: BleDeviceKind = BleDeviceKind.SENSOR,
    /**
     * Which retired watch integration wrote this entry when it is a
     * [BleDeviceKind.WATCH]. Preserved for lossless storage round-trips only;
     * null for a sensor.
     */
    val integration: DeviceIntegration? = null,
    /**
     * When the retired watch integration last pulled this device's recorded
     * files. Preserved for lossless storage round-trips only; always null for
     * a [BleDeviceKind.SENSOR].
     */
    val lastSyncedAt: Instant? = null,
) {
    /**
     * Can hold live [BleSensorCapability]s and take part in a recording: a
     * plain [BleDeviceKind.SENSOR], or a [BleDeviceKind.BIKE_COMPUTER]
     * broadcasting standard GATT. A stored watch-era [BleDeviceKind.WATCH]
     * entry cannot — the app no longer talks to watches — so this gates the
     * Sensors-screen listing and the recording coordinator alike.
     */
    val isLiveSensorCapable: Boolean
        get() = kind == BleDeviceKind.SENSOR || kind == BleDeviceKind.BIKE_COMPUTER

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
     * The span the recorded samples actually cover, or null when there are
     * none. The session written to Health Connect has to CONTAIN this: Health
     * Connect clamps a sample that falls outside its session into the bounds,
     * so a session that ends even a second early does not drop the samples past
     * its end — it stacks every one of them onto the closing instant, which is
     * worse than losing them.
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
        // ~6 hours at 1 Hz; applied only when finishing a recording as a safety cap.
        const val MaxSamplesPerSeries = 21_600
    }
}

data class BleDiscoveredDevice(
    val address: String,
    val name: String?,
    val rssi: Int?,
    val suggestedCapabilities: Set<BleSensorCapability>,
)
