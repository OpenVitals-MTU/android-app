package tech.mmarca.openvitals.devices.garmin

import java.time.Instant

/**
 * One file the watch is offering. A directory is a flat array of 16-byte
 * records, no FIT decoding, no protobuf.
 */
data class GarminDirectoryEntry(
    /** The handle to pass to a download request. */
    val fileIndex: Int,
    val type: GarminFileType,
    val fileNumber: Int,
    val specificFlags: Int,
    val fileFlags: Int,
    val fileSize: Long,
    /** When the watch recorded the file, or null for its "no date" sentinel. */
    val fileDate: Instant?,
) {
    companion object {
        /** The watch's "no file number" sentinel. Several different files carry it. */
        const val UNSET_FILE_NUMBER = 0xFFFF
    }

    /**
     * A stable key for cross-sync dedup: type, number, date and size. The
     * number alone cycles; a `type/number` key once made a new day's file
     * look synced and sent the archive flag for it. Null when the number or
     * the date is a sentinel: two distinct sleep files shared 65535. The
     * archive flag and `clientRecordId` make a re-download safe.
     */
    val dedupKey: String?
        get() {
            val date = fileDate
            if (fileNumber == UNSET_FILE_NUMBER || date == null) return null
            return "${type.dataType}/${type.subType}/$fileNumber/${date.epochSecond}/$fileSize"
        }
}

/** What a directory parse found, rejects included: they tell the causes of "zero entries" apart. */
data class GarminDirectoryListing(
    val entries: List<GarminDirectoryEntry>,
    /** Every 16-byte record read, before any filtering. */
    val totalRecords: Int,
    /**
     * `index:dataType/subType` of each dropped record. The index matters:
     * the watch also announces files by index over protobuf.
     */
    val skipped: List<String>,
    /** The indexes of every record read, to match against other channels. */
    val allIndexes: List<Int> = emptyList(),
) {
    fun describe(): String = "records=$totalRecords kept=${entries.size} " +
        "skipped=[${skipped.joinToString(", ")}] " +
        "indexes=[${allIndexes.joinToString(",")}]"
}

/**
 * Parses a downloaded directory into the entries worth pulling. Each record
 * is 16 bytes little-endian: `u16 index, u8 dataType, u8 subType, u16
 * number, u8 specificFlags, u8 fileFlags, u32 size, u32 garminTimestamp`.
 * Unknown, unwanted and all-zero sentinel records are dropped.
 */
object GarminDirectory {

    private const val ENTRY_SIZE = 16

    /** Convenience for callers that only want the usable entries. */
    fun parse(data: ByteArray): List<GarminDirectoryEntry> =
        parseWithDiagnostics(data).entries

    fun parseWithDiagnostics(data: ByteArray): GarminDirectoryListing {
        val entries = mutableListOf<GarminDirectoryEntry>()
        val skipped = mutableListOf<String>()
        val allIndexes = mutableListOf<Int>()
        var totalRecords = 0
        // A trailing partial record is truncated data; stop before it.
        val reader = GarminByteReader(data)
        while (reader.remaining >= ENTRY_SIZE) {
            totalRecords++
            val fileIndex = reader.readShort()
            val dataType = reader.readByte()
            val subType = reader.readByte()
            val fileNumber = reader.readShort()
            val specificFlags = reader.readByte()
            val fileFlags = reader.readByte()
            val fileSize = reader.readInt()
            val wireTimestamp = reader.readInt()
            allIndexes.add(fileIndex)

            // End-of-list padding. Skipping it stops the caller re-requesting index 0.
            if (fileIndex == 0 &&
                dataType == 0 &&
                subType == 0 &&
                fileNumber == 0 &&
                fileSize == 0L
            ) {
                skipped.add("$fileIndex:pad")
                continue
            }

            val type = GarminFileType.fromCodes(dataType, subType)
            if (type == null) {
                skipped.add("$fileIndex:$dataType/$subType?")
                continue
            }
            if (!type.wanted) {
                skipped.add("$fileIndex:${type.label}!")
                continue
            }

            entries.add(
                GarminDirectoryEntry(
                    fileIndex = fileIndex,
                    type = type,
                    fileNumber = fileNumber,
                    specificFlags = specificFlags,
                    fileFlags = fileFlags,
                    fileSize = fileSize,
                    fileDate = if (wireTimestamp == 0L) {
                        null
                    } else {
                        GarminTime.toInstant(wireTimestamp)
                    },
                ),
            )
        }
        return GarminDirectoryListing(
            entries = entries,
            totalRecords = totalRecords,
            skipped = skipped,
            allIndexes = allIndexes,
        )
    }
}
