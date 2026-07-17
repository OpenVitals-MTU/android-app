import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/ui/charts/chart_decimation.dart';

void main() {
  group('decimateOffsets', () {
    test('returns the same list unchanged when already at or below target', () {
      final points = [
        for (var i = 0; i < 50; i++) Offset(i.toDouble(), i.toDouble()),
      ];
      expect(identical(decimateOffsets(points, 50), points), isTrue);
      expect(identical(decimateOffsets(points, 100), points), isTrue);
    });

    test('does not downsample when target is degenerate', () {
      final points = [
        for (var i = 0; i < 50; i++) Offset(i.toDouble(), i.toDouble()),
      ];
      expect(identical(decimateOffsets(points, 2), points), isTrue);
    });

    test('reduces to exactly the target count', () {
      final points = [
        for (var i = 0; i < 5000; i++) Offset(i.toDouble(), (i % 7).toDouble()),
      ];
      expect(decimateOffsets(points, 500).length, 500);
    });

    test('keeps the first and last point', () {
      final points = [
        for (var i = 0; i < 1000; i++) Offset(i.toDouble(), (i % 13).toDouble()),
      ];
      final result = decimateOffsets(points, 100);
      expect(result.first, points.first);
      expect(result.last, points.last);
    });

    test('preserves an isolated peak (LTTB keeps extremes)', () {
      // A flat line with one tall spike in the middle.
      final points = <Offset>[
        for (var i = 0; i < 1000; i++) Offset(i.toDouble(), 0),
      ];
      const spikeIndex = 500;
      points[spikeIndex] = Offset(spikeIndex.toDouble(), 1000);

      final result = decimateOffsets(points, 50);
      final maxY = result.map((o) => o.dy).reduce((a, b) => a > b ? a : b);
      expect(maxY, 1000, reason: 'the spike must survive downsampling');
    });
  });
}
