import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:flutter/services.dart' show rootBundle;
import 'package:flutter/widgets.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:mapsforge_flutter_core/model.dart' as mapsforge;
import 'package:mapsforge_flutter_core/projection.dart' as mapsforge;
import 'package:mapsforge_flutter_mapfile/mapfile.dart';
import 'package:mapsforge_flutter_renderer/cache.dart';
import 'package:mapsforge_flutter_renderer/offline_renderer.dart';
import 'package:mapsforge_flutter_rendertheme/rendertheme.dart';
import 'package:pmtiles/pmtiles.dart' as pmt;
import 'package:vector_map_tiles/vector_map_tiles.dart';
import 'package:vector_tile_renderer/vector_tile_renderer.dart' as vtr;

import 'mapsforge_tile_provider.dart';
import 'mapsforge_tile_renderer.dart';
import 'offline_base_map.dart';
import 'offline_map_models.dart';
import 'offline_map_style.dart';
import 'pmtiles_tile_provider.dart';

/// The offline base-map layer inside a [FlutterMap], the port of the Kotlin
/// engine split in `OfflineRouteMapOrPreview`:
///
/// * active format PMTILES → the bundled Protomaps style expanded per pack
///   (`offline_map_style.dart`) rendered as a [VectorTileLayer], the
///   equivalent of the Kotlin MapLibre view with its `pmtiles://` sources;
/// * active format MAPSFORGE → a raster [TileLayer] whose tiles the pure-Dart
///   Mapsforge renderer draws on demand from a
///   `MultimapDatastore(DataPolicy.RETURN_ALL)` over all active packs with
///   the stock `default.xml` render theme — Kotlin's `TileRendererLayer` +
///   `MapsforgeThemes.DEFAULT`;
/// * no active pack, resources still loading, or a pack that fails to open →
///   nothing, leaving the plain route canvas exactly like Kotlin's
///   `RoutePreview` fallback.
class OfflineBaseMapLayer extends ConsumerWidget {
  const OfflineBaseMapLayer({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final resources = ref.watch(_offlineBaseMapResourcesProvider).value;
    return switch (resources) {
      final _PmtilesResources resources => VectorTileLayer(
          tileProviders: resources.providers,
          theme: resources.theme,
          tileOffset: TileOffset.DEFAULT,
        ),
      final _MapsforgeResources resources => TileLayer(
          tileProvider: resources.tileProvider,
          userAgentPackageName: 'tech.mmarca.openvitals',
          // The default transformer emits a tile update for EVERY map event —
          // every frame of a pinch — and each one asks for a fresh tile set at
          // the newly rounded zoom. With tiles this expensive that is the whole
          // "zooming reloads everything" symptom. Throttle, not debounce:
          // debounce loads nothing until the gesture ends, which reads as a
          // frozen map.
          tileUpdateTransformer:
              TileUpdateTransformers.throttle(const Duration(milliseconds: 200)),
          // Each fading tile is an AnimationController. Twenty of them on the
          // first-paint frame is a cost the offline renderer cannot afford, and
          // with keepAlive they would idle on for the screen's life.
          tileDisplay: const TileDisplay.instantaneous(),
          // Prefetching a ring the user may never pan to is a poor trade when a
          // tile costs a vector render. keepBuffer stays at its default 2, so
          // already-drawn neighbours are still not pruned.
          panBuffer: 0,
        ),
      null => const SizedBox.shrink(),
    };
  }
}

/// The heavy resources for the active pack set, held for the app's lifetime
/// and rebuilt (old set disposed) only when the active format or pack list
/// changes. App-lifetime rather than per-screen on purpose: the Mapsforge
/// renderer spawns a reader isolate whose ~600ms startup would tax every
/// screen open — and [DatastoreRenderer.dispose] never kills that isolate, so
/// a per-screen lifecycle would leak one isolate per visit.
final _offlineBaseMapResourcesProvider =
    FutureProvider<_OfflineBaseMapResources?>((ref) async {
  final baseMap = ref.watch(offlineBaseMapProvider);
  if (baseMap == null) return null;
  final _OfflineBaseMapResources resources;
  try {
    resources = await _loadResources(baseMap);
  } catch (error) {
    // Kotlin's Mapsforge path falls back to the plain RoutePreview when a
    // pack fails to open; a broken pack must not take the screen down.
    debugPrint('Offline base map failed to load: $error');
    return null;
  }
  ref.onDispose(resources.dispose);
  return resources;
});

/// Heavyweight per-pack-set resources (open archives / datastores / themes),
/// rebuilt whenever the active format or pack list changes.
sealed class _OfflineBaseMapResources {
  void dispose();
}

class _PmtilesResources implements _OfflineBaseMapResources {
  _PmtilesResources(this.archives, this.theme, this.providers);

  final List<pmt.PmTilesArchive> archives;
  final vtr.Theme theme;
  final TileProviders providers;

  @override
  void dispose() {
    for (final archive in archives) {
      unawaited(archive.close().catchError((Object _) {}));
    }
  }
}

class _MapsforgeResources implements _OfflineBaseMapResources {
  _MapsforgeResources(this.tileRenderer)
      : tileProvider = MapsforgeTileProvider(tileRenderer);

  final MapsforgeTileRenderer tileRenderer;
  final MapsforgeTileProvider tileProvider;

  @override
  void dispose() {
    // MapsforgeTileRenderer.dispose drops its cached tiles, then disposes the
    // DatastoreRenderer beneath — which tears down the render theme and the
    // MultimapDatastore (which disposes every Mapfile).
    tileRenderer.dispose();
  }
}

Future<_OfflineBaseMapResources> _loadResources(OfflineBaseMap baseMap) =>
    switch (baseMap.format) {
      OfflineMapPackFormat.pmtiles => _loadPmtiles(baseMap.packPaths),
      OfflineMapPackFormat.mapsforge => _loadMapsforge(baseMap.packPaths),
    };

/// Kotlin `Context.offlineMapStyleJson` + MapLibre `Style.Builder().fromJson`.
Future<_OfflineBaseMapResources> _loadPmtiles(List<String> packPaths) async {
  final styleText = await rootBundle.loadString(offlineMapStyleAsset);
  final style = expandPmtilesStyle(
    jsonDecode(styleText) as Map<String, dynamic>,
    packPaths,
  );
  final theme = vtr.ThemeReader().read(style);
  final sourceIds = pmtilesSourceIds(packPaths.length);
  final archives = <pmt.PmTilesArchive>[];
  try {
    final providers = <String, VectorTileProvider>{};
    for (var index = 0; index < packPaths.length; index++) {
      final archive = await pmt.PmTilesArchive.fromFile(File(packPaths[index]));
      archives.add(archive);
      providers[sourceIds[index]] = PmtilesVectorTileProvider(archive);
    }
    return _PmtilesResources(archives, theme, TileProviders(providers));
  } catch (error) {
    for (final archive in archives) {
      unawaited(archive.close().catchError((Object _) {}));
    }
    rethrow;
  }
}

/// Kotlin `createMapsforgeMapView`: MultiMapDataStore over every active pack +
/// the stock default render theme, wrapped in the shared
/// [MapsforgeTileRenderer] that warms, throttles and caches the tile path.
Future<_OfflineBaseMapResources> _loadMapsforge(List<String> packPaths) async {
  // The theme's `jar:symbols/...` / `jar:patterns/...` resources resolve
  // against the app bundle where the mapsforge-themes jar contents are
  // mirrored (assets/mapsforge/), replacing the loader's default
  // package-relative prefix.
  SymbolCacheMgr().addLoader(
    'jar:',
    ImageBundleLoader(bundle: rootBundle, pathPrefix: 'assets/mapsforge/'),
  );
  // RETURN_ALL, not DEDUPLICATE and NOT RETURN_FIRST.
  //
  // Deduplication is O(ways² × coordinates) per tile: MultimapDatastore
  // accumulates every pack's bundle through DatastoreBundle.addDeduplicate,
  // which is a linear `ways.contains(way)` scan whose Way.== compares every
  // coordinate of both ways (the package's own comment on it reads "note:
  // listEquals() is very expensive"). It costs that even when a single pack
  // answers, because the accumulator grows as it scans.
  //
  // RETURN_FIRST is not the answer either, and shipping it left a blank wedge
  // across the seam between two packs. It returns the FIRST datastore whose
  // bounding box intersects the tile and whose `supportsTile` is true — but
  // `Mapfile.supportsTile` only checks the zoom range and that same bounding
  // box, never whether the pack holds anything there. A pack's box is a
  // RECTANGLE around a region-shaped extract, so two adjacent regions have
  // overlapping boxes: in the overlap the first pack claims the tile, returns
  // its empty bundle, and the pack that actually has the data is never asked.
  //
  // RETURN_ALL reads every intersecting pack (concurrently, via Future.wait)
  // and merges with `addDeduplicate(result, false)` — a plain addAll. That is
  // the merge, at O(n), and it keeps the whole win over DEDUPLICATE, because
  // the quadratic part was the dedup flag and never the accumulation. Where two
  // packs genuinely hold the same feature it is drawn twice, which is invisible
  // at a seam and far cheaper than comparing every coordinate to find out.
  final datastore = MultimapDatastore(DataPolicy.RETURN_ALL);
  try {
    final mapfiles = <Mapfile>[];
    for (final path in packPaths) {
      final mapfile = await Mapfile.createFromFile(filename: path);
      mapfiles.add(mapfile);
      await datastore.addDatastore(mapfile);
    }
    // addDatastore's bounding-box read opened RandomAccessFiles on this
    // isolate. The renderer's reader isolate below gets the whole datastore
    // SENT to it, and open file handles are unsendable — drop them here (the
    // isolate's copy lazily reopens its own).
    for (final mapfile in mapfiles) {
      mapfile.readBufferSource.freeRessources();
    }
    final themeXml = await rootBundle.loadString('assets/mapsforge/default.xml');
    // Labels render onto the tiles like Kotlin's TileRendererLayer (the map
    // never rotates, so a separate label layer buys nothing).
    //
    // useIsolateReader keeps block parsing AND rendertheme matching off the
    // UI thread: with them inline, a dense tile burst holds the main thread
    // past the 5s input-dispatch deadline and Android ANR-kills the app.
    final tileRenderer = MapsforgeTileRenderer(
      DatastoreRenderer(
        datastore,
        RenderThemeBuilder.createFromString(themeXml),
        useSeparateLabelLayer: false,
        useIsolateReader: true,
      ),
      warmUpTile: await _warmUpTile(datastore),
    );
    // Spawn the reader isolate now, while exactly one caller can ask for it.
    // Left to the first tile burst, the renderer's own lazy `??= await` spawns
    // one isolate per concurrent tile (~600ms each) and orphans all but one.
    await tileRenderer.warmUp();
    return _MapsforgeResources(tileRenderer);
  } catch (error) {
    datastore.dispose();
    rethrow;
  }
}

/// A tile at the centre of the active packs' combined coverage, used only to
/// force the renderer's reader isolate into existence.
///
/// z16 rather than a low zoom on purpose: a z16 tile spans a few hundred metres
/// and reads almost nothing, where a z5 tile would drag in a country's worth of
/// ways just to warm a pointer. Which tile it is barely matters — `executeJob`
/// assigns its reader before it reads — so an empty one does the job too.
///
/// The bounding box is the union `addDatastore` already cached, so this costs no
/// file access and is safe after the handles were freed above.
Future<mapsforge.Tile> _warmUpTile(MultimapDatastore datastore) async {
  const zoomLevel = 16;
  final box = await datastore.getBoundingBox();
  final projection = mapsforge.MercatorProjection.fromZoomlevel(zoomLevel);
  return mapsforge.Tile(
    projection.longitudeToTileX((box.minLongitude + box.maxLongitude) / 2),
    projection.latitudeToTileY((box.minLatitude + box.maxLatitude) / 2),
    zoomLevel,
    0,
  );
}
