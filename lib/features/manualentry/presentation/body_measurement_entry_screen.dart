import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/presentation/screen_error.dart';
import '../../../core/presentation/measurement_input.dart';
import '../../../core/presentation/unit_formatter.dart';
import '../../../di/providers.dart';
import '../../../domain/model/body_models.dart';
import '../../../domain/preferences/unit_system.dart';
import '../../../l10n/app_localizations.dart';
import '../../../state/app_providers.dart';
import '../../../ui/components/health_connect_gate.dart';
import '../../../ui/components/ov_card.dart';
import '../../../ui/theme/app_colors.dart';
import '../application/body_measurement_entry_view_model.dart';
import 'manual_entry_form_scaffold.dart';
import 'manual_entry_timestamp_fields.dart';

/// Body-measurement manual-entry screen pushed over the shell. Backs the
/// new-entry route (carries [bodyMeasurementType]) and the edit route (also
/// carries [bodyEntryId]).
///
/// Riverpod/Flutter port of the Kotlin `BodyMeasurementEntryScreen`: a value
/// field (metric/imperial aware) writing a `BodyMeasurementWriteRequest`.
class BodyMeasurementEntryScreen extends ConsumerStatefulWidget {
  const BodyMeasurementEntryScreen({
    super.key,
    required this.bodyMeasurementType,
    this.bodyEntryId,
  });

  /// The `BodyMeasurementType` storage name (WEIGHT, HEIGHT, BODY_FAT).
  final String bodyMeasurementType;
  final String? bodyEntryId;

  @override
  ConsumerState<BodyMeasurementEntryScreen> createState() =>
      _BodyMeasurementEntryScreenState();
}

class _BodyMeasurementEntryScreenState
    extends ConsumerState<BodyMeasurementEntryScreen>
    with RefreshPermissionOnResume {
  final TextEditingController _controller = TextEditingController();
  bool _syncedFromState = false;

  late final BodyMeasurementType _type =
      BodyMeasurementType.fromStorage(widget.bodyMeasurementType) ??
          BodyMeasurementType.weight;

  late final NotifierProvider<BodyMeasurementEntryViewModel,
      BodyMeasurementEntryState> _provider = NotifierProvider.autoDispose<
      BodyMeasurementEntryViewModel, BodyMeasurementEntryState>(
    () => BodyMeasurementEntryViewModel(
      _type,
      editRecordId: widget.bodyEntryId,
      imperial: ref.read(unitSystemProvider) == UnitSystem.imperial,
    ),
  );

  @override
  void refreshPermission() => ref.read(_provider.notifier).refreshPermission();

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    ref.listen(_provider.select((s) => s.saveCompleted), (previous, next) {
      if (next) {
        ref.read(_provider.notifier).onSaveCompletedHandled();
        onManualEntrySaved(context, 'Measurement saved');
      }
    });
    // In edit mode the notifier loads the existing value into the input text;
    // reflect it into the controller once.
    ref.listen(_provider.select((s) => s.inputText), (previous, next) {
      if (!_syncedFromState && next.isNotEmpty && _controller.text != next) {
        _controller.text = next;
        _syncedFromState = true;
      }
    });

    final writePermissions =
        ref.watch(bodyRepositoryProvider).bodyWritePermissions(_type);

    return Scaffold(
      appBar: AppBar(title: Text(bodyMeasurementTitle(_type, l10n))),
      body: HealthConnectGate(
        requiredPermissions: writePermissions,
        child: _BodyEntryForm(
          type: _type,
          provider: _provider,
          controller: _controller,
        ),
      ),
    );
  }
}

class _BodyEntryForm extends ConsumerWidget {
  const _BodyEntryForm({
    required this.type,
    required this.provider,
    required this.controller,
  });

  final BodyMeasurementType type;
  final NotifierProvider<BodyMeasurementEntryViewModel,
      BodyMeasurementEntryState> provider;
  final TextEditingController controller;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    final state = ref.watch(provider);
    final notifier = ref.read(provider.notifier);
    final formatter = ref.watch(unitFormatterProvider);
    final title = bodyMeasurementTitle(type, l10n);
    final unitLabel = _bodyUnitLabel(type, formatter);
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
                      Icon(_bodyIcon(type),
                          color: _bodyAccent(type), size: 22),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(title, style: theme.textTheme.titleSmall),
                            Text(
                              state.canWrite
                                  ? l10n.bodyEntrySubtitle(title)
                                  : l10n.bodyEntryPermissionNeeded(title),
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
                  TextField(
                    controller: controller,
                    enabled: !state.isSavingEntry,
                    maxLines: 1,
                    keyboardType: const TextInputType.numberWithOptions(
                      decimal: true,
                    ),
                    onChanged: notifier.updateInput,
                    decoration: InputDecoration(
                      border: const OutlineInputBorder(),
                      labelText: l10n.bodyEntryValueLabel(title, unitLabel),
                    ),
                  ),
                  // Kotlin 1.9.0 (3420d23) dropped the edit-mode gate: you can
                  // now date a *new* weight/height/body-fat entry, not only an
                  // edited one. The notifier already supported it (`addEntry`
                  // uses `editTime ?? now`, clamped to now).
                  const SizedBox(height: 12),
                  ManualEntryTimestampFields(
                    timestamp: state.editTime,
                    enabled: !state.isSavingEntry,
                    onChanged: notifier.updateEntryTime,
                  ),
                  const SizedBox(height: 12),
                  FilledButton.icon(
                    onPressed: enabled
                        ? () => notifier
                            .addEntry(_canonicalValue(state.inputText, type, formatter))
                        : null,
                    icon: Icon(state.isEditMode ? Icons.check : Icons.add,
                        size: 18),
                    label: Text(state.isEditMode
                        ? l10n.actionSave
                        : l10n.bodyEntryAddSelected(title)),
                  ),
                  if (state.entryError != null)
                    Padding(
                      padding: const EdgeInsets.only(top: 12),
                      child: Text(
                        _errorText(
                            state.entryError!, state.writeError, title, l10n),
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

  String _errorText(
    BodyMeasurementEntryError error,
    ScreenError? writeError,
    String title,
    AppLocalizations l10n,
  ) {
    switch (error) {
      case BodyMeasurementEntryError.invalidValue:
        return l10n.bodyEntryInvalidValue;
      case BodyMeasurementEntryError.missingWritePermission:
        return l10n.bodyEntryPermissionNeeded(title);
      case BodyMeasurementEntryError.writeFailed:
        return l10n.bodyEntryWriteFailed(screenErrorText(writeError, l10n));
    }
  }
}

/// The localized metric name for [type], shared by the app bar, the field label
/// and the error copy.
String bodyMeasurementTitle(BodyMeasurementType type, AppLocalizations l10n) =>
    switch (type) {
      BodyMeasurementType.weight => l10n.metricWeight,
      BodyMeasurementType.height => l10n.metricHeight,
      BodyMeasurementType.bodyFat => l10n.metricBodyFat,
    };

/// Port of the Kotlin `BodyMeasurementType.inputUnitLabel`. The unit table and
/// its conversions live in [MeasurementInput], not here.
String _bodyUnitLabel(BodyMeasurementType type, UnitFormatter formatter) =>
    switch (type) {
      BodyMeasurementType.weight => formatter.weightInputUnit,
      BodyMeasurementType.height => formatter.heightInputUnit,
      BodyMeasurementType.bodyFat => '%',
    };

/// The typed value in its stored (metric) unit. Port of the Kotlin
/// `canonicalBodyMeasurementValue`.
double? _canonicalValue(
  String input,
  BodyMeasurementType type,
  UnitFormatter formatter,
) =>
    switch (type) {
      BodyMeasurementType.weight => formatter.weightInputToKilograms(input),
      BodyMeasurementType.height => formatter.heightInputToCentimeters(input),
      BodyMeasurementType.bodyFat => parseDecimalInput(input),
    };

IconData _bodyIcon(BodyMeasurementType type) => switch (type) {
      BodyMeasurementType.height => Icons.straighten_outlined,
      _ => Icons.monitor_weight_outlined,
    };

Color _bodyAccent(BodyMeasurementType type) => switch (type) {
      BodyMeasurementType.bodyFat => AppColors.bodyFat,
      _ => AppColors.weight,
    };
