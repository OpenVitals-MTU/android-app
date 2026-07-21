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

  /// A stable key for cross-sync dedup: type + file number identify the same
  /// recording across re-syncs, independent of the volatile file index.
  String get dedupKey => '${type.dataType}/${type.subType}/$fileNumber';
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
class GarminDirectory {
  const GarminDirectory._();

  static const int _entrySize = 16;

  static List<GarminDirectoryEntry> parse(Uint8List data) {
    final entries = <GarminDirectoryEntry>[];
    // A trailing partial record is truncated data, not an entry — stop before it
    // rather than read past the buffer.
    final reader = GarminByteReader(data);
    while (reader.remaining >= _entrySize) {
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
        continue;
      }

      final type = GarminFileType.fromCodes(dataType, subType);
      if (type == null || !type.wanted) continue;

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
    return entries;
  }
}
