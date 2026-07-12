import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../core/presentation/screen_error.dart';
import '../../../core/result/result.dart';
import '../../../core/time/local_date.dart';
import '../../../di/providers.dart';
import '../../../domain/usecase/load_recovery_days_use_case.dart';
import 'recovery_detail_display.dart';

// The scored day is the use case's shape; the two detail screens read it
// straight off the display, so it stays visible from here.
export '../../../domain/usecase/load_recovery_days_use_case.dart'
    show RecoveryDay, recoveryLookbackDays;

part 'recovery_detail_view_model.freezed.dart';

/// Port of the Kotlin `RecoveryUiState` (the sleep-score slice the two detail
/// screens read; the stress slice lives in `RecoveryViewModel`).
///
/// [display] is the selected day, picked out of [days] and scored, at load time
/// — the screens render it and scan nothing.
@freezed
abstract class RecoveryDetailState with _$RecoveryDetailState {
  const RecoveryDetailState._();

  const factory RecoveryDetailState({
    required LocalDate selectedDate,
    @Default(true) bool isLoading,
    @Default(<RecoveryDay>[]) List<RecoveryDay> days,
    ScreenError? error,
    RecoveryDetailDisplay? display,
  }) = _RecoveryDetailState;

  /// Kotlin `RecoveryUiState.today`. The screens read [display] instead; this
  /// stays as the state's own answer to "which day is this?".
  RecoveryDay get today {
    for (final day in days) {
      if (day.date == selectedDate) return day;
    }
    return RecoveryDay(date: selectedDate);
  }
}

/// Port of the Kotlin `RecoveryViewModel`'s sleep-score load: the last
/// [recoveryLookbackDays] days of sleep sessions turned into [RecoveryDay]s
/// via [calculateSleepScoresByDate]. Kept separate from [RecoveryViewModel],
/// which is stress-only in the Flutter port.
class RecoveryDetailViewModel extends Notifier<RecoveryDetailState> {
  int _generation = 0;

  @override
  RecoveryDetailState build() {
    Future.microtask(() {
      if (ref.mounted) load();
    });
    final today = LocalDate.now();
    return RecoveryDetailState(
      selectedDate: today,
      display: buildRecoveryDetailDisplay(const <RecoveryDay>[], today),
    );
  }

  Future<void> load([LocalDate? date]) async {
    final today = date ?? LocalDate.now();
    final generation = ++_generation;
    final loadRecoveryDays = ref.read(loadRecoveryDaysUseCaseProvider);
    state = RecoveryDetailState(
      isLoading: true,
      selectedDate: today,
      days: state.days,
      display: buildRecoveryDetailDisplay(state.days, today),
    );

    // The lookback window, and the scoring of each night against the ones
    // around it, belong to the use case.
    final result = await loadRecoveryDays(today);
    if (!ref.mounted || generation != _generation) return;
    switch (result) {
      case Ok(:final value):
        state = RecoveryDetailState(
          isLoading: false,
          selectedDate: today,
          days: value,
          display: buildRecoveryDetailDisplay(value, today),
        );
      case Err(:final failure):
        // The days already on screen survive the failed reload, as they did
        // when this bridged through `orThrow`.
        state = RecoveryDetailState(
          isLoading: false,
          selectedDate: today,
          days: state.days,
          display: buildRecoveryDetailDisplay(state.days, today),
          error: failure.toScreenError(fallback: 'Unable to load sleep data.'),
        );
    }
  }
}

/// Shared by the sleep-score and sleep-efficiency detail screens (the Kotlin
/// screens share one `RecoveryViewModel` instance the same way).
final recoveryDetailProvider =
    NotifierProvider.autoDispose<RecoveryDetailViewModel, RecoveryDetailState>(
  RecoveryDetailViewModel.new,
);
