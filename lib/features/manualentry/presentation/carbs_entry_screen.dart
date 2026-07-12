import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/presentation/command_state.dart';
import '../../../core/presentation/measurement_input.dart';
import '../../../di/providers.dart';
import '../../../l10n/app_localizations.dart';
import '../../../state/app_providers.dart';
import '../../../ui/components/health_connect_gate.dart';
import '../../../ui/components/ov_card.dart';
import '../../../ui/theme/app_colors.dart';
import '../application/carbs_entry_view_model.dart';
import 'manual_entry_form_scaffold.dart';

/// Carbs manual-entry screen pushed over the shell (`/manual_entry/carbs`).
///
/// Riverpod/Flutter port of the Kotlin `CarbsEntryScreen`: a single value field
/// writing a `NutritionWriteRequest` via `NutritionRepository.writeCarbsEntry`.
class CarbsEntryScreen extends ConsumerStatefulWidget {
  const CarbsEntryScreen({super.key});

  @override
  ConsumerState<CarbsEntryScreen> createState() => _CarbsEntryScreenState();
}

class _CarbsEntryScreenState extends ConsumerState<CarbsEntryScreen>
    with RefreshPermissionOnResume {
  late final NotifierProvider<CarbsEntryViewModel, CarbsEntryState> _provider =
      NotifierProvider.autoDispose<CarbsEntryViewModel, CarbsEntryState>(
    CarbsEntryViewModel.new,
  );

  final TextEditingController _controller = TextEditingController();

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
    ref.listen(_provider.select((s) => s.save), (previous, next) {
      // The success is consumed exactly once, then the command is put back to
      // rest — otherwise returning to this route would replay the toast.
      if (next is CommandSuccess<void>) {
        ref.read(_provider.notifier).onSaveCompletedHandled();
        // Kotlin just navigates back; the confirmation snackbar is a Flutter
        // addition and has no ARB key (the catalogs are generated from Kotlin).
        onManualEntrySaved(context, 'Carbs entry saved');
      }
    });

    final writePermissions =
        ref.watch(nutritionRepositoryProvider).nutritionWritePermissions;

    return Scaffold(
      appBar: AppBar(title: Text(l10n.screenCarbsEntry)),
      body: HealthConnectGate(
        requiredPermissions: writePermissions,
        child: _CarbsEntryForm(provider: _provider, controller: _controller),
      ),
    );
  }
}

class _CarbsEntryForm extends ConsumerWidget {
  const _CarbsEntryForm({required this.provider, required this.controller});

  final NotifierProvider<CarbsEntryViewModel, CarbsEntryState> provider;
  final TextEditingController controller;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    final state = ref.watch(provider);
    final notifier = ref.read(provider.notifier);
    final formatter = ref.watch(unitFormatterProvider);
    final unitLabel = formatter.carbsInputUnit;
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
                      const Icon(Icons.restaurant_outlined,
                          color: AppColors.nutrition, size: 22),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(l10n.metricCarbs,
                                style: theme.textTheme.titleSmall),
                            Text(
                              state.canWrite
                                  ? l10n.carbsEntrySubtitle
                                  : l10n.carbsEntryPermissionNeeded,
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
                      labelText: l10n.carbsEntryValueLabel(unitLabel),
                    ),
                  ),
                  const SizedBox(height: 12),
                  FilledButton.icon(
                    onPressed: enabled
                        ? () => notifier
                            .addEntry(formatter.carbsInputToGrams(state.inputText))
                        : null,
                    icon: const Icon(Icons.add, size: 18),
                    label: Text(l10n.carbsEntryAdd),
                  ),
                  if (_errorText(state, l10n) case final message?)
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

  /// The one message the form shows: why it refuses to save, or — failing that
  /// — why the last attempt to save did not land.
  String? _errorText(CarbsEntryState state, AppLocalizations l10n) {
    final entryError = state.entryError;
    if (entryError != null) {
      return switch (entryError) {
        CarbsEntryError.invalidValue => l10n.carbsEntryInvalidValue,
        CarbsEntryError.missingWritePermission =>
          l10n.carbsEntryPermissionNeeded,
      };
    }
    final blocking = state.blockingError;
    if (blocking == null) return null;
    return l10n.carbsEntryWriteFailed(screenErrorText(blocking, l10n));
  }
}
