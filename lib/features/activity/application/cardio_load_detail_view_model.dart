import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../core/presentation/screen_error.dart';
import '../../../core/time/local_date.dart';
import '../../../di/providers.dart';
import '../../../domain/insights/cardio_load.dart';

part 'cardio_load_detail_view_model.freezed.dart';

/// The Riverpod port of the Kotlin `ActivityOverviewUiState`, reduced to the
/// single day the cardio-load detail renders.
@freezed
abstract class CardioLoadState with _$CardioLoadState {
  const factory CardioLoadState({
    required LocalDate date,
    @Default(true) bool isLoading,
    ScreenError? error,
    @Default(CardioLoadEstimate.noData) CardioLoadEstimate estimate,
    @Default(0) int steps,
    double? activeCaloriesKcal,
  }) = _CardioLoadState;
}

/// The Riverpod port of the Kotlin `ActivityOverviewViewModel` +
/// `CardioLoadDetailScreen`, computing today's cardio-load estimate via
/// [calculateCardioLoad] over a 30-day heart-rate baseline window.
class CardioLoadViewModel extends Notifier<CardioLoadState> {
  int _generation = 0;

  @override
  CardioLoadState build() {
    Future.microtask(() {
      if (ref.mounted) _load();
    });
    return CardioLoadState(date: LocalDate.now());
  }

  Future<void> refresh() => _load();

  Future<void> _load() async {
    final generation = ++_generation;
    final today = LocalDate.now();
    final loadCardioLoadDetail = ref.read(loadCardioLoadDetailUseCaseProvider);

    state = state.copyWith(isLoading: true, error: null, date: today);

    try {
      // The 30-day baseline window, the four reads it spans and the scoring of
      // today's slice against them are domain knowledge, and live in the use
      // case. What stays here is the generation guard and the state mapping.
      final result = await loadCardioLoadDetail(today);
      if (!ref.mounted || generation != _generation) return;

      state = state.copyWith(
        isLoading: false,
        estimate: result.estimate,
        steps: result.steps,
        activeCaloriesKcal: result.activeCaloriesKcal,
      );
    } catch (error) {
      if (!ref.mounted || generation != _generation) return;
      state = state.copyWith(
        isLoading: false,
        error: throwableToScreenError(
          error,
          fallback: 'Unable to load cardio load.',
        ),
      );
    }
  }
}

final cardioLoadProvider =
    NotifierProvider<CardioLoadViewModel, CardioLoadState>(
  CardioLoadViewModel.new,
);
