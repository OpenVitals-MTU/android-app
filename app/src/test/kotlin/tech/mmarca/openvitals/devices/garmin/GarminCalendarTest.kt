package tech.mmarca.openvitals.devices.garmin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The calendar glance responder. The watch asks with a window and its own limits; there is no push. */
class GarminCalendarTest {

    /** 2026-08-12T08:00:00Z and friends. */
    private val begin = 1_786_608_000L

    private fun event(
        title: String = "Standup",
        start: Long = begin + 3_600,
        end: Long = start + 1_800,
        allDay: Boolean = false,
        location: String? = null,
        description: String? = null,
        organizer: String? = null,
    ) = GarminCalendarEvent(
        title = title,
        location = location,
        description = description,
        organizer = organizer,
        startEpochSeconds = start,
        endEpochSeconds = end,
        allDay = allDay,
    )

    /** The ask the way the watch frames it. */
    private fun request(
        window: Pair<Long, Long> = begin to begin + 7 * 86_400,
        maxEvents: Int = 10,
        maxTitle: Int = 30,
        includeAllDay: Boolean = false,
        includeLocation: Boolean = true,
        includeDescription: Boolean = false,
    ): ByteArray {
        val ask = ProtobufWriter()
            .varint(1, window.first)
            .varint(2, window.second)
            .varint(5, if (includeLocation) 1 else 0)
            .varint(6, if (includeDescription) 1 else 0)
            .varint(9, if (includeAllDay) 1 else 0)
            .varint(11, maxTitle)
            .varint(14, maxEvents)
            .toBytes()
        return ProtobufWriter()
            .nested(
                GarminSmartService.CALENDAR,
                ProtobufWriter().nested(1, ask).toBytes(),
            )
            .toBytes()
    }

    private fun responder(
        events: List<GarminCalendarEvent>?,
        offsetSeconds: Long = 3 * 3_600, // Tallinn in summer
    ) = GarminCalendarResponder(
        eventsProvider = { _, _ -> events },
        zoneOffsetSeconds = { offsetSeconds },
    )

    /** Unwraps `Smart > CalendarService > CalendarServiceResponse`. */
    private fun response(reply: ByteArray): List<ProtobufField> {
        val service = protobufField(readProtobuf(reply), GarminSmartService.CALENDAR)!!.bytes!!
        return readProtobuf(protobufField(readProtobuf(service), 2)!!.bytes!!)
    }

    private fun events(reply: ByteArray): List<List<ProtobufField>> =
        response(reply).filter { it.field == 2 }.map { readProtobuf(it.bytes!!) }

    private fun title(event: List<ProtobufField>): String =
        protobufField(event, 2)!!.bytes!!.toString(Charsets.UTF_8)

    @Test
    fun `events inside the window reach the watch`() {
        val reply = responder(listOf(event(title = "Standup"))).handle(request())!!

        assertEquals(1L, protobufField(response(reply), 1)?.varint) // OK
        val sent = events(reply)
        assertEquals(1, sent.size)
        assertEquals("Standup", title(sent[0]))
        assertEquals(begin + 3_600, protobufField(sent[0], 5)?.varint)
    }

    @Test
    fun `sync off answers OK with nothing, never silence`() {
        // Unanswered, the watch re-asks every thirty seconds forever.
        val reply = responder(events = null).handle(request())!!

        assertEquals(1L, protobufField(response(reply), 1)?.varint)
        assertTrue(events(reply).isEmpty())
    }

    @Test
    fun `the watch's own limits are respected`() {
        val crowded = (1..30).map { event(title = "Event number $it with a very long name indeed") }

        val reply = responder(crowded).handle(request(maxEvents = 5, maxTitle = 12))!!

        val sent = events(reply)
        // The watch said 5 events of 12 characters; sending more asks a 176-pixel screen to cope.
        assertEquals(5, sent.size)
        assertEquals("Event number", title(sent[0]))
    }

    @Test
    fun `events outside the window are not sent`() {
        val nextMonth = event(start = begin + 40 * 86_400, end = begin + 40 * 86_400 + 3_600)
        val lastWeek = event(start = begin - 7 * 86_400, end = begin - 7 * 86_400 + 3_600)

        val reply = responder(listOf(nextMonth, lastWeek)).handle(request())!!

        assertTrue(events(reply).isEmpty())
    }

    @Test
    fun `all-day events are filtered or shifted as the watch asked`() {
        // An all-day event carries midnight-UTC boundaries from the provider.
        val utcMidnight = 1_786_579_200L // 2026-08-13T00:00:00Z
        val allDay = event(
            title = "Holiday",
            start = utcMidnight,
            end = utcMidnight + 86_400,
            allDay = true,
        )

        // A watch that did not ask for all-day events is not sent them.
        assertTrue(events(responder(listOf(allDay)).handle(request(includeAllDay = false))!!).isEmpty())

        // One that did wants them at LOCAL midnight, not 03:00.
        val sent = events(responder(listOf(allDay)).handle(request(includeAllDay = true))!!)
        assertEquals(utcMidnight - 3 * 3_600, protobufField(sent[0], 5)?.varint)
        assertEquals(1L, protobufField(sent[0], 7)?.varint)
    }

    @Test
    fun `optional fields ride only when asked for`() {
        val full = event(location = "Office", description = "Bring slides")

        val minimal = events(
            responder(listOf(full)).handle(
                request(includeLocation = false, includeDescription = false),
            )!!,
        )[0]
        assertNull(protobufField(minimal, 3))
        assertNull(protobufField(minimal, 4))

        val verbose = events(
            responder(listOf(full)).handle(
                request(includeLocation = true, includeDescription = true),
            )!!,
        )[0]
        assertEquals("Office", protobufField(verbose, 3)!!.bytes!!.toString(Charsets.UTF_8))
        assertEquals("Bring slides", protobufField(verbose, 4)!!.bytes!!.toString(Charsets.UTF_8))
    }

    @Test
    fun `messages for other services are left alone`() {
        val smart = ProtobufWriter()
            .nested(GarminSmartService.SETTINGS, ByteArray(0))
            .toBytes()
        assertNull(responder(emptyList()).handle(smart))
    }
}
