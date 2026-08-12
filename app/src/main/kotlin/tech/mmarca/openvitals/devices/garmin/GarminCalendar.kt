package tech.mmarca.openvitals.devices.garmin

/**
 * One event the phone can offer the watch's calendar glance.
 *
 * Times are epoch seconds. All-day events keep their calendar-provider
 * convention (midnight UTC boundaries); the responder shifts them to local
 * midnight the way the watch expects.
 */
data class GarminCalendarEvent(
    val title: String,
    val location: String? = null,
    val description: String? = null,
    val organizer: String? = null,
    val startEpochSeconds: Long,
    val endEpochSeconds: Long,
    val allDay: Boolean = false,
)

/**
 * Answers the watch's calendar asks (`GdiCalendarService`).
 *
 * The watch names the window, the field lengths and the event cap it can
 * take, and re-asks periodically for as long as the link lives — there is no
 * push. Ported from Gadgetbridge's `processProtobufCalendarRequest`.
 *
 * [eventsProvider] returns the events overlapping the asked window, or null
 * when calendar sync is off for this watch — which still gets an OK with no
 * events, upstream's answer, because an UNANSWERED ask is re-sent every
 * thirty seconds forever.
 */
class GarminCalendarResponder(
    private val eventsProvider: ((beginEpochSeconds: Long, endEpochSeconds: Long) -> List<GarminCalendarEvent>?)? = null,
    private val zoneOffsetSeconds: (epochSeconds: Long) -> Long = { epochSeconds ->
        java.util.TimeZone.getDefault().getOffset(epochSeconds * 1000L) / 1000L
    },
) {

    /** Handles one `Smart` message; null when it is not a calendar ask. */
    fun handle(payload: ByteArray): ByteArray? {
        val service = protobufField(readProtobuf(payload), GarminSmartService.CALENDAR)?.bytes
            ?: return null
        val request = protobufField(readProtobuf(service), CALENDAR_REQUEST)?.bytes
            // A calendar message that is not a request (a stray response,
            // some future shape): claimed but unknown, as upstream answers.
            ?: return calendarResponse(STATUS_UNKNOWN, emptyList())

        val fields = readProtobuf(request)
        val begin = protobufField(fields, REQ_BEGIN)?.varint ?: 0L
        val end = protobufField(fields, REQ_END)?.varint ?: Long.MAX_VALUE
        val includeAllDay = protobufField(fields, REQ_INCLUDE_ALL_DAY)?.varint == 1L
        val includeLocation = protobufField(fields, REQ_INCLUDE_LOCATION)?.varint != 0L
        val includeDescription = protobufField(fields, REQ_INCLUDE_DESCRIPTION)?.varint == 1L
        val includeOrganizer = protobufField(fields, REQ_INCLUDE_ORGANIZER)?.varint == 1L
        val maxTitle = protobufField(fields, REQ_MAX_TITLE)?.varint?.toInt() ?: DEFAULT_MAX_TEXT
        val maxLocation =
            protobufField(fields, REQ_MAX_LOCATION)?.varint?.toInt() ?: DEFAULT_MAX_TEXT
        val maxDescription =
            protobufField(fields, REQ_MAX_DESCRIPTION)?.varint?.toInt() ?: DEFAULT_MAX_TEXT
        val maxOrganizer =
            protobufField(fields, REQ_MAX_ORGANIZER)?.varint?.toInt() ?: DEFAULT_MAX_TEXT
        val maxEvents = protobufField(fields, REQ_MAX_EVENTS)?.varint?.toInt() ?: DEFAULT_MAX_EVENTS

        val events = eventsProvider?.invoke(begin, end)
        if (events == null) {
            GarminLog.log("[GARMIN-CAL] watch asked for events; calendar sync is off")
            return calendarResponse(STATUS_OK, emptyList())
        }

        val sent = events.asSequence()
            .filter { it.endEpochSeconds >= begin && it.startEpochSeconds <= end }
            .filter { includeAllDay || !it.allDay }
            // Upstream ships up to double the reported max without issue, but
            // the reported number is the one the watch stands behind.
            .take(maxEvents)
            .map { event ->
                // All-day events carry midnight-UTC boundaries; the watch
                // wants midnight in the wearer's day.
                val shift = if (event.allDay) -zoneOffsetSeconds(event.startEpochSeconds) else 0L
                ProtobufWriter().apply {
                    if (includeOrganizer && event.organizer != null) {
                        string(EVENT_ORGANIZER, event.organizer.take(maxOrganizer))
                    }
                    string(EVENT_TITLE, event.title.take(maxTitle))
                    if (includeLocation && event.location != null) {
                        string(EVENT_LOCATION, event.location.take(maxLocation))
                    }
                    if (includeDescription && event.description != null) {
                        string(EVENT_DESCRIPTION, event.description.take(maxDescription))
                    }
                    varint(EVENT_START, event.startEpochSeconds + shift)
                    varint(EVENT_END, event.endEpochSeconds + shift)
                    varint(EVENT_ALL_DAY, if (event.allDay) 1 else 0)
                }.toBytes()
            }
            .toList()

        GarminLog.log("[GARMIN-CAL] sending ${sent.size} event(s) to the watch")
        return calendarResponse(STATUS_OK, sent)
    }

    private fun calendarResponse(status: Int, events: List<ByteArray>): ByteArray {
        val response = ProtobufWriter().varint(RESP_STATUS, status)
        for (event in events) response.nested(RESP_EVENT, event)
        return ProtobufWriter()
            .nested(
                GarminSmartService.CALENDAR,
                ProtobufWriter().nested(CALENDAR_RESPONSE, response.toBytes()).toBytes(),
            )
            .toBytes()
    }

    // ── field numbers (gdi_calendar_service.proto) ──────────────────────────
    private companion object {
        const val CALENDAR_REQUEST = 1
        const val CALENDAR_RESPONSE = 2

        const val REQ_BEGIN = 1
        const val REQ_END = 2
        const val REQ_INCLUDE_ORGANIZER = 3
        const val REQ_INCLUDE_LOCATION = 5
        const val REQ_INCLUDE_DESCRIPTION = 6
        const val REQ_INCLUDE_ALL_DAY = 9
        const val REQ_MAX_ORGANIZER = 10
        const val REQ_MAX_TITLE = 11
        const val REQ_MAX_LOCATION = 12
        const val REQ_MAX_DESCRIPTION = 13
        const val REQ_MAX_EVENTS = 14

        const val RESP_STATUS = 1
        const val RESP_EVENT = 2

        const val EVENT_ORGANIZER = 1
        const val EVENT_TITLE = 2
        const val EVENT_LOCATION = 3
        const val EVENT_DESCRIPTION = 4
        const val EVENT_START = 5
        const val EVENT_END = 6
        const val EVENT_ALL_DAY = 7

        const val STATUS_UNKNOWN = 0
        const val STATUS_OK = 1

        /** For a watch that names no limits — generous, since none observed do. */
        const val DEFAULT_MAX_TEXT = 100
        const val DEFAULT_MAX_EVENTS = 20
    }
}
