package tech.mmarca.openvitals.devices.garmin

import java.time.Instant

/**
 * One file the watch is offering, as listed in the downloaded directory.
 *
 * Port of `FileTransferHandler.DirectoryEntry` + the old-sync-protocol parse
 * in `parseDirectoryEntries`. A directory is a flat array of 16-byte records
 * — no FIT decoding, no protobuf (that is the "new sync protocol", out of
 * scope).
 */
data class GarminDirectoryEntry(
    /** The handle to pass to a download request. */
    val fileIndex: Int,
    val type: GarminFileType,
    val fileNumber: Int,
    val specificFlags: Int,
    val fileFlags: Int,
    val fileSize: Long,
    /**
     * When the watch recorded the file, or null for its "no date" sentinel
     * (wire timestamp 0).
     */
    val fileDate: Instant?,
) {
    companion object {
        /**
         * The watch's "no file number" sentinel. Observed on a real
         * vívoactive 5 for sleep and HRV files, where several DIFFERENT files
         * all carry it.
         */
        const val UNSET_FILE_NUMBER = 0xFFFF
    }

    /**
     * A stable key for cross-sync dedup: type + file number identify the same
     * recording across re-syncs, independent of the volatile file index.
     *
     * **Null when the file number is [UNSET_FILE_NUMBER]**, because then it
     * identifies nothing: a real watch returned two distinct sleep files both
     * numbered 65535, which collapsed to one key and would have made every
     * future sleep file look already-synced — silent, permanent data loss.
     *
     * Declining to dedup those is safe in a way that guessing is not. The
     * archive flag set on the watch is the PRIMARY mechanism and still
     * applies, and Health Connect's `clientRecordId` makes any re-import
     * idempotent, so the worst case is re-downloading a file. Keying on the
     * volatile [fileIndex] instead was rejected for the opposite reason: an
     * index the watch later reuses would skip a genuinely new file.
     */
    val dedupKey: String?
        get() = if (fileNumber == UNSET_FILE_NUMBER) {
            null
        } else {
            "${type.dataType}/${type.subType}/$fileNumber"
        }
}

/**
 * What a directory parse found, including what it threw away.
 *
 * The rejects are carried, not just counted: "zero entries" has several very
 * different causes — an empty listing, a listing of types this app does not
 * map, a listing of types it maps but does not want — and only the raw
 * `(dataType, subType)` pairs tell them apart on a device.
 */
data class GarminDirectoryListing(
    val entries: List<GarminDirectoryEntry>,
    /** Every 16-byte record read, before any filtering. */
    val totalRecords: Int,
    /**
     * `index:dataType/subType` of each record that was dropped, and why.
     *
     * The INDEX matters as much as the type: the watch also announces files
     * over the protobuf FileSyncService by index, and without it there is no
     * way to tell whether an announced file is one the legacy directory
     * already lists and we skip, or one it never mentions at all.
     */
    val skipped: List<String>,
    /**
     * The indexes of every record read, kept or dropped, so a listing can be
     * matched against what other channels claim exists.
     */
    val allIndexes: List<Int> = emptyList(),
) {
    fun describe(): String = "records=$totalRecords kept=${entries.size} " +
        "skipped=[${skipped.joinToString(", ")}] " +
        "indexes=[${allIndexes.joinToString(",")}]"
}

/**
 * Parses a downloaded directory file into the entries worth pulling.
 *
 * Each record is 16 bytes, little-endian:
 * `u16 index, u8 dataType, u8 subType, u16 number, u8 specificFlags,
 *  u8 fileFlags, u32 size, u32 garminTimestamp`.
 *
 * Entries are dropped when: the type is unknown to this app, the type is not
 * [GarminFileType.wanted], or the record is the all-zero sentinel (which the
 * watch emits and which would otherwise loop the downloader forever).
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
        // A trailing partial record is truncated data, not an entry — stop
        // before it rather than read past the buffer.
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

            // The device's end-of-list padding: every field zero. Skipping it
            // is what stops the caller re-requesting index 0 (the directory
            // itself) forever.
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
