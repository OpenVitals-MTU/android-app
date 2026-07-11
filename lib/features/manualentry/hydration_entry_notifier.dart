import 'dart:async';
import 'dart:math' as math;

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../core/presentation/screen_error.dart';
import '../../core/time/local_date.dart';
import '../../data/repository/contract/hydration_repository.dart';
import '../../data/repository/contract/nutrition_repository.dart';
import '../../di/providers.dart';
import '../../domain/model/caffeine_models.dart';
import '../../domain/model/nutrition_models.dart';
import 'hydration_drink_usage.dart';

part 'hydration_entry_notifier.freezed.dart';

const double kMillilitersPerLiter = 1000.0;
const double _maxHealthConnectHydrationLiters = 100.0;
const double kMinHydrationContainerMilliliters = 1.0;
const double kMaxHydrationContainerMilliliters =
    _maxHealthConnectHydrationLiters * kMillilitersPerLiter;

/// Kotlin `MaxCustomDrinkNutrientValue`.
const double kMaxCustomDrinkNutrientValue = 10000.0;

/// Kotlin `FullHydrationImpactMultiplier` / `DefaultPartialHydrationImpactPercent`.
const double kFullHydrationImpactMultiplier = 1.0;
const int kDefaultPartialHydrationImpactPercent = 50;

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

/// Whether [milliliters] is an acceptable hydration volume. Port of the Kotlin
/// `isValidHydrationContainerMilliliters`.
bool isValidHydrationContainerMilliliters(double milliliters) =>
    milliliters >= kMinHydrationContainerMilliliters &&
    milliliters <= kMaxHydrationContainerMilliliters &&
    milliliters.isFinite;

bool _isValidCustomDrinkHydrationMultiplier(double value) =>
    value >= 0.0 && value <= 1.0 && value.isFinite;

/// Whether [drink] can be logged at all. Port of the Kotlin
/// `CustomHydrationDrink.isValidCustomHydrationDrink()`.
bool isValidCustomHydrationDrink(CustomHydrationDrink drink) =>
    drink.id.isNotEmpty &&
    drink.name.isNotEmpty &&
    isValidHydrationContainerMilliliters(drink.volumeMilliliters) &&
    _isValidCustomDrinkHydrationMultiplier(drink.hydrationMultiplier);

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
  if (!_isValidCustomDrinkHydrationMultiplier(input.hydrationMultiplier)) {
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
    final repo = ref.read(hydrationRepositoryProvider);
    final options = _containerOptions(repo);
    final lastCustom = repo.lastCustomHydrationAmountMilliliters();
    final initial = HydrationEntryState(
      containerOptions: options,
      selectedContainer: options.first,
      dailyGoalLiters: repo.hydrationDailyGoalLiters(),
      lastCustomAmountMilliliters:
          (lastCustom != null && isValidHydrationContainerMilliliters(lastCustom))
              ? lastCustom
              : null,
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
    final hydrationRepo = ref.read(hydrationRepositoryProvider);
    final nutritionRepo = ref.read(nutritionRepositoryProvider);
    state = state.copyWith(
      isCheckingPermission: true,
      entryError: null,
      writeError: null,
    );
    try {
      final canWriteHydration =
          await hydrationRepo.hasHydrationWritePermission();
      final canWriteNutrition =
          await nutritionRepo.hasNutritionWritePermission();
      if (!ref.mounted) return;
      state = state.copyWith(
        isCheckingPermission: false,
        hydrationWritePermissions: hydrationRepo.hydrationWritePermissions,
        nutritionWritePermissions: nutritionRepo.nutritionWritePermissions,
        canWriteHydration: canWriteHydration,
        canWriteNutrition: canWriteNutrition,
      );
    } catch (error) {
      if (!ref.mounted) return;
      state = state.copyWith(
        isCheckingPermission: false,
        hydrationWritePermissions: hydrationRepo.hydrationWritePermissions,
        nutritionWritePermissions: nutritionRepo.nutritionWritePermissions,
        canWriteHydration: false,
        canWriteNutrition: false,
        entryError: HydrationEntryError.writeFailed,
        writeError: throwableToScreenError(error),
      );
    }
  }

  Future<void> refreshTodayHydration() async {
    final today = LocalDate.now();
    try {
      final entries = await ref
          .read(hydrationRepositoryProvider)
          .loadDailyHydration(today, today);
      final liters = entries.fold<double>(0.0, (sum, e) => sum + e.liters);
      if (!ref.mounted) return;
      state = state.copyWith(todayHydrationLiters: liters);
    } catch (_) {
      // Best-effort; ignore failures (matches Kotlin `runCatching { }`).
    }
  }

  /// Re-reads the persisted daily goal (Kotlin `refreshDailyGoal`).
  void refreshDailyGoal() {
    state = state.copyWith(
      dailyGoalLiters:
          ref.read(hydrationRepositoryProvider).hydrationDailyGoalLiters(),
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
      ref
          .read(hydrationRepositoryProvider)
          .setHydrationContainerVolumeMilliliters(container.id, milliliters);
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
    await ref.read(hydrationRepositoryProvider).saveCustomHydrationDrink(drink);
    await _refreshDrinkOptions();
  }

  /// Port of the Kotlin `deleteCustomDrink`.
  Future<void> deleteCustomDrink(CustomHydrationDrink drink) async {
    await ref
        .read(hydrationRepositoryProvider)
        .deleteCustomHydrationDrink(drink.id);
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
    unawaited(ref
        .read(hydrationRepositoryProvider)
        .reorderCustomHydrationDrinks([for (final it in updated) it.id]));
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
    await ref
        .read(hydrationRepositoryProvider)
        .moveCustomHydrationDrinkToCategory(drinkId, category);
    await _refreshDrinkOptions();
  }

  /// Re-reads the persisted drinks and clears any transient entry feedback.
  /// Port of the Kotlin `refreshDrinkOptions` (minus the frequent-drink pass,
  /// which the catalog carousel owns and is not ported yet).
  Future<void> _refreshDrinkOptions() async {
    final loaded =
        await ref.read(hydrationRepositoryProvider).customHydrationDrinks();
    if (!ref.mounted) return;
    final drinks = loaded.where(isValidCustomHydrationDrink).toList();
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

  /// Re-derives the frequently-consumed drinks from the last
  /// [kFrequentHydrationDrinkLookbackDays] of hydration + nutrition entries.
  /// Port of the Kotlin `refreshFrequentDrinkOptions`; failures are swallowed,
  /// as there (`runCatching`), because the ranking is a convenience.
  Future<void> refreshFrequentDrinkOptions() async {
    if (state.customDrinkOptions.isEmpty) {
      state = state.copyWith(frequentDrinkOptions: const <CustomHydrationDrink>[]);
      return;
    }
    final end = LocalDate.now();
    final start = end.minusDays(kFrequentHydrationDrinkLookbackDays - 1);
    try {
      final hydrationEntries = await ref
          .read(hydrationRepositoryProvider)
          .loadHydrationEntries(start, end);
      final nutritionEntries = await ref
          .read(nutritionRepositoryProvider)
          .loadNutritionEntries(start, end);
      if (!ref.mounted) return;
      state = state.copyWith(
        frequentDrinkOptions: frequentHydrationDrinkOptions(
          drinks: state.customDrinkOptions,
          hydrationEntries: hydrationEntries,
          nutritionEntries: nutritionEntries,
        ),
      );
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
    ref
        .read(hydrationRepositoryProvider)
        .setLastCustomHydrationAmountMilliliters(milliliters);
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
    ref
        .read(hydrationRepositoryProvider)
        .setLastCustomHydrationAmountMilliliters(amount);
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
      final outcome = await writeHydrationAndNutritionEntry(
        hydrationRepository: ref.read(hydrationRepositoryProvider),
        nutritionRepository: ref.read(nutritionRepositoryProvider),
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
      final entry =
          await ref.read(hydrationRepositoryProvider).loadHydrationEntry(
                recordId,
              );
      if (!ref.mounted) return;
      if (entry == null || !entry.isOpenVitalsEntry) {
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

  List<HydrationContainerOption> _containerOptions(HydrationRepository repo) {
    final overrides = repo.hydrationContainerVolumeMilliliters();
    return kDefaultHydrationContainers.map((option) {
      final override = overrides[option.id];
      if (override != null && isValidHydrationContainerMilliliters(override)) {
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

/// Logs one saved drink at its own volume — the whole drink, nutrients included.
/// Port of the Kotlin `logCustomHydrationDrinkEntry` (HydrationDrinkLogger).
///
/// Shared by the hydration entry screen and the quick-beverage home widget, so
/// that a widget tap cannot silently drop the drink's caffeine (or any other
/// nutrient): it goes through the very same write path.
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
  return writeHydrationAndNutritionEntry(
    hydrationRepository: hydrationRepository,
    nutritionRepository: nutritionRepository,
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

/// Port of the Kotlin `writeHydrationAndNutritionEntry` (HydrationDrinkLogger):
/// writes (or updates) the hydration record and, when nutrient values are
/// present, the associated nutrition record — validating permissions + volume.
Future<HydrationDrinkLogOutcome> writeHydrationAndNutritionEntry({
  required HydrationRepository hydrationRepository,
  required NutritionRepository nutritionRepository,
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
  if (!_isValidCustomDrinkHydrationMultiplier(hydrationMultiplier)) {
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
      hydrationClientRecordId = await hydrationRepository.writeHydrationEntry(
        HydrationWriteRequest(
          time: entryTime,
          volumeLiters: effectiveLiters,
          drinkId: drinkId,
        ),
      );
    }
    if (writesNutrition) {
      await nutritionRepository.writeNutritionEntry(
        NutritionWriteRequest(
          time: entryTime,
          nutrientValues: nutrientValues,
          name: nutritionName,
          associatedHydrationClientRecordId: hydrationClientRecordId,
        ),
      );
    }
  } else {
    await hydrationRepository.updateHydrationEntry(
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
