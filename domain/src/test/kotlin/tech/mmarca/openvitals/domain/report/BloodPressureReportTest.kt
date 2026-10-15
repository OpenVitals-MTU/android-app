package tech.mmarca.openvitals.domain.report

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.model.BloodPressureEntry
import tech.mmarca.openvitals.domain.model.BpMealContext

class BloodPressureReportTest {

    private val zone: ZoneId = ZoneId.of("Europe/Madrid")

    private fun reading(
        dayOfMonth: Int,
        hour: Int,
        minute: Int,
        systolic: Int,
        diastolic: Int,
        second: Int = 0,
        mealContext: BpMealContext? = null,
    ) = BloodPressureEntry(
        time = LocalDateTime.of(2026, 6, dayOfMonth, hour, minute, second).atZone(zone).toInstant(),
        systolicMmHg = systolic,
        diastolicMmHg = diastolic,
        source = "test",
        mealContext = mealContext,
    )

    @Test fun `no readings means no detail`() {
        assertNull(bloodPressureDetail(emptyList(), zone))
    }

    @Test fun `readings come back sorted by time whatever order they arrived in`() {
        val detail = bloodPressureDetail(
            listOf(reading(2, 8, 0, 120, 80), reading(1, 8, 0, 110, 70)),
            zone,
        )!!

        assertTrue(detail.readings[0].time < detail.readings[1].time)
        assertEquals(110, detail.readings[0].systolicMmHg)
    }

    @Test fun `estimated contexts follow the six time windows`() {
        assertEquals(BpMealContext.BEFORE_BREAKFAST, estimatedContext(4))
        assertEquals(BpMealContext.BEFORE_BREAKFAST, estimatedContext(7))
        assertEquals(BpMealContext.AFTER_BREAKFAST, estimatedContext(8))
        assertEquals(BpMealContext.BEFORE_LUNCH, estimatedContext(11))
        assertEquals(BpMealContext.AFTER_LUNCH, estimatedContext(14))
        assertEquals(BpMealContext.BEFORE_DINNER, estimatedContext(17))
        assertEquals(BpMealContext.AFTER_DINNER, estimatedContext(20))
        // Past midnight is still "after dinner": taken before sleep, not breakfast.
        assertEquals(BpMealContext.AFTER_DINNER, estimatedContext(1))
    }

    @Test fun `an explicit context beats the time-of-day estimate`() {
        // 08:30 would estimate as after-breakfast; the record says before lunch.
        val detail = bloodPressureDetail(
            listOf(reading(1, 8, 30, 110, 70, mealContext = BpMealContext.BEFORE_LUNCH)),
            zone,
        )!!

        val readingRow = detail.readings.single()
        assertEquals(BpMealContext.BEFORE_LUNCH, readingRow.context)
        assertEquals(false, readingRow.contextEstimated)
        assertEquals(BpMealContext.BEFORE_LUNCH, detail.slotAverages.first().context)
    }

    @Test fun `records without a context are estimated and marked as such`() {
        val detail = bloodPressureDetail(listOf(reading(1, 8, 30, 110, 70)), zone)!!

        val readingRow = detail.readings.single()
        assertEquals(BpMealContext.AFTER_BREAKFAST, readingRow.context)
        assertEquals(true, readingRow.contextEstimated)
    }

    @Test fun `context averages mean their own readings only`() {
        val detail = bloodPressureDetail(
            listOf(
                reading(1, 8, 0, 100, 60),
                reading(1, 9, 0, 110, 70),
                reading(1, 20, 0, 130, 90),
            ),
            zone,
        )!!

        val afterBreakfast = detail.slotAverages.first { it.context == BpMealContext.AFTER_BREAKFAST }
        assertEquals(105.0, afterBreakfast.systolic, 1e-9)
        assertEquals(65.0, afterBreakfast.diastolic, 1e-9)
        assertEquals(2, afterBreakfast.readings)
    }

    @Test fun `empty slots are omitted but the all-readings total row always closes the table`() {
        val detail = bloodPressureDetail(
            listOf(reading(1, 8, 0, 100, 60), reading(1, 9, 0, 110, 70)),
            zone,
        )!!

        assertEquals(2, detail.slotAverages.size)
        assertEquals(BpMealContext.AFTER_BREAKFAST, detail.slotAverages.first().context)
        val total = detail.slotAverages.last()
        assertNull(total.context)
        assertEquals(2, total.readings)
        assertEquals(105.0, total.systolic, 1e-9)
    }

    @Test fun `each component gets its own summary and they never mix`() {
        val detail = bloodPressureDetail(
            listOf(
                reading(1, 8, 0, 103, 68),
                reading(1, 20, 0, 127, 72),
            ),
            zone,
        )!!

        assertEquals(115.0, detail.systolic.average, 1e-9)
        assertEquals(103.0, detail.systolic.min, 1e-9)
        assertEquals(127.0, detail.systolic.max, 1e-9)
        assertNull(detail.systolic.total)
        assertEquals(70.0, detail.diastolic.average, 1e-9)
        assertEquals(68.0, detail.diastolic.min, 1e-9)
        assertEquals(72.0, detail.diastolic.max, 1e-9)
    }

    @Test fun `days with data counts distinct local dates, not readings`() {
        val detail = bloodPressureDetail(
            listOf(
                reading(1, 8, 0, 100, 60),
                reading(1, 20, 0, 110, 70),
                reading(3, 8, 0, 120, 80),
            ),
            zone,
        )!!

        assertEquals(2, detail.systolic.daysWithData)
        assertEquals(3, detail.readings.size)
    }

    @Test fun `a duplicate record counts as one reading everywhere`() {
        val detail = bloodPressureDetail(
            listOf(
                reading(1, 8, 0, 110, 70),
                reading(1, 8, 0, 110, 70), // same instant, same values: the same reading twice
                reading(1, 8, 0, 110, 70, second = 20), // same values 20 s later: a shifted copy
                reading(1, 20, 0, 120, 80),
            ),
            zone,
        )!!

        assertEquals(2, detail.readings.size)
        assertEquals(2, detail.slotAverages.last().readings)
        assertEquals(1, detail.slotAverages.first { it.context == BpMealContext.AFTER_BREAKFAST }.readings)
    }

    @Test fun `back-to-back measurements with different values or times all stay`() {
        val detail = bloodPressureDetail(
            listOf(
                reading(1, 8, 0, 110, 70),
                reading(1, 8, 0, 112, 70), // same instant, different systolic: conflicting, keep both
                reading(1, 8, 0, 110, 70, second = 31), // same values PAST the window: a repeat measurement
                reading(1, 8, 2, 110, 70), // two minutes later, same values: clearly a repeat
            ),
            zone,
        )!!

        assertEquals(4, detail.readings.size)
    }

    @Test fun `a duplicate collapse keeps the copy that carries the explicit context`() {
        val detail = bloodPressureDetail(
            listOf(
                reading(1, 8, 0, 110, 70), // an echoing app's bare copy
                reading(1, 8, 0, 110, 70, mealContext = BpMealContext.BEFORE_BREAKFAST),
            ),
            zone,
        )!!

        val readingRow = detail.readings.single()
        assertEquals(BpMealContext.BEFORE_BREAKFAST, readingRow.context)
        assertEquals(false, readingRow.contextEstimated)
    }

    @Test fun `slot assignment follows the given zone, not UTC`() {
        // 23:30 UTC on the 1st is 01:30 on the 2nd in Madrid: an evening reading whose local date is the next day.
        val entry = BloodPressureEntry(
            time = Instant.parse("2026-06-01T23:30:00Z"),
            systolicMmHg = 115,
            diastolicMmHg = 75,
            source = "test",
        )

        val detail = bloodPressureDetail(listOf(entry), zone)!!

        assertEquals(BpMealContext.AFTER_DINNER, detail.slotAverages.first().context)
        assertEquals(1, detail.systolic.daysWithData)
    }
}
