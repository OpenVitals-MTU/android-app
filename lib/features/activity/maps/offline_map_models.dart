import 'package:collection/collection.dart';

/// Offline map value types, ported from the Kotlin `OfflineMapModels.kt`.
///
/// Pure data + behaviour (no plugin imports) so the metadata store, import
/// controller and their tests can use them freely.

/// A supported offline map pack container format.
enum OfflineMapPackFormat {
  pmtiles('PMTILES', '.pmtiles', <String>['.pmtiles']),
  mapsforge('MAPSFORGE', '.map', <String>['.map', '.maps']);

  const OfflineMapPackFormat(
    this.storageName,
    this.fileExtension,
    this.acceptedFileExtensions,
  );

  /// Kotlin enum `.name`, used verbatim for metadata persistence.
  final String storageName;

  /// Canonical extension written to disk for this format.
  final String fileExtension;

  /// Extensions accepted when detecting an incoming file's format.
  final List<String> acceptedFileExtensions;

  /// Parses a persisted [storageName] (`PMTILES` / `MAPSFORGE`).
  static OfflineMapPackFormat? fromStorage(String value) =>
      values.firstWhereOrNull((format) => format.storageName == value);

  /// The format whose accepted extensions match [fileName], if any.
  static OfflineMapPackFormat? fromFileName(String fileName) =>
      values.firstWhereOrNull(
        (format) => format.extensionForFileName(fileName) != null,
      );

  /// The accepted extension [fileName] ends with (case-insensitive), or null.
  String? extensionForFileName(String fileName) {
    final lower = fileName.toLowerCase();
    return acceptedFileExtensions
        .firstWhereOrNull((extension) => lower.endsWith(extension));
  }
}

/// A single imported offline map pack.
class OfflineMapPack {
  const OfflineMapPack({
    required this.id,
    required this.displayName,
    required this.originalFileName,
    required this.sizeBytes,
    required this.importedAtMillis,
    required this.path,
    this.format = OfflineMapPackFormat.pmtiles,
  });

  final String id;
  final String displayName;
  final String originalFileName;
  final int sizeBytes;
  final int importedAtMillis;
  final String path;
  final OfflineMapPackFormat format;

  OfflineMapPack copyWith({
    String? id,
    String? displayName,
    String? originalFileName,
    int? sizeBytes,
    int? importedAtMillis,
    String? path,
    OfflineMapPackFormat? format,
  }) =>
      OfflineMapPack(
        id: id ?? this.id,
        displayName: displayName ?? this.displayName,
        originalFileName: originalFileName ?? this.originalFileName,
        sizeBytes: sizeBytes ?? this.sizeBytes,
        importedAtMillis: importedAtMillis ?? this.importedAtMillis,
        path: path ?? this.path,
        format: format ?? this.format,
      );

  @override
  bool operator ==(Object other) =>
      other is OfflineMapPack &&
      other.id == id &&
      other.displayName == displayName &&
      other.originalFileName == originalFileName &&
      other.sizeBytes == sizeBytes &&
      other.importedAtMillis == importedAtMillis &&
      other.path == path &&
      other.format == format;

  @override
  int get hashCode => Object.hash(
        id,
        displayName,
        originalFileName,
        sizeBytes,
        importedAtMillis,
        path,
        format,
      );
}

/// The persisted offline map library: the imported packs plus which format is
/// currently active for rendering.
class OfflineMapLibraryState {
  const OfflineMapLibraryState({
    this.mapPacks = const <OfflineMapPack>[],
    this.activeFormat,
  });

  final List<OfflineMapPack> mapPacks;
  final OfflineMapPackFormat? activeFormat;

  /// The packs matching [activeFormat] (empty when no format is active).
  List<OfflineMapPack> get activeMapPacks => activeFormat == null
      ? const <OfflineMapPack>[]
      : mapPacks.where((pack) => pack.format == activeFormat).toList();

  OfflineMapLibraryState copyWith({
    List<OfflineMapPack>? mapPacks,
    OfflineMapPackFormat? activeFormat,
    bool clearActiveFormat = false,
  }) =>
      OfflineMapLibraryState(
        mapPacks: mapPacks ?? this.mapPacks,
        activeFormat:
            clearActiveFormat ? null : (activeFormat ?? this.activeFormat),
      );
}

/// Phases surfaced while an offline map import runs.
enum OfflineMapImportPhase { queued, copying, complete }

/// Progress of an in-flight import.
class OfflineMapImportProgress {
  const OfflineMapImportProgress({
    this.phase = OfflineMapImportPhase.queued,
    this.bytesCopied = 0,
    this.totalBytes = 0,
  });

  final OfflineMapImportPhase phase;
  final int bytesCopied;
  final int totalBytes;

  /// 0-100 completion, or null when the total size is unknown.
  int? get percent {
    if (totalBytes <= 0) return null;
    final copied = bytesCopied > totalBytes ? totalBytes : bytesCopied;
    return (copied * 100) ~/ totalBytes;
  }
}

/// The outcome of a completed import.
class OfflineMapImportResult {
  const OfflineMapImportResult({
    required this.mapId,
    required this.displayName,
    required this.sizeBytes,
    this.format = OfflineMapPackFormat.pmtiles,
  });

  final String mapId;
  final String displayName;
  final int sizeBytes;
  final OfflineMapPackFormat format;
}
