package tech.mmarca.openvitals.domain.model

import java.time.Instant

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
    val addedAt: Instant,
) {
    fun normalized(): BleSensorDevice =
        copy(
            displayName = displayName.trim().ifBlank { bluetoothName.orEmpty().ifBlank { address } },
            wheelCircumferenceMm = wheelCircumferenceMm?.coerceIn(
                DefaultWheelCircumferenceMm,
                MaxWheelCircumferenceMm,
            ),
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
