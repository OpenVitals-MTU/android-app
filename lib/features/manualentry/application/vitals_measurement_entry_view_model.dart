import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../core/presentation/measurement_input.dart';
import '../../../core/presentation/screen_error.dart';
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

/// Port of the Kotlin `VitalsMeasurementEntryError`.
enum VitalsMeasurementEntryError {
  invalidValue,
  missingWritePermission,
  writeFailed,
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
    @Default(false) bool isSavingEntry,
    String? editRecordId,
    DateTime? editTime,
    @Default(false) bool saveCompleted,
    VitalsMeasurementEntryError? entryError,
    ScreenError? writeError,
  }) = _VitalsMeasurementEntryState;

  bool get isEditMode => editRecordId != null;
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
      writeError: null,
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
      entryError: error == null ? null : VitalsMeasurementEntryError.writeFailed,
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

  void updateSecondaryInput(String text) {
    state = state.copyWith(
      secondaryInputText: text,
      saveCompleted: false,
      entryError: null,
      writeError: null,
    );
  }

  void updateEntryTime(DateTime time) {
    final now = DateTime.now();
    state = state.copyWith(
      editTime: time.isAfter(now) ? now : time,
      saveCompleted: false,
      entryError: null,
      writeError: null,
    );
  }

  /// Validates + writes (or updates) the measurement. [value] is the canonical
  /// primary value; [secondaryValue] the diastolic for blood pressure.
  Future<void> addEntry(double? value, {double? secondaryValue}) async {
    if (!state.canWrite) {
      state = state.copyWith(
        entryError: VitalsMeasurementEntryError.missingWritePermission,
        writeError: null,
      );
      return;
    }
    if (!isValidVitalsValue(type, value, secondaryValue)) {
      state = state.copyWith(
        entryError: VitalsMeasurementEntryError.invalidValue,
        writeError: null,
      );
      return;
    }

    state = state.copyWith(
      isSavingEntry: true,
      entryError: null,
      writeError: null,
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
    try {
      await ref.read(saveVitalsMeasurementUseCaseProvider)(
        request,
        editRecordId: editRecordId,
      );
      if (!ref.mounted) return;
      state = state.copyWith(
        inputText: state.isEditMode ? state.inputText : '',
        secondaryInputText:
            state.isEditMode ? state.secondaryInputText : '',
        isSavingEntry: false,
        saveCompleted: true,
        entryError: null,
        writeError: null,
      );
    } catch (error) {
      if (!ref.mounted) return;
      state = state.copyWith(
        isSavingEntry: false,
        entryError: VitalsMeasurementEntryError.writeFailed,
        writeError: throwableToScreenError(error),
      );
    }
  }

  void onSaveCompletedHandled() {
    state = state.copyWith(saveCompleted: false);
  }

  Future<void> _loadEditEntry() async {
    final recordId = editRecordId;
    if (recordId == null) return;
    try {
      final entry = await ref.read(loadVitalsMeasurementForEditUseCaseProvider)(
        type,
        recordId,
      );
      if (!ref.mounted) return;
      // Null covers both "no such record" and "not ours to edit".
      if (entry == null) {
        state = state.copyWith(
          entryError: VitalsMeasurementEntryError.writeFailed,
          writeError: const ScreenErrorMessage(
            'Only OpenVitals entries can be edited.',
          ),
        );
        return;
      }
      final now = DateTime.now();
      state = state.copyWith(
        inputText: _toDisplayInput(entry.value, type),
        secondaryInputText: entry.secondaryValue == null
            ? ''
            : _trimInput(entry.secondaryValue!),
        editTime: entry.time.isAfter(now) ? now : entry.time,
        entryError: null,
        writeError: null,
      );
    } catch (error) {
      if (!ref.mounted) return;
      state = state.copyWith(
        entryError: VitalsMeasurementEntryError.writeFailed,
        writeError: throwableToScreenError(error),
      );
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
