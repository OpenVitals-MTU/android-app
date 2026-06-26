package tech.mmarca.openvitals.sensors.ble.aggregators

import java.time.Duration
import java.time.Instant
import tech.mmarca.openvitals.sensors.ble.BleUintUtils
import tech.mmarca.openvitals.sensors.ble.parsers.BleCrankData
import tech.mmarca.openvitals.sensors.ble.parsers.BleCyclingPowerData
import tech.mmarca.openvitals.sensors.ble.parsers.BleRunningSpeedCadenceData
import tech.mmarca.openvitals.sensors.ble.parsers.BleWheelData

internal data class BleAggregatedSample<T>(
    val value: T,
    val receivedAt: Instant,
)

internal abstract class BleSampleAggregator<Input, Output> {
    private var previous: Pair<Instant, Input>? = null
    protected var output: Output? = null
    private var lastReceivedAt: Instant? = null

    protected abstract fun computeValue(now: Instant, current: Input)

    fun add(now: Instant, current: Input) {
        computeValue(now, current)
        previous = now to current
        lastReceivedAt = now
    }

    fun current(now: Instant = Instant.now()): Output? {
        val receivedAt = lastReceivedAt ?: return null
        if (Duration.between(receivedAt, now) > MaxAge) {
            output = null
        }
        return output
    }

    fun reset() {
        previous = null
        output = null
        lastReceivedAt = null
    }

    protected fun previousValue(): Input? = previous?.second

    protected fun previousTime(): Instant? = previous?.first

    companion object {
        private val MaxAge = Duration.ofSeconds(5)
    }
}

internal class BleHeartRateAggregator : BleSampleAggregator<Long, Long>() {
    override fun computeValue(now: Instant, current: Long) {
        output = current
    }
}

internal class BlePowerAggregator : BleSampleAggregator<BleCyclingPowerData, Double>() {
    override fun computeValue(now: Instant, current: BleCyclingPowerData) {
        output = current.powerWatts.toDouble()
    }
}

internal class BleCyclingCadenceAggregator : BleSampleAggregator<BleCrankData, Long>() {
    override fun computeValue(now: Instant, current: BleCrankData) {
        val previous = previousValue() ?: return
        val timeDiffMs = BleUintUtils.diff(
            current.crankRevolutionsTime.toLong(),
            previous.crankRevolutionsTime.toLong(),
            BleUintUtils.UINT16_MAX.toLong(),
        ) / 1024.0 * 1000.0
        if (timeDiffMs <= 0.0) return
        if (current.crankRevolutionsCount < previous.crankRevolutionsCount) return
        val crankDiff = BleUintUtils.diff(
            current.crankRevolutionsCount,
            previous.crankRevolutionsCount,
            BleUintUtils.UINT32_MAX,
        )
        output = (crankDiff / (timeDiffMs / 60_000.0)).toLong().coerceAtLeast(0L)
    }
}

internal class BleCyclingSpeedAggregator(
    private var wheelCircumferenceMeters: Double,
) : BleSampleAggregator<BleWheelData, Double>() {
    fun setWheelCircumferenceMeters(value: Double) {
        wheelCircumferenceMeters = value
    }

    override fun computeValue(now: Instant, current: BleWheelData) {
        val previous = previousValue() ?: return
        val timeDiffMs = BleUintUtils.diff(
            current.wheelRevolutionsTime.toLong(),
            previous.wheelRevolutionsTime.toLong(),
            BleUintUtils.UINT16_MAX.toLong(),
        ) / 1024.0 * 1000.0
        if (timeDiffMs <= 0.0) return
        if (current.wheelRevolutionsCount < previous.wheelRevolutionsCount) return
        val wheelDiff = BleUintUtils.diff(
            current.wheelRevolutionsCount,
            previous.wheelRevolutionsCount,
            BleUintUtils.UINT32_MAX,
        )
        output = wheelCircumferenceMeters * wheelDiff / (timeDiffMs / 1000.0)
    }
}

internal class BleRunningSpeedCadenceAggregator :
    BleSampleAggregator<BleRunningSpeedCadenceData, Pair<Double?, Long?>>() {
    override fun computeValue(now: Instant, current: BleRunningSpeedCadenceData) {
        output = current.speedMetersPerSecond to current.cadenceRpm
    }
}
