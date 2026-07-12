import '../../core/period/period_load_query.dart';
import '../../core/result/result.dart';
import '../../data/repository/contract/cycle_repository.dart';
import '../model/refresh_mode.dart';
import '../query/cycle_period_data.dart';

/// Loads one period of cycle tracking.
///
/// The result carries its own `missingPermissions` set rather than throwing:
/// cycle data spans several Health Connect record types (flow, ovulation tests,
/// cervical mucus, basal body temperature, …) and a user may well have granted
/// some and not others. A partly-permitted read must still render the part it
/// can, with the screen prompting for the rest.
class LoadCyclePeriodUseCase {
  const LoadCyclePeriodUseCase(this._cycleRepository);

  final CycleRepository _cycleRepository;

  Future<Result<CyclePeriodData>> call(
    PeriodLoadQuery query, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) =>
      _cycleRepository.loadCyclePeriod(query, refreshMode: refreshMode);
}
