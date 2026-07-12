import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../core/presentation/screen_error.dart';
import '../../../core/result/result.dart';
import '../../../di/providers.dart';
import '../../../domain/model/sleep_models.dart';
import 'sleep_detail_display.dart';

part 'sleep_detail_view_model.freezed.dart';

/// The Riverpod port of the Kotlin `SleepDetailUiState` — one sleep session,
/// with the stage order and per-stage totals the screen renders precomputed
/// into [SleepDetailState.display].
@freezed
abstract class SleepDetailState with _$SleepDetailState {
  const SleepDetailState._();

  const factory SleepDetailState({
    @Default(true) bool isLoading,
    SleepData? session,
    ScreenError? error,
    SleepDetailDisplay? display,
  }) = _SleepDetailState;
}

/// The Riverpod port of the Kotlin `SleepDetailViewModel`.
///
/// One instance per detail screen: the screen creates an auto-dispose provider
/// bound to its `sleepId` (so stacked detail routes stay independent) and
/// [build] kicks off the first load.
class SleepDetailViewModel extends Notifier<SleepDetailState> {
  SleepDetailViewModel(this.sleepId);

  final String sleepId;
  int _generation = 0;

  @override
  SleepDetailState build() {
    Future.microtask(() {
      if (ref.mounted) load();
    });
    return const SleepDetailState();
  }

  Future<void> load() async {
    if (sleepId.trim().isEmpty) {
      state = const SleepDetailState(
        isLoading: false,
        error: ScreenErrorMissingArgument(),
      );
      return;
    }

    final generation = ++_generation;
    final loadSleepDetail = ref.read(loadSleepDetailUseCaseProvider);
    state = SleepDetailState(
      isLoading: true,
      session: state.session,
      display: state.display,
    );

    final result = await loadSleepDetail(sleepId);
    if (!ref.mounted || generation != _generation) return;
    switch (result) {
      case Ok(:final value):
        state = SleepDetailState(
          isLoading: false,
          session: value,
          display: value == null ? null : buildSleepDetailDisplay(value),
          error: value == null ? const ScreenErrorNotFound() : null,
        );
      case Err(:final failure):
        state = SleepDetailState(
          isLoading: false,
          error: failure.toScreenError(
            fallback: 'Unable to load the sleep session.',
          ),
        );
    }
  }
}
