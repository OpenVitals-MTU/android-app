import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/presentation/unit_formatter.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/domain/model/mindfulness_models.dart';
import 'package:openvitals/domain/preferences/unit_system.dart';
import 'package:openvitals/features/mindfulness/presentation/mindfulness_intraday_chart.dart';
import 'package:openvitals/l10n/app_localizations.dart';
import 'package:openvitals/ui/charts/metric_line_plot.dart';

/// Kotlin had a mindfulness day chart. The Flutter port dropped it, and the Day
/// range fell back to a bar chart of a single day — one fat bar, repeating the
/// number already printed on the card above it.
MindfulnessSession _session(DateTime start, Duration duration) =>
    MindfulnessSession(
      id: start.toIso8601String(),
      title: null,
      startTime: start,
      endTime: start.add(duration),
      durationMs: duration.inMilliseconds,
      source: 'Test',
    );

void main() {
  const day = LocalDate(2026, 3, 4);
  final dayStart = DateTime(2026, 3, 4);
  final formatter = UnitFormatter(unitSystemProvider: () => UnitSystem.metric);

  Future<void> pump(WidgetTester tester, Widget card) async {
    await tester.pumpWidget(MaterialApp(
      localizationsDelegates: AppLocalizations.localizationsDelegates,
      supportedLocales: AppLocalizations.supportedLocales,
      home: Scaffold(body: SingleChildScrollView(child: card)),
    ));
    expect(tester.takeException(), isNull);
  }

  test('cumulativeMindfulness banks the minutes when a session ENDS', () {
    // Not when it starts: a session you are still sitting is not minutes you have
    // done. A 30-minute sit begun at 06:00 lands on the chart at 06:30.
    final points = cumulativeMindfulness([
      _session(dayStart.add(const Duration(hours: 6)),
          const Duration(minutes: 30)),
      _session(dayStart.add(const Duration(hours: 18)),
          const Duration(minutes: 10)),
    ]);

    expect(points.map((p) => p.value).toList(), [30.0, 40.0]);
    expect(points.first.time.hour, 6);
    expect(points.first.time.minute, 30);
  });

  test('a zero-length session never enters the curve', () {
    final points = cumulativeMindfulness([_session(dayStart, Duration.zero)]);
    expect(points, isEmpty);
  });

  testWidgets('a day with a session draws a plot, an empty day does not',
      (tester) async {
    await pump(
      tester,
      MindfulnessIntradayChartCard(
        selectedDate: day,
        sessions: [
          _session(dayStart.add(const Duration(hours: 6)),
              const Duration(minutes: 30)),
        ],
        formatter: formatter,
        now: DateTime(2026, 3, 10),
      ),
    );

    final plot = tester.widget<MetricLinePlot>(find.byType(MetricLinePlot));
    // 06:30 is 27% of the way through the day, and that is where it is drawn — the
    // bug this whole exercise came from put it somewhere else entirely.
    expect(plot.points[1].xFraction, closeTo(0.271, 0.002));
    expect(find.text('12:00'), findsOneWidget);

    await pump(
      tester,
      MindfulnessIntradayChartCard(
        selectedDate: day,
        sessions: const [],
        formatter: formatter,
        now: DateTime(2026, 3, 10),
      ),
    );
    expect(find.byType(MetricLinePlot), findsNothing);
  });
}
