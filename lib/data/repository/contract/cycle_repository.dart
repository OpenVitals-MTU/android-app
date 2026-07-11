import '../../../core/period/period_load_query.dart';
import '../../../core/time/local_date.dart';
import '../../../domain/model/cycle_models.dart';
import '../../../domain/model/refresh_mode.dart';
import '../../../domain/query/cycle_period_data.dart';

/// Port of the Kotlin `CycleRepository` contract.
abstract interface class CycleRepository {
  Set<String> get phase4Permissions;

  Future<Set<String>> missingPermissions();

  Future<CyclePeriodData> loadCyclePeriod(
    PeriodLoadQuery query, {
    RefreshMode refreshMode = RefreshMode.normal,
  });

  Future<CycleData> loadCycleData(LocalDate start, LocalDate end);
}
