import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/core/presentation/command_state.dart';
import 'package:openvitals/core/presentation/screen_error.dart';
import 'package:openvitals/core/result/app_failure.dart';
import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/data/repository/contract/body_repository.dart';
import 'package:openvitals/domain/health/health_permissions.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/body_models.dart';
import 'package:openvitals/features/manualentry/application/body_measurement_entry_view_model.dart';

/// The save command's lifecycle, which used to be three booleans and an enum
/// member (`isSavingEntry` / `saveCompleted` / `writeError` / `writeFailed`).
class _FakeBodyRepository implements BodyRepository {
  _FakeBodyRepository({this.write = const Ok('record-id')});

  Result<String> write;
  final List<BodyMeasurementWriteRequest> writes = [];

  @override
  Set<String> bodyWritePermissions(BodyMeasurementType type) =>
      {HcPermissions.writeWeight};

  @override
  Future<Result<bool>> hasBodyWritePermission(BodyMeasurementType type) async =>
      const Ok(true);

  @override
  Future<Result<String>> writeBodyMeasurementEntry(
    BodyMeasurementWriteRequest request,
  ) async {
    writes.add(request);
    return write;
  }

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late _FakeBodyRepository repository;
  late ProviderContainer container;
  late NotifierProvider<BodyMeasurementEntryViewModel,
      BodyMeasurementEntryState> provider;

  Future<void> boot({Result<String> write = const Ok('record-id')}) async {
    SharedPreferences.setMockInitialValues(const <String, Object>{});
    final prefs = await SharedPreferences.getInstance();
    repository = _FakeBodyRepository(write: write);
    container = ProviderContainer(overrides: [
      sharedPreferencesProvider.overrideWithValue(prefs),
      bodyRepositoryProvider.overrideWithValue(repository),
    ]);
    addTearDown(container.dispose);
    provider = NotifierProvider<BodyMeasurementEntryViewModel,
        BodyMeasurementEntryState>(
      () => BodyMeasurementEntryViewModel(BodyMeasurementType.weight),
    );
    container.listen(provider, (_, _) {});
    // The permission probe runs in a microtask off build().
    await Future<void>.delayed(Duration.zero);
  }

  BodyMeasurementEntryState state() => container.read(provider);
  BodyMeasurementEntryViewModel viewModel() =>
      container.read(provider.notifier);

  test('a command at rest is idle', () async {
    await boot();

    expect(state().save, const CommandState<void>.idle());
    expect(state().isSavingEntry, isFalse);
    expect(state().blockingError, isNull);
  });

  test('a successful save settles on success, and is consumed once', () async {
    await boot();
    viewModel().updateInput('72.5');

    await viewModel().addEntry(72.5);

    expect(repository.writes, hasLength(1));
    expect(repository.writes.single.value, 72.5);
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
    viewModel().updateInput('72.5');

    await viewModel().addEntry(72.5);

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
    viewModel().updateInput('72.5');
    await viewModel().addEntry(72.5);
    expect(state().save, isA<CommandFailure<void>>());

    viewModel().updateInput('73');

    expect(state().save, const CommandState<void>.idle());
    expect(state().blockingError, isNull);
  });

  test('validation refuses before the command ever runs', () async {
    await boot();
    viewModel().updateInput('0');

    await viewModel().addEntry(0.0);

    expect(repository.writes, isEmpty);
    expect(state().entryError, BodyMeasurementEntryError.invalidValue);
    expect(state().save, const CommandState<void>.idle());
  });
}
