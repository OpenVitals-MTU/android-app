import 'dart:math' as math;
import 'dart:ui' show Offset, Size;

import 'package:meta/meta.dart';

/// The pure map-pixel arithmetic the label layer needs, kept out of the widget
/// so it can be tested without a camera or a canvas.
///
/// Two coordinate spaces meet here:
///
/// * **flutter_map** works in its CRS pixel space at the camera's *fractional*
///   zoom, where the world is `256 * 2^zoom` pixels across. `pixelOrigin` is the
///   top-left of the viewport in that space.
/// * **mapsforge** works at an *integer* zoom, where the world is
///   `tileSize * 2^z` pixels across, and draws each element at
///   `absolute - reference`.
///
/// With mapsforge's tile size left at flutter_map's own 256, the two spaces
/// differ only by the zoom fraction, so one scale factor reconciles them.

/// A rectangle of tile indices, inclusive on both ends.
@immutable
class LabelTileRange {
  const LabelTileRange({
    required this.zoom,
    required this.minX,
    required this.minY,
    required this.maxX,
    required this.maxY,
  });

  final int zoom;
  final int minX;
  final int minY;
  final int maxX;
  final int maxY;

  @override
  bool operator ==(Object other) =>
      other is LabelTileRange &&
      other.zoom == zoom &&
      other.minX == minX &&
      other.minY == minY &&
      other.maxX == maxX &&
      other.maxY == maxY;

  @override
  int get hashCode => Object.hash(zoom, minX, minY, maxX, maxY);

  @override
  String toString() => 'z$zoom x$minX..$maxX y$minY..$maxY';
}

/// How the label picture maps onto the screen for one camera position.
@immutable
class LabelPaintTransform {
  const LabelPaintTransform({required this.scale, required this.reference});

  /// Screen pixels per mapsforge pixel at the integer zoom: `2^(zoom - z)`.
  /// 1.0 when the camera sits exactly on a zoom level.
  final double scale;

  /// The mapsforge coordinate of the screen's CENTRE.
  ///
  /// Centre, not top-left: the renderer draws each element at
  /// `absolute - reference` and mapsforge's own view translates the canvas by
  /// half the viewport before painting, so that difference is measured from the
  /// middle of the canvas. Passing the top-left here displaces every label by
  /// half the viewport — which is exactly what it looked like.
  final Offset reference;
}

/// The tiles covering the viewport at [zoom]'s integer level, widened by
/// [margin] tiles.
///
/// The margin is what actually fixes the reported bug's cousin: a label anchored
/// just off-screen still has text reaching into view, so the range has to extend
/// past the viewport or labels would pop in at the edges as you pan.
LabelTileRange visibleLabelTileRange({
  required Offset pixelOrigin,
  required Size viewportSize,
  required double zoom,
  double tileSize = 256,
  int margin = 1,
}) {
  final z = zoom.round();
  final scale = math.pow(2.0, zoom - z).toDouble();
  // Screen corners, back in mapsforge pixels at the integer zoom.
  final left = pixelOrigin.dx / scale;
  final top = pixelOrigin.dy / scale;
  final right = (pixelOrigin.dx + viewportSize.width) / scale;
  final bottom = (pixelOrigin.dy + viewportSize.height) / scale;

  final lastTile = (1 << z) - 1;
  int clamp(int value) => value < 0 ? 0 : (value > lastTile ? lastTile : value);

  // `ceil() - 1` for the far edge, not `floor()`: a tile spans a half-open
  // pixel range, so a viewport ending exactly on a boundary stops at the tile
  // before it and must not pull in the next column.
  int lastTileIndex(double edge) => (edge / tileSize).ceil() - 1;

  return LabelTileRange(
    zoom: z,
    minX: clamp((left / tileSize).floor() - margin),
    minY: clamp((top / tileSize).floor() - margin),
    maxX: clamp(lastTileIndex(right) + margin),
    maxY: clamp(lastTileIndex(bottom) + margin),
  );
}

/// The scale and reference for painting labels under [zoom] on a canvas of
/// [viewportSize], whose origin the painter has translated to the centre.
///
/// With the canvas translated by `viewportSize / 2` and then scaled by [scale],
/// an element drawn at `absolute - reference` lands at
/// `absolute * scale - pixelOrigin` on screen — exactly where flutter_map draws
/// the tile underneath it.
LabelPaintTransform labelPaintTransform({
  required Offset pixelOrigin,
  required Size viewportSize,
  required double zoom,
}) {
  final scale = math.pow(2.0, zoom - zoom.round()).toDouble();
  final centre = pixelOrigin +
      Offset(viewportSize.width / 2, viewportSize.height / 2);
  return LabelPaintTransform(scale: scale, reference: centre / scale);
}
