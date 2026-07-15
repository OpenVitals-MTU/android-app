import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/domain/insights/body_energy_timeline.dart';
import 'package:openvitals/features/bodyenergy/application/body_energy_display.dart';
import 'package:openvitals/features/bodyenergy/presentation/body_energy_timeline_chart.dart';
import 'package:openvitals/l10n/app_localizations.dart';
import 'package:openvitals/ui/charts/chart_zoom.dart';

/// The Body Energy timeline is a line PLUS an influence strip PLUS an hour row.
/// Pinching has to move all three together — a strip that kept the whole day's
/// scale while the line stretched would place the bars under the wrong hours.
void main() {
  testWidgets('pinching the Body Energy chart zooms the line, strip and hours', (
    tester,
  ) async {
    final points = [
      for (var i = 0; i <= 24; i++)
        BodyEnergyChartPoint(i / 24.0, 50 + (i % 5) * 5.0),
    ];
    final bars = [
      for (var i = 0; i < 24; i++)
        BodyEnergyInfluenceBar(
          xFraction: i / 24.0,
          widthFraction: 1 / 24.0,
          charge: 0,
          drain: (i % 3).toDouble(),
          influence: BodyEnergyPrimaryInfluence.everydayActivity,
        ),
    ];

    await tester.pumpWidget(
      MaterialApp(
        localizationsDelegates: AppLocalizations.localizationsDelegates,
        supportedLocales: AppLocalizations.supportedLocales,
        home: Scaffold(
          body: BodyEnergyTimelineChart(
            points: points,
            influenceBars: bars,
            maxMagnitude: 3,
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    // Before: the whole day is on show.
    expect(find.byType(ChartZoom), findsOneWidget);
    expect(find.text('24:00'), findsOneWidget);

    final center = tester.getCenter(find.byType(ChartZoom));
    final left = await tester.startGesture(center - const Offset(30, 0));
    final right = await tester.startGesture(center + const Offset(30, 0));
    await tester.pump();
    await left.moveBy(const Offset(-90, 0));
    await right.moveBy(const Offset(90, 0));
    await tester.pump();
    await left.up();
    await right.up();
    await tester.pumpAndSettle();

    // After: only a slice remains, so the day's end label is no longer on the row.
    expect(find.text('24:00'), findsNothing);
    expect(find.text('00:00'), findsNothing);
  });
}
