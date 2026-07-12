import 'dart:io';
import 'dart:math';

import 'package:collection/collection.dart';
import 'package:flutter/foundation.dart';
import 'package:mapsforge_flutter_mapfile/mapfile.dart';
import 'package:path/path.dart' as p;

import '../../../core/result/app_failure.dart';
import '../../../core/result/result.dart';
import 'offline_map_metadata_store.dart';
import 'offline_map_models.dart';

/// Imports offline map packs into the app's private maps directory and tracks
/// them via [OfflineMapMetadataStore]. Ported from the Kotlin
/// `OfflineMapRepository` (+ `OfflineMapImportWorkController`).
///
/// Judgment calls:
/// * The Kotlin import runs inside a WorkManager `CoroutineWorker` with a
///   foreground notification. The Dart port is a plain async copy with a
///   progress callback — a real background worker / foreground service is
///   deferred to on-device (Phase 8). The copy is still chunked so a UI can
///   render a progress bar.
/// * File selection (SAF / document picker) is device UI; the controller takes
///   an already-resolved source [File] plus its original name.
/// * Mapsforge validation mirrors Kotlin's `MapFile(...)` open-check via the
///   pure-Dart `mapsforge_flutter_mapfile` reader.
class OfflineMapImportController {
  OfflineMapImportController({
    required this.metadataStore,
    required this.mapsDirectoryPath,
    Random? random,
    this.now = DateTime.now,
  })  : _random = random ?? Random(),
        state = ValueNotifier<OfflineMapLibraryState>(metadataStore.read());

  final OfflineMapMetadataStore metadataStore;
  final String mapsDirectoryPath;
  final DateTime Function() now;
  final Random _random;

  /// The current library, updated after every mutation. UI watches this.
  final ValueNotifier<OfflineMapLibraryState> state;

  /// Re-reads persisted metadata (dropping packs whose files disappeared).
  void refresh() {
    state.value = metadataStore.read();
  }

  /// Copies [source] into the maps directory and records it. [originalFileName]
  /// defaults to the source's basename and drives format detection.
  ///
  /// A rejected pack (unsupported format, empty file, unreadable map) is an
  /// [Err], not a throw: the caller is a view-model with a failure to render,
  /// not a crash to report. The rejection reasons are written for a person, so
  /// they are carried as the failure's message — an `ArgumentError`'s
  /// `toString()` would prefix them with "Invalid argument(s)".
  Future<Result<OfflineMapPack>> importMap(
    File source, {
    String? originalFileName,
    void Function(OfflineMapImportProgress progress)? onProgress,
  }) async {
    try {
      return Ok(await _importMap(
        source,
        originalFileName: originalFileName,
        onProgress: onProgress,
      ));
    } catch (error, stackTrace) {
      return Err(
        UnexpectedFailure(
          error is ArgumentError ? '${error.message}' : error.toString(),
          cause: error,
          stackTrace: stackTrace,
        ),
      );
    }
  }

  Future<OfflineMapPack> _importMap(
    File source, {
    String? originalFileName,
    void Function(OfflineMapImportProgress progress)? onProgress,
  }) async {
    // Format check first: an unsupported file must fail before any disk work
    // (Kotlin's `require` runs before the copy too). The cheap setup IO is
    // synchronous, mirroring Kotlin's Dispatchers.IO block — only the chunked
    // copy itself stays async for progress reporting.
    final fileName = (originalFileName != null && originalFileName.isNotEmpty)
        ? originalFileName
        : p.basename(source.path);
    final format = OfflineMapPackFormat.fromFileName(fileName);
    if (format == null) {
      throw ArgumentError(
        'Only .pmtiles, .map, and .maps offline map packs are supported.',
      );
    }
    Directory(mapsDirectoryPath).createSync(recursive: true);
    final originalExtension =
        format.extensionForFileName(fileName) ?? format.fileExtension;

    final id = _mapIdFor(fileName, originalExtension);
    final totalBytes = source.lengthSync();
    final tempFile = File(p.join(mapsDirectoryPath, '$id${format.fileExtension}.tmp'));
    final finalFile = File(p.join(mapsDirectoryPath, '$id${format.fileExtension}'));
    var finalFileRecorded = false;

    onProgress?.call(const OfflineMapImportProgress());
    try {
      if (tempFile.existsSync()) tempFile.deleteSync();
      if (finalFile.existsSync()) finalFile.deleteSync();

      final copiedBytes = await _copyFile(source, tempFile, totalBytes, onProgress);
      if (copiedBytes <= 0) {
        throw ArgumentError('The selected offline map pack is empty.');
      }
      await _validateImportedMap(tempFile, format);
      tempFile.renameSync(finalFile.path);

      final finalLength = finalFile.lengthSync();
      final pack = OfflineMapPack(
        id: id,
        displayName: _removeSuffixIgnoreCase(fileName, originalExtension),
        originalFileName: fileName,
        sizeBytes: finalLength > 0 ? finalLength : copiedBytes,
        importedAtMillis: now().millisecondsSinceEpoch,
        path: finalFile.path,
        format: format,
      );

      final current = metadataStore.read();
      final mergedPacks = <OfflineMapPack>[
        ...current.mapPacks.where((existing) => existing.id != pack.id),
        pack,
      ]..sort((a, b) => b.importedAtMillis.compareTo(a.importedAtMillis));
      final keepActive = current.activeFormat != null &&
          (current.mapPacks.any((p) => p.format == current.activeFormat) ||
              pack.format == current.activeFormat);
      final updated = OfflineMapLibraryState(
        mapPacks: mergedPacks,
        activeFormat: keepActive ? current.activeFormat : pack.format,
      );
      metadataStore.write(updated);
      state.value = updated;
      finalFileRecorded = true;

      onProgress?.call(
        OfflineMapImportProgress(
          phase: OfflineMapImportPhase.complete,
          bytesCopied: pack.sizeBytes,
          totalBytes: pack.sizeBytes,
        ),
      );
      return pack;
    } catch (_) {
      if (tempFile.existsSync()) tempFile.deleteSync();
      if (!finalFileRecorded && finalFile.existsSync()) finalFile.deleteSync();
      rethrow;
    }
  }

  /// Deletes the pack [id], its file, and re-picks an active format if needed.
  Future<void> deleteMap(String id) async {
    final current = metadataStore.read();
    final deleted = current.mapPacks.where((pack) => pack.id == id).firstOrNull;
    final remaining =
        current.mapPacks.where((pack) => pack.id != id).toList();
    if (deleted != null) {
      final file = File(deleted.path);
      if (file.existsSync()) file.deleteSync();
    }
    final activeFormat = (current.activeFormat != null &&
            remaining.any((pack) => pack.format == current.activeFormat))
        ? current.activeFormat
        : remaining.firstOrNull?.format;
    final updated = OfflineMapLibraryState(
      mapPacks: remaining,
      activeFormat: activeFormat,
    );
    metadataStore.write(updated);
    state.value = updated;
  }

  /// Sets [format] as active (ignored when no pack of that format exists).
  void setActiveFormat(OfflineMapPackFormat? format) {
    final current = metadataStore.read();
    final activeFormat = (format != null &&
            current.mapPacks.any((pack) => pack.format == format))
        ? format
        : null;
    final updated = current.copyWith(
      activeFormat: activeFormat,
      clearActiveFormat: activeFormat == null,
    );
    metadataStore.write(updated);
    state.value = updated;
  }

  Future<int> _copyFile(
    File source,
    File destination,
    int totalBytes,
    void Function(OfflineMapImportProgress progress)? onProgress,
  ) async {
    final sink = destination.openWrite();
    var bytesCopied = 0;
    try {
      await for (final chunk in source.openRead()) {
        sink.add(chunk);
        bytesCopied += chunk.length;
        onProgress?.call(
          OfflineMapImportProgress(
            phase: OfflineMapImportPhase.copying,
            bytesCopied: bytesCopied,
            totalBytes: totalBytes,
          ),
        );
      }
    } finally {
      await sink.close();
    }
    return bytesCopied;
  }

  /// Kotlin `validateImportedMap`: a Mapsforge pack must open as a `MapFile`
  /// (header + index parse) or the import fails as invalid; PMTiles packs are
  /// not structurally validated, exactly like Kotlin.
  Future<void> _validateImportedMap(
      File file, OfflineMapPackFormat format) async {
    if (format == OfflineMapPackFormat.pmtiles) return;
    Mapfile? mapfile;
    try {
      mapfile = await Mapfile.createFromFile(filename: file.path);
      // Opening is lazy; the bounding-box read forces the header parse and
      // throws on anything that is not a Mapsforge map.
      await mapfile.getBoundingBox();
    } catch (error) {
      throw ArgumentError(
        'The selected file is not a valid or supported Mapsforge map: $error',
      );
    } finally {
      mapfile?.dispose();
    }
  }

  String _mapIdFor(String originalFileName, String fileExtension) {
    final base = _removeSuffixIgnoreCase(originalFileName, fileExtension)
        .toLowerCase()
        .replaceAll(RegExp('[^a-z0-9]+'), '-');
    final trimmed = _trimDashes(base);
    final safe = trimmed.isEmpty ? 'offline-map' : trimmed;
    return '$safe-${_randomHex(8)}';
  }

  String _randomHex(int length) {
    const chars = '0123456789abcdef';
    return List.generate(length, (_) => chars[_random.nextInt(chars.length)])
        .join();
  }

  static String _removeSuffixIgnoreCase(String value, String suffix) =>
      value.toLowerCase().endsWith(suffix.toLowerCase())
          ? value.substring(0, value.length - suffix.length)
          : value;

  static String _trimDashes(String value) {
    var start = 0;
    var end = value.length;
    while (start < end && value[start] == '-') {
      start++;
    }
    while (end > start && value[end - 1] == '-') {
      end--;
    }
    return value.substring(start, end);
  }
}
