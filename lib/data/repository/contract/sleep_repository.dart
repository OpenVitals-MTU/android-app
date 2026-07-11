import '../../../core/period/period_load_query.dart';
import '../../../core/time/local_date.dart';
import '../../../domain/model/refresh_mode.dart';
import '../../../domain/model/sleep_models.dart';
import '../../../domain/preferences/sleep_range_mode.dart';
import '../../../domain/query/sleep_period_data.dart';

/// Port of the Kotlin `SleepRepository` contract.
abstract interface class SleepRepository {
  Future<SleepPeriodData> loadSleepPeriod(
    PeriodLoadQuery query,
    SleepRangeMode sleepRangeMode, {
    RefreshMode refreshMode = RefreshMode.normal,
  });

  Future<List<SleepData>> loadSleepSessions(LocalDate start, LocalDate end);

  Future<SleepData?> loadSleepSession(String id);
}
