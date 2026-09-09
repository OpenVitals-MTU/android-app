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

    @Test
    fun `keeps GATT UUIDs and separator lines readable`() {
        val lines = mutableListOf<String>()
        GarminLog.installSink(lines::add)
        try {
            GarminLog.log(
                "[GARMIN-BLE] using V1 receive=6a4ecd28-667b-11e3-949a-0800200c9a66 " +
                    "send=6a4e4c80-667b-11e3-949a-0800200c9a66 mtu=515",
            )
            GarminLog.log("[GARMIN-GATT]   service 6a4e2800-667b-11e3-949a-0800200c9a66")
            GarminLog.log("----------------------------------------")
            GarminLog.log("[GARMIN-AUTH] token QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVowMTIzNDU2Nzg5 issued")
        } finally {
            GarminLog.installSink(null)
        }

        assertEquals(
            listOf(
                "[GARMIN-BLE] using V1 receive=6a4ecd28-667b-11e3-949a-0800200c9a66 " +
                    "send=6a4e4c80-667b-11e3-949a-0800200c9a66 mtu=515",
                "[GARMIN-GATT]   service 6a4e2800-667b-11e3-949a-0800200c9a66",
                "----------------------------------------",
                "[GARMIN-AUTH] token [redacted] issued",
            ),
            lines,
        )
    }
}
