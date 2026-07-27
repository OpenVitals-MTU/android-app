import 'dart:async';
import 'dart:ui' as ui;

import 'package:flutter_test/flutter_test.dart';
import 'package:mapsforge_flutter_renderer/offline_renderer.dart';
import 'package:mapsforge_flutter_renderer/ui.dart' show TilePicture;

import 'package:openvitals/features/activity/maps/mapsforge_tile_renderer.dart';

/// The tile path's coordination layer, at the edges that used to have none: one
/// reader isolate instead of one per tile, a bounded number of concurrent
/// renders, and a cache whose eviction must not pull an image out from under a
/// caller already holding it.
///
/// A hand-written fake over the 5-member [Renderer] interface — no real `.map`
/// file, no widget tree.
class _FakeRenderer implements Renderer {
  _FakeRenderer({this.answer});

  /// Per-call answer; defaults to a drawn green tile.
  final JobResult Function(int callIndex)? answer;

  int calls = 0;
  int inFlight = 0;
  int maxInFlight = 0;
  bool throwOnRender = false;

  /// When set, every render parks on this until it is completed.
  Completer<void>? gate;

  @override
  Future<JobResult> executeJob(JobRequest jobRequest) async {
    final index = calls++;
    inFlight++;
    if (inFlight > maxInFlight) maxInFlight = inFlight;
    try {
      if (gate != null) await gate!.future;
      if (throwOnRender) throw StateError('render failed');
      return answer?.call(index) ??
          JobResult.normal(TilePicture.fromPicture(_picture(_green)));
    } finally {
      inFlight--;
    }
  }

  @override
  Future<JobResult> retrieveLabels(JobRequest jobRequest) async =>
      throw UnimplementedError();

  @override
  String getRenderKey() => 'fake';

  @override
  bool supportLabels() => false;

  @override
  void dispose() {}
}

const ui.Color _green = ui.Color(0xFF00FF00);
const ui.Color _red = ui.Color(0xFFFF0000);

/// The same colours as `toByteData(rawRgba)` reports them: R,G,B,A in byte
/// order, read back big-endian.
const int _greenRgba = 0x00FF00FF;
const int _redRgba = 0xFF0000FF;

ui.Picture _picture(ui.Color color) {
  final recorder = ui.PictureRecorder();
  ui.Canvas(recorder).drawRect(
    const ui.Rect.fromLTWH(0, 0, 256, 256),
    ui.Paint()..color = color,
  );
  return recorder.endRecording();
}

/// The colour of the pixel at (0,0), so a test can tell one render from another.
Future<int> _topLeft(ui.Image image) async {
  final data = await image.toByteData(format: ui.ImageByteFormat.rawRgba);
  return data!.getUint32(0);
}

/// A renderer whose reader is already warm, with the fake's counters reset — so
/// a test measures only the tiles it asks for, not the warm-up render.
Future<MapsforgeTileRenderer> _warmed(_FakeRenderer fake, {int? maxCacheBytes}) async {
  final renderer = MapsforgeTileRenderer(
    fake,
    maxCacheBytes: maxCacheBytes ?? 48 << 20,
  );
  await renderer.warmUp();
  fake.calls = 0;
  fake.maxInFlight = 0;
  return renderer;
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  test('twenty concurrent tile requests warm the renderer exactly once',
      () async {
    // The regression test for the isolate spawn storm: DatastoreRenderer's own
    // `_datastoreReader ??= await ...` tests for null before it suspends, so
    // every concurrent tile spawned its own reader isolate at ~600ms each.
    final fake = _FakeRenderer();
    final renderer = MapsforgeTileRenderer(fake);

    await Future.wait([
      for (var i = 0; i < 20; i++) renderer.tile(14, 8710 + i, 4300),
    ]);

    expect(renderer.stats.warmUps, 1);
  });

  test('a second request for the same tile is served from the cache and never '
      're-renders', () async {
    // Green first, red afterwards: if the second request re-rendered, the map
    // would visibly flicker to different content for the same coordinate.
    final fake = _FakeRenderer(
      answer: (index) => JobResult.normal(
        TilePicture.fromPicture(_picture(index == 0 ? _green : _red)),
      ),
    );
    final renderer = MapsforgeTileRenderer(fake);
    await renderer.warmUp(); // consumes answer index 0

    final first = await renderer.tile(14, 8710, 4300);
    final second = await renderer.tile(14, 8710, 4300);

    expect(await _topLeft(first!), await _topLeft(second!));
    expect(await _topLeft(second), _redRgba,
        reason: 'index 1 drew red; a re-render would have drawn index 2');
    expect(renderer.stats.cacheHits, 1);
    expect(fake.calls, 2, reason: 'the warm-up plus exactly one tile render');
  });

  test('the renderer never has more than four renders in flight', () async {
    final fake = _FakeRenderer();
    final renderer = await _warmed(fake);
    fake.gate = Completer<void>();

    final pending = <Future<ui.Image?>>[
      for (var i = 0; i < 20; i++) renderer.tile(14, 8710 + i, 4300),
    ];
    await pumpEventQueue();
    expect(fake.inFlight, 4, reason: 'the queue must cap concurrency at four');

    fake.gate!.complete();
    await Future.wait(pending);

    expect(fake.maxInFlight, 4);
    expect(renderer.stats.maxInFlight, 4);
  });

  test('concurrent requests for the same tile share one render', () async {
    final fake = _FakeRenderer();
    final renderer = await _warmed(fake);
    fake.gate = Completer<void>();

    final pending = <Future<ui.Image?>>[
      for (var i = 0; i < 5; i++) renderer.tile(14, 8710, 4300),
    ];
    await pumpEventQueue();
    fake.gate!.complete();
    final images = await Future.wait(pending);

    expect(renderer.stats.coalesced, 4);
    expect(fake.calls, 1);
    // Each caller owns its own handle, so disposing one must not break another.
    for (final image in images) {
      expect(image, isNotNull);
      expect(image!.debugDisposed, isFalse);
    }
  });

  test('evicting a cached tile disposes the master but not an image already '
      'handed out', () async {
    // A one-byte budget keeps exactly one tile, whatever a tile weighs.
    final renderer = await _warmed(_FakeRenderer(), maxCacheBytes: 1);

    final handedOut = await renderer.tile(14, 1, 1);
    await renderer.tile(14, 2, 2); // pushes the first out of the budget

    expect(renderer.stats.evictions, greaterThan(0));
    expect(handedOut!.debugDisposed, isFalse,
        reason: 'the caller owns its clone; eviction may only drop the master');
    // Still drawable: the underlying buffer outlives the master's handle.
    expect(await _topLeft(handedOut), _greenRgba);
  });

  test('a tile the packs do not cover is rendered once and then remembered as '
      'empty', () async {
    final fake = _FakeRenderer(answer: (_) => JobResult.unsupported());
    final renderer = await _warmed(fake);

    final first = await renderer.tile(14, 8710, 4300);
    final second = await renderer.tile(14, 8710, 4300);

    expect(first, isNull);
    expect(second, isNull);
    expect(fake.calls, 1, reason: 'an ocean tile must not be re-read per visit');
    expect(renderer.stats.emptyTiles, 1);
  });

  test('a renderer failure surfaces as an empty tile, never as a thrown map',
      () async {
    final fake = _FakeRenderer();
    final renderer = await _warmed(fake);
    fake.throwOnRender = true;

    expect(await renderer.tile(14, 8710, 4300), isNull);
  });

  test('a failed tile is retried rather than remembered as empty', () async {
    // A transient failure must not blank that coordinate for the session.
    final fake = _FakeRenderer();
    final renderer = await _warmed(fake);
    fake.throwOnRender = true;

    await renderer.tile(14, 8710, 4300);
    fake.throwOnRender = false;
    final retried = await renderer.tile(14, 8710, 4300);

    expect(retried, isNotNull);
    expect(fake.calls, 2);
  });

  test('disposing the renderer drops its cached tiles and answers null',
      () async {
    final renderer = await _warmed(_FakeRenderer());
    await renderer.tile(14, 8710, 4300);

    renderer.dispose();

    expect(renderer.stats.cachedTiles, 0);
    expect(renderer.stats.cachedBytes, 0);
    expect(await renderer.tile(14, 8710, 4300), isNull);
  });
}
