package tech.mmarca.openvitals.domain.model

import androidx.health.connect.client.records.BloodPressureRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The clientRecordId token is the ONLY place the BP meal context lives —
 * Health Connect has no field for it — so encode/parse must round-trip
 * perfectly, and a foreign or mangled id must read as "no context", never
 * crash or mis-read.
 */
class BpMealContextTest {

    private val baseId = "openvitals_vitals_blood_pressure_1750000000000_abc-123"

    @Test fun `every context round-trips through the client id`() {
        BpMealContext.entries.forEach { context ->
            val encoded = baseId.withBpMealContext(context)
            assertEquals(context, bpMealContextFromClientRecordId(encoded))
        }
    }

    @Test fun `re-encoding replaces the token instead of stacking`() {
        val once = baseId.withBpMealContext(BpMealContext.BEFORE_BREAKFAST)
        val twice = once.withBpMealContext(BpMealContext.AFTER_DINNER)

        assertEquals(BpMealContext.AFTER_DINNER, bpMealContextFromClientRecordId(twice))
        assertEquals(baseId.withBpMealContext(BpMealContext.AFTER_DINNER), twice)
    }

    @Test fun `a null context strips the token back to the base id`() {
        val encoded = baseId.withBpMealContext(BpMealContext.BEFORE_LUNCH)

        assertEquals(baseId, encoded.withBpMealContext(null))
    }

    @Test fun `foreign, bare and mangled ids read as no context`() {
        assertNull(bpMealContextFromClientRecordId(null))
        assertNull(bpMealContextFromClientRecordId(baseId))
        assertNull(bpMealContextFromClientRecordId("some_other_app_record_42"))
        assertNull(bpMealContextFromClientRecordId("${baseId}_bpctx-"))
        assertNull(bpMealContextFromClientRecordId("${baseId}_bpctx-nonsense"))
    }

    @Test fun `the mirrored position and location constants match the androidx library`() {
        assertEquals(BloodPressureRecord.BODY_POSITION_UNKNOWN, BpRecordValues.BODY_POSITION_UNKNOWN)
        assertEquals(BloodPressureRecord.BODY_POSITION_STANDING_UP, BpRecordValues.BODY_POSITION_STANDING_UP)
        assertEquals(BloodPressureRecord.BODY_POSITION_SITTING_DOWN, BpRecordValues.BODY_POSITION_SITTING_DOWN)
        assertEquals(BloodPressureRecord.BODY_POSITION_LYING_DOWN, BpRecordValues.BODY_POSITION_LYING_DOWN)
        assertEquals(BloodPressureRecord.BODY_POSITION_RECLINING, BpRecordValues.BODY_POSITION_RECLINING)
        assertEquals(BloodPressureRecord.MEASUREMENT_LOCATION_UNKNOWN, BpRecordValues.MEASUREMENT_LOCATION_UNKNOWN)
        assertEquals(BloodPressureRecord.MEASUREMENT_LOCATION_LEFT_WRIST, BpRecordValues.MEASUREMENT_LOCATION_LEFT_WRIST)
        assertEquals(BloodPressureRecord.MEASUREMENT_LOCATION_RIGHT_WRIST, BpRecordValues.MEASUREMENT_LOCATION_RIGHT_WRIST)
        assertEquals(
            BloodPressureRecord.MEASUREMENT_LOCATION_LEFT_UPPER_ARM,
            BpRecordValues.MEASUREMENT_LOCATION_LEFT_UPPER_ARM,
        )
        assertEquals(
            BloodPressureRecord.MEASUREMENT_LOCATION_RIGHT_UPPER_ARM,
            BpRecordValues.MEASUREMENT_LOCATION_RIGHT_UPPER_ARM,
        )
    }
}
