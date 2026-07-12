import '../../core/period/period_load_query.dart';
import '../../data/repository/contract/activity_repository.dart';
import '../model/refresh_mode.dart';
import '../query/activity_period_data.dart';

/// Loads one movement metric's period.
///
/// The six movement metrics share a single repository read; what differs is
/// which of its three optional slices they need. Asking for all three on every
/// metric would make the steps screen pay for a nutrition read it never renders,
/// so the caller passes the metric's own flags and the read stays as narrow as
/// the screen.
class LoadActivityMetricPeriodUseCase {
  const LoadActivityMetricPeriodUseCase(this._activityRepository);

  final ActivityRepository _activityRepository;

  Future<ActivityPeriodData> call(
    PeriodLoadQuery query, {
    required bool includeSteps,
    required bool includeNutrition,
    required bool includeWheelchairPushes,
    RefreshMode refreshMode = RefreshMode.normal,
  }) =>
      _activityRepository.loadActivityPeriod(
        query,
        includeSteps: includeSteps,
        includeNutrition: includeNutrition,
        includeWheelchairPushes: includeWheelchairPushes,
        refreshMode: refreshMode,
      );
}
