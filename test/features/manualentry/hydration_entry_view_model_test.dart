import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/data/repository/contract/hydration_repository.dart';
import 'package:openvitals/data/repository/contract/nutrition_repository.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/caffeine_models.dart';
import 'package:openvitals/domain/model/nutrition_models.dart';
import 'package:openvitals/core/reminders/reminder_controller.dart';
import 'package:openvitals/features/hydration/reminders/hydration_reminder_controller.dart';
import 'package:openvitals/features/manualentry/application/hydration_entry_view_model.dart';
import 'package:openvitals/domain/health/health_permissions.dart';

/// An in-memory drink store + write log, standing in for prefs + Health Connect.
class _FakeHydrationRepository implements HydrationRepository {
  final List<CustomHydrationDrink> drinks = [];
  final Map<String, double> containerOverrides = {};
  final List<HydrationWriteRequest> writes = [];
  List<String>? lastReorder;
  double? lastCustomAmount;

  @override
  Set<String> get hydrationWritePermissions => {HcPermissions.writeHydration};

  @override
  Map<String, double> hydrationContainerVolumeMilliliters() =>
      Map<String, double>.of(containerOverrides);

  @override
  void setHydrationContainerVolumeMilliliters(String id, double milliliters) =>
      containerOverrides[id] = milliliters;

  @override
  double hydrationDailyGoalLiters() => 2.5;

  @override
  double? lastCustomHydrationAmountMilliliters() => lastCustomAmount;

  @override
  void setLastCustomHydrationAmountMilliliters(double milliliters) =>
      lastCustomAmount = milliliters;

  @override
  Future<Result<List<CustomHydrationDrink>>> customHydrationDrinks() async =>
      Ok(List<CustomHydrationDrink>.of(drinks));

  @override
  Future<Result<void>> saveCustomHydrationDrink(
    CustomHydrationDrink drink,
  ) async {
    final index = drinks.indexWhere((it) => it.id == drink.id);
    if (index >= 0) {
      drinks[index] = drink;
    } else {
      drinks.add(drink);
    }
    return const Ok(null);
  }

  @override
  Future<Result<void>> deleteCustomHydrationDrink(String drinkId) async {
    drinks.removeWhere((it) => it.id == drinkId);
    return const Ok(null);
  }

  @override
  Future<Result<void>> reorderCustomHydrationDrinks(
    List<String> drinkIds,
  ) async {
    lastReorder = drinkIds;
    drinks.sort((a, b) => drinkIds.indexOf(a.id).compareTo(drinkIds.indexOf(b.id)));
    return const Ok(null);
  }

  @override
  Future<Result<void>> moveCustomHydrationDrinkToCategory(
    String drinkId,
    CaffeineSourceCategory? category,
  ) async {
    final index = drinks.indexWhere((it) => it.id == drinkId);
    if (index >= 0) drinks[index] = drinks[index].copyWith(category: category);
    return const Ok(null);
  }

  @override
  Future<Result<bool>> hasHydrationWritePermission() async => const Ok(true);

  @override
  Future<Result<List<DailyHydration>>> loadDailyHydration(
    LocalDate start,
    LocalDate end,
  ) async =>
      const Ok(<DailyHydration>[]);

  /// Entries the frequent-drink ranking reads back.
  List<HydrationEntry> hydrationEntries = const <HydrationEntry>[];

  @override
  Future<Result<List<HydrationEntry>>> loadHydrationEntries(
    LocalDate start,
    LocalDate end,
  ) async =>
      Ok(hydrationEntries);

  @override
  Future<Result<String>> writeHydrationEntry(
    HydrationWriteRequest request,
  ) async {
    writes.add(request);
    return const Ok('hydration-id');
  }

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class _FakeNutritionRepository implements NutritionRepository {
  final List<NutritionWriteRequest> writes = [];

  @override
  Set<String> get nutritionWritePermissions => {HcPermissions.writeNutrition};

  @override
  Future<Result<bool>> hasNutritionWritePermission() async => const Ok(true);

  List<NutritionEntry> nutritionEntries = const <NutritionEntry>[];

  @override
  Future<Result<List<NutritionEntry>>> loadNutritionEntries(
    LocalDate start,
    LocalDate end,
  ) async =>
      Ok(nutritionEntries);

  @override
  Future<Result<String>> writeNutritionEntry(
    NutritionWriteRequest request,
  ) async {
    writes.add(request);
    return const Ok('nutrition-id');
  }

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

/// Records the reminder seams so the entry-save dismissal can be observed.
class RecordingReminderNotifier implements ReminderScheduler, ReminderNotifier {
  int notificationCancels = 0;
  int alarmCancels = 0;

  @override
  Future<void> show(ReminderGoalProgress progress) async {}

  @override
  Future<void> cancel() async => notificationCancels++;

  @override
  Future<void> schedule(DateTime triggerAt) async {}
}

/// The scheduler seam, kept separate so a notification cancel is
/// distinguishable from an alarm cancel.
class RecordingAlarmScheduler implements ReminderScheduler {
  int cancels = 0;

  @override
  Future<void> schedule(DateTime triggerAt) async {}

  @override
  Future<void> cancel() async => cancels++;
}

late RecordingReminderNotifier reminderNotifier;
late RecordingAlarmScheduler reminderScheduler;

ProviderContainer _container(
  _FakeHydrationRepository hydration,
  _FakeNutritionRepository nutrition,
  SharedPreferences prefs,
) {
  reminderNotifier = RecordingReminderNotifier();
  reminderScheduler = RecordingAlarmScheduler();
  final container = ProviderContainer(
    overrides: [
      sharedPreferencesProvider.overrideWithValue(prefs),
      hydrationRepositoryProvider.overrideWithValue(hydration),
      nutritionRepositoryProvider.overrideWithValue(nutrition),
      hydrationReminderControllerProvider.overrideWith(
        (ref) => HydrationReminderController(
          preferences: ref.watch(preferencesRepositoryProvider),
          hydrationRepository: hydration,
          notifier: reminderNotifier,
          scheduler: reminderScheduler,
        ),
      ),
    ],
  );
  addTearDown(container.dispose);
  return container;
}

final _provider =
    NotifierProvider<HydrationEntryViewModel, HydrationEntryState>(
  HydrationEntryViewModel.new,
);

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late _FakeHydrationRepository hydration;
  late _FakeNutritionRepository nutrition;
  late ProviderContainer container;

  setUp(() async {
    hydration = _FakeHydrationRepository();
    nutrition = _FakeNutritionRepository();
    SharedPreferences.setMockInitialValues(const <String, Object>{});
    container = _container(
      hydration,
      nutrition,
      await SharedPreferences.getInstance(),
    );
  });

  HydrationEntryViewModel notifier() => container.read(_provider.notifier);
  HydrationEntryState state() => container.read(_provider);

  /// Builds the notifier and lets its `refreshPermission` / `refreshTodayHydration`
  /// microtask chain finish, so writes are not rejected for a missing permission.
  Future<void> settle() async {
    container.read(_provider);
    for (var i = 0; i < 8; i++) {
      await Future<void>.delayed(Duration.zero);
    }
  }

  test('saveCustomDrink persists a new drink and reloads the options', () async {
    await settle();
    await notifier().saveCustomDrink(
      const CustomHydrationDrinkInput(name: 'Cola', volumeMilliliters: 330),
    );

    expect(hydration.drinks, hasLength(1));
    expect(hydration.drinks.single.name, 'Cola');
    expect(hydration.drinks.single.id, isNotEmpty);
    expect(state().customDrinkOptions.single.name, 'Cola');
    expect(state().entryError, isNull);
  });

  test('saveCustomDrink on an invalid input reports invalidCustomDrink',
      () async {
    // Settle the initial catalog load first: it clears transient entry errors,
    // as the Kotlin `refreshDrinkOptions` does.
    await settle();
    await notifier().saveCustomDrink(
      const CustomHydrationDrinkInput(name: '  ', volumeMilliliters: 330),
    );

    expect(hydration.drinks, isEmpty);
    expect(state().entryError, HydrationEntryError.invalidCustomDrink);
  });

  test('editing a drink keeps its id and preloaded flag', () async {
    hydration.drinks.add(const CustomHydrationDrink(
      id: 'preset-1',
      name: 'Espresso',
      volumeMilliliters: 30,
      isPreloaded: true,
    ));
    // Rebuild so the notifier picks the seeded drink up.
    container.invalidate(_provider);

    await settle();
    await notifier().saveCustomDrink(
      const CustomHydrationDrinkInput(
        name: 'Double espresso',
        volumeMilliliters: 60,
      ),
      existingDrinkId: 'preset-1',
    );

    expect(hydration.drinks, hasLength(1));
    final saved = hydration.drinks.single;
    expect(saved.id, 'preset-1');
    expect(saved.name, 'Double espresso');
    expect(saved.volumeMilliliters, 60);
    expect(saved.isPreloaded, isTrue, reason: 'preloaded must survive an edit');
  });

  test('deleteCustomDrink removes it from storage and state', () async {
    hydration.drinks.add(const CustomHydrationDrink(
      id: 'd1',
      name: 'Cola',
      volumeMilliliters: 330,
    ));
    await settle();
    expect(state().customDrinkOptions, hasLength(1));

    await notifier().deleteCustomDrink(state().customDrinkOptions.single);

    expect(hydration.drinks, isEmpty);
    expect(state().customDrinkOptions, isEmpty);
  });

  test('moveCustomDrinkToTarget drops the drink onto the target slot', () async {
    for (final name in ['a', 'b', 'c', 'd']) {
      hydration.drinks.add(CustomHydrationDrink(
        id: name,
        name: name,
        volumeMilliliters: 100,
      ));
    }
    await settle();

    // Same drop-on-target semantics as the dashboard reorder: 'a' lands on 'd'.
    await notifier().moveCustomDrinkToTarget('a', 'd');

    expect([for (final it in state().customDrinkOptions) it.id], ['b', 'c', 'd', 'a']);
    expect(hydration.lastReorder, ['b', 'c', 'd', 'a']);
  });

  test('moveCustomDrinkToTarget is a no-op on an unknown or self target',
      () async {
    hydration.drinks.add(const CustomHydrationDrink(
      id: 'a',
      name: 'a',
      volumeMilliliters: 100,
    ));
    await settle();

    await notifier().moveCustomDrinkToTarget('a', 'a');
    await notifier().moveCustomDrinkToTarget('a', 'missing');

    expect(hydration.lastReorder, isNull);
  });

  test('moveCustomDrinkToCategory persists and reloads', () async {
    hydration.drinks.add(const CustomHydrationDrink(
      id: 'd1',
      name: 'Cola',
      volumeMilliliters: 330,
    ));
    await settle();

    await notifier().moveCustomDrinkToCategory('d1', CaffeineSourceCategory.soda);

    expect(hydration.drinks.single.category, CaffeineSourceCategory.soda);
    expect(state().customDrinkOptions.single.category, CaffeineSourceCategory.soda);
  });

  test('updateContainerSize persists a default preset and selects it', () {
    final coffee = state().containerOptions.first;
    notifier().updateContainerSize(coffee, 125);

    expect(hydration.containerOverrides[coffee.id], 125);
    expect(state().selectedContainer.volumeMilliliters, 125);
    expect(
      state().containerOptions.firstWhere((it) => it.id == coffee.id).volumeMilliliters,
      125,
    );
  });

  test('updateContainerSize rejects an out-of-range volume', () {
    final coffee = state().containerOptions.first;
    notifier().updateContainerSize(coffee, 0);

    expect(hydration.containerOverrides, isEmpty);
    expect(state().entryError, HydrationEntryError.invalidAmount);
  });

  test('an ad-hoc container resize is session-only, never persisted', () {
    // Only the seven defaults persist their override (Kotlin `Defaults.any`).
    const adHoc = HydrationContainerOption(
      id: 'current_entry',
      volumeMilliliters: 275,
    );
    notifier().updateContainerSize(adHoc, 300);

    expect(hydration.containerOverrides, isEmpty);
    expect(state().selectedContainer.volumeMilliliters, 300);
  });

  test('addSavedCustomDrinkEntry honours the requested entry time', () async {
    const drink = CustomHydrationDrink(
      id: 'd1',
      name: 'Cola',
      volumeMilliliters: 330,
    );
    hydration.drinks.add(drink);
    await settle();
    expect(state().canWriteHydration, isTrue);

    final when = DateTime.now().subtract(const Duration(hours: 3));
    await notifier().addSavedCustomDrinkEntry(drink, entryTime: when);

    expect(hydration.writes, hasLength(1));
    expect(
      hydration.writes.single.time.millisecondsSinceEpoch,
      when.millisecondsSinceEpoch,
    );
  });

  test('addSavedCustomDrinkEntry scales nutrients to a partial amount', () async {
    const drink = CustomHydrationDrink(
      id: 'd1',
      name: 'Cola',
      volumeMilliliters: 330,
      nutrientValues: {NutritionNutrient.energy: 140},
    );
    hydration.drinks.add(drink);
    await settle();
    expect(state().canWriteNutrition, isTrue);

    await notifier().addSavedCustomDrinkEntry(drink, amountMilliliters: 165);

    expect(nutrition.writes, hasLength(1));
    expect(
      nutrition.writes.single.nutrientValues[NutritionNutrient.energy],
      closeTo(70, 1e-9),
    );
    expect(hydration.writes.single.volumeLiters, closeTo(0.165, 1e-9));
  });

  test('refreshDailyGoal re-reads the persisted goal', () {
    notifier().refreshDailyGoal();
    expect(state().dailyGoalLiters, 2.5);
  });

  test('a saved entry dismisses the reminder but keeps its alarm armed',
      () async {
    await settle();
    expect(state().canWriteHydration, isTrue);

    await notifier().addCustomHydrationEntry(250);

    expect(hydration.writes, hasLength(1));
    // The notification is hidden; the schedule must survive so the next
    // reminder still fires today.
    expect(reminderNotifier.notificationCancels, 1);
    expect(reminderScheduler.cancels, 0);
  });

  test('a rejected entry does not dismiss the reminder', () async {
    await settle();

    await notifier().addCustomHydrationEntry(0); // invalid amount

    expect(hydration.writes, isEmpty);
    expect(reminderNotifier.notificationCancels, 0);
  });
}
