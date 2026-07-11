import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:flutter/services.dart' show rootBundle;
import 'package:flutter/widgets.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:mapsforge_flutter_mapfile/mapfile.dart';
import 'package:mapsforge_flutter_renderer/cache.dart';
import 'package:mapsforge_flutter_renderer/offline_renderer.dart';
import 'package:mapsforge_flutter_rendertheme/rendertheme.dart';
import 'package:pmtiles/pmtiles.dart' as pmt;
import 'package:vector_map_tiles/vector_map_tiles.dart';
import 'package:vector_tile_renderer/vector_tile_renderer.dart' as vtr;

import 'mapsforge_tile_provider.dart';
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
///   `MultimapDatastore(DataPolicy.DEDUPLICATE)` over all active packs with
///   the stock `default.xml` render theme — Kotlin's `TileRendererLayer` +
///   `MapsforgeThemes.DEFAULT`;
/// * no active pack, resources still loading, or a pack that fails to open →
///   nothing, leaving the plain route canvas exactly like Kotlin's
///   `RoutePreview` fallback.
class OfflineBaseMapLayer extends ConsumerStatefulWidget {
  const OfflineBaseMapLayer({super.key});

  @override
  ConsumerState<OfflineBaseMapLayer> createState() =>
      _OfflineBaseMapLayerState();
}

class _OfflineBaseMapLayerState extends ConsumerState<OfflineBaseMapLayer> {
  OfflineBaseMap? _config;
  _OfflineBaseMapResources? _resources;
  int _loadGeneration = 0;

  @override
  void dispose() {
    _loadGeneration += 1;
    _resources?.dispose();
    _resources = null;
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final baseMap = ref.watch(offlineBaseMapProvider);
    if (baseMap != _config) {
      _config = baseMap;
      _reload(baseMap);
    }
    return switch (_resources) {
      final _PmtilesResources resources => VectorTileLayer(
          tileProviders: resources.providers,
          theme: resources.theme,
          tileOffset: TileOffset.DEFAULT,
        ),
      final _MapsforgeResources resources => TileLayer(
          tileProvider: resources.tileProvider,
          userAgentPackageName: 'tech.mmarca.openvitals',
        ),
      null => const SizedBox.shrink(),
    };
  }

  void _reload(OfflineBaseMap? baseMap) {
    final generation = ++_loadGeneration;
    _resources?.dispose();
    _resources = null;
    if (baseMap == null) return;
    unawaited(() async {
      final _OfflineBaseMapResources resources;
      try {
        resources = await _loadResources(baseMap);
      } catch (error) {
        // Kotlin's Mapsforge path falls back to the plain RoutePreview when a
        // pack fails to open; a broken pack must not take the screen down.
        debugPrint('Offline base map failed to load: $error');
        return;
      }
      if (!mounted || generation != _loadGeneration) {
        resources.dispose();
        return;
      }
      setState(() => _resources = resources);
    }());
  }
}

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
  _MapsforgeResources(this.renderer) : tileProvider = MapsforgeTileProvider(renderer);

  final DatastoreRenderer renderer;
  final MapsforgeTileProvider tileProvider;

  @override
  void dispose() {
    // DatastoreRenderer.dispose tears down the render theme and the
    // MultimapDatastore (which disposes every Mapfile).
    renderer.dispose();
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

/// Kotlin `createMapsforgeMapView`: MultiMapDataStore(DEDUPLICATE) over every
/// active pack + the stock default render theme.
Future<_OfflineBaseMapResources> _loadMapsforge(List<String> packPaths) async {
  // The theme's `jar:symbols/...` / `jar:patterns/...` resources resolve
  // against the app bundle where the mapsforge-themes jar contents are
  // mirrored (assets/mapsforge/), replacing the loader's default
  // package-relative prefix.
  SymbolCacheMgr().addLoader(
    'jar:',
    ImageBundleLoader(bundle: rootBundle, pathPrefix: 'assets/mapsforge/'),
  );
  final datastore = MultimapDatastore(DataPolicy.DEDUPLICATE);
  try {
    for (final path in packPaths) {
      await datastore.addDatastore(await Mapfile.createFromFile(filename: path));
    }
    final themeXml = await rootBundle.loadString('assets/mapsforge/default.xml');
    // Labels render onto the tiles like Kotlin's TileRendererLayer (the map
    // never rotates, so a separate label layer buys nothing).
    return _MapsforgeResources(DatastoreRenderer(
      datastore,
      RenderThemeBuilder.createFromString(themeXml),
      useSeparateLabelLayer: false,
    ));
  } catch (error) {
    datastore.dispose();
    rethrow;
  }
}
