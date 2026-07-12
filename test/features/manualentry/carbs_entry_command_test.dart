import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/core/presentation/command_state.dart';
import 'package:openvitals/core/presentation/screen_error.dart';
import 'package:openvitals/core/result/app_failure.dart';
import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/data/repository/contract/nutrition_repository.dart';
import 'package:openvitals/domain/health/health_permissions.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/nutrition_models.dart';
import 'package:openvitals/features/manualentry/application/carbs_entry_view_model.dart';

/// The save command's lifecycle, which used to be three booleans and an enum
/// member (`isSavingEntry` / `saveCompleted` / `writeError` / `writeFailed`).
class _FakeNutritionRepository implements NutritionRepository {
  _FakeNutritionRepository({this.write = const Ok('carbs-id')});

  Result<String> write;
  final List<NutritionWriteRequest> writes = [];

  @override
  Set<String> get nutritionWritePermissions => {HcPermissions.writeNutrition};

  @override
  Future<Result<bool>> hasNutritionWritePermission() async => const Ok(true);

  @override
  Future<Result<String>> writeCarbsEntry(NutritionWriteRequest request) async {
    writes.add(request);
    return write;
  }

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late _FakeNutritionRepository repository;
  late ProviderContainer container;
  late NotifierProvider<CarbsEntryViewModel, CarbsEntryState> provider;

  Future<void> boot({Result<String> write = const Ok('carbs-id')}) async {
    SharedPreferences.setMockInitialValues(const <String, Object>{});
    final prefs = await SharedPreferences.getInstance();
    repository = _FakeNutritionRepository(write: write);
    container = ProviderContainer(overrides: [
      sharedPreferencesProvider.overrideWithValue(prefs),
      nutritionRepositoryProvider.overrideWithValue(repository),
    ]);
    addTearDown(container.dispose);
    provider = NotifierProvider<CarbsEntryViewModel, CarbsEntryState>(
      CarbsEntryViewModel.new,
    );
    container.listen(provider, (_, _) {});
    // The permission probe runs in a microtask off build().
    await Future<void>.delayed(Duration.zero);
  }

  CarbsEntryState state() => container.read(provider);
  CarbsEntryViewModel viewModel() => container.read(provider.notifier);

  test('a command at rest is idle', () async {
    await boot();

    expect(state().save, const CommandState<void>.idle());
    expect(state().isSavingEntry, isFalse);
    expect(state().blockingError, isNull);
  });

  test('a successful save settles on success, and is consumed once', () async {
    await boot();
    viewModel().updateInput('45');

    await viewModel().addEntry(45.0);

    expect(repository.writes, hasLength(1));
    expect(state().save, isA<CommandSuccess<void>>());
    // A new entry clears the field for the next one.
    expect(state().inputText, '');

    // The screen shows its toast, then hands the command back to rest — so
    // re-entering the route cannot replay it.
    viewModel().onSaveCompletedHandled();
    expect(state().save, const CommandState<void>.idle());
  });

  test('a failed save carries the failure to the form, not an exception',
      () async {
    await boot(write: const Err(UnexpectedFailure('the provider hung up')));
    viewModel().updateInput('45');

    await viewModel().addEntry(45.0);

    expect(state().save, isA<CommandFailure<void>>());
    expect(
      state().blockingError,
      const ScreenErrorMessage('the provider hung up'),
    );
    // A failed write is not a validation error — the field is still valid.
    expect(state().entryError, isNull);
    expect(state().isSavingEntry, isFalse);
  });

  test('editing a field clears the failure the last attempt left behind',
      () async {
    await boot(write: const Err(UnexpectedFailure('boom')));
    viewModel().updateInput('45');
    await viewModel().addEntry(45.0);
    expect(state().save, isA<CommandFailure<void>>());

    viewModel().updateInput('50');

    expect(state().save, const CommandState<void>.idle());
    expect(state().blockingError, isNull);
  });

  test('validation refuses before the command ever runs', () async {
    await boot();
    viewModel().updateInput('0');

    await viewModel().addEntry(0.0);

    expect(repository.writes, isEmpty);
    expect(state().entryError, CarbsEntryError.invalidValue);
    expect(state().save, const CommandState<void>.idle());
  });
}
