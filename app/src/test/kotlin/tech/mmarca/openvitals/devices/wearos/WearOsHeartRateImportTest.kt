package tech.mmarca.openvitals.devices.wearos

import androidx.health.connect.client.records.HeartRateRecord
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WearOsHeartRateImportTest {

    private val hour = Instant.parse("2026-10-07T10:00:00Z")

    private fun sample(offsetSeconds: Long, bpm: Int = 70) =
        WearOsHeartRateSample(hour.plusSeconds(offsetSeconds), bpm)

    @Test
    fun `one record per clock hour, keyed on the bucket's first sample`() {
        val records = WearOsHeartRateImport.records(
            listOf(sample(10), sample(20), sample(3600 + 5), sample(3600 + 15)),
        ).map { it as HeartRateRecord }

        assertEquals(2, records.size)
        assertEquals(hour.plusSeconds(10), records[0].startTime)
        assertEquals(hour.plusSeconds(20), records[0].endTime)
        assertEquals(2, records[0].samples.size)
        assertEquals("wearos_hr_${hour.plusSeconds(10).toEpochMilli()}", records[0].metadata.clientRecordId)
        assertEquals("wearos_hr_${hour.plusSeconds(3605).toEpochMilli()}", records[1].metadata.clientRecordId)
    }

    @Test
    fun `the same samples twice give the same ids, so a re-pull upserts`() {
        val once = WearOsHeartRateImport.records(listOf(sample(10), sample(20)))
        val again = WearOsHeartRateImport.records(listOf(sample(20), sample(10), sample(10)))

        assertEquals(once.map { it.metadata.clientRecordId }, again.map { it.metadata.clientRecordId })
        assertEquals(2, (again.single() as HeartRateRecord).samples.size)
    }

    @Test
    fun `a lone sample still makes a valid record`() {
        val record = WearOsHeartRateImport.records(listOf(sample(0, 58))).single() as HeartRateRecord

        assertTrue(record.endTime.isAfter(record.startTime))
        assertEquals(58L, record.samples.single().beatsPerMinute)
    }

    @Test
    fun `nothing in, nothing out`() {
        assertTrue(WearOsHeartRateImport.records(emptyList()).isEmpty())
    }
}
