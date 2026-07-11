import '../../../core/period/period_load_query.dart';
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
abstract interface class VitalsRepository {
  Set<String> get phase3Permissions;

  Set<String> vitalsWritePermissions(VitalsMeasurementType type);

  Future<Set<String>> missingPermissions();

  Future<VitalsPeriodData> loadVitalsPeriod(
    PeriodLoadQuery query,
    VitalsPeriodMetric metric, {
    RefreshMode refreshMode = RefreshMode.normal,
  });

  Future<List<BloodPressureEntry>> loadBloodPressure(
    LocalDate start,
    LocalDate end,
  );

  Future<List<SpO2Entry>> loadSpO2(LocalDate start, LocalDate end);

  Future<List<RespiratoryRateEntry>> loadRespiratoryRate(
    LocalDate start,
    LocalDate end,
  );

  Future<List<BodyTempEntry>> loadBodyTemperature(LocalDate start, LocalDate end);

  Future<List<Vo2MaxEntry>> loadVo2Max(LocalDate start, LocalDate end);

  Future<List<BloodGlucoseEntry>> loadBloodGlucose(
    LocalDate start,
    LocalDate end,
  );

  Future<List<SkinTemperatureEntry>> loadSkinTemperature(
    LocalDate start,
    LocalDate end,
  );

  Future<bool> hasVitalsWritePermission(VitalsMeasurementType type);

  Future<String> writeVitalsMeasurementEntry(
    VitalsMeasurementWriteRequest request,
  );

  Future<VitalsMeasurementEntry?> loadVitalsMeasurementEntry(
    VitalsMeasurementType type,
    String id,
  );

  Future<void> updateVitalsMeasurementEntry(
    String id,
    VitalsMeasurementWriteRequest request,
  );

  Future<void> deleteVitalsMeasurementEntry(
    VitalsMeasurementType type,
    String id,
  );
}
