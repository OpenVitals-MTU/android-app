import '../../core/result/result.dart';
import '../../data/repository/contract/hydration_repository.dart';
import '../model/nutrition_models.dart';
import 'save_hydration_entry_use_case.dart';

/// The saved-drink catalog, filtered to the drinks that can actually be logged.
///
/// The catalog is persisted (drift), which means it long outlives the rules that
/// validate it: a drink written by an older build, or seeded from the bundled
/// beverage list, can carry a volume or a hydration multiplier that today's entry
/// path would refuse. Showing it anyway produces a tile that does nothing when
/// tapped, so the same predicate that guards the write — [isValidCustomHydrationDrink]
/// — guards the read.
class LoadCustomHydrationDrinksUseCase {
  const LoadCustomHydrationDrinksUseCase(this._hydrationRepository);

  final HydrationRepository _hydrationRepository;

  Future<Result<List<CustomHydrationDrink>>> call() async {
    final drinks = await _hydrationRepository.customHydrationDrinks();
    return drinks.map(
      (catalog) => catalog.where(isValidCustomHydrationDrink).toList(),
    );
  }
}
