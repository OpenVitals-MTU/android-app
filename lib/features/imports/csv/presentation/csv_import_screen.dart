import 'package:cross_file/cross_file.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../../../../l10n/app_localizations.dart';
import '../../../../ui/components/health_connect_gate.dart';
import '../../../../ui/components/ov_card.dart';
import '../../../../ui/components/permission_callout.dart';
import '../application/csv_import_view_model.dart';
import '../csv_column_mapping.dart';
import '../csv_import_models.dart';
import '../csv_row_converter.dart';
import '../csv_table_reader.dart';
import 'csv_column_mapping_tile.dart';
import 'csv_datetime_section.dart';
import 'csv_import_labels.dart';
import 'csv_import_result_view.dart';
import 'csv_import_step_bar.dart';
import 'csv_preview_table.dart';

/// The CSV importer: pick → map columns → confirm → import → result.
///
/// A pushed route rather than a Settings card, because a column-mapping editor
/// cannot live in the flat card list `SettingsSectionScreen` builds without that
/// screen hosting another screen's state. `DeviceSyncScreen` is the same shape.
///
/// The gate requires NO permissions: which ones are needed is not known until the
/// mapping is done, and swapping the whole screen out mid-mapping would be
/// hostile. The mapped-metric permissions are asked for at the confirm step.
class CsvImportScreen extends ConsumerWidget {
  const CsvImportScreen({super.key, this.pickCsvFile});

  /// Test seam so a widget test never opens a real file picker.
  final Future<XFile?> Function()? pickCsvFile;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = AppLocalizations.of(context);
    final state = ref.watch(csvImportProvider);

    return Scaffold(
      appBar: AppBar(title: Text(l10n.settingsCsvImportScreenTitle)),
      body: HealthConnectGate(
        child: SafeArea(
          child: switch (state.step) {
            CsvImportStep.pick => _PickStep(pickCsvFile: pickCsvFile),
            CsvImportStep.mapping => const _MappingStep(),
            CsvImportStep.confirm => const _ConfirmStep(),
            CsvImportStep.importing => const _ImportingStep(),
            CsvImportStep.done => const CsvImportResultView(),
          },
        ),
      ),
    );
  }
}

class _PickStep extends ConsumerWidget {
  const _PickStep({this.pickCsvFile});

  final Future<XFile?> Function()? pickCsvFile;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    final state = ref.watch(csvImportProvider);

    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        OpenVitalsCard(
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  l10n.settingsCsvImportPickTitle,
                  style: theme.textTheme.titleMedium,
                ),
                const SizedBox(height: 8),
                Text(
                  l10n.settingsCsvImportPickBody,
                  style: theme.textTheme.bodyMedium
                      ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                ),
                if (state.error case final message?) ...[
                  const SizedBox(height: 12),
                  Text(
                    l10n.settingsCsvImportUnreadableFile(message),
                    style: theme.textTheme.bodySmall
                        ?.copyWith(color: theme.colorScheme.error),
                  ),
                ],
                if (state.isLoadingFile) ...[
                  const SizedBox(height: 12),
                  const LinearProgressIndicator(),
                  const SizedBox(height: 8),
                  Text(
                    l10n.settingsCsvImportLoading,
                    style: theme.textTheme.bodySmall,
                  ),
                ],
                const SizedBox(height: 16),
                SizedBox(
                  width: double.infinity,
                  child: FilledButton.icon(
                    onPressed: state.isLoadingFile
                        ? null
                        : () => ref
                            .read(csvImportProvider.notifier)
                            .pickFile(picker: pickCsvFile),
                    icon: const Icon(Icons.description_outlined, size: 18),
                    label: Text(l10n.settingsCsvImportPickAction),
                  ),
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }
}

class _MappingStep extends ConsumerWidget {
  const _MappingStep();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    final state = ref.watch(csvImportProvider);
    final notifier = ref.read(csvImportProvider.notifier);
    final sample = state.sample;

    if (sample == null || sample.isEmpty) {
      return _EmptyFileBody(onBack: notifier.reset);
    }

    final mapping = state.mapping;
    if (mapping == null) return _EmptyFileBody(onBack: notifier.reset);

    return Column(
      children: [
        Expanded(
          child: ListView(
            padding: const EdgeInsets.all(16),
            children: [
              Text(
                l10n.settingsCsvImportFileLabel(
                  state.fileName ?? '',
                  sample.columnCount,
                ),
                style: theme.textTheme.bodySmall
                    ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
              ),
              const SizedBox(height: 12),
              CsvDialectCard(sample: sample),
              const SizedBox(height: 12),
              CsvPreviewTable(sample: sample),
              const SizedBox(height: 12),
              CsvDateTimeSection(sample: sample, mapping: mapping),
              const SizedBox(height: 12),
              Text(
                l10n.settingsCsvImportColumnsTitle,
                style: theme.textTheme.titleSmall,
              ),
              const SizedBox(height: 8),
              for (var index = 0; index < sample.columnCount; index++)
                CsvColumnMappingTile(
                  header: sample.headerRow[index],
                  samples: sample.columnValues(index).take(3).toList(),
                  mapping: mapping.columns.firstWhere(
                    (it) => it.columnIndex == index,
                    orElse: () => CsvColumnMapping(columnIndex: index),
                  ),
                ),
              if (state.issues.isNotEmpty) ...[
                const SizedBox(height: 12),
                for (final issue in state.issues)
                  Padding(
                    padding: const EdgeInsets.only(bottom: 4),
                    child: Text(
                      csvIssueLabel(l10n, issue),
                      style: theme.textTheme.bodySmall
                          ?.copyWith(color: theme.colorScheme.error),
                    ),
                  ),
              ],
            ],
          ),
        ),
        CsvImportStepBar(
          onBack: notifier.reset,
          onNext: state.canContinue
              ? () => notifier.goToStep(CsvImportStep.confirm)
              : null,
          nextLabel: l10n.settingsCsvImportContinue,
        ),
      ],
    );
  }
}

class _EmptyFileBody extends StatelessWidget {
  const _EmptyFileBody({required this.onBack});

  final VoidCallback onBack;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(
              l10n.settingsCsvImportEmptyFile,
              textAlign: TextAlign.center,
              style: Theme.of(context).textTheme.bodyMedium,
            ),
            const SizedBox(height: 16),
            OutlinedButton(
              onPressed: onBack,
              child: Text(l10n.settingsCsvImportBack),
            ),
          ],
        ),
      ),
    );
  }
}

class _ConfirmStep extends ConsumerWidget {
  const _ConfirmStep();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    final state = ref.watch(csvImportProvider);
    final notifier = ref.read(csvImportProvider.notifier);
    final mapping = state.mapping;
    final sample = state.sample;
    if (mapping == null || sample == null) return const SizedBox.shrink();

    final missing = state.missingPermissions;

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
                        l10n.settingsCsvImportConfirmTitle,
                        style: theme.textTheme.titleMedium,
                      ),
                      const SizedBox(height: 8),
                      Text(
                        l10n.settingsCsvImportConfirmSummary(
                          state.fileName ?? '',
                          mapping.metricColumns.length,
                        ),
                        style: theme.textTheme.bodyMedium,
                      ),
                      const SizedBox(height: 12),
                      // The date span is the last guard against a day/month
                      // mix-up. The live echo on the previous step only shows
                      // row 1, and `01/07` reads plausibly either way; a span
                      // running to the wrong month — or backwards — does not.
                      _DateRangeLine(sample: sample, mapping: mapping),
                      // The per-metric range is the guard against a bad
                      // derivation: fat mass divided by the wrong weight column
                      // shows up here as 3% or 150%.
                      for (final column in mapping.metricColumns)
                        _MetricRangeLine(
                          column: column,
                          sample: sample,
                          mapping: mapping,
                        ),
                    ],
                  ),
                ),
              ),
              if (missing.isNotEmpty) ...[
                const SizedBox(height: 12),
                PermissionCallout(
                  title: l10n.settingsCsvImportPermissionTitle,
                  body: l10n.settingsCsvImportPermissionBody,
                  onGrant: notifier.grantPermissions,
                ),
              ],
            ],
          ),
        ),
        CsvImportStepBar(
          onBack: () => notifier.goToStep(CsvImportStep.mapping),
          onNext: state.isGranting ? null : notifier.startImport,
          nextLabel: l10n.settingsCsvImportStart,
        ),
      ],
    );
  }
}

/// The first and last dates the sampled rows resolve to.
///
/// Rendered with the app's own long date format rather than echoing the file's
/// text, so a `01/07/2026` read as January shows the word "January" — the point
/// is to make the interpretation legible, not to repeat the input.
class _DateRangeLine extends StatelessWidget {
  const _DateRangeLine({required this.sample, required this.mapping});

  final CsvSample sample;
  final CsvImportMapping mapping;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);

    final range = previewInstantRange(
      rows: sample.dataRows,
      mapping: mapping,
    );
    if (range == null) return const SizedBox.shrink();

    // Locale passed explicitly, as the other 17 `DateFormat.yMMMd(locale)` call
    // sites do. The bare form would also localize correctly today — `app.dart`
    // assigns `Intl.defaultLocale` in `MaterialApp.builder` — but that couples
    // this widget to a global set by an ancestor, and a widget test pumping this
    // screen under a plain MaterialApp has no such ancestor.
    final format =
        DateFormat.yMMMMd(Localizations.localeOf(context).toLanguageTag());
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Text(
        l10n.settingsCsvImportConfirmDates(
          format.format(range.first),
          format.format(range.last),
        ),
        style: theme.textTheme.bodyMedium
            ?.copyWith(color: theme.colorScheme.primary),
      ),
    );
  }
}

/// One metric's observed range across the sample, in its canonical unit.
class _MetricRangeLine extends StatelessWidget {
  const _MetricRangeLine({
    required this.column,
    required this.sample,
    required this.mapping,
  });

  final CsvColumnMapping column;
  final CsvSample sample;
  final CsvImportMapping mapping;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final theme = Theme.of(context);
    final metric = column.metric;
    if (metric == null) return const SizedBox.shrink();

    final values = previewCanonicalValues(
      rows: sample.dataRows,
      mapping: mapping,
      metric: metric,
    );
    if (values.isEmpty) return const SizedBox.shrink();
    values.sort();

    return Padding(
      padding: const EdgeInsets.only(bottom: 4),
      child: Text(
        l10n.settingsCsvImportConfirmRange(
          csvMetricLabel(l10n, metric),
          values.first.toStringAsFixed(1),
          values.last.toStringAsFixed(1),
        ),
        style: theme.textTheme.bodySmall,
      ),
    );
  }
}

class _ImportingStep extends ConsumerWidget {
  const _ImportingStep();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    final state = ref.watch(csvImportProvider);
    final progress = state.progress ?? const CsvImportProgress();

    return Padding(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          LinearProgressIndicator(value: progress.fraction),
          const SizedBox(height: 12),
          Text(
            l10n.settingsCsvImportProgress(
              progress.rowsRead,
              progress.written,
              progress.alreadyPresent,
              progress.rejected,
            ),
            style: theme.textTheme.bodyMedium,
          ),
          const Spacer(),
          OutlinedButton(
            onPressed: ref.read(csvImportProvider.notifier).cancelImport,
            child: Text(l10n.settingsCsvImportCancel),
          ),
        ],
      ),
    );
  }
}

