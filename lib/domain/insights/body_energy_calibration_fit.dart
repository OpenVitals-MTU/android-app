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

/// One reading from a watch that computes its own body-energy score (Garmin
/// Body Battery), paired with what this app's model predicted for that moment.
///
/// Structurally the same observation as a [BodyEnergyFeelCheck] — "the model
/// predicted P, an independent source says O" — and fed through the same fit.
/// It is a distinct type because it is NOT the same kind of evidence: a
/// feel-check is the user's lived experience, while this is another vendor's
/// MODEL. That earns it less weight per reading, not more, however many of them
/// arrive.
class BodyEnergyWatchReading {
  const BodyEnergyWatchReading({
    required this.time,
    required this.observedScore,
    required this.predictedScore,
    required this.dominantInfluence,
  });

  final DateTime time;

  /// The watch's own 0–100 body-energy score.
  final int observedScore;

  /// This app's score at [time] under the current gains.
  final int predictedScore;

  final BodyEnergyPrimaryInfluence dominantInfluence;
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
  List<BodyEnergyWatchReading> watchReadings = const [],
  double watchLearningRate = _defaultWatchLearningRate,
}) {
  if (feelChecks.isEmpty && watchReadings.isEmpty) return current.normalized();

  var sleep = current.sleepChargeGain;
  var activity = current.activityDrainGain;
  var basal = current.basalDrainGain;
  var stress = current.stressDrainGain;

  // Both sources are the same shape of evidence — predicted vs observed — so
  // they run through one loop, differing only in how hard each nudges.
  final observations = <(int observed, int predicted, BodyEnergyPrimaryInfluence, double rate)>[
    for (final c in feelChecks)
      (c.observedScore, c.predictedScore, c.dominantInfluence, learningRate),
    for (final w in watchReadings)
      (w.observedScore, w.predictedScore, w.dominantInfluence, watchLearningRate),
  ];

  for (final (observed, predicted, influence, rate) in observations) {
    // Normalised error in [-1, 1]: positive means the observation was higher
    // than predicted, negative means lower.
    final error = (observed - predicted) / 100.0;
    if (error == 0.0) continue;
    final step = rate * error;

    // An observation must move the gain that scales the drain it is blaming.
    // Anything else is either impotent — no gain scales that component — or
    // aimed at a component that was not responsible, and both show up as a gain
    // drifting to answer for something it does not control.
    switch (influence) {
      case BodyEnergyPrimaryInfluence.sleepRecovery:
        // Felt better → sleep recharged more than modelled → raise the gain.
        sleep += step;
      case BodyEnergyPrimaryInfluence.everydayActivity:
      case BodyEnergyPrimaryInfluence.exertion:
      case BodyEnergyPrimaryInfluence.recoveryDebt:
        // Felt worse → activity drained more than modelled → raise the gain.
        // Recovery debt belongs here too: it is scaled by `activityDrainGain`
        // like the other two, being the tail of the same effort. It used to move
        // `basalDrainGain`, which scales the waking floor and not recovery debt
        // at all — so it could not fix the error it aimed at, and corrupted the
        // basal figure while failing to.
        activity -= step;
      case BodyEnergyPrimaryInfluence.elevatedHeartRate:
        stress -= step;
      case BodyEnergyPrimaryInfluence.steady:
        // The one influence basal should answer for: `_primaryInfluence` reports
        // steady exactly when every competing drain is zero, which leaves the
        // basal floor as the only thing that moved the score.
        basal -= step;
      case BodyEnergyPrimaryInfluence.quietRest:
        // A charge with no sleep in the bucket. The waking-rest charge is
        // scaled by `sleepChargeGain` like sleep itself, so that is the gain to
        // move. If the two ever need to diverge, that is a fifth gain rather
        // than a different routing here.
        sleep += step;
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
        watchObservationCount:
            current.watchObservationCount + watchReadings.length,
      )
      .normalized();
}

/// The generation of the watch-fit machinery, bumped when a fix means the
/// evidence already consumed has to be read again.
///
/// Distinct from the algorithm version, which describes the MODEL. A bug in the
/// fit leaves the model untouched, so an algorithm bump to force a refit would
/// both misdescribe the change and discard the stored chain for nothing.
///
/// 1 — the watch fit watermark was advanced past days whose timelines had not
/// been computed yet, and was never rewound when the gains reset. Between them,
/// an install could sit on thousands of stored samples with a watch observation
/// count of 2 and every gain at exactly 1.00.
const int bodyEnergyWatchFitEpoch = 1;

/// One feel-check moves a gain at most this far; small so a single mood swing
/// can't swamp the model, and the gains converge over weeks of check-ins.
const double _defaultLearningRate = 0.15;

/// A watch reading moves a gain less than a feel-check, but not by much.
///
/// Still below the feel-check rate on purpose: a watch reading is another
/// model's OUTPUT, not the user's lived experience, so a check-in should always
/// outweigh one. The gap is deliberately modest so the watch actually converges
/// the gains in days rather than months.
///
/// The trade-off that buys: a day of readings that disagree hard and
/// consistently CAN now reach a gain's clamp. That is judged acceptable — such
/// a day means the model is badly wrong and a large correction is the right
/// answer — and the hourly downsampling plus the [BodyEnergyCalibration] bounds
/// still stop it running away.
const double _defaultWatchLearningRate = 0.1;
