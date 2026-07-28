/// The finished-import view: the tally, why rows were rejected, and what to do
/// next.
library;

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/presentation/command_state.dart';
import '../../../../core/presentation/report_saving.dart';
import '../../../../l10n/app_localizations.dart';
import '../../../../ui/components/ov_card.dart';
import '../application/csv_import_view_model.dart';
import '../csv_import_models.dart';
import 'csv_import_labels.dart';
import 'csv_import_step_bar.dart';

class CsvImportResultView extends ConsumerWidget {
  const CsvImportResultView({super.key, this.saveReportFile});

  /// Test seam for the save picker, matching `AppleHealthImportCard`.
  final TextReportSaver? saveReportFile;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    final state = ref.watch(csvImportProvider);
    final notifier = ref.read(csvImportProvider.notifier);
    final result = state.result;
    if (result == null) return const SizedBox.shrink();

    _consumeSaveOutcome(context, ref, l10n, state.saveReport);

    return Column(
      children: [
        Expanded(
          child: ListView(
            padding: const EdgeInsets.all(16),
            children: [
              OpenVitalsCard(
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        l10n.settingsCsvImportResult(
                          result.progress.written,
                          result.progress.alreadyPresent,
                          result.progress.rejected,
                        ),
                        style: theme.textTheme.titleMedium,
                      ),
                      if (result.wroteNothing) ...[
                        const SizedBox(height: 8),
                        Text(
                          l10n.settingsCsvImportResultEmpty,
                          style: theme.textTheme.bodySmall?.copyWith(
                            color: theme.colorScheme.onSurfaceVariant,
                          ),
                        ),
                      ],
                      if (result.outcome == CsvImportOutcome.cancelled) ...[
                        const SizedBox(height: 8),
                        Text(
                          l10n.settingsCsvImportResultCancelled,
                          style: theme.textTheme.bodySmall,
                        ),
                      ],
                      if (result.outcome == CsvImportOutcome.rateLimited) ...[
                        const SizedBox(height: 8),
                        Text(
                          l10n.settingsCsvImportResultRateLimited,
                          style: theme.textTheme.bodySmall
                              ?.copyWith(color: theme.colorScheme.error),
                        ),
                      ],
                      if (result.outcome == CsvImportOutcome.failed &&
                          result.error != null) ...[
                        const SizedBox(height: 8),
                        Text(
                          l10n.settingsCsvImportError(result.error!),
                          style: theme.textTheme.bodySmall
                              ?.copyWith(color: theme.colorScheme.error),
                        ),
                      ],
                    ],
                  ),
                ),
              ),
              if (result.diagnosticCounts.isNotEmpty) ...[
                const SizedBox(height: 12),
                OpenVitalsCard(
                  child: Padding(
                    padding: const EdgeInsets.all(16),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          l10n.settingsCsvImportDiagnosticsTitle,
                          style: theme.textTheme.titleSmall,
                        ),
                        const SizedBox(height: 8),
                        for (final entry in result.diagnosticCounts.entries)
                          Text(
                            l10n.settingsCsvImportDiagnosticLine(
                              csvDiagnosticReasonLabel(l10n, entry.key),
                              entry.value,
                            ),
                            style: theme.textTheme.bodySmall,
                          ),
                      ],
                    ),
                  ),
                ),
              ],
              const SizedBox(height: 12),
              SizedBox(
                width: double.infinity,
                child: OutlinedButton.icon(
                  onPressed: state.saveReport is CommandRunning<bool>
                      ? null
                      : () => notifier.saveReport(saver: saveReportFile),
                  icon: const Icon(Icons.save_alt_outlined, size: 18),
                  label: Text(l10n.settingsCsvImportSaveReport),
                ),
              ),
            ],
          ),
        ),
        CsvImportStepBar(
          onBack: notifier.reset,
          backLabel: l10n.settingsCsvImportImportAnother,
          onNext: () => Navigator.of(context).maybePop(),
          nextLabel: l10n.settingsCsvImportDone,
        ),
      ],
    );
  }

  /// Shows the outcome once and hands the command back to idle, so rebuilding
  /// this step cannot replay the snackbar.
  void _consumeSaveOutcome(
    BuildContext context,
    WidgetRef ref,
    AppLocalizations l10n,
    CommandState<bool> command,
  ) {
    final message = switch (command) {
      CommandSuccess<bool>(:final value) => value
          ? l10n.settingsCsvImportSaveReportSaved
          : l10n.settingsCsvImportSaveReportCancelled,
      CommandFailure<bool>() => l10n.settingsCsvImportSaveReportFailed,
      _ => null,
    };
    if (message == null) return;

    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!context.mounted) return;
      ScaffoldMessenger.maybeOf(context)
          ?.showSnackBar(SnackBar(content: Text(message)));
      ref.read(csvImportProvider.notifier).clearSaveReport();
    });
  }
}

