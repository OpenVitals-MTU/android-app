package tech.mmarca.openvitals.devices.core.sync

import kotlin.time.Duration.Companion.hours
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoSyncIntervalTest {

    @Test
    fun `every offered interval round-trips through its stored minutes`() {
        AutoSyncInterval.entries.forEach { interval ->
            assertEquals(interval, AutoSyncInterval.fromMinutes(interval.minutes))
        }
    }

    @Test
    fun `an absent or zero value reads as off`() {
        assertEquals(AutoSyncInterval.OFF, AutoSyncInterval.fromMinutes(null))
        assertEquals(AutoSyncInterval.OFF, AutoSyncInterval.fromMinutes(0))
        assertFalse(AutoSyncInterval.OFF.isOn)
    }

    @Test
    fun `an interval this build does not offer reads as off`() {
        // A schedule the app cannot honour must read as off rather than as a
        // silently different one: 10 minutes is below Android's floor for
        // periodic work, and 45 was never offered.
        assertEquals(AutoSyncInterval.OFF, AutoSyncInterval.fromMinutes(10))
        assertEquals(AutoSyncInterval.OFF, AutoSyncInterval.fromMinutes(45))
        assertEquals(AutoSyncInterval.OFF, AutoSyncInterval.fromMinutes(-30))
    }

    @Test
    fun `every schedulable interval clears Android's fifteen-minute floor`() {
        AutoSyncInterval.entries.filter { it.isOn }.forEach { interval ->
            assertTrue(
                "${interval.name} is below WorkManager's minimum period",
                interval.minutes >= 15,
            )
        }
    }

    @Test
    fun `the duration matches the stored minutes`() {
        assertEquals(2.hours, AutoSyncInterval.EVERY_2_HOURS.duration)
    }
}
