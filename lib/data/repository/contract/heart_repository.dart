import '../../../core/period/period_load_query.dart';
import '../../../core/time/local_date.dart';
import '../../../domain/model/heart_models.dart';
import '../../../domain/model/refresh_mode.dart';
import '../../../domain/query/heart_period_data.dart';

/// Which heart metric family a period load should populate. Port of the Kotlin
/// `HeartPeriodMetric` (declared alongside `HeartRepositoryImpl`).
enum HeartPeriodMetric { all, averageHeartRate, restingHeartRate, hrv }

/// Port of the Kotlin `HeartRepository` contract.
abstract interface class HeartRepository {
  Future<HeartPeriodData> loadHeartPeriod(
    PeriodLoadQuery query,
    HeartPeriodMetric metric, {
    RefreshMode refreshMode = RefreshMode.normal,
  });

  Future<List<HeartRateSample>> loadHeartRateSamplesForDay(LocalDate date);

  Future<List<HeartRateSample>> loadRawHeartRateSamplesForDayGraph(
    LocalDate date,
  );

  Future<List<HeartRateSample>> loadHeartRateSamples(
    LocalDate start,
    LocalDate end,
  );

  Future<List<HeartRateSample>> loadHeartRateSamplesInstant(
    DateTime start,
    DateTime end,
  );

  Future<List<HeartRateSummary>> loadDailyHeartRateSummaries(
    LocalDate start,
    LocalDate end,
  );

  Future<int?> loadRestingHeartRate(LocalDate date);

  Future<List<DailyRestingHR>> loadDailyRestingHR(LocalDate start, LocalDate end);

  Future<double?> loadHrvRmssd(LocalDate date);

  Future<List<HrvSample>> loadHrvSamples(DateTime start, DateTime end);

  Future<List<DailyHrv>> loadDailyHRV(LocalDate start, LocalDate end);
}
