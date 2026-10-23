package tech.mmarca.openvitals.domain.report

import androidx.health.connect.client.records.BloodGlucoseRecord
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.model.BloodGlucoseEntry
import tech.mmarca.openvitals.domain.model.GlucoseRecordValues

class BloodGlucoseReportTest {

    private val zone: ZoneId = ZoneId.of("Europe/Madrid")

    private fun reading(
        dayOfMonth: Int,
        hour: Int,
        mmol: Double,
        relation: Int = GlucoseRecordValues.RELATION_TO_MEAL_GENERAL,
        second: Int = 0,
    ) = BloodGlucoseEntry(
        time = LocalDateTime.of(2026, 6, dayOfMonth, hour, 0, second).atZone(zone).toInstant(),
        millimolesPerLiter = mmol,
        specimenSource = 0,
        mealType = 0,
        relationToMeal = relation,
        source = "test",
    )

    @Test fun `the mirrored relation constants match the androidx library`() {
        assertEquals(BloodGlucoseRecord.RELATION_TO_MEAL_UNKNOWN, GlucoseRecordValues.RELATION_TO_MEAL_UNKNOWN)
        assertEquals(BloodGlucoseRecord.RELATION_TO_MEAL_GENERAL, GlucoseRecordValues.RELATION_TO_MEAL_GENERAL)
        assertEquals(BloodGlucoseRecord.RELATION_TO_MEAL_FASTING, GlucoseRecordValues.RELATION_TO_MEAL_FASTING)
        assertEquals(BloodGlucoseRecord.RELATION_TO_MEAL_BEFORE_MEAL, GlucoseRecordValues.RELATION_TO_MEAL_BEFORE_MEAL)
        assertEquals(BloodGlucoseRecord.RELATION_TO_MEAL_AFTER_MEAL, GlucoseRecordValues.RELATION_TO_MEAL_AFTER_MEAL)
    }

    @Test fun `no readings means no detail`() {
        assertNull(bloodGlucoseDetail(emptyList(), zone))
    }

    @Test fun `contexts average their own readings, fasting listed first`() {
        val detail = bloodGlucoseDetail(
            listOf(
                reading(1, 20, 7.8, GlucoseRecordValues.RELATION_TO_MEAL_AFTER_MEAL),
                reading(1, 8, 5.0, GlucoseRecordValues.RELATION_TO_MEAL_FASTING),
                reading(2, 8, 5.4, GlucoseRecordValues.RELATION_TO_MEAL_FASTING),
            ),
            zone,
        )!!

        val fasting = detail.contextAverages.first()
        assertEquals(GlucoseRecordValues.RELATION_TO_MEAL_FASTING, fasting.relationToMeal)
        assertEquals(5.2, fasting.average, 1e-9)
        assertEquals(2, fasting.readings)
        val afterMeal = detail.contextAverages[1]
        assertEquals(GlucoseRecordValues.RELATION_TO_MEAL_AFTER_MEAL, afterMeal.relationToMeal)
        assertEquals(7.8, afterMeal.average, 1e-9)
    }

    @Test fun `empty contexts are omitted and the total row closes the table`() {
        val detail = bloodGlucoseDetail(
            listOf(reading(1, 8, 5.0, GlucoseRecordValues.RELATION_TO_MEAL_FASTING)),
            zone,
        )!!

        assertEquals(2, detail.contextAverages.size)
        val total = detail.contextAverages.last()
        assertNull(total.relationToMeal)
        assertEquals(1, total.readings)
    }

    @Test fun `an out-of-range relation value reads as unspecified rather than crashing`() {
        val detail = bloodGlucoseDetail(listOf(reading(1, 8, 5.0, relation = 99)), zone)!!

        assertEquals(GlucoseRecordValues.RELATION_TO_MEAL_UNKNOWN, detail.contextAverages.first().relationToMeal)
    }

    @Test fun `duplicated records collapse, repeat measurements stay`() {
        val detail = bloodGlucoseDetail(
            listOf(
                reading(1, 8, 5.0, GlucoseRecordValues.RELATION_TO_MEAL_FASTING),
                reading(1, 8, 5.0, GlucoseRecordValues.RELATION_TO_MEAL_FASTING), // exact copy
                reading(1, 8, 5.0, GlucoseRecordValues.RELATION_TO_MEAL_FASTING, second = 31), // repeat
            ),
            zone,
        )!!

        assertEquals(2, detail.readings.size)
    }

    @Test fun `summary spans all readings and counts distinct days`() {
        val detail = bloodGlucoseDetail(
            listOf(
                reading(1, 8, 4.8),
                reading(1, 20, 7.6),
                reading(3, 8, 5.2),
            ),
            zone,
        )!!

        assertEquals(4.8, detail.summary.min, 1e-9)
        assertEquals(7.6, detail.summary.max, 1e-9)
        assertEquals(2, detail.summary.daysWithData)
        assertNull(detail.summary.total)
        assertTrue(detail.readings.zipWithNext().all { (a, b) -> a.time <= b.time })
    }
}
