import '../../core/period/period_load_query.dart';
import '../../core/result/result.dart';
import '../../data/repository/contract/body_repository.dart';
import '../model/refresh_mode.dart';
import '../query/body_period_data.dart';

/// Loads every body metric for one period, in one read.
///
/// The eight body metrics (weight, height, body fat, lean mass, BMR, bone mass,
/// body water, …) are read together — [BodyPeriodMetric.all] — rather than one
/// screen at a time: they share a period, a Health Connect round-trip is far
/// more expensive than the extra rows, and the aggregate `/body` screen needs
/// them all anyway. That is why there is no per-metric variant of this call.
class LoadBodyPeriodUseCase {
  const LoadBodyPeriodUseCase(this._bodyRepository);

  final BodyRepository _bodyRepository;

  Future<Result<BodyPeriodData>> call(
    PeriodLoadQuery query, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) =>
      _bodyRepository.loadBodyPeriod(
        query,
        BodyPeriodMetric.all,
        refreshMode: refreshMode,
      );
}
