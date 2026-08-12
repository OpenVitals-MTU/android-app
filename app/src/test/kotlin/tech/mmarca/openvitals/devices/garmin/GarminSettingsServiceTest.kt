package tech.mmarca.openvitals.devices.garmin

import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** Port of the Flutter build's `garmin_settings_service_test.dart` — fixtures identical. */
class GarminSettingsServiceTest {

    private fun b(vararg xs: Int) = ByteArray(xs.size) { xs[it].toByte() }

    // ------------------------------------------------------------------
    // ChangeRequest — the only write in the stack.
    // ------------------------------------------------------------------

    private fun valueOf(req: ByteArray, valueField: Int, inner: Int): Long? {
        val settings = protobufField(readProtobuf(req), 42)!!.bytes!!
        val change = protobufField(readProtobuf(settings), 5)!!.bytes!!
        val value = protobufField(readProtobuf(change), valueField)?.bytes ?: return null
        return protobufField(readProtobuf(value), inner)?.varint
    }

    private fun targetOf(req: ByteArray): List<Long?> {
        val settings = protobufField(readProtobuf(req), 42)!!.bytes!!
        val change = protobufField(readProtobuf(settings), 5)!!.bytes!!
        val fields = readProtobuf(change)
        return listOf(
            protobufField(fields, 1)?.varint,
            protobufField(fields, 2)?.varint,
        )
    }

    @Test
    fun `a change names the screen and entry it changes`() {
        val req = GarminSettingsService.changeSwitch(screenId = 64, entryId = 3, value = true)
        assertEquals(listOf(64L, 3L), targetOf(req))
    }

    @Test
    fun `each value kind lands in its OWN field`() {
        // One generic setter could put a time in a switch's field, and the
        // watch would apply whatever it read. Separate fields make that
        // impossible.
        assertEquals(
            1L,
            valueOf(
                GarminSettingsService.changeSwitch(screenId = 1, entryId = 1, value = true),
                3,
                1,
            ),
        )
        assertEquals(
            2L,
            valueOf(
                GarminSettingsService.changeOption(screenId = 1, entryId = 1, index = 2),
                4,
                1,
            ),
        )
        assertEquals(
            25200L, // 07:00 as seconds since midnight
            valueOf(
                GarminSettingsService.changeTime(
                    screenId = 1,
                    entryId = 1,
                    sinceMidnight = 7.hours,
                ),
                6,
                1,
            ),
        )
        assertEquals(
            42L,
            valueOf(
                GarminSettingsService.changeNumber(screenId = 1, entryId = 1, value = 42),
                8,
                1,
            ),
        )
    }

    @Test
    fun `a switch off is a present false not an absent field`() {
        // Omitting it would read as "no change", and the switch would stay on.
        assertEquals(
            0L,
            valueOf(
                GarminSettingsService.changeSwitch(screenId = 1, entryId = 1, value = false),
                3,
                1,
            ),
        )
    }

    @Test
    fun `a time outside one day is refused not wrapped`() {
        try {
            GarminSettingsService.changeTime(
                screenId = 1,
                entryId = 1,
                sinceMidnight = 25.hours,
            )
            fail("expected an IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
            // Expected.
        }
        try {
            GarminSettingsService.changeTime(
                screenId = 1,
                entryId = 1,
                sinceMidnight = (-1).seconds,
            )
            fail("expected an IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
            // Expected.
        }
    }

    @Test
    fun `SUCCESS is ZERO here — the opposite of the find service`() {
        fun reply(status: Int?): ByteArray {
            val w = ProtobufWriter()
            if (status != null) w.varint(1, status)
            val service = ProtobufWriter().nested(6, w.toBytes()).toBytes()
            return ProtobufWriter().nested(42, service).toBytes()
        }

        assertEquals(true, GarminSettingsService.changeSucceeded(reply(0)))
        assertEquals(false, GarminSettingsService.changeSucceeded(reply(1)))
        // A response with no status at all is acceptance, as the find service
        // taught us — but here zero means success, not "unset".
        assertEquals(true, GarminSettingsService.changeSucceeded(reply(null)))
        // No change response at all is not an answer.
        assertNull(GarminSettingsService.changeSucceeded(null))
    }

    // ------------------------------------------------------------------
    // Walking the tree.
    // ------------------------------------------------------------------

    /**
     * Builds the reply shape the watch actually sends, from the captured
     * bytes: Smart{42: SettingsService{2: definitionResponse{2:
     * ScreenDefinition}}}.
     */
    private fun definitionReply(entries: List<ByteArray>, screenId: Int = 36352): ByteArray {
        val def = ProtobufWriter().varint(1, screenId)
        for (e in entries) def.nested(5, e)
        val response = ProtobufWriter().nested(2, def.toBytes()).toBytes()
        val service = ProtobufWriter().nested(2, response).toBytes()
        return ProtobufWriter().nested(42, service).toBytes()
    }

    private fun entry(
        id: Int,
        title: String? = null,
        targetType: Int? = null,
        subscreen: Int? = null,
    ): ByteArray {
        val w = ProtobufWriter().varint(1, id)
        if (title != null) {
            w.nested(3, ProtobufWriter().string(2, title).toBytes())
        }
        if (targetType != null) {
            val t = ProtobufWriter().varint(1, targetType)
            if (subscreen != null) t.varint(2, subscreen)
            w.nested(9, t.toBytes())
        }
        return w.toBytes()
    }

    @Test
    fun `finds the screens an entry leads to`() {
        // Shapes taken from a real vívoactive 5 root: Clocks → 204, and
        // Garmin Pay whose target type 6 opens something ON the watch.
        val reply = definitionReply(
            listOf(
                entry(id = 4, title = "Clocks", targetType = 0, subscreen = 204),
                entry(id = 6, title = "Glances", targetType = 0, subscreen = 920),
            ),
        )
        val found = GarminSettingsService.subscreens(reply)
        assertEquals(listOf(204, 920), found.map { it.screenId })
        assertEquals("Clocks", found.first().title)
    }

    @Test
    fun `follows a subscreen-WITH-OPTIONS which is what an alarm is`() {
        // A populated alarm slot is target type 9, not 0. Following only 0
        // meant the walk reached the Alarms list and stopped at it.
        val reply = definitionReply(
            listOf(entry(id = 0, title = "7:00 am", targetType = 9, subscreen = 64)),
        )
        assertEquals(
            listOf(64),
            GarminSettingsService.subscreens(reply).map { it.screenId },
        )
    }

    @Test
    fun `an empty alarm slot points at screen zero and is skipped`() {
        // The list reserves a row per slot; unused ones target nothing.
        // Asking the watch for screen zero requests something that does not
        // exist.
        val reply = definitionReply(
            listOf(
                entry(id = 1, targetType = 0, subscreen = 0),
                entry(id = 2, targetType = 0, subscreen = 0),
            ),
        )
        assertTrue(GarminSettingsService.subscreens(reply).isEmpty())
    }

    @Test
    fun `does not try to walk into what it cannot open`() {
        // Type 6 opens an activity on the watch and 7 is hidden; treating
        // either as a screen would request an id that means something else
        // entirely.
        val reply = definitionReply(
            listOf(
                entry(id = 2, title = "Garmin Pay", targetType = 6, subscreen = 2),
                entry(id = 3, title = "Hidden", targetType = 7, subscreen = 9),
                entry(id = 1, title = "Finish Setup"), // no target at all
            ),
        )
        assertTrue(GarminSettingsService.subscreens(reply).isEmpty())
    }

    @Test
    fun `a reply carrying a STATE is not a definition`() {
        val state = ProtobufWriter().emptyMessage(4).toBytes()
        val reply = ProtobufWriter().nested(42, state).toBytes()
        assertFalse(
            GarminSettingsService.carries(
                reply,
                GarminSettingsService.DEFINITION_RESPONSE_FIELD,
            ),
        )
        assertTrue(
            GarminSettingsService.carries(reply, GarminSettingsService.STATE_RESPONSE_FIELD),
        )
        assertTrue(GarminSettingsService.subscreens(reply).isEmpty())
    }

    // ------------------------------------------------------------------
    // Requests.
    // ------------------------------------------------------------------

    @Test
    fun `init carries the locale that translates the whole tree`() {
        val bytes = GarminSettingsService.init(language = "en_US", region = "us")
        val settings = protobufField(readProtobuf(bytes), 42)!!.bytes!!
        val init = protobufField(readProtobuf(settings), 8)!!.bytes!!
        val fields = readProtobuf(init)
        assertEquals("en_US", String(protobufField(fields, 1)!!.bytes!!, Charsets.ISO_8859_1))
        assertEquals("us", String(protobufField(fields, 2)!!.bytes!!, Charsets.ISO_8859_1))
    }

    @Test
    fun `a definition request names the screen and the language`() {
        val bytes = GarminSettingsService.screenDefinition(36352)
        val settings = protobufField(readProtobuf(bytes), 42)!!.bytes!!
        val request = protobufField(readProtobuf(settings), 1)!!.bytes!!
        val fields = readProtobuf(request)
        assertEquals(36352L, protobufField(fields, 1)!!.varint)
        assertEquals("en_US", String(protobufField(fields, 3)!!.bytes!!, Charsets.ISO_8859_1))
    }

    @Test
    fun `a state request carries only the screen id`() {
        val settings = protobufField(
            readProtobuf(GarminSettingsService.screenState(36352)),
            42,
        )!!.bytes!!
        val request = protobufField(readProtobuf(settings), 3)!!.bytes!!
        assertEquals(36352L, readProtobuf(request).single().varint)
    }

    @Test
    fun `recognises a definition reply and does not mistake a state for one`() {
        fun reply(field: Int): ByteArray {
            val service = ProtobufWriter().emptyMessage(field).toBytes()
            return ProtobufWriter().nested(42, service).toBytes()
        }

        assertTrue(GarminSettingsService.hasDefinition(reply(2)))
        assertFalse(GarminSettingsService.hasState(reply(2)))
        assertTrue(GarminSettingsService.hasState(reply(4)))
        assertFalse(GarminSettingsService.hasDefinition(reply(4)))
    }

    @Test
    fun `a reply for another service is not a settings reply`() {
        // The watch narrates on services this app does not speak; mistaking
        // one for a settings screen would render a menu out of unrelated
        // bytes.
        val other = ProtobufWriter().emptyMessage(12).toBytes()
        assertNull(GarminSettingsService.unwrap(other))
        assertFalse(GarminSettingsService.hasDefinition(other))
        assertFalse(GarminSettingsService.hasDefinition(null))
        assertFalse(GarminSettingsService.hasDefinition(b(0xFF, 0xFF)))
    }
}
