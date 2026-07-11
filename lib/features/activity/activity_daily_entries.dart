import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../core/presentation/display_value.dart';
import '../../core/time/local_date.dart';
import '../../l10n/app_localizations.dart';
import '../../ui/components/ov_card.dart';
import '../../ui/components/paginated_entry_list.dart';

/// Port of the Kotlin `ActivityDailyEntriesContent` / `ActivityDailyEntryRow`:
/// one card per tracked day, newest first, paginated.

/// A day's value, already formatted.
@immutable
class ActivityDailyEntry {
  const ActivityDailyEntry({required this.date, required this.value});

  final LocalDate date;
  final DisplayValue value;
}

/// Kotlin `entryListTitle`: the section is titled by the pinned day when one is
/// selected, otherwise generically.
String activityEntryListTitle(
  LocalDate? titleDate,
  String locale,
  AppLocalizations l10n,
) {
  if (titleDate == null) return l10n.sectionEntries;
  return DateFormat.yMMMd(locale)
      .format(DateTime(titleDate.year, titleDate.month, titleDate.day));
}

class ActivityDailyEntriesContent extends StatelessWidget {
  const ActivityDailyEntriesContent({
    super.key,
    required this.entries,
    required this.accentColor,
    this.titleDate,
  });

  final List<ActivityDailyEntry> entries;
  final Color accentColor;
  final LocalDate? titleDate;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final locale = Localizations.localeOf(context).toLanguageTag();
    final sorted = [...entries]..sort((a, b) => b.date.compareTo(a.date));

    return PaginatedEntryList<ActivityDailyEntry>(
      title: activityEntryListTitle(titleDate, locale, l10n),
      entries: sorted,
      rowBuilder: (context, entry) => ActivityDailyEntryRow(
        entry: entry,
        accentColor: accentColor,
      ),
    );
  }
}

class ActivityDailyEntryRow extends StatelessWidget {
  const ActivityDailyEntryRow({
    super.key,
    required this.entry,
    required this.accentColor,
  });

  final ActivityDailyEntry entry;
  final Color accentColor;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final locale = Localizations.localeOf(context).toLanguageTag();
    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        child: Row(
          children: [
            Expanded(
              child: Text(
                DateFormat.yMMMd(locale).format(
                  DateTime(entry.date.year, entry.date.month, entry.date.day),
                ),
                style: theme.textTheme.bodyMedium,
              ),
            ),
            Text(
              entry.value.text,
              style: theme.textTheme.titleMedium?.copyWith(color: accentColor),
            ),
          ],
        ),
      ),
    );
  }
}
