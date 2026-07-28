/// The separator/header controls and the sample preview table for the mapping
/// step.
library;

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../l10n/app_localizations.dart';
import '../../../../ui/components/ov_card.dart';
import '../application/csv_import_view_model.dart';
import '../csv_table_reader.dart';
import 'csv_import_labels.dart';

class CsvDialectCard extends ConsumerWidget {
  const CsvDialectCard({super.key, required this.sample});

  final CsvSample sample;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = AppLocalizations.of(context);
    final notifier = ref.read(csvImportProvider.notifier);

    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            DropdownButtonFormField<String>(
              isExpanded: true,
              initialValue: sample.dialect.fieldDelimiter,
              decoration: InputDecoration(
                labelText: l10n.settingsCsvImportSeparatorLabel,
              ),
              items: [
                for (final delimiter in kCsvFieldDelimiters)
                  DropdownMenuItem(
                    value: delimiter,
                    child: Text(csvSeparatorLabel(l10n, delimiter)),
                  ),
              ],
              onChanged: (value) {
                if (value == null) return;
                notifier.setDialect(
                  sample.dialect.copyWith(fieldDelimiter: value),
                );
              },
            ),
            SwitchListTile(
              contentPadding: EdgeInsets.zero,
              title: Text(l10n.settingsCsvImportHasHeaderLabel),
              value: sample.hasHeaderRow,
              onChanged: (value) => notifier.setDialect(
                sample.dialect,
                hasHeaderRow: value,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class CsvPreviewTable extends StatelessWidget {
  const CsvPreviewTable({super.key, required this.sample});

  final CsvSample sample;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    final rows = sample.dataRows.take(5).toList();

    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              l10n.settingsCsvImportPreviewTitle,
              style: theme.textTheme.titleSmall,
            ),
            const SizedBox(height: 8),
            // The table scrolls inside itself; the page must never scroll
            // sideways.
            SingleChildScrollView(
              scrollDirection: Axis.horizontal,
              child: DataTable(
                columns: [
                  for (final header in sample.headerRow)
                    DataColumn(label: Text(header)),
                ],
                rows: [
                  for (final row in rows)
                    DataRow(
                      cells: [
                        for (var i = 0; i < sample.columnCount; i++)
                          DataCell(Text(i < row.length ? row[i] : '')),
                      ],
                    ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

