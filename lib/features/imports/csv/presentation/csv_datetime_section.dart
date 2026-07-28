/// The date/time format, time-zone and live-echo controls for the mapping step.
library;

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../../../../l10n/app_localizations.dart';
import '../../../../ui/components/ov_card.dart';
import '../application/csv_import_view_model.dart';
import '../csv_column_mapping.dart';
import '../csv_datetime_format.dart';
import '../csv_table_reader.dart';
import 'csv_import_labels.dart';

class CsvDateTimeSection extends ConsumerWidget {
  const CsvDateTimeSection({
    super.key,
    required this.sample,
    required this.mapping,
  });

  final CsvSample sample;
  final CsvImportMapping mapping;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    final notifier = ref.read(csvImportProvider.notifier);
    final settings = mapping.dateTime;
    final timestampColumn = mapping.timestampColumn;

    final firstValue = timestampColumn == null
        ? null
        : sample.columnValues(timestampColumn.columnIndex).firstOrNull;
    final fromFile =
        firstValue != null && csvTimestampHasExplicitOffset(firstValue);
    final resolved =
        firstValue == null ? null : resolveCsvInstant(firstValue, settings);

    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              l10n.settingsCsvImportDateTimeTitle,
              style: theme.textTheme.titleSmall,
            ),
            const SizedBox(height: 8),
            DropdownButtonFormField<CsvDateTimeFormat>(
              isExpanded: true,
              initialValue: settings.format,
              decoration: InputDecoration(
                labelText: l10n.settingsCsvImportDateFormatLabel,
              ),
              items: [
                for (final format in CsvDateTimeFormat.values)
                  DropdownMenuItem(
                    value: format,
                    child: Text(csvDateFormatLabel(l10n, format)),
                  ),
              ],
              onChanged: (value) {
                if (value == null) return;
                notifier.setDateTimeSettings(settings.copyWith(format: value));
              },
            ),
            if (settings.format == CsvDateTimeFormat.custom) ...[
              const SizedBox(height: 8),
              TextFormField(
                initialValue: settings.customPattern,
                decoration: InputDecoration(
                  labelText: l10n.settingsCsvImportDateFormatCustom,
                  hintText: l10n.settingsCsvImportDateFormatCustomHint,
                ),
                onChanged: (value) => notifier
                    .setDateTimeSettings(settings.copyWith(customPattern: value)),
              ),
            ],
            const SizedBox(height: 8),
            if (fromFile)
              Text(
                l10n.settingsCsvImportTimeZoneFromFile,
                style: theme.textTheme.bodySmall
                    ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
              )
            else
              DropdownButtonFormField<CsvTimeZoneMode>(
                isExpanded: true,
                initialValue: settings.zone,
                decoration: InputDecoration(
                  labelText: l10n.settingsCsvImportTimeZoneLabel,
                ),
                items: [
                  for (final mode in CsvTimeZoneMode.values)
                    DropdownMenuItem(
                      value: mode,
                      child: Text(csvTimeZoneLabel(l10n, mode)),
                    ),
                ],
                onChanged: (value) {
                  if (value == null) return;
                  notifier.setDateTimeSettings(settings.copyWith(zone: value));
                },
              ),
            if (!fromFile && settings.zone == CsvTimeZoneMode.fixedOffset) ...[
              const SizedBox(height: 8),
              TextFormField(
                decoration: InputDecoration(
                  labelText: l10n.settingsCsvImportTimeZoneOffsetLabel,
                ),
                onChanged: (value) {
                  final offset = _parseOffset(value);
                  if (offset == null) return;
                  notifier.setDateTimeSettings(
                    settings.copyWith(fixedOffset: offset),
                  );
                },
              ),
            ],
            // The live echo. A dd/MM vs MM/dd mistake is invisible in the raw
            // text and obvious here, BEFORE anything is written.
            //
            // Locale passed explicitly, the house convention — see the date
            // range on the confirm step for why the bare form is not wrong, just
            // dependent on a global an ancestor happens to set.
            if (resolved != null) ...[
              const SizedBox(height: 12),
              Text(
                l10n.settingsCsvImportExampleRow(
                  DateFormat.yMMMEd(
                    Localizations.localeOf(context).toLanguageTag(),
                  ).add_Hm().format(resolved.utc.add(resolved.offset)),
                ),
                style: theme.textTheme.bodyMedium
                    ?.copyWith(color: theme.colorScheme.primary),
              ),
            ],
          ],
        ),
      ),
    );
  }
}

Duration? _parseOffset(String value) {
  final match = RegExp(r'^([+-]?)(\d{1,2}):?(\d{2})$').firstMatch(value.trim());
  if (match == null) return null;
  final sign = match.group(1) == '-' ? -1 : 1;
  return Duration(
    hours: sign * int.parse(match.group(2)!),
    minutes: sign * int.parse(match.group(3)!),
  );
}
