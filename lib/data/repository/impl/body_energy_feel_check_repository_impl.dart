import '../../../core/result/result.dart';
import '../../../domain/insights/body_energy_calibration_fit.dart';
import '../../../domain/insights/body_energy_timeline.dart';
import '../../local/open_vitals_database.dart';
import '../../prefs/preferences_repository.dart';
import '../contract/body_energy_feel_check_repository.dart';
import 'run_catching.dart';

class BodyEnergyFeelCheckRepositoryImpl
    implements BodyEnergyFeelCheckRepository {
  BodyEnergyFeelCheckRepositoryImpl({
    required FeelCheckDao feelCheckDao,
    required PreferencesRepository preferencesRepository,
    DateTime Function() now = DateTime.now,
  }) : _dao = feelCheckDao,
       _preferences = preferencesRepository,
       // ignore: prefer_initializing_formals
       _now = now;

  final FeelCheckDao _dao;
  final PreferencesRepository _preferences;
  final DateTime Function() _now;

  @override
  Future<Result<void>> recordFeelCheck({
    required int rating,
    required int predictedScore,
    required BodyEnergyPrimaryInfluence dominantInfluence,
    DateTime? at,
  }) {
    return runCatching(() async {
      final time = at ?? _now();
      await _dao.insertFeelCheck(
        recordedAtMillis: time.millisecondsSinceEpoch,
        rating: rating.clamp(0, 10),
      );
      // Fold this single check into the gains, once, at log time. Applying the
      // fit here (rather than re-fitting all history on every load) keeps each
      // check counted exactly once.
      final fitted = fitBodyEnergyGains(_preferences.bodyEnergyCalibration(), [
        BodyEnergyFeelCheck(
          time: time,
          rating: rating,
          predictedScore: predictedScore,
          dominantInfluence: dominantInfluence,
        ),
      ]);
      _preferences.setBodyEnergyCalibration(fitted);
    });
  }
}
