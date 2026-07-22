import 'dart:typed_data';

import 'garmin_byte_reader.dart';
import 'garmin_file_types.dart';
import 'garmin_time.dart';

/// One file the watch is offering, as listed in the downloaded directory.
///
/// Port of `FileTransferHandler.DirectoryEntry` + the old-sync-protocol parse in
/// `parseDirectoryEntries`. A directory is a flat array of 16-byte records — no
/// FIT decoding, no protobuf (that is the "new sync protocol", out of scope).
class GarminDirectoryEntry {
  const GarminDirectoryEntry({
    required this.fileIndex,
    required this.type,
    required this.fileNumber,
    required this.specificFlags,
    required this.fileFlags,
    required this.fileSize,
    required this.fileDate,
  });

  /// The handle to pass to a download request.
  final int fileIndex;
  final GarminFileType type;
  final int fileNumber;
  final int specificFlags;
  final int fileFlags;
  final int fileSize;

  /// When the watch recorded the file, or null for its "no date" sentinel
  /// (wire timestamp 0).
  final DateTime? fileDate;

  /// The watch's "no file number" sentinel. Observed on a real vívoactive 5 for
  /// sleep and HRV files, where several DIFFERENT files all carry it.
  static const int unsetFileNumber = 0xFFFF;

  /// A stable key for cross-sync dedup: type + file number identify the same
  /// recording across re-syncs, independent of the volatile file index.
  ///
  /// **Null when the file number is [unsetFileNumber]**, because then it
  /// identifies nothing: a real watch returned two distinct sleep files both
  /// numbered 65535, which collapsed to one key and would have made every
  /// future sleep file look already-synced — silent, permanent data loss.
  ///
  /// Declining to dedup those is safe in a way that guessing is not. The archive
  /// flag set on the watch is the PRIMARY mechanism and still applies, and
  /// Health Connect's `clientRecordId` makes any re-import idempotent, so the
  /// worst case is re-downloading a file. Keying on the volatile [fileIndex]
  /// instead was rejected for the opposite reason: an index the watch later
  /// reuses would skip a genuinely new file.
  String? get dedupKey => fileNumber == unsetFileNumber
      ? null
      : '${type.dataType}/${type.subType}/$fileNumber';
}

/// Parses a downloaded directory file into the entries worth pulling.
///
/// Each record is 16 bytes, little-endian:
/// `u16 index, u8 dataType, u8 subType, u16 number, u8 specificFlags,
///  u8 fileFlags, u32 size, u32 garminTimestamp`.
///
/// Entries are dropped when: the type is unknown to this app, the type is not
/// [GarminFileType.wanted], or the record is the all-zero sentinel (which the
/// watch emits and which would otherwise loop the downloader forever).
/// What a directory parse found, including what it threw away.
///
/// The rejects are carried, not just counted: "zero entries" has several very
/// different causes — an empty listing, a listing of types this app does not
/// map, a listing of types it maps but does not want — and only the raw
/// `(dataType, subType)` pairs tell them apart on a device.
class GarminDirectoryListing {
  const GarminDirectoryListing({
    required this.entries,
    required this.totalRecords,
    required this.skipped,
  });

  final List<GarminDirectoryEntry> entries;

  /// Every 16-byte record read, before any filtering.
  final int totalRecords;

  /// `(dataType, subType)` of each record that was dropped, and why.
  final List<String> skipped;

  String describe() => 'records=$totalRecords kept=${entries.length} '
      'skipped=[${skipped.join(", ")}]';
}

class GarminDirectory {
  const GarminDirectory._();

  static const int _entrySize = 16;

  /// Convenience for callers that only want the usable entries.
  static List<GarminDirectoryEntry> parse(Uint8List data) =>
      parseWithDiagnostics(data).entries;

  static GarminDirectoryListing parseWithDiagnostics(Uint8List data) {
    final entries = <GarminDirectoryEntry>[];
    final skipped = <String>[];
    var totalRecords = 0;
    // A trailing partial record is truncated data, not an entry — stop before it
    // rather than read past the buffer.
    final reader = GarminByteReader(data);
    while (reader.remaining >= _entrySize) {
      totalRecords++;
      final fileIndex = reader.readShort();
      final dataType = reader.readByte();
      final subType = reader.readByte();
      final fileNumber = reader.readShort();
      final specificFlags = reader.readByte();
      final fileFlags = reader.readByte();
      final fileSize = reader.readInt();
      final wireTimestamp = reader.readInt();

      // The device's end-of-list padding: every field zero. Skipping it is what
      // stops the caller re-requesting index 0 (the directory itself) forever.
      if (fileIndex == 0 &&
          dataType == 0 &&
          subType == 0 &&
          fileNumber == 0 &&
          fileSize == 0) {
        skipped.add('pad');
        continue;
      }

      final type = GarminFileType.fromCodes(dataType, subType);
      if (type == null) {
        skipped.add('$dataType/$subType?');
        continue;
      }
      if (!type.wanted) {
        skipped.add('${type.name}!');
        continue;
      }

      entries.add(GarminDirectoryEntry(
        fileIndex: fileIndex,
        type: type,
        fileNumber: fileNumber,
        specificFlags: specificFlags,
        fileFlags: fileFlags,
        fileSize: fileSize,
        fileDate: wireTimestamp == 0 ? null : GarminTime.toDateTime(wireTimestamp),
      ));
    }
    return GarminDirectoryListing(
      entries: entries,
      totalRecords: totalRecords,
      skipped: skipped,
    );
  }
}
