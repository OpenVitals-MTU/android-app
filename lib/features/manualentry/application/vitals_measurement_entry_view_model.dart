import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../core/presentation/command_state.dart';
import '../../../core/presentation/measurement_input.dart';
import '../../../core/presentation/screen_error.dart';
import '../../../core/result/result.dart';
import '../../../di/providers.dart';
import '../../../domain/model/vitals_models.dart';

part 'vitals_measurement_entry_view_model.freezed.dart';

const double _minSystolicMmHg = 20.0;
const double _maxSystolicMmHg = 200.0;
const double _minDiastolicMmHg = 10.0;
const double _maxDiastolicMmHg = 180.0;
const double _maxPercent = 100.0;
const double _maxRespiratoryRate = 1000.0;
const double _maxBodyTemperatureCelsius = 100.0;

/// Why the form refuses to save. Validation and gating only — a write that was
/// attempted and *failed* is not one of these; it lives in
/// [VitalsMeasurementEntryState.save] as a [CommandFailure], carrying the real
/// error.
enum VitalsMeasurementEntryError {
  invalidValue,
  missingWritePermission,
}

/// Riverpod port of the Kotlin `VitalsMeasurementEntryUiState`.
@freezed
abstract class VitalsMeasurementEntryState with _$VitalsMeasurementEntryState {
  const VitalsMeasurementEntryState._();

  const factory VitalsMeasurementEntryState({
    required VitalsMeasurementType type,
    @Default('') String inputText,
    @Default('') String secondaryInputText,
    @Default(<String>{}) Set<String> writePermissions,
    @Default(false) bool canWrite,
    @Default(true) bool isCheckingPermission,
    @Default(CommandState<void>.idle()) CommandState<void> save,
    String? editRecordId,
    DateTime? editTime,
    VitalsMeasurementEntryError? entryError,

    /// The edit prefill could not be read — a different thing from a save that
    /// failed, and it blocks the form before the user has done anything.
    ScreenError? prefillError,
  }) = _VitalsMeasurementEntryState;

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

/// Parses a raw vitals field (comma-tolerant). Port of the Kotlin
/// `String.toVitalsDoubleOrNull`.
double? parseVitalsDouble(String input) =>
    double.tryParse(input.trim().replaceAll(',', '.'));

/// Whether ([value], [secondaryValue]) form a valid measurement for [type].
/// Port of the Kotlin `isValidVitalsValue`.
bool isValidVitalsValue(
  VitalsMeasurementType type,
  double? value,
  double? secondaryValue,
) {
  if (value == null) return false;
  switch (type) {
    case VitalsMeasurementType.bloodPressure:
      final diastolic = secondaryValue;
      if (diastolic == null) return false;
      return value >= _minSystolicMmHg &&
          value <= _maxSystolicMmHg &&
          diastolic >= _minDiastolicMmHg &&
          diastolic <= _maxDiastolicMmHg &&
          value > diastolic;
    case VitalsMeasurementType.spo2:
      return value > 0.0 && value <= _maxPercent;
    case VitalsMeasurementType.respiratoryRate:
      return value > 0.0 && value <= _maxRespiratoryRate;
    case VitalsMeasurementType.bodyTemperature:
      return value > 0.0 && value <= _maxBodyTemperatureCelsius;
  }
}

/// Riverpod port of the Kotlin `VitalsMeasurementEntryViewModel`. One instance
/// per entry screen, bound to its [VitalsMeasurementType] and optional edit id.
class VitalsMeasurementEntryViewModel
    extends Notifier<VitalsMeasurementEntryState> {
  VitalsMeasurementEntryViewModel(this.type,
      {this.editRecordId, this.imperial = false});

  final VitalsMeasurementType type;
  final String? editRecordId;
  final bool imperial;

  @override
  VitalsMeasurementEntryState build() {
    Future.microtask(() async {
      if (!ref.mounted) return;
      await refreshPermission();
      if (ref.mounted) await _loadEditEntry();
    });
    return VitalsMeasurementEntryState(type: type, editRecordId: editRecordId);
  }

  Future<void> refreshPermission() async {
    state = state.copyWith(
      isCheckingPermission: true,
      entryError: null,
    );
    // The probe reports a failure rather than throwing it, so the permissions it
    // could not get a verdict for are still known here — see
    // [WritePermissionStatus].
    final status =
        await ref.read(checkVitalsWritePermissionUseCaseProvider)(type);
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

  void updateSecondaryInput(String text) {
    state = state.copyWith(
      secondaryInputText: text,
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

  /// Validates + writes (or updates) the measurement. [value] is the canonical
  /// primary value; [secondaryValue] the diastolic for blood pressure.
  Future<void> addEntry(double? value, {double? secondaryValue}) async {
    if (!state.canWrite) {
      state = state.copyWith(
        entryError: VitalsMeasurementEntryError.missingWritePermission,
      );
      return;
    }
    if (!isValidVitalsValue(type, value, secondaryValue)) {
      state = state.copyWith(
        entryError: VitalsMeasurementEntryError.invalidValue,
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
    final request = VitalsMeasurementWriteRequest(
      type: type,
      time: time,
      value: value!,
      secondaryValue: secondaryValue,
    );
    final result = await ref.read(saveVitalsMeasurementUseCaseProvider)(
      request,
      editRecordId: editRecordId,
    );
    if (!ref.mounted) return;
    switch (result) {
      case Ok():
        state = state.copyWith(
          inputText: state.isEditMode ? state.inputText : '',
          secondaryInputText:
              state.isEditMode ? state.secondaryInputText : '',
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
  /// [VitalsMeasurementEntryState.prefillError], not on the save command.
  Future<void> _loadEditEntry() async {
    final recordId = editRecordId;
    if (recordId == null) return;
    final result = await ref.read(loadVitalsMeasurementForEditUseCaseProvider)(
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
          secondaryInputText: value.secondaryValue == null
              ? ''
              : _trimInput(value.secondaryValue!),
          editTime: value.time.isAfter(now) ? now : value.time,
          entryError: null,
          prefillError: null,
        );
      case Err(:final failure):
        state = state.copyWith(prefillError: failure.toScreenError());
    }
  }

  String _toDisplayInput(double value, VitalsMeasurementType type) {
    final display =
        type == VitalsMeasurementType.bodyTemperature && imperial
            ? value * kFahrenheitPerCelsius + kFahrenheitFreezingPoint
            : value;
    return _trimInput(display);
  }
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
