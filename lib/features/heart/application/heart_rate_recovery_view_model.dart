import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../core/period/period_load_query.dart';
import '../../../core/period/period_selection.dart';
import '../../../core/period/time_range.dart';
import '../../../core/presentation/screen_error.dart';
import '../../../core/result/result.dart';
import '../../../core/time/local_date.dart';
import '../../../di/providers.dart';
import '../../../state/app_providers.dart';
import '../../../domain/usecase/load_heart_rate_recovery_period_use_case.dart';

part 'heart_rate_recovery_view_model.freezed.dart';

@freezed
abstract class HeartRateRecoveryState with _$HeartRateRecoveryState {
  const HeartRateRecoveryState._();

  const factory HeartRateRecoveryState({
    required LocalDate selectedDate,
    @Default(TimeRange.month) TimeRange selectedRange,
    @Default(true) bool isLoading,
    ScreenError? error,
    HeartRateRecoveryPeriodData? data,
  }) = _HeartRateRecoveryState;

  /// Every workout in the period that was hard enough to be worth reporting on, whether
  /// or not its recovery could be measured. The ones that could NOT are the point: a
  /// screen that quietly dropped them would look like the user simply had not trained.
  List<HeartRateRecoverySessionReading> get readings =>
      data?.readings ?? const [];

  List<HeartRateRecoverySessionReading> get comparable =>
      data?.comparable ?? const [];
}

/// The read path for the heart-rate-recovery history.
///
/// Nothing is stored. Every point on this screen is recomputed, on the spot, from the
/// heart-rate samples Health Connect holds — the same pure function the single-workout
/// card uses, so the two can never disagree about the same workout.
class HeartRateRecoveryViewModel extends Notifier<HeartRateRecoveryState> {
  int _generation = 0;
  PeriodLoadQuery? _lastQuery;

  @override
  HeartRateRecoveryState build() =>
      HeartRateRecoveryState(selectedDate: LocalDate.now());

  Future<void> load(PeriodSelection selection) {
    final query = PeriodLoadQuery(
      range: selection.selectedRange,
      anchorDate: selection.selectedDate,
      weekPeriodMode: ref.read(weekPeriodModeProvider),
    );
    state = state.copyWith(
      selectedDate: selection.selectedDate,
      selectedRange: selection.selectedRange,
    );
    return _run(query);
  }

  Future<void> refresh() {
    final query = _lastQuery;
    if (query == null) return Future.value();
    return _run(query);
  }

  Future<void> _run(PeriodLoadQuery query) async {
    _lastQuery = query;
    final generation = ++_generation;
    state = state.copyWith(isLoading: true, error: null);

    final profile =
        ref.read(preferencesRepositoryProvider).bodyProfileListenable.value;
    final result =
        await ref.read(loadHeartRateRecoveryPeriodUseCaseProvider)(
      query,
      profile: profile,
    );
    if (!ref.mounted || generation != _generation) return;

    switch (result) {
      case Ok(:final value):
        state = state.copyWith(isLoading: false, data: value, error: null);
      case Err(:final failure):
        state = state.copyWith(
          isLoading: false,
          error: failure.toScreenError(
            fallback: 'Could not load heart rate recovery.',
          ),
        );
    }
  }
}

final heartRateRecoveryProvider =
    NotifierProvider<HeartRateRecoveryViewModel, HeartRateRecoveryState>(
  HeartRateRecoveryViewModel.new,
);
