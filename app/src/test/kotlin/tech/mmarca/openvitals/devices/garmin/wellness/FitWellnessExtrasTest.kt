package tech.mmarca.openvitals.devices.garmin.wellness

import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.model.GarminWellnessMetric

/** The series the decoder once skipped, from hand-built FIT bytes. */
class FitWellnessExtrasTest {

    private val at: Instant = Instant.parse("2026-07-22T02:30:00Z")
    private val ts = listOf(253, 4, 134)

    @Test
    fun `Pulse Ox in the monitor file becomes one record per reading, apart from the snapshot's`() {
        // spo2 (269): timestamp, reading_spo2 (0, uint8), confidence (1), mode (2, enum).
        val d = FitW().fileId(32)
        d.def(1, 269, listOf(ts, listOf(0, 1, 2), listOf(1, 1, 2), listOf(2, 1, 0)))
        d.u8(1).u32(fitTimestamp(at)).u8(95).u8(90).u8(3)
        d.u8(1).u32(fitTimestamp(at.plusSeconds(60))).u8(0).u8(0).u8(3) // 0: no reading
        d.u8(1).u32(fitTimestamp(at.plusSeconds(120))).u8(97).u8(80).u8(1)

        val wellness = parseGarminWellness(fitWrap(d.toBytes()))

        assertEquals(listOf(at to 95, at.plusSeconds(120) to 97), wellness.extras.spo2)
        val records = fitExtrasImportRecords(wellness.extras)
        assertEquals(2, records.size)
        val first = records.first() as OxygenSaturationRecord
        assertEquals(95.0, first.percentage.value, 0.0)
        assertEquals("garmin_fit_spo2_${at.toEpochMilli()}", first.metadata.clientRecordId)
    }

    @Test
    fun `five-minute HRV values ride beside the nightly summary, in ms`() {
        // hrv_value (371): timestamp, value (0, uint16, scale 128).
        val d = FitW().fileId(68)
        d.def(1, 370, listOf(ts, listOf(1, 2, 0x84)))
        d.u8(1).u32(fitTimestamp(at.plusSeconds(3600))).u16(42 * 128)
        d.def(2, 371, listOf(ts, listOf(0, 2, 0x84)))
        d.u8(2).u32(fitTimestamp(at)).u16(40 * 128)
        d.u8(2).u32(fitTimestamp(at.plusSeconds(300))).u16(0xFFFF)
        d.u8(2).u32(fitTimestamp(at.plusSeconds(600))).u16(44 * 128 + 64)

        val wellness = parseGarminWellness(fitWrap(d.toBytes()))

        assertEquals(42.0, wellness.hrv!!.rmssdMillis, 0.0)
        assertEquals(listOf(at to 40.0, at.plusSeconds(600) to 44.5), wellness.extras.hrvValues)
        val records = fitExtrasImportRecords(wellness.extras).map { it as HeartRateVariabilityRmssdRecord }
        assertEquals(listOf(40.0, 44.5), records.map { it.heartRateVariabilityMillis })
        assertEquals("garmin_fit_hrv_value_${at.toEpochMilli()}", records.first().metadata.clientRecordId)
    }

    @Test
    fun `thresholds and scores land as watch-only samples at the watch's time`() {
        val d = FitW().fileId(44)
        // functional_metrics (356): ftp (4, uint16), lactate power (7, uint16), lactate hr (8, uint8).
        d.def(1, 356, listOf(ts, listOf(4, 2, 0x84), listOf(7, 2, 0x84), listOf(8, 1, 2)))
        d.u8(1).u32(fitTimestamp(at)).u16(250).u16(0xFFFF).u8(168)
        // hill_score (402): score, strength, endurance (uint8 each).
        d.def(2, 402, listOf(ts, listOf(0, 1, 2), listOf(1, 1, 2), listOf(2, 1, 2)))
        d.u8(2).u32(fitTimestamp(at.plusSeconds(10))).u8(61).u8(55).u8(0)
        // endurance_score (403): score (0, uint16).
        d.def(3, 403, listOf(ts, listOf(0, 2, 0x84)))
        d.u8(3).u32(fitTimestamp(at.plusSeconds(20))).u16(5400)

        val extras = parseGarminWellness(fitWrap(d.toBytes())).extras
        val samples = fitExtrasWatchOnlySamples(extras, nightStart = null)

        assertEquals(250, extras.functionalMetrics!!.functionalThresholdPowerWatts)
        // The uint16 sentinel is absent, not a threshold.
        assertNull(extras.functionalMetrics!!.runningLactateThresholdPowerWatts)
        // A zero part is a real score; a zero threshold is not.
        assertEquals(0, extras.hillScore!!.endurance)
        assertEquals(
            mapOf(
                GarminWellnessMetric.FUNCTIONAL_THRESHOLD_POWER to (at to 250L),
                GarminWellnessMetric.LACTATE_THRESHOLD_HEART_RATE to (at to 168L),
                GarminWellnessMetric.HILL_SCORE to (at.plusSeconds(10) to 61L),
                GarminWellnessMetric.HILL_STRENGTH to (at.plusSeconds(10) to 55L),
                GarminWellnessMetric.HILL_ENDURANCE to (at.plusSeconds(10) to 0L),
                GarminWellnessMetric.ENDURANCE_SCORE to (at.plusSeconds(20) to 5400L),
            ),
            samples.associate { it.metric to (it.time to it.value) },
        )
        // Nothing here has a Health Connect type.
        assertTrue(fitExtrasImportRecords(extras).isEmpty())
    }

    @Test
    fun `restless moments are keyed to the night, and dropped without one`() {
        // sleep_restless_moments (382): unknown (0, uint32), count (1, uint8), durations (2, uint8).
        val d = FitW().fileId(49)
        d.def(1, 382, listOf(listOf(0, 4, 0x86), listOf(1, 1, 2), listOf(2, 1, 2)))
        d.u8(1).u32(0).u8(7).u8(3)

        val extras = parseGarminWellness(fitWrap(d.toBytes())).extras

        assertEquals(7, extras.restlessMoments)
        val night = at.minusSeconds(3600)
        val sample = fitExtrasWatchOnlySamples(extras, nightStart = night).single()
        assertEquals(GarminWellnessMetric.SLEEP_RESTLESS_MOMENTS, sample.metric)
        assertEquals(night to 7L, sample.time to sample.value)
        assertTrue(fitExtrasWatchOnlySamples(extras, nightStart = null).isEmpty())
    }

    @Test
    fun `a file with only extras is not empty, and later files win the snapshots`() {
        val first = FitWellnessExtras(enduranceScore = FitEnduranceScore(at, 5000), spo2 = listOf(at to 95))
        val second = FitWellnessExtras(enduranceScore = FitEnduranceScore(at.plusSeconds(1), 5100), spo2 = listOf(at.plusSeconds(1) to 96))

        val merged = first.merge(second)

        assertEquals(5100, merged.enduranceScore!!.score)
        assertEquals(2, merged.spo2.size)
        assertTrue(FitWellnessExtras().isEmpty)
    }
}
