import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/presentation/screen_error.dart';
import '../../../core/result/result.dart';
import '../../../core/time/local_date.dart';
import '../../../di/providers.dart';
import '../../../domain/usecase/load_recovery_days_use_case.dart';

// The scored day is the use case's shape; the two detail screens read it
// straight off the state, so it stays visible from here.
export '../../../domain/usecase/load_recovery_days_use_case.dart'
    show RecoveryDay, recoveryLookbackDays;

/// Port of the Kotlin `RecoveryUiState` (the sleep-score slice the two detail
/// screens read; the stress slice lives in `RecoveryViewModel`).
class RecoveryDetailState {
  RecoveryDetailState({
    this.isLoading = true,
    LocalDate? selectedDate,
    this.days = const <RecoveryDay>[],
    this.error,
  }) : selectedDate = selectedDate ?? LocalDate.now();

  final bool isLoading;
  final LocalDate selectedDate;
  final List<RecoveryDay> days;
  final ScreenError? error;

  /// Kotlin `RecoveryUiState.today`.
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
    return RecoveryDetailState();
  }

  Future<void> load([LocalDate? date]) async {
    final today = date ?? LocalDate.now();
    final generation = ++_generation;
    final loadRecoveryDays = ref.read(loadRecoveryDaysUseCaseProvider);
    state = RecoveryDetailState(
      isLoading: true,
      selectedDate: today,
      days: state.days,
    );

    try {
      // The lookback window, and the scoring of each night against the ones
      // around it, belong to the use case.
      final days = (await loadRecoveryDays(today)).orThrow();
      if (!ref.mounted || generation != _generation) return;
      state = RecoveryDetailState(
        isLoading: false,
        selectedDate: today,
        days: days,
      );
    } catch (error) {
      if (!ref.mounted || generation != _generation) return;
      state = RecoveryDetailState(
        isLoading: false,
        selectedDate: today,
        days: state.days,
        error: throwableToScreenError(
          error,
          fallback: 'Unable to load sleep data.',
        ),
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
