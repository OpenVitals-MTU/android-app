import '../../../core/result/result.dart';
import '../../../domain/insights/body_energy_timeline.dart';

/// Records the user's own Body Energy "feel-checks" and folds each one into the
/// personal calibration gains.
abstract interface class BodyEnergyFeelCheckRepository {
  /// Logs a [rating] of 0–10 for how the user's energy feels right now, and
  /// nudges the personal gains toward it. [predictedScore] and
  /// [dominantInfluence] describe what the model was showing at that moment, so
  /// the fit knows which gain the mismatch belongs to.
  Future<Result<void>> recordFeelCheck({
    required int rating,
    required int predictedScore,
    required BodyEnergyPrimaryInfluence dominantInfluence,
    DateTime? at,
  });
}
