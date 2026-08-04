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
