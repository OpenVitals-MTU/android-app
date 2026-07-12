import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/presentation/command_state.dart';
import '../../../core/presentation/measurement_input.dart';
import '../../../core/presentation/unit_formatter.dart';
import '../../../di/providers.dart';
import '../../../domain/model/vitals_models.dart';
import '../../../domain/preferences/unit_system.dart';
import '../../../l10n/app_localizations.dart';
import '../../../state/app_providers.dart';
import '../../../ui/components/health_connect_gate.dart';
import '../../../ui/components/ov_card.dart';
import '../../../ui/theme/app_colors.dart';
import 'manual_entry_form_scaffold.dart';
import 'manual_entry_timestamp_fields.dart';
import '../application/vitals_measurement_entry_view_model.dart';

const Color _oxygenColor = Color(0xFF00897B);
const Color _respiratoryColor = Color(0xFF5E97F6);
const Color _temperatureColor = Color(0xFFFF7043);

/// Vitals-measurement manual-entry screen pushed over the shell. Backs the
/// new-entry route (carries [vitalsMeasurementType]) and the edit route (also
/// carries [vitalsEntryId]).
///
/// Riverpod/Flutter port of the Kotlin `VitalsMeasurementEntryScreen`: single
/// value field (systolic + diastolic for blood pressure) writing a
/// `VitalsMeasurementWriteRequest`.
class VitalsMeasurementEntryScreen extends ConsumerStatefulWidget {
  const VitalsMeasurementEntryScreen({
    super.key,
    required this.vitalsMeasurementType,
    this.vitalsEntryId,
  });

  /// The `VitalsMeasurementType` storage name (BLOOD_PRESSURE, SPO2, …).
  final String vitalsMeasurementType;
  final String? vitalsEntryId;

  @override
  ConsumerState<VitalsMeasurementEntryScreen> createState() =>
      _VitalsMeasurementEntryScreenState();
}

class _VitalsMeasurementEntryScreenState
    extends ConsumerState<VitalsMeasurementEntryScreen>
    with RefreshPermissionOnResume {

  @override
  void refreshPermission() => ref.read(_provider.notifier).refreshPermission();

  final TextEditingController _controller = TextEditingController();
  final TextEditingController _secondaryController = TextEditingController();
  bool _syncedFromState = false;

  late final VitalsMeasurementType _type =
      VitalsMeasurementType.fromStorage(widget.vitalsMeasurementType) ??
          VitalsMeasurementType.bloodPressure;

  late final NotifierProvider<VitalsMeasurementEntryViewModel,
      VitalsMeasurementEntryState> _provider = NotifierProvider.autoDispose<
      VitalsMeasurementEntryViewModel, VitalsMeasurementEntryState>(
    () => VitalsMeasurementEntryViewModel(
      _type,
      editRecordId: widget.vitalsEntryId,
      imperial: ref.read(unitSystemProvider) == UnitSystem.imperial,
    ),
  );

  @override
  void dispose() {
    _controller.dispose();
    _secondaryController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    ref.listen(_provider.select((s) => s.save), (previous, next) {
      // The success is consumed exactly once, then the command is put back to
      // rest — otherwise returning to this route would replay the toast.
      if (next is CommandSuccess<void>) {
        ref.read(_provider.notifier).onSaveCompletedHandled();
        onManualEntrySaved(context, 'Measurement saved');
      }
    });
    ref.listen(_provider, (previous, next) {
      if (!_syncedFromState &&
          (next.inputText.isNotEmpty || next.secondaryInputText.isNotEmpty)) {
        if (_controller.text != next.inputText) {
          _controller.text = next.inputText;
        }
        if (_secondaryController.text != next.secondaryInputText) {
          _secondaryController.text = next.secondaryInputText;
        }
        _syncedFromState = true;
      }
    });

    final writePermissions =
        ref.watch(vitalsWritePermissionsProvider(_type));

    return Scaffold(
      appBar: AppBar(title: Text(vitalsMeasurementTitle(_type, l10n))),
      body: HealthConnectGate(
        requiredPermissions: writePermissions,
        child: _VitalsEntryForm(
          type: _type,
          provider: _provider,
          controller: _controller,
          secondaryController: _secondaryController,
        ),
      ),
    );
  }
}

class _VitalsEntryForm extends ConsumerWidget {
  const _VitalsEntryForm({
    required this.type,
    required this.provider,
    required this.controller,
    required this.secondaryController,
  });

  final VitalsMeasurementType type;
  final NotifierProvider<VitalsMeasurementEntryViewModel,
      VitalsMeasurementEntryState> provider;
  final TextEditingController controller;
  final TextEditingController secondaryController;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    final state = ref.watch(provider);
    final notifier = ref.read(provider.notifier);
    final formatter = ref.watch(unitFormatterProvider);
    final title = vitalsMeasurementTitle(type, l10n);
    final unitLabel = _vitalsUnitLabel(type, formatter);
    final isBloodPressure = type == VitalsMeasurementType.bloodPressure;
    final enabled =
        state.canWrite && !state.isSavingEntry && !state.isCheckingPermission;

    return ListView(
      padding: const EdgeInsets.symmetric(vertical: 8),
      children: [
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
          child: OpenVitalsCard(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Row(
                    children: [
                      Icon(_vitalsIcon(type),
                          color: _vitalsAccent(type), size: 22),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(title, style: theme.textTheme.titleSmall),
                            Text(
                              state.canWrite
                                  ? l10n.vitalsEntrySubtitle(title)
                                  : l10n.vitalsEntryPermissionNeeded(title),
                              style: theme.textTheme.bodySmall?.copyWith(
                                color: theme.colorScheme.onSurfaceVariant,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  if (isBloodPressure)
                    Row(
                      children: [
                        Expanded(
                          child: _valueField(
                            controller: controller,
                            label: l10n.vitalsEntrySystolicLabel,
                            enabled: !state.isSavingEntry,
                            onChanged: notifier.updateInput,
                          ),
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: _valueField(
                            controller: secondaryController,
                            label: l10n.vitalsEntryDiastolicLabel,
                            enabled: !state.isSavingEntry,
                            onChanged: notifier.updateSecondaryInput,
                          ),
                        ),
                      ],
                    )
                  else
                    _valueField(
                      controller: controller,
                      label: l10n.vitalsEntryValueLabel(title, unitLabel),
                      enabled: !state.isSavingEntry,
                      onChanged: notifier.updateInput,
                    ),
                  if (state.isEditMode) ...[
                    const SizedBox(height: 12),
                    ManualEntryTimestampFields(
                      timestamp: state.editTime,
                      enabled: !state.isSavingEntry,
                      onChanged: notifier.updateEntryTime,
                    ),
                  ],
                  const SizedBox(height: 12),
                  FilledButton.icon(
                    onPressed: enabled
                        ? () => notifier.addEntry(
                              _canonicalValue(state.inputText, type, formatter),
                              secondaryValue: isBloodPressure
                                  ? parseVitalsDouble(state.secondaryInputText)
                                  : null,
                            )
                        : null,
                    icon: Icon(state.isEditMode ? Icons.check : Icons.add,
                        size: 18),
                    label: Text(state.isEditMode
                        ? l10n.actionSave
                        : l10n.vitalsEntryAddSelected(title)),
                  ),
                  if (_errorText(state, title, l10n) case final message?)
                    Padding(
                      padding: const EdgeInsets.only(top: 12),
                      child: Text(
                        message,
                        style: theme.textTheme.bodySmall
                            ?.copyWith(color: theme.colorScheme.error),
                      ),
                    ),
                ],
              ),
            ),
          ),
        ),
      ],
    );
  }

  Widget _valueField({
    required TextEditingController controller,
    required String label,
    required bool enabled,
    required ValueChanged<String> onChanged,
  }) =>
      TextField(
        controller: controller,
        enabled: enabled,
        maxLines: 1,
        keyboardType: const TextInputType.numberWithOptions(decimal: true),
        onChanged: onChanged,
        decoration: InputDecoration(
          border: const OutlineInputBorder(),
          labelText: label,
        ),
      );

  /// The one message the form shows: why it refuses to save, or — failing that
  /// — why the last attempt to save (or to read the record being edited) did
  /// not land.
  String? _errorText(
    VitalsMeasurementEntryState state,
    String title,
    AppLocalizations l10n,
  ) {
    final entryError = state.entryError;
    if (entryError != null) {
      return switch (entryError) {
        VitalsMeasurementEntryError.invalidValue => l10n.vitalsEntryInvalidValue,
        VitalsMeasurementEntryError.missingWritePermission =>
          l10n.vitalsEntryPermissionNeeded(title),
      };
    }
    final blocking = state.blockingError;
    if (blocking == null) return null;
    return l10n.vitalsEntryWriteFailed(screenErrorText(blocking, l10n));
  }
}

/// The localized metric name for [type], shared by the app bar, the field labels
/// and the error copy.
String vitalsMeasurementTitle(
  VitalsMeasurementType type,
  AppLocalizations l10n,
) =>
    switch (type) {
      VitalsMeasurementType.bloodPressure => l10n.metricBloodPressure,
      VitalsMeasurementType.spo2 => l10n.metricSpo2,
      VitalsMeasurementType.respiratoryRate => l10n.metricRespiratoryRate,
      VitalsMeasurementType.bodyTemperature => l10n.metricBodyTemp,
    };

/// Port of the Kotlin `VitalsMeasurementType.inputUnitLabel`. Only temperature
/// varies by unit system, and that label lives in [MeasurementInput].
String _vitalsUnitLabel(VitalsMeasurementType type, UnitFormatter formatter) =>
    switch (type) {
      VitalsMeasurementType.bloodPressure => 'mmHg',
      VitalsMeasurementType.spo2 => '%',
      VitalsMeasurementType.respiratoryRate => 'br/min',
      VitalsMeasurementType.bodyTemperature => formatter.temperatureInputUnit,
    };

/// The typed value in its stored (metric) unit. Port of the Kotlin
/// `canonicalVitalsValue`.
double? _canonicalValue(
  String input,
  VitalsMeasurementType type,
  UnitFormatter formatter,
) =>
    type == VitalsMeasurementType.bodyTemperature
        ? formatter.temperatureInputToCelsius(input)
        : parseDecimalInput(input);

IconData _vitalsIcon(VitalsMeasurementType type) => switch (type) {
      VitalsMeasurementType.bloodPressure => Icons.favorite,
      VitalsMeasurementType.spo2 => Icons.favorite_border,
      VitalsMeasurementType.respiratoryRate => Icons.air,
      VitalsMeasurementType.bodyTemperature => Icons.device_thermostat,
    };

Color _vitalsAccent(VitalsMeasurementType type) => switch (type) {
      VitalsMeasurementType.bloodPressure => AppColors.vitals,
      VitalsMeasurementType.spo2 => _oxygenColor,
      VitalsMeasurementType.respiratoryRate => _respiratoryColor,
      VitalsMeasurementType.bodyTemperature => _temperatureColor,
    };
