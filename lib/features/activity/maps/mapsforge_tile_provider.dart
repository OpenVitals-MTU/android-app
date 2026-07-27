import 'dart:async';
import 'dart:ui' as ui;

import 'package:flutter/foundation.dart';
import 'package:flutter/painting.dart';
import 'package:flutter_map/flutter_map.dart';

import 'mapsforge_tile_renderer.dart';

/// Bridges the pure-Dart Mapsforge renderer into flutter_map, replacing the
/// Kotlin app's `TileRendererLayer`: every tile requested by the [TileLayer] is
/// drawn on demand from the imported `.map` packs (combined in a
/// `MultimapDatastore(DataPolicy.RETURN_ALL)`, the Kotlin `MultiMapDataStore`
/// with its cheapest MERGING policy — see the note there on why the cheaper
/// RETURN_FIRST leaves a blank wedge at a pack seam).
///
/// The rendering itself, its warm-up, its concurrency cap and its cache all
/// belong to the shared [MapsforgeTileRenderer]; this class is only the adapter
/// onto flutter_map's [ImageProvider] contract.
///
/// Tiles outside the datastore's coverage resolve to a transparent image so
/// flutter_map shows the plain background there — matching the Android
/// mapsforge view, which simply leaves uncovered tiles empty.
class MapsforgeTileProvider extends TileProvider {
  MapsforgeTileProvider(this.tileRenderer);

  /// The shared renderer over the active Mapsforge packs. NOT owned here — see
  /// [dispose].
  final MapsforgeTileRenderer tileRenderer;

  @override
  ImageProvider getImage(TileCoordinates coordinates, TileLayer options) =>
      _MapsforgeTileImage(
        tileRenderer: tileRenderer,
        z: coordinates.z,
        x: coordinates.x,
        y: coordinates.y,
      );

  /// Deliberately empty, and load-bearing.
  ///
  /// flutter_map calls `tileProvider.dispose()` from `_TileLayerState.dispose`,
  /// i.e. every time the route card leaves the tree — but the renderer, its
  /// reader isolate and its tile cache are app-lifetime, owned by the provider
  /// that built them. Letting this fall through to a disposal would throw all of
  /// that away every time the user scrolls the card out of view.
  @override
  void dispose() {}
}

/// An [ImageProvider] producing one rendered Mapsforge tile.
class _MapsforgeTileImage extends ImageProvider<_MapsforgeTileImage> {
  const _MapsforgeTileImage({
    required this.tileRenderer,
    required this.z,
    required this.x,
    required this.y,
  });

  final MapsforgeTileRenderer tileRenderer;
  final int z;
  final int x;
  final int y;

  /// The stand-in for a tile the packs do not cover, shared across every such
  /// tile: a route near a pack's edge would otherwise allocate one 1×1 image per
  /// uncovered tile, over and over. Handed out as clones, like a rendered tile.
  static Future<ui.Image>? _transparentMaster;

  @override
  Future<_MapsforgeTileImage> obtainKey(ImageConfiguration configuration) =>
      SynchronousFuture<_MapsforgeTileImage>(this);

  @override
  ImageStreamCompleter loadImage(
    _MapsforgeTileImage key,
    ImageDecoderCallback decode,
  ) =>
      OneFrameImageStreamCompleter(
        _render(),
        informationCollector: () => [
          DiagnosticsProperty('Mapsforge tile', 'z=$z x=$x y=$y'),
        ],
      );

  Future<ImageInfo> _render() async {
    // The renderer hands back an image this call owns; ImageInfo.dispose (run by
    // Flutter's ImageCache) releases it.
    final tile = await tileRenderer.tile(z, x, y);
    if (tile != null) return ImageInfo(image: tile);
    // No data for this tile (outside pack coverage) or a render error: show
    // nothing, like the Android mapsforge view.
    final master = _transparentMaster ??= _createTransparentTile();
    return ImageInfo(image: (await master).clone());
  }

  static Future<ui.Image> _createTransparentTile() {
    final recorder = ui.PictureRecorder();
    ui.Canvas(recorder);
    final picture = recorder.endRecording();
    final image = picture.toImage(1, 1);
    picture.dispose();
    return image;
  }

  @override
  bool operator ==(Object other) =>
      other is _MapsforgeTileImage &&
      other.tileRenderer == tileRenderer &&
      other.z == z &&
      other.x == x &&
      other.y == y;

  @override
  int get hashCode => Object.hash(tileRenderer, z, x, y);
}
