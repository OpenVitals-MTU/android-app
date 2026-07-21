import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../core/period/period_titles.dart';
import '../../core/period/time_range.dart';
import '../../core/time/local_date.dart';
import '../../l10n/app_localizations.dart';

// Getters, not cached finals: constructed per use so they follow the current
// Intl.defaultLocale (the app language) instead of freezing at first access.
DateFormat get _dayTitleFormat => DateFormat('EEE, d MMM');
DateFormat get _daySubtitleFormat => DateFormat('d MMM yyyy');
DateFormat get _periodSubtitleFormat => DateFormat('EEE d MMM');

DateTime _toDateTime(LocalDate date) => DateTime(date.year, date.month, date.day);

/// The subtitle beneath a period title (single date for DAY, range otherwise).
/// Port of Kotlin `localizedPeriodSubtitle`.
String periodSubtitle(TimeRange range, DatePeriod period) {
  final start = _periodSubtitleFormat.format(_toDateTime(period.start));
  if (range == TimeRange.day) return start;
  final end = _periodSubtitleFormat.format(_toDateTime(period.end));
  return '$start - $end';
}

String _dayTitle(AppLocalizations l10n, LocalDate date) {
  final today = LocalDate.now();
  if (date == today) return l10n.periodToday;
  if (date == today.minusDays(1)) return l10n.periodYesterday;
  return _dayTitleFormat.format(_toDateTime(date));
}

/// The prev/next + title header for a period, with tap-to-open calendar and
/// horizontal swipe navigation. Forward navigation is disabled past the current
/// period ([canGoForward]). Port of Kotlin `PeriodNavigator`.
class PeriodNavigator extends StatelessWidget {
  const PeriodNavigator({
    super.key,
    required this.selectedRange,
    required this.period,
    required this.canGoForward,
    required this.onPreviousPeriod,
    required this.onNextPeriod,
    required this.onOpenCalendar,
    this.title,
    this.subtitle,
    this.weekPeriodMode = WeekPeriodMode.mondayToSunday,
  });

  final TimeRange selectedRange;
  final DatePeriod period;
  final bool canGoForward;
  final VoidCallback onPreviousPeriod;
  final VoidCallback onNextPeriod;
  final VoidCallback onOpenCalendar;
  final String? title;
  final String? subtitle;
  final WeekPeriodMode weekPeriodMode;

  @override
  Widget build(BuildContext context) {
    return _NavigatorRow(
      title: title ??
          periodTitle(
            AppLocalizations.of(context),
            selectedRange,
            period,
            weekPeriodMode: weekPeriodMode,
          ),
      subtitle: subtitle ?? periodSubtitle(selectedRange, period),
      canGoForward: canGoForward,
      onPrevious: onPreviousPeriod,
      onNext: onNextPeriod,
      onOpenCalendar: onOpenCalendar,
      previousTooltip: 'Previous period',
      nextTooltip: 'Next period',
    );
  }
}

/// The single-day variant of [PeriodNavigator]. Port of Kotlin `DayNavigator`.
class DayNavigator extends StatelessWidget {
  const DayNavigator({
    super.key,
    required this.date,
    required this.canGoForward,
    required this.onPreviousDay,
    required this.onNextDay,
    required this.onOpenCalendar,
  });

  final LocalDate date;
  final bool canGoForward;
  final VoidCallback onPreviousDay;
  final VoidCallback onNextDay;
  final VoidCallback onOpenCalendar;

  @override
  Widget build(BuildContext context) {
    return _NavigatorRow(
      title: _dayTitle(AppLocalizations.of(context), date),
      subtitle: _daySubtitleFormat.format(_toDateTime(date)),
      canGoForward: canGoForward,
      onPrevious: onPreviousDay,
      onNext: onNextDay,
      onOpenCalendar: onOpenCalendar,
      previousTooltip: 'Previous day',
      nextTooltip: 'Next day',
    );
  }
}

class _NavigatorRow extends StatelessWidget {
  const _NavigatorRow({
    required this.title,
    required this.subtitle,
    required this.canGoForward,
    required this.onPrevious,
    required this.onNext,
    required this.onOpenCalendar,
    required this.previousTooltip,
    required this.nextTooltip,
  });

  final String title;
  final String subtitle;
  final bool canGoForward;
  final VoidCallback onPrevious;
  final VoidCallback onNext;
  final VoidCallback onOpenCalendar;
  final String previousTooltip;
  final String nextTooltip;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Row(
      crossAxisAlignment: CrossAxisAlignment.center,
      children: [
        Expanded(
          child: _SwipeNavigation(
            canGoForward: canGoForward,
            onPrevious: onPrevious,
            onNext: onNext,
            child: GestureDetector(
              behavior: HitTestBehavior.opaque,
              onTap: onOpenCalendar,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(title, style: theme.textTheme.titleLarge),
                  Text(
                    subtitle,
                    style: theme.textTheme.bodySmall
                        ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                  ),
                ],
              ),
            ),
          ),
        ),
        const SizedBox(width: 12),
        _IconSurfaceButton(
          icon: Icons.chevron_left,
          tooltip: previousTooltip,
          onPressed: onPrevious,
        ),
        const SizedBox(width: 8),
        _IconSurfaceButton(
          icon: Icons.chevron_right,
          tooltip: nextTooltip,
          onPressed: canGoForward ? onNext : null,
        ),
        const SizedBox(width: 8),
        _IconSurfaceButton(
          icon: Icons.calendar_month_outlined,
          tooltip: 'Open calendar',
          onPressed: onOpenCalendar,
        ),
      ],
    );
  }
}

class _IconSurfaceButton extends StatelessWidget {
  const _IconSurfaceButton({
    required this.icon,
    required this.tooltip,
    required this.onPressed,
  });

  final IconData icon;
  final String tooltip;
  final VoidCallback? onPressed;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final enabled = onPressed != null;
    return Material(
      color: scheme.surfaceContainer,
      shape: const CircleBorder(),
      clipBehavior: Clip.antiAlias,
      child: IconButton(
        tooltip: tooltip,
        onPressed: onPressed,
        icon: Icon(
          icon,
          color: enabled
              ? scheme.onSurface
              : scheme.onSurface.withValues(alpha: 0.38),
        ),
      ),
    );
  }
}

/// Horizontal-drag navigation: a rightward swipe goes to the previous period, a
/// leftward swipe (when [canGoForward]) to the next. Port of Kotlin
/// `Modifier.dateNavigationSwipe`.
class _SwipeNavigation extends StatefulWidget {
  const _SwipeNavigation({
    required this.child,
    required this.canGoForward,
    required this.onPrevious,
    required this.onNext,
  });

  final Widget child;
  final bool canGoForward;
  final VoidCallback onPrevious;
  final VoidCallback onNext;

  @override
  State<_SwipeNavigation> createState() => _SwipeNavigationState();
}

class _SwipeNavigationState extends State<_SwipeNavigation> {
  static const double _thresholdFraction = 0.25;
  static const double _minThreshold = 40;
  static const double _maxThreshold = 96;

  double _drag = 0;
  double _width = 0;

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        _width = constraints.maxWidth;
        return GestureDetector(
          behavior: HitTestBehavior.opaque,
          onHorizontalDragStart: (_) => _drag = 0,
          onHorizontalDragUpdate: (details) => _drag += details.delta.dx,
          onHorizontalDragEnd: (_) {
            final threshold =
                (_width * _thresholdFraction).clamp(_minThreshold, _maxThreshold);
            if (_drag >= threshold) {
              widget.onPrevious();
            } else if (_drag <= -threshold && widget.canGoForward) {
              widget.onNext();
            }
            _drag = 0;
          },
          child: widget.child,
        );
      },
    );
  }
}
