import '../../core/period/time_range.dart';
import '../../data/repository/contract/caffeine_repository.dart';
import '../model/caffeine_models.dart';
import '../model/refresh_mode.dart';

/// Loads the caffeine entries the screen needs, in a single read.
///
/// The screen renders two windows at once: today's caffeine curve (the home
/// card) and whatever analytics range the user picked (which may be 90 days
/// back, and may not include today at all — "Yesterday" does not). Reading them
/// separately would double the Health Connect round-trip and, worse, let the two
/// disagree at the boundary, so the use case reads their *union* once and both
/// insight passes are computed from the same entry list.
class LoadCaffeineUseCase {
  const LoadCaffeineUseCase(this._caffeineRepository);

  final CaffeineRepository _caffeineRepository;

  Future<CaffeinePeriodData> call(
    DatePeriod homePeriod,
    DatePeriod analyticsPeriod, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) =>
      _caffeineRepository.loadCaffeineData(
        _union(homePeriod, analyticsPeriod),
        refreshMode: refreshMode,
      );

  DatePeriod _union(DatePeriod a, DatePeriod b) => DatePeriod(
        a.start.isBefore(b.start) ? a.start : b.start,
        a.end.isAfter(b.end) ? a.end : b.end,
      );
}
