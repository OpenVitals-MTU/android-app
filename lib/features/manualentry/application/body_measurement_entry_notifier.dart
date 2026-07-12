import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../core/presentation/measurement_input.dart';
import '../../../core/presentation/screen_error.dart';
import '../../../di/providers.dart';
import '../../../domain/model/body_models.dart';

part 'body_measurement_entry_notifier.freezed.dart';

const double _maxWeightKg = 1000.0;
const double _maxHeightCm = 300.0;
const double _maxBodyFatPercent = 100.0;

/// Port of the Kotlin `BodyMeasurementEntryError`.
enum BodyMeasurementEntryError {
  invalidValue,
  missingWritePermission,
  writeFailed,
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
    @Default(false) bool isSavingEntry,
    String? editRecordId,
    DateTime? editTime,
    @Default(false) bool saveCompleted,
    BodyMeasurementEntryError? entryError,
    ScreenError? writeError,
  }) = _BodyMeasurementEntryState;

  bool get isEditMode => editRecordId != null;
}

/// Riverpod port of the Kotlin `BodyMeasurementEntryViewModel`. One instance per
/// entry screen, bound to its [BodyMeasurementType] and optional edit record id.
class BodyMeasurementEntryNotifier extends Notifier<BodyMeasurementEntryState> {
  BodyMeasurementEntryNotifier(this.type, {this.editRecordId, this.imperial = false});

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
      writeError: null,
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
      entryError: error == null ? null : BodyMeasurementEntryError.writeFailed,
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

  void updateEntryTime(DateTime time) {
    final now = DateTime.now();
    state = state.copyWith(
      editTime: time.isAfter(now) ? now : time,
      saveCompleted: false,
      entryError: null,
      writeError: null,
    );
  }

  /// Validates + writes (or updates) the measurement. [canonicalValue] is the
  /// metric value; the screen converts from the display unit before calling.
  Future<void> addEntry(double? canonicalValue) async {
    if (!state.canWrite) {
      state = state.copyWith(
        entryError: BodyMeasurementEntryError.missingWritePermission,
        writeError: null,
      );
      return;
    }
    if (canonicalValue == null || !_isValidFor(canonicalValue, type)) {
      state = state.copyWith(
        entryError: BodyMeasurementEntryError.invalidValue,
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
    final request = BodyMeasurementWriteRequest(
      type: type,
      time: time,
      value: canonicalValue,
    );
    try {
      await ref.read(saveBodyMeasurementUseCaseProvider)(
        request,
        editRecordId: editRecordId,
      );
      if (!ref.mounted) return;
      state = state.copyWith(
        inputText: state.isEditMode ? state.inputText : '',
        isSavingEntry: false,
        saveCompleted: true,
        entryError: null,
        writeError: null,
      );
    } catch (error) {
      if (!ref.mounted) return;
      state = state.copyWith(
        isSavingEntry: false,
        entryError: BodyMeasurementEntryError.writeFailed,
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
      final entry = await ref.read(loadBodyMeasurementForEditUseCaseProvider)(
        type,
        recordId,
      );
      if (!ref.mounted) return;
      // Null covers both "no such record" and "not ours to edit".
      if (entry == null) {
        state = state.copyWith(
          entryError: BodyMeasurementEntryError.writeFailed,
          writeError: const ScreenErrorMessage(
            'Only OpenVitals entries can be edited.',
          ),
        );
        return;
      }
      final now = DateTime.now();
      state = state.copyWith(
        inputText: _toDisplayInput(entry.value, type),
        editTime: entry.time.isAfter(now) ? now : entry.time,
        entryError: null,
        writeError: null,
      );
    } catch (error) {
      if (!ref.mounted) return;
      state = state.copyWith(
        entryError: BodyMeasurementEntryError.writeFailed,
        writeError: throwableToScreenError(error),
      );
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
