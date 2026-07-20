import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/core/presentation/command_state.dart';
import 'package:openvitals/core/presentation/screen_error.dart';
import 'package:openvitals/core/reminders/reminder_controller.dart';
import 'package:openvitals/core/result/app_failure.dart';
import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/data/repository/contract/hydration_repository.dart';
import 'package:openvitals/data/repository/contract/nutrition_repository.dart';
import 'package:openvitals/domain/health/health_permissions.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/nutrition_models.dart';
import 'package:openvitals/features/hydration/reminders/hydration_reminder_controller.dart';
import 'package:openvitals/features/manualentry/application/hydration_entry_view_model.dart';

/// The drink-logging command's lifecycle, which used to be three booleans and
/// an enum member (`isSavingEntry` / `saveCompleted` / `writeError` /
/// `writeFailed`).
///
/// Unlike its sibling forms, this one's use case *throws* on a write that
/// failed mid-flight (it answers a refusal with an outcome instead), so the
/// failure reaches the command through a catch rather than an `Err`.
class _FakeHydrationRepository implements HydrationRepository {
  _FakeHydrationRepository({this.write = const Ok('hydration-id')});

  Result<String> write;
  final List<HydrationWriteRequest> writes = [];

  @override
  Set<String> get hydrationWritePermissions => {HcPermissions.writeHydration};

  @override
  Map<String, double> hydrationContainerVolumeMilliliters() =>
      const <String, double>{};

  @override
  double hydrationDailyGoalLiters() => 2.0;

  @override
  double? lastCustomHydrationAmountMilliliters() => null;

  @override
  void setLastCustomHydrationAmountMilliliters(double milliliters) {}

  @override
  Future<Result<List<CustomHydrationDrink>>> customHydrationDrinks() async =>
      const Ok(<CustomHydrationDrink>[]);

  @override
  Future<Result<bool>> hasHydrationWritePermission() async => const Ok(true);

  @override
  Future<Result<List<DailyHydration>>> loadDailyHydration(
    LocalDate start,
    LocalDate end,
  ) async =>
      const Ok(<DailyHydration>[]);

  @override
  Future<Result<List<HydrationEntry>>> loadHydrationEntries(
    LocalDate start,
    LocalDate end,
  ) async =>
      const Ok(<HydrationEntry>[]);

  @override
  Future<Result<String>> writeHydrationEntry(
    HydrationWriteRequest request,
  ) async {
    writes.add(request);
    return write;
  }

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class _FakeNutritionRepository implements NutritionRepository {
  @override
  Set<String> get nutritionWritePermissions => {HcPermissions.writeNutrition};

  @override
  Future<Result<bool>> hasNutritionWritePermission() async => const Ok(true);

  @override
  Future<Result<List<NutritionEntry>>> loadNutritionEntries(
    LocalDate start,
    LocalDate end,
  ) async =>
      const Ok(<NutritionEntry>[]);

  @override
  Future<Result<String>> writeNutritionEntry(
    NutritionWriteRequest request,
  ) async =>
      const Ok('nutrition-id');

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

/// The reminder scheduler a completed save reaches for; silent here.
class _SilentReminders implements ReminderScheduler {
  @override
  Future<void> scheduleAll(List<DateTime> triggers, ReminderGoalProgress progress) async {}

  @override
  Future<void> cancel() async {}
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late _FakeHydrationRepository hydration;
  late ProviderContainer container;
  late NotifierProvider<HydrationEntryViewModel, HydrationEntryState> provider;

  Future<void> boot({Result<String> write = const Ok('hydration-id')}) async {
    SharedPreferences.setMockInitialValues(const <String, Object>{});
    final prefs = await SharedPreferences.getInstance();
    hydration = _FakeHydrationRepository(write: write);
    final nutrition = _FakeNutritionRepository();
    final reminders = _SilentReminders();
    container = ProviderContainer(overrides: [
      sharedPreferencesProvider.overrideWithValue(prefs),
      hydrationRepositoryProvider.overrideWithValue(hydration),
      nutritionRepositoryProvider.overrideWithValue(nutrition),
      hydrationReminderControllerProvider.overrideWith(
        (ref) => HydrationReminderController(
          preferences: ref.watch(preferencesRepositoryProvider),
          hydrationRepository: hydration,
          scheduler: reminders,
        ),
      ),
    ]);
    addTearDown(container.dispose);
    provider =
        NotifierProvider<HydrationEntryViewModel, HydrationEntryState>(
      HydrationEntryViewModel.new,
    );
    container.listen(provider, (_, _) {});
    // The permission probe, today's total and the drink catalog all load off
    // build() in a microtask chain.
    for (var i = 0; i < 8; i++) {
      await Future<void>.delayed(Duration.zero);
    }
  }

  HydrationEntryState state() => container.read(provider);
  HydrationEntryViewModel viewModel() => container.read(provider.notifier);

  test('a command at rest is idle', () async {
    await boot();

    expect(state().save, const CommandState<void>.idle());
    expect(state().isSavingEntry, isFalse);
    expect(state().blockingError, isNull);
  });

  test('a successful save settles on success, and is consumed once', () async {
    await boot();

    await viewModel().addCustomHydrationEntry(250);

    expect(hydration.writes, hasLength(1));
    expect(state().save, isA<CommandSuccess<void>>());
    expect(state().todayHydrationLiters, closeTo(0.25, 0.0001));

    // The screen shows its toast, then hands the command back to rest — so
    // re-entering the route cannot replay it.
    viewModel().onSaveCompletedHandled();
    expect(state().save, const CommandState<void>.idle());
  });

  test('a failed save carries the failure to the form, not an exception',
      () async {
    await boot(write: const Err(UnexpectedFailure('the provider hung up')));

    await viewModel().addCustomHydrationEntry(250);

    expect(state().save, isA<CommandFailure<void>>());
    expect(
      state().blockingError,
      isA<ScreenErrorMessage>().having(
        (it) => it.text,
        'text',
        contains('the provider hung up'),
      ),
    );
    // A failed write is not a validation error — the amount was fine.
    expect(state().entryError, isNull);
    expect(state().isSavingEntry, isFalse);
  });

  test('editing a field clears the failure the last attempt left behind',
      () async {
    await boot(write: const Err(UnexpectedFailure('boom')));
    await viewModel().addCustomHydrationEntry(250);
    expect(state().save, isA<CommandFailure<void>>());

    viewModel().updateEntryTime(DateTime.now());

    expect(state().save, const CommandState<void>.idle());
    expect(state().blockingError, isNull);
  });

  test('validation refuses before the command ever runs', () async {
    await boot();

    await viewModel().addCustomHydrationEntry(0);

    expect(hydration.writes, isEmpty);
    expect(state().entryError, HydrationEntryError.invalidAmount);
    expect(state().save, const CommandState<void>.idle());
  });
}
