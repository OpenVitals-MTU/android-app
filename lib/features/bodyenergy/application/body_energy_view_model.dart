import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../core/presentation/screen_error.dart';
import '../../../core/time/local_date.dart';
import '../../../data/repository/contract/body_energy_repository.dart';
import '../../../di/providers.dart';
import '../../../domain/model/refresh_mode.dart';

part 'body_energy_view_model.freezed.dart';

/// The Riverpod port of the Kotlin `BodyEnergyUiState`. Body Energy is a
/// selected-day derived-wellness detail (not a Day/Week/Month/Year screen), so
/// the state carries only the selected day, the loaded timeline result, and
/// loading/error flags.
@freezed
abstract class BodyEnergyState with _$BodyEnergyState {
  const BodyEnergyState._();

  const factory BodyEnergyState({
    required LocalDate selectedDate,
    @Default(true) bool isLoading,
    ScreenError? error,
    BodyEnergyTimelineResult? result,
  }) = _BodyEnergyState;

  bool get canGoForward => selectedDate.isBefore(LocalDate.now());
}

/// The Riverpod port of the Kotlin `BodyEnergyViewModel`. A manual [Notifier]
/// (no codegen) that loads the 5-minute-bucket timeline for the selected day via
/// [BodyEnergyRepository] (which runs `calculateBodyEnergyTimeline`, backed by
/// the timeline cache). A monotonic [_generation] guard drops stale results.
class BodyEnergyViewModel extends Notifier<BodyEnergyState> {
  int _generation = 0;

  @override
  BodyEnergyState build() =>
      BodyEnergyState(selectedDate: LocalDate.now());

  Future<void> load(
    LocalDate date, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) async {
    final clamped = date.coerceAtMost(LocalDate.now());
    final generation = ++_generation;
    final loadBodyEnergyTimeline =
        ref.read(loadBodyEnergyTimelineUseCaseProvider);

    state = state.copyWith(
      selectedDate: clamped,
      isLoading: true,
      error: null,
    );

    try {
      final result =
          await loadBodyEnergyTimeline(clamped, refreshMode: refreshMode);
      if (!ref.mounted || generation != _generation) return;
      state = state.copyWith(isLoading: false, result: result, error: null);
    } catch (error) {
      if (!ref.mounted || generation != _generation) return;
      state = state.copyWith(
        isLoading: false,
        error: throwableToScreenError(
          error,
          fallback: 'Unable to load Body Energy.',
        ),
      );
    }
  }

  void previousDay() => load(state.selectedDate.minusDays(1));

  void nextDay() {
    final next = state.selectedDate.plusDays(1);
    if (!next.isAfter(LocalDate.now())) load(next);
  }

  void selectDate(LocalDate date) => load(date);

  Future<void> refresh() =>
      load(state.selectedDate, refreshMode: RefreshMode.force);
}

/// The Body Energy state provider (manually declared, no codegen).
final bodyEnergyProvider =
    NotifierProvider<BodyEnergyViewModel, BodyEnergyState>(
  BodyEnergyViewModel.new,
);
