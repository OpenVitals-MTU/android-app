import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/presentation/screen_error.dart';
import '../../di/providers.dart';
import '../../domain/model/sleep_models.dart';

/// The Riverpod port of the Kotlin `SleepDetailUiState` — one sleep session.
class SleepDetailState {
  const SleepDetailState({
    this.isLoading = true,
    this.session,
    this.error,
  });

  final bool isLoading;
  final SleepData? session;
  final ScreenError? error;
}

/// The Riverpod port of the Kotlin `SleepDetailViewModel`.
///
/// One instance per detail screen: the screen creates an auto-dispose provider
/// bound to its `sleepId` (so stacked detail routes stay independent) and
/// [build] kicks off the first load.
class SleepDetailNotifier extends Notifier<SleepDetailState> {
  SleepDetailNotifier(this.sleepId);

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
    );

    try {
      final session = await loadSleepDetail(sleepId);
      if (!ref.mounted || generation != _generation) return;
      state = SleepDetailState(
        isLoading: false,
        session: session,
        error: session == null ? const ScreenErrorNotFound() : null,
      );
    } catch (error) {
      if (!ref.mounted || generation != _generation) return;
      state = SleepDetailState(
        isLoading: false,
        error: throwableToScreenError(
          error,
          fallback: 'Unable to load the sleep session.',
        ),
      );
    }
  }
}
