package tech.mmarca.openvitals.devices.garmin

import java.time.DayOfWeek
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.core.fit.FitDecoder
import tech.mmarca.openvitals.core.fit.fitTimestamp

/** The alarms file decodes back through the app's own FIT decoder. */
class GarminAlarmsFileTest {

    private val now = Instant.parse("2026-09-19T10:00:00Z")

    private val wakeUp = GarminAlarm(
        hour = 6,
        minute = 30,
        days = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
        sound = GarminAlarmSound.VIBRATION,
        backlight = false,
        label = GarminAlarmLabel.WAKE_UP,
    )
    private val once = GarminAlarm(hour = 22, minute = 5, enabled = false)

    private fun decode(alarms: List<GarminAlarm>) =
        FitDecoder.readFile(GarminAlarmsFile.build(alarms, now), startOffset = 0).messages

    @Test fun `the file holds a file id, one record per alarm and the device settings`() {
        val messages = decode(listOf(wakeUp, once))

        assertEquals(listOf(0, 222, 222, 2), messages.map { it.globalMessageNumber })
        val fileId = messages[0]
        assertEquals(2L, fileId.values[0]) // type: settings
        assertEquals(1L, fileId.values[1])
        assertEquals(65534L, fileId.values[2])
        assertEquals(1L, fileId.values[3])
        assertEquals(fitTimestamp(now), fileId.values[4])
    }

    @Test fun `an alarm record carries every field`() {
        val alarm = decode(listOf(wakeUp, once))[1]

        assertEquals(390L, alarm.values[0]) // 6:30 as minutes
        assertEquals(17L, alarm.values[1]) // Monday is bit 0, Friday bit 4
        assertEquals(1L, alarm.values[2])
        assertEquals(2L, alarm.values[3]) // vibration
        assertEquals(0L, alarm.values[4])
        assertEquals(fitTimestamp(now), alarm.values[5])
        assertEquals(0L, alarm.values[7])
        assertEquals(1L, alarm.values[8]) // wake up
        assertEquals(0L, alarm.values[254])
    }

    @Test fun `an alarm with no days rings once`() {
        val alarm = decode(listOf(wakeUp, once))[2]

        assertEquals(1325L, alarm.values[0])
        assertEquals(128L, alarm.values[1])
        assertEquals(0L, alarm.values[2])
        assertEquals(1L, alarm.values[254])
    }

    @Test fun `the device settings repeat the alarms as arrays`() {
        val settings = decode(listOf(wakeUp, once))[3]

        assertEquals(listOf(390L, 1325L), settings.arrays[8])
        assertEquals(listOf(5L, 5L), settings.arrays[9]) // custom: the mask says which days
        assertEquals(listOf(1L, 0L), settings.arrays[28])
        assertEquals(listOf(17L, 128L), settings.arrays[92])
    }

    @Test fun `every day is the low seven bits`() {
        val alarm = decode(listOf(GarminAlarm(7, 0, days = DayOfWeek.entries.toSet())))[1]

        assertEquals(127L, alarm.values[1])
    }

    @Test fun `an empty list is a file id alone, as upstream sends`() {
        assertEquals(listOf(0), decode(emptyList()).map { it.globalMessageNumber })
    }

    @Test fun `the file is a framed FIT file`() {
        assertTrue(FitDecoder.isFitFile(GarminAlarmsFile.build(listOf(wakeUp), now)))
    }

    @Test fun `more alarms than the watch holds is refused`() {
        val tooMany = List(GarminAlarmsFile.MaxAlarms + 1) { GarminAlarm(hour = it, minute = 0) }

        assertThrows(IllegalArgumentException::class.java) { GarminAlarmsFile.build(tooMany, now) }
    }

    @Test fun `a time that is not a time of day is refused`() {
        assertThrows(IllegalArgumentException::class.java) { GarminAlarm(hour = 24, minute = 0) }
        assertThrows(IllegalArgumentException::class.java) { GarminAlarm(hour = 0, minute = 60) }
    }
}
