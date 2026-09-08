package tech.mmarca.openvitals.devices.garmin

import org.junit.Assert.assertEquals
import org.junit.Test

class GarminLogTest {

    @Test
    fun `redacts only credential values and preserves the rest of each log line`() {
        val lines = mutableListOf<String>()
        GarminLog.installSink(lines::add)
        try {
            GarminLog.log(
                "[GARMIN-HTTP] request access_token=secret-value " +
                    "refreshToken:another-secret completed",
            )
            GarminLog.log("[GARMIN-HTTP] Authorization: Bearer abc.def.ghi accepted")
            GarminLog.log(
                "[GARMIN-AUTH] key ABCDEFGHIJKLMNOPQRSTUVWXYZ012345678 issued",
            )
        } finally {
            GarminLog.installSink(null)
        }

        assertEquals(
            listOf(
                "[GARMIN-HTTP] request access_token=[redacted] " +
                    "refreshToken:[redacted] completed",
                "[GARMIN-HTTP] Authorization: [redacted] [redacted] accepted",
                "[GARMIN-AUTH] key [redacted] issued",
            ),
            lines,
        )
    }

    @Test
    fun `keeps ordinary protocol diagnostics readable`() {
        val lines = mutableListOf<String>()
        GarminLog.installSink(lines::add)
        try {
            GarminLog.log("[GARMIN-PB] reply #42 (128B)")
            GarminLog.log("[GARMIN-SYNC] complete: 27 files")
            GarminLog.log("[GARMIN-NOTIFY] reconnecting in 15s")
            GarminLog.log(
                "[GARMIN-SYNC] downloaded 120 files and completed the import without errors",
            )
        } finally {
            GarminLog.installSink(null)
        }

        assertEquals(
            listOf(
                "[GARMIN-PB] reply #42 (128B)",
                "[GARMIN-SYNC] complete: 27 files",
                "[GARMIN-NOTIFY] reconnecting in 15s",
                "[GARMIN-SYNC] downloaded 120 files and completed the import without errors",
            ),
            lines,
        )
    }
}
