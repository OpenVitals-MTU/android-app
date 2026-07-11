/// Verified local staging for the Apple Health importer, ported from the Kotlin
/// `AppleHealthImportStagingStore.kt` (1.9.0 `dcca0dc` + `7b4b417`).
///
/// The picked export is copied into app-private storage *before* anything reads
/// it, so the rest of the importer works against a stable local [File] instead
/// of the platform's document stream — and never against a multi-gigabyte
/// `List<int>` held in RAM. The copy is written to a `.tmp` sibling, flushed,
/// renamed into place, and its byte count is verified against the size the
/// picker reported; a short copy throws [AppleHealthExportCopyException] instead
/// of handing a truncated ZIP to the parser. An existing staged copy is reused
/// when the source fingerprint still matches.
library;

import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:path_provider/path_provider.dart';

/// Directory (under the app-support dir) holding every staged import artifact.
const String appleHealthImportDirectoryName = 'apple_health_import';

const String _stagedExportFileName = 'staged_export.bin';
const String _stagedExportTempFileName = '$_stagedExportFileName.tmp';
const String _metadataFileName = 'staged_export.properties';

const String _keySourceId = 'sourceUri';
const String _keyDisplayName = 'displayName';
const String _keySize = 'size';
const String _keyBytesCopied = 'bytesCopied';

/// Resolves the app-private directory the importer stages into. Injected in
/// tests so nothing ever touches the real app-support directory.
typedef AppleHealthImportDirectoryResolver = Future<Directory> Function();

/// `<appSupport>/apple_health_import` (Kotlin: `File(context.filesDir, ...)`).
Future<Directory> defaultAppleHealthImportDirectory() async {
  final support = await getApplicationSupportDirectory();
  return Directory('${support.path}/$appleHealthImportDirectoryName');
}

/// What the platform picker told us about the picked export (Kotlin
/// `AppleHealthExportFingerprint`: the SAF `DISPLAY_NAME` + `SIZE` columns).
class AppleHealthExportFingerprint {
  const AppleHealthExportFingerprint({this.displayName, this.size});

  final String? displayName;
  final int? size;

  bool get isIdentifiable => displayName != null || size != null;

  /// The provider-reported size, when it is usable as a copy expectation.
  int? get expectedBytes {
    final value = size;
    return value != null && value > 0 ? value : null;
  }

  @override
  bool operator ==(Object other) =>
      other is AppleHealthExportFingerprint &&
      other.displayName == displayName &&
      other.size == size;

  @override
  int get hashCode => Object.hash(displayName, size);

  @override
  String toString() =>
      'AppleHealthExportFingerprint(displayName: $displayName, size: $size)';
}

/// A picked Apple Health export: its identity, its fingerprint, and how to read
/// it. The Kotlin side passes a `Uri` and re-opens it through the
/// `ContentResolver`; Dart passes the picker's `openRead` callback so the file
/// is streamed rather than materialised.
class AppleHealthExportSource {
  const AppleHealthExportSource({
    required this.sourceId,
    required this.fingerprint,
    required this.openRead,
  });

  /// Stages a plain [File] (used by tests and by pickers that hand back a path).
  factory AppleHealthExportSource.file(File file, {String? displayName}) {
    final name = displayName ??
        file.path.split(Platform.pathSeparator).last.split('/').last;
    return AppleHealthExportSource(
      sourceId: file.path,
      fingerprint: AppleHealthExportFingerprint(
        displayName: name,
        size: file.existsSync() ? file.lengthSync() : null,
      ),
      openRead: file.openRead,
    );
  }

  /// Stable identity of the picked document (the Kotlin `Uri.toString()`).
  final String sourceId;
  final AppleHealthExportFingerprint fingerprint;
  final Stream<List<int>> Function() openRead;

  /// The Kotlin `AppleHealthImportCheckpointStore.sourceKey(uri, fingerprint)`.
  String get sourceKey => appleHealthImportSourceKey(sourceId, fingerprint);
}

/// `uri|displayName|size` — the identity a checkpoint is keyed by.
String appleHealthImportSourceKey(
  String sourceId,
  AppleHealthExportFingerprint fingerprint,
) =>
    [
      sourceId,
      fingerprint.displayName ?? '',
      fingerprint.size?.toString() ?? '',
    ].join('|');

/// The result of [AppleHealthImportStagingStore.stage].
class AppleHealthStagedExport {
  const AppleHealthStagedExport({
    required this.file,
    required this.bytes,
    required this.reused,
  });

  final File file;
  final int bytes;
  final bool reused;
}

/// Thrown when the platform handed us fewer bytes than it advertised — the
/// classic "the ZIP is still syncing from iCloud/Drive" failure, which would
/// otherwise surface as an unreadable archive much later (Kotlin
/// `AppleHealthExportCopyException`).
class AppleHealthExportCopyException implements Exception {
  const AppleHealthExportCopyException({
    required this.expectedBytes,
    required this.copiedBytes,
  });

  final int expectedBytes;
  final int copiedBytes;

  String get message =>
      'Apple Health export copy was incomplete: the file picker reported '
      '$expectedBytes byte(s), but only $copiedBytes byte(s) were copied into '
      'app storage. Download the ZIP fully to local storage and select it again.';

  @override
  String toString() => message;
}

class AppleHealthImportStagingStore {
  AppleHealthImportStagingStore({AppleHealthImportDirectoryResolver? directory})
      : _directory = directory ?? defaultAppleHealthImportDirectory;

  final AppleHealthImportDirectoryResolver _directory;

  Future<Directory> importDirectory() => _directory();

  Future<File> stagedExportFile() async =>
      File('${(await _directory()).path}/$_stagedExportFileName');

  /// Copies [source] into app-private storage, verifying the copied byte count,
  /// and reuses an existing staged copy whose fingerprint still matches.
  Future<AppleHealthStagedExport> stage(AppleHealthExportSource source) async {
    final directory = await _directory();
    final file = File('${directory.path}/$_stagedExportFileName');
    final metadata = File('${directory.path}/$_metadataFileName');
    final temp = File('${directory.path}/$_stagedExportTempFileName');

    if (file.existsSync()) {
      final fileBytes = file.lengthSync();
      if (await _matches(metadata, source, fileBytes)) {
        return AppleHealthStagedExport(
          file: file,
          bytes: fileBytes,
          reused: true,
        );
      }
    }

    await directory.create(recursive: true);
    await _deleteQuietly(temp);

    var copied = 0;
    try {
      final sink = await temp.open(mode: FileMode.write);
      try {
        await for (final chunk in source.openRead()) {
          await sink.writeFrom(chunk);
          copied += chunk.length;
        }
        // fsync: the staged copy must survive a process death mid-import.
        await sink.flush();
      } finally {
        await sink.close();
      }
    } catch (error) {
      await _deleteQuietly(temp);
      rethrow;
    }

    final expectedBytes = source.fingerprint.expectedBytes;
    if (expectedBytes != null && expectedBytes != copied) {
      await _deleteQuietly(temp);
      throw AppleHealthExportCopyException(
        expectedBytes: expectedBytes,
        copiedBytes: copied,
      );
    }

    try {
      await temp.rename(file.path);
    } catch (_) {
      // Cross-device rename (or a Windows in-use target): fall back to a copy.
      await temp.copy(file.path);
      await _deleteQuietly(temp);
    }
    await _writeMetadata(metadata, source, copied);
    return AppleHealthStagedExport(file: file, bytes: copied, reused: false);
  }

  /// Deletes the staged export, its metadata, any leftover interrupted-copy
  /// `.tmp` file, and the (now empty) import directory. Returns `true` when
  /// nothing is left behind.
  Future<bool> clear() async {
    final Directory directory;
    try {
      directory = await _directory();
    } catch (_) {
      return false;
    }
    final files = [
      File('${directory.path}/$_stagedExportFileName'),
      File('${directory.path}/$_stagedExportTempFileName'),
      File('${directory.path}/$_metadataFileName'),
    ];
    for (final file in files) {
      await _deleteQuietly(file);
    }
    try {
      // Non-recursive: only removes the directory once it is genuinely empty.
      if (directory.existsSync()) directory.deleteSync();
    } catch (_) {
      // A concurrent checkpoint write can keep the directory alive; ignore.
    }
    return files.every((file) => !file.existsSync());
  }

  Future<bool> _matches(
    File metadata,
    AppleHealthExportSource source,
    int fileBytes,
  ) async {
    if (fileBytes <= 0 || !metadata.existsSync()) return false;
    final expectedBytes = source.fingerprint.expectedBytes;
    if (expectedBytes != null && expectedBytes != fileBytes) return false;
    final properties = await _readMetadata(metadata);
    if (properties == null) return false;
    return properties[_keySourceId] == source.sourceId &&
        (properties[_keyDisplayName] ?? '') ==
            (source.fingerprint.displayName ?? '') &&
        (properties[_keySize] ?? '') ==
            (source.fingerprint.size?.toString() ?? '') &&
        int.tryParse(properties[_keyBytesCopied] ?? '') == fileBytes;
  }

  Future<Map<String, String>?> _readMetadata(File metadata) async {
    try {
      final decoded = jsonDecode(await metadata.readAsString());
      if (decoded is! Map) return null;
      return decoded.map((key, value) => MapEntry('$key', '$value'));
    } catch (_) {
      return null;
    }
  }

  Future<void> _writeMetadata(
    File metadata,
    AppleHealthExportSource source,
    int bytesCopied,
  ) async {
    await metadata.writeAsString(
      jsonEncode(<String, String>{
        _keySourceId: source.sourceId,
        _keyDisplayName: source.fingerprint.displayName ?? '',
        _keySize: source.fingerprint.size?.toString() ?? '',
        _keyBytesCopied: bytesCopied.toString(),
      }),
      flush: true,
    );
  }
}

Future<void> _deleteQuietly(File file) async {
  try {
    if (file.existsSync()) await file.delete();
  } catch (_) {
    // Best effort; a stale file only costs disk, never correctness.
  }
}
