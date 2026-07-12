import 'dart:async';
import 'dart:math' as math;

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../core/presentation/screen_error.dart';
import '../../../core/time/local_date.dart';
import '../../../di/providers.dart';
import '../../../domain/model/caffeine_models.dart';
import '../../../domain/model/nutrition_models.dart';
import '../../../domain/usecase/edit_custom_hydration_drinks_use_case.dart';
import '../../../domain/usecase/load_frequent_hydration_drinks_use_case.dart';
import '../../../domain/usecase/save_hydration_entry_use_case.dart';

// The write path's vocabulary — its errors, its outcome, its volume limits — is
// the use case's; the screens and the quick-beverage home widget read it from
// here.
export '../../../domain/usecase/save_hydration_entry_use_case.dart';

part 'hydration_entry_notifier.freezed.dart';

/// Kotlin `MaxCustomDrinkNutrientValue`.
const double kMaxCustomDrinkNutrientValue = 10000.0;

/// Kotlin `DefaultPartialHydrationImpactPercent`.
const int kDefaultPartialHydrationImpactPercent = 50;

/// Port of the Kotlin `HydrationContainerOption`.
@freezed
abstract class HydrationContainerOption with _$HydrationContainerOption {
  const HydrationContainerOption._();

  const factory HydrationContainerOption({
    required String id,
    required double volumeMilliliters,
  }) = _HydrationContainerOption;

  double get volumeLiters => volumeMilliliters / kMillilitersPerLiter;
}

/// The seven default containers (Kotlin `HydrationContainerOption.Defaults`).
const List<HydrationContainerOption> kDefaultHydrationContainers =
    <HydrationContainerOption>[
  HydrationContainerOption(id: 'coffee_cup', volumeMilliliters: 100.0),
  HydrationContainerOption(id: 'tea_cup', volumeMilliliters: 150.0),
  HydrationContainerOption(id: 'small_cup', volumeMilliliters: 175.0),
  HydrationContainerOption(id: 'medium_glass', volumeMilliliters: 200.0),
  HydrationContainerOption(id: 'large_glass', volumeMilliliters: 300.0),
  HydrationContainerOption(id: 'water_bottle', volumeMilliliters: 500.0),
  HydrationContainerOption(id: 'large_bottle', volumeMilliliters: 1000.0),
];

/// Riverpod port of the Kotlin `HydrationEntryUiState`.
@freezed
abstract class HydrationEntryState with _$HydrationEntryState {
  const HydrationEntryState._();

  const factory HydrationEntryState({
    @Default(true) bool isCheckingPermission,
    @Default(<String>{}) Set<String> hydrationWritePermissions,
    @Default(<String>{}) Set<String> nutritionWritePermissions,
    @Default(false) bool canWriteHydration,
    @Default(false) bool canWriteNutrition,
    @Default(0.0) double todayHydrationLiters,
    @Default(2.0) double dailyGoalLiters,
    @Default(false) bool isSavingEntry,
    @Default(kDefaultHydrationContainers)
    List<HydrationContainerOption> containerOptions,
    required HydrationContainerOption selectedContainer,
    double? lastCustomAmountMilliliters,
    @Default(<CustomHydrationDrink>[])
    List<CustomHydrationDrink> customDrinkOptions,
    /// The most-logged saved drinks, derived from Health Connect entries.
    @Default(<CustomHydrationDrink>[])
    List<CustomHydrationDrink> frequentDrinkOptions,
    String? editRecordId,
    DateTime? editTime,
    @Default(false) bool saveCompleted,
    HydrationEntryNotice? entryNotice,
    HydrationEntryError? entryError,
    ScreenError? writeError,
  }) = _HydrationEntryState;

  bool get isEditMode => editRecordId != null;

  Set<String> get writePermissions =>
      {...hydrationWritePermissions, ...nutritionWritePermissions};
}

/// Port of the Kotlin `isValidCustomDrinkNutrientValue`.
bool isValidCustomDrinkNutrientValue(double value) =>
    value > 0.0 && value <= kMaxCustomDrinkNutrientValue && value.isFinite;

/// The user-authored fields of a custom drink. Port of the Kotlin
/// `CustomHydrationDrinkInput`.
class CustomHydrationDrinkInput {
  const CustomHydrationDrinkInput({
    required this.name,
    required this.volumeMilliliters,
    this.hydrationMultiplier = kFullHydrationImpactMultiplier,
    this.category,
    this.nutrientValues = const <NutritionNutrient, double>{},
  });

  final String name;
  final double volumeMilliliters;
  final double hydrationMultiplier;
  final CaffeineSourceCategory? category;
  final Map<NutritionNutrient, double> nutrientValues;
}

/// Validates and normalizes [input] into a drink, or null when any field is out
/// of range. Port of the Kotlin `CustomHydrationDrinkInput.toCustomHydrationDrink`:
/// a single invalid nutrient value rejects the whole drink rather than silently
/// dropping that nutrient.
CustomHydrationDrink? customHydrationDrinkFromInput(
  CustomHydrationDrinkInput input, {
  required String id,
}) {
  final name = input.name.trim();
  if (name.isEmpty) return null;
  if (!isValidHydrationContainerMilliliters(input.volumeMilliliters)) return null;
  if (!isValidCustomDrinkHydrationMultiplier(input.hydrationMultiplier)) {
    return null;
  }
  final valid = <NutritionNutrient, double>{
    for (final entry in input.nutrientValues.entries)
      if (isValidCustomDrinkNutrientValue(entry.value)) entry.key: entry.value,
  };
  if (valid.length != input.nutrientValues.length) return null;
  // Kotlin sorts by the enum-constant name so persisted maps round-trip stably.
  final sortedKeys = valid.keys.toList()..sort((a, b) => a.name.compareTo(b.name));
  return CustomHydrationDrink(
    id: id,
    name: name,
    volumeMilliliters: input.volumeMilliliters,
    hydrationMultiplier: input.hydrationMultiplier,
    category: input.category,
    nutrientValues: {for (final key in sortedKeys) key: valid[key]!},
  );
}

/// How much a drink counts toward hydration. Port of the Kotlin
/// `HydrationImpactOption`.
enum HydrationImpactOption { full, partial, none }

/// Port of the Kotlin `hydrationImpactOptionForMultiplier`.
HydrationImpactOption hydrationImpactOptionForMultiplier(double multiplier) {
  if (multiplier <= 0.0) return HydrationImpactOption.none;
  if ((multiplier - kFullHydrationImpactMultiplier).abs() < 0.0001) {
    return HydrationImpactOption.full;
  }
  return HydrationImpactOption.partial;
}

/// Port of the Kotlin `hydrationImpactMultiplier`: null when a partial percent
/// is not a number strictly between 0 and 100.
double? hydrationImpactMultiplier(
  HydrationImpactOption option,
  String percentText,
) {
  switch (option) {
    case HydrationImpactOption.full:
      return kFullHydrationImpactMultiplier;
    case HydrationImpactOption.none:
      return 0.0;
    case HydrationImpactOption.partial:
      final percent =
          double.tryParse(percentText.trim().replaceAll(',', '.'));
      if (percent == null || percent <= 0.0 || percent >= 100.0) return null;
      return percent / 100.0;
  }
}

/// Port of the Kotlin `hydrationImpactPercentText`.
String hydrationImpactPercentText(double multiplier) {
  if (multiplier > 0.0 && multiplier < kFullHydrationImpactMultiplier) {
    return (multiplier * 100.0).round().clamp(1, 99).toString();
  }
  return kDefaultPartialHydrationImpactPercent.toString();
}

/// Identifier for a newly-created custom drink. The Kotlin uses `UUID.randomUUID()`;
/// there is no uuid dependency here, so this composes the microsecond clock with
/// random bits — unique enough for a per-user drink list.
String _newCustomDrinkId() {
  final stamp = DateTime.now().microsecondsSinceEpoch.toRadixString(16);
  final suffix =
      math.Random().nextInt(1 << 32).toRadixString(16).padLeft(8, '0');
  return '$stamp-$suffix';
}

/// Riverpod port of the Kotlin `HydrationEntryViewModel`. One instance per entry
/// screen, bound to an optional edit record id.
class HydrationEntryNotifier extends Notifier<HydrationEntryState> {
  HydrationEntryNotifier({this.editRecordId});

  final String? editRecordId;

  @override
  HydrationEntryState build() {
    // Synchronous: the containers and the goal are configuration, and they are
    // painted on the first frame — see [ReadHydrationEntrySettingsUseCase], which
    // also drops any stored size the entry path would refuse to log.
    final settings = ref.read(readHydrationEntrySettingsUseCaseProvider)();
    final options = _containerOptions(settings.containerVolumeOverridesMilliliters);
    final initial = HydrationEntryState(
      containerOptions: options,
      selectedContainer: options.first,
      dailyGoalLiters: ref.read(readHydrationDailyGoalUseCaseProvider)(),
      lastCustomAmountMilliliters: settings.lastCustomAmountMilliliters,
      // Loaded below: the drink catalog lives in the drift-backed beverage
      // store, which seeds its presets on first read.
      customDrinkOptions: const <CustomHydrationDrink>[],
      editRecordId: editRecordId,
    );
    Future.microtask(() async {
      if (!ref.mounted) return;
      await refreshPermission();
      if (ref.mounted) await refreshTodayHydration();
      if (ref.mounted) await _loadEditEntry();
      if (ref.mounted) await _refreshDrinkOptions();
    });
    return initial;
  }

  Future<void> refreshPermission() async {
    state = state.copyWith(
      isCheckingPermission: true,
      entryError: null,
      writeError: null,
    );
    // A drink is two records with two permissions, and one failed probe sinks
    // both verdicts — see [CheckHydrationWriteAccessUseCase].
    final access = await ref.read(checkHydrationWriteAccessUseCaseProvider)();
    if (!ref.mounted) return;
    final error = access.error;
    state = state.copyWith(
      isCheckingPermission: false,
      hydrationWritePermissions: access.hydrationPermissions,
      nutritionWritePermissions: access.nutritionPermissions,
      canWriteHydration: access.canWriteHydration,
      canWriteNutrition: access.canWriteNutrition,
      entryError: error == null ? null : HydrationEntryError.writeFailed,
      writeError: error == null ? null : throwableToScreenError(error),
    );
  }

  Future<void> refreshTodayHydration() async {
    try {
      final liters = await ref.read(loadTodayHydrationUseCaseProvider)();
      if (!ref.mounted) return;
      state = state.copyWith(todayHydrationLiters: liters);
    } catch (_) {
      // Best-effort; ignore failures (matches Kotlin `runCatching { }`).
    }
  }

  /// Re-reads the persisted daily goal (Kotlin `refreshDailyGoal`).
  void refreshDailyGoal() {
    state = state.copyWith(
      dailyGoalLiters: ref.read(readHydrationDailyGoalUseCaseProvider)(),
    );
  }

  void selectContainer(HydrationContainerOption container) {
    state = state.copyWith(
      selectedContainer: container,
      saveCompleted: false,
      entryNotice: null,
      entryError: null,
      writeError: null,
    );
  }

  /// Resizes [container]. Only the seven defaults persist their override; an
  /// ad-hoc container (the one synthesized while editing an entry) is
  /// session-only. Port of the Kotlin `updateContainerSize`.
  ///
  /// No screen calls this: the Kotlin ViewModel keeps the method but its UI
  /// dropped the container presets, so the tracker card has no size editor.
  void updateContainerSize(
    HydrationContainerOption container,
    double milliliters,
  ) {
    if (!isValidHydrationContainerMilliliters(milliliters)) {
      state = state.copyWith(
        entryError: HydrationEntryError.invalidAmount,
        entryNotice: null,
        writeError: null,
      );
      return;
    }
    final updated = container.copyWith(volumeMilliliters: milliliters);
    if (kDefaultHydrationContainers.any((it) => it.id == container.id)) {
      ref.read(saveHydrationContainerSizeUseCaseProvider)(
        container.id,
        milliliters,
      );
    }
    state = state.copyWith(
      containerOptions: [
        for (final option in state.containerOptions)
          if (option.id == container.id) updated else option,
      ],
      selectedContainer: updated,
      saveCompleted: false,
      entryNotice: null,
      entryError: null,
      writeError: null,
    );
  }

  /// Creates a drink, or updates [existingDrinkId] in place (preserving its
  /// `isPreloaded` flag). Port of the Kotlin `saveCustomDrink`.
  Future<void> saveCustomDrink(
    CustomHydrationDrinkInput input, {
    String? existingDrinkId,
  }) async {
    final existing = existingDrinkId == null
        ? null
        : state.customDrinkOptions
            .where((it) => it.id == existingDrinkId)
            .firstOrNull;
    final drink = customHydrationDrinkFromInput(
      input,
      id: existingDrinkId ?? _newCustomDrinkId(),
    )?.copyWith(isPreloaded: existing?.isPreloaded ?? false);
    if (drink == null) {
      state = state.copyWith(
        entryError: HydrationEntryError.invalidCustomDrink,
        entryNotice: null,
        writeError: null,
      );
      return;
    }
    await ref.read(editCustomHydrationDrinksUseCaseProvider)(
      SaveCustomHydrationDrink(drink),
    );
    await _refreshDrinkOptions();
  }

  /// Port of the Kotlin `deleteCustomDrink`.
  Future<void> deleteCustomDrink(CustomHydrationDrink drink) async {
    await ref.read(editCustomHydrationDrinksUseCaseProvider)(
      DeleteCustomHydrationDrink(drink.id),
    );
    await _refreshDrinkOptions();
  }

  /// Drops [drinkId] onto [targetDrinkId]'s slot and persists the new order.
  /// Port of the Kotlin `moveCustomDrinkToTarget`.
  Future<void> moveCustomDrinkToTarget(
    String drinkId,
    String targetDrinkId,
  ) async {
    if (drinkId == targetDrinkId) return;
    final current = state.customDrinkOptions;
    final from = current.indexWhere((it) => it.id == drinkId);
    final target = current.indexWhere((it) => it.id == targetDrinkId);
    if (from < 0 || target < 0) return;
    final updated = [...current];
    updated.insert(target.clamp(0, updated.length - 1), updated.removeAt(from));
    unawaited(ref.read(editCustomHydrationDrinksUseCaseProvider)(
      ReorderCustomHydrationDrinks([for (final it in updated) it.id]),
    ));
    state = state.copyWith(
      customDrinkOptions: updated,
      entryError: null,
      entryNotice: null,
      writeError: null,
      saveCompleted: false,
    );
  }

  /// Port of the Kotlin `moveCustomDrinkToCategory`.
  Future<void> moveCustomDrinkToCategory(
    String drinkId,
    CaffeineSourceCategory? category,
  ) async {
    await ref.read(editCustomHydrationDrinksUseCaseProvider)(
      RecategorizeCustomHydrationDrink(drinkId, category),
    );
    await _refreshDrinkOptions();
  }

  /// Re-reads the persisted drinks and clears any transient entry feedback.
  /// Port of the Kotlin `refreshDrinkOptions` (minus the frequent-drink pass,
  /// which the catalog carousel owns and is not ported yet).
  Future<void> _refreshDrinkOptions() async {
    // Already filtered to the drinks that can actually be logged — see
    // [LoadCustomHydrationDrinksUseCase].
    final drinks = await ref.read(loadCustomHydrationDrinksUseCaseProvider)();
    if (!ref.mounted) return;
    final drinkIds = {for (final drink in drinks) drink.id};
    state = state.copyWith(
      customDrinkOptions: drinks,
      // Drop any frequent entry whose drink just disappeared, then re-derive.
      frequentDrinkOptions: [
        for (final drink in state.frequentDrinkOptions)
          if (drinkIds.contains(drink.id)) drink,
      ],
      entryError: null,
      entryNotice: null,
      writeError: null,
      saveCompleted: false,
    );
    unawaited(refreshFrequentDrinkOptions());
  }

  /// Re-derives the frequently-consumed drinks from the recent hydration +
  /// nutrition entries — see [LoadFrequentHydrationDrinksUseCase] for why both
  /// halves are needed. Port of the Kotlin `refreshFrequentDrinkOptions`;
  /// failures are swallowed, as there (`runCatching`), because the ranking is a
  /// convenience and the previous list is still perfectly usable.
  Future<void> refreshFrequentDrinkOptions() async {
    if (state.customDrinkOptions.isEmpty) {
      state = state.copyWith(frequentDrinkOptions: const <CustomHydrationDrink>[]);
      return;
    }
    try {
      final frequent =
          await ref.read(loadFrequentHydrationDrinksUseCaseProvider)(
        state.customDrinkOptions,
      );
      if (!ref.mounted) return;
      state = state.copyWith(frequentDrinkOptions: frequent);
    } catch (_) {
      // Best-effort ranking; leave the previous list in place.
    }
  }

  void updateEntryTime(DateTime time) {
    final now = DateTime.now();
    state = state.copyWith(
      editTime: time.isAfter(now) ? now : time,
      saveCompleted: false,
      entryNotice: null,
      entryError: null,
      writeError: null,
    );
  }

  /// Saves the currently-selected container's volume.
  Future<void> addSelectedHydrationEntry() =>
      _saveHydrationEntry(rawLiters: state.selectedContainer.volumeLiters);

  /// Selects [container] and (outside edit mode) immediately logs it.
  Future<void> addContainerHydrationEntry(
    HydrationContainerOption container,
  ) async {
    if (state.isEditMode) {
      selectContainer(container);
      return;
    }
    state = state.copyWith(
      selectedContainer: container,
      saveCompleted: false,
      entryNotice: null,
      entryError: null,
      writeError: null,
    );
    await _saveHydrationEntry(rawLiters: container.volumeLiters);
  }

  /// Logs a free-form custom amount, remembering it as the last custom amount.
  Future<void> addCustomHydrationEntry(double milliliters) async {
    if (!isValidHydrationContainerMilliliters(milliliters)) {
      state = state.copyWith(
        entryError: HydrationEntryError.invalidAmount,
        entryNotice: null,
        writeError: null,
      );
      return;
    }
    state = state.copyWith(
      lastCustomAmountMilliliters: milliliters,
      entryNotice: null,
    );
    ref.read(saveLastCustomHydrationAmountUseCaseProvider)(milliliters);
    await _saveHydrationEntry(rawLiters: milliliters / kMillilitersPerLiter);
  }

  /// Logs a saved custom drink (with its hydration multiplier + nutrients),
  /// scaling the nutrient values to [amountMilliliters]. Port of the Kotlin
  /// `addSavedCustomDrinkEntry` / `logCustomDrinkEntry`.
  Future<void> addSavedCustomDrinkEntry(
    CustomHydrationDrink drink, {
    double? amountMilliliters,
    DateTime? entryTime,
  }) async {
    final amount = amountMilliliters ?? drink.volumeMilliliters;
    if (!isValidCustomHydrationDrink(drink)) {
      state = state.copyWith(
        entryError: HydrationEntryError.invalidCustomDrink,
        entryNotice: null,
        writeError: null,
      );
      return;
    }
    if (!isValidHydrationContainerMilliliters(amount)) {
      state = state.copyWith(
        entryError: HydrationEntryError.invalidAmount,
        entryNotice: null,
        writeError: null,
      );
      return;
    }
    final portionMultiplier = amount / drink.volumeMilliliters;
    final scaledNutrients = drink.nutrientValues
        .map((key, value) => MapEntry(key, value * portionMultiplier));
    state = state.copyWith(
      lastCustomAmountMilliliters: amount,
      entryNotice: null,
    );
    ref.read(saveLastCustomHydrationAmountUseCaseProvider)(amount);
    await _saveHydrationEntry(
      rawLiters: amount / kMillilitersPerLiter,
      hydrationMultiplier: drink.hydrationMultiplier,
      drinkId: drink.id,
      nutritionName: drink.name,
      nutrientValues: scaledNutrients,
      requestedEntryTime: entryTime,
    );
  }

  void onSaveCompletedHandled() {
    state = state.copyWith(saveCompleted: false);
  }

  Future<void> _saveHydrationEntry({
    required double rawLiters,
    double hydrationMultiplier = 1.0,
    String? drinkId,
    String? nutritionName,
    Map<NutritionNutrient, double> nutrientValues =
        const <NutritionNutrient, double>{},
    DateTime? requestedEntryTime,
  }) async {
    final current = state;
    state = state.copyWith(
      isSavingEntry: true,
      saveCompleted: false,
      entryNotice: null,
      entryError: null,
      writeError: null,
    );
    try {
      // Both halves of the drink — the volume and its nutrients — are written by
      // the use case; see [SaveHydrationEntryUseCase] for why they cannot be
      // written apart.
      final outcome = await ref.read(saveHydrationEntryUseCaseProvider)(
        rawLiters: rawLiters,
        hydrationMultiplier: hydrationMultiplier,
        drinkId: drinkId,
        nutritionName: nutritionName,
        nutrientValues: nutrientValues,
        requestedEntryTime: requestedEntryTime,
        fallbackEntryTime: current.editTime,
        editRecordId: current.editRecordId,
        canWriteHydration: current.canWriteHydration,
        canWriteNutrition: current.canWriteNutrition,
      );
      if (!ref.mounted) return;
      if (outcome is HydrationDrinkLogInvalid) {
        state = state.copyWith(
          isSavingEntry: false,
          entryError: outcome.error,
          entryNotice: null,
          writeError: null,
        );
        return;
      }
      final success = outcome as HydrationDrinkLogSuccess;
      state = state.copyWith(
        isSavingEntry: false,
        todayHydrationLiters:
            (current.isEditMode || !_isToday(success.entryTime))
                ? state.todayHydrationLiters
                : state.todayHydrationLiters + success.effectiveLiters,
        saveCompleted: true,
        entryNotice: success.notice,
        entryError: null,
        writeError: null,
      );
      // "Saving a hydration entry can automatically hide an active hydration
      // reminder" (the Kotlin reminders doc). The state above is already
      // published, and the call swallows its own failures, so awaiting here
      // cannot turn a completed write into a save error.
      await _hideHydrationReminder();
    } catch (error) {
      if (!ref.mounted) return;
      state = state.copyWith(
        isSavingEntry: false,
        entryError: HydrationEntryError.writeFailed,
        entryNotice: null,
        writeError: throwableToScreenError(error),
      );
    }
  }

  /// Dismisses a visible hydration reminder after a successful save. Leaves the
  /// alarm chain armed, so the next reminder still fires.
  Future<void> _hideHydrationReminder() async {
    try {
      await ref.read(hydrationReminderControllerProvider).hideReminderNotification();
    } catch (_) {
      // Notifications are a nicety; never fail a completed write over them.
    }
  }

  Future<void> _loadEditEntry() async {
    final recordId = editRecordId;
    if (recordId == null) return;
    try {
      final entry = await ref.read(loadHydrationEntryForEditUseCaseProvider)(
        recordId,
      );
      if (!ref.mounted) return;
      // Null covers both "no such entry" and "not ours to edit".
      if (entry == null) {
        state = state.copyWith(
          entryError: HydrationEntryError.writeFailed,
          writeError: const ScreenErrorMessage(
            'Only OpenVitals entries can be edited.',
          ),
        );
        return;
      }
      final existingOptions = state.containerOptions;
      final match = existingOptions.where(
        (o) => (o.volumeLiters - entry.liters).abs() < 0.0001,
      );
      final option = match.isNotEmpty
          ? match.first
          : HydrationContainerOption(
              id: 'current_entry',
              volumeMilliliters: entry.liters * kMillilitersPerLiter,
            );
      final options = <HydrationContainerOption>[
        option,
        ...existingOptions,
      ];
      final seen = <String>{};
      final deduped = <HydrationContainerOption>[];
      for (final o in options) {
        if (seen.add(o.id)) deduped.add(o);
      }
      final now = DateTime.now();
      state = state.copyWith(
        containerOptions: deduped,
        selectedContainer: option,
        editTime: entry.startTime.isAfter(now) ? now : entry.startTime,
        entryError: null,
        writeError: null,
      );
    } catch (error) {
      if (!ref.mounted) return;
      state = state.copyWith(
        entryError: HydrationEntryError.writeFailed,
        writeError: throwableToScreenError(error),
      );
    }
  }

  /// The seven defaults, resized by whatever the user has persisted.
  /// [overridesMilliliters] arrives already filtered to the sizes that can still
  /// be logged.
  List<HydrationContainerOption> _containerOptions(
    Map<String, double> overridesMilliliters,
  ) {
    return kDefaultHydrationContainers.map((option) {
      final override = overridesMilliliters[option.id];
      if (override != null) {
        return option.copyWith(volumeMilliliters: override);
      }
      return option;
    }).toList();
  }

  bool _isToday(DateTime time) {
    final today = LocalDate.now();
    final local = time.toLocal();
    return local.year == today.year &&
        local.month == today.month &&
        local.day == today.day;
  }
}
