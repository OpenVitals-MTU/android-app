package tech.mmarca.openvitals.devices.garmin

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * What a settings entry actually is, once its target has been read.
 *
 * The watch decides this, not the app: it sends a menu whose every row
 * declares its own control. An entry read as the wrong kind produces a
 * plausible screen with the wrong widget on it — a list where a time should
 * be — which is worse than one that fails to render.
 *
 * Port of the Flutter build's `garmin_settings_model.dart`, byte for byte.
 */
enum class GarminEntryKind {
    /** Opens another screen. Alarms is one of these on the Clocks screen. */
    SUBSCREEN,

    /**
     * One of a set of options the watch supplies with the entry — never a
     * list this app invents.
     */
    OPTIONS,

    /** A time of day, changed as seconds since midnight. */
    TIME,

    /** An on/off switch, which carries no target at all. */
    TOGGLE,

    /** A number the watch bounds itself. */
    NUMBER,

    /**
     * A button rather than a setting: the watch MARKS this row as one it can
     * be asked to remove. Its "Delete" row on an alarm is the case this was
     * built for, and the mark is what says so — see
     * [GarminSettingsEntry.isRemovable] in the Dart original; here the mark
     * arrives as this kind directly.
     */
    ACTION,

    /**
     * Present on the screen but not something a phone can act on: it opens
     * something ON the watch, or is hidden outright.
     */
    INERT,
}

/** One option the watch offered for an [GarminEntryKind.OPTIONS] entry. */
class GarminSettingsOption(
    /**
     * Position in the list the watch sent. A change names THIS, so it must
     * never be a guessed ordinal.
     */
    val index: Int,
    val title: String,
)

/** One row on a settings screen. */
class GarminSettingsEntry(
    /** Identifies the entry within its screen — what a change names. */
    val id: Int,
    val kind: GarminEntryKind,
    /** What the watch calls it, already in the requested language. */
    val title: String? = null,
    /**
     * The current value as the watch renders it, when it sent one. Display
     * text, never parsed: "7:00 am" is the watch's formatting, not ours to
     * reproduce.
     */
    val summary: String? = null,
    val subscreenId: Int? = null,
    val options: List<GarminSettingsOption> = emptyList(),
    /**
     * Only meaningful for [GarminEntryKind.TOGGLE]; null when the state has
     * not been read.
     */
    val switchedOn: Boolean? = null,
    /**
     * The target type the watch declared, kept even when it is one this app
     * does not handle. An entry that came out [GarminEntryKind.INERT] is
     * otherwise indistinguishable from a hidden row, and the number is what
     * says which control it really is.
     */
    val rawTargetType: Int? = null,
    /**
     * Which of [options] the watch says is chosen, as a POSITION in the list
     * it sent.
     *
     * The value, not a re-derivation of it: matching the summary text against
     * the option titles worked until a screen arrived whose summary was
     * empty, and then every option looked unselected.
     */
    val selectedIndex: Int? = null,
    /**
     * The time behind a [GarminEntryKind.TIME] row, as the watch holds it.
     *
     * Opening the picker at the CURRENT time instead meant every edit started
     * from the wrong number, which is how a nudge to an alarm becomes a
     * reset.
     */
    val time: Duration? = null,
    /** What a number is measured in, when the watch said. Its own word for it. */
    val unit: String? = null,
) {

    /**
     * A row carrying nothing a person could read — an unused slot in a list
     * that reserves one per position. Worth hiding rather than drawing as a
     * blank.
     *
     * An untitled inert row counts even when it carries a summary: a value
     * with no name is not something anybody can read. Real alarms put their
     * time in the TITLE, so nothing legible is lost.
     */
    val isBlank: Boolean
        get() = kind == GarminEntryKind.INERT && title.isNullOrBlank()

    /** Whether a phone can do anything with this row. */
    val isActionable: Boolean
        get() = kind != GarminEntryKind.INERT
}

/** One screen: what it is called, and the rows on it. */
class GarminSettingsScreen(
    val screenId: Int,
    val title: String? = null,
    val entries: List<GarminSettingsEntry> = emptyList(),
    /**
     * Whether the watch supplied the CURRENT VALUES alongside the layout.
     *
     * False leaves every switch inert, which on its own looks like a bug
     * rather than a missing reply — so the screen says which it is.
     */
    val hasState: Boolean = true,
) {
    val isEmpty: Boolean get() = entries.isEmpty()
}

/**
 * Turns a definition reply — and optionally the matching state reply — into a
 * screen.
 *
 * The two are separate requests because they answer different questions: the
 * definition says there is a "Repeat" row with four options, the state says
 * it is currently "Weekday". A screen built from the definition alone cannot
 * show what anything is set to.
 */
fun parseGarminSettingsScreen(
    definitionReply: ByteArray?,
    stateReply: ByteArray? = null,
): GarminSettingsScreen? {
    val definition = definitionOf(definitionReply) ?: return null

    val fields = readProtobuf(definition)
    val screenId = protobufField(fields, SCREEN_ID)?.varint?.toInt() ?: return null

    val states = statesById(stateReply)
    val entries = mutableListOf<GarminSettingsEntry>()
    for (field in fields) {
        if (field.field != ENTRY) continue
        val entry = parseEntry(field.bytes, states, stateReply != null)
        if (entry != null) entries.add(entry)
    }

    return GarminSettingsScreen(
        screenId = screenId,
        title = labelText(protobufField(fields, SCREEN_TITLE)?.bytes),
        entries = entries,
        hasState = stateReply != null && states.isNotEmpty(),
    )
}

private fun parseEntry(
    bytes: ByteArray?,
    states: Map<Int, EntryState>,
    stateAvailable: Boolean,
): GarminSettingsEntry? {
    if (bytes == null) return null
    val fields = readProtobuf(bytes)
    val id = protobufField(fields, ENTRY_ID)?.varint?.toInt() ?: return null

    val title = labelText(protobufField(fields, ENTRY_TITLE)?.bytes)
    val state = states[id]
    val target = protobufField(fields, ENTRY_TARGET)?.bytes

    // No target at all: a switch, which carries its value in the STATE rather
    // than declaring anything in the definition.
    if (target == null) {
        // A switch and a button are both target-less. What separates them is
        // not the absence of anything — it is a MARK the watch puts on the row
        // it will accept a removal for, and only on that row.
        //
        // Inferring the button from "no target, but it has a name" was wrong
        // and dangerous: at the root of the tree it caught Finish Setup,
        // Shortcut, Help & Info, Software Update and Find My Device, none of
        // which are buttons. Tapping Find My Device sent the watch a DELETE
        // for that row. It refused — by its own choice, not because the app
        // stopped itself.
        val kind = when {
            state?.switchedOn != null -> GarminEntryKind.TOGGLE
            stateAvailable && state?.removable == true -> GarminEntryKind.ACTION
            else -> GarminEntryKind.INERT
        }
        return GarminSettingsEntry(
            id = id,
            kind = kind,
            title = title,
            summary = state?.summary,
            switchedOn = state?.switchedOn,
        )
    }

    val targetFields = readProtobuf(target)
    val targetType = protobufField(targetFields, TARGET_TYPE)?.varint?.toInt()
    val subscreen = protobufField(targetFields, TARGET_SUBSCREEN)?.varint?.toInt()

    return when (targetType) {
        TARGET_SUBSCREEN_PLAIN, TARGET_SUBSCREEN_WITH_OPTIONS -> {
            // Screen zero is how an empty slot is written — a row that leads
            // nowhere.
            if (subscreen == null || subscreen == 0) {
                GarminSettingsEntry(
                    id = id,
                    kind = GarminEntryKind.INERT,
                    title = title,
                    summary = state?.summary,
                )
            } else {
                GarminSettingsEntry(
                    id = id,
                    kind = GarminEntryKind.SUBSCREEN,
                    title = title,
                    summary = state?.summary,
                    subscreenId = subscreen,
                )
            }
        }

        TARGET_OPTIONS -> GarminSettingsEntry(
            id = id,
            kind = GarminEntryKind.OPTIONS,
            title = title,
            summary = state?.summary,
            options = options(protobufField(targetFields, TARGET_OPTION_LIST)?.bytes),
            selectedIndex = state?.selectedIndex,
        )

        TARGET_TIME -> GarminSettingsEntry(
            id = id,
            kind = GarminEntryKind.TIME,
            title = title,
            summary = state?.summary,
            time = state?.time,
        )

        TARGET_NUMBER_PICKER -> GarminSettingsEntry(
            id = id,
            kind = GarminEntryKind.NUMBER,
            title = title,
            summary = state?.summary,
            unit = state?.unit,
        )

        else ->
            // Type 6 opens something ON the watch and 7 is hidden. Anything
            // else is a control this app has never seen, and rendering a
            // guess at it would put the wrong widget in front of a real
            // setting. The declared type is kept so it can be identified
            // rather than merely dismissed.
            GarminSettingsEntry(
                id = id,
                kind = GarminEntryKind.INERT,
                title = title,
                summary = state?.summary,
                rawTargetType = targetType,
            )
    }
}

private fun options(bytes: ByteArray?): List<GarminSettingsOption> {
    if (bytes == null) return emptyList()
    val out = mutableListOf<GarminSettingsOption>()
    var index = 0
    for (field in readProtobuf(bytes)) {
        if (field.field != OPTION_ENTRY) continue
        val entry = field.bytes ?: continue
        val title = labelText(protobufField(readProtobuf(entry), ENTRY_TITLE)?.bytes)
        out.add(GarminSettingsOption(index = index, title = title ?: "?"))
        index++
    }
    return out
}

/** The current value behind each entry, keyed by entry id. */
private fun statesById(reply: ByteArray?): Map<Int, EntryState> {
    val service = GarminSettingsService.unwrap(reply) ?: return emptyMap()
    val response = protobufField(
        readProtobuf(service),
        GarminSettingsService.STATE_RESPONSE_FIELD,
    )?.bytes ?: return emptyMap()
    val state = protobufField(readProtobuf(response), STATE_INNER)?.bytes ?: return emptyMap()

    val out = mutableMapOf<Int, EntryState>()
    for (field in readProtobuf(state)) {
        if (field.field != ENTRY_STATE) continue
        val entryBytes = field.bytes ?: continue
        val fields = readProtobuf(entryBytes)
        val id = protobufField(fields, ENTRY_ID)?.varint?.toInt() ?: continue
        val switchBytes = protobufField(fields, STATE_SWITCH)?.bytes
        val summaryBytes = protobufField(fields, STATE_SUMMARY)?.bytes
        val summaryFields = summaryBytes?.let { readProtobuf(it) }
        val list = summaryFields?.let { protobufField(it, SUMMARY_VALUE_LIST)?.bytes }
        val timeValue = summaryFields?.let { protobufField(it, SUMMARY_VALUE_TIME)?.bytes }
        val number = summaryFields?.let { protobufField(it, SUMMARY_VALUE_NUMBER)?.bytes }

        out[id] = EntryState(
            // Present, even empty, is the whole signal. It is not in
            // Gadgetbridge's schema at all — that proto is older than this
            // firmware — so it is read for its PRESENCE and nothing is
            // assumed about what it contains.
            removable = protobufField(fields, STATE_REMOVABLE) != null,
            selectedIndex = list?.let {
                protobufField(readProtobuf(it), VALUE_INDEX)?.varint?.toInt()
            },
            time = timeValue?.let {
                protobufField(readProtobuf(it), VALUE_SECONDS)?.varint?.seconds
            },
            unit = number?.let {
                labelText(protobufField(readProtobuf(it), NUMBER_UNIT)?.bytes)
            },
            switchedOn = switchBytes?.let {
                (protobufField(readProtobuf(it), 1)?.varint ?: 0L) != 0L
            },
            summary = labelText(summaryBytes)
                ?: labelText(
                    protobufField(readProtobuf(summaryBytes ?: ByteArray(0)), 1)?.bytes,
                ),
        )
    }
    return out
}

private class EntryState(
    val switchedOn: Boolean? = null,
    val summary: String? = null,
    /** The watch marked this row as one it will accept a removal for. */
    val removable: Boolean = false,
    val selectedIndex: Int? = null,
    val time: Duration? = null,
    val unit: String? = null,
)

private fun definitionOf(reply: ByteArray?): ByteArray? {
    val service = GarminSettingsService.unwrap(reply) ?: return null
    val response = protobufField(
        readProtobuf(service),
        GarminSettingsService.DEFINITION_RESPONSE_FIELD,
    )?.bytes ?: return null
    return protobufField(readProtobuf(response), DEFINITION_INNER)?.bytes
}

/** A `Label`'s display text — field 2, with field 1 being an opaque id. */
private fun labelText(bytes: ByteArray?): String? {
    if (bytes == null) return null
    val text = protobufField(readProtobuf(bytes), LABEL_TEXT_FIELD)?.bytes ?: return null
    return String(text, Charsets.ISO_8859_1)
}

private const val DEFINITION_INNER = 2
private const val STATE_INNER = 2
private const val SCREEN_ID = 1
private const val SCREEN_TITLE = 4
private const val ENTRY = 5
private const val ENTRY_ID = 1
private const val ENTRY_TITLE = 3
private const val ENTRY_TARGET = 9
private const val LABEL_TEXT_FIELD = 2

private const val TARGET_TYPE = 1
private const val TARGET_SUBSCREEN = 2
private const val TARGET_OPTION_LIST = 4
private const val TARGET_SUBSCREEN_PLAIN = 0
private const val TARGET_OPTIONS = 1
private const val TARGET_TIME = 3
private const val TARGET_SUBSCREEN_WITH_OPTIONS = 9
private const val TARGET_NUMBER_PICKER = 8
private const val OPTION_ENTRY = 1

private const val ENTRY_STATE = 4
private const val STATE_SWITCH = 3
private const val STATE_SUMMARY = 4

/**
 * Set — present but empty — on the one row of an alarm's screen that deletes
 * it, and on nothing at the root of the tree. Not in Gadgetbridge's schema.
 */
private const val STATE_REMOVABLE = 9

// Inside a Summary: the VALUE behind a row, in a shape that depends on the
// kind of control. Read off a vívoactive 5 — an alarm's Repeat came back as
// `valueList{index:0}` beside the label "Once", and its Time as
// `valueTime{seconds:40200}` beside "11:10 am".
private const val SUMMARY_VALUE_LIST = 2
private const val SUMMARY_VALUE_TIME = 4
private const val SUMMARY_VALUE_NUMBER = 6
private const val VALUE_INDEX = 1
private const val VALUE_SECONDS = 1
private const val NUMBER_UNIT = 4
