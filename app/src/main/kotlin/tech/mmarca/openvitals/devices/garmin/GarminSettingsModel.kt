package tech.mmarca.openvitals.devices.garmin

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * What a settings entry is, once its target is read. The watch decides: a
 * wrong kind draws a plausible screen with the wrong widget.
 */
enum class GarminEntryKind {
    /** Opens another screen. Alarms is one of these on the Clocks screen. */
    SUBSCREEN,

    /** One of a set of options the watch supplies. */
    OPTIONS,

    /** A time of day, changed as seconds since midnight. */
    TIME,

    /** An on/off switch, which carries no target at all. */
    TOGGLE,

    /** A number the watch bounds itself. */
    NUMBER,

    /** A button, not a setting: the watch marks the row as removable. */
    ACTION,

    /** On the screen but not actionable from a phone. */
    INERT,
}

/** One option the watch offered for an [GarminEntryKind.OPTIONS] entry. */
class GarminSettingsOption(
    /** Position in the list the watch sent. A change names this. */
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
    /** The current value as the watch renders it. Display text, never parsed. */
    val summary: String? = null,
    val subscreenId: Int? = null,
    val options: List<GarminSettingsOption> = emptyList(),
    /** Only for [GarminEntryKind.TOGGLE]; null when the state has not been read. */
    val switchedOn: Boolean? = null,
    /** The target type the watch declared, kept even when unhandled. */
    val rawTargetType: Int? = null,
    /** Which of [options] is chosen, as a position. The value, not derived from the summary. */
    val selectedIndex: Int? = null,
    /** The time behind a [GarminEntryKind.TIME] row, so the picker opens on it. */
    val time: Duration? = null,
    /** What a number is measured in, when the watch said. Its own word for it. */
    val unit: String? = null,
) {

    /** A row with nothing readable: an unused slot. Untitled inert rows count. */
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
    /** Whether the watch supplied the current values. False leaves every switch inert. */
    val hasState: Boolean = true,
) {
    val isEmpty: Boolean get() = entries.isEmpty()
}

/**
 * Turns a definition reply, and optionally the matching state reply, into a
 * screen. The definition alone cannot show what anything is set to.
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

    // No target: a switch, whose value lives in the state.
    if (target == null) {
        // A switch and a button are both target-less. Only the watch's removable
        // mark tells them apart. Inferring a button from "named, no target"
        // once sent a DELETE for Find My Device.
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
            // Screen zero is an empty slot.
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
            // Type 6 opens something on the watch and 7 is hidden. Anything else
            // is unknown; keep the declared type rather than guess a widget.
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
            // Presence is the whole signal. Not in Gadgetbridge's schema.
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

/** Present but empty on an alarm's delete row. Not in Gadgetbridge's schema. */
private const val STATE_REMOVABLE = 9

// Inside a Summary: the value behind a row, shaped by the kind of control.
private const val SUMMARY_VALUE_LIST = 2
private const val SUMMARY_VALUE_TIME = 4
private const val SUMMARY_VALUE_NUMBER = 6
private const val VALUE_INDEX = 1
private const val VALUE_SECONDS = 1
private const val NUMBER_UNIT = 4
