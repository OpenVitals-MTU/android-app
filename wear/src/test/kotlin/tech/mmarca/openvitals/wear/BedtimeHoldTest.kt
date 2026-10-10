package tech.mmarca.openvitals.wear

import org.junit.Assert.assertEquals
import org.junit.Test

class BedtimeHoldTest {

    @Test
    fun `the processor is held only in bedtime mode, on the wrist, off the charger`() {
        assertEquals(BedtimeHold.HOLDING, BedtimeHold.decide(bedtime = true, worn = true, charging = false))
        assertEquals(BedtimeHold.OFF, BedtimeHold.decide(bedtime = false, worn = true, charging = false))
        assertEquals(BedtimeHold.PAUSED_OFF_WRIST, BedtimeHold.decide(bedtime = true, worn = false, charging = false))
        assertEquals(BedtimeHold.PAUSED_CHARGING, BedtimeHold.decide(bedtime = true, worn = true, charging = true))
        // On the charger the watch is usually off the wrist too; charging names it.
        assertEquals(BedtimeHold.PAUSED_CHARGING, BedtimeHold.decide(bedtime = true, worn = false, charging = true))
    }
}
