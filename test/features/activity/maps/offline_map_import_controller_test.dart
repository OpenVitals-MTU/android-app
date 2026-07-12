import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/result/app_failure.dart';
import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/features/activity/maps/offline_map_import_controller.dart';
import 'package:openvitals/features/activity/maps/offline_map_metadata_store.dart';
import 'package:openvitals/features/activity/maps/offline_map_models.dart';
import 'package:path/path.dart' as p;

void main() {
  late Directory root;
  late Directory mapsDir;
  String? raw;

  setUp(() {
    root = Directory.systemTemp.createTempSync('offline_import_test');
    mapsDir = Directory(p.join(root.path, 'offline_maps'));
    raw = null;
  });

  tearDown(() {
    if (root.existsSync()) root.deleteSync(recursive: true);
  });

  OfflineMapImportController controller() => OfflineMapImportController(
        metadataStore: OfflineMapMetadataStore(
          readRaw: () => raw,
          writeRaw: (value) => raw = value,
          mapsDirectoryPath: mapsDir.path,
        ),
        mapsDirectoryPath: mapsDir.path,
      );

  File sourceFile(String name, List<int> bytes) =>
      File(p.join(root.path, name))..writeAsBytesSync(bytes);

  test('imports a pmtiles pack, copies the file and records metadata', () async {
    final source = sourceFile('estonia.pmtiles', List.filled(2048, 7));
    final ctrl = controller();
    final progress = <OfflineMapImportProgress>[];

    final pack =
        (await ctrl.importMap(source, onProgress: progress.add)).orThrow();

    expect(pack.format, OfflineMapPackFormat.pmtiles);
    expect(pack.displayName, 'estonia');
    expect(pack.sizeBytes, 2048);
    expect(File(pack.path).existsSync(), isTrue);
    expect(ctrl.state.value.mapPacks.single.id, pack.id);
    expect(ctrl.state.value.activeFormat, OfflineMapPackFormat.pmtiles);
    expect(progress.last.phase, OfflineMapImportPhase.complete);

    // Metadata survives a fresh store read.
    final reread = controller().state.value;
    expect(reread.mapPacks.single.id, pack.id);
  });

  test('rejects unsupported file extensions', () async {
    final source = sourceFile('estonia.osm.pbf', const [1, 2, 3]);

    final result = await controller().importMap(source);

    expect(result, isA<Err<OfflineMapPack>>());
    expect(
      (result as Err<OfflineMapPack>).failure,
      isA<UnexpectedFailure>().having(
        (failure) => failure.message,
        'message',
        contains('supported'),
      ),
    );
  });

  test('rejects a mapsforge pack that is not a valid map file', () async {
    // Kotlin validates by opening `MapFile(...)`; garbage bytes must fail the
    // import and leave nothing behind.
    final source = sourceFile('broken.map', List.filled(4096, 0xAB));
    final ctrl = controller();

    expect(await ctrl.importMap(source), isA<Err<OfflineMapPack>>());

    expect(ctrl.state.value.mapPacks, isEmpty);
    expect(
      mapsDir.existsSync() ? mapsDir.listSync() : const <FileSystemEntity>[],
      isEmpty,
      reason: 'failed imports must clean up temp and final files',
    );
  });

  test('deleteMap removes the file and its metadata entry', () async {
    final ctrl = controller();
    final pack = (await ctrl.importMap(
      sourceFile('city.pmtiles', List.filled(16, 1)),
    ))
        .orThrow();
    expect(File(pack.path).existsSync(), isTrue);

    await ctrl.deleteMap(pack.id);

    expect(File(pack.path).existsSync(), isFalse);
    expect(ctrl.state.value.mapPacks, isEmpty);
  });
}
