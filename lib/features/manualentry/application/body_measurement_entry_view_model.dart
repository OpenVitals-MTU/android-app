import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../core/presentation/command_state.dart';
import '../../../core/presentation/measurement_input.dart';
import '../../../core/presentation/screen_error.dart';
import '../../../core/result/result.dart';
import '../../../di/providers.dart';
import '../../../domain/model/body_models.dart';

part 'body_measurement_entry_view_model.freezed.dart';

const double _maxWeightKg = 1000.0;
const double _maxHeightCm = 300.0;
const double _maxBodyFatPercent = 100.0;

/// Why the form refuses to save. Validation and gating only — a write that was
/// attempted and *failed* is not one of these; it lives in
/// [BodyMeasurementEntryState.save] as a [CommandFailure], carrying the real
/// error.
enum BodyMeasurementEntryError {
  invalidValue,
  missingWritePermission,
}

/// Riverpod port of the Kotlin `BodyMeasurementEntryUiState`.
@freezed
abstract class BodyMeasurementEntryState with _$BodyMeasurementEntryState {
  const BodyMeasurementEntryState._();

  const factory BodyMeasurementEntryState({
    required BodyMeasurementType type,
    @Default('') String inputText,
    @Default(<String>{}) Set<String> writePermissions,
    @Default(false) bool canWrite,
    @Default(true) bool isCheckingPermission,
    @Default(CommandState<void>.idle()) CommandState<void> save,
    String? editRecordId,
    DateTime? editTime,
    BodyMeasurementEntryError? entryError,

    /// The edit prefill could not be read — a different thing from a save that
    /// failed, and it blocks the form before the user has done anything.
    ScreenError? prefillError,
  }) = _BodyMeasurementEntryState;

  bool get isEditMode => editRecordId != null;

  bool get isSavingEntry => save is CommandRunning<void>;

  /// The error the form should be showing, if any: a failed prefill outranks a
  /// failed write, because it means the form was never trustworthy to begin
  /// with.
  ScreenError? get blockingError =>
      prefillError ??
      switch (save) {
        CommandFailure<void>(:final error) => error,
        _ => null,
      };
}

/// Riverpod port of the Kotlin `BodyMeasurementEntryViewModel`. One instance per
/// entry screen, bound to its [BodyMeasurementType] and optional edit record id.
class BodyMeasurementEntryViewModel extends Notifier<BodyMeasurementEntryState> {
  BodyMeasurementEntryViewModel(this.type, {this.editRecordId, this.imperial = false});

  final BodyMeasurementType type;
  final String? editRecordId;
  final bool imperial;

  @override
  BodyMeasurementEntryState build() {
    Future.microtask(() async {
      if (!ref.mounted) return;
      await refreshPermission();
      if (ref.mounted) await _loadEditEntry();
    });
    return BodyMeasurementEntryState(type: type, editRecordId: editRecordId);
  }

  Future<void> refreshPermission() async {
    state = state.copyWith(
      isCheckingPermission: true,
      entryError: null,
    );
    // The probe reports a failure rather than throwing it, so the permissions it
    // could not get a verdict for are still known here — see
    // [WritePermissionStatus].
    final status = await ref.read(checkBodyWritePermissionUseCaseProvider)(type);
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
          : CommandState.failure(throwableToScreenError(error)),
    );
  }

  void updateInput(String text) {
    state = state.copyWith(
      inputText: text,
      save: const CommandState.idle(),
      entryError: null,
    );
  }

  void updateEntryTime(DateTime time) {
    final now = DateTime.now();
    state = state.copyWith(
      editTime: time.isAfter(now) ? now : time,
      save: const CommandState.idle(),
      entryError: null,
    );
  }

  /// Validates + writes (or updates) the measurement. [canonicalValue] is the
  /// metric value; the screen converts from the display unit before calling.
  Future<void> addEntry(double? canonicalValue) async {
    if (!state.canWrite) {
      state = state.copyWith(
        entryError: BodyMeasurementEntryError.missingWritePermission,
      );
      return;
    }
    if (canonicalValue == null || !_isValidFor(canonicalValue, type)) {
      state = state.copyWith(
        entryError: BodyMeasurementEntryError.invalidValue,
      );
      return;
    }

    state = state.copyWith(
      save: const CommandState.running(),
      entryError: null,
    );
    final now = DateTime.now();
    final editTime = state.editTime;
    final time = editTime == null
        ? now
        : (editTime.isAfter(now) ? now : editTime);
    final request = BodyMeasurementWriteRequest(
      type: type,
      time: time,
      value: canonicalValue,
    );
    final result = await ref.read(saveBodyMeasurementUseCaseProvider)(
      request,
      editRecordId: editRecordId,
    );
    if (!ref.mounted) return;
    switch (result) {
      case Ok():
        state = state.copyWith(
          inputText: state.isEditMode ? state.inputText : '',
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

  /// Prefills the form from the record being edited. A failure here is a *read*
  /// failure — the form has nothing to correct — so it lands on
  /// [BodyMeasurementEntryState.prefillError], not on the save command.
  Future<void> _loadEditEntry() async {
    final recordId = editRecordId;
    if (recordId == null) return;
    final result = await ref.read(loadBodyMeasurementForEditUseCaseProvider)(
      type,
      recordId,
    );
    if (!ref.mounted) return;
    switch (result) {
      case Ok(:final value):
        // Null covers both "no such record" and "not ours to edit".
        if (value == null) {
          state = state.copyWith(
            prefillError: const ScreenErrorMessage(
              'Only OpenVitals entries can be edited.',
            ),
          );
          return;
        }
        final now = DateTime.now();
        state = state.copyWith(
          inputText: _toDisplayInput(value.value, type),
          editTime: value.time.isAfter(now) ? now : value.time,
          entryError: null,
          prefillError: null,
        );
      case Err(:final failure):
        state = state.copyWith(prefillError: failure.toScreenError());
    }
  }

  String _toDisplayInput(double value, BodyMeasurementType type) {
    final display = switch (type) {
      BodyMeasurementType.weight =>
        imperial ? value * kPoundsPerKilogram : value,
      BodyMeasurementType.height =>
        imperial ? value / kCentimetersPerInch : value,
      BodyMeasurementType.bodyFat => value,
    };
    return _trimInput(display);
  }

  bool _isValidFor(double value, BodyMeasurementType type) => switch (type) {
        BodyMeasurementType.weight => value > 0.0 && value <= _maxWeightKg,
        BodyMeasurementType.height => value > 0.0 && value <= _maxHeightCm,
        BodyMeasurementType.bodyFat =>
          value >= 0.0 && value <= _maxBodyFatPercent,
      };
}

/// Formats a double as a compact input string (2 decimals, trailing zeros
/// trimmed), matching the Kotlin `Double.toInputText`.
String _trimInput(double value) {
  var text = value.toStringAsFixed(2);
  if (text.contains('.')) {
    text = text.replaceAll(RegExp(r'0+$'), '').replaceAll(RegExp(r'\.$'), '');
  }
  return text;
}
