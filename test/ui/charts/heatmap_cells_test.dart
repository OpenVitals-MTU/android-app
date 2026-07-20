import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/core/period/time_range.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/ui/charts/bar_chart.dart' show PeriodChartValue;
import 'package:openvitals/ui/charts/heatmap_chart.dart';

void main() {
  group('periodMonthHeatmapCells', () {
    test('a calendar month renders the whole month, greying past the window',
        () {
      // "This month" to date: July, loaded through the 20th.
      final period = DatePeriod(LocalDate(2026, 7, 1), LocalDate(2026, 7, 20));
      final cells = periodMonthHeatmapCells(const [], period);
      final dated = cells.where((c) => c.date != null).toList();

      expect(dated.first.date, LocalDate(2026, 7, 1));
      expect(dated.last.date, LocalDate(2026, 7, 31));
      expect(dated.length, 31);
      // Loaded through the 20th; later days are drawn but flagged out-of-window.
      expect(
        dated.firstWhere((c) => c.date == LocalDate(2026, 7, 20))
            .isWithinLoadedPeriod,
        isTrue,
      );
      expect(
        dated.firstWhere((c) => c.date == LocalDate(2026, 7, 21))
            .isWithinLoadedPeriod,
        isFalse,
      );
    });

    test('a rolling window spans both months and keeps every day', () {
      // "Last 30 days": 21 Jun – 20 Jul, crossing the month boundary.
      final period = DatePeriod(LocalDate(2026, 6, 21), LocalDate(2026, 7, 20));
      final cells = periodMonthHeatmapCells(
        [
          PeriodChartValue(LocalDate(2026, 6, 25), 1800), // first month
          PeriodChartValue(LocalDate(2026, 7, 5), 2200), // second month
        ],
        period,
        rolling: true,
      );
      final dated = cells.where((c) => c.date != null).toList();

      // The grid is exactly the 30-day window — not one calendar month of it.
      expect(dated.first.date, LocalDate(2026, 6, 21));
      expect(dated.last.date, LocalDate(2026, 7, 20));
      expect(dated.length, 30);
      // Every day of the window is loaded, and both months' values land — the
      // July value used to be dropped when only June was drawn.
      expect(dated.every((c) => c.isWithinLoadedPeriod), isTrue);
      expect(
        dated.firstWhere((c) => c.date == LocalDate(2026, 7, 5)).value,
        2200,
      );
      expect(
        dated.firstWhere((c) => c.date == LocalDate(2026, 6, 25)).value,
        1800,
      );
      // Nothing before the window sneaks in.
      expect(dated.any((c) => c.date!.isBefore(LocalDate(2026, 6, 21))), isFalse);
    });
  });
}
