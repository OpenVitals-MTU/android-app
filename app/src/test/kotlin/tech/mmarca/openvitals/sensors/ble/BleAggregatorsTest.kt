package tech.mmarca.openvitals.sensors.ble.aggregators

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import tech.mmarca.openvitals.sensors.ble.parsers.BleCrankData
import tech.mmarca.openvitals.sensors.ble.parsers.BleCyclingPowerData
import tech.mmarca.openvitals.sensors.ble.parsers.BleRunningSpeedCadenceData
import tech.mmarca.openvitals.sensors.ble.parsers.BleWheelData

class BleAggregatorsTest {
    @Test
    fun heartRateAggregator_returnsLatestValue() {
        val aggregator = BleHeartRateAggregator()
        val now = Instant.parse("2024-01-01T12:00:00Z")
        aggregator.add(now, 120L)
        assertEquals(120L, aggregator.current(now))
    }

    @Test
    fun cyclingCadenceAggregator_computesRpmFromCrankDelta() {
        val aggregator = BleCyclingCadenceAggregator()
        val t0 = Instant.parse("2024-01-01T12:00:00Z")
        val t1 = Instant.parse("2024-01-01T12:00:01Z")
        aggregator.add(t0, BleCrankData(crankRevolutionsCount = 10, crankRevolutionsTime = 0))
        aggregator.add(t1, BleCrankData(crankRevolutionsCount = 11, crankRevolutionsTime = 1024))
        assertEquals(60L, aggregator.current(t1))
    }

    @Test
    fun cyclingCadenceAggregator_returnsZeroWhenCrankStops() {
        val aggregator = BleCyclingCadenceAggregator()
        val t0 = Instant.parse("2024-01-01T12:00:00Z")
        val t1 = Instant.parse("2024-01-01T12:00:01Z")
        val t2 = Instant.parse("2024-01-01T12:00:02Z")
        aggregator.add(t0, BleCrankData(crankRevolutionsCount = 10, crankRevolutionsTime = 0))
        aggregator.add(t1, BleCrankData(crankRevolutionsCount = 11, crankRevolutionsTime = 1024))
        aggregator.add(t2, BleCrankData(crankRevolutionsCount = 11, crankRevolutionsTime = 1024))
        assertEquals(0L, aggregator.current(t2))
    }

    @Test
    fun cyclingSpeedAggregator_computesMetersPerSecond() {
        val aggregator = BleCyclingSpeedAggregator(wheelCircumferenceMeters = 2.1)
        val t0 = Instant.parse("2024-01-01T12:00:00Z")
        val t1 = Instant.parse("2024-01-01T12:00:01Z")
        aggregator.add(t0, BleWheelData(wheelRevolutionsCount = 100, wheelRevolutionsTime = 0))
        aggregator.add(t1, BleWheelData(wheelRevolutionsCount = 102, wheelRevolutionsTime = 1024))
        val speed = aggregator.current(t1)
        assertEquals(4.2, speed!!, 0.01)
    }

    @Test
    fun cyclingSpeedAggregator_returnsZeroWhenWheelStops() {
        val aggregator = BleCyclingSpeedAggregator(wheelCircumferenceMeters = 2.1)
        val t0 = Instant.parse("2024-01-01T12:00:00Z")
        val t1 = Instant.parse("2024-01-01T12:00:01Z")
        val t2 = Instant.parse("2024-01-01T12:00:02Z")
        aggregator.add(t0, BleWheelData(wheelRevolutionsCount = 100, wheelRevolutionsTime = 0))
        aggregator.add(t1, BleWheelData(wheelRevolutionsCount = 102, wheelRevolutionsTime = 1024))
        aggregator.add(t2, BleWheelData(wheelRevolutionsCount = 102, wheelRevolutionsTime = 1024))
        assertEquals(0.0, aggregator.current(t2)!!, 0.01)
    }

    @Test
    fun powerAggregator_returnsInstantaneousPower() {
        val aggregator = BlePowerAggregator()
        val now = Instant.parse("2024-01-01T12:00:00Z")
        aggregator.add(now, BleCyclingPowerData(powerWatts = 250, crank = null))
        assertEquals(250.0, aggregator.current(now))
    }

    @Test
    fun runningAggregator_returnsLatestSpeedAndCadence() {
        val aggregator = BleRunningSpeedCadenceAggregator()
        val now = Instant.parse("2024-01-01T12:00:00Z")
        aggregator.add(
            now,
            BleRunningSpeedCadenceData(speedMetersPerSecond = 3.5, cadenceRpm = 90L),
        )
        val current = aggregator.current(now)
        assertEquals(3.5, current?.first)
        assertEquals(90L, current?.second)
    }

    @Test
    fun aggregator_clearsStaleValues() {
        val aggregator = BleHeartRateAggregator()
        val now = Instant.parse("2024-01-01T12:00:00Z")
        aggregator.add(now, 120L)
        assertNull(aggregator.current(now.plusSeconds(6)))
    }

    @Test
    fun speedAndCadenceAggregators_returnZeroWhenStale() {
        val now = Instant.parse("2024-01-01T12:00:00Z")

        val cadenceAggregator = BleCyclingCadenceAggregator()
        cadenceAggregator.add(
            now,
            BleCrankData(crankRevolutionsCount = 10, crankRevolutionsTime = 0),
        )
        assertEquals(0L, cadenceAggregator.current(now.plusSeconds(6)))

        val speedAggregator = BleCyclingSpeedAggregator(wheelCircumferenceMeters = 2.1)
        speedAggregator.add(
            now,
            BleWheelData(wheelRevolutionsCount = 100, wheelRevolutionsTime = 0),
        )
        assertEquals(0.0, speedAggregator.current(now.plusSeconds(6))!!, 0.01)

        val runningAggregator = BleRunningSpeedCadenceAggregator()
        runningAggregator.add(
            now,
            BleRunningSpeedCadenceData(speedMetersPerSecond = 3.5, cadenceRpm = 90L),
        )
        val running = runningAggregator.current(now.plusSeconds(6))
        assertEquals(0.0, running?.first)
        assertEquals(0L, running?.second)
    }
}
