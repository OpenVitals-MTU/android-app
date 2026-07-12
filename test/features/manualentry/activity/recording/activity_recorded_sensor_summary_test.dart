import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/presentation/unit_formatter.dart';
import 'package:openvitals/domain/model/ble_sensor_models.dart';
import 'package:openvitals/domain/preferences/unit_system.dart';
import 'package:openvitals/features/activity/presentation/activity_heart_rate_chart_card.dart';
import 'package:openvitals/features/manualentry/activity/recording/activity_recorded_sensor_summary.dart';
import 'package:openvitals/l10n/app_localizations.dart';
import 'package:openvitals/ui/charts/metric_line_plot.dart';

void main() {
  // The unit system boundary: pin metric so the harness does not follow the
  // host locale.
  final unitFormatter = UnitFormatter(
    unitSystemProvider: () => UnitSystem.metric,
    localeProvider: () => 'en_US',
  );

  final sessionStart = DateTime(2026, 6, 1, 8);
  final sessionEnd = DateTime(2026, 6, 1, 9);

  BleRecordingSampleBuffer buffer() => BleRecordingSampleBuffer(
        heartRateSamples: [
          for (var minute = 0; minute < 30; minute++)
            BleHeartRateSample(
              time: sessionStart.add(Duration(minutes: minute * 2)),
              beatsPerMinute: 120 + minute,
            ),
        ],
      );

  Widget harness(Widget child) => MaterialApp(
        localizationsDelegates: AppLocalizations.localizationsDelegates,
        supportedLocales: AppLocalizations.supportedLocales,
        home: Scaffold(body: SingleChildScrollView(child: child)),
      );

  testWidgets('renders the time-axis heart-rate chart card', (tester) async {
    await tester.pumpWidget(
      harness(
        ActivityRecordedSensorSummary(
          samples: buffer(),
          unitFormatter: unitFormatter,
          sessionStart: sessionStart,
          sessionEnd: sessionEnd,
        ),
      ),
    );

    expect(find.byType(ActivityHeartRateChartCard), findsOneWidget);
    expect(find.byType(MetricLinePlot), findsOneWidget);
    // The localized title and the avg / range / samples stat row.
    expect(find.text('Heart rate'), findsOneWidget);
    expect(find.text('Avg'), findsOneWidget);
    // Average of 120..149; also the Y-axis midpoint label, so more than one.
    expect(find.text('135 bpm'), findsWidgets);
    expect(find.text('Range'), findsOneWidget);
    expect(find.text('120 bpm-149 bpm'), findsOneWidget);
    expect(find.text('Samples'), findsOneWidget);
    expect(find.text('30'), findsOneWidget);
    // The elapsed-time X-axis spans the full one-hour session.
    expect(find.text('0:00'), findsOneWidget);
    expect(find.text('1:00:00'), findsOneWidget);
  });

  testWidgets('falls back to the sample range without a session range',
      (tester) async {
    await tester.pumpWidget(
      harness(
        ActivityRecordedSensorSummary(
          samples: buffer(),
          unitFormatter: unitFormatter,
        ),
      ),
    );

    expect(find.byType(ActivityHeartRateChartCard), findsOneWidget);
    // Samples span 58 minutes (0, 2, ..., 58), so the axis ends at 58:00.
    expect(find.text('58:00'), findsOneWidget);
  });

  testWidgets('renders nothing when no sensor produced samples',
      (tester) async {
    await tester.pumpWidget(
      harness(
        ActivityRecordedSensorSummary(
          samples: const BleRecordingSampleBuffer(),
          unitFormatter: unitFormatter,
        ),
      ),
    );

    expect(find.byType(ActivityHeartRateChartCard), findsNothing);
  });
}
