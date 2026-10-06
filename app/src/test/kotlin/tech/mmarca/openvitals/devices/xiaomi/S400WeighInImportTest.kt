package tech.mmarca.openvitals.devices.xiaomi

import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.BodyWaterMassRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test
import tech.mmarca.openvitals.domain.insights.BodyCompositionEstimate
import tech.mmarca.openvitals.domain.model.ScaleReading
import tech.mmarca.openvitals.domain.model.ScaleWeighIn

/** What a weigh-in becomes in Health Connect. The ids here are stored on users' phones. */
class S400WeighInImportTest {

    private val time = Instant.ofEpochSecond(1744250605)
    private val madrid = ZoneId.of("Europe/Madrid")

    private val weighIn = ScaleWeighIn(
        scaleTimestamp = 1744250605,
        profile = 1,
        time = time,
        reading = ScaleReading(69.9, 92, 543.2, 497.6),
        updatedMillis = 4_000,
    )
    private val composition = BodyCompositionEstimate(fatFreeMassKg = 55.0, bodyFatPercent = 21.3, bodyWaterKg = 40.1)

    @Test
    fun `a full weigh-in gives five records, each under its own stable id`() {
        val records = scaleWeighInRecords(weighIn, composition, ScaleWritePermissions, madrid)

        assertEquals(
            listOf(
                "xiaomi_s400_weight_1744250605_p1",
                "xiaomi_s400_heart_rate_1744250605_p1",
                "xiaomi_s400_body_fat_1744250605_p1",
                "xiaomi_s400_lean_mass_1744250605_p1",
                "xiaomi_s400_body_water_1744250605_p1",
            ),
            records.map { it.metadata.clientRecordId },
        )
        assertEquals(69.9, (records[0] as WeightRecord).weight.inKilograms, 1e-9)
        assertEquals(21.3, (records[2] as BodyFatRecord).percentage.value, 1e-9)
        assertEquals(55.0, (records[3] as LeanBodyMassRecord).mass.inKilograms, 1e-9)
        assertEquals(40.1, (records[4] as BodyWaterMassRecord).mass.inKilograms, 1e-9)

        val heartRate = records[1] as HeartRateRecord
        assertEquals(listOf(time to 92L), heartRate.samples.map { it.time to it.beatsPerMinute })
        assertEquals(time to time, heartRate.startTime to heartRate.endTime)
    }

    @Test
    fun `every record says a scale measured it, when, and in which revision`() {
        val weight = scaleWeighInRecords(weighIn, composition, ScaleWritePermissions, madrid).first() as WeightRecord

        assertEquals(time, weight.time)
        assertEquals(ZoneOffset.ofHours(2), weight.zoneOffset)
        assertEquals(Metadata.RECORDING_METHOD_AUTOMATICALLY_RECORDED, weight.metadata.recordingMethod)
        assertEquals(Device.TYPE_SCALE, weight.metadata.device?.type)
        // The revision is the version: a fuller rewrite replaces this record, a repeat does not.
        assertEquals(4_000, weight.metadata.clientRecordVersion)
    }

    @Test
    fun `only what was measured, estimated and allowed is written`() {
        fun kinds(
            reading: ScaleReading = weighIn.reading,
            estimate: BodyCompositionEstimate? = composition,
            granted: Set<String> = ScaleWritePermissions,
        ) = scaleWeighInRecords(weighIn.copy(reading = reading), estimate, granted, madrid).map { it::class }

        assertEquals("socks on", listOf(WeightRecord::class), kinds(ScaleReading(weightKg = 69.9), estimate = null))
        assertEquals(
            "no profile to estimate from",
            listOf(WeightRecord::class, HeartRateRecord::class),
            kinds(estimate = null),
        )
        assertEquals(
            "weight is the only grant",
            listOf(WeightRecord::class),
            kinds(granted = setOf(ScaleRecordKind.WEIGHT.writePermission)),
        )
        assertEquals("the weight has not arrived", emptyList<Any>(), kinds(ScaleReading(impedanceHighOhm = 497.6)))
    }

    @Test
    fun `the scale's clock is believed only when it agrees with the phone's`() {
        val heardAt = Instant.ofEpochSecond(1744250605 + 20)

        assertEquals(time, scaleRecordTime(1744250605, heardAt))
        assertEquals("ten minutes slow", time, scaleRecordTime(1744250605, time.plusSeconds(600)))
        assertEquals("an hour off: a zone, not a weigh-in", heardAt, scaleRecordTime(1744250605 + 3600, heardAt))
        assertEquals("a clock that was never set", heardAt, scaleRecordTime(42, heardAt))
    }
}
