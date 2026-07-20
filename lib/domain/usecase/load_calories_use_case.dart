import '../../core/period/period_load_query.dart';
import '../../core/result/result.dart';
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

  Future<Result<CaloriesLoadResult>> call(
    PeriodLoadQuery query, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) async {
    final results = await (
      _activityRepository.loadActivityPeriod(
        query,
        includeSteps: true,
        includeNutrition: true,
        // The calories overview draws its two series from the daily-steps and
        // nutrition slices only; it never renders the intraday cumulative chart,
        // so it skips that Day-only aggregate read entirely.
        includeActivityProgress: false,
        // It shows the current period alone (no previous/baseline comparison),
        // so it skips the four extra window reads — on the Year range that is
        // four fewer 365/90-day Health Connect aggregates, the bulk of the load.
        includeComparisonWindows: false,
        refreshMode: refreshMode,
      ),
      _bodyRepository.loadLatestBMR(),
    ).wait;
    // Both halves are the story the screen tells (active burn + resting burn),
    // so the composition is STRICT: either read failing fails the overview,
    // exactly as before the Result migration.
    return results.$1.flatMap(
      (data) async => results.$2.map(
        (latestBmrKcal) =>
            CaloriesLoadResult(data: data, latestBmrKcal: latestBmrKcal),
      ),
    );
  }
}
