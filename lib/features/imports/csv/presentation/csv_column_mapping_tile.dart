/// One CSV column's role and value-interpretation editor.
library;

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../l10n/app_localizations.dart';
import '../../../../ui/components/ov_card.dart';
import '../application/csv_import_view_model.dart';
import '../csv_column_mapping.dart';
import '../csv_import_metric.dart';
import 'csv_import_labels.dart';

class CsvColumnMappingTile extends ConsumerWidget {
  const CsvColumnMappingTile({
    super.key,
    required this.header,
    required this.samples,
    required this.mapping,
  });

  final String header;
  final List<String> samples;
  final CsvColumnMapping mapping;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    final notifier = ref.read(csvImportProvider.notifier);
    final spec =
        mapping.metric == null ? null : kCsvMetricCatalog[mapping.metric!];

    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: OpenVitalsCard(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(header, style: theme.textTheme.titleSmall),
              if (samples.isNotEmpty)
                Text(
                  samples.join(' · '),
                  style: theme.textTheme.bodySmall
                      ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                ),
              const SizedBox(height: 8),
              DropdownButtonFormField<String>(
                isExpanded: true,
                initialValue: _roleValue(mapping),
                items: [
                  DropdownMenuItem(
                    value: 'ignore',
                    child: Text(l10n.settingsCsvImportRoleIgnore),
                  ),
                  DropdownMenuItem(
                    value: 'timestamp',
                    child: Text(l10n.settingsCsvImportRoleTimestamp),
                  ),
                  for (final metric in CsvImportMetric.values)
                    DropdownMenuItem(
                      value: metric.name,
                      child: Text(csvMetricLabel(l10n, metric)),
                    ),
                ],
                onChanged: (value) {
                  if (value == null) return;
                  if (value == 'ignore') {
                    notifier.setColumnRole(
                      mapping.columnIndex,
                      role: CsvColumnRole.ignore,
                    );
                  } else if (value == 'timestamp') {
                    notifier.setColumnRole(
                      mapping.columnIndex,
                      role: CsvColumnRole.timestamp,
                    );
                  } else {
                    notifier.setColumnRole(
                      mapping.columnIndex,
                      role: CsvColumnRole.metric,
                      metric: CsvImportMetric.values
                          .firstWhere((it) => it.name == value),
                    );
                  }
                },
              ),
              if (spec != null) ...[
                const SizedBox(height: 8),
                DropdownButtonFormField<int>(
                  isExpanded: true,
                  initialValue: spec.interpretations
                      .indexOf(mapping.effectiveInterpretation!)
                      .clamp(0, spec.interpretations.length - 1),
                  decoration: InputDecoration(
                    labelText: l10n.settingsCsvImportValueLabel,
                  ),
                  items: [
                    for (var i = 0; i < spec.interpretations.length; i++)
                      DropdownMenuItem(
                        value: i,
                        child: Text(
                          csvInterpretationLabel(l10n, spec.interpretations[i]),
                        ),
                      ),
                  ],
                  onChanged: (value) {
                    if (value == null) return;
                    notifier.setColumnInterpretation(
                      mapping.columnIndex,
                      spec.interpretations[value],
                    );
                  },
                ),
                if (mapping.effectiveInterpretation?.needsRowWeight ?? false)
                  Padding(
                    padding: const EdgeInsets.only(top: 6),
                    child: Text(
                      l10n.settingsCsvImportMassShareHint,
                      style: theme.textTheme.bodySmall
                          ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                    ),
                  ),
              ],
            ],
          ),
        ),
      ),
    );
  }

  static String _roleValue(CsvColumnMapping mapping) => switch (mapping.role) {
        CsvColumnRole.timestamp => 'timestamp',
        CsvColumnRole.metric => mapping.metric?.name ?? 'ignore',
        CsvColumnRole.ignore => 'ignore',
      };
}

