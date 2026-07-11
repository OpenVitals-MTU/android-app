import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/features/activity/maps/offline_map_metadata_store.dart';
import 'package:openvitals/features/activity/maps/offline_map_models.dart';
import 'package:path/path.dart' as p;

/// Ported from the Kotlin `OfflineMapMetadataStoreTest` +
/// `OfflineMapPackFormatTest`.
void main() {
  late Directory mapsDir;
  String? raw;

  setUp(() {
    mapsDir = Directory.systemTemp.createTempSync('offline_maps_test');
    raw = null;
  });

  tearDown(() {
    if (mapsDir.existsSync()) mapsDir.deleteSync(recursive: true);
  });

  OfflineMapMetadataStore store() => OfflineMapMetadataStore(
        readRaw: () => raw,
        writeRaw: (value) => raw = value,
        mapsDirectoryPath: mapsDir.path,
      );

  OfflineMapPack mapPack(
    String id, {
    required int importedAtMillis,
    OfflineMapPackFormat format = OfflineMapPackFormat.pmtiles,
  }) {
    final file = File(p.join(mapsDir.path, '$id${format.fileExtension}'))
      ..writeAsBytesSync(const [1, 2, 3]);
    return OfflineMapPack(
      id: id,
      displayName: id,
      originalFileName: '$id${format.fileExtension}',
      sizeBytes: file.lengthSync(),
      importedAtMillis: importedAtMillis,
      path: file.path,
      format: format,
    );
  }

  test('write then read preserves imported maps sorted newest-first', () {
    final first = mapPack('city-a', importedAtMillis: 1000);
    final second = mapPack('city-b', importedAtMillis: 2000);

    store().write(
      OfflineMapLibraryState(
        mapPacks: [first, second],
        activeFormat: OfflineMapPackFormat.pmtiles,
      ),
    );
    final read = store().read();

    expect(read.mapPacks.map((pack) => pack.id), ['city-b', 'city-a']);
    expect(read.activeFormat, OfflineMapPackFormat.pmtiles);
  });

  test('read drops missing map files and clears active format', () {
    final pack = mapPack('missing-city', importedAtMillis: 1000);
    store().write(
      OfflineMapLibraryState(mapPacks: [pack], activeFormat: pack.format),
    );

    File(pack.path).deleteSync();
    final read = store().read();

    expect(read.mapPacks, isEmpty);
    expect(read.activeFormat, isNull);
  });

  test('preserves mapsforge format and .map extension in the path', () {
    final pack = mapPack(
      'estonia',
      importedAtMillis: 1000,
      format: OfflineMapPackFormat.mapsforge,
    );
    store().write(
      OfflineMapLibraryState(mapPacks: [pack], activeFormat: pack.format),
    );

    final read = store().read();

    expect(read.mapPacks.single.format, OfflineMapPackFormat.mapsforge);
    expect(read.mapPacks.single.path, p.join(mapsDir.path, 'estonia.map'));
    expect(read.activeFormat, OfflineMapPackFormat.mapsforge);
  });

  test('migrates legacy activeMapId to activeFormat', () {
    File(p.join(mapsDir.path, 'estonia.map')).writeAsBytesSync(const [1, 2, 3]);
    raw = '''
      {
        "activeMapId": "estonia",
        "packs": [
          {
            "id": "estonia",
            "displayName": "estonia",
            "originalFileName": "estonia.map",
            "format": "MAPSFORGE",
            "sizeBytes": 3,
            "importedAtMillis": 1000
          }
        ]
      }
    ''';

    final read = store().read();

    expect(read.activeFormat, OfflineMapPackFormat.mapsforge);
  });

  test('detects supported offline map file extensions', () {
    expect(
      OfflineMapPackFormat.fromFileName('estonia.pmtiles'),
      OfflineMapPackFormat.pmtiles,
    );
    expect(
      OfflineMapPackFormat.fromFileName('estonia.map'),
      OfflineMapPackFormat.mapsforge,
    );
    expect(
      OfflineMapPackFormat.fromFileName('estonia.maps'),
      OfflineMapPackFormat.mapsforge,
    );
    expect(OfflineMapPackFormat.fromFileName('estonia.osm.pbf'), isNull);
  });
}
