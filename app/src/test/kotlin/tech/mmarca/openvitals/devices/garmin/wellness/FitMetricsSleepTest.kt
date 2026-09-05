package tech.mmarca.openvitals.devices.garmin.wellness

import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The metrics file (type 44), Health Snapshot (type 70), intensity minutes and sleep extras, from hand-built FIT bytes. */
class FitMetricsSleepTest {

    private val at: Instant = Instant.parse("2026-07-22T06:30:00Z")

    // Health snapshot.

    @Test
    fun `unpacks an array field into one sample per interval`() {
        // Array fields: reading only the first element dropped a two-minute recording to its opening reading.
        val snapshot = parseGarminWellness(
            hsaFile(at = at, globalMessage = 305, intervalSeconds = 5, samples = listOf(96, 97, 97, 98)),
        ).healthSnapshot!!

        assertEquals(
            listOf(
                at to 96,
                at.plusSeconds(5) to 97,
                at.plusSeconds(10) to 97,
                at.plusSeconds(15) to 98,
            ),
            snapshot.spo2,
        )
    }

    @Test
    fun `SpO2 reaches Health Connect, stress does not`() {
        val spo2 = parseGarminWellness(
            hsaFile(at = at, globalMessage = 305, intervalSeconds = 60, samples = listOf(95, 96)),
        ).healthSnapshot!!
        val records = fitHealthSnapshotImportRecords(spo2)
        assertEquals(2, records.size)
        assertTrue(records.first() is OxygenSaturationRecord)
        // Namespaced apart from the all-day series, so a spot measurement does not overwrite the passive one.
        assertTrue(
            records.first().metadata.clientRecordId!!.startsWith("garmin_fit_hsa_spo2_"),
        )

        val stress = parseGarminWellness(
            hsaFile(at = at, globalMessage = 306, intervalSeconds = 60, samples = listOf(30, 35)),
        ).healthSnapshot!!
        assertEquals(2, stress.stress.size)
        // No Health Connect type for stress, so nothing to map.
        assertTrue(fitHealthSnapshotImportRecords(stress).isEmpty())
    }

    @Test
    fun `respiration is scaled by 100`() {
        val snapshot = parseGarminWellness(
            hsaFile(
                at = at,
                globalMessage = 307,
                intervalSeconds = 30,
                samples = listOf(1450, 1520),
                elementSize = 2,
                baseType = 131, // sint16
            ),
        ).healthSnapshot!!

        assertEquals(listOf(14.5, 15.2), snapshot.respiration.map { it.second })
    }

    @Test
    fun `a zero interval drops the record rather than stacking samples`() {
        // Otherwise every sample lands on the same instant and the (metric, time) key collapses them.
        val wellness = parseGarminWellness(
            hsaFile(at = at, globalMessage = 305, intervalSeconds = 0, samples = listOf(96, 97, 98)),
        )
        assertNull(wellness.healthSnapshot)
    }

    @Test
    fun `out-of-range readings are dropped`() {
        val snapshot = parseGarminWellness(
            hsaFile(at = at, globalMessage = 305, intervalSeconds = 10, samples = listOf(96, 0, 97)),
        ).healthSnapshot!!
        // 0% blood oxygen is a sentinel, not a reading.
        assertEquals(listOf(96, 97), snapshot.spo2.map { it.second })
        // The dropped sample must not shift the ones after it.
        assertEquals(at.plusSeconds(20), snapshot.spo2.last().first)
    }

    // Daily sleep, from the metrics file.

    private val endTime: Instant = Instant.parse("2026-07-22T07:20:00Z")

    @Test
    fun `reads awake duration as SECONDS not the profile's minutes`() {
        // From a real night: 1020 inside an 8.7-hour window. Read as minutes it would be 17 hours awake.
        val daily = parseGarminWellness(
            dailySleepFile(endTime = endTime, score = 71, awakeSeconds = 1020),
        ).dailySleep!!

        assertEquals(Duration.ofMinutes(17), daily.awakeDuration)
        assertEquals(71, daily.score)
        assertEquals(endTime, daily.endTime)
    }

    @Test
    fun `sleep pressure passes through raw including negatives`() {
        val daily = parseGarminWellness(
            dailySleepFile(endTime = endTime, pressure = -33),
        ).dailySleep!!
        // Undocumented scale; passed on as is.
        assertEquals(-33, daily.pressure)
    }

    @Test
    fun `reads Sleep Coach need against the usual need`() {
        val demand = parseGarminWellness(
            dailySleepFile(endTime = endTime, normalMinutes = 470, demandMinutes = 520),
        ).sleepDemand!!

        assertEquals(Duration.ofHours(7).plusMinutes(50), demand.normal)
        assertEquals(Duration.ofHours(8).plusMinutes(40), demand.demand)
    }

    @Test
    fun `a metrics file of only sleep data is not empty`() {
        // This watch puts no training-load messages in the metrics file, so the file used to be discarded.
        val wellness = parseGarminWellness(dailySleepFile(endTime = endTime, score = 71))

        assertFalse(wellness.isEmpty)
        assertNull(wellness.metrics) // no VO2 max / load in this file
        assertNotNull(wellness.dailySleep)
    }

    @Test
    fun `invalid sentinels do not become readings`() {
        val wellness = parseGarminWellness(dailySleepFile(endTime = endTime))
        assertNull(wellness.dailySleep)
        assertNull(wellness.sleepDemand)
    }

    // Intensity minutes.

    @Test
    fun `reads the running daily totals`() {
        val m = parseGarminWellness(
            intensityFile(
                listOf(
                    Triple(at, 12, 4),
                    Triple(at.plusSeconds(15 * 60), 19, 4),
                ),
            ),
        ).monitoring!!

        // Cumulative totals, kept as the watch wrote them. Differencing is the mapper's job.
        assertEquals(listOf(at to 12, at.plusSeconds(15 * 60) to 19), m.moderateMinutes)
        assertEquals(listOf(at to 4, at.plusSeconds(15 * 60) to 4), m.vigorousMinutes)
    }

    @Test
    fun `reads the alternate field pair too`() {
        val m = parseGarminWellness(
            intensityFile(listOf(Triple(at, 7, 2)), alt = true),
        ).monitoring!!
        assertEquals(listOf(at to 7), m.moderateMinutes)
        assertEquals(listOf(at to 2), m.vigorousMinutes)
    }

    @Test
    fun `zero is a real total and is kept`() {
        // The vívoactive 5 writes 0 all day until minutes are earned; dropping those hides "no data yet".
        val m = parseGarminWellness(intensityFile(listOf(Triple(at, 0, 0)))).monitoring!!
        assertEquals(listOf(at to 0), m.moderateMinutes)
        assertEquals(listOf(at to 0), m.vigorousMinutes)
    }

    @Test
    fun `the uint16 invalid sentinel is not a total`() {
        val m = parseGarminWellness(
            intensityFile(listOf(Triple(at, 0xFFFF, 0xFFFF))),
        ).monitoring
        assertTrue((m?.moderateMinutes ?: emptyList()).isEmpty())
        assertTrue((m?.vigorousMinutes ?: emptyList()).isEmpty())
    }

    // Metrics file.

    @Test
    fun `reads VO2 max, recovery, readiness and load from one file`() {
        val metrics = parseGarminWellness(
            metricsFile(
                at = at,
                vo2MaxTenths = 425,
                recoveryMinutes = 1320,
                readiness = 68,
                loadAcute = 412,
                loadChronic = 380,
            ),
        ).metrics!!

        assertEquals(42.5, metrics.vo2Max!!, 0.0001) // uint16 scale 10
        assertEquals(1320, metrics.recoveryTimeMinutes)
        assertEquals(68, metrics.trainingReadiness)
        assertEquals(412, metrics.trainingLoadAcute)
        assertEquals(380, metrics.trainingLoadChronic)
        assertEquals(at, metrics.time)
    }

    @Test
    fun `a file carrying only training load still yields metrics`() {
        // The watch re-offers metrics files constantly and most carry a subset.
        val wellness = parseGarminWellness(metricsFile(at = at, loadAcute = 300))

        assertNotNull(wellness.metrics)
        assertEquals(300, wellness.metrics!!.trainingLoadAcute)
        assertNull(wellness.metrics!!.vo2Max)
        assertFalse(wellness.isEmpty)
    }

    @Test
    fun `only VO2 max reaches Health Connect`() {
        val wellness = parseGarminWellness(
            metricsFile(at = at, vo2MaxTenths = 501, recoveryMinutes = 60, readiness = 80),
        )
        val records = fitMetricsImportRecords(wellness.metrics!!)

        // Recovery time and readiness have no Health Connect type.
        assertEquals(1, records.size)
        val record = records.single() as Vo2MaxRecord
        assertEquals(50.1, record.vo2MillilitersPerMinuteKilogram, 0.001)
        assertEquals(
            "garmin_fit_vo2max_${at.toEpochMilli()}",
            record.metadata.clientRecordId,
        )
    }

    @Test
    fun `a metrics file with no VO2 max maps to nothing`() {
        val wellness = parseGarminWellness(metricsFile(at = at, readiness = 70))
        assertTrue(fitMetricsImportRecords(wellness.metrics!!).isEmpty())
    }

    // Sleep extras.

    private val nightStart: Instant = Instant.parse("2026-07-22T00:10:00Z")
    private val nightEnd: Instant = Instant.parse("2026-07-22T07:20:00Z")

    @Test
    fun `carries the watch's own score alongside the derived stages`() {
        val sleep = parseGarminWellness(
            sleepFile(start = nightStart, end = nightEnd, score = 74, awakenings = 3),
        ).sleep!!

        // Both survive: the score is Garmin's verdict, the stages are ours.
        assertEquals(74, sleep.overallScore)
        assertEquals(3, sleep.awakeningsCount)
        assertTrue(sleep.stages.isNotEmpty())
    }

    @Test
    fun `a night without sleep_stats still parses`() {
        val sleep = parseGarminWellness(sleepFile(start = nightStart, end = nightEnd)).sleep!!
        assertNull(sleep.overallScore)
        assertNull(sleep.awakeningsCount)
        assertTrue(sleep.stages.isNotEmpty())
    }

    @Test
    fun `naps become their own sleep sessions recorded as one light stage`() {
        val napStart = Instant.parse("2026-07-22T14:00:00Z")
        val napEnd = Instant.parse("2026-07-22T14:35:00Z")
        val wellness = parseGarminWellness(
            sleepFile(start = nightStart, end = nightEnd, naps = listOf(napStart to napEnd)),
        )

        assertEquals(1, wellness.naps.size)
        val records = fitNapImportRecords(wellness.naps)
        val nap = records.single() as SleepSessionRecord
        assertEquals(napStart, nap.startTime)
        assertEquals(napEnd, nap.endTime)
        // The nap message has no stage breakdown, so the whole span is one light stage, as Gadgetbridge does.
        val stage = nap.stages.single()
        assertEquals(napStart, stage.startTime)
        assertEquals(napEnd, stage.endTime)
        assertEquals(SleepSessionRecord.STAGE_TYPE_LIGHT, stage.stage)
        assertEquals(
            "garmin_fit_nap_${napStart.toEpochMilli()}",
            nap.metadata.clientRecordId,
        )
    }

    @Test
    fun `a nap that ends before it starts is dropped`() {
        val wellness = parseGarminWellness(
            sleepFile(
                start = nightStart,
                end = nightEnd,
                naps = listOf(
                    Instant.parse("2026-07-22T15:00:00Z") to Instant.parse("2026-07-22T14:00:00Z"),
                ),
            ),
        )
        assertTrue(wellness.naps.isEmpty())
    }
}

// Builders.

/** A metrics file (type 44): VO2 max, recovery time, readiness and load, each in its own message. */
private fun metricsFile(
    at: Instant,
    vo2MaxTenths: Int? = null,
    recoveryMinutes: Int? = null,
    readiness: Int? = null,
    loadAcute: Int? = null,
    loadChronic: Int? = null,
): ByteArray {
    val d = FitW().fileId(44)
    if (vo2MaxTenths != null) {
        d.def(1, 229, listOf(listOf(253, 4, 134), listOf(2, 2, 132)))
        d.u8(0x01)
            .u32(fitTimestamp(at))
            .u16(vo2MaxTenths)
    }
    if (recoveryMinutes != null) {
        d.def(2, 140, listOf(listOf(253, 4, 134), listOf(9, 2, 132)))
        d.u8(0x02)
            .u32(fitTimestamp(at))
            .u16(recoveryMinutes)
    }
    if (readiness != null) {
        d.def(4, 369, listOf(listOf(253, 4, 134), listOf(0, 1, 2)))
        d.u8(0x04)
            .u32(fitTimestamp(at))
            .u8(readiness)
    }
    if (loadAcute != null || loadChronic != null) {
        d.def(5, 378, listOf(listOf(253, 4, 134), listOf(3, 2, 132), listOf(4, 2, 132)))
        d.u8(0x05)
            .u32(fitTimestamp(at))
            .u16(loadAcute ?: 0xFFFF)
            .u16(loadChronic ?: 0xFFFF)
    }
    return fitWrap(d.toBytes())
}

/** A sleep file (type 49): the event/74 bounds, one stage transition, sleep_stats and any naps. */
private fun sleepFile(
    start: Instant,
    end: Instant,
    score: Int? = null,
    awakenings: Int? = null,
    naps: List<Pair<Instant, Instant>> = emptyList(),
): ByteArray {
    val d = FitW().fileId(49)
    // event (21): field 0 event, 1 event_type, 253 timestamp.
    d.def(1, 21, listOf(listOf(253, 4, 134), listOf(0, 1, 0), listOf(1, 1, 0)))
    d.u8(0x01)
        .u32(fitTimestamp(start))
        .u8(74)
        .u8(0) // start
    d.u8(0x01)
        .u32(fitTimestamp(end))
        .u8(74)
        .u8(1) // stop
    // sleep_level (275): one transition to light. Its timestamp is the stage's end, so it sits at the stop.
    d.def(2, 275, listOf(listOf(253, 4, 134), listOf(0, 1, 0)))
    d.u8(0x02)
        .u32(fitTimestamp(end))
        .u8(2) // light
    if (score != null || awakenings != null) {
        d.def(4, 346, listOf(listOf(6, 1, 2), listOf(11, 1, 2)))
        d.u8(0x04)
            .u8(score ?: 0xFF)
            .u8(awakenings ?: 0xFF)
    }
    if (naps.isNotEmpty()) {
        d.def(5, 412, listOf(listOf(0, 4, 134), listOf(2, 4, 134)))
        for ((napStart, napEnd) in naps) {
            d.u8(0x05)
                .u32(fitTimestamp(napStart))
                .u32(fitTimestamp(napEnd))
        }
    }
    return fitWrap(d.toBytes())
}

/** A monitoring file (type 32) with intensity-minute totals. [alt] writes them into 33/34 instead of 37/38. */
private fun intensityFile(
    samples: List<Triple<Instant, Int, Int>>,
    alt: Boolean = false,
): ByteArray {
    val d = FitW().fileId(32)
    d.def(
        1,
        55,
        listOf(
            listOf(253, 4, 134),
            listOf(if (alt) 33 else 37, 2, 132),
            listOf(if (alt) 34 else 38, 2, 132),
        ),
    )
    for ((sampleAt, moderate, vigorous) in samples) {
        d.u8(0x01)
            .u32(fitTimestamp(sampleAt))
            .u16(moderate)
            .u16(vigorous)
    }
    return fitWrap(d.toBytes())
}

/** A metrics file as a vívoactive 5 writes it: daily_sleep (384) and sleep_demand (410), no training-load messages. */
private fun dailySleepFile(
    endTime: Instant,
    score: Int? = null,
    awakeSeconds: Int? = null,
    pressure: Int? = null,
    normalMinutes: Int? = null,
    demandMinutes: Int? = null,
    demandAt: Instant? = null,
): ByteArray {
    val d = FitW().fileId(44)
    d.def(
        1,
        384,
        listOf(
            listOf(2, 1, 2), // sleep_score, uint8
            listOf(3, 2, 132), // awake_duration, uint16
            listOf(11, 4, 134), // sleep_end_time, uint32
            listOf(22, 2, 131), // sleep_pressure, sint16
        ),
    )
    d.u8(0x01)
        .u8(score ?: 0xFF)
        .u16(awakeSeconds ?: 0xFFFF)
        .u32(fitTimestamp(endTime))
        .u16((pressure ?: 0x7FFF) and 0xFFFF)
    if (normalMinutes != null || demandMinutes != null) {
        d.def(2, 410, listOf(listOf(253, 4, 134), listOf(0, 2, 132), listOf(1, 2, 132)))
        d.u8(0x02)
            .u32(fitTimestamp(demandAt ?: endTime))
            .u16(normalMinutes ?: 0xFFFF)
            .u16(demandMinutes ?: 0xFFFF)
    }
    return fitWrap(d.toBytes())
}

/** A Health Snapshot file (type 70). Field 0 is the seconds between samples, field 1 an array. */
private fun hsaFile(
    at: Instant,
    globalMessage: Int,
    intervalSeconds: Int,
    samples: List<Int>,
    elementSize: Int = 1,
    baseType: Int = 2, // uint8
): ByteArray {
    val d = FitW().fileId(70)
    d.def(
        1,
        globalMessage,
        listOf(
            listOf(253, 4, 134), // timestamp, uint32
            listOf(0, 2, 132), // processing_interval, uint16
            listOf(1, samples.size * elementSize, baseType), // the array
        ),
    )
    d.u8(0x01)
        .u32(fitTimestamp(at))
        .u16(intervalSeconds)
    for (s in samples) {
        if (elementSize == 1) {
            d.u8(s)
        } else {
            d.u16(s)
        }
    }
    return fitWrap(d.toBytes())
}
