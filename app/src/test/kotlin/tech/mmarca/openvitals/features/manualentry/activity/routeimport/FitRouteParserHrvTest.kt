package tech.mmarca.openvitals.features.manualentry.activity.routeimport

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class FitRouteParserHrvTest {

    /** Seconds between the Unix epoch and FIT's 1989-12-31 epoch. */
    private val fitEpochUnixSeconds = 631_065_600L

    private fun fitFile(vararg dataRecords: ByteArray): ByteArray {
        val data = dataRecords.fold(ByteArray(0), ByteArray::plus)
        val header = byteArrayOf(
            12, // header size
            0x10, // protocol version
            0x54, 0x08, // profile version
            (data.size and 0xFF).toByte(),
            ((data.size shr 8) and 0xFF).toByte(),
            ((data.size shr 16) and 0xFF).toByte(),
            ((data.size shr 24) and 0xFF).toByte(),
            '.'.code.toByte(), 'F'.code.toByte(), 'I'.code.toByte(), 'T'.code.toByte(),
        )
        return header + data
    }

    /** Definition for local message 0, global 370 (hrv_status_summary). */
    private fun hrvDefinition(): ByteArray = byteArrayOf(
        0x40, // definition message, local type 0
        0x00, // reserved
        0x00, // little endian
        0x72, 0x01, // global message 370
        0x02, // two fields
        253.toByte(), 0x04, 0x86.toByte(), // timestamp, uint32
        0x01, 0x02, 0x84.toByte(), // last_night_average, uint16
    )

    private fun hrvData(timestampRaw: Long, rawValue: Int): ByteArray = byteArrayOf(
        0x00, // data message, local type 0
        (timestampRaw and 0xFF).toByte(),
        ((timestampRaw shr 8) and 0xFF).toByte(),
        ((timestampRaw shr 16) and 0xFF).toByte(),
        ((timestampRaw shr 24) and 0xFF).toByte(),
        (rawValue and 0xFF).toByte(),
        ((rawValue shr 8) and 0xFF).toByte(),
    )

    @Test fun `decodes the nightly average scaled by 128 into milliseconds`() {
        val timestampRaw = 1_000_000_000L
        val bytes = fitFile(hrvDefinition(), hrvData(timestampRaw, rawValue = 70 * 128))

        val readings = FitRouteParser.parseWellnessHrv(bytes)

        val reading = readings.single()
        assertEquals(Instant.ofEpochSecond(fitEpochUnixSeconds + timestampRaw), reading.time)
        assertEquals(70.0, reading.rmssdMillis, 0.0001)
    }

    @Test fun `the uint16 invalid sentinel is dropped`() {
        val bytes = fitFile(hrvDefinition(), hrvData(timestampRaw = 1_000_000_000L, rawValue = 0xFFFF))

        assertTrue(FitRouteParser.parseWellnessHrv(bytes).isEmpty())
    }

    @Test fun `a wellness file still fails the activity parse, which is what triggers the fallback`() {
        val bytes = fitFile(hrvDefinition(), hrvData(timestampRaw = 1_000_000_000L, rawValue = 9000))

        assertThrows(IllegalArgumentException::class.java) {
            FitRouteParser.parse(bytes)
        }
        assertEquals(1, FitRouteParser.parseWellnessHrv(bytes).size)
    }
}
