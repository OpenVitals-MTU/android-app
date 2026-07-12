import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../core/presentation/screen_error.dart';
import '../../di/providers.dart';
import '../../domain/model/nutrition_models.dart';

part 'carbs_entry_notifier.freezed.dart';

/// Maximum carbohydrate grams accepted by a single manual entry (Kotlin
/// `MaxCarbsGrams`).
const double kMaxCarbsGrams = 10000.0;

/// Port of the Kotlin `CarbsEntryError`.
enum CarbsEntryError {
  invalidValue,
  missingWritePermission,
  writeFailed,
}

/// Riverpod port of the Kotlin `CarbsEntryUiState`.
@freezed
abstract class CarbsEntryState with _$CarbsEntryState {
  const factory CarbsEntryState({
    @Default('') String inputText,
    @Default(<String>{}) Set<String> writePermissions,
    @Default(false) bool canWrite,
    @Default(true) bool isCheckingPermission,
    @Default(false) bool isSavingEntry,
    @Default(false) bool saveCompleted,
    CarbsEntryError? entryError,
    ScreenError? writeError,
  }) = _CarbsEntryState;
}

/// Riverpod port of the Kotlin `CarbsEntryViewModel`.
class CarbsEntryNotifier extends Notifier<CarbsEntryState> {
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
      writeError: null,
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
      entryError: error == null ? null : CarbsEntryError.writeFailed,
      writeError: error == null ? null : throwableToScreenError(error),
    );
  }

  void updateInput(String text) {
    state = state.copyWith(
      inputText: text,
      saveCompleted: false,
      entryError: null,
      writeError: null,
    );
  }

  /// Validates + writes the carbohydrate entry. [carbsGrams] is the canonical
  /// (metric) value; the screen converts from the display unit before calling.
  Future<void> addEntry(double? carbsGrams) async {
    if (!state.canWrite) {
      state = state.copyWith(
        entryError: CarbsEntryError.missingWritePermission,
        writeError: null,
      );
      return;
    }
    if (carbsGrams == null || !_isValidCarbsGrams(carbsGrams)) {
      state = state.copyWith(
        entryError: CarbsEntryError.invalidValue,
        writeError: null,
      );
      return;
    }

    state = state.copyWith(
      isSavingEntry: true,
      entryError: null,
      writeError: null,
    );
    try {
      await ref.read(saveCarbsEntryUseCaseProvider)(
        NutritionWriteRequest.carbs(DateTime.now(), carbsGrams),
      );
      if (!ref.mounted) return;
      state = state.copyWith(
        inputText: '',
        isSavingEntry: false,
        saveCompleted: true,
        entryError: null,
        writeError: null,
      );
    } catch (error) {
      if (!ref.mounted) return;
      state = state.copyWith(
        isSavingEntry: false,
        entryError: CarbsEntryError.writeFailed,
        writeError: throwableToScreenError(error),
      );
    }
  }

  void onSaveCompletedHandled() {
    state = state.copyWith(saveCompleted: false);
  }

  bool _isValidCarbsGrams(double value) =>
      value.isFinite && value > 0.0 && value <= kMaxCarbsGrams;
}
