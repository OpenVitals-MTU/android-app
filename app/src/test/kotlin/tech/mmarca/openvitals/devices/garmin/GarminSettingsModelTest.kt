package tech.mmarca.openvitals.devices.garmin

import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Every reply is rebuilt in the wire shape the watch sends, so the parser sees real bytes. */
class GarminSettingsModelTest {

    private fun definitionReply(
        screenId: Int,
        title: String? = null,
        entries: List<ByteArray> = emptyList(),
    ): ByteArray {
        val def = ProtobufWriter().varint(1, screenId)
        if (title != null) def.nested(4, label(title))
        for (e in entries) def.nested(5, e)
        val response = ProtobufWriter()
            .varint(1, 0)
            .nested(2, def.toBytes())
            .toBytes()
        val service = ProtobufWriter().nested(2, response).toBytes()
        return ProtobufWriter().nested(42, service).toBytes()
    }

    private fun stateReply(screenId: Int, states: List<ByteArray> = emptyList()): ByteArray {
        val state = ProtobufWriter().varint(1, screenId)
        for (s in states) state.nested(4, s)
        val response = ProtobufWriter()
            .varint(1, 0)
            .nested(2, state.toBytes())
            .toBytes()
        val service = ProtobufWriter().nested(4, response).toBytes()
        return ProtobufWriter().nested(42, service).toBytes()
    }

    private fun label(text: String): ByteArray = ProtobufWriter().string(2, text).toBytes()

    private fun entry(
        id: Int,
        title: String? = null,
        targetType: Int? = null,
        subscreen: Int? = null,
        options: List<String> = emptyList(),
    ): ByteArray {
        val w = ProtobufWriter().varint(1, id)
        if (title != null) w.nested(3, label(title))
        if (targetType != null) {
            val t = ProtobufWriter().varint(1, targetType)
            if (subscreen != null) t.varint(2, subscreen)
            if (options.isNotEmpty()) {
                val list = ProtobufWriter()
                for (o in options) {
                    list.nested(1, ProtobufWriter().nested(3, label(o)).toBytes())
                }
                t.nested(4, list.toBytes())
            }
            w.nested(9, t.toBytes())
        }
        return w.toBytes()
    }

    private fun switchState(id: Int, on: Boolean): ByteArray = ProtobufWriter()
        .varint(1, id)
        .nested(3, ProtobufWriter().varint(1, if (on) 1 else 0).toBytes())
        .toBytes()

    /** A removable row: field 9 present and empty. That mark separates an alarm's "Delete" from untargeted root rows. */
    private fun removableState(id: Int): ByteArray = ProtobufWriter()
        .varint(1, id)
        .nested(9, ByteArray(0))
        .toBytes()

    /** A row the watch mentioned and said nothing about, as the root's rows arrive. */
    private fun bareState(id: Int): ByteArray = ProtobufWriter().varint(1, id).toBytes()

    // An alarm's own screen, as a vívoactive 5 sends it.

    /** Screen 64, "Customize", from a real watch: an untargeted switch, a time, and two option lists. */
    private fun alarmScreen(): ByteArray = definitionReply(
        screenId = 64,
        title = "Customize",
        entries = listOf(
            entry(id = 0, title = "Status"),
            entry(id = 1, title = "Time", targetType = 3),
            entry(
                id = 2,
                title = "Repeat",
                targetType = 1,
                options = listOf("Once", "Daily", "Weekday", "Weekend"),
            ),
            entry(
                id = 3,
                title = "Label",
                targetType = 1,
                options = listOf("None", "Wake Up"),
            ),
        ),
    )

    @Test
    fun `reads each row as the control the watch declared`() {
        val screen = parseGarminSettingsScreen(
            alarmScreen(),
            stateReply = stateReply(
                screenId = 64,
                states = listOf(switchState(id = 0, on = true)),
            ),
        )!!

        assertEquals(64, screen.screenId)
        assertEquals("Customize", screen.title)
        assertEquals(
            listOf(
                GarminEntryKind.TOGGLE, // Status — no target, value lives in the state
                GarminEntryKind.TIME, // target type 3
                GarminEntryKind.OPTIONS, // target type 1
                GarminEntryKind.OPTIONS,
            ),
            screen.entries.map { it.kind },
        )
    }

    @Test
    fun `the options come from the WATCH never from this app`() {
        // Repeat is Once/Daily/Weekday/Weekend on this firmware. A hard-coded list would be wrong elsewhere.
        val repeat = parseGarminSettingsScreen(alarmScreen())!!.entries[2]
        assertEquals(
            listOf("Once", "Daily", "Weekday", "Weekend"),
            repeat.options.map { it.title },
        )
        // Index is what a change names, so it must match the order sent.
        assertEquals(listOf(0, 1, 2, 3), repeat.options.map { it.index })
    }

    @Test
    fun `a switch takes its value from the STATE not the definition`() {
        val on = parseGarminSettingsScreen(
            alarmScreen(),
            stateReply = stateReply(64, listOf(switchState(id = 0, on = true))),
        )!!
        val off = parseGarminSettingsScreen(
            alarmScreen(),
            stateReply = stateReply(64, listOf(switchState(id = 0, on = false))),
        )!!

        assertEquals(true, on.entries.first().switchedOn)
        assertEquals(false, off.entries.first().switchedOn)
    }

    @Test
    fun `without a state a switch is neither a toggle NOR a button`() {
        // Its value lives only in the state. Without it every alarm would show OFF,
        // and "Status" would be offered the action reserved for "Delete".
        val screen = parseGarminSettingsScreen(alarmScreen())!!
        assertEquals(GarminEntryKind.INERT, screen.entries.first().kind)
        assertNull(screen.entries.first().switchedOn)
    }

    @Test
    fun `a button is the row the WATCH marked not one we inferred`() {
        // "Delete" on a real alarm screen carries field 9 in its state. Nothing else does.
        val screen = parseGarminSettingsScreen(
            definitionReply(
                screenId = 65600,
                entries = listOf(
                    entry(id = 0, title = "Status"),
                    entry(id = 4, title = "Delete"),
                ),
            ),
            stateReply = stateReply(
                screenId = 65600,
                states = listOf(
                    switchState(id = 0, on = true),
                    removableState(id = 4),
                ),
            ),
        )!!
        assertEquals(GarminEntryKind.TOGGLE, screen.entries.first().kind)
        assertEquals(GarminEntryKind.ACTION, screen.entries.last().kind)
    }

    @Test
    fun `an untargeted row the watch did NOT mark is never a button`() {
        // The root of the tree: Finish Setup, Help & Info, Software Update and Find My Device
        // arrive with no target and a bare state. Read as buttons, they offered a DELETE.
        val screen = parseGarminSettingsScreen(
            definitionReply(
                screenId = 36352,
                entries = listOf(
                    entry(id = 1, title = "Finish Setup"),
                    entry(id = 24, title = "Help & Info"),
                    entry(id = 25, title = "Find My Device"),
                ),
            ),
            stateReply = stateReply(
                screenId = 36352,
                states = listOf(bareState(1), bareState(24), bareState(25)),
            ),
        )!!
        assertTrue(screen.entries.all { it.kind == GarminEntryKind.INERT })
    }

    // Rows a phone cannot act on.

    @Test
    fun `an unused slot is blank and blank rows are droppable`() {
        // A real alarm list came back as twenty untitled rows plus "Add Alarm".
        val screen = parseGarminSettingsScreen(
            definitionReply(
                screenId = 68,
                entries = listOf(
                    entry(id = 0),
                    entry(id = 20, title = "Add Alarm", targetType = 0, subscreen = 999),
                ),
            ),
        )!!
        assertTrue(screen.entries.first().isBlank)
        assertFalse(screen.entries.last().isBlank)
    }

    @Test
    fun `an unhandled target keeps the type it declared`() {
        // "Delete" came out inert, which looks like a hidden row without the control number.
        val screen = parseGarminSettingsScreen(
            definitionReply(
                screenId = 65600,
                entries = listOf(entry(id = 4, title = "Delete", targetType = 11)),
            ),
        )!!
        assertEquals(GarminEntryKind.INERT, screen.entries.single().kind)
        assertEquals(11, screen.entries.single().rawTargetType)
        assertFalse(screen.entries.single().isBlank)
    }

    @Test
    fun `an empty alarm slot leads nowhere`() {
        // The Alarms list points unused slots at screen zero.
        val screen = parseGarminSettingsScreen(
            definitionReply(
                screenId = 68,
                entries = listOf(entry(id = 1, targetType = 0, subscreen = 0)),
            ),
        )!!
        assertEquals(GarminEntryKind.INERT, screen.entries.single().kind)
        assertFalse(screen.entries.single().isActionable)
    }

    @Test
    fun `opens-on-the-watch and hidden are inert not guessed at`() {
        val screen = parseGarminSettingsScreen(
            definitionReply(
                screenId = 36352,
                entries = listOf(
                    entry(id = 2, title = "Garmin Pay", targetType = 6, subscreen = 2),
                    entry(id = 1, title = "Stopwatch", targetType = 7),
                ),
            ),
        )!!
        assertEquals(
            listOf(GarminEntryKind.INERT, GarminEntryKind.INERT),
            screen.entries.map { it.kind },
        )
    }

    @Test
    fun `an unknown target type is inert rather than a guessed widget`() {
        // Garmin's schema is older than the firmware. An unrecognised control must not be rendered as a familiar one.
        val screen = parseGarminSettingsScreen(
            definitionReply(
                screenId = 1,
                entries = listOf(entry(id = 0, title = "Something new", targetType = 99)),
            ),
        )!!
        assertEquals(GarminEntryKind.INERT, screen.entries.single().kind)
        assertEquals("Something new", screen.entries.single().title)
    }

    // The Clocks screen.

    @Test
    fun `a populated alarm is a subscreen whichever target type it uses`() {
        // Type 9 is "subscreen with options"; type 0 is the plain form. Both walk into another screen.
        val screen = parseGarminSettingsScreen(
            definitionReply(
                screenId = 204,
                title = "Clocks",
                entries = listOf(
                    entry(id = 0, title = "7:00 am", targetType = 9, subscreen = 64),
                    entry(id = 3, title = "Time", targetType = 0, subscreen = 738),
                ),
            ),
        )!!

        assertEquals(
            listOf(GarminEntryKind.SUBSCREEN, GarminEntryKind.SUBSCREEN),
            screen.entries.map { it.kind },
        )
        assertEquals(listOf(64, 738), screen.entries.map { it.subscreenId })
    }

    // Telling one screen's reply from another.

    @Test
    fun `a definition names the screen it describes`() {
        assertEquals(
            65600,
            GarminSettingsService.screenIdOf(
                definitionReply(screenId = 65600),
                GarminSettingsService.DEFINITION_RESPONSE_FIELD,
            ),
        )
    }

    @Test
    fun `a state names it too`() {
        assertEquals(
            68,
            GarminSettingsService.screenIdOf(
                stateReply(screenId = 68),
                GarminSettingsService.STATE_RESPONSE_FIELD,
            ),
        )
    }

    @Test
    fun `a change response names it from a field of its own`() {
        // A delete response nests its screen at field 3, not field 2.
        val reply = byteArrayOf(
            0xd2.toByte(), 0x02, 0x19, 0x32, 0x17, 0x08, 0x00, 0x1a, 0x11,
            0x08, 0xc0.toByte(), 0x80.toByte(), 0x8c.toByte(), 0x08, 0x10, 0x00,
            0x18, 0xa8.toByte(), 0x88.toByte(), 0x68,
            0x22, 0x04, 0x08, 0x04, 0x4a, 0x00, 0x28, 0x01,
        )
        assertEquals(
            16973888,
            GarminSettingsService.screenIdOf(
                reply,
                GarminSettingsService.CHANGE_RESPONSE_FIELD,
            ),
        )
    }

    @Test
    fun `a reply about another screen is not this screen's answer`() {
        // The watch retransmits, so the alarm list's definition arrived while one alarm's screen
        // was pending and was taken as the answer.
        val list = GarminSettingsService.screenIdOf(
            definitionReply(screenId = 68),
            GarminSettingsService.DEFINITION_RESPONSE_FIELD,
        )
        assertTrue(list != 65600)
    }

    // The value behind a row, as the watch reports it.

    /** The state for an alarm's Repeat and Time rows: a chosen option is a position, a time is seconds since midnight. */
    private fun valueState(): ByteArray = stateReply(
        screenId = 65600,
        states = listOf(
            ProtobufWriter()
                .varint(1, 2)
                .nested(
                    4,
                    ProtobufWriter()
                        .nested(1, label("Once"))
                        .nested(2, ProtobufWriter().varint(1, 0).toBytes())
                        .toBytes(),
                )
                .toBytes(),
            ProtobufWriter()
                .varint(1, 1)
                .nested(
                    4,
                    ProtobufWriter()
                        .nested(1, label("11:10 am"))
                        .nested(4, ProtobufWriter().varint(1, 40200).toBytes())
                        .toBytes(),
                )
                .toBytes(),
        ),
    )

    private fun valueDefinition(): ByteArray = definitionReply(
        screenId = 65600,
        entries = listOf(
            entry(id = 1, title = "Time", targetType = 3),
            entry(
                id = 2,
                title = "Repeat",
                targetType = 1,
                options = listOf("Once", "Daily", "Weekday", "Weekend"),
            ),
        ),
    )

    @Test
    fun `a chosen option is a position not the summary text`() {
        // Matching the summary against option titles failed once a summary came back empty.
        val repeat = parseGarminSettingsScreen(valueDefinition(), stateReply = valueState())!!
            .entries
            .first { it.id == 2 }
        assertEquals(0, repeat.selectedIndex)
        assertEquals("Once", repeat.options[repeat.selectedIndex!!].title)
    }

    @Test
    fun `a time comes back as the time not just its rendering`() {
        // 40200 seconds is 11:10. Starting the picker from "now" reset the alarm.
        val time = parseGarminSettingsScreen(valueDefinition(), stateReply = valueState())!!
            .entries
            .first { it.id == 1 }
        assertEquals(11.hours + 10.minutes, time.time)
    }

    // Degenerate replies.

    @Test
    fun `a nameless row is hidden even when it carries a value`() {
        // After a delete the freed slots came back with a leftover summary and no title.
        val screen = parseGarminSettingsScreen(
            definitionReply(screenId = 68, entries = listOf(entry(id = 2))),
        )!!
        assertTrue(screen.entries.single().isBlank)
    }

    @Test
    fun `a reply that is not a definition yields no screen`() {
        assertNull(parseGarminSettingsScreen(null))
        assertNull(parseGarminSettingsScreen(byteArrayOf(0xFF.toByte(), 0xFF.toByte())))
        // A STATE reply is not a definition.
        assertNull(parseGarminSettingsScreen(stateReply(screenId = 64)))
    }
}
