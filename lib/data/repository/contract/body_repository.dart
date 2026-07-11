import '../../../core/period/period_load_query.dart';
import '../../../core/time/local_date.dart';
import '../../../domain/model/body_models.dart';
import '../../../domain/model/refresh_mode.dart';
import '../../../domain/query/body_period_data.dart';

/// Which body metric family a period load should populate. Port of the Kotlin
/// `BodyPeriodMetric`.
enum BodyPeriodMetric {
  all,
  weight,
  height,
  bmi,
  bodyFat,
  leanMass,
  bmr,
  boneMass,
  bodyWaterMass,
}

/// Port of the Kotlin `BodyRepository` contract.
abstract interface class BodyRepository {
  Set<String> bodyWritePermissions(BodyMeasurementType type);

  Future<BodyPeriodData> loadBodyPeriod(
    PeriodLoadQuery query,
    BodyPeriodMetric metric, {
    RefreshMode refreshMode = RefreshMode.normal,
  });

  Future<List<WeightEntry>> loadWeightEntries(LocalDate start, LocalDate end);

  Future<double?> loadLatestHeight();

  Future<List<HeightEntry>> loadHeightEntries(LocalDate start, LocalDate end);

  Future<List<BodyFatEntry>> loadBodyFatEntries(LocalDate start, LocalDate end);

  Future<double?> loadLatestLeanBodyMass();

  Future<List<LeanBodyMassEntry>> loadLeanBodyMassEntries(
    LocalDate start,
    LocalDate end,
  );

  Future<double?> loadLatestBMR();

  Future<List<BmrEntry>> loadBmrEntries(LocalDate start, LocalDate end);

  Future<double?> loadLatestBoneMass();

  Future<List<BoneMassEntry>> loadBoneMassEntries(LocalDate start, LocalDate end);

  Future<double?> loadLatestBodyWaterMass();

  Future<List<BodyWaterMassEntry>> loadBodyWaterMassEntries(
    LocalDate start,
    LocalDate end,
  );

  Future<bool> hasBodyWritePermission(BodyMeasurementType type);

  Future<String> writeBodyMeasurementEntry(BodyMeasurementWriteRequest request);

  Future<BodyMeasurementEntry?> loadBodyMeasurementEntry(
    BodyMeasurementType type,
    String id,
  );

  Future<void> updateBodyMeasurementEntry(
    String id,
    BodyMeasurementWriteRequest request,
  );

  Future<void> deleteBodyMeasurementEntry(BodyMeasurementType type, String id);
}
