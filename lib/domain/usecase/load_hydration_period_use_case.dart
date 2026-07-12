import '../../core/period/period_load_query.dart';
import '../../core/result/result.dart';
import '../../data/repository/contract/hydration_repository.dart';
import '../../data/repository/contract/nutrition_repository.dart';
import '../hydration/hydration_entry_merge.dart';
import '../model/nutrition_models.dart';
import '../model/refresh_mode.dart';

/// One period of hydration: the daily totals, and the entries with their drinks
/// named.
class HydrationPeriodLoadResult {
  const HydrationPeriodLoadResult({
    required this.dailyHydration,
    required this.entries,
  });

  final List<DailyHydration> dailyHydration;

  /// Hydration entries joined with their nutrition records — see
  /// [LoadHydrationPeriodUseCase] for why that join is needed.
  final List<HydrationEntry> entries;
}

/// Loads one period of hydration, with each entry's drink named.
///
/// A logged drink is written as *two* Health Connect records: a
/// `HydrationRecord` carrying the volume, and a `NutritionRecord` carrying the
/// name and the nutrients. The hydration read alone therefore returns a list of
/// anonymous volumes — which is why this needs a second repository, and why the
/// two are merged here rather than in the view (Kotlin's
/// `HydrationViewModel.load()` joins the two at the same point).
///
/// Without the nutrition read permission the nutrition list is empty and the
/// entries simply stay unnamed; the volumes, and the whole chart, are unaffected.
class LoadHydrationPeriodUseCase {
  const LoadHydrationPeriodUseCase(
    this._hydrationRepository,
    this._nutritionRepository,
  );

  final HydrationRepository _hydrationRepository;
  final NutritionRepository _nutritionRepository;

  Future<Result<HydrationPeriodLoadResult>> call(
    PeriodLoadQuery query, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) async {
    final loaded = await _hydrationRepository.loadHydrationPeriod(
      query,
      refreshMode: refreshMode,
    );
    return loaded.flatMap((data) async {
      // The nutrition read only names the drinks: a failed read degrades the
      // same way a missing permission does — unnamed entries, intact chart.
      final window = query.windows.current;
      final nutritionEntries = (await _nutritionRepository.loadNutritionEntries(
            window.start,
            window.end,
          ))
              .getOrNull() ??
          const <NutritionEntry>[];
      return Ok(HydrationPeriodLoadResult(
        dailyHydration: data.dailyHydration,
        entries: mergeHydrationAndNutrition(
          hydrationEntries: data.hydrationEntries,
          nutritionEntries: nutritionEntries,
        ),
      ));
    });
  }
}
