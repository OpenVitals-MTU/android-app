import 'package:freezed_annotation/freezed_annotation.dart';

import 'screen_error.dart';

part 'command_state.freezed.dart';

/// The lifecycle of one user-triggered action (a "command" in the Flutter
/// app-architecture sense): save, delete, import. A view-model exposes one
/// `CommandState` field per action on its state, replacing ad-hoc
/// `isSaving`/`saveCompleted` boolean pairs.
///
/// The screen renders [CommandRunning] as the action's busy state, shows
/// [CommandFailure.error], and consumes [CommandSuccess] exactly once
/// (navigate/snackbar, then ask the view-model to reset to [CommandIdle]).
/// Read paths keep the existing `isLoading` + `ScreenError?` state fields —
/// this type is for mutations only.
@freezed
sealed class CommandState<T> with _$CommandState<T> {
  const factory CommandState.idle() = CommandIdle<T>;

  const factory CommandState.running() = CommandRunning<T>;

  const factory CommandState.success(T value) = CommandSuccess<T>;

  const factory CommandState.failure(ScreenError error) = CommandFailure<T>;
}
