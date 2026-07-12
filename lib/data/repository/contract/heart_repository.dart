import '../../../core/period/period_load_query.dart';
import '../../../core/result/result.dart';
import '../../../core/time/local_date.dart';
import '../../../domain/model/heart_models.dart';
import '../../../domain/model/refresh_mode.dart';
import '../../../domain/query/heart_period_data.dart';

/// Which heart metric family a period load should populate. Port of the Kotlin
/// `HeartPeriodMetric` (declared alongside `HeartRepositoryImpl`).
enum HeartPeriodMetric { all, averageHeartRate, restingHeartRate, hrv }

/// Port of the Kotlin `HeartRepository` contract.
///
/// Fallible operations return [Result]; every method here is a Health Connect
/// read, so there is no synchronous cached-state probe to stay bare.
abstract interface class HeartRepository {
  Future<Result<HeartPeriodData>> loadHeartPeriod(
    PeriodLoadQuery query,
    HeartPeriodMetric metric, {
    RefreshMode refreshMode = RefreshMode.normal,
  });

  Future<Result<List<HeartRateSample>>> loadHeartRateSamplesForDay(
    LocalDate date,
  );

  Future<Result<List<HeartRateSample>>> loadRawHeartRateSamplesForDayGraph(
    LocalDate date,
  );

  Future<Result<List<HeartRateSample>>> loadHeartRateSamples(
    LocalDate start,
    LocalDate end,
  );

  Future<Result<List<HeartRateSample>>> loadHeartRateSamplesInstant(
    DateTime start,
    DateTime end,
  );

  Future<Result<List<HeartRateSummary>>> loadDailyHeartRateSummaries(
    LocalDate start,
    LocalDate end,
  );

  Future<Result<int?>> loadRestingHeartRate(LocalDate date);

  Future<Result<List<DailyRestingHR>>> loadDailyRestingHR(
    LocalDate start,
    LocalDate end,
  );

  Future<Result<double?>> loadHrvRmssd(LocalDate date);

  Future<Result<List<HrvSample>>> loadHrvSamples(DateTime start, DateTime end);

  Future<Result<List<DailyHrv>>> loadDailyHRV(LocalDate start, LocalDate end);
}
