import 'package:intl/intl.dart';

import '../../l10n/app_localizations.dart';
import '../time/local_date.dart';
import 'time_range.dart';

final DateFormat _dateFormatter = DateFormat('EEE d MMM');
final DateFormat _monthFormatter = DateFormat('LLLL yyyy');
final DateFormat _yearFormatter = DateFormat('yyyy');

DateTime _asDateTime(LocalDate date) => DateTime(date.year, date.month, date.day);

/// The human-readable title for a period. Port of the Kotlin `periodTitle` /
/// `localizedPeriodTitle` pair — the Flutter side keeps a single localized
/// implementation that takes [l10n] instead of a Composable context.
///
/// With a rolling [weekPeriodMode], week/month/year are rolling 7/30/365-day
/// windows, so a period that ends today reads "Last 7/30/365 days" instead of
/// "This week/month/year".
String periodTitle(
  AppLocalizations l10n,
  TimeRange range,
  DatePeriod period, {
  LocalDate? today,
  WeekPeriodMode weekPeriodMode = WeekPeriodMode.mondayToSunday,
}) {
  final resolvedToday = today ?? LocalDate.now();
  final rolling = weekPeriodMode.usesRollingDates && period.end == resolvedToday;
  switch (range) {
    case TimeRange.day:
      if (period.start == resolvedToday) return l10n.periodToday;
      if (period.start == resolvedToday.minusDays(1)) {
        return l10n.periodYesterday;
      }
      return _dateFormatter.format(_asDateTime(period.start));
    case TimeRange.week:
      if (rolling) return l10n.periodLast7Days;
      if (resolvedToday.isBetween(period.start, period.end)) {
        return l10n.periodThisWeek;
      }
      return l10n.periodWeekOf(_dateFormatter.format(_asDateTime(period.start)));
    case TimeRange.month:
      if (rolling) return l10n.periodLast30Days;
      if (period.end == resolvedToday) return l10n.periodThisMonth;
      return _monthFormatter.format(_asDateTime(period.start));
    case TimeRange.year:
      if (rolling) return l10n.periodLast365Days;
      if (period.end == resolvedToday) return l10n.periodThisYear;
      return _yearFormatter.format(_asDateTime(period.start));
  }
}
