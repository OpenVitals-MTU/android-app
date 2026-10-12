package tech.mmarca.openvitals.devices.wearos

import androidx.health.connect.client.records.FloorsClimbedRecord
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WearOsFloorsImportTest {

    private val minute = Instant.parse("2026-10-12T08:00:00Z")
    private val before = minute.minusSeconds(60)

    private fun floors(floors: List<WearOsFloorMinute>, steps: List<WearOsStepMinute>): List<Double> =
        WearOsFloorsImport.records(floors, steps).map { (it as FloorsClimbedRecord).floors }

    @Test
    fun `a floor counts with ten steps in its minute and the one before`() {
        assertEquals(listOf(2.0), floors(listOf(WearOsFloorMinute(minute, 2)), listOf(WearOsStepMinute(minute, 25))))
        // Stairs that started in the minute before.
        assertEquals(listOf(1.0), floors(listOf(WearOsFloorMinute(minute, 1)), listOf(WearOsStepMinute(before, 12))))
    }

    @Test
    fun `too few steps cut the count, and none drop the minute`() {
        // 20 steps carry two floors, not five: the rest was an elevator.
        assertEquals(listOf(2.0), floors(listOf(WearOsFloorMinute(minute, 5)), listOf(WearOsStepMinute(minute, 20))))
        assertTrue(floors(listOf(WearOsFloorMinute(minute, 5)), emptyList()).isEmpty())
        assertTrue(floors(listOf(WearOsFloorMinute(minute, 1)), listOf(WearOsStepMinute(minute, 9))).isEmpty())
    }

    @Test
    fun `the record is keyed on the minute and versioned by its count`() {
        val record = WearOsFloorsImport.records(listOf(WearOsFloorMinute(minute, 3)), listOf(WearOsStepMinute(minute, 60)))
            .single() as FloorsClimbedRecord
        assertEquals(minute, record.startTime)
        assertEquals(minute.plusSeconds(60), record.endTime)
        assertEquals("wearos_floors_${minute.toEpochMilli()}", record.metadata.clientRecordId)
        assertEquals(3L, record.metadata.clientRecordVersion)
    }
}
