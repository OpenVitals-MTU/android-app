package tech.mmarca.openvitals.healthconnect

import androidx.health.connect.client.records.CervicalMucusRecord
import androidx.health.connect.client.records.MenstruationFlowRecord
import androidx.health.connect.client.records.OvulationTestRecord
import androidx.health.connect.client.records.SexualActivityRecord
import java.time.Instant
import tech.mmarca.openvitals.domain.model.CycleEntryKind
import tech.mmarca.openvitals.domain.model.CycleEntryWriteRequest
import tech.mmarca.openvitals.domain.model.CycleRecordValues
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Pins the per-kind payload rules of the cycle write path, and pins the
 * domain-mirrored constants against the Health Connect library constants —
 * a drifted constant would silently write wrong enum values into records.
 */
class CycleWriteValidationTest {

    private val now: Instant = Instant.parse("2026-08-05T10:00:00Z")

    private fun request(
        kind: CycleEntryKind,
        flow: Int? = null,
        protectionUsed: Int? = null,
        ovulationTestResult: Int? = null,
        mucusAppearance: Int? = null,
        mucusSensation: Int? = null,
        temperatureCelsius: Double? = null,
        measurementLocation: Int? = null,
        time: Instant = now,
    ) = CycleEntryWriteRequest(
        kind = kind,
        time = time,
        flow = flow,
        protectionUsed = protectionUsed,
        ovulationTestResult = ovulationTestResult,
        mucusAppearance = mucusAppearance,
        mucusSensation = mucusSensation,
        temperatureCelsius = temperatureCelsius,
        measurementLocation = measurementLocation,
    )

    private fun assertRejected(request: CycleEntryWriteRequest) {
        assertThrows(IllegalArgumentException::class.java) { validateCycleEntry(request, now) }
    }

    @Test
    fun `domain constants match the health connect library`() {
        assertEquals(MenstruationFlowRecord.FLOW_LIGHT, CycleRecordValues.FLOW_LIGHT)
        assertEquals(MenstruationFlowRecord.FLOW_MEDIUM, CycleRecordValues.FLOW_MEDIUM)
        assertEquals(MenstruationFlowRecord.FLOW_HEAVY, CycleRecordValues.FLOW_HEAVY)
        assertEquals(OvulationTestRecord.RESULT_INCONCLUSIVE, CycleRecordValues.OVULATION_INCONCLUSIVE)
        assertEquals(OvulationTestRecord.RESULT_POSITIVE, CycleRecordValues.OVULATION_POSITIVE)
        assertEquals(OvulationTestRecord.RESULT_HIGH, CycleRecordValues.OVULATION_HIGH)
        assertEquals(OvulationTestRecord.RESULT_NEGATIVE, CycleRecordValues.OVULATION_NEGATIVE)
        assertEquals(CervicalMucusRecord.APPEARANCE_DRY, CycleRecordValues.MUCUS_APPEARANCE_DRY)
        assertEquals(CervicalMucusRecord.APPEARANCE_STICKY, CycleRecordValues.MUCUS_APPEARANCE_STICKY)
        assertEquals(CervicalMucusRecord.APPEARANCE_CREAMY, CycleRecordValues.MUCUS_APPEARANCE_CREAMY)
        assertEquals(CervicalMucusRecord.APPEARANCE_WATERY, CycleRecordValues.MUCUS_APPEARANCE_WATERY)
        assertEquals(CervicalMucusRecord.APPEARANCE_EGG_WHITE, CycleRecordValues.MUCUS_APPEARANCE_EGG_WHITE)
        assertEquals(CervicalMucusRecord.APPEARANCE_UNUSUAL, CycleRecordValues.MUCUS_APPEARANCE_UNUSUAL)
        assertEquals(CervicalMucusRecord.SENSATION_LIGHT, CycleRecordValues.MUCUS_SENSATION_LIGHT)
        assertEquals(CervicalMucusRecord.SENSATION_MEDIUM, CycleRecordValues.MUCUS_SENSATION_MEDIUM)
        assertEquals(CervicalMucusRecord.SENSATION_HEAVY, CycleRecordValues.MUCUS_SENSATION_HEAVY)
        assertEquals(SexualActivityRecord.PROTECTION_USED_UNKNOWN, CycleRecordValues.PROTECTION_UNKNOWN)
        assertEquals(SexualActivityRecord.PROTECTION_USED_PROTECTED, CycleRecordValues.PROTECTION_PROTECTED)
        assertEquals(SexualActivityRecord.PROTECTION_USED_UNPROTECTED, CycleRecordValues.PROTECTION_UNPROTECTED)
    }

    @Test
    fun `a future time beyond the grace window is rejected for every kind`() {
        assertRejected(
            request(
                CycleEntryKind.SPOTTING,
                time = now.plusSeconds(6 * 60),
            )
        )
    }

    @Test
    fun `a time inside the grace window is accepted`() {
        validateCycleEntry(request(CycleEntryKind.SPOTTING, time = now.plusSeconds(4 * 60)), now)
    }

    @Test
    fun `flow requires a known level`() {
        validateCycleEntry(request(CycleEntryKind.MENSTRUATION_FLOW, flow = CycleRecordValues.FLOW_LIGHT), now)
        assertRejected(request(CycleEntryKind.MENSTRUATION_FLOW, flow = null))
        assertRejected(request(CycleEntryKind.MENSTRUATION_FLOW, flow = CycleRecordValues.FLOW_UNKNOWN))
        assertRejected(request(CycleEntryKind.MENSTRUATION_FLOW, flow = 4))
    }

    @Test
    fun `sexual activity accepts unknown protection but not unknown codes`() {
        validateCycleEntry(
            request(CycleEntryKind.SEXUAL_ACTIVITY, protectionUsed = CycleRecordValues.PROTECTION_UNKNOWN),
            now,
        )
        assertRejected(request(CycleEntryKind.SEXUAL_ACTIVITY, protectionUsed = null))
        assertRejected(request(CycleEntryKind.SEXUAL_ACTIVITY, protectionUsed = 3))
    }

    @Test
    fun `ovulation test requires one of the four results`() {
        validateCycleEntry(
            request(CycleEntryKind.OVULATION_TEST, ovulationTestResult = CycleRecordValues.OVULATION_HIGH),
            now,
        )
        assertRejected(request(CycleEntryKind.OVULATION_TEST, ovulationTestResult = null))
        assertRejected(request(CycleEntryKind.OVULATION_TEST, ovulationTestResult = 4))
    }

    @Test
    fun `cervical mucus needs at least one informative axis`() {
        validateCycleEntry(
            request(CycleEntryKind.CERVICAL_MUCUS, mucusAppearance = CycleRecordValues.MUCUS_APPEARANCE_CREAMY),
            now,
        )
        validateCycleEntry(
            request(CycleEntryKind.CERVICAL_MUCUS, mucusSensation = CycleRecordValues.MUCUS_SENSATION_LIGHT),
            now,
        )
        assertRejected(request(CycleEntryKind.CERVICAL_MUCUS))
        assertRejected(
            request(
                CycleEntryKind.CERVICAL_MUCUS,
                mucusAppearance = CycleRecordValues.MUCUS_APPEARANCE_UNKNOWN,
                mucusSensation = CycleRecordValues.MUCUS_SENSATION_UNKNOWN,
            )
        )
        assertRejected(request(CycleEntryKind.CERVICAL_MUCUS, mucusAppearance = 7))
    }

    @Test
    fun `basal body temperature enforces the basal range`() {
        validateCycleEntry(
            request(CycleEntryKind.BASAL_BODY_TEMPERATURE, temperatureCelsius = 35.0),
            now,
        )
        validateCycleEntry(
            request(CycleEntryKind.BASAL_BODY_TEMPERATURE, temperatureCelsius = 39.0),
            now,
        )
        assertRejected(request(CycleEntryKind.BASAL_BODY_TEMPERATURE, temperatureCelsius = null))
        assertRejected(request(CycleEntryKind.BASAL_BODY_TEMPERATURE, temperatureCelsius = 34.9))
        assertRejected(request(CycleEntryKind.BASAL_BODY_TEMPERATURE, temperatureCelsius = 39.1))
        assertRejected(
            request(
                CycleEntryKind.BASAL_BODY_TEMPERATURE,
                temperatureCelsius = 36.5,
                measurementLocation = 11,
            )
        )
    }
}
