package tech.mmarca.openvitals.devices.garmin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GarminLogTest {

    @Test
    fun `redacts named credentials bearer tokens and bare auth keys`() {
        val lines = mutableListOf<String>()
        GarminLog.installSink(lines::add)
        try {
            GarminLog.log("access_token=secret-value refreshToken:another-secret")
            GarminLog.log("Authorization: Bearer abc.def.ghi")
            GarminLog.log("ABCDEFGHIJKLMNOPQRSTUVWXYZ012345678")
        } finally {
            GarminLog.installSink(null)
        }

        assertEquals(3, lines.size)
        assertTrue(lines.all { "[redacted]" in it })
        assertFalse(lines.joinToString().contains("secret-value"))
        assertFalse(lines.joinToString().contains("abc.def.ghi"))
        assertFalse(lines.joinToString().contains("ABCDEFGHIJKLMNOPQRSTUVWXYZ012345678"))
    }

    @Test
    fun `keeps ordinary protocol diagnostics readable`() {
        val lines = mutableListOf<String>()
        GarminLog.installSink(lines::add)
        try {
            GarminLog.log("[GARMIN-PB] reply #42 (128B)")
        } finally {
            GarminLog.installSink(null)
        }

        assertEquals(listOf("[GARMIN-PB] reply #42 (128B)"), lines)
    }
}
