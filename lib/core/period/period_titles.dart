import 'package:intl/intl.dart';

import '../../l10n/app_localizations.dart';
import '../time/local_date.dart';
import 'time_range.dart';

// Getters, not cached finals: constructed per use so they follow the current
// Intl.defaultLocale (the app language) instead of freezing at first access.
DateFormat get _dateFormatter => DateFormat('EEE d MMM');
DateFormat get _monthFormatter => DateFormat('LLLL yyyy');
final DateFormat _yearFormatter = DateFormat('yyyy');
final DateFormat _spanFormatter = DateFormat('d MMM');
final DateFormat _spanFormatterWithYear = DateFormat('d MMM yyyy');

DateTime _asDateTime(LocalDate date) => DateTime(date.year, date.month, date.day);

/// A dated span for a *past* rolling window ("22 May – 20 Jun 2026"), so a
/// rolling last-N-days period that no longer ends today reads as the span it
/// actually is rather than borrowing the single calendar month/year its start
/// falls in (which named a mostly-June window "May 2026"). The year rides on the
/// end date, and on the start too when the window straddles a year boundary.
String _rollingSpanTitle(DatePeriod period) {
  final crossesYears = period.start.year != period.end.year;
  final start = (crossesYears ? _spanFormatterWithYear : _spanFormatter)
      .format(_asDateTime(period.start));
  final end = _spanFormatterWithYear.format(_asDateTime(period.end));
  return '$start – $end';
}

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
  final rollingMode = weekPeriodMode.usesRollingDates;
  final rolling = rollingMode && period.end == resolvedToday;
  switch (range) {
    case TimeRange.day:
      if (period.start == resolvedToday) return l10n.periodToday;
      if (period.start == resolvedToday.minusDays(1)) {
        return l10n.periodYesterday;
      }
      return _dateFormatter.format(_asDateTime(period.start));
    case TimeRange.week:
      if (rolling) return l10n.periodLast7Days;
      // A past rolling window is a dated span, not the calendar week of its start.
      if (rollingMode) return _rollingSpanTitle(period);
      if (resolvedToday.isBetween(period.start, period.end)) {
        return l10n.periodThisWeek;
      }
      return l10n.periodWeekOf(_dateFormatter.format(_asDateTime(period.start)));
    case TimeRange.month:
      if (rolling) return l10n.periodLast30Days;
      if (rollingMode) return _rollingSpanTitle(period);
      if (period.end == resolvedToday) return l10n.periodThisMonth;
      return _monthFormatter.format(_asDateTime(period.start));
    case TimeRange.year:
      if (rolling) return l10n.periodLast365Days;
      if (rollingMode) return _rollingSpanTitle(period);
      if (period.end == resolvedToday) return l10n.periodThisYear;
      return _yearFormatter.format(_asDateTime(period.start));
  }
}
