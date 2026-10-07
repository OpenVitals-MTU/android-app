package tech.mmarca.openvitals.features.vitals

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import tech.mmarca.openvitals.domain.model.SkinTemperatureDeltaSample
import tech.mmarca.openvitals.domain.model.SkinTemperatureEntry

/** The day chart draws the samples the records carry, not one dot per record. */
class SkinTemperatureDayChartTest {

    private val dayStart = Instant.parse("2026-03-02T00:00:00Z")
    private val dayEnd = Instant.parse("2026-03-03T00:00:00Z")

    private fun at(hour: Int, minute: Int = 0): Instant = Instant.parse("2026-03-02T%02d:%02d:00Z".format(hour, minute))

    private fun record(vararg samples: Pair<Instant, Double>) = SkinTemperatureEntry(
        startTime = samples.firstOrNull()?.first ?: dayStart,
        endTime = samples.lastOrNull()?.first ?: dayStart,
        baselineCelsius = 33.0,
        averageDeltaCelsius = samples.map { it.second }.average().takeIf { samples.isNotEmpty() },
        minDeltaCelsius = null,
        maxDeltaCelsius = null,
        measurementLocation = 0,
        source = "Test",
        deltas = samples.map { (time, delta) -> SkinTemperatureDeltaSample(time, delta) },
    )

    @Test fun `samples across records are flattened and sorted, the day's only`() {
        val entries = listOf(
            record(at(14, 10) to 0.0, at(8, 44) to -1.8, at(8, 45) to 0.2),
            record(Instant.parse("2026-03-01T23:30:00Z") to 0.5),
            record(),
        )

        val samples = skinTemperatureDaySamples(entries, dayStart, dayEnd)

        assertEquals(listOf(at(8, 44), at(8, 45), at(14, 10)), samples.map { it.time })
        assertEquals(listOf(-1.8, 0.2, 0.0), samples.map { it.deltaCelsius })
    }

    @Test fun `the summary is zero-centred and spans the recorded times`() {
        val summary = skinTemperatureDaySummary(
            listOf(
                SkinTemperatureDeltaSample(at(8), -0.7),
                SkinTemperatureDeltaSample(at(9), 0.1),
                SkinTemperatureDeltaSample(at(14), 0.0),
            ),
        )!!

        assertEquals(-0.2, summary.averageCelsius, 1e-9)
        assertEquals(-0.756, summary.axisMin, 1e-9)
        assertEquals(0.756, summary.axisMax, 1e-9)
        assertEquals(at(8), summary.first)
        assertEquals(at(14), summary.last)
    }

    @Test fun `one sample is a dot, not a line`() {
        assertNull(skinTemperatureDaySummary(listOf(SkinTemperatureDeltaSample(at(8), 0.3))))
        assertNull(skinTemperatureDaySummary(emptyList()))
    }
}
