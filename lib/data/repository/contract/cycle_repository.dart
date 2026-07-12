import '../../../core/period/period_load_query.dart';
import '../../../core/result/result.dart';
import '../../../core/time/local_date.dart';
import '../../../domain/model/cycle_models.dart';
import '../../../domain/model/refresh_mode.dart';
import '../../../domain/query/cycle_period_data.dart';

/// Port of the Kotlin `CycleRepository` contract.
///
/// Fallible operations return [Result]; the synchronous probe
/// ([phase4Permissions]) reads cached state and cannot fail, so it stays bare.
abstract interface class CycleRepository {
  Set<String> get phase4Permissions;

  Future<Result<Set<String>>> missingPermissions();

  Future<Result<CyclePeriodData>> loadCyclePeriod(
    PeriodLoadQuery query, {
    RefreshMode refreshMode = RefreshMode.normal,
  });

  Future<Result<CycleData>> loadCycleData(LocalDate start, LocalDate end);
}
