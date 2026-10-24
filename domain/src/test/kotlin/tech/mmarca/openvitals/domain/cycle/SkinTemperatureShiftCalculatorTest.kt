package tech.mmarca.openvitals.domain.cycle

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.model.SkinTemperatureDeltaSample
import tech.mmarca.openvitals.domain.model.SkinTemperatureEntry

/** Synthetic cycles only. No recorded data is used, here or anywhere in the repo. */
private class SyntheticCycle(
    val start: LocalDate = LocalDate.of(2026, 3, 1),
    val length: Int = 28,
    /** 1-based cycle day of the first high night; null for a cycle without a rise. */
    val firstHighCycleDay: Int? = 15,
    val baseline: Double = -0.1,
    val rise: Double = 0.3,
    val noiseSd: Double = 0.04,
    val gaps: Set<Int> = emptySet(),
    /** Cycle day to the extra degrees of a night with a fever. */
    val illNights: Map<Int, Double> = emptyMap(),
    val source: String = "device.a",
    val seed: Int = 7,
) {
    fun nights(): List<SkinTemperatureNight> {
        val random = Random(seed)
        return (1..length).filter { it !in gaps }.map { day ->
            val raised = firstHighCycleDay != null && day >= firstHighCycleDay
            val value = baseline + (if (raised) rise else 0.0) + noiseSd * gaussian(random) + (illNights[day] ?: 0.0)
            SkinTemperatureNight(start.plusDays((day - 1).toLong()), source, value)
        }
    }

    private fun gaussian(random: Random): Double =
        sqrt(-2.0 * ln(1.0 - random.nextDouble())) * cos(2.0 * Math.PI * random.nextDouble())

    fun day(cycleDay: Int): LocalDate = start.plusDays((cycleDay - 1).toLong())
}

class SkinTemperatureShiftCalculatorTest {

    private fun evaluate(cycle: SyntheticCycle, nextPeriod: Boolean = true, extra: List<SkinTemperatureNight> = emptyList()) =
        SkinTemperatureShiftCalculator.evaluateCycle(
            cycleStart = cycle.start,
            nextPeriodStart = if (nextPeriod) cycle.day(cycle.length + 1) else null,
            nights = cycle.nights() + extra,
        )

    @Test
    fun `a clean rise is confirmed on its first high night`() {
        val cycle = SyntheticCycle()
        val result = evaluate(cycle).result as SkinTemperatureShiftResult.Confirmed
        assertEquals(cycle.day(15), result.firstHighDay)
        assertEquals("device.a", result.source)
        assertEquals(0.3, result.amplitudeCelsius, 0.06)
    }

    @Test
    fun `a running cycle needs no known luteal length`() {
        val cycle = SyntheticCycle(length = 22)
        assertTrue(evaluate(cycle, nextPeriod = false).result is SkinTemperatureShiftResult.Confirmed)
    }

    @Test
    fun `a cycle without a rise is never confirmed`() {
        val quiet = evaluate(SyntheticCycle(firstHighCycleDay = null)).result
        assertTrue(quiet is SkinTemperatureShiftResult.SignalTooWeak)
        val noisy = evaluate(SyntheticCycle(firstHighCycleDay = null, noiseSd = 0.15)).result
        assertTrue(noisy is SkinTemperatureShiftResult.None || noisy is SkinTemperatureShiftResult.SignalTooWeak)
    }

    @Test
    fun `a strong signal without a sustained rise is no shift detected`() {
        // A slow ramp: the 0.2 degree signal is there, but no night clears the six before it.
        val nights = SyntheticCycle(firstHighCycleDay = null, noiseSd = 0.005).nights()
            .mapIndexed { index, night -> night.copy(deltaCelsius = night.deltaCelsius + 0.03 * index) }
        val result = SkinTemperatureShiftCalculator.evaluateSource(LocalDate.of(2026, 3, 1), null, "device.a", nights)
        assertEquals(SkinTemperatureShiftResult.None, result)
    }

    @Test
    fun `gaps are filled and the rise is still found`() {
        val cycle = SyntheticCycle(gaps = setOf(4, 9, 10, 20, 24))
        val result = evaluate(cycle).result as SkinTemperatureShiftResult.Confirmed
        assertEquals(cycle.day(15), result.firstHighDay)
    }

    @Test
    fun `one outlier night is rejected`() {
        val cycle = SyntheticCycle(illNights = mapOf(5 to 1.2))
        val result = evaluate(cycle).result as SkinTemperatureShiftResult.Confirmed
        assertEquals(cycle.day(15), result.firstHighDay)
    }

    @Test
    fun `two ill nights before the rise do not move it`() {
        val cycle = SyntheticCycle(illNights = mapOf(9 to 0.9, 10 to 0.9))
        val result = evaluate(cycle).result as SkinTemperatureShiftResult.Confirmed
        assertEquals(cycle.day(15), result.firstHighDay)
    }

    @Test
    fun `a rise under the signal gate is too weak and says by how much`() {
        val result = evaluate(SyntheticCycle(rise = 0.08, noiseSd = 0.02)).result as SkinTemperatureShiftResult.SignalTooWeak
        assertTrue(result.amplitudeCelsius < 0.2)
        assertEquals("device.a", result.source)
    }

    @Test
    fun `a luteal phase longer than 17 days is not plausible`() {
        val cycle = SyntheticCycle(length = 40, firstHighCycleDay = 15)
        val result = evaluate(cycle).result as SkinTemperatureShiftResult.NotPlausible
        assertEquals(27, result.lutealDays)
        assertEquals(14, result.follicularDays)
    }

    @Test
    fun `a luteal phase shorter than 7 days is not plausible`() {
        val result = evaluate(SyntheticCycle(length = 18, firstHighCycleDay = 14)).result
        assertTrue(result is SkinTemperatureShiftResult.NotPlausible)
    }

    @Test
    fun `a follicular phase shorter than 10 days is not plausible`() {
        val result = evaluate(SyntheticCycle(firstHighCycleDay = 8), nextPeriod = false).result
        assertTrue(result is SkinTemperatureShiftResult.NotPlausible)
    }

    @Test
    fun `fewer than 15 nights are insufficient`() {
        val result = evaluate(SyntheticCycle(length = 14, firstHighCycleDay = null)).result
        assertEquals(SkinTemperatureShiftResult.InsufficientData(14), result)
    }

    @Test
    fun `no nights at all are insufficient`() {
        val result = SkinTemperatureShiftCalculator.evaluateCycle(LocalDate.of(2026, 3, 1), null, emptyList())
        assertEquals(SkinTemperatureShiftResult.InsufficientData(0), result.result)
    }

    @Test
    fun `two sources are judged apart and the confirmed one carries the result`() {
        val weak = SyntheticCycle(source = "device.weak", rise = 0.05, noiseSd = 0.02, seed = 3)
        val strong = SyntheticCycle()
        val analysis = evaluate(strong, extra = weak.nights())
        assertTrue(analysis.bySource.getValue("device.weak") is SkinTemperatureShiftResult.SignalTooWeak)
        assertTrue(analysis.bySource.getValue("device.a") is SkinTemperatureShiftResult.Confirmed)
        assertEquals("device.a", analysis.source)
        assertTrue(analysis.result is SkinTemperatureShiftResult.Confirmed)
    }

    @Test
    fun `nights of another cycle are ignored`() {
        val cycle = SyntheticCycle()
        val before = SyntheticCycle(start = cycle.start.minusDays(30), firstHighCycleDay = null).nights()
        val result = evaluate(cycle, extra = before).result as SkinTemperatureShiftResult.Confirmed
        assertEquals(cycle.day(15), result.firstHighDay)
    }

    @Test
    fun `a short clear rise beyond 2 SD of the whole cycle is not dropped as an outlier`() {
        val cycle = SyntheticCycle(length = 22, firstHighCycleDay = 19, rise = 0.4, noiseSd = 0.02)
        val result = evaluate(cycle, nextPeriod = false).result as SkinTemperatureShiftResult.Confirmed
        assertEquals(cycle.day(19), result.firstHighDay)
    }

    @Test
    fun `fifteen nights spread over sixty days are insufficient`() {
        val start = LocalDate.of(2026, 3, 1)
        val nights = (0 until 15).map { SkinTemperatureNight(start.plusDays(it * 4L), "device.a", if (it > 8) 0.3 else 0.0) }
        val result = SkinTemperatureShiftCalculator.evaluateSource(start, null, "device.a", nights)
        assertEquals(SkinTemperatureShiftResult.InsufficientData(15), result)
    }

    @Test
    fun `a gap longer than three days is not filled`() {
        val cycle = SyntheticCycle(length = 30, gaps = setOf(10, 11, 12, 13))
        assertTrue(evaluate(cycle, nextPeriod = false).result is SkinTemperatureShiftResult.InsufficientData)
    }

    @Test
    fun `a filled night is never the first high day`() {
        val cycle = SyntheticCycle(gaps = setOf(15))
        val result = evaluate(cycle).result as SkinTemperatureShiftResult.Confirmed
        assertTrue(result.firstHighDay != cycle.day(15))
    }

    @Test
    fun `a series that falls after a spike does not pass the gate`() {
        // Spikes of two nights early, then flat: high-minus-low exists only with the high first.
        val nights = SyntheticCycle(firstHighCycleDay = null, noiseSd = 0.01).nights()
            .map { if (it.date.dayOfMonth in 3..4) it.copy(deltaCelsius = it.deltaCelsius + 0.5) else it }
        val result = SkinTemperatureShiftCalculator.evaluateSource(LocalDate.of(2026, 3, 1), null, "device.a", nights)
        assertTrue(result is SkinTemperatureShiftResult.SignalTooWeak || result is SkinTemperatureShiftResult.None)
        assertTrue(result !is SkinTemperatureShiftResult.Confirmed)
    }

    @Test
    fun `equal sources are ordered by source id`() {
        val a = SyntheticCycle(source = "b.device", firstHighCycleDay = null, seed = 1).nights()
        val b = SyntheticCycle(source = "a.device", firstHighCycleDay = null, seed = 1).nights()
        val first = SkinTemperatureShiftCalculator.evaluateCycle(LocalDate.of(2026, 3, 1), null, a + b)
        val second = SkinTemperatureShiftCalculator.evaluateCycle(LocalDate.of(2026, 3, 1), null, b + a)
        assertEquals(first.source, second.source)
    }

    @Test
    fun `a night is the mean of its samples, grouped by date and source`() {
        val zone = ZoneOffset.UTC
        fun entry(end: String, source: String, vararg deltas: Double, average: Double? = null) = SkinTemperatureEntry(
            startTime = Instant.parse(end).minusSeconds(8 * 3600),
            endTime = Instant.parse(end),
            baselineCelsius = null,
            averageDeltaCelsius = average,
            minDeltaCelsius = null,
            maxDeltaCelsius = null,
            measurementLocation = 0,
            source = source,
            deltas = deltas.map { SkinTemperatureDeltaSample(Instant.parse(end), it) },
        )
        val nights = SkinTemperatureNights.fromEntries(
            listOf(
                entry("2026-03-02T06:00:00Z", "a", 0.1, 0.3),
                entry("2026-03-02T07:00:00Z", "b", 0.5),
                entry("2026-03-03T06:00:00Z", "a", average = 0.4),
                entry("2026-03-04T06:00:00Z", "a"),
            ),
            zone,
        )
        assertEquals(
            listOf(
                SkinTemperatureNight(LocalDate.of(2026, 3, 2), "a", 0.2),
                SkinTemperatureNight(LocalDate.of(2026, 3, 3), "a", 0.4),
                SkinTemperatureNight(LocalDate.of(2026, 3, 2), "b", 0.5),
            ),
            nights.map { it.copy(deltaCelsius = Math.round(it.deltaCelsius * 100) / 100.0) },
        )
    }
}
