package tech.mmarca.openvitals.features.body

import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.period.WeekPeriodMode
import tech.mmarca.openvitals.domain.model.BodyFatEntry
import tech.mmarca.openvitals.domain.model.WeightEntry
import tech.mmarca.openvitals.domain.query.BodyPeriodData
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BodyPresentationMapperTest {

    private val anchorDate = LocalDate.of(2026, 5, 10)
    private val weekQuery = PeriodLoadQuery(
        range = TimeRange.WEEK,
        anchorDate = anchorDate,
        weekPeriodMode = WeekPeriodMode.MONDAY_TO_SUNDAY,
    )

    private fun weightAt(weightKg: Double, epochSeconds: Long) =
        WeightEntry(time = Instant.ofEpochSecond(epochSeconds), weightKg = weightKg, source = "test")

    private fun bodyFatAt(percent: Double, epochSeconds: Long) =
        BodyFatEntry(time = Instant.ofEpochSecond(epochSeconds), percent = percent, source = "test")

    @Test fun `summary uses latest body values when period entries are empty`() {
        val display = BodyPresentationMapper.build(
            query = weekQuery,
            data = BodyPeriodData(
                latestWeightKg = 77.0,
                heightCm = 186.0,
                latestBodyFatPercent = 20.0,
            ),
        )

        assertEquals(77.0, display.summary.latestWeightKg!!, 0.01)
        assertEquals(20.0, display.summary.latestBodyFatPercent!!, 0.01)
        assertEquals(22.26, display.summary.bmi!!, 0.01)
        assertEquals(17.81, display.summary.ffmi!!, 0.01)
        assertEquals(17.43, display.summary.adjustedFfmi!!, 0.01)
    }

    @Test fun `bmi is null when height is missing`() {
        val display = BodyPresentationMapper.build(
            query = weekQuery,
            data = BodyPeriodData(
                weightEntries = listOf(weightAt(75.0, 1_000)),
            ),
        )

        assertNull(display.summary.bmi)
    }

    @Test fun `bmi computed correctly from weight and height`() {
        val display = BodyPresentationMapper.build(
            query = weekQuery,
            data = BodyPeriodData(
                weightEntries = listOf(weightAt(75.0, 1_000)),
                heightCm = 178.0,
            ),
        )

        assertEquals(23.67, display.summary.bmi!!, 0.01)
    }

    @Test fun `bmi uses most recent weight entry`() {
        val display = BodyPresentationMapper.build(
            query = weekQuery,
            data = BodyPeriodData(
                weightEntries = listOf(weightAt(80.0, 1_000), weightAt(76.0, 2_000)),
                heightCm = 180.0,
            ),
        )

        assertEquals(23.46, display.summary.bmi!!, 0.01)
    }

    @Test fun `ffmi computed correctly from weight height and body fat`() {
        val display = BodyPresentationMapper.build(
            query = weekQuery,
            data = BodyPeriodData(
                weightEntries = listOf(weightAt(75.0, 1_000)),
                heightCm = 178.0,
                bodyFatEntries = listOf(bodyFatAt(20.0, 1_000)),
            ),
        )

        assertEquals(18.94, display.summary.ffmi!!, 0.01)
        assertEquals(19.06, display.summary.adjustedFfmi!!, 0.01)
    }

    @Test fun `ffmi history has one reading per body fat reading, with the nearest weight`() {
        val display = BodyPresentationMapper.build(
            query = weekQuery,
            data = BodyPeriodData(
                // The middle weigh-in has no body fat near it, and adds no reading.
                weightEntries = listOf(weightAt(80.0, 1_000), weightAt(75.0, 3_000), weightAt(70.0, 5_000)),
                heightCm = 180.0,
                bodyFatEntries = listOf(bodyFatAt(20.0, 4_800), bodyFatAt(25.0, 1_100)),
            ),
        )

        val readings = display.ffmiReadings
        assertEquals(listOf(1_100L, 4_800L), readings.map { it.time.epochSecond })
        // 80 kg at 25% is 60 kg fat-free; / 1.8² = 18.52, and 1.8 m adjusts by nothing.
        assertEquals(18.52, readings[0].ffmi, 0.01)
        assertEquals(18.52, readings[0].adjustedFfmi, 0.01)
        // 70 kg at 20% is 56 kg fat-free.
        assertEquals(17.28, readings[1].adjustedFfmi, 0.01)
    }

    @Test fun `ffmi history takes a weight from across the period edge`() {
        val display = BodyPresentationMapper.build(
            query = weekQuery,
            data = BodyPeriodData(
                previousWeightEntries = listOf(weightAt(75.0, 1_000)),
                heightCm = 178.0,
                bodyFatEntries = listOf(bodyFatAt(20.0, 1_200)),
                previousBodyFatEntries = listOf(bodyFatAt(22.0, 900)),
            ),
        )

        assertEquals(19.06, display.ffmiReadings.single().adjustedFfmi, 0.01)
        // 75 kg at 22% is 58.5 kg fat-free; / 1.78² = 18.46, + 6.3 * 0.02 = 18.59.
        assertEquals(18.59, display.previousFfmiReadings.single().adjustedFfmi, 0.01)
    }

    @Test fun `ffmi history is empty without a height or a weight`() {
        val noHeight = BodyPresentationMapper.build(
            query = weekQuery,
            data = BodyPeriodData(
                weightEntries = listOf(weightAt(75.0, 1_000)),
                bodyFatEntries = listOf(bodyFatAt(20.0, 1_000)),
            ),
        )
        val noWeight = BodyPresentationMapper.build(
            query = weekQuery,
            data = BodyPeriodData(
                heightCm = 178.0,
                bodyFatEntries = listOf(bodyFatAt(20.0, 1_000)),
            ),
        )

        assertEquals(emptyList<FfmiReading>(), noHeight.ffmiReadings)
        assertEquals(emptyList<FfmiReading>(), noWeight.ffmiReadings)
    }

    @Test fun `weight change ignores single entry periods`() {
        val display = BodyPresentationMapper.build(
            query = weekQuery,
            data = BodyPeriodData(
                weightEntries = listOf(weightAt(75.0, 1_000)),
            ),
        )

        assertNull(display.summary.weightChangeKg)
    }

    @Test fun `weight change is positive when weight increased`() {
        val display = BodyPresentationMapper.build(
            query = weekQuery,
            data = BodyPeriodData(
                weightEntries = listOf(weightAt(70.0, 1_000), weightAt(73.5, 2_000)),
            ),
        )

        assertEquals(3.5, display.summary.weightChangeKg!!, 0.01)
    }
}
