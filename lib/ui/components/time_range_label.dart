import '../../core/period/time_range.dart';
import '../../l10n/app_localizations.dart';

/// The localized label for a [TimeRange] selector chip. Kept out of the core
/// enum so `TimeRange` carries no UI copy.
String timeRangeLabel(AppLocalizations l10n, TimeRange range) => switch (range) {
      TimeRange.day => l10n.rangeDay,
      TimeRange.week => l10n.rangeWeek,
      TimeRange.month => l10n.rangeMonth,
      TimeRange.year => l10n.rangeYear,
    };
