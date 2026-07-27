import 'dart:ui' show Offset, Size;

import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/features/activity/maps/mapsforge_label_geometry.dart';

/// The arithmetic that puts a label on the same pixel as the feature it names.
///
/// Labels moved out of the tiles (where a wide one was clipped by the tile's own
/// canvas) onto a layer above them, which means their position is no longer
/// implied by the tile they were drawn into — it has to be computed. Every case
/// here is one way that computation can silently drift a label off its feature.
void main() {
  group('visibleLabelTileRange', () {
    test('a viewport sitting exactly on one tile reads that tile plus a ring',
        () {
      // Tile (10, 20) at z14 starts at pixel (2560, 5120).
      final range = visibleLabelTileRange(
        pixelOrigin: const Offset(2560, 5120),
        viewportSize: const Size(256, 256),
        zoom: 14,
      );

      expect(range.zoom, 14);
      // The ring is not padding for its own sake: a label anchored just off
      // screen still reaches into view, and without it labels pop in at the
      // edges as you pan.
      expect(range.minX, 9);
      expect(range.maxX, 11);
      expect(range.minY, 19);
      expect(range.maxY, 21);
    });

    test('a viewport spanning several tiles covers all of them', () {
      final range = visibleLabelTileRange(
        pixelOrigin: const Offset(2560, 5120),
        viewportSize: const Size(800, 600),
        zoom: 14,
        margin: 0,
      );

      expect(range.minX, 10);
      expect(range.maxX, 13); // 2560..3360 spans tiles 10-13
      expect(range.minY, 20);
      expect(range.maxY, 22); // 5120..5720 spans tiles 20-22
    });

    test('a fractional zoom reads the tiles of the rounded zoom level', () {
      // Mapsforge has no fractional zoom: it reads integer levels, and the
      // difference is taken up by scaling the drawn result.
      final range = visibleLabelTileRange(
        pixelOrigin: const Offset(5120, 10240),
        viewportSize: const Size(256, 256),
        zoom: 14.4,
        margin: 0,
      );

      expect(range.zoom, 14, reason: '14.4 rounds down to 14');
      // At z14 the same screen covers 1/2^0.4 as many map pixels.
      expect(range.minX, 15);
      expect(range.minY, 30);
    });

    test('a viewport at the edge of the world clamps to real tiles', () {
      final range = visibleLabelTileRange(
        pixelOrigin: Offset.zero,
        viewportSize: const Size(256, 256),
        zoom: 2,
      );

      // z2 has tiles 0..3; the margin must not ask for -1 or 4.
      expect(range.minX, 0);
      expect(range.minY, 0);
      expect(range.maxX, lessThanOrEqualTo(3));
      expect(range.maxY, lessThanOrEqualTo(3));
    });

    test('ranges compare by value, so panning within one tile reads once', () {
      // What stops the layer from issuing a read per frame of a pan.
      const a = LabelTileRange(zoom: 14, minX: 1, minY: 2, maxX: 3, maxY: 4);
      const b = LabelTileRange(zoom: 14, minX: 1, minY: 2, maxX: 3, maxY: 4);

      expect(a, b);
      expect(a.hashCode, b.hashCode);
      expect(a, isNot(const LabelTileRange(
        zoom: 15,
        minX: 1,
        minY: 2,
        maxX: 3,
        maxY: 4,
      )));
    });
  });

  group('labelPaintTransform', () {
    test('the reference is the map pixel at the centre of the screen, not its '
        'corner', () {
      // The bug this replaces: mapsforge measures `absolute - reference` from
      // the MIDDLE of the canvas (its own view translates by half the screen
      // before painting), so a top-left reference displaced every label by half
      // the viewport.
      final transform = labelPaintTransform(
        pixelOrigin: const Offset(2560, 5120),
        viewportSize: const Size(400, 240),
        zoom: 14,
      );

      expect(transform.scale, 1.0);
      expect(transform.reference, const Offset(2560 + 200, 5120 + 120));
    });

    test('half a zoom level in, the labels scale with the tiles under them',
        () {
      final transform = labelPaintTransform(
        pixelOrigin: const Offset(4096, 4096),
        viewportSize: const Size(400, 240),
        zoom: 14.5,
      );

      // 14.5 rounds to 15, so the labels are read one level in and drawn
      // slightly smaller: 2^(14.5-15).
      expect(transform.scale, closeTo(0.7071, 0.0001));
      expect(transform.reference.dx, closeTo((4096 + 200) / 0.7071, 0.1));
    });

    // The painter translates the canvas to the centre, then scales, then draws
    // at `absolute - reference`. These reproduce that whole chain: get any part
    // of it wrong and labels drift off the features they name.
    Offset screenOf(Offset absolute, LabelPaintTransform t, Size viewport) =>
        (absolute - t.reference) * t.scale +
        Offset(viewport.width / 2, viewport.height / 2);

    test('the map pixel under the top-left corner is drawn at the top-left '
        'corner', () {
      const pixelOrigin = Offset(2560, 5120);
      const viewport = Size(400, 240);
      final transform = labelPaintTransform(
        pixelOrigin: pixelOrigin,
        viewportSize: viewport,
        zoom: 14.3,
      );

      final screen = screenOf(pixelOrigin / transform.scale, transform, viewport);

      expect(screen.dx, closeTo(0, 0.0001));
      expect(screen.dy, closeTo(0, 0.0001));
    });

    test('the map pixel under the bottom-right corner is drawn at the '
        'bottom-right corner', () {
      const pixelOrigin = Offset(2560, 5120);
      const viewport = Size(400, 240);
      final transform = labelPaintTransform(
        pixelOrigin: pixelOrigin,
        viewportSize: viewport,
        zoom: 14.3,
      );

      final corner = Offset(
        (pixelOrigin.dx + viewport.width) / transform.scale,
        (pixelOrigin.dy + viewport.height) / transform.scale,
      );
      final screen = screenOf(corner, transform, viewport);

      expect(screen.dx, closeTo(viewport.width, 0.0001));
      expect(screen.dy, closeTo(viewport.height, 0.0001));
    });

    test('panning moves a label by exactly what the map moved under it', () {
      // "Not fixed onto the map" in test form: shift the camera by 50px and the
      // same feature must land 50px to the left, no more and no less.
      const viewport = Size(400, 240);
      const before = Offset(2560, 5120);
      const after = Offset(2610, 5120);
      final t1 = labelPaintTransform(
          pixelOrigin: before, viewportSize: viewport, zoom: 14);
      final t2 = labelPaintTransform(
          pixelOrigin: after, viewportSize: viewport, zoom: 14);
      const feature = Offset(2700, 5200);

      final moved = screenOf(feature, t1, viewport).dx -
          screenOf(feature, t2, viewport).dx;

      expect(moved, closeTo(50, 0.0001));
    });
  });
}
