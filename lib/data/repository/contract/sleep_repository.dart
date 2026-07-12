import '../../../core/period/period_load_query.dart';
import '../../../core/result/result.dart';
import '../../../core/time/local_date.dart';
import '../../../domain/model/refresh_mode.dart';
import '../../../domain/model/sleep_models.dart';
import '../../../domain/preferences/sleep_range_mode.dart';
import '../../../domain/query/sleep_period_data.dart';

/// Port of the Kotlin `SleepRepository` contract.
///
/// Fallible operations return [Result]; every method here is a Health Connect
/// read, so there is no synchronous cached-state probe to stay bare.
abstract interface class SleepRepository {
  Future<Result<SleepPeriodData>> loadSleepPeriod(
    PeriodLoadQuery query,
    SleepRangeMode sleepRangeMode, {
    RefreshMode refreshMode = RefreshMode.normal,
  });

  Future<Result<List<SleepData>>> loadSleepSessions(
    LocalDate start,
    LocalDate end,
  );

  Future<Result<SleepData?>> loadSleepSession(String id);
}
