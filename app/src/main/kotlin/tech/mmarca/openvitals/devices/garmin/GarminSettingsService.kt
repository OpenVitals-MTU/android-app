package tech.mmarca.openvitals.devices.garmin

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * The watch's own settings tree, over the protobuf settings service.
 *
 * The app defines none of this: the watch sends a MENU — screens, entries,
 * titles and option lists — already translated into whatever locale it is
 * handed. This layer asks for a screen and reports what came back; deciding
 * how to draw it is somebody else's job.
 *
 * Field numbers from Gadgetbridge's `gdi_settings_service.proto` (AGPLv3).
 * That schema is older than this watch's firmware, so anything unrecognised is
 * carried rather than dropped — a settings tree read wrongly produces a screen
 * of plausible but incorrect controls, which is worse than one that is missing.
 *
 * Port of the Flutter build's `garmin_settings_service.dart`, byte for byte.
 */
object GarminSettingsService {

    // SettingsService fields.
    private const val DEFINITION_REQUEST = 1
    private const val DEFINITION_RESPONSE = 2
    private const val STATE_REQUEST = 3
    private const val STATE_RESPONSE = 4
    private const val INIT_REQUEST = 8

    // ScreenDefinitionRequest fields.
    private const val REQ_SCREEN_ID = 1
    private const val REQ_UNK_2 = 2
    private const val REQ_LANGUAGE = 3

    // ScreenDefinition / ScreenEntry / Target fields.
    private const val DEF_ENTRY = 5
    private const val ENTRY_TITLE = 3
    private const val ENTRY_TARGET = 9
    private const val TARGET_TYPE = 1
    private const val TARGET_SUBSCREEN = 2
    private const val TARGET_SUBSCREEN_WITH_OPTIONS = 9
    private const val LABEL_TEXT = 2

    // ChangeRequest fields.
    private const val CHANGE_REQUEST = 5
    private const val CHANGE_RESPONSE = 6
    private const val CHANGE_SCREEN_ID = 1
    private const val CHANGE_ENTRY_ID = 2
    private const val CHANGE_SWITCH = 3
    private const val CHANGE_OPTION = 4
    private const val CHANGE_TIME = 6
    private const val CHANGE_NUMBER = 8
    private const val CHANGE_POSITION = 11
    private const val POSITION_DELETE = 2

    // Where each response nests the thing it is about, and where that thing
    // names its screen. Read off a vívoactive 5: the definition and state
    // responses use field 2, the change response field 3.
    private const val RESPONSE_INNER = 2
    private const val CHANGE_RESPONSE_INNER = 3
    private const val RESPONSE_SCREEN_ID = 1

    // InitRequest fields.
    private const val INIT_LANGUAGE = 1
    private const val INIT_REGION = 2

    /** The tree's root, from Gadgetbridge's `GarminRealtimeSettingsFragment`. */
    const val ROOT_SCREEN_ID = 36352

    /**
     * The Alarms list, measured on a vívoactive 5 (Settings → Clocks →
     * Alarms).
     *
     * A well-known id rather than a walk from the root: reaching it by walking
     * costs four round trips every time somebody taps Alarms, and the id has
     * been stable across every read of this watch. If a future model moves it,
     * the screen will come back empty rather than wrong — which is why the
     * caller reports "the watch sent nothing" instead of inventing a list.
     */
    const val ALARMS_SCREEN_ID = 68

    /**
     * How long the watch may take to build a screen.
     *
     * Measured: a root definition arrived after more than ten seconds, so the
     * transport's default timed the request out and a later, unrelated
     * settings message was mistaken for the answer.
     */
    val REPLY_TIMEOUT: Duration = 30.seconds

    /** The SettingsService field a reply of each kind arrives in. */
    const val DEFINITION_RESPONSE_FIELD = DEFINITION_RESPONSE
    const val STATE_RESPONSE_FIELD = STATE_RESPONSE
    const val CHANGE_RESPONSE_FIELD = CHANGE_RESPONSE

    /**
     * Whether [reply] carries the response field [responseField].
     *
     * Correlating on "is this a settings message" was not enough: the watch
     * sends several, and the first to arrive was a five-byte one on field 7
     * that answered nothing we asked.
     */
    fun carries(reply: ByteArray?, responseField: Int): Boolean {
        val service = unwrap(reply) ?: return false
        return protobufField(readProtobuf(service), responseField) != null
    }

    /**
     * Which SCREEN a reply is about, or null if it does not say.
     *
     * Every response nests the screen it describes at the same place, which is
     * the only way to tell one from another: the watch retransmits anything it
     * thinks went unacknowledged, so a definition for a screen asked about
     * minutes ago can arrive while a different one is pending. Matching on the
     * response field alone handed the alarm LIST's layout back as the answer
     * for one alarm's screen, which drew a list of rows whose titles and
     * values came from two different places.
     */
    fun screenIdOf(reply: ByteArray?, responseField: Int): Int? {
        val service = unwrap(reply) ?: return null
        val response =
            protobufField(readProtobuf(service), responseField)?.bytes ?: return null
        val innerField =
            if (responseField == CHANGE_RESPONSE) CHANGE_RESPONSE_INNER else RESPONSE_INNER
        val inner = protobufField(readProtobuf(response), innerField)?.bytes ?: return null
        return protobufField(readProtobuf(inner), RESPONSE_SCREEN_ID)?.varint?.toInt()
    }

    /**
     * Opens the settings service for a locale.
     *
     * The watch translates every title it later sends using this, so it
     * decides what language the whole tree comes back in.
     */
    fun init(language: String = "en_US", region: String = "us"): ByteArray {
        val request = ProtobufWriter()
            .string(INIT_LANGUAGE, language)
            .string(INIT_REGION, region)
            .toBytes()
        val service = ProtobufWriter().nested(INIT_REQUEST, request).toBytes()
        return smart(service)
    }

    /** Asks for one screen's DEFINITION — its title and the entries on it. */
    fun screenDefinition(screenId: Int, language: String = "en_US"): ByteArray {
        val request = ProtobufWriter()
            .varint(REQ_SCREEN_ID, screenId)
            .varint(REQ_UNK_2, 0)
            .string(REQ_LANGUAGE, language)
            .toBytes()
        val service = ProtobufWriter().nested(DEFINITION_REQUEST, request).toBytes()
        return smart(service)
    }

    /**
     * Asks for one screen's STATE — the current value behind each entry.
     *
     * Separate from the definition because the two change on different clocks:
     * the layout is fixed for a firmware, the values move as the watch is
     * used.
     */
    fun screenState(screenId: Int): ByteArray {
        val request = ProtobufWriter().varint(REQ_SCREEN_ID, screenId).toBytes()
        val service = ProtobufWriter().nested(STATE_REQUEST, request).toBytes()
        return smart(service)
    }

    private fun smart(service: ByteArray): ByteArray =
        ProtobufWriter().nested(GarminSmartService.SETTINGS, service).toBytes()

    /** The settings payload inside a `Smart` reply, or null if it carries none. */
    fun unwrap(reply: ByteArray?): ByteArray? {
        if (reply == null) return null
        return protobufField(readProtobuf(reply), GarminSmartService.SETTINGS)?.bytes
    }

    /** Whether a reply carries a screen definition. */
    fun hasDefinition(reply: ByteArray?): Boolean {
        val service = unwrap(reply) ?: return false
        return protobufField(readProtobuf(service), DEFINITION_RESPONSE) != null
    }

    /** Whether a reply carries a screen state. */
    fun hasState(reply: ByteArray?): Boolean {
        val service = unwrap(reply) ?: return false
        return protobufField(readProtobuf(service), STATE_RESPONSE) != null
    }

    /**
     * Changes ONE entry on ONE screen.
     *
     * The only write in this whole stack — everything else reads. A malformed
     * change does not fail politely: it is applied to a real watch someone
     * depends on, so each value kind is a separate builder rather than one
     * generic setter that could put a time in a switch's field.
     *
     * The reply carries a status AND the screen's new state, so a caller can
     * confirm what the watch actually did rather than assume the request
     * landed.
     */
    fun changeSwitch(screenId: Int, entryId: Int, value: Boolean): ByteArray =
        change(
            screenId,
            entryId,
            CHANGE_SWITCH,
            ProtobufWriter().varint(1, if (value) 1 else 0).toBytes(),
        )

    /**
     * [index] is a position in the option list the DEFINITION supplied for
     * this entry — never a guessed ordinal.
     */
    fun changeOption(screenId: Int, entryId: Int, index: Int): ByteArray =
        change(screenId, entryId, CHANGE_OPTION, ProtobufWriter().varint(1, index).toBytes())

    /** Seconds since midnight, which is how the watch stores a time of day. */
    fun changeTime(screenId: Int, entryId: Int, sinceMidnight: Duration): ByteArray {
        val seconds = sinceMidnight.inWholeSeconds
        require(seconds in 0 until SECONDS_PER_DAY) {
            "sinceMidnight is $seconds s — " +
                "must be within one day, the watch takes seconds since midnight"
        }
        return change(
            screenId,
            entryId,
            CHANGE_TIME,
            ProtobufWriter().varint(1, seconds).toBytes(),
        )
    }

    /**
     * Activates a row that deletes something.
     *
     * The row carries no target at all — it is a button, not a setting — and
     * `ChangeRequest` has no field for "activate this". `Position { index,
     * delete }` is its only delete-shaped member, and a vívoactive 5 accepted
     * it and removed the alarm, so that is what this sends.
     */
    fun changeDelete(screenId: Int, entryId: Int): ByteArray =
        change(
            screenId,
            entryId,
            CHANGE_POSITION,
            ProtobufWriter().varint(POSITION_DELETE, 1).toBytes(),
        )

    fun changeNumber(screenId: Int, entryId: Int, value: Int): ByteArray =
        change(screenId, entryId, CHANGE_NUMBER, ProtobufWriter().varint(1, value).toBytes())

    private fun change(
        screenId: Int,
        entryId: Int,
        valueField: Int,
        value: ByteArray,
    ): ByteArray {
        val request = ProtobufWriter()
            .varint(CHANGE_SCREEN_ID, screenId)
            .varint(CHANGE_ENTRY_ID, entryId)
            .nested(valueField, value)
            .toBytes()
        val service = ProtobufWriter().nested(CHANGE_REQUEST, request).toBytes()
        return smart(service)
    }

    /**
     * What the watch made of a change: null when it did not answer with one.
     *
     * SUCCESS is 0 here — unlike the find service, where OK is 100. Two
     * enums, two meanings for zero, which is exactly the kind of thing that
     * turns a refusal into a silent success.
     */
    fun changeSucceeded(reply: ByteArray?): Boolean? {
        val service = unwrap(reply) ?: return null
        val response =
            protobufField(readProtobuf(service), CHANGE_RESPONSE)?.bytes ?: return null
        val status = protobufField(readProtobuf(response), 1)?.varint
        // Absent status on a present response means it was accepted.
        return status == null || status == 0L
    }

    /** The entries on a definition reply's screen that lead somewhere else. */
    fun subscreens(reply: ByteArray): List<GarminSettingsSubscreen> {
        val service = unwrap(reply) ?: return emptyList()
        val response =
            protobufField(readProtobuf(service), DEFINITION_RESPONSE)?.bytes
                ?: return emptyList()
        val definition = protobufField(readProtobuf(response), 2)?.bytes ?: return emptyList()

        val out = mutableListOf<GarminSettingsSubscreen>()
        for (field in readProtobuf(definition)) {
            if (field.field != DEF_ENTRY) continue
            val entry = field.bytes ?: continue
            val fields = readProtobuf(entry)
            val target = protobufField(fields, ENTRY_TARGET)?.bytes ?: continue
            val targetFields = readProtobuf(target)
            // Types 0 and 9 are both "another screen" — 9 carries an option
            // list with it, which is what an alarm's own screen is. Type 6
            // opens an activity ON the watch and 7 is hidden; neither can be
            // walked into.
            val targetType = protobufField(targetFields, TARGET_TYPE)?.varint
            if (targetType != 0L && targetType != TARGET_SUBSCREEN_WITH_OPTIONS.toLong()) {
                continue
            }
            val screenId = protobufField(targetFields, TARGET_SUBSCREEN)?.varint?.toInt()
            // Screen zero is how an EMPTY slot is written — an alarm list
            // reserves a row per slot and points the unused ones at nothing.
            // Requesting it would ask the watch for a screen that does not
            // exist.
            if (screenId == null || screenId == 0) continue

            var title: String? = null
            val label = protobufField(fields, ENTRY_TITLE)?.bytes
            if (label != null) {
                val text = protobufField(readProtobuf(label), LABEL_TEXT)?.bytes
                if (text != null) title = String(text, Charsets.ISO_8859_1)
            }
            out.add(GarminSettingsSubscreen(screenId = screenId, title = title))
        }
        return out
    }

    /**
     * Prints a reply's structure, field by field, without interpreting it.
     *
     * The point of the first exchange is to SEE what the watch sends. Naming
     * the fields we think we recognise while still showing everything else is
     * what separates "the schema matches" from "the schema is close enough to
     * look like it matches".
     */
    fun describe(payload: ByteArray, indent: String = "  ") {
        for (field in readProtobuf(payload)) {
            val bytes = field.bytes
            if (bytes == null) {
                GarminLog.log("$indent${field.field}: ${field.varint}")
                continue
            }
            val text = asText(bytes)
            if (text != null) {
                GarminLog.log("$indent${field.field}: \"$text\"")
                continue
            }
            val nested = readProtobuf(bytes)
            if (nested.isEmpty()) {
                GarminLog.log("$indent${field.field}: (${bytes.size}B) ${hex(bytes)}")
                continue
            }
            GarminLog.log("$indent${field.field}: {")
            describe(bytes, indent = "$indent  ")
            GarminLog.log("$indent}")
        }
    }

    /**
     * Printable ASCII only — the watch sends titles as UTF-8 strings, and
     * guessing that arbitrary bytes are text turns a nested message into
     * mojibake.
     */
    private fun asText(bytes: ByteArray): String? {
        if (bytes.isEmpty()) return null
        for (byte in bytes) {
            val value = byte.toInt() and 0xFF
            if (value < 0x20 || value > 0x7E) return null
        }
        return String(bytes, Charsets.ISO_8859_1)
    }

    private fun hex(bytes: ByteArray): String =
        bytes.joinToString(" ") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }

    private const val SECONDS_PER_DAY = 24L * 60L * 60L
}

/** An entry that leads to another screen: where it goes, and what it is called. */
class GarminSettingsSubscreen(
    val screenId: Int,
    val title: String?,
)
