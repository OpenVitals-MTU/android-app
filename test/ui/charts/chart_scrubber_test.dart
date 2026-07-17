import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/ui/charts/chart_scrubber.dart';
import 'package:openvitals/ui/charts/chart_zoom.dart';

/// Scrubbing a chart, and — far more importantly — still being able to scroll the
/// page it is sitting on.
///
/// Every chart in this app lives inside a vertically scrolling screen, and a chart
/// is most of what is on that screen. A `GestureDetector` with `onPanUpdate` claims
/// BOTH axes the moment a drag begins inside it, so a user dragging a thumb up the
/// page would find the page frozen — not a subtle regression, but the app not
/// working, on ten screens.
///
/// `onHorizontalDrag*` enters the arena for horizontal movement only. This is the
/// test that says so, and it is the reason the scrubber is allowed to exist.
void main() {
  const targets = <ScrubTarget>[
    (xFraction: 0.0, yFraction: 0.1, primary: '800 steps', secondary: '7:00 AM'),
    (xFraction: 0.5, yFraction: 0.5, primary: '5,100 steps', secondary: '12:00 PM'),
    (xFraction: 1.0, yFraction: 0.9, primary: '11,200 steps', secondary: '9:00 PM'),
  ];

  Widget harness({ScrollController? controller}) => MaterialApp(
        home: Scaffold(
          body: ListView(
            controller: controller,
            children: [
              const SizedBox(height: 400),
              const ChartScrubber(
                targets: targets,
                accentColor: Colors.green,
                child: SizedBox(height: 200, width: 300),
              ),
              const SizedBox(height: 1200),
            ],
          ),
        ),
      );

  testWidgets('a VERTICAL drag starting on the chart still scrolls the page',
      (tester) async {
    final controller = ScrollController();
    addTearDown(controller.dispose);
    await tester.pumpWidget(harness(controller: controller));

    expect(controller.offset, 0);
    // Thumb lands on the chart and drags UP, exactly as it would to read further
    // down a metric screen.
    await tester.drag(find.byType(ChartScrubber), const Offset(0, -300));
    await tester.pumpAndSettle();

    expect(
      controller.offset,
      greaterThan(0),
      reason: 'the page must still scroll under a finger that started on a chart',
    );
    // And it did not scrub while doing it.
    expect(find.textContaining('steps'), findsNothing);
  });

  testWidgets('a HORIZONTAL drag reads the chart', (tester) async {
    await tester.pumpWidget(harness());

    final chart = find.byType(ChartScrubber);
    final gesture = await tester.startGesture(tester.getCenter(chart));
    await gesture.moveBy(const Offset(40, 0));
    await tester.pump();

    // Landed on the middle sample: the value, and when it was taken.
    expect(find.text('5,100 steps'), findsOneWidget);
    expect(find.text('12:00 PM'), findsOneWidget);

    // And it lets go cleanly.
    await gesture.up();
    await tester.pumpAndSettle();
    expect(find.text('5,100 steps'), findsNothing);
  });

  testWidgets('it snaps to the nearest SAMPLE, never between two', (tester) async {
    // The curve between two samples is an interpolation the app invented. A
    // tooltip may only ever report a number that was actually measured.
    await tester.pumpWidget(harness());

    final chart = find.byType(ChartScrubber);
    final left = tester.getTopLeft(chart);
    final width = tester.getSize(chart).width;

    final gesture = await tester.startGesture(
      Offset(left.dx + width * 0.82, tester.getCenter(chart).dy),
    );
    // Past the touch slop, or the drag never starts and the arena never hands the
    // gesture over.
    await gesture.moveBy(const Offset(24, 0));
    await tester.pump();

    // 0.9 is nearer the last sample (1.0) than the middle one (0.5).
    expect(find.text('11,200 steps'), findsOneWidget);
    await gesture.up();
  });

  testWidgets('it stands down while a pinch is in progress', (tester) async {
    // A pinch is a zoom, not a read. The finger that began this scrub is already
    // routed to the scrubber and cannot be handed back once a second finger lands,
    // so the scrubber hides itself off [ChartZoomScope] rather than fight the zoom.
    Widget scoped(bool pinching) => MaterialApp(
          home: Scaffold(
            body: ListView(
              children: [
                const SizedBox(height: 400),
                ChartZoomScope(
                  multiTouch: pinching,
                  child: const ChartScrubber(
                    targets: targets,
                    accentColor: Colors.green,
                    child: SizedBox(height: 200, width: 300),
                  ),
                ),
                const SizedBox(height: 1200),
              ],
            ),
          ),
        );

    await tester.pumpWidget(scoped(false));
    final chart = find.byType(ChartScrubber);
    final gesture = await tester.startGesture(tester.getCenter(chart));
    await gesture.moveBy(const Offset(40, 0));
    await tester.pump();
    // One finger reads the chart as usual.
    expect(find.text('5,100 steps'), findsOneWidget);

    // A second finger lands (a pinch): the tooltip is dropped and the scrubber
    // ignores further drags, leaving the two-finger gesture to the zoom.
    await tester.pumpWidget(scoped(true));
    await tester.pump();
    expect(find.text('5,100 steps'), findsNothing);

    await gesture.moveBy(const Offset(40, 0));
    await tester.pump();
    expect(find.textContaining('steps'), findsNothing);

    await gesture.up();
  });

  testWidgets('a chart with nothing to say stays inert', (tester) async {
    await tester.pumpWidget(
      const MaterialApp(
        home: Scaffold(
          body: ChartScrubber(
            targets: <ScrubTarget>[],
            accentColor: Colors.green,
            child: SizedBox(height: 200, width: 300),
          ),
        ),
      ),
    );

    // No targets, no gesture detector at all — it hands the child straight back,
    // so an empty chart cannot eat a drag that belongs to the page.
    expect(find.byType(GestureDetector), findsNothing);
  });
}
