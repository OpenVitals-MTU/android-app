import '../../data/repository/contract/hydration_repository.dart';
import '../../data/repository/contract/nutrition_repository.dart';
import '../model/nutrition_models.dart';

const double kMillilitersPerLiter = 1000.0;
const double _maxHealthConnectHydrationLiters = 100.0;
const double kMinHydrationContainerMilliliters = 1.0;
const double kMaxHydrationContainerMilliliters =
    _maxHealthConnectHydrationLiters * kMillilitersPerLiter;

/// Kotlin `FullHydrationImpactMultiplier`.
const double kFullHydrationImpactMultiplier = 1.0;

/// Port of the Kotlin `HydrationEntryError`.
enum HydrationEntryError {
  invalidAmount,
  invalidCustomDrink,
  missingWritePermission,
  missingNutritionWritePermission,
  writeFailed,
}

/// Port of the Kotlin `HydrationEntryNotice`.
enum HydrationEntryNotice {
  nonHydratingDrinkSaved,
}

/// Whether [milliliters] is an acceptable hydration volume. Port of the Kotlin
/// `isValidHydrationContainerMilliliters`.
bool isValidHydrationContainerMilliliters(double milliliters) =>
    milliliters >= kMinHydrationContainerMilliliters &&
    milliliters <= kMaxHydrationContainerMilliliters &&
    milliliters.isFinite;

/// A drink counts for anywhere between none of its volume and all of it — a
/// black coffee may be logged as non-hydrating, a squash as fully so. Anything
/// outside that range is not a multiplier.
bool isValidCustomDrinkHydrationMultiplier(double value) =>
    value >= 0.0 && value <= 1.0 && value.isFinite;

/// Whether [drink] can be logged at all. Port of the Kotlin
/// `CustomHydrationDrink.isValidCustomHydrationDrink()`.
bool isValidCustomHydrationDrink(CustomHydrationDrink drink) =>
    drink.id.isNotEmpty &&
    drink.name.isNotEmpty &&
    isValidHydrationContainerMilliliters(drink.volumeMilliliters) &&
    isValidCustomDrinkHydrationMultiplier(drink.hydrationMultiplier);

/// The result of a hydration/nutrition write. Port of the Kotlin
/// `HydrationDrinkLogOutcome`.
sealed class HydrationDrinkLogOutcome {
  const HydrationDrinkLogOutcome();
}

/// Successful hydration/nutrition write result (Kotlin `HydrationDrinkLogSuccess`).
class HydrationDrinkLogSuccess extends HydrationDrinkLogOutcome {
  const HydrationDrinkLogSuccess({
    required this.effectiveLiters,
    required this.entryTime,
    required this.notice,
    required this.wroteHydration,
    required this.wroteNutrition,
  });

  final double effectiveLiters;
  final DateTime entryTime;
  final HydrationEntryNotice? notice;

  /// Which records actually landed. A zero-multiplier drink (e.g. a black
  /// coffee logged as non-hydrating) writes nutrition only — the quick-beverage
  /// widget reports that as "Saved as nutrition".
  final bool wroteHydration;
  final bool wroteNutrition;
}

/// A rejected write (Kotlin `HydrationDrinkLogOutcome.Invalid`).
class HydrationDrinkLogInvalid extends HydrationDrinkLogOutcome {
  const HydrationDrinkLogInvalid(this.error);
  final HydrationEntryError error;
}

/// Logs a drink: the volume, and the nutrients that came with it.
///
/// A drink is not one record. Health Connect's `HydrationRecord` is a volume and
/// nothing else — no name, no caffeine — so OpenVitals writes the drink's name
/// and nutrients as a second, `NutritionRecord`, tied back to the first by its
/// client record id. Both halves therefore have to be written here, together,
/// which is why this needs the hydration *and* the nutrition repository: a save
/// path that only knew about hydration would silently drop a coffee's caffeine.
///
/// The multiplier is what makes the two halves come apart. A drink counts for
/// only part of its volume (a coffee at 50%) or none of it (a spirit at 0%), so:
///
/// - a fully non-hydrating drink writes **nutrition only** — hence the
///   `nonHydratingDrinkSaved` notice, so the UI can say "saved as nutrition"
///   rather than claim a hydration entry that does not exist;
/// - editing an existing entry, by contrast, must still produce a hydration
///   record — there is one on file to update — so a zero volume in edit mode is
///   rejected rather than turned into a nutrition-only write.
///
/// Permissions are checked per half, and only for the half that would actually
/// be written: logging a plain glass of water must not fail for want of a
/// nutrition permission it never uses.
///
/// This returns an outcome rather than throwing on a rejected write: an invalid
/// amount or a missing permission is a message for the entry screen, not an
/// exception. Only a write that genuinely fails mid-flight propagates.
///
/// Shared, via [logCustomHydrationDrinkEntry], with the quick-beverage home
/// widget — so that a widget tap cannot take a different (and lossier) path than
/// the entry screen.
class SaveHydrationEntryUseCase {
  const SaveHydrationEntryUseCase(
    this._hydrationRepository,
    this._nutritionRepository,
  );

  final HydrationRepository _hydrationRepository;
  final NutritionRepository _nutritionRepository;

  Future<HydrationDrinkLogOutcome> call({
    required double rawLiters,
    required double hydrationMultiplier,
    String? drinkId,
    String? nutritionName,
    required Map<NutritionNutrient, double> nutrientValues,
    DateTime? requestedEntryTime,
    DateTime? fallbackEntryTime,
    String? editRecordId,
    required bool canWriteHydration,
    required bool canWriteNutrition,
  }) async {
    if (!isValidCustomDrinkHydrationMultiplier(hydrationMultiplier)) {
      return const HydrationDrinkLogInvalid(
        HydrationEntryError.invalidCustomDrink,
      );
    }

    final effectiveLiters = rawLiters * hydrationMultiplier;
    final writesHydration = effectiveLiters > 0.0;
    final writesNutrition = nutrientValues.isNotEmpty;

    if (editRecordId != null && !writesHydration) {
      return const HydrationDrinkLogInvalid(HydrationEntryError.invalidAmount);
    }
    if (writesHydration && !canWriteHydration) {
      return const HydrationDrinkLogInvalid(
        HydrationEntryError.missingWritePermission,
      );
    }
    if (writesNutrition && !canWriteNutrition) {
      return const HydrationDrinkLogInvalid(
        HydrationEntryError.missingNutritionWritePermission,
      );
    }
    if (writesHydration &&
        effectiveLiters >
            kMaxHydrationContainerMilliliters / kMillilitersPerLiter) {
      return const HydrationDrinkLogInvalid(HydrationEntryError.invalidAmount);
    }
    if (!writesHydration && !writesNutrition) {
      return const HydrationDrinkLogInvalid(
        HydrationEntryError.invalidCustomDrink,
      );
    }

    final now = DateTime.now();
    DateTime coerce(DateTime t) => t.isAfter(now) ? now : t;
    final entryTime = requestedEntryTime != null
        ? coerce(requestedEntryTime)
        : (fallbackEntryTime != null ? coerce(fallbackEntryTime) : now);

    if (editRecordId == null) {
      String? hydrationClientRecordId;
      if (writesHydration) {
        hydrationClientRecordId = await _hydrationRepository.writeHydrationEntry(
          HydrationWriteRequest(
            time: entryTime,
            volumeLiters: effectiveLiters,
            drinkId: drinkId,
          ),
        );
      }
      if (writesNutrition) {
        await _nutritionRepository.writeNutritionEntry(
          NutritionWriteRequest(
            time: entryTime,
            nutrientValues: nutrientValues,
            name: nutritionName,
            associatedHydrationClientRecordId: hydrationClientRecordId,
          ),
        );
      }
    } else {
      await _hydrationRepository.updateHydrationEntry(
        editRecordId,
        HydrationWriteRequest(time: entryTime, volumeLiters: effectiveLiters),
      );
    }

    return HydrationDrinkLogSuccess(
      effectiveLiters: effectiveLiters,
      entryTime: entryTime,
      notice: (!writesHydration && writesNutrition)
          ? HydrationEntryNotice.nonHydratingDrinkSaved
          : null,
      wroteHydration: writesHydration,
      wroteNutrition: writesNutrition,
    );
  }
}

/// Logs one saved drink at its own volume — the whole drink, nutrients included.
/// Port of the Kotlin `logCustomHydrationDrinkEntry` (HydrationDrinkLogger).
///
/// A free function, not a Riverpod-wired use case, because its other caller is
/// the quick-beverage home widget: that runs in a background isolate which
/// builds its own repositories, and it must go through the very same write path
/// as the entry screen or a widget tap would silently drop the drink's caffeine.
Future<HydrationDrinkLogOutcome> logCustomHydrationDrinkEntry({
  required HydrationRepository hydrationRepository,
  required NutritionRepository nutritionRepository,
  required CustomHydrationDrink drink,
  required bool canWriteHydration,
  required bool canWriteNutrition,
  DateTime? entryTime,
}) async {
  if (!isValidCustomHydrationDrink(drink)) {
    return const HydrationDrinkLogInvalid(
      HydrationEntryError.invalidCustomDrink,
    );
  }
  return SaveHydrationEntryUseCase(hydrationRepository, nutritionRepository)(
    rawLiters: drink.volumeMilliliters / kMillilitersPerLiter,
    hydrationMultiplier: drink.hydrationMultiplier,
    drinkId: drink.id,
    nutritionName: drink.name,
    nutrientValues: drink.nutrientValues,
    requestedEntryTime: entryTime,
    canWriteHydration: canWriteHydration,
    canWriteNutrition: canWriteNutrition,
  );
}
