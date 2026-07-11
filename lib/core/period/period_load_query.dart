import '../time/local_date.dart';
import 'period_calculations.dart';
import 'time_range.dart';

class PeriodWindows {
  const PeriodWindows({
    required this.current,
    required this.previous,
    required this.baseline,
  });

  final DatePeriod current;
  final DatePeriod previous;
  final DatePeriod baseline;

  @override
  bool operator ==(Object other) =>
      other is PeriodWindows &&
      other.current == current &&
      other.previous == previous &&
      other.baseline == baseline;

  @override
  int get hashCode => Object.hash(current, previous, baseline);
}

class PeriodLoadQuery {
  PeriodLoadQuery({
    required this.range,
    required this.anchorDate,
    LocalDate? today,
    this.baselineDays = defaultBaselineDays,
    this.weekPeriodMode = WeekPeriodMode.mondayToSunday,
  }) : today = today ?? LocalDate.now();

  final TimeRange range;
  final LocalDate anchorDate;
  final LocalDate today;
  final int baselineDays;
  final WeekPeriodMode weekPeriodMode;

  LocalDate get selectedDate => anchorDate.coerceAtMost(today);

  PeriodWindows get windows => periodWindowsFor(
        range,
        selectedDate,
        today: today,
        baselineDays: baselineDays,
        weekPeriodMode: weekPeriodMode,
      );

  @override
  bool operator ==(Object other) =>
      other is PeriodLoadQuery &&
      other.range == range &&
      other.anchorDate == anchorDate &&
      other.today == today &&
      other.baselineDays == baselineDays &&
      other.weekPeriodMode == weekPeriodMode;

  @override
  int get hashCode =>
      Object.hash(range, anchorDate, today, baselineDays, weekPeriodMode);
}
