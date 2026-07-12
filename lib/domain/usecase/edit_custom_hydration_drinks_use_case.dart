import '../../core/result/result.dart';
import '../../data/repository/contract/hydration_repository.dart';
import '../model/caffeine_models.dart';
import '../model/nutrition_models.dart';

/// One edit to the saved-drink catalog.
sealed class CustomHydrationDrinkEdit {
  const CustomHydrationDrinkEdit();
}

/// Adds [drink], or overwrites the drink already stored under its id. Create and
/// update are one operation because the id decides which it is, and the caller
/// (a form with an optional "editing" id) does not want to know.
class SaveCustomHydrationDrink extends CustomHydrationDrinkEdit {
  const SaveCustomHydrationDrink(this.drink);

  final CustomHydrationDrink drink;
}

class DeleteCustomHydrationDrink extends CustomHydrationDrinkEdit {
  const DeleteCustomHydrationDrink(this.drinkId);

  final String drinkId;
}

/// The catalog is drag-ordered by the user, so its order is data — [drinkIds] is
/// the whole list, in the order it must be restored in.
class ReorderCustomHydrationDrinks extends CustomHydrationDrinkEdit {
  const ReorderCustomHydrationDrinks(this.drinkIds);

  final List<String> drinkIds;
}

/// Files a drink under a caffeine category, or (null) under none.
class RecategorizeCustomHydrationDrink extends CustomHydrationDrinkEdit {
  const RecategorizeCustomHydrationDrink(this.drinkId, this.category);

  final String drinkId;
  final CaffeineSourceCategory? category;
}

/// Applies one edit to the saved-drink catalog.
///
/// The four edits are one use case because they are one thing: every one of them
/// mutates the same persisted catalog, and every one of them invalidates the
/// drink list the screen is holding. Split into four classes they would read as
/// four unrelated capabilities, and the next edit would be a fifth registration
/// in the DI graph rather than a fifth case here.
///
/// Nothing is returned. The catalog is re-read afterwards rather than patched in
/// place — the store owns preset seeding and ordering, so what comes back out is
/// not always what went in.
class EditCustomHydrationDrinksUseCase {
  const EditCustomHydrationDrinksUseCase(this._hydrationRepository);

  final HydrationRepository _hydrationRepository;

  Future<Result<void>> call(CustomHydrationDrinkEdit edit) => switch (edit) {
        SaveCustomHydrationDrink(:final drink) =>
          _hydrationRepository.saveCustomHydrationDrink(drink),
        DeleteCustomHydrationDrink(:final drinkId) =>
          _hydrationRepository.deleteCustomHydrationDrink(drinkId),
        ReorderCustomHydrationDrinks(:final drinkIds) =>
          _hydrationRepository.reorderCustomHydrationDrinks(drinkIds),
        RecategorizeCustomHydrationDrink(:final drinkId, :final category) =>
          _hydrationRepository.moveCustomHydrationDrinkToCategory(
            drinkId,
            category,
          ),
      };
}
