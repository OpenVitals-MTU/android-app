package tech.mmarca.openvitals.devices.wearos

import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZoneOffset.UTC
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WearOsStepsImportTest {

    private val minute = Instant.parse("2026-10-07T10:15:00Z")

    @Test
    fun `each minute carries the offset in force at it, across a DST change`() {
        val berlin = ZoneId.of("Europe/Berlin")
        // 2026-10-25 01:00Z: Berlin goes from +02:00 to +01:00.
        val before = Instant.parse("2026-10-25T00:58:00Z")
        val after = Instant.parse("2026-10-25T01:05:00Z")
        val records = WearOsStepsImport.records(listOf(WearOsStepMinute(before, 50), WearOsStepMinute(after, 50)), berlin)

        assertEquals(listOf(ZoneOffset.ofHours(2), ZoneOffset.ofHours(1)), records.map { it.startZoneOffset })
    }

    @Test
    fun `one record per minute, keyed on its start, with the count as its version`() {
        val record = WearOsStepsImport.records(listOf(WearOsStepMinute(minute, 87)), UTC).single()

        assertEquals(minute, record.startTime)
        assertEquals(minute.plusSeconds(60), record.endTime)
        assertEquals(87L, record.count)
        assertEquals("wearos_steps_${minute.toEpochMilli()}", record.metadata.clientRecordId)
        assertEquals(87L, record.metadata.clientRecordVersion)
    }

    @Test
    fun `a minute stated twice in one page keeps the larger count`() {
        val minutes = WearOsStepsImport.latestPerMinute(listOf(WearOsStepMinute(minute, 30), WearOsStepMinute(minute, 55)))

        assertEquals(listOf(55), minutes.map { it.steps })
    }

    @Test
    fun `no minutes, no records`() {
        assertTrue(WearOsStepsImport.records(emptyList(), UTC).isEmpty())
    }

    @Test
    fun `the cadence is the minute's count, a measurement from the watch`() {
        val record = WearOsStepsImport.cadenceRecords(listOf(WearOsStepMinute(minute, 104)), UTC).single()

        assertEquals(104.0, record.samples.single().rate, 0.0)
        assertEquals("wearos_cadence_${minute.toEpochMilli()}", record.metadata.clientRecordId)
        assertEquals(104L, record.metadata.clientRecordVersion)
    }

    @Test
    fun `walking energy needs the body weight and skips minutes that cost no more than rest`() {
        val minutes = listOf(WearOsStepMinute(minute, 100), WearOsStepMinute(minute.plusSeconds(60), 10))

        assertTrue(WearOsStepsImport.activeCaloriesRecords(minutes, weightKg = null, zone = UTC).isEmpty())
        val record = WearOsStepsImport.activeCaloriesRecords(minutes, weightKg = 70.0, zone = UTC).single()
        assertEquals(minute, record.startTime)
        assertTrue(record.energy.inKilocalories > 0.0)
        assertEquals(100L, record.metadata.clientRecordVersion)
    }

    @Test
    fun `minutes inside an exercise session are left out`() {
        val minute1 = minute
        val minute2 = minute.plusSeconds(60)
        val minutes = listOf(WearOsStepMinute(minute1, 100), WearOsStepMinute(minute2, 100))
        val exerciseSessions = listOf(minute1..minute1.plusSeconds(60))

        assertEquals(listOf(minute2), WearOsStepsImport.outsideExerciseSessions(minutes, exerciseSessions).map { it.start })
    }
}
