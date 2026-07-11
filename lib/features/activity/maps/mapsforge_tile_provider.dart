import 'dart:async';
import 'dart:ui' as ui;

import 'package:flutter/foundation.dart';
import 'package:flutter/painting.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:mapsforge_flutter_core/model.dart' as mapsforge;
import 'package:mapsforge_flutter_renderer/offline_renderer.dart';

/// Bridges the pure-Dart Mapsforge renderer into flutter_map, replacing the
/// Kotlin app's `TileRendererLayer`: every tile requested by the [TileLayer]
/// is rendered on demand by a [DatastoreRenderer] over the imported `.map`
/// packs (combined in a `MultimapDatastore(DataPolicy.DEDUPLICATE)`, exactly
/// like the Kotlin `MultiMapDataStore`).
///
/// Tiles outside the datastore's coverage resolve to a transparent image so
/// flutter_map shows the plain background there — matching the Android
/// mapsforge view, which simply leaves uncovered tiles empty.
class MapsforgeTileProvider extends TileProvider {
  MapsforgeTileProvider(this.renderer);

  /// Renderer over the active Mapsforge packs. Owned by the widget that
  /// created it (disposal there tears down the datastore + render theme).
  final DatastoreRenderer renderer;

  @override
  ImageProvider getImage(TileCoordinates coordinates, TileLayer options) =>
      _MapsforgeTileImage(
        renderer: renderer,
        z: coordinates.z,
        x: coordinates.x,
        y: coordinates.y,
      );
}

/// An [ImageProvider] producing one rendered Mapsforge tile.
class _MapsforgeTileImage extends ImageProvider<_MapsforgeTileImage> {
  const _MapsforgeTileImage({
    required this.renderer,
    required this.z,
    required this.x,
    required this.y,
  });

  final DatastoreRenderer renderer;
  final int z;
  final int x;
  final int y;

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
    final job = JobRequest(mapsforge.Tile(x, y, z, 0));
    final JobResult result = await renderer.executeJob(job);
    final picture = result.picture;
    if (result.result != JOBRESULT.NORMAL || picture == null) {
      // No data for this tile (outside pack coverage) or a render error:
      // show nothing, like the Android mapsforge view.
      return ImageInfo(image: await _transparentTile());
    }
    // `convertPictureToImage` returns either the picture's own image (which
    // the TilePicture would dispose) or a freshly rasterized one (which it
    // would not), so clone for the ImageInfo and release both originals here.
    final ui.Image image = await picture.convertPictureToImage();
    final ui.Image tile = image.clone();
    image.dispose();
    picture.getPicture()?.dispose();
    return ImageInfo(image: tile);
  }

  static Future<ui.Image> _transparentTile() {
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
      other.renderer == renderer &&
      other.z == z &&
      other.x == x &&
      other.y == y;

  @override
  int get hashCode => Object.hash(renderer, z, x, y);
}
