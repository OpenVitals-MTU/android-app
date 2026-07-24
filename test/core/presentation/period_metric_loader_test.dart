import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/core/period/period_load_query.dart';
import 'package:openvitals/core/period/period_selection.dart';
import 'package:openvitals/core/period/time_range.dart';
import 'package:openvitals/core/presentation/period_metric_loader.dart';
import 'package:openvitals/core/presentation/screen_error.dart';
import 'package:openvitals/core/result/app_failure.dart';
import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/data/prefs/preferences_repository.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/refresh_mode.dart';

/// Exercises [PeriodMetricLoader] against a tiny hand-rolled view-model so the
/// shared load orchestration — and the range-switch loading-flash fix — is tested
/// once, independent of any feature's freezed state.
void main() {
  late ProviderContainer container;

  Future<_TinyViewModel> boot() async {
    SharedPreferences.setMockInitialValues(const <String, Object>{});
    final prefs = await SharedPreferences.getInstance();
    container = ProviderContainer(overrides: [
      sharedPreferencesProvider.overrideWithValue(prefs),
      preferencesRepositoryProvider
          .overrideWithValue(PreferencesRepository(prefs)),
    ]);
    addTearDown(container.dispose);
    container.listen(_tinyProvider, (_, _) {});
    return container.read(_tinyProvider.notifier);
  }

  final jan1 = const LocalDate(2026, 1, 1);
  _TinyState state() => container.read(_tinyProvider);

  test('a navigation clears the stale display so the loading state shows',
      () async {
    final vm = await boot();

    // Seed a loaded week.
    final seed = vm.load(PeriodSelection(TimeRange.week, jan1));
    vm.gates.removeLast().complete(const Ok(7));
    await seed;
    expect(state().display, 7);

    // Switch to a different range: mid-load, the stale display must be gone so
    // the screen shows its loading skeleton (not the previous window's chart).
    final navigate = vm.load(PeriodSelection(TimeRange.year, jan1));
    expect(state().display, isNull);
    expect(state().isLoading, isTrue);

    vm.gates.removeLast().complete(const Ok(42));
    await navigate;
    expect(state().display, 42);
    expect(state().isLoading, isFalse);
  });

  test('a same-window refresh keeps the display (no loading flash)', () async {
    final vm = await boot();
    final seed = vm.load(PeriodSelection(TimeRange.week, jan1));
    vm.gates.removeLast().complete(const Ok(7));
    await seed;

    // Same range + date: a refresh must NOT blank the chart while it reloads.
    vm.load(PeriodSelection(TimeRange.week, jan1),
        refreshMode: RefreshMode.force);
    expect(state().display, 7);
    expect(state().isLoading, isTrue);
  });

  test('rapid navigations coalesce: one fetch in flight, latest wins',
      () async {
    final vm = await boot();

    final first = vm.load(PeriodSelection(TimeRange.week, jan1));
    // Fired while the first fetch is on the wire: parked, never fetched. A
    // Health Connect read cannot be cancelled, so every skipped fetch here is
    // a slow read that never hits the native queue.
    final second = vm.load(PeriodSelection(TimeRange.month, jan1));
    final third = vm.load(PeriodSelection(TimeRange.year, jan1));
    expect(vm.gates, hasLength(1));
    // The UI still tracks the newest request immediately.
    expect(state().range, TimeRange.year);
    expect(state().isLoading, isTrue);

    // The superseded first result is dropped; completing it dispatches ONE
    // fetch for the newest selection (month never ran).
    vm.gates.removeLast().complete(const Ok(1));
    await Future<void>.delayed(Duration.zero);
    expect(state().display, isNull);
    expect(vm.gates, hasLength(1));

    vm.gates.removeLast().complete(const Ok(3));
    await Future.wait([first, second, third]);
    expect(state().display, 3);
    expect(state().range, TimeRange.year);
    expect(state().isLoading, isFalse);
  });

  test('an error sets the error and clears loading', () async {
    final vm = await boot();
    final run = vm.load(PeriodSelection(TimeRange.week, jan1));
    vm.gates.removeLast().complete(const Err(NotFoundFailure()));
    await run;

    expect(state().error, isNotNull);
    expect(state().isLoading, isFalse);
  });
}

// ── A minimal view-model on the mixin ────────────────────────────────────────

const Object _keep = Object();

class _TinyState {
  const _TinyState({
    this.range = TimeRange.week,
    required this.date,
    this.isLoading = true,
    this.display,
    this.error,
  });

  final TimeRange range;
  final LocalDate date;
  final bool isLoading;
  final int? display;
  final ScreenError? error;

  /// Hand-rolled copyWith with a `_keep` sentinel so `display: null` truly clears
  /// (real freezed copyWith does the same via its own sentinel).
  _TinyState copyWith({
    TimeRange? range,
    LocalDate? date,
    bool? isLoading,
    Object? display = _keep,
    ScreenError? error,
  }) =>
      _TinyState(
        range: range ?? this.range,
        date: date ?? this.date,
        isLoading: isLoading ?? this.isLoading,
        display: identical(display, _keep) ? this.display : display as int?,
        error: error,
      );
}

class _TinyViewModel extends Notifier<_TinyState>
    with PeriodMetricLoader<_TinyState, int> {
  /// Completed by the test, so loads can be held in flight.
  final List<Completer<Result<int>>> gates = [];

  @override
  _TinyState build() => const _TinyState(date: LocalDate(2026, 1, 1));

  Future<void> load(
    PeriodSelection selection, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) =>
      runLoad(selection, refreshMode: refreshMode);

  @override
  PeriodSelection selectionOf(_TinyState state) =>
      PeriodSelection(state.range, state.date);

  @override
  _TinyState onLoadStart(
    _TinyState state,
    PeriodSelection selection, {
    required bool navigated,
  }) {
    final next = state.copyWith(
      range: selection.selectedRange,
      date: selection.selectedDate,
      isLoading: true,
      error: null,
    );
    return navigated ? next.copyWith(display: null) : next;
  }

  @override
  Future<Result<int>> fetch(PeriodLoadQuery query, RefreshMode refreshMode) {
    final completer = Completer<Result<int>>();
    gates.add(completer);
    return completer.future;
  }

  @override
  _TinyState onLoadSuccess(_TinyState state, int value, PeriodLoadQuery query) =>
      state.copyWith(isLoading: false, display: value, error: null);

  @override
  _TinyState onLoadError(_TinyState state, ScreenError error) =>
      state.copyWith(isLoading: false, error: error);
}

final _tinyProvider =
    NotifierProvider<_TinyViewModel, _TinyState>(_TinyViewModel.new);
