import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/core/presentation/command_state.dart';
import 'package:openvitals/core/presentation/screen_error.dart';
import 'package:openvitals/core/result/app_failure.dart';
import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/data/prefs/preferences_repository.dart';
import 'package:openvitals/data/repository/contract/mindfulness_repository.dart';
import 'package:openvitals/data/source/health/health_permissions.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/mindfulness_models.dart';
import 'package:openvitals/features/manualentry/application/mindfulness_entry_view_model.dart';

/// The save command's lifecycle, which used to be three booleans and an enum
/// member (`isSavingEntry` / `saveCompleted` / `writeError` / `writeFailed`).
class _FakeMindfulnessRepository implements MindfulnessRepository {
  _FakeMindfulnessRepository({this.write = const Ok('id')});

  Result<String> write;
  final List<MindfulnessSessionWriteRequest> writes = [];

  @override
  bool isMindfulnessAvailable() => true;

  @override
  Future<Result<bool>> hasMindfulnessWritePermission() async => const Ok(true);

  @override
  Set<String> get mindfulnessWritePermissions => {HcPermissions.writeMindfulness};

  @override
  Future<Result<String>> writeMindfulnessSessionEntry(
    MindfulnessSessionWriteRequest request,
  ) async {
    writes.add(request);
    return write;
  }

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late _FakeMindfulnessRepository repository;
  late ProviderContainer container;
  late NotifierProvider<MindfulnessEntryViewModel, MindfulnessEntryState>
      provider;

  Future<void> boot({Result<String> write = const Ok('id')}) async {
    SharedPreferences.setMockInitialValues(const <String, Object>{});
    final prefs = await SharedPreferences.getInstance();
    repository = _FakeMindfulnessRepository(write: write);
    container = ProviderContainer(overrides: [
      sharedPreferencesProvider.overrideWithValue(prefs),
      preferencesRepositoryProvider
          .overrideWithValue(PreferencesRepository(prefs)),
      mindfulnessRepositoryProvider.overrideWithValue(repository),
    ]);
    addTearDown(container.dispose);
    provider = NotifierProvider<MindfulnessEntryViewModel,
        MindfulnessEntryState>(MindfulnessEntryViewModel.new);
    container.listen(provider, (_, __) {});
    // The permission probe runs in a microtask off build().
    await Future<void>.delayed(Duration.zero);
  }

  MindfulnessEntryState state() => container.read(provider);
  MindfulnessEntryViewModel viewModel() => container.read(provider.notifier);

  test('a command at rest is idle', () async {
    await boot();

    expect(state().save, const CommandState<void>.idle());
    expect(state().isSavingEntry, isFalse);
    expect(state().blockingError, isNull);
  });

  test('a successful save settles on success, and is consumed once', () async {
    await boot();
    viewModel().updateManualMinutes('20');

    await viewModel().addManualEntry();

    expect(repository.writes, hasLength(1));
    expect(state().save, isA<CommandSuccess<void>>());

    // The screen shows its toast, then hands the command back to rest — so
    // re-entering the route cannot replay it.
    viewModel().onSaveCompletedHandled();
    expect(state().save, const CommandState<void>.idle());
  });

  test('a failed save carries the failure to the form, not an exception',
      () async {
    await boot(write: const Err(UnexpectedFailure('the provider hung up')));
    viewModel().updateManualMinutes('20');

    await viewModel().addManualEntry();

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
    viewModel().updateManualMinutes('20');
    await viewModel().addManualEntry();
    expect(state().save, isA<CommandFailure<void>>());

    viewModel().updateManualMinutes('25');

    expect(state().save, const CommandState<void>.idle());
    expect(state().blockingError, isNull);
  });

  test('validation refuses before the command ever runs', () async {
    await boot();
    viewModel().updateManualMinutes('0');

    await viewModel().addManualEntry();

    expect(repository.writes, isEmpty);
    expect(state().entryError, MindfulnessEntryError.invalidManualEntry);
    expect(state().save, const CommandState<void>.idle());
  });
}
