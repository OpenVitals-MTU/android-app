import '../../../core/period/period_load_query.dart';
import '../../../core/result/result.dart';
import '../../../core/time/local_date.dart';
import '../../../domain/model/refresh_mode.dart';
import '../../../domain/model/vitals_models.dart';
import '../../../domain/query/vitals_period_data.dart';

/// Which vitals metric family a period load should populate. Port of the Kotlin
/// `VitalsPeriodMetric`.
enum VitalsPeriodMetric {
  all,
  bloodPressure,
  spo2,
  vo2Max,
  respiratoryRate,
  bodyTemperature,
  bloodGlucose,
  skinTemperature,
}

/// Port of the Kotlin `VitalsRepository` contract.
///
/// Fallible operations return [Result]; the synchronous probes
/// ([phase3Permissions], [vitalsWritePermissions]) read cached state and
/// cannot fail, so they stay bare.
abstract interface class VitalsRepository {
  Set<String> get phase3Permissions;

  Set<String> vitalsWritePermissions(VitalsMeasurementType type);

  Future<Result<Set<String>>> missingPermissions();

  Future<Result<VitalsPeriodData>> loadVitalsPeriod(
    PeriodLoadQuery query,
    VitalsPeriodMetric metric, {
    RefreshMode refreshMode = RefreshMode.normal,
  });

  Future<Result<List<BloodPressureEntry>>> loadBloodPressure(
    LocalDate start,
    LocalDate end,
  );

  Future<Result<List<SpO2Entry>>> loadSpO2(LocalDate start, LocalDate end);

  Future<Result<List<RespiratoryRateEntry>>> loadRespiratoryRate(
    LocalDate start,
    LocalDate end,
  );

  Future<Result<List<BodyTempEntry>>> loadBodyTemperature(
    LocalDate start,
    LocalDate end,
  );

  Future<Result<List<Vo2MaxEntry>>> loadVo2Max(LocalDate start, LocalDate end);

  Future<Result<List<BloodGlucoseEntry>>> loadBloodGlucose(
    LocalDate start,
    LocalDate end,
  );

  Future<Result<List<SkinTemperatureEntry>>> loadSkinTemperature(
    LocalDate start,
    LocalDate end,
  );

  Future<Result<bool>> hasVitalsWritePermission(VitalsMeasurementType type);

  Future<Result<String>> writeVitalsMeasurementEntry(
    VitalsMeasurementWriteRequest request,
  );

  Future<Result<VitalsMeasurementEntry?>> loadVitalsMeasurementEntry(
    VitalsMeasurementType type,
    String id,
  );

  Future<Result<void>> updateVitalsMeasurementEntry(
    String id,
    VitalsMeasurementWriteRequest request,
  );

  Future<Result<void>> deleteVitalsMeasurementEntry(
    VitalsMeasurementType type,
    String id,
  );
}
