package tech.mmarca.openvitals.features.manualentry.activity.routeimport

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Which of a FIT file's several names becomes the imported activity's title.
 *
 * A watch can write a name on the `workout` message (the plan that was
 * followed) and another on the `activity` message (what the wearer called
 * the result). The second is the one the wrist shows, so it wins; the
 * workout name is the fallback when the activity message carries none.
 */
class FitActivityNameTest {

    private fun fitFile(vararg dataRecords: ByteArray): ByteArray {
        val data = dataRecords.fold(ByteArray(0), ByteArray::plus)
        val header = byteArrayOf(
            12, 0x10, 0x54, 0x08,
            (data.size and 0xFF).toByte(),
            ((data.size shr 8) and 0xFF).toByte(),
            ((data.size shr 16) and 0xFF).toByte(),
            ((data.size shr 24) and 0xFF).toByte(),
            '.'.code.toByte(), 'F'.code.toByte(), 'I'.code.toByte(), 'T'.code.toByte(),
        )
        return header + data
    }

    private fun u32(value: Long): ByteArray = byteArrayOf(
        (value and 0xFF).toByte(),
        ((value shr 8) and 0xFF).toByte(),
        ((value shr 16) and 0xFF).toByte(),
        ((value shr 24) and 0xFF).toByte(),
    )

    /** A definition whose only field is a fixed-width string in field 8. */
    private fun nameDefinition(localType: Int, globalMessage: Int, width: Int): ByteArray = byteArrayOf(
        (0x40 or localType).toByte(), 0x00, 0x00,
        (globalMessage and 0xFF).toByte(), ((globalMessage shr 8) and 0xFF).toByte(),
        0x01,
        0x08, width.toByte(), 0x07, // field 8, string
    )

    private fun nameData(localType: Int, name: String, width: Int): ByteArray {
        val padded = name.toByteArray(Charsets.UTF_8).copyOf(width)
        return byteArrayOf(localType.toByte()) + padded
    }

    /** Local 2 → session (18): start_time, total_elapsed_time, sport. */
    private fun sessionDefinition(): ByteArray = byteArrayOf(
        0x42, 0x00, 0x00, 0x12, 0x00, 0x03,
        0x02, 0x04, 0x86.toByte(), // start_time uint32
        0x07, 0x04, 0x86.toByte(), // total_elapsed_time uint32 (x1000)
        0x05, 0x01, 0x00, // sport enum
    )

    private fun sessionData(): ByteArray =
        byteArrayOf(0x02) + u32(1_000_000_000L) + u32(1_800_000L) + byteArrayOf(0x01)

    private val width = 16

    @Test fun `the activity name outranks the workout name`() {
        val bytes = fitFile(
            nameDefinition(localType = 0, globalMessage = 26, width = width),
            nameData(localType = 0, name = "Tempo run", width = width),
            sessionDefinition(),
            sessionData(),
            nameDefinition(localType = 1, globalMessage = 34, width = width),
            nameData(localType = 1, name = "Evening Run", width = width),
        )

        assertEquals("Evening Run", FitRouteParser.parse(bytes).name)
    }

    @Test fun `without an activity name the workout name still applies`() {
        val bytes = fitFile(
            nameDefinition(localType = 0, globalMessage = 26, width = width),
            nameData(localType = 0, name = "Tempo run", width = width),
            sessionDefinition(),
            sessionData(),
            nameDefinition(localType = 1, globalMessage = 34, width = width),
            nameData(localType = 1, name = "", width = width),
        )

        assertEquals("Tempo run", FitRouteParser.parse(bytes).name)
    }
}
