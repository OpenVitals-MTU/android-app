import 'dart:async';
import 'dart:ui' as ui;

import 'package:flutter/painting.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mapsforge_flutter_renderer/offline_renderer.dart';
import 'package:mapsforge_flutter_renderer/ui.dart' show TilePicture;
import 'package:pmtiles/pmtiles.dart';
import 'package:vector_map_tiles/vector_map_tiles.dart';

import 'package:openvitals/features/activity/maps/mapsforge_tile_provider.dart';
import 'package:openvitals/features/activity/maps/pmtiles_tile_provider.dart';

/// The two offline tile providers, at their failure edges. The user-visible
/// symptom of getting these wrong is a BLANK map mid-activity — a tile outside
/// pack coverage must degrade to "nothing drawn there", and a broken read must
/// stay a per-tile error, never a crash of the whole map view.
class _FakeRenderer implements DatastoreRenderer {
  _FakeRenderer(this.answer);

  final JobResult Function() answer;

  @override
  Future<JobResult> executeJob(JobRequest jobRequest) async => answer();

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class _FakeArchive implements PmTilesArchive {
  _FakeArchive(this.answer);

  final Tile Function(int tileId) answer;

  @override
  int get minZoom => 4;

  @override
  int get maxZoom => 14;

  @override
  Future<Tile> tile(int tileId) async => answer(tileId);

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

/// Resolves one tile through the ImageProvider machinery, as flutter_map does.
Future<ImageInfo> _resolve(MapsforgeTileProvider provider) {
  final completer = Completer<ImageInfo>();
  provider
      .getImage(const TileCoordinates(8710, 4300, 14), TileLayer())
      .resolve(ImageConfiguration.empty)
      .addListener(ImageStreamListener(
        (info, _) => completer.complete(info),
        onError: (error, _) => completer.completeError(error),
      ));
  return completer.future;
}

ui.Picture _drawnPicture() {
  final recorder = ui.PictureRecorder();
  ui.Canvas(recorder).drawRect(
    const ui.Rect.fromLTWH(0, 0, 10, 10),
    ui.Paint()..color = const ui.Color(0xFF00FF00),
  );
  return recorder.endRecording();
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  group('MapsforgeTileProvider', () {
    test('a tile outside pack coverage draws as transparent, not an error',
        () async {
      final provider =
          MapsforgeTileProvider(_FakeRenderer(JobResult.unsupported));

      final info = await _resolve(provider);

      // A 1x1 transparent stand-in — flutter_map shows the plain background,
      // like the Android mapsforge view left uncovered tiles empty.
      expect(info.image.width, 1);
      expect(info.image.height, 1);
    });

    test('a rendered tile reaches flutter_map as a live image', () async {
      final provider = MapsforgeTileProvider(_FakeRenderer(
        () => JobResult.normal(TilePicture.fromPicture(_drawnPicture())),
      ));

      final info = await _resolve(provider);

      expect(info.image.width, greaterThan(1),
          reason: 'the rendered picture must be rasterized at tile size, and '
              'the clone handed out must survive the originals being disposed');
      expect(info.image.debugDisposed, isFalse);
    });

    test('tiles are keyed by coordinate, so the image cache can work',
        () async {
      final renderer = _FakeRenderer(JobResult.unsupported);
      final provider = MapsforgeTileProvider(renderer);
      ImageProvider at(int x, int y, int z) =>
          provider.getImage(TileCoordinates(x, y, z), TileLayer());

      expect(at(1, 2, 3), at(1, 2, 3));
      expect(at(1, 2, 3).hashCode, at(1, 2, 3).hashCode);
      expect(at(1, 2, 3), isNot(at(1, 2, 4)));
    });
  });

  group('PmtilesVectorTileProvider', () {
    test('zoom bounds come from the archive header', () {
      final provider = PmtilesVectorTileProvider(_FakeArchive(
        (_) => Tile(0, bytes: const [1], compression: Compression.none),
      ));

      expect(provider.minimumZoom, 4);
      expect(provider.maximumZoom, 14);
    });

    test('serves the uncompressed tile bytes', () async {
      final provider = PmtilesVectorTileProvider(_FakeArchive(
        (_) => Tile(0, bytes: const [1, 2, 3], compression: Compression.none),
      ));

      expect(await provider.provide(TileIdentity(5, 10, 11)), [1, 2, 3]);
    });

    test('a missing tile maps to a 404 the map renders as empty', () async {
      final provider = PmtilesVectorTileProvider(_FakeArchive(
        (id) => Tile(id, exception: TileNotFoundException(id)),
      ));

      await expectLater(
        provider.provide(TileIdentity(5, 10, 11)),
        throwsA(isA<ProviderException>()
            .having((e) => e.statusCode, 'statusCode', 404)
            .having((e) => e.retryable, 'retryable', Retryable.none)),
      );
    });

    test('a corrupt read maps to a per-tile 500, never a crash', () async {
      final provider = PmtilesVectorTileProvider(_FakeArchive(
        (id) => Tile(id, exception: Exception('truncated archive')),
      ));

      await expectLater(
        provider.provide(TileIdentity(5, 10, 11)),
        throwsA(isA<ProviderException>()
            .having((e) => e.statusCode, 'statusCode', 500)),
      );
    });
  });
}
