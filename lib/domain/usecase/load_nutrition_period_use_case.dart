import '../../core/period/period_load_query.dart';
import '../../core/result/result.dart';
import '../../data/repository/contract/nutrition_repository.dart';
import '../model/nutrition_models.dart';
import '../model/refresh_mode.dart';

/// One period of nutrition, with the two comparison windows the statistics
/// section is drawn against.
class NutritionPeriodLoadResult {
  const NutritionPeriodLoadResult({
    required this.dailyMacros,
    required this.previousDailyMacros,
    required this.baselineDailyMacros,
    required this.entries,
  });

  final List<DailyMacros> dailyMacros;
  final List<DailyMacros> previousDailyMacros;
  final List<DailyMacros> baselineDailyMacros;
  final List<NutritionEntry> entries;
}

/// Loads one nutrition period, plus the windows it is judged against.
///
/// A macro total means nothing on its own: "2,100 kcal" is only interesting next
/// to the week before it (the comparison) and next to the user's own longer-run
/// habit (the baseline). So this reads three windows, not one — the current
/// period in full (entries included), and the previous and baseline windows as
/// daily macros only, which is all the statistics section plots. Kotlin's
/// `NutritionPresentationMapper` folds in exactly the same two extra windows.
class LoadNutritionPeriodUseCase {
  const LoadNutritionPeriodUseCase(this._nutritionRepository);

  final NutritionRepository _nutritionRepository;

  Future<Result<NutritionPeriodLoadResult>> call(
    PeriodLoadQuery query, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) async {
    final loaded = await _nutritionRepository.loadNutritionPeriod(
      query,
      refreshMode: refreshMode,
    );
    return loaded.flatMap((data) async {
      // The comparison windows are enrichment: a failed secondary read keeps
      // its window empty rather than sinking the period that did load.
      final windows = query.windows;
      final previousMacros = (await _nutritionRepository.loadDailyMacros(
            windows.previous.start,
            windows.previous.end,
          ))
              .getOrNull() ??
          const <DailyMacros>[];
      final baselineMacros = (await _nutritionRepository.loadDailyMacros(
            windows.baseline.start,
            windows.baseline.end,
          ))
              .getOrNull() ??
          const <DailyMacros>[];
      return Ok(NutritionPeriodLoadResult(
        dailyMacros: data.dailyMacros,
        previousDailyMacros: previousMacros,
        baselineDailyMacros: baselineMacros,
        entries: data.entries,
      ));
    });
  }
}
