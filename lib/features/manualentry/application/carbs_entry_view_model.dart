import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../core/presentation/command_state.dart';
import '../../../core/presentation/screen_error.dart';
import '../../../core/result/result.dart';
import '../../../di/providers.dart';
import '../../../domain/model/nutrition_models.dart';

part 'carbs_entry_view_model.freezed.dart';

/// Maximum carbohydrate grams accepted by a single manual entry (Kotlin
/// `MaxCarbsGrams`).
const double kMaxCarbsGrams = 10000.0;

/// Why the form refuses to save. Validation and gating only — a write that was
/// attempted and *failed* is not one of these; it lives in
/// [CarbsEntryState.save] as a [CommandFailure], carrying the real error.
enum CarbsEntryError {
  invalidValue,
  missingWritePermission,
}

/// Riverpod port of the Kotlin `CarbsEntryUiState`.
@freezed
abstract class CarbsEntryState with _$CarbsEntryState {
  const CarbsEntryState._();

  const factory CarbsEntryState({
    @Default('') String inputText,
    @Default(<String>{}) Set<String> writePermissions,
    @Default(false) bool canWrite,
    @Default(true) bool isCheckingPermission,
    @Default(CommandState<void>.idle()) CommandState<void> save,
    CarbsEntryError? entryError,
  }) = _CarbsEntryState;

  bool get isSavingEntry => save is CommandRunning<void>;

  /// The error the form is stuck on: the last write (or permission probe) that
  /// did not land. There is no prefill here — a carbs entry is never edited.
  ScreenError? get blockingError => switch (save) {
        CommandFailure<void>(:final error) => error,
        _ => null,
      };
}

/// Riverpod port of the Kotlin `CarbsEntryViewModel`.
class CarbsEntryViewModel extends Notifier<CarbsEntryState> {
  @override
  CarbsEntryState build() {
    Future.microtask(() {
      if (ref.mounted) refreshPermission();
    });
    return const CarbsEntryState();
  }

  Future<void> refreshPermission() async {
    state = state.copyWith(
      isCheckingPermission: true,
      entryError: null,
    );
    // The probe reports a failure rather than throwing it, so the permissions it
    // could not get a verdict for are still known here — see
    // [WritePermissionStatus].
    final status = await ref.read(checkNutritionWritePermissionUseCaseProvider)();
    if (!ref.mounted) return;
    final error = status.error;
    state = state.copyWith(
      isCheckingPermission: false,
      writePermissions: status.permissions,
      canWrite: status.granted,
      // A probe that could not answer is surfaced where a failed write is:
      // either way the form cannot promise the save will land.
      save: error == null
          ? const CommandState.idle()
          : CommandState.failure(error.toScreenError()),
    );
  }

  void updateInput(String text) {
    state = state.copyWith(
      inputText: text,
      save: const CommandState.idle(),
      entryError: null,
    );
  }

  /// Validates + writes the carbohydrate entry. [carbsGrams] is the canonical
  /// (metric) value; the screen converts from the display unit before calling.
  Future<void> addEntry(double? carbsGrams) async {
    if (!state.canWrite) {
      state = state.copyWith(
        entryError: CarbsEntryError.missingWritePermission,
      );
      return;
    }
    if (carbsGrams == null || !_isValidCarbsGrams(carbsGrams)) {
      state = state.copyWith(
        entryError: CarbsEntryError.invalidValue,
      );
      return;
    }

    state = state.copyWith(
      save: const CommandState.running(),
      entryError: null,
    );
    final result = await ref.read(saveCarbsEntryUseCaseProvider)(
      NutritionWriteRequest.carbs(DateTime.now(), carbsGrams),
    );
    if (!ref.mounted) return;
    switch (result) {
      case Ok():
        state = state.copyWith(
          inputText: '',
          save: const CommandState.success(null),
          entryError: null,
        );
      case Err(:final failure):
        state = state.copyWith(
          save: CommandState.failure(failure.toScreenError()),
          entryError: null,
        );
    }
  }

  /// The screen consumed the success (it showed the toast and left), so the
  /// command returns to rest — otherwise re-entering the route would fire it
  /// again.
  void onSaveCompletedHandled() {
    state = state.copyWith(save: const CommandState.idle());
  }

  bool _isValidCarbsGrams(double value) =>
      value.isFinite && value > 0.0 && value <= kMaxCarbsGrams;
}
