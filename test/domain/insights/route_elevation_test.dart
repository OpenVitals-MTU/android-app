import 'dart:math';

import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/domain/insights/route_elevation.dart';

/// A climb of [trueGain] metres spread evenly over [samples], with Gaussian
/// vertical error of [sigma] applied to every sample.
///
/// sigma = 3 m is representative of real GPS vertical accuracy, which is roughly
/// 1.5x the horizontal figure a phone reports.
List<double?> _noisyClimb({
  required double trueGain,
  required int samples,
  double sigma = 3.0,
  int seed = 11,
}) {
  final random = Random(seed);
  return [
    for (var i = 0; i < samples; i++)
      trueGain * (i / max(samples - 1, 1)) + _gaussian(random) * sigma,
  ];
}

double _gaussian(Random random) {
  // Box-Muller: dart:math has no normal distribution.
  final u1 = 1.0 - random.nextDouble();
  final u2 = 1.0 - random.nextDouble();
  return sqrt(-2.0 * log(u1)) * cos(2 * pi * u2);
}

void main() {
  group('elevationGainFromAltitudes', () {
    // The bug this exists to prevent (issue #242): summing raw per-point rises
    // turned a real 750 m climb into ~15 km and 300 m into ~7 km on the review
    // screen. GPS vertical noise is larger than any sane per-step threshold, so
    // it was banked thousands of times over a long ride.
    test('a flat route reports essentially no climb', () {
      final flat = _noisyClimb(trueGain: 0, samples: 3600);
      // A naive sum over this same data yields ~6000 m.
      expect(elevationGainFromAltitudes(flat), lessThan(60));
    });

    test('a real climb is reported accurately, not inflated', () {
      final oneHour = _noisyClimb(trueGain: 300, samples: 3600);
      final twoHours = _noisyClimb(trueGain: 750, samples: 7200);

      expect(elevationGainFromAltitudes(oneHour), closeTo(300, 60));
      expect(elevationGainFromAltitudes(twoHours), closeTo(750, 100));
    });

    test('accuracy does not decay with route length', () {
      // The old accumulator's error grew with sample count, which is why a
      // longer ride was proportionally more wrong. Same climb, 4x the samples.
      final short = elevationGainFromAltitudes(
        _noisyClimb(trueGain: 200, samples: 900),
      );
      final long = elevationGainFromAltitudes(
        _noisyClimb(trueGain: 200, samples: 3600),
      );
      expect((short - long).abs(), lessThan(80));
    });

    test('a clean staircase is measured, and descent is not counted as gain', () {
      // 30 m up, 30 m down, 30 m up. Each level is held for a stretch of samples
      // because the smoothing needs a few readings to follow a step -- four bare
      // points is not something GPS ever produces, and a filter that trusted
      // four points could not reject noise at all.
      const hold = 40;
      final altitudes = <double?>[
        for (final level in [0.0, 30.0, 0.0, 30.0])
          for (var i = 0; i < hold; i++) level,
      ];
      expect(elevationGainFromAltitudes(altitudes), closeTo(60, 6));
      expect(elevationLossFromAltitudes(altitudes), closeTo(30, 6));
    });

    test('a sparse imported route is not under-reported', () {
      // A GPX with one point every ~100 m: few samples, so a heavily lagging
      // filter would swallow much of the climb. Regression guard for the
      // smoothing constant.
      final sparse = <double?>[
        for (var i = 0; i < 50; i++) 750.0 * (i / 49),
      ];
      expect(elevationGainFromAltitudes(sparse), greaterThan(680));
    });

    test('a two-point climb is not swallowed by the smoothing lag', () {
      // The moving average trails the truth, and with only two points that lag
      // IS the whole route: this reported 24 m of an 80 m climb before the
      // residual was settled against the last real altitude.
      expect(elevationGainFromAltitudes(const <double?>[10, 90]), closeTo(80, 2));
      expect(elevationLossFromAltitudes(const <double?>[90, 10]), closeTo(80, 2));
    });

    test('movement below the step threshold never accumulates', () {
      // Jitter of ±2 m, five hundred times. Every sample is under the 5 m step,
      // so the reference must not move and nothing may be banked.
      final jitter = <double?>[
        for (var i = 0; i < 500; i++) (i.isEven ? 2.0 : -2.0),
      ];
      expect(elevationGainFromAltitudes(jitter), 0);
    });

    test('null and non-finite altitudes are skipped, not treated as zero', () {
      // A route that briefly loses altitude fixes has holes in it; treating a
      // null as 0 m would invent a fall to sea level and a climb back out.
      final withHoles = <double?>[100, null, 103, double.nan, 106, null, 109];
      expect(elevationGainFromAltitudes(withHoles), lessThan(14));
      expect(elevationGainFromAltitudes(const <double?>[]), 0);
      expect(elevationGainFromAltitudes(const <double?>[null, null]), 0);
      expect(elevationGainFromAltitudes(const <double?>[42]), 0);
    });
  });
}
