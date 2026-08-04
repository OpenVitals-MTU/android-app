package tech.mmarca.openvitals.domain.model

import java.time.Instant

/**
 * What a registered Bluetooth device IS, which decides how the app talks to it.
 *
 * A [SENSOR] streams live values over standard GATT services while a recording
 * runs (heart-rate strap, power meter) and owns [BleSensorCapability]s. A
 * [WATCH] streams nothing: it holds recorded FIT files that are pulled over
 * GFDI on demand, so it carries no capabilities. A [BIKE_COMPUTER] (Garmin
 * Edge) does BOTH: it pulls recorded ride FIT files over GFDI like a watch AND
 * can broadcast live standard-GATT sensor values (heart rate, speed/cadence,
 * power) into a recording like a sensor. The two roles are independent —
 * file-sync keys off [BleSensorDevice.isGarminGfdi] (kind + integration), the
 * live role off a non-empty [BleSensorDevice.capabilities] — so a device can
 * hold either or both.
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
 * Which integration owns a [BleDeviceKind.WATCH]. A Garmin watch speaks GFDI
 * over BLE (FIT-file sync, settings tree, find). A WearOS watch (Galaxy,
 * Pixel, …) shares none of that protocol: it is a BLE-discoverable live
 * heart-rate source whose recorded data arrives through Health Connect, not a
 * FIT pull.
 *
 * Null for a plain sensor, and for a Garmin watch stored before this field
 * existed — [BleSensorDevice.isGarminWatch] treats a null-integration watch as
 * Garmin, the only watch integration that existed then.
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
     * Which integration owns this device when it is a [BleDeviceKind.WATCH].
     * Null for a sensor, and for a Garmin watch stored before this field
     * existed — [isGarminWatch] treats a null-integration watch as Garmin.
     */
    val integration: DeviceIntegration? = null,
    /**
     * When this device's recorded files were last pulled. Null for a watch
     * that has never synced, and always null for a [BleDeviceKind.SENSOR].
     */
    val lastSyncedAt: Instant? = null,
) {
    /**
     * Literally a watch — deliberately NOT a bike computer, so an Edge never
     * renders with watch-only UI (the avatar, the wellness "Data" view).
     */
    val isWatch: Boolean
        get() = kind == BleDeviceKind.WATCH

    /**
     * A Garmin Edge bike computer: a GFDI file-sync device (like a watch) that
     * is also a candidate live BLE sensor (unlike a watch).
     */
    val isBikeComputer: Boolean
        get() = kind == BleDeviceKind.BIKE_COMPUTER

    /**
     * A device the app drives over Garmin's GFDI protocol (FIT sync, settings,
     * find) — a watch OR a bike computer, but never a WearOS watch. This is the
     * file-sync eligibility concept; it depends on [kind] + [integration] and
     * is independent of [capabilities]. A null-integration watch is legacy
     * Garmin — the sole GFDI integration before WearOS.
     */
    val isGarminGfdi: Boolean
        get() = (kind == BleDeviceKind.WATCH || kind == BleDeviceKind.BIKE_COMPUTER) &&
            integration != DeviceIntegration.WEAROS

    /**
     * A watch the app drives over Garmin's GFDI protocol. Use where the UI
     * genuinely means "a watch"; for file-sync eligibility use [isGarminGfdi],
     * which also admits an Edge bike computer.
     */
    val isGarminWatch: Boolean
        get() = isWatch && integration != DeviceIntegration.WEAROS

    /**
     * A WearOS smartwatch (Galaxy, Pixel, …): a watch with no Garmin protocol —
     * live heart rate over BLE, recorded data via Health Connect.
     */
    val isWearosWatch: Boolean
        get() = isWatch && integration == DeviceIntegration.WEAROS

    /**
     * Can hold live [BleSensorCapability]s and take part in a recording: a
     * plain [BleDeviceKind.SENSOR], or a [BleDeviceKind.BIKE_COMPUTER]
     * broadcasting standard GATT. A watch cannot (scoped out for now). Gates
     * the Sensors-screen listing and capability UI.
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
    /**
     * The advertisement carried a member service that an integration's
     * `DeviceScanClassifier` recognised — the scanner's signal that this is a
     * file-sync watch to onboard rather than a live sensor. A single
     * integration (Garmin) claims these today; the per-integration verdict
     * lives in the classifier, so this generic model holds the evidence, not
     * the classification.
     *
     * Deliberately the ADVERTISED member service, not a GFDI/transport UUID:
     * those are GATT services, invisible until connected, so no advertisement
     * ever carries them.
     */
    val advertisesSyncService: Boolean = false,
)
