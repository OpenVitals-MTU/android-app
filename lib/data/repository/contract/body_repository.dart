import '../../../core/period/period_load_query.dart';
import '../../../core/result/result.dart';
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
///
/// Fallible operations return [Result]; the synchronous probe
/// ([bodyWritePermissions]) reads cached state and cannot fail, so it stays
/// bare.
abstract interface class BodyRepository {
  Set<String> bodyWritePermissions(BodyMeasurementType type);

  Future<Result<BodyPeriodData>> loadBodyPeriod(
    PeriodLoadQuery query,
    BodyPeriodMetric metric, {
    RefreshMode refreshMode = RefreshMode.normal,
  });

  Future<Result<List<WeightEntry>>> loadWeightEntries(
    LocalDate start,
    LocalDate end,
  );

  Future<Result<double?>> loadLatestHeight();

  Future<Result<List<HeightEntry>>> loadHeightEntries(
    LocalDate start,
    LocalDate end,
  );

  Future<Result<List<BodyFatEntry>>> loadBodyFatEntries(
    LocalDate start,
    LocalDate end,
  );

  Future<Result<double?>> loadLatestLeanBodyMass();

  Future<Result<List<LeanBodyMassEntry>>> loadLeanBodyMassEntries(
    LocalDate start,
    LocalDate end,
  );

  Future<Result<double?>> loadLatestBMR();

  Future<Result<List<BmrEntry>>> loadBmrEntries(LocalDate start, LocalDate end);

  Future<Result<double?>> loadLatestBoneMass();

  Future<Result<List<BoneMassEntry>>> loadBoneMassEntries(
    LocalDate start,
    LocalDate end,
  );

  Future<Result<double?>> loadLatestBodyWaterMass();

  Future<Result<List<BodyWaterMassEntry>>> loadBodyWaterMassEntries(
    LocalDate start,
    LocalDate end,
  );

  Future<Result<bool>> hasBodyWritePermission(BodyMeasurementType type);

  Future<Result<String>> writeBodyMeasurementEntry(
    BodyMeasurementWriteRequest request,
  );

  Future<Result<BodyMeasurementEntry?>> loadBodyMeasurementEntry(
    BodyMeasurementType type,
    String id,
  );

  Future<Result<void>> updateBodyMeasurementEntry(
    String id,
    BodyMeasurementWriteRequest request,
  );

  Future<Result<void>> deleteBodyMeasurementEntry(
    BodyMeasurementType type,
    String id,
  );
}
