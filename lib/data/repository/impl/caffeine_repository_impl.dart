import '../../../core/period/time_range.dart';
import '../../../core/result/result.dart';
import '../../../domain/model/caffeine_models.dart';
import '../../../domain/model/nutrition_models.dart';
import '../../../domain/model/refresh_mode.dart';
import '../contract/caffeine_repository.dart';
import '../contract/nutrition_repository.dart';
import 'run_catching.dart';

/// Port of the Kotlin `CaffeineRepositoryImpl`.
///
/// Derives caffeine intake from nutrition entries (Health Connect has no
/// dedicated caffeine record beyond the `caffeine` nutrient), with a 7-day
/// modelling look-back so decay curves have context.
///
/// The nutrition read already returns a [Result]; its failure is rethrown via
/// `orThrow` inside [runCatching], so the boundary stays a single wrap.
class CaffeineRepositoryImpl
    with CaffeineRepositoryDefaults
    implements CaffeineRepository {
  CaffeineRepositoryImpl(this._nutritionRepository);

  static const int _modelingLookbackDays = 7;

  final NutritionRepository _nutritionRepository;

  @override
  Future<Result<CaffeinePeriodData>> loadCaffeineData(
    DatePeriod period, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) =>
      runCatching(() async {
        final entries = (await _nutritionRepository.loadNutritionEntries(
          period.start.minusDays(_modelingLookbackDays),
          period.end,
        ))
            .orThrow();
        final caffeineEntries = <CaffeineEntry>[];
        for (final entry in entries) {
          final caffeineGrams = entry.valueFor(NutritionNutrient.caffeine);
          if (caffeineGrams == null || caffeineGrams <= 0) continue;
          caffeineEntries.add(
            CaffeineEntry(
              id: entry.id,
              startTime: entry.time,
              endTime: entry.endTime,
              caffeineMg: caffeineGrams * 1000.0,
              name: entry.name,
              source: entry.source,
              mealType: entry.mealType,
              clientRecordId: entry.clientRecordId,
              isOpenVitalsEntry: entry.isOpenVitalsEntry,
            ),
          );
        }
        return CaffeinePeriodData(entries: caffeineEntries);
      });
}
