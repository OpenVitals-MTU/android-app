package tech.mmarca.openvitals.devices.wearos

import androidx.health.connect.client.records.ElevationGainedRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WearOsFloorsImportTest {

    private val minute = Instant.parse("2026-10-12T08:00:00Z")
    private val before = minute.minusSeconds(60)

    private fun floors(ascent: List<WearOsAscentMinute>, steps: List<WearOsStepMinute>): List<Double> =
        WearOsFloorsImport.records(ascent, steps).map { (it as FloorsClimbedRecord).floors }

    @Test
    fun `height counts with ten steps per floor in its minute and the one before`() {
        assertEquals(listOf(2.0), floors(listOf(WearOsAscentMinute(minute, 6.0)), listOf(WearOsStepMinute(minute, 25))), 1e-9)
        // Stairs that started in the minute before.
        assertEquals(listOf(1.0), floors(listOf(WearOsAscentMinute(minute, 3.0)), listOf(WearOsStepMinute(before, 12))), 1e-9)
    }

    @Test
    fun `a partial floor is kept, so a day's floors add up`() {
        // 4 m with plenty of steps: one and a third floors, not one.
        assertEquals(listOf(4.0 / 3.0), floors(listOf(WearOsAscentMinute(minute, 4.0)), listOf(WearOsStepMinute(minute, 60))), 1e-9)
    }

    @Test
    fun `too few steps cut the height, and none drop the minute`() {
        // 20 steps carry 6 m, not 15: the rest was an elevator.
        assertEquals(listOf(2.0), floors(listOf(WearOsAscentMinute(minute, 15.0)), listOf(WearOsStepMinute(minute, 20))), 1e-9)
        assertTrue(floors(listOf(WearOsAscentMinute(minute, 15.0)), emptyList()).isEmpty())
    }

    @Test
    fun `the record is keyed on the minute and versioned by its credited decimetres`() {
        val record = WearOsFloorsImport.records(listOf(WearOsAscentMinute(minute, 9.0)), listOf(WearOsStepMinute(minute, 60)))
            .single() as FloorsClimbedRecord
        assertEquals(minute, record.startTime)
        assertEquals(minute.plusSeconds(60), record.endTime)
        assertEquals("wearos_floors_${minute.toEpochMilli()}", record.metadata.clientRecordId)
        assertEquals(90L, record.metadata.clientRecordVersion)
    }

    private fun assertEquals(expected: List<Double>, actual: List<Double>, delta: Double) {
        assertEquals(expected.size, actual.size)
        expected.zip(actual).forEach { (e, a) -> assertEquals(e, a, delta) }
    }

    @Test
    fun `the elevation gain is the credited metres, under its own id`() {
        // 20 steps carry 6 m of the 15 m climbed.
        val record = WearOsFloorsImport.elevationRecords(listOf(WearOsAscentMinute(minute, 15.0)), listOf(WearOsStepMinute(minute, 20)))
            .single() as ElevationGainedRecord
        assertEquals(6.0, record.elevation.inMeters, 1e-9)
        assertEquals("wearos_elevation_${minute.toEpochMilli()}", record.metadata.clientRecordId)
        assertEquals(60L, record.metadata.clientRecordVersion)
        assertTrue(WearOsFloorsImport.elevationRecords(listOf(WearOsAscentMinute(minute, 15.0)), emptyList()).isEmpty())
    }
}
