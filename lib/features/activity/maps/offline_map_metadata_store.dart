import 'dart:convert';
import 'dart:io';

import 'package:path/path.dart' as p;
import 'package:shared_preferences/shared_preferences.dart';

import 'offline_map_models.dart';

/// Persists the offline map library, ported from the Kotlin
/// `OfflineMapMetadataStore`.
///
/// The Kotlin store serialises to a `metadata.json` file; the Dart port keeps
/// the same JSON shape (so old payloads round-trip, including the legacy
/// `activeMapId` migration) but stores the string wherever [readRaw] /
/// [writeRaw] point — [OfflineMapMetadataStore.sharedPreferences] backs them
/// with [SharedPreferences]. Pack file paths are reconstructed from
/// [mapsDirectoryPath] and packs whose files no longer exist are dropped on
/// read, matching the source. All of this is pure logic: the persistence and
/// file-existence seams are injectable, so a test can round-trip in memory.
class OfflineMapMetadataStore {
  OfflineMapMetadataStore({
    required this.readRaw,
    required this.writeRaw,
    required this.mapsDirectoryPath,
    bool Function(String path)? fileExists,
  }) : _fileExists = fileExists ?? _defaultFileExists;

  /// SharedPreferences-backed store: the JSON payload lives under [prefsKey].
  factory OfflineMapMetadataStore.sharedPreferences(
    SharedPreferences prefs,
    String mapsDirectoryPath, {
    String prefsKey = defaultPrefsKey,
    bool Function(String path)? fileExists,
  }) =>
      OfflineMapMetadataStore(
        readRaw: () => prefs.getString(prefsKey),
        writeRaw: (value) => prefs.setString(prefsKey, value),
        mapsDirectoryPath: mapsDirectoryPath,
        fileExists: fileExists,
      );

  /// Reads the raw persisted JSON payload (null when absent).
  final String? Function() readRaw;

  /// Writes the raw JSON payload.
  final void Function(String value) writeRaw;

  /// Directory the pack files live in; used to reconstruct pack paths.
  final String mapsDirectoryPath;

  final bool Function(String path) _fileExists;

  static const String defaultPrefsKey = 'offline_maps_metadata';

  static bool _defaultFileExists(String path) => File(path).existsSync();

  static const String _activeFormatKey = 'activeFormat';
  static const String _activeMapIdKey = 'activeMapId';
  static const String _packsKey = 'packs';
  static const String _idKey = 'id';
  static const String _displayNameKey = 'displayName';
  static const String _originalFileNameKey = 'originalFileName';
  static const String _formatKey = 'format';
  static const String _sizeBytesKey = 'sizeBytes';
  static const String _importedAtMillisKey = 'importedAtMillis';

  OfflineMapLibraryState read() {
    final root = _decodeRoot();
    if (root == null) return const OfflineMapLibraryState();

    final rawPacks = root[_packsKey];
    final packs = <OfflineMapPack>[];
    if (rawPacks is List) {
      for (final element in rawPacks) {
        if (element is! Map) continue;
        final pack = _toMapPack(element.cast<String, dynamic>());
        if (pack != null && _fileExists(pack.path)) {
          packs.add(pack);
        }
      }
    }
    packs.sort((a, b) => b.importedAtMillis.compareTo(a.importedAtMillis));

    final activeFormat = _resolveActiveFormat(root, packs);
    return OfflineMapLibraryState(mapPacks: packs, activeFormat: activeFormat);
  }

  void write(OfflineMapLibraryState state) {
    final normalizedActiveFormat = state.activeFormat != null &&
            state.mapPacks.any((pack) => pack.format == state.activeFormat)
        ? state.activeFormat
        : null;
    final json = <String, dynamic>{
      if (normalizedActiveFormat != null)
        _activeFormatKey: normalizedActiveFormat.storageName,
      _packsKey: state.mapPacks
          .map(
            (pack) => <String, dynamic>{
              _idKey: pack.id,
              _displayNameKey: pack.displayName,
              _originalFileNameKey: pack.originalFileName,
              _formatKey: pack.format.storageName,
              _sizeBytesKey: pack.sizeBytes,
              _importedAtMillisKey: pack.importedAtMillis,
            },
          )
          .toList(),
    };
    writeRaw(jsonEncode(json));
  }

  Map<String, dynamic>? _decodeRoot() {
    final raw = readRaw();
    if (raw == null || raw.isEmpty) return null;
    try {
      final decoded = jsonDecode(raw);
      return decoded is Map ? decoded.cast<String, dynamic>() : null;
    } catch (_) {
      return null;
    }
  }

  OfflineMapPack? _toMapPack(Map<String, dynamic> json) {
    final id = _stringOrNull(json[_idKey]);
    if (id == null || id.isEmpty) return null;
    final displayName = _stringOrNull(json[_displayNameKey]);
    if (displayName == null || displayName.isEmpty) return null;

    final savedOriginalFileName = _stringOrNull(json[_originalFileNameKey]);
    final format = _formatOrNull(json[_formatKey]) ??
        (savedOriginalFileName != null
            ? OfflineMapPackFormat.fromFileName(savedOriginalFileName)
            : null) ??
        OfflineMapPackFormat.pmtiles;
    final originalFileName =
        (savedOriginalFileName != null && savedOriginalFileName.isNotEmpty)
            ? savedOriginalFileName
            : '$displayName${format.fileExtension}';
    final sizeBytes = _intOrNull(json[_sizeBytesKey]);
    final importedAtMillis = _intOrNull(json[_importedAtMillisKey]);

    return OfflineMapPack(
      id: id,
      displayName: displayName,
      originalFileName: originalFileName,
      sizeBytes: (sizeBytes != null && sizeBytes >= 0) ? sizeBytes : 0,
      importedAtMillis:
          (importedAtMillis != null && importedAtMillis > 0) ? importedAtMillis : 0,
      path: p.join(mapsDirectoryPath, '$id${format.fileExtension}'),
      format: format,
    );
  }

  OfflineMapPackFormat? _resolveActiveFormat(
    Map<String, dynamic> root,
    List<OfflineMapPack> packs,
  ) {
    final rawFormat = _formatOrNull(root[_activeFormatKey]);
    if (rawFormat != null && packs.any((pack) => pack.format == rawFormat)) {
      return rawFormat;
    }
    final legacyId = _stringOrNull(root[_activeMapIdKey]);
    if (legacyId != null) {
      for (final pack in packs) {
        if (pack.id == legacyId) return pack.format;
      }
    }
    return null;
  }

  static OfflineMapPackFormat? _formatOrNull(Object? value) {
    final name = _stringOrNull(value);
    return name == null ? null : OfflineMapPackFormat.fromStorage(name);
  }

  static String? _stringOrNull(Object? value) => value is String ? value : null;

  static int? _intOrNull(Object? value) {
    if (value is int) return value;
    if (value is num) return value.toInt();
    if (value is String) return int.tryParse(value);
    return null;
  }
}
