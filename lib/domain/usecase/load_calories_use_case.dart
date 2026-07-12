import '../../core/period/period_load_query.dart';
import '../../data/repository/contract/activity_repository.dart';
import '../../data/repository/contract/body_repository.dart';
import '../model/refresh_mode.dart';
import '../query/activity_period_data.dart';

/// The calories overview: the period's burn alongside the body's baseline burn.
class CaloriesLoadResult {
  const CaloriesLoadResult({required this.data, required this.latestBmrKcal});

  final ActivityPeriodData data;

  /// The most recent basal metabolic rate on record, or null if nothing has
  /// ever measured one.
  final double? latestBmrKcal;
}

/// Loads the calories overview.
///
/// "Calories burned" is only half the story a user is after: active burn comes
/// from the activity period (steps + nutrition), while the resting burn that
/// makes up most of a day comes from the *body* repository's basal metabolic
/// rate. Two repositories, so the two reads run together rather than in series.
///
/// The BMR is a single latest-value read, not a period one — it is a slow-moving
/// body attribute, and Health Connect stores it as an occasional record rather
/// than a daily series.
class LoadCaloriesUseCase {
  const LoadCaloriesUseCase(this._activityRepository, this._bodyRepository);

  final ActivityRepository _activityRepository;
  final BodyRepository _bodyRepository;

  Future<CaloriesLoadResult> call(
    PeriodLoadQuery query, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) async {
    final results = await (
      _activityRepository.loadActivityPeriod(
        query,
        includeSteps: true,
        includeNutrition: true,
        refreshMode: refreshMode,
      ),
      _bodyRepository.loadLatestBMR(),
    ).wait;
    return CaloriesLoadResult(data: results.$1, latestBmrKcal: results.$2);
  }
}
