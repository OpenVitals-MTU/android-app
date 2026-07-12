import '../../core/time/local_date.dart';
import '../../data/repository/contract/hydration_repository.dart';
import '../../data/repository/contract/nutrition_repository.dart';
import '../hydration/hydration_drink_usage.dart';
import '../model/nutrition_models.dart';

/// Ranks the user's saved drinks by how often they have actually been logged.
///
/// The ranking cannot be read off the drink catalog — the catalog only knows
/// what drinks exist, not which ones get drunk. The evidence is in the entries,
/// and a logged drink is split across two record types (the hydration volume and
/// the nutrition record carrying its name), so counting it needs both
/// repositories: a coffee logged as non-hydrating leaves only a nutrition record
/// behind, and would be invisible to a hydration-only count.
///
/// Only the last [kFrequentHydrationDrinkLookbackDays] count, so the carousel
/// follows what the user drinks *now* rather than what they drank a year ago.
class LoadFrequentHydrationDrinksUseCase {
  const LoadFrequentHydrationDrinksUseCase(
    this._hydrationRepository,
    this._nutritionRepository,
  );

  final HydrationRepository _hydrationRepository;
  final NutritionRepository _nutritionRepository;

  Future<List<CustomHydrationDrink>> call(
    List<CustomHydrationDrink> drinks,
  ) async {
    final end = LocalDate.now();
    final start = end.minusDays(kFrequentHydrationDrinkLookbackDays - 1);
    final hydrationEntries =
        await _hydrationRepository.loadHydrationEntries(start, end);
    final nutritionEntries =
        await _nutritionRepository.loadNutritionEntries(start, end);
    return frequentHydrationDrinkOptions(
      drinks: drinks,
      hydrationEntries: hydrationEntries,
      nutritionEntries: nutritionEntries,
    );
  }
}
