package tech.mmarca.openvitals.devices.garmin.wellness

import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.devices.garmin.FitCounterWatermark

/**
 * Sleep, HRV, monitoring and the intraday counter walk, decoded from hand-built FIT bytes.
 * The record ids match the Flutter build so the two builds dedup against each other.
 */
class FitWellnessImportTest {

    private fun utc(
        year: Int,
        month: Int,
        day: Int,
        hour: Int = 0,
        minute: Int = 0,
        second: Int = 0,
    ): Instant = LocalDateTime.of(year, month, day, hour, minute, second)
        .atZone(ZoneId.of("UTC"))
        .toInstant()

    private val start = utc(2024, 1, 1, 23, 0, 0)
    private val stop = utc(2024, 1, 2, 6, 0, 0)

    // (transition, sleep_level). Each timestamp is the end of the stage it names.
    // The last transition is the session stop.
    private val levels = listOf(
        utc(2024, 1, 1, 23, 10) to 2, // light: 23:00 -> 23:10
        utc(2024, 1, 1, 23, 40) to 3, // deep:  23:10 -> 23:40
        utc(2024, 1, 2, 0, 30) to 4, // rem:   23:40 -> 00:30
        utc(2024, 1, 2, 0, 45) to 1, // awake: 00:30 -> 00:45
        utc(2024, 1, 2, 6, 0) to 2, // light:  00:45 -> 06:00 (to stop)
    )

    // parseGarminSleepSession.

    @Test
    fun `reads the session bounds and a contiguous stage timeline`() {
        val session = parseGarminSleepSession(
            fitSleepBytes(start = start, stop = stop, levels = levels),
        )!!

        assertEquals(start, session.start)
        assertEquals(stop, session.end)
        assertEquals(5, session.stages.size)

        // Each stage ends at its transition; the first begins at the session start.
        assertEquals(FitSleepLevel.LIGHT, session.stages.first().level)
        assertEquals(start, session.stages.first().start)
        assertEquals(utc(2024, 1, 1, 23, 10), session.stages.first().end)
        assertEquals(FitSleepLevel.DEEP, session.stages[1].level)
        assertEquals(utc(2024, 1, 1, 23, 40), session.stages[1].end)
        assertEquals(FitSleepLevel.REM, session.stages[2].level)
        assertEquals(FitSleepLevel.LIGHT, session.stages.last().level)
        assertEquals(stop, session.stages.last().end)

        // Contiguous: every stage ends where the next begins.
        for (i in 0 until session.stages.size - 1) {
            assertEquals(session.stages[i].end, session.stages[i + 1].start)
        }
    }

    @Test
    fun `returns null when the file carries no sleep timeline`() {
        val session = parseGarminSleepSession(
            fitSleepBytes(start = start, stop = stop, levels = emptyList()),
        )
        assertNull(session)
    }

    // fitSleepImportRecords.

    @Test
    fun `maps to one SleepSessionRecord with a deterministic id`() {
        val session = parseGarminSleepSession(
            fitSleepBytes(start = start, stop = stop, levels = levels),
        )!!

        val records = fitSleepImportRecords(session)
        assertEquals(1, records.size)
        val record = records.single() as SleepSessionRecord

        assertEquals(
            "garmin_fit_sleep_${start.toEpochMilli()}",
            record.metadata.clientRecordId,
        )
        assertEquals(start, record.startTime)
        assertEquals(stop, record.endTime)
        assertEquals(
            listOf(
                SleepSessionRecord.STAGE_TYPE_LIGHT,
                SleepSessionRecord.STAGE_TYPE_DEEP,
                SleepSessionRecord.STAGE_TYPE_REM,
                SleepSessionRecord.STAGE_TYPE_AWAKE,
                SleepSessionRecord.STAGE_TYPE_LIGHT,
            ),
            record.stages.map { it.stage },
        )
    }

    @Test
    fun `drops unmeasurable spans which have no Health Connect stage`() {
        val withUnmeasurable = listOf(
            utc(2024, 1, 1, 23, 10) to 0, // unmeasurable
            utc(2024, 1, 1, 23, 30) to 2, // light
        )
        val session = parseGarminSleepSession(
            fitSleepBytes(start = start, stop = stop, levels = withUnmeasurable),
        )!!

        val record = fitSleepImportRecords(session).single() as SleepSessionRecord
        assertEquals(
            listOf(SleepSessionRecord.STAGE_TYPE_LIGHT),
            record.stages.map { it.stage },
        )
    }

    // HRV (type 68).

    private val hrvTime = utc(2024, 1, 2, 6, 0, 0)

    @Test
    fun `reads last_night_average as an RMSSD in ms`() {
        val wellness = parseGarminWellness(
            fitHrvBytes(time = hrvTime, rmssdMillis = 42.5),
        )
        assertNull(wellness.sleep)
        assertNotNull(wellness.hrv)
        assertEquals(hrvTime, wellness.hrv!!.time)
        // 42.5 ms -> raw round(42.5*128)=5440 -> 5440/128 = 42.5.
        assertEquals(42.5, wellness.hrv!!.rmssdMillis, 0.01)
    }

    @Test
    fun `maps to one HeartRateVariabilityRmssd record`() {
        val reading = parseGarminWellness(
            fitHrvBytes(time = hrvTime, rmssdMillis = 42.5),
        ).hrv!!
        val record = fitHrvImportRecords(reading).single() as HeartRateVariabilityRmssdRecord
        assertEquals(
            "garmin_fit_hrv_${hrvTime.toEpochMilli()}",
            record.metadata.clientRecordId,
        )
        assertEquals(42.5, record.heartRateVariabilityMillis, 0.01)
    }

    @Test
    fun `the invalid uint16 sentinel is not read as a reading`() {
        val wellness = parseGarminWellness(
            fitHrvBytes(time = hrvTime, rawOverride = 0xFFFF),
        )
        assertNull(wellness.hrv)
    }

    // Monitoring (type 32) summary.

    private val monitoringAt = utc(2024, 1, 18, 13, 42, 0)

    @Test
    fun `reads resting HR and BMR and maps to two records`() {
        val wellness = parseGarminWellness(
            fitMonitoringBytes(time = monitoringAt, restingHrBpm = 65, bmrKcalPerDay = 2265),
        )
        val m = wellness.monitoring!!
        assertEquals(65, m.restingHeartRateBpm)
        assertEquals(2265.0, m.bmrKcalPerDay!!, 0.0)

        val records = fitMonitoringImportRecords(m)
        assertEquals(2, records.size)
        val rhr = records[0] as RestingHeartRateRecord
        val bmr = records[1] as BasalMetabolicRateRecord
        assertEquals(65L, rhr.beatsPerMinute)
        assertEquals(
            "garmin_fit_resting_hr_${monitoringAt.toEpochMilli()}",
            rhr.metadata.clientRecordId,
        )
        assertEquals(2265.0, bmr.basalMetabolicRate.inKilocaloriesPerDay, 0.001)
    }

    @Test
    fun `a file with only resting HR maps to one record`() {
        val wellness = parseGarminWellness(
            fitMonitoringBytes(time = monitoringAt, restingHrBpm = 58),
        )
        val records = fitMonitoringImportRecords(wellness.monitoring!!)
        assertEquals(1, records.size)
        assertEquals(58L, (records.single() as RestingHeartRateRecord).beatsPerMinute)
    }

    // Monitoring (type 32) high-frequency series.

    @Test
    fun `HR packs hourly, respiration averages hourly, steps span the file`() {
        val bytes = fitMonitoringSeriesBytes(
            hr = listOf(
                utc(2024, 1, 18, 9, 10) to 70,
                utc(2024, 1, 18, 9, 40) to 72,
                utc(2024, 1, 18, 10, 10) to 68,
                utc(2024, 1, 18, 10, 40) to 74,
            ),
            respiration = listOf(
                utc(2024, 1, 18, 9, 15) to 13.0,
                utc(2024, 1, 18, 9, 45) to 15.0,
                utc(2024, 1, 18, 10, 15) to 14.0,
            ),
            stepsCumulative = listOf(
                utc(2024, 1, 18, 9, 0) to 0,
                utc(2024, 1, 18, 10, 0) to 500,
                utc(2024, 1, 18, 11, 0) to 1200,
            ),
        )
        val m = parseGarminWellness(bytes).monitoring!!
        val records = fitMonitoringImportRecords(m)

        val hr = records.filterIsInstance<HeartRateRecord>()
        assertEquals(2, hr.size) // one per hour (09:xx, 10:xx)
        assertEquals(4, hr.flatMap { it.samples }.size)

        val resp = records.filterIsInstance<RespiratoryRateRecord>().sortedBy { it.time }
        assertEquals(2, resp.size)
        assertEquals(14.0, resp.first().rate, 0.001) // avg(13,15)

        // Counters are accumulated across a sync and mapped once; see the intraday tests.
        assertTrue(records.filterIsInstance<StepsRecord>().isEmpty())
    }

    @Test
    fun `a typed message does not lend its type to the untyped one after it`() {
        // A counter record with no activity_type restates the whole day.
        // Inheriting the last declared type counted it as fresh steps.
        val data = FitW().fileId(32)
        data.def(4, 55, listOf(listOf(253, 4, 0x86), listOf(5, 1, 0x00), listOf(3, 4, 0x86)))
        data.u8(4)
            .u32(fitTimestamp(utc(2024, 1, 18, 12)))
            .u8(6) // walking
            .u32(500L)
        data.def(2, 55, listOf(listOf(253, 4, 0x86), listOf(3, 4, 0x86)))
        data.u8(2)
            .u32(fitTimestamp(utc(2024, 1, 18, 12, 1)))
            .u32(620L) // the day's total, restated with no type of its own
        val m = parseGarminWellness(fitWrap(data.toBytes())).monitoring!!

        assertEquals(2, m.stepPoints.size)
        assertEquals(6, m.stepPoints[0].activityType)
        assertEquals(UNKNOWN_FIT_ACTIVITY_TYPE, m.stepPoints[1].activityType)

        // The untyped rule drops the restatement instead of minting 120 steps.
        val import = fitMonitoringCounterRecords(fitMonitoringCounters(m), zone = zone)
        assertEquals(500, stepsTotal(import))
    }

    // Intraday counters.

    private val zone: ZoneId = ZoneId.of("Europe/Madrid")

    private fun local(
        year: Int,
        month: Int,
        day: Int,
        hour: Int = 0,
        minute: Int = 0,
    ): Instant = LocalDateTime.of(year, month, day, hour, minute)
        .atZone(zone)
        .toInstant()

    /** Maps the counters as one sync would: accumulated across files, then differenced against [previous]. */
    private fun counterImport(
        stepsCumulative: List<Pair<Instant, Int>> = emptyList(),
        typedStepsCumulative: List<Triple<Instant, Int, Int>> = emptyList(),
        caloriesCumulative: List<Pair<Instant, Int>> = emptyList(),
        previous: Map<String, FitCounterWatermark> = emptyMap(),
        zone: ZoneId = this.zone,
    ): FitCounterImport {
        val monitoring = parseGarminWellness(
            fitMonitoringSeriesBytes(
                stepsCumulative = stepsCumulative,
                typedStepsCumulative = typedStepsCumulative,
                caloriesCumulative = caloriesCumulative,
            ),
        ).monitoring!!
        return fitMonitoringCounterRecords(
            fitMonitoringCounters(monitoring),
            previous = previous,
            zone = zone,
        )
    }

    @Test
    fun `a time zone change between syncs does not double steps`() {
        // Madrid -> Tokyo (+7h) overnight: the same instants fall on the next local day.
        // The reset is read from the counter, so day keys may move but differences must not.
        val madrid = zone
        val tokyo = ZoneId.of("Asia/Tokyo")
        val evening = counterImport(
            stepsCumulative = listOf(
                local(2024, 1, 18, 20, 0) to 5_000,
                local(2024, 1, 18, 23, 30) to 6_000,
            ),
            zone = madrid,
        )
        // A first sync has no yesterday, so the opening reading is the day's accrual.
        assertEquals(6_000, stepsTotal(evening))

        val afterLanding = counterImport(
            stepsCumulative = listOf(
                // The watermark reading again, now on Tokyo's Jan 19.
                local(2024, 1, 18, 23, 30) to 6_000,
                local(2024, 1, 18, 23, 45) to 6_100,
                // The watch closes its day after midnight and the counter restarts.
                local(2024, 1, 19, 0, 30) to 50,
                local(2024, 1, 19, 1, 0) to 250,
            ),
            previous = evening.watermarks,
            zone = tokyo,
        )

        // 6000 -> 6100 is 100. The reset is a rollover, so the 50 before the next reading are not claimed.
        // 50 -> 250 is 200. The 6000 must not count twice.
        assertEquals(100 + 200, stepsTotal(afterLanding))
        // Nothing after landing starts before the watermark, so no span overlaps.
        val watermarkAt = local(2024, 1, 18, 23, 30)
        steps(afterLanding).forEach { record ->
            assertTrue(record.clientRecordId, !record.startTime.isBefore(watermarkAt))
        }
    }

    private fun steps(import: FitCounterImport): List<StepsRecord> =
        import.records.filterIsInstance<StepsRecord>().sortedBy { it.startTime }

    private fun stepsTotal(import: FitCounterImport): Int =
        steps(import).sumOf { it.count }.toInt()

    private val Record.clientRecordId: String
        get() = metadata.clientRecordId!!

    @Test
    fun `a day of counters becomes intraday records not one flat total`() {
        // One record per day had no timing, so Health Connect drew a straight ramp from midnight.
        val import = counterImport(
            stepsCumulative = listOf(
                local(2024, 1, 18, 9) to 0,
                local(2024, 1, 18, 10) to 500,
                local(2024, 1, 18, 11) to 1200,
            ),
        )
        val stepRecords = steps(import)

        // 09:00 read zero, so nothing happened before it worth recording.
        assertEquals(2, stepRecords.size)
        assertEquals(local(2024, 1, 18, 9), stepRecords[0].startTime)
        assertEquals(local(2024, 1, 18, 10), stepRecords[0].endTime)
        assertEquals(500L, stepRecords[0].count)
        assertEquals(700L, stepRecords[1].count)
        // And they still add up.
        assertEquals(1200, stepsTotal(import))
    }

    @Test
    fun `what came before the first reading is not lost`() {
        // A first sync at noon: the counter has nothing to difference against,
        // so the steps go on the stretch from midnight.
        val import = counterImport(
            stepsCumulative = listOf(local(2024, 1, 18, 12) to 8000),
        )
        val record = steps(import).single()

        assertEquals(local(2024, 1, 18), record.startTime)
        assertEquals(local(2024, 1, 18, 12), record.endTime)
        assertEquals(8000L, record.count)
    }

    @Test
    fun `standing still writes nothing`() {
        val import = counterImport(
            stepsCumulative = listOf(
                local(2024, 1, 18, 9) to 500,
                local(2024, 1, 18, 10) to 500,
                local(2024, 1, 18, 11) to 500,
            ),
        )

        // One record before 09:00 and nothing for the empty hours after it.
        assertEquals(1, steps(import).size)
        assertEquals(500, stepsTotal(import))
    }

    @Test
    fun `the next sync carries on from the watermark not from midnight`() {
        // Each file holds only the minutes since the last sync, so the steps across
        // the seam are in neither file's own differences.
        val first = counterImport(
            stepsCumulative = listOf(
                local(2024, 1, 18, 9) to 500,
                local(2024, 1, 18, 10) to 900,
            ),
        )
        val second = counterImport(
            stepsCumulative = listOf(
                local(2024, 1, 18, 11) to 1500,
                local(2024, 1, 18, 12) to 1700,
            ),
            previous = first.watermarks,
        )

        // 900 -> 1500 across the seam, then 1500 -> 1700.
        assertEquals(local(2024, 1, 18, 10), steps(second).first().startTime)
        assertEquals(600L, steps(second).first().count)
        // Every step once.
        assertEquals(1700, stepsTotal(first) + stepsTotal(second))
    }

    @Test
    fun `a sync resuming inside a bucket does not overlap the record before it`() {
        // A real day read 889 in Health Connect and 1007 in its records: consecutive records overlapped.
        // A record's end follows the data, but the next sync resumed at the grid slot containing that end.
        val first = counterImport(
            stepsCumulative = listOf(
                local(2024, 1, 18, 9, 31) to 0,
                local(2024, 1, 18, 10, 59) to 253,
            ),
        )
        val second = counterImport(
            stepsCumulative = listOf(local(2024, 1, 18, 11, 28) to 354),
            previous = first.watermarks,
        )

        // One record from the 09:30 slot to where the counters next spoke.
        assertEquals(local(2024, 1, 18, 9, 30), steps(first).single().startTime)
        assertEquals(local(2024, 1, 18, 10, 59), steps(first).single().endTime)

        // The next one starts there, not at its slot's 10:45.
        assertEquals(local(2024, 1, 18, 10, 59), steps(second).single().startTime)
        assertEquals(101L, steps(second).single().count)
        assertEquals(354, stepsTotal(first) + stepsTotal(second))
    }

    @Test
    fun `a re-opened bucket keeps the start it was first written with`() {
        // The next sync rewrites the open bucket under the same id.
        // Re-deriving its start from the grid would overlap the predecessor again.
        val first = counterImport(
            stepsCumulative = listOf(
                local(2024, 1, 18, 9, 31) to 0,
                local(2024, 1, 18, 10, 46) to 253,
            ),
        )
        val second = counterImport(
            stepsCumulative = listOf(local(2024, 1, 18, 10, 52) to 300),
            previous = first.watermarks,
        )
        val third = counterImport(
            stepsCumulative = listOf(local(2024, 1, 18, 10, 58) to 320),
            previous = second.watermarks,
        )

        // Second and third write the same record; the third's 67 replaces the second's 47.
        assertEquals(steps(second).single().clientRecordId, steps(third).single().clientRecordId)
        assertEquals(47L, steps(second).single().count)
        assertEquals(67L, steps(third).single().count)

        // And it still begins where the first sync's record left off.
        assertEquals(local(2024, 1, 18, 10, 46), steps(second).single().startTime)
        assertEquals(local(2024, 1, 18, 10, 46), steps(third).single().startTime)
        assertEquals(320, stepsTotal(first) + stepsTotal(third))
    }

    @Test
    fun `the day's last movement is written not left for a sync that never comes`() {
        // The final bucket of a day used to wait for a next sync that belongs to the next day,
        // so it was never written.
        val import = counterImport(
            stepsCumulative = listOf(
                local(2024, 1, 18, 9) to 100,
                local(2024, 1, 18, 9, 16) to 150,
                local(2024, 1, 18, 9, 20) to 250,
            ),
        )

        assertEquals(250, stepsTotal(import))
        val last = steps(import).last()
        assertEquals(local(2024, 1, 18, 9, 15), last.startTime)
        assertEquals(100L, last.count)
    }

    @Test
    fun `the open bucket is rewritten in full next sync under the same id`() {
        // A half-filled bucket is safe: its id follows the clock, and the next sync rewrites the whole bucket.
        val first = counterImport(
            stepsCumulative = listOf(
                local(2024, 1, 18, 9) to 100,
                local(2024, 1, 18, 9, 16) to 150,
                local(2024, 1, 18, 9, 20) to 200,
            ),
        )
        val firstOpen = steps(first).last()
        assertEquals(50L, firstOpen.count)

        val second = counterImport(
            stepsCumulative = listOf(
                local(2024, 1, 18, 9, 25) to 275,
                local(2024, 1, 18, 9, 40) to 300,
            ),
            previous = first.watermarks,
        )

        val rewritten = steps(second).single { it.clientRecordId == firstOpen.clientRecordId }
        assertEquals(150L, rewritten.count)
        // The latest version of each id is exactly the wrist's total.
        val latest = mutableMapOf<String, Long>()
        for (record in steps(first)) latest[record.clientRecordId] = record.count
        for (record in steps(second)) latest[record.clientRecordId] = record.count
        assertEquals(300L, latest.values.sum())
    }

    @Test
    fun `re-importing a file already behind the watermark writes nothing`() {
        // 540 steps became 1403 over thirteen syncs: the watch re-offers files,
        // and a sync re-reads a file it was halfway through.
        val cumulative = listOf(
            local(2024, 1, 18, 9) to 200,
            local(2024, 1, 18, 10) to 540,
        )
        val first = counterImport(stepsCumulative = cumulative)
        val again = counterImport(stepsCumulative = cumulative, previous = first.watermarks)

        assertEquals(540, stepsTotal(first))
        assertTrue(again.records.isEmpty())
    }

    @Test
    fun `a counter rollover is not a walk backwards and not a full stop`() {
        // The counters restart when the watch rolls its day over, some time after midnight.
        val import = counterImport(
            stepsCumulative = listOf(
                local(2024, 1, 18, 9) to 900,
                local(2024, 1, 18, 10) to 0,
                local(2024, 1, 18, 11) to 300,
            ),
        )

        // The 900 stands, the rollover adds nothing, and the 300 after it are real.
        assertEquals(1200, stepsTotal(import))
    }

    @Test
    fun `a morning sync does not carry yesterday onto today`() {
        // Counters do not roll over at local midnight, so post-midnight messages still carry yesterday's totals.
        val import = counterImport(
            stepsCumulative = listOf(
                local(2026, 7, 27, 22) to 6100,
                local(2026, 7, 27, 23, 30) to 6123,
                // Past midnight, still counting from yesterday.
                local(2026, 7, 28, 0, 20) to 6123,
                local(2026, 7, 28, 8, 40) to 6132,
            ),
        )

        val today = steps(import)
            .filter { it.startTime.atZone(zone).dayOfMonth == 28 }
            .sumOf { it.count }
        assertEquals("today walked nine steps, not a whole day", 9L, today)
        assertEquals(6132, stepsTotal(import))
    }

    @Test
    fun `yesterday carries over from its watermark not just from this run`() {
        // Yesterday's file was archived two syncs ago; the watermark is the only record of the counter.
        val yesterday = counterImport(
            stepsCumulative = listOf(
                local(2026, 7, 27, 22) to 6100,
                local(2026, 7, 27, 23, 30) to 6123,
            ),
        )
        val morning = counterImport(
            stepsCumulative = listOf(
                local(2026, 7, 28, 0, 20) to 6123,
                local(2026, 7, 28, 8, 40) to 6132,
            ),
            previous = yesterday.watermarks,
        )

        assertTrue(stepsTotal(morning) < 100)
    }

    @Test
    fun `a day still starts from zero once the counter has rolled over`() {
        // Afternoon sync after the rollover: the first reading is below yesterday's end,
        // so those steps are the day's own.
        val yesterday = counterImport(
            stepsCumulative = listOf(local(2026, 7, 27, 23, 30) to 9000),
        )
        val today = counterImport(
            stepsCumulative = listOf(local(2026, 7, 28, 14) to 4000),
            previous = yesterday.watermarks,
        )

        assertEquals(4000, stepsTotal(today))
    }

    @Test
    fun `a carry is not spent across a gap of days`() {
        // A watch left in a drawer: 26 Jul's total says nothing about 28 Jul.
        val earlier = counterImport(
            stepsCumulative = listOf(local(2026, 7, 26, 23) to 3000),
        )
        val later = counterImport(
            stepsCumulative = listOf(local(2026, 7, 28, 14) to 9000),
            previous = earlier.watermarks,
        )

        assertEquals(9000, stepsTotal(later))
    }

    @Test
    fun `activity-type counters are summed never subtracted`() {
        // A walking counter at 540 beside a generic one at 0 is not a 540-step change.
        assertEquals(
            540,
            stepsTotal(
                counterImport(
                    stepsCumulative = listOf(
                        local(2024, 1, 18, 9) to 540,
                        local(2024, 1, 18, 10) to 0,
                    ),
                ),
            ),
        )
    }

    @Test
    fun `a total moved between activity types is not counted twice`() {
        // 24,724 steps reached Health Connect as 49,448.
        // The watch moves a total between buckets and zeroes the one it left.
        assertEquals(
            24724,
            stepsTotal(
                counterImport(
                    typedStepsCumulative = listOf(
                        Triple(local(2024, 1, 18, 9), 0, 24724),
                        Triple(local(2024, 1, 18, 9), 6, 0),
                        // The gaining bucket comes first on purpose: a point-by-point sum would see 24,724 twice.
                        Triple(local(2024, 1, 18, 10), 6, 24724),
                        Triple(local(2024, 1, 18, 10), 0, 0),
                    ),
                ),
            ),
        )
    }

    @Test
    fun `types still add up when they hold different totals`() {
        // Walking and running are separate counters; the day is their sum.
        assertEquals(
            24843,
            stepsTotal(
                counterImport(
                    typedStepsCumulative = listOf(
                        Triple(local(2024, 1, 18, 10), 0, 0),
                        Triple(local(2024, 1, 18, 10), 6, 24724),
                        Triple(local(2024, 1, 18, 10), 1, 119),
                    ),
                ),
            ),
        )
    }

    @Test
    fun `a counter naming no activity is not a bucket of its own`() {
        // An untyped counter beside typed ones is the same total under another name.
        assertEquals(
            24724,
            stepsTotal(
                counterImport(
                    stepsCumulative = listOf(local(2024, 1, 18, 9) to 24724),
                    typedStepsCumulative = listOf(Triple(local(2024, 1, 18, 9), 6, 24724)),
                ),
            ),
        )
    }

    @Test
    fun `a type absent from a sync's first readings is not the day again`() {
        // 6,323 steps reached Health Connect as 19,906. A sync restates only recently active types,
        // so the summed reading dipped below the watermark and read as a rollover.
        val first = counterImport(
            typedStepsCumulative = listOf(
                Triple(local(2026, 7, 30, 9), 0, 400),
                Triple(local(2026, 7, 30, 9), 6, 3000),
            ),
        )
        val second = counterImport(
            typedStepsCumulative = listOf(
                Triple(local(2026, 7, 30, 15, 0), 6, 3005),
                Triple(local(2026, 7, 30, 15, 20), 6, 3520),
                Triple(local(2026, 7, 30, 15, 40), 0, 400),
            ),
            previous = first.watermarks,
        )

        assertEquals(3400, stepsTotal(first))
        assertEquals("walking grew 3000 to 3520; generic never moved", 520, stepsTotal(second))
    }

    @Test
    fun `yesterday's counter restated unchanged overnight writes nothing`() {
        // Restating one type made the sum fall short of the watermark and read as a rollover.
        // Per type, an unchanged reading is a continuation.
        val yesterday = counterImport(
            typedStepsCumulative = listOf(
                Triple(local(2026, 7, 29, 22), 0, 3506),
                Triple(local(2026, 7, 29, 22), 6, 2817),
            ),
        )
        val morning = counterImport(
            typedStepsCumulative = listOf(
                Triple(local(2026, 7, 30, 4, 30), 0, 3506),
            ),
            previous = yesterday.watermarks,
        )

        assertEquals(0, stepsTotal(morning))
    }

    @Test
    fun `a genuinely reset type still counts from zero across midnight`() {
        // Walking restates below yesterday's end, so the 350 are today's.
        val yesterday = counterImport(
            typedStepsCumulative = listOf(Triple(local(2026, 7, 29, 22), 6, 2817)),
        )
        val today = counterImport(
            typedStepsCumulative = listOf(Triple(local(2026, 7, 30, 14), 6, 350)),
            previous = yesterday.watermarks,
        )

        assertEquals(350, stepsTotal(today))
    }

    @Test
    fun `a watermark from before the per-type maps never re-counts the day`() {
        // Upgrading mid-day: the summed watermark knows no types,
        // so restated types are adopted silently and only growth counts.
        val legacy = mapOf(
            "2026-07-30" to FitCounterWatermark(
                time = local(2026, 7, 30, 9),
                steps = 3400,
            ),
        )
        val sync = counterImport(
            typedStepsCumulative = listOf(
                Triple(local(2026, 7, 30, 15, 0), 6, 3005),
                Triple(local(2026, 7, 30, 15, 20), 6, 3200),
            ),
            previous = legacy,
        )

        assertEquals(195, stepsTotal(sync))
    }

    @Test
    fun `an untyped counter still counts when it is all the file has`() {
        // Dropping it would report zero steps for a file with no typed counter at all.
        assertEquals(
            1200,
            stepsTotal(
                counterImport(
                    stepsCumulative = listOf(
                        local(2024, 1, 18, 9) to 500,
                        local(2024, 1, 18, 10) to 1200,
                    ),
                ),
            ),
        )
    }

    // Incremental files in the same hour.

    private fun monitoringWith(
        hr: List<Pair<Instant, Int>>,
        respiration: List<Pair<Instant, Double>> = emptyList(),
    ): FitMonitoringSummary = FitMonitoringSummary(
        heartRateSamples = hr,
        respiration = respiration,
    )

    @Test
    fun `two HR chunks in one hour produce two distinct records`() {
        // Two sync windows in the same hour must not share an id.
        val first = fitMonitoringImportRecords(
            monitoringWith(
                hr = listOf(
                    utc(2026, 7, 22, 10, 5) to 70,
                    utc(2026, 7, 22, 10, 6) to 71,
                ),
            ),
        ).filterIsInstance<HeartRateRecord>().single()
        val second = fitMonitoringImportRecords(
            monitoringWith(
                hr = listOf(
                    utc(2026, 7, 22, 10, 40) to 74,
                    utc(2026, 7, 22, 10, 41) to 75,
                ),
            ),
        ).filterIsInstance<HeartRateRecord>().single()

        assertNotEquals(first.metadata.clientRecordId, second.metadata.clientRecordId)
    }

    @Test
    fun `re-importing the same chunk stays idempotent`() {
        val samples = listOf(
            utc(2026, 7, 22, 10, 5) to 70,
            utc(2026, 7, 22, 10, 6) to 71,
        )
        val a = fitMonitoringImportRecords(monitoringWith(hr = samples))
            .filterIsInstance<HeartRateRecord>().single()
        val b = fitMonitoringImportRecords(monitoringWith(hr = samples))
            .filterIsInstance<HeartRateRecord>().single()

        // Same data, same id, so a repeat sync overwrites itself.
        assertEquals(a.metadata.clientRecordId, b.metadata.clientRecordId)
    }

    @Test
    fun `a whole-day file still yields one record per hour`() {
        val records = fitMonitoringImportRecords(
            monitoringWith(hr = (0 until 24).map { h -> utc(2026, 7, 22, h, 30) to 60 + h }),
        ).filterIsInstance<HeartRateRecord>()

        assertEquals(24, records.size)
        assertEquals(24, records.map { it.metadata.clientRecordId }.toSet().size)
    }

    @Test
    fun `respiration is keyed and timed on its first reading`() {
        val record = fitMonitoringImportRecords(
            monitoringWith(
                hr = emptyList(),
                respiration = listOf(
                    utc(2026, 7, 22, 10, 5) to 14.0,
                    utc(2026, 7, 22, 10, 6) to 16.0,
                ),
            ),
        ).filterIsInstance<RespiratoryRateRecord>().single()

        // Not the top of the hour, which every file in that hour would claim.
        assertEquals(utc(2026, 7, 22, 10, 5), record.time)
        assertEquals(15.0, record.rate, 0.001)
    }

    // Counter record identity.

    /** A later record with the same clientRecordId replaces the earlier one. */
    private fun upserted(imports: List<FitCounterImport>): Map<String, StepsRecord> {
        val byId = mutableMapOf<String, StepsRecord>()
        for (import in imports) {
            for (record in steps(import)) {
                byId[record.clientRecordId] = record
            }
        }
        return byId
    }

    @Test
    fun `a re-sync from a lost watermark replaces rather than accumulates`() {
        // 49,695 steps for a 24,844 day: the id came from a walking cursor, so a re-sync re-partitioned the day.
        val first = counterImport(
            stepsCumulative = (0 until 180 step 5).map { minute ->
                local(2024, 1, 18, 9).plusSeconds(minute * 60L) to minute * 10
            },
        )
        // The watermark is gone, so the day is walked again with different instants.
        val relearned = counterImport(
            stepsCumulative = (0 until 180 step 7).map { minute ->
                local(2024, 1, 18, 9).plusSeconds(minute * 60L) to minute * 10
            },
        )

        val stored = upserted(listOf(first, relearned))
        val total = stored.values.sumOf { it.count }
        assertTrue("the day must not be counted twice", total <= 1790)

        // And no two stored records may cover the same minute.
        val spans = stored.values.sortedBy { it.startTime }
        for (i in 1 until spans.size) {
            assertFalse(
                "records overlap: ${spans[i - 1].clientRecordId} and ${spans[i].clientRecordId}",
                spans[i].startTime.isBefore(spans[i - 1].endTime),
            )
        }
    }

    @Test
    fun `the same minutes always produce the same id`() {
        val cumulative = listOf(
            local(2024, 1, 18, 9) to 0,
            local(2024, 1, 18, 9, 20) to 400,
            local(2024, 1, 18, 10) to 900,
        )

        assertEquals(
            steps(counterImport(stepsCumulative = cumulative)).map { it.clientRecordId },
            steps(counterImport(stepsCumulative = cumulative)).map { it.clientRecordId },
        )
    }

    @Test
    fun `the day's first record keeps the legacy day-keyed id`() {
        // Daily records `garmin_fit_steps_<yyyy-mm-dd>` are still in Health Connect.
        // Reusing the id makes the first bucket overwrite the stale one.
        val import = counterImport(
            stepsCumulative = listOf(
                local(2024, 1, 18, 6) to 300,
                local(2024, 1, 18, 10) to 900,
            ),
        )

        assertEquals("garmin_fit_steps_2024-01-18", steps(import).first().clientRecordId)
    }

    @Test
    fun `the legacy day key is handed out once not re-handed each sync`() {
        // One-shot: recomputing the first bucket every sync would move the id and lose minutes.
        val first = counterImport(
            stepsCumulative = listOf(
                local(2024, 1, 18, 6) to 300,
                local(2024, 1, 18, 10) to 900,
            ),
        )
        assertEquals("garmin_fit_steps_2024-01-18", steps(first).first().clientRecordId)
        assertTrue(first.watermarks.getValue("2024-01-18").legacyRetired)

        val second = counterImport(
            stepsCumulative = listOf(
                local(2024, 1, 18, 14) to 1500,
                local(2024, 1, 18, 18) to 2100,
            ),
            previous = first.watermarks,
        )

        assertFalse(
            "the second sync must not steal the id from the first bucket",
            steps(second).any { it.clientRecordId == "garmin_fit_steps_2024-01-18" },
        )
        assertTrue(second.watermarks.getValue("2024-01-18").legacyRetired)
    }

    @Test
    fun `a first sync that only touched the open bucket retires the legacy id later`() {
        // The first sync's only bucket is still filling and goes out under its grid id.
        // The legacy id waits for a bucket that never wore one.
        val first = counterImport(
            stepsCumulative = listOf(local(2024, 1, 18, 0, 5) to 300),
        )
        assertEquals(1, steps(first).size)
        assertNotEquals("garmin_fit_steps_2024-01-18", steps(first).single().clientRecordId)
        assertEquals(300L, steps(first).single().count)
        assertFalse(first.watermarks.getValue("2024-01-18").legacyRetired)

        val second = counterImport(
            stepsCumulative = listOf(
                local(2024, 1, 18, 6) to 300,
                local(2024, 1, 18, 10) to 900,
            ),
            previous = first.watermarks,
        )

        val ids = steps(second).map { it.clientRecordId }
        assertTrue(ids.contains("garmin_fit_steps_2024-01-18"))
        assertTrue(
            "the previously open bucket is rewritten under its grid id, never day-keyed",
            ids.contains(steps(first).single().clientRecordId),
        )
        assertTrue(second.watermarks.getValue("2024-01-18").legacyRetired)
    }

    @Test
    fun `calories ride the same grid as steps`() {
        // Calories share the id derivation with steps.
        val import = counterImport(
            stepsCumulative = listOf(
                local(2024, 1, 18, 9) to 0,
                local(2024, 1, 18, 9, 20) to 400,
            ),
            caloriesCumulative = listOf(
                local(2024, 1, 18, 9) to 0,
                local(2024, 1, 18, 9, 20) to 80,
            ),
        )

        val calories = import.records.filterIsInstance<ActiveCaloriesBurnedRecord>()
        assertTrue(calories.isNotEmpty())
        val calorieDerivedStepIds = calories.map {
            it.metadata.clientRecordId!!.replaceFirst("active_cal", "steps")
        }
        assertTrue(
            "both counters must share one grid, or their records drift apart across re-syncs",
            calorieDerivedStepIds.containsAll(steps(import).map { it.clientRecordId }),
        )
        val kilocalories = calories.sumOf { it.energy.inKilocalories }
        assertEquals(80, kilocalories.roundToInt())
        assertTrue(abs(kilocalories - 80.0) < 0.001)
    }
}

// Hand-built FIT byte builders.

private val tsField = listOf(253, 4, 0x86) // timestamp, uint32
private val enumField0 = listOf(0, 1, 0x00) // field 0, enum/uint8

internal fun fitSleepBytes(
    start: Instant,
    stop: Instant,
    levels: List<Pair<Instant, Int>>,
): ByteArray {
    val data = FitW()

    // file_id (type = 49, sleep)
    data.fileId(FIT_FILE_TYPE_SLEEP)

    // event (21): timestamp, event, event_type.
    data.def(1, 21, listOf(tsField, listOf(0, 1, 0x00), listOf(1, 1, 0x00)))
    data.u8(1)
        .u32(fitTimestamp(start))
        .u8(74) // event = sleep
        .u8(0) // event_type = start
    data.u8(1)
        .u32(fitTimestamp(stop))
        .u8(74)
        .u8(1) // event_type = stop

    // sleep_level (275): timestamp, sleep_level.
    data.def(2, 275, listOf(tsField, enumField0))
    for ((at, level) in levels) {
        data.u8(2)
            .u32(fitTimestamp(at))
            .u8(level)
    }

    return fitWrap(data.toBytes())
}

internal fun fitHrvBytes(
    time: Instant,
    rmssdMillis: Double? = null,
    rawOverride: Int? = null,
): ByteArray {
    val raw = rawOverride ?: Math.round(rmssdMillis!! * 128).toInt()
    val data = FitW().fileId(68) // file_id type 68 (HRV)

    // hrv_status_summary (370): timestamp, last_night_average (field 1, uint16).
    data.def(1, 370, listOf(tsField, listOf(1, 2, 0x84)))
    data.u8(1)
        .u32(fitTimestamp(time))
        .u16(raw)

    return fitWrap(data.toBytes())
}

internal fun fitMonitoringBytes(
    time: Instant,
    restingHrBpm: Int? = null,
    bmrKcalPerDay: Int? = null,
): ByteArray {
    val data = FitW().fileId(32) // file_id type 32 (monitoring_b)

    if (restingHrBpm != null) {
        // monitoring_hr_data (211): timestamp, resting_heart_rate (field 0, uint8).
        data.def(1, 211, listOf(tsField, listOf(0, 1, 0x02)))
        data.u8(1)
            .u32(fitTimestamp(time))
            .u8(restingHrBpm)
    }
    if (bmrKcalPerDay != null) {
        // monitoring_info (103): timestamp, resting_metabolic_rate (field 5, uint16).
        data.def(2, 103, listOf(tsField, listOf(5, 2, 0x84)))
        data.u8(2)
            .u32(fitTimestamp(time))
            .u16(bmrKcalPerDay)
    }

    return fitWrap(data.toBytes())
}

internal fun fitMonitoringSeriesBytes(
    hr: List<Pair<Instant, Int>> = emptyList(),
    respiration: List<Pair<Instant, Double>> = emptyList(),
    stepsCumulative: List<Pair<Instant, Int>> = emptyList(),
    typedStepsCumulative: List<Triple<Instant, Int, Int>> = emptyList(),
    caloriesCumulative: List<Pair<Instant, Int>> = emptyList(),
): ByteArray {
    val data = FitW().fileId(32) // file_id type 32

    // monitoring HR (local 1, global 55): timestamp + heart_rate (uint8).
    data.def(1, 55, listOf(tsField, listOf(27, 1, 0x02)))
    for ((t, bpm) in hr) {
        data.u8(1)
            .u32(fitTimestamp(t))
            .u8(bpm)
    }
    // monitoring steps (local 2, global 55): timestamp + cumulative steps (uint32).
    data.def(2, 55, listOf(tsField, listOf(3, 4, 0x86)))
    for ((t, s) in stepsCumulative) {
        data.u8(2)
            .u32(fitTimestamp(t))
            .u32(s.toLong())
    }
    // Steps with activity_type (local 4, global 55), one message per active type.
    // The type is message-local.
    data.def(4, 55, listOf(tsField, listOf(5, 1, 0x00), listOf(3, 4, 0x86)))
    for ((t, activityType, s) in typedStepsCumulative) {
        data.u8(4)
            .u32(fitTimestamp(t))
            .u8(activityType)
            .u32(s.toLong())
    }
    // Active calories (local 5, global 55): timestamp + cumulative kcal (field 19, uint16).
    data.def(5, 55, listOf(tsField, listOf(19, 2, 0x84)))
    for ((t, kcal) in caloriesCumulative) {
        data.u8(5)
            .u32(fitTimestamp(t))
            .u16(kcal)
    }
    // respiration_rate (local 3, global 297): timestamp + rate (sint16, ×100).
    data.def(3, 297, listOf(tsField, listOf(0, 2, 0x83)))
    for ((t, r) in respiration) {
        data.u8(3)
            .u32(fitTimestamp(t))
            .u16(Math.round(r * 100).toInt())
    }

    return fitWrap(data.toBytes())
}
