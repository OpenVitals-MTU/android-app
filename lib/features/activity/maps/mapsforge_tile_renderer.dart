import 'dart:async';
import 'dart:collection';
import 'dart:developer' as developer;
import 'dart:ui' as ui;

import 'package:flutter/foundation.dart';
import 'package:mapsforge_flutter_core/model.dart' as mapsforge;
import 'package:mapsforge_flutter_core/task_queue.dart';
import 'package:mapsforge_flutter_renderer/offline_renderer.dart';
import 'package:mapsforge_flutter_rendertheme/model.dart';

/// A tile's coordinate, as the cache keys it.
///
/// Deliberately not `mapsforge.Tile`, which also carries an indoor level and
/// derived geometry that have no business in a cache key.
@immutable
class MapsforgeTileKey {
  const MapsforgeTileKey(this.z, this.x, this.y);

  final int z;
  final int x;
  final int y;

  @override
  bool operator ==(Object other) =>
      other is MapsforgeTileKey && other.z == z && other.x == x && other.y == y;

  @override
  int get hashCode => Object.hash(z, x, y);

  @override
  String toString() => 'z=$z x=$x y=$y';
}

/// Counters for what the tile path actually did.
///
/// Plain ints behind no `assert`, so they are readable in profile and release
/// builds as well as in tests — unlike the package's own [PerformanceProfiler],
/// whose enable flag is written inside an assert and therefore stripped
/// everywhere but debug.
class MapsforgeTileStats {
  /// Renders that actually reached the mapsforge renderer.
  int renders = 0;

  /// Requests served from the rasterised-tile cache.
  int cacheHits = 0;

  /// Requests that joined a render already in flight for the same tile.
  int coalesced = 0;

  /// Tiles the active packs hold no data for, remembered as such.
  int emptyTiles = 0;

  /// Warm-up runs. Must be exactly one per renderer, ever — see [warmUp].
  int warmUps = 0;

  /// The high-water mark of concurrent renders. Must never exceed
  /// [MapsforgeTileRenderer.maxConcurrentRenders].
  int maxInFlight = 0;

  /// Cached tiles dropped to stay inside the byte budget.
  int evictions = 0;

  /// Tiles currently held (including remembered-empty ones).
  int cachedTiles = 0;

  /// Bytes currently held by cached tile images.
  int cachedBytes = 0;

  @override
  String toString() => 'renders=$renders hits=$cacheHits coalesced=$coalesced '
      'empty=$emptyTiles evictions=$evictions '
      'cached=$cachedTiles (${cachedBytes >> 20}MB) maxInFlight=$maxInFlight';
}

/// One cached tile: the master image, or null when the packs cover nothing here.
class _CachedTile {
  _CachedTile(this.image);

  /// The master. Never handed out — callers get [ui.Image.clone]s of it.
  final ui.Image? image;

  /// Set before [image] is disposed, so a caller whose `await` resumed after
  /// this entry was evicted can tell rather than cloning a dead handle.
  bool disposed = false;

  int get bytes => image == null ? 0 : image!.width * image!.height * 4;
}

/// Serves rasterised Mapsforge tiles: one renderer, one reader isolate, a
/// bounded render queue and a byte-budgeted cache of what has been drawn.
///
/// This exists because the pieces below it have no such coordination.
///
/// **The warm-up gate.** `DatastoreRenderer.executeJob` lazily creates its
/// reader isolate with `_datastoreReader ??= await IsolateDatastoreReader
/// .create(datastore)`. `??=` evaluates the null test *before* suspending on the
/// `await`, so every concurrent call sees null and spawns its own isolate —
/// flutter_map asks for a dozen or more tiles in a single frame, and the
/// package's own comment puts an isolate spawn at ~600ms. All but the last are
/// orphaned forever (`IsolateDatastoreReader` has no dispose). [warmUp] closes
/// that window by making the first spawn a single shared future.
///
/// **The queue.** Tile requests arrive as fast as flutter_map can issue them.
/// Upstream's own view layer caps renderer calls at four
/// (`TileJobQueue._maxConcurrentTiles`); nothing did here.
///
/// **The cache.** The packages this app depends on ship no tile cache at all —
/// `MemoryTileCache` lives in the umbrella `mapsforge_flutter` package, which is
/// not a dependency. Flutter's own `imageCache` does hold rendered tiles (
/// flutter_map only evicts them on load *error*, and this app leaves
/// `evictErrorTileStrategy` at `none`), but it is a shared 100MB budget the rest
/// of the app competes for, and it dies with the `TileLayer`. This one is
/// dedicated, byte-budgeted, and outlives the layer.
class MapsforgeTileRenderer {
  MapsforgeTileRenderer(
    this.renderer, {
    this.maxConcurrentRenders = 4,
    this.maxCacheBytes = 48 << 20,
    mapsforge.Tile? warmUpTile,
  })  : _warmUpTile = warmUpTile ?? mapsforge.Tile(0, 0, 0, 0),
        _queue = ParallelTaskQueue(maxConcurrentRenders,
            name: 'MapsforgeTileRenderer');

  /// Typed as the 5-member [Renderer] interface rather than [DatastoreRenderer]
  /// so a test can hand-write a fake without `noSuchMethod`.
  final Renderer renderer;

  /// The ceiling on concurrent renders, matching upstream's own tile queue.
  final int maxConcurrentRenders;

  /// The cache budget. A 256² RGBA tile is 256KB, so the default holds ~190.
  final int maxCacheBytes;

  final mapsforge.Tile _warmUpTile;
  final ParallelTaskQueue _queue;
  final MapsforgeTileStats _stats = MapsforgeTileStats();

  /// Insertion-ordered, so the oldest key is the least recently used: a hit
  /// re-inserts to move the entry to the end.
  final LinkedHashMap<MapsforgeTileKey, _CachedTile> _cache =
      LinkedHashMap<MapsforgeTileKey, _CachedTile>();

  final Map<MapsforgeTileKey, Future<_CachedTile?>> _inFlight =
      <MapsforgeTileKey, Future<_CachedTile?>>{};

  Future<void>? _warmUp;
  int _inFlightRenders = 0;
  bool _disposed = false;

  MapsforgeTileStats get stats => _stats;

  /// Spawns the renderer's reader isolate exactly once, before anything can ask
  /// for tiles in parallel.
  ///
  /// Idempotent and cheap to call repeatedly — [tile] awaits it on **every**
  /// request, which is what actually guarantees the single spawn. Calling it up
  /// front merely moves the ~600ms off the first paint.
  ///
  /// The `??=` here is safe where the package's is not: the right-hand side is a
  /// synchronous call that returns a future, so nothing suspends between the
  /// null test and the assignment.
  Future<void> warmUp() => _warmUp ??= _runWarmUp();

  Future<void> _runWarmUp() async {
    _stats.warmUps++;
    try {
      final result = await developer.Timeline.timeSync(
        'mapsforge.warmUp',
        () => renderer.executeJob(JobRequest(_warmUpTile)),
      );
      // The warm-up exists to spawn the reader isolate; its tile is thrown
      // away, so release what it drew rather than leaking a picture.
      result.picture?.getPicture()?.dispose();
    } catch (error, stack) {
      // The reader may well have been assigned anyway — executeJob assigns it
      // before it reads — but we cannot know. Re-arm so the first real request
      // takes the gate and serialises itself instead of racing.
      _warmUp = null;
      debugPrint('Mapsforge warm-up failed: $error\n$stack');
    }
  }

  /// One rendered tile, or null when the active packs hold no data for it.
  ///
  /// The returned image belongs to the CALLER, who must dispose it. It is a
  /// clone sharing the cached master's pixel buffer, so it costs no extra bytes.
  ///
  /// Never throws for a per-tile failure: a broken tile is a null, exactly like
  /// an uncovered one, because the map must not take the screen down.
  Future<ui.Image?> tile(int z, int x, int y) async {
    if (_disposed) return null;
    await warmUp();
    if (_disposed) return null;

    final key = MapsforgeTileKey(z, x, y);

    final cached = _lookup(key);
    if (cached != null) {
      _stats.cacheHits++;
      // No suspension point between the lookup and the clone, so the master
      // cannot be evicted out from under it.
      return cached.image?.clone();
    }

    final inFlight = _inFlight[key];
    if (inFlight != null) {
      _stats.coalesced++;
      return _cloneWhenReady(inFlight);
    }

    final future = _renderAndCache(key);
    _inFlight[key] = future;
    return _cloneWhenReady(future);
  }

  /// The labels for a rectangle of tiles, collision-resolved as one set.
  ///
  /// Separate from [tile] because labels are no longer drawn INTO tiles: a label
  /// wider than the tile it is anchored in was clipped by that tile's canvas,
  /// and mapsforge's cross-tile compensation only works when the neighbour is
  /// rendered second — a coin flip once tiles render concurrently. Resolved over
  /// the whole visible rectangle at once, they cannot be clipped and cannot
  /// disagree between neighbours.
  ///
  /// Returns null when the packs cover nothing here, or on any failure — a
  /// missing label must never take the map down.
  Future<RenderInfoCollection?> labels(
    mapsforge.Tile upperLeft,
    mapsforge.Tile lowerRight,
  ) async {
    if (_disposed) return null;
    await warmUp();
    if (_disposed) return null;
    try {
      final result = await _queue.add(
        () => renderer.retrieveLabels(JobRequest(upperLeft, lowerRight)),
      );
      if (result.result != JOBRESULT.NORMAL) return null;
      return result.renderInfo;
    } catch (error, stack) {
      if (!_disposed) {
        debugPrint('Mapsforge labels $upperLeft..$lowerRight failed: '
            '$error\n$stack');
      }
      return null;
    }
  }

  /// Awaits a render, then clones its master — re-checking liveness first,
  /// because another caller's continuation may have evicted it while this one
  /// was parked.
  Future<ui.Image?> _cloneWhenReady(Future<_CachedTile?> pending) async {
    final entry = await pending;
    if (entry == null || entry.image == null || entry.disposed) return null;
    return entry.image!.clone();
  }

  _CachedTile? _lookup(MapsforgeTileKey key) {
    final entry = _cache.remove(key);
    if (entry == null) return null;
    _cache[key] = entry; // re-insert: most recently used goes last
    return entry;
  }

  Future<_CachedTile?> _renderAndCache(MapsforgeTileKey key) async {
    final task = developer.TimelineTask()
      ..start('mapsforge.tile', arguments: {'z': key.z, 'x': key.x, 'y': key.y});
    try {
      final image = await _queue.add(() => _render(key));
      final entry = _CachedTile(image);
      if (image == null) _stats.emptyTiles++;
      _store(key, entry);
      task.finish(arguments: {'empty': image == null});
      return entry;
    } catch (error, stack) {
      // QueueCancelledException on dispose, or a renderer failure: either way
      // the tile is simply absent. Not cached — a transient failure should not
      // blank that coordinate for the rest of the session.
      task.finish(arguments: {'error': '$error'});
      if (!_disposed) debugPrint('Mapsforge tile $key failed: $error\n$stack');
      return null;
    } finally {
      _inFlight.remove(key);
    }
  }

  Future<ui.Image?> _render(MapsforgeTileKey key) async {
    if (_disposed) return null;
    _stats.renders++;
    _inFlightRenders++;
    if (_inFlightRenders > _stats.maxInFlight) {
      _stats.maxInFlight = _inFlightRenders;
    }
    try {
      final result =
          await renderer.executeJob(JobRequest(mapsforge.Tile(key.x, key.y, key.z, 0)));
      final picture = result.picture;
      if (result.result != JOBRESULT.NORMAL || picture == null) return null;
      // `convertPictureToImage` returns either the picture's own image (which
      // the TilePicture would dispose) or a freshly rasterized one (which it
      // would not), so clone for the cache and release both originals here. The
      // clone becomes the master; every handout is a clone of it.
      final image = await picture.convertPictureToImage();
      final master = image.clone();
      image.dispose();
      picture.getPicture()?.dispose();
      return master;
    } finally {
      _inFlightRenders--;
    }
  }

  /// Stores [entry] and evicts from the front until the budget holds.
  ///
  /// Eviction removes the entry from the map first and disposes its master
  /// second. A clone already handed out survives that: `ui.Image` handles are
  /// ref-counted over a shared buffer, so disposing the master only releases the
  /// cache's own handle.
  void _store(MapsforgeTileKey key, _CachedTile entry) {
    if (_disposed) {
      _release(entry);
      return;
    }
    _cache[key] = entry;
    _stats.cachedBytes += entry.bytes;
    while (_stats.cachedBytes > maxCacheBytes && _cache.length > 1) {
      final oldest = _cache.keys.first;
      final evicted = _cache.remove(oldest)!;
      _stats.cachedBytes -= evicted.bytes;
      _stats.evictions++;
      _release(evicted);
    }
    _stats.cachedTiles = _cache.length;
  }

  void _release(_CachedTile entry) {
    entry.disposed = true;
    entry.image?.dispose();
  }

  /// Tears down the queue, the cached masters and the renderer beneath.
  void dispose() {
    if (_disposed) return;
    _disposed = true;
    _queue.cancel();
    for (final entry in _cache.values) {
      _release(entry);
    }
    _cache.clear();
    _stats.cachedTiles = 0;
    _stats.cachedBytes = 0;
    renderer.dispose();
  }
}
