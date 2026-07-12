import '../../../core/period/period_load_query.dart';
import '../../../core/period/time_range.dart';
import '../../../core/result/result.dart';
import '../../../domain/model/caffeine_models.dart';
import '../../../domain/model/refresh_mode.dart';

/// Port of the Kotlin `CaffeineRepository` contract.
///
/// The Kotlin interface gives `loadCaffeinePeriod` a default body delegating to
/// `loadCaffeineData(query.windows.current, ...)`; here that default lives in
/// the mixin [CaffeineRepositoryDefaults] so implementations only supply
/// [loadCaffeineData].
///
/// Fallible operations return [Result].
abstract interface class CaffeineRepository {
  Future<Result<CaffeinePeriodData>> loadCaffeinePeriod(
    PeriodLoadQuery query, {
    RefreshMode refreshMode = RefreshMode.normal,
  });

  Future<Result<CaffeinePeriodData>> loadCaffeineData(
    DatePeriod period, {
    RefreshMode refreshMode = RefreshMode.normal,
  });
}

/// Mirrors the Kotlin interface's default `loadCaffeinePeriod` implementation.
mixin CaffeineRepositoryDefaults implements CaffeineRepository {
  @override
  Future<Result<CaffeinePeriodData>> loadCaffeinePeriod(
    PeriodLoadQuery query, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) =>
      loadCaffeineData(query.windows.current, refreshMode: refreshMode);
}
