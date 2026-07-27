import 'dart:async';

import 'package:flutter/widgets.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:mapsforge_flutter_core/model.dart' as mapsforge;
import 'package:mapsforge_flutter_core/projection.dart' as mapsforge;
import 'package:mapsforge_flutter_renderer/ui.dart';
import 'package:mapsforge_flutter_rendertheme/model.dart';

import 'mapsforge_label_geometry.dart';
import 'mapsforge_tile_renderer.dart';

/// Draws the map's labels above the raster tiles, instead of inside them.
///
/// ## Why this layer exists
///
/// Mapsforge can burn labels into each tile it rasterises, and that is what this
/// app used to do. A label wider than the tile it is anchored in is then clipped
/// by that tile's 256×256 canvas — the "Zwingenberg" cut in half at a seam.
///
/// The package compensates with [TileDependencies]: when a tile renders, any
/// label overflowing into a neighbour is registered so the neighbour draws it
/// too. That only works if the neighbour renders *second*. flutter_map requests
/// tiles in whatever order it likes and several render at once, so it is a coin
/// flip — and once a tile is marked drawn it is excluded from receiving overflow
/// for the renderer's whole life, so a re-render after a cache eviction gets
/// none at all.
///
/// Drawing labels here removes the problem rather than compensating for it.
/// There are no tile edges on this canvas, so nothing can be clipped by one, and
/// the whole visible set is collision-resolved in a single pass, so two
/// neighbours cannot disagree about which of two overlapping labels wins.
///
/// ## Placement
///
/// Must be a child of [FlutterMap], after the tile layer — it reads
/// [MapCamera] from context and paints over whatever is beneath it.
class MapsforgeLabelLayer extends StatefulWidget {
  const MapsforgeLabelLayer({super.key, required this.tileRenderer});

  final MapsforgeTileRenderer tileRenderer;

  @override
  State<MapsforgeLabelLayer> createState() => _MapsforgeLabelLayerState();
}

class _MapsforgeLabelLayerState extends State<MapsforgeLabelLayer> {
  /// The range the labels currently on screen were read for.
  LabelTileRange? _loadedRange;

  /// The range a read is in flight for, so a pan does not queue one per frame.
  LabelTileRange? _pendingRange;

  RenderInfoCollection? _labels;

  @override
  Widget build(BuildContext context) {
    final camera = MapCamera.of(context);
    final range = visibleLabelTileRange(
      pixelOrigin: camera.pixelOrigin,
      viewportSize: camera.size,
      zoom: camera.zoom,
    );
    // Reading is driven from build because that is what a camera change
    // rebuilds; the guard below is what keeps it to one read per range rather
    // than one per frame of a pan.
    if (range != _loadedRange && range != _pendingRange) {
      unawaited(_loadLabels(range));
    }

    final labels = _labels;
    if (labels == null || labels.renderInfos.isEmpty) {
      return const SizedBox.shrink();
    }
    // MobileLayerTransformer is what every flutter_map layer wraps itself in:
    // it pins the layer to exactly the camera's size through an OverflowBox and
    // applies the map rotation. Without it the painter's size and origin are
    // whatever the loose parent Stack gives it, and every label coordinate is
    // measured from the wrong box.
    return MobileLayerTransformer(
      child: CustomPaint(
        painter: _MapsforgeLabelPainter(
          labels: labels,
          zoom: camera.zoom,
          pixelOrigin: camera.pixelOrigin,
          labelZoom: _loadedRange?.zoom ?? camera.zoom.round(),
        ),
        size: camera.size,
      ),
    );
  }

  Future<void> _loadLabels(LabelTileRange range) async {
    _pendingRange = range;
    final labels = await widget.tileRenderer.labels(
      mapsforge.Tile(range.minX, range.minY, range.zoom, 0),
      mapsforge.Tile(range.maxX, range.maxY, range.zoom, 0),
    );
    if (!mounted || _pendingRange != range) return;
    setState(() {
      _labels = labels;
      _loadedRange = range;
      _pendingRange = null;
    });
  }
}

/// Paints one already-collision-resolved label set for the current camera.
class _MapsforgeLabelPainter extends CustomPainter {
  const _MapsforgeLabelPainter({
    required this.labels,
    required this.zoom,
    required this.pixelOrigin,
    required this.labelZoom,
  });

  final RenderInfoCollection labels;
  final double zoom;
  final Offset pixelOrigin;

  /// The integer zoom the labels were read at. Usually `zoom.round()`, but a
  /// zoom change is drawn with the previous set until the new one lands, and
  /// scaling to THAT zoom is what keeps those labels on their features instead
  /// of sliding while the read is in flight.
  final int labelZoom;

  @override
  void paint(Canvas canvas, Size size) {
    // A label that cannot be drawn costs a label. An exception escaping a
    // painter costs the whole subtree beneath it — the map. The file says as
    // much about the reader; the painter has to honour it too.
    try {
      _paint(canvas, size);
    } catch (error, stack) {
      debugPrint('Mapsforge label paint failed: $error\n$stack');
    }
  }

  void _paint(Canvas canvas, Size size) {
    final transform = labelPaintTransform(
      pixelOrigin: pixelOrigin,
      viewportSize: size,
      zoom: zoom - (zoom.round() - labelZoom),
    );
    canvas.save();
    // Origin to the centre BEFORE scaling, because mapsforge measures
    // `absolute - reference` from the middle of the canvas — its own view does
    // this with a Transform.translate of half the screen.
    canvas.translate(size.width / 2, size.height / 2);
    canvas.scale(transform.scale);
    // The screen size, as mapsforge's own LabelPainter passes it. The canvas
    // origin is now central, so the renderer's off-screen rejection (which
    // assumes 0..width) under-rejects rather than over-rejects: it may draw
    // something just out of view, never drop something in view.
    final uiCanvas = UiCanvas(canvas, size);
    final context = UiRenderContext(
      canvas: uiCanvas,
      reference: mapsforge.Mappoint(
        transform.reference.dx,
        transform.reference.dy,
      ),
      projection: mapsforge.PixelProjection(labelZoom),
    );
    for (final renderInfo in labels.renderInfos) {
      renderInfo.render(context);
    }
    canvas.restore();
  }

  @override
  bool shouldRepaint(covariant _MapsforgeLabelPainter oldDelegate) =>
      oldDelegate.labels != labels ||
      oldDelegate.zoom != zoom ||
      oldDelegate.pixelOrigin != pixelOrigin ||
      oldDelegate.labelZoom != labelZoom;
}
