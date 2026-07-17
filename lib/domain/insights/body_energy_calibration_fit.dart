import '../preferences/body_energy_calibration.dart';
import 'body_energy_timeline.dart';

/// One "feel-check": the user's own 0–10 energy rating at a moment in time,
/// paired with what the model predicted for that moment.
class BodyEnergyFeelCheck {
  const BodyEnergyFeelCheck({
    required this.time,
    required this.rating,
    required this.predictedScore,
    required this.dominantInfluence,
  });

  /// The user's rating, 0–10 (×10 gives an observed 0–100 score).
  final int rating;

  /// The model's score at [time] under the current gains.
  final int predictedScore;

  /// Which influence was drawing the score most in the window leading up to
  /// [time] — the gain a mismatch is attributed to.
  final BodyEnergyPrimaryInfluence dominantInfluence;

  final DateTime time;

  int get observedScore => (rating * 10).clamp(0, 100);
}

/// Fits the personal gains from feel-checks — transparently.
///
/// Each feel-check says "the model predicted P, I felt O". A gap means the model
/// moved the score too much or too little in the direction its dominant driver
/// was pushing. We nudge exactly that one gain by a small step, bounded to
/// [BodyEnergyCalibration.minGain]..[maxGain], so the outcome is always one
/// legible number the user can read and override — not a hidden optimiser.
///
/// A drain driver (activity, basal, stress): if the user felt *lower* than
/// predicted, they were drained harder than modelled, so raise that drain gain;
/// if they felt *higher*, lower it. A charge driver (sleep recovery) is the
/// mirror: felt higher → raise the charge gain.
BodyEnergyCalibration fitBodyEnergyGains(
  BodyEnergyCalibration current,
  List<BodyEnergyFeelCheck> feelChecks, {
  double learningRate = _defaultLearningRate,
}) {
  if (feelChecks.isEmpty) return current.normalized();

  var sleep = current.sleepChargeGain;
  var activity = current.activityDrainGain;
  var basal = current.basalDrainGain;
  var stress = current.stressDrainGain;

  for (final check in feelChecks) {
    // Normalised error in [-1, 1]: positive means the user felt better than
    // predicted, negative means worse.
    final error = (check.observedScore - check.predictedScore) / 100.0;
    if (error == 0.0) continue;
    final step = learningRate * error;

    switch (check.dominantInfluence) {
      case BodyEnergyPrimaryInfluence.sleepRecovery:
        // Felt better → sleep recharged more than modelled → raise the gain.
        sleep += step;
      case BodyEnergyPrimaryInfluence.everydayActivity:
      case BodyEnergyPrimaryInfluence.exertion:
        // Felt worse → activity drained more than modelled → raise the gain.
        activity -= step;
      case BodyEnergyPrimaryInfluence.elevatedHeartRate:
        stress -= step;
      case BodyEnergyPrimaryInfluence.recoveryDebt:
      case BodyEnergyPrimaryInfluence.quietRest:
      case BodyEnergyPrimaryInfluence.steady:
        // Steady windows are basal-dominated; a mismatch there is the baseline
        // burn being off.
        basal -= step;
      case BodyEnergyPrimaryInfluence.noData:
        break;
    }
  }

  const lo = BodyEnergyCalibration.minGain;
  const hi = BodyEnergyCalibration.maxGain;
  return current
      .copyWith(
        sleepChargeGain: sleep.clamp(lo, hi),
        activityDrainGain: activity.clamp(lo, hi),
        basalDrainGain: basal.clamp(lo, hi),
        stressDrainGain: stress.clamp(lo, hi),
        feelCheckCount: current.feelCheckCount + feelChecks.length,
      )
      .normalized();
}

/// One feel-check moves a gain at most this far; small so a single mood swing
/// can't swamp the model, and the gains converge over weeks of check-ins.
const double _defaultLearningRate = 0.15;
