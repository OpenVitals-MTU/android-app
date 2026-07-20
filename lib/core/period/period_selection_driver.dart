// ignore_for_file: prefer_initializing_formals

import '../time/local_date.dart';
import 'period_selection.dart';
import 'time_range.dart';

/// Mutable driver that owns the current [PeriodSelection] and tracks whether the
/// user has intentionally pinned a past period (so we do not auto-resume to
/// today under them). Mirrors the Kotlin `PeriodSelectionDriver`.
class PeriodSelectionDriver {
  PeriodSelectionDriver({
    required TimeRange initialRange,
    LocalDate? initialDate,
    this.weekPeriodMode = WeekPeriodMode.mondayToSunday,
    void Function(TimeRange)? onRangeSelected,
  })  : _onRangeSelected = onRangeSelected,
        _selection = PeriodSelection(
          initialRange,
          (initialDate ?? LocalDate.now()).coerceAtMost(LocalDate.now()),
        ) {
    _userPinnedPastPeriod = _isPastPeriod(_selection);
  }

  WeekPeriodMode weekPeriodMode;
  final void Function(TimeRange)? _onRangeSelected;

  PeriodSelection _selection;
  PeriodSelection get selection => _selection;

  late bool _userPinnedPastPeriod;

  PeriodSelection selectRange(TimeRange range) {
    _onRangeSelected?.call(range);
    return _update(_selection.selectRange(range), _userPinnedPastPeriod);
  }

  PeriodSelection previousPeriod() => _updateUserSelection(
        _selection.previousPeriod(weekPeriodMode: weekPeriodMode),
      );

  PeriodSelection? nextPeriod() {
    final next = _selection.nextPeriod(weekPeriodMode: weekPeriodMode);
    return next == _selection ? null : _updateUserSelection(next);
  }

  PeriodSelection selectDate(LocalDate date) =>
      _updateUserSelection(_selection.selectDate(date));

  /// Drill into a single day: switch to the Day range anchored on [date] (the
  /// month heatmap's tap-to-open-day). Persists the range like [selectRange] so
  /// the screen reopens on Day, and pins the past period like [selectDate].
  PeriodSelection selectDay(LocalDate date) {
    _onRangeSelected?.call(TimeRange.day);
    return _updateUserSelection(
      _selection.selectRange(TimeRange.day).selectDate(date),
    );
  }

  PeriodSelection? resumeCurrentPeriod({LocalDate? today}) {
    final resolvedToday = today ?? LocalDate.now();
    if (_userPinnedPastPeriod || !_isPastPeriod(_selection, resolvedToday)) {
      return null;
    }
    return _update(
      _selection.selectDate(resolvedToday, today: resolvedToday),
      false,
    );
  }

  PeriodSelection _updateUserSelection(PeriodSelection next) =>
      _update(next, _isPastPeriod(next));

  PeriodSelection _update(
    PeriodSelection next,
    bool userPinnedPastPeriod,
  ) {
    _selection = next;
    _userPinnedPastPeriod = userPinnedPastPeriod;
    return next;
  }

  bool _isPastPeriod(PeriodSelection selection, [LocalDate? today]) {
    final resolvedToday = today ?? LocalDate.now();
    return selection
        .period(today: resolvedToday, weekPeriodMode: weekPeriodMode)
        .end
        .isBefore(resolvedToday);
  }
}
