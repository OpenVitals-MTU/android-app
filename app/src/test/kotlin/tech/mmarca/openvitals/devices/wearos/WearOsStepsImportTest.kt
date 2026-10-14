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
}
