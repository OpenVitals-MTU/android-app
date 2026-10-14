package tech.mmarca.openvitals.devices.wearos

import androidx.health.connect.client.records.StepsRecord
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WearOsStepsImportTest {

    private val minute = Instant.parse("2026-10-07T10:15:00Z")

    @Test
    fun `one record per minute, keyed on its start, with the count as its version`() {
        val record = WearOsStepsImport.records(listOf(WearOsStepMinute(minute, 87))).single() as StepsRecord

        assertEquals(minute, record.startTime)
        assertEquals(minute.plusSeconds(60), record.endTime)
        assertEquals(87L, record.count)
        assertEquals("wearos_steps_${minute.toEpochMilli()}", record.metadata.clientRecordId)
        assertEquals(87L, record.metadata.clientRecordVersion)
    }

    @Test
    fun `a minute stated twice in one page keeps the larger count`() {
        val records = WearOsStepsImport.records(listOf(WearOsStepMinute(minute, 30), WearOsStepMinute(minute, 55)))

        assertEquals(listOf(55L), records.map { (it as StepsRecord).count })
    }

    @Test
    fun `no minutes, no records`() {
        assertTrue(WearOsStepsImport.records(emptyList()).isEmpty())
    }

    @Test
    fun `the cadence is the minute's count, a measurement from the watch`() {
        val record = WearOsStepsImport.cadenceRecords(listOf(WearOsStepMinute(minute, 104))).single()
            as androidx.health.connect.client.records.StepsCadenceRecord

        assertEquals(104.0, record.samples.single().rate, 0.0)
        assertEquals("wearos_cadence_${minute.toEpochMilli()}", record.metadata.clientRecordId)
        assertEquals(104L, record.metadata.clientRecordVersion)
    }

    @Test
    fun `walking energy needs the body weight and skips minutes that cost no more than rest`() {
        val minutes = listOf(WearOsStepMinute(minute, 100), WearOsStepMinute(minute.plusSeconds(60), 10))

        assertTrue(WearOsStepsImport.activeCaloriesRecords(minutes, weightKg = null).isEmpty())
        val record = WearOsStepsImport.activeCaloriesRecords(minutes, weightKg = 70.0).single()
            as androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
        assertEquals(minute, record.startTime)
        assertEquals(2.584, record.energy.inKilocalories, 0.001)
        assertEquals(100L, record.metadata.clientRecordVersion)
    }
}
