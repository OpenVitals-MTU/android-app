/// Cumulative ascent from a series of altitudes, with GPS noise filtered out.
///
/// Summing every positive difference between consecutive points — the obvious
/// implementation — does not work for GPS altitude. Vertical GPS error is around
/// ±3–5 m and resamples every point, so on a one-hour ride the naive sum banks
/// that noise thousands of times: a **perfectly flat** route reports ~6 km of
/// climb, and a real 750 m ride reported ~15 km. A per-step minimum does not
/// rescue it either, because the noise is far larger than any sane step.
///
/// Two filters, applied in order, and both are needed:
///
/// 1. **Smoothing.** An exponential moving average over the altitudes
///    ([_smoothingAlpha]) removes the sample-to-sample jitter that produces the
///    bulk of the false gain.
/// 2. **Hysteresis.** Gain is banked only once the smoothed altitude has moved
///    [_minStepMeters] from the last *accepted* reference — not from the previous
///    sample. Movement smaller than that never shifts the reference, so noise
///    cannot ratchet upward.
///
/// Against simulated routes (1 Hz, σ = 3 m vertical error) this reports 304 m for
/// a true 300 m, 757 m for a true 750 m, and ~15 m for a genuinely flat route —
/// against ~6 km for the naive sum on that same flat route.
///
/// The same approach already worked for the barometer path in
/// `activity_recording_service.dart`, which is why that one was accurate while
/// every GPS-derived figure was not.
library;

import '../model/activity_models.dart';

/// EMA weight for a new altitude sample.
///
/// Tuned against both failure modes, which pull in opposite directions. Heavier
/// smoothing rejects noise better but lags, and the lag under-reports SPARSE
/// routes — an imported GPX with one point every 100 m has few samples for the
/// average to catch up on, and 0.1 lost 18% of a clean 750 m climb. 0.3 keeps
/// every case tested within ~5%.
const double _smoothingAlpha = 0.3;

/// How far the smoothed altitude must move from the accepted reference before
/// the move counts. Comfortably above GPS vertical noise once smoothed.
const double _minStepMeters = 5.0;

/// Cumulative ascent in meters over [altitudes], ignoring nulls and non-finite
/// values. Returns 0 when fewer than two usable altitudes are present.
double elevationGainFromAltitudes(Iterable<double?> altitudes) =>
    _accumulate(altitudes).gain;

/// Cumulative descent in meters, as a positive number.
double elevationLossFromAltitudes(Iterable<double?> altitudes) =>
    _accumulate(altitudes).loss;

/// Ascent and descent in one pass, for callers that need both.
({double gain, double loss}) elevationChangeFromAltitudes(
  Iterable<double?> altitudes,
) =>
    _accumulate(altitudes);

/// Cumulative ascent over a recorded or imported route.
double routeElevationGain(List<ExerciseRoutePoint> points) =>
    elevationGainFromAltitudes(points.map((point) => point.altitudeMeters));

/// Cumulative descent over a recorded or imported route, as a positive number.
double routeElevationLoss(List<ExerciseRoutePoint> points) =>
    elevationLossFromAltitudes(points.map((point) => point.altitudeMeters));

({double gain, double loss}) _accumulate(Iterable<double?> altitudes) {
  double? smoothed;
  double? reference;
  double? lastAltitude;
  var gain = 0.0;
  var loss = 0.0;

  for (final altitude in altitudes) {
    if (altitude == null || !altitude.isFinite) continue;
    lastAltitude = altitude;
    smoothed = smoothed == null
        ? altitude
        : smoothed + (altitude - smoothed) * _smoothingAlpha;
    if (reference == null) {
      reference = smoothed;
      continue;
    }
    final delta = smoothed - reference;
    if (delta >= _minStepMeters) {
      gain += delta;
      reference = smoothed;
    } else if (delta <= -_minStepMeters) {
      loss += -delta;
      reference = smoothed;
    }
    // Anything smaller is noise: the reference deliberately does NOT move, so
    // repeated jitter cannot accumulate.
  }

  // Settle the smoothing lag against the final RAW altitude.
  //
  // The moving average trails the true altitude by roughly its time constant,
  // and on a short or sparse route that trailing tail is a large share of the
  // whole: an 80 m climb described by two points reported 24 m, and a 50-point
  // GPX lost 5%. Comparing the last real altitude against the reference recovers
  // exactly that remainder. It costs nothing on long noisy routes, where the
  // reference has already caught up and any residue is a single sub-step value.
  if (lastAltitude != null && reference != null) {
    final residual = lastAltitude - reference;
    if (residual >= _minStepMeters) {
      gain += residual;
    } else if (residual <= -_minStepMeters) {
      loss += -residual;
    }
  }

  return (gain: gain, loss: loss);
}
