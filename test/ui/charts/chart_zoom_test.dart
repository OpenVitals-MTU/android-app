import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/ui/charts/chart_viewport.dart';
import 'package:openvitals/ui/charts/chart_zoom.dart';

/// Pinch-to-zoom, and the two things it must not break.
///
/// A chart sits inside a scrolling page and already claims the single-finger horizontal
/// drag for its scrubber, leaving the vertical one to the page. A zoom that accepted one
/// finger would eat one or the other. So the invariants are as important as the feature:
/// one finger must still scrub, and the page must still scroll.

void main() {
  group('ChartViewport', () {
    test('starts as the whole chart', () {
      expect(ChartViewport.full.span, 1.0);
      expect(ChartViewport.full.isZoomed, isFalse);
      expect(ChartViewport.full.visibleFraction(0.25), 0.25);
    });

    test('zooming keeps the point between the fingers under the fingers', () {
      // Pinch out by 2x around a point a quarter of the way across the plot. Whatever
      // datum was under that point must still be under it — that is what makes the chart
      // feel like it is being stretched rather than replaced.
      const before = ChartViewport.full;
      final anchorData = before.dataFraction(0.25);

      final after = before.zoomed(2.0, 0.25);

      expect(after.span, closeTo(0.5, 1e-9));
      expect(after.visibleFraction(anchorData), closeTo(0.25, 1e-9));
    });

    test('a point outside the window is NOT clamped into it', () {
      final view = ChartViewport.full.zoomed(2.0, 0.5);

      // A line leaving the left edge has to carry on to where it really is. Clamping it
      // would bend it up into the corner and draw a value nobody recorded.
      expect(view.visibleFraction(0.0), lessThan(0.0));
      expect(view.visibleFraction(1.0), greaterThan(1.0));
    });

    test('panning moves the data under the finger by the distance dragged', () {
      final view = ChartViewport.full.zoomed(4.0, 0.5); // span 0.25
      final datum = view.dataFraction(0.5);

      // Drag a tenth of the plot to the left and the datum comes WITH the finger: it was
      // halfway across the plot, and it is now a tenth further left.
      final panned = view.panned(-0.1);

      expect(panned.visibleFraction(datum), closeTo(0.4, 1e-9));
    });

    test('the window stops at the ends rather than sliding off', () {
      final view = ChartViewport.full.zoomed(4.0, 0.0);
      expect(view.start, 0.0);

      // Dragging further left has nothing to show, so it does nothing.
      expect(view.panned(1.0).start, 0.0);
      expect(view.panned(1.0).end, closeTo(view.span, 1e-9));

      final right = ChartViewport.full.zoomed(4.0, 1.0);
      expect(right.end, closeTo(1.0, 1e-9));
      expect(right.panned(-1.0).end, closeTo(1.0, 1e-9));
    });

    test('there is a floor on how far you can zoom in', () {
      final view = ChartViewport.full.zoomed(1000.0, 0.5);
      expect(view.span, closeTo(ChartViewport.minimumSpan, 1e-9));
    });

    test('zooming back out never overshoots the whole chart', () {
      final view = ChartViewport.full.zoomed(4.0, 0.5).zoomed(0.01, 0.5);
      expect(view.span, 1.0);
      expect(view.start, 0.0);
      expect(view.isZoomed, isFalse);
    });
  });

  group('ChartZoom', () {
    Future<ChartViewport> pumpAndRead(
      WidgetTester tester,
      Future<void> Function(WidgetTester tester) gesture, {
      bool inScrollable = false,
      ScrollController? controller,
    }) async {
      var latest = ChartViewport.full;
      final chart = SizedBox(
        height: 200,
        width: 400,
        child: ChartZoom(
          builder: (context, viewport) {
            latest = viewport;
            return const ColoredBox(color: Color(0xFF000000));
          },
        ),
      );

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: inScrollable
                ? ListView(
                    controller: controller,
                    children: [
                      const SizedBox(height: 400),
                      chart,
                      const SizedBox(height: 2000),
                    ],
                  )
                : Center(child: chart),
          ),
        ),
      );
      await tester.pumpAndSettle();

      await gesture(tester);
      await tester.pumpAndSettle();
      return latest;
    }

    testWidgets('two fingers pinching apart zooms in', (tester) async {
      final viewport = await pumpAndRead(tester, (tester) async {
        final center = tester.getCenter(find.byType(ChartZoom));
        final left = await tester.startGesture(center - const Offset(20, 0));
        final right = await tester.startGesture(center + const Offset(20, 0));
        await tester.pump();

        // Spread them apart: 40px becomes 160px, a 4x pinch.
        await left.moveBy(const Offset(-60, 0));
        await right.moveBy(const Offset(60, 0));
        await tester.pump();

        await left.up();
        await right.up();
      });

      expect(viewport.isZoomed, isTrue);
      expect(viewport.span, lessThan(0.5));
    });

    testWidgets('two fingers zoom even inside a scrolling page', (tester) async {
      // The device bug this fixes: on a screen tall enough to scroll (the year
      // overview stacks a dozen charts), a passive Listener never held the pinch —
      // the parent Scrollable claimed the pointers and the second finger never
      // reached the chart. A ScaleGestureRecognizer wins the two-finger gesture in
      // the arena so the zoom survives inside a ListView.
      final controller = ScrollController();
      addTearDown(controller.dispose);

      final viewport = await pumpAndRead(
        tester,
        (tester) async {
          final center = tester.getCenter(find.byType(ChartZoom));
          final left = await tester.startGesture(center - const Offset(20, 0));
          final right = await tester.startGesture(center + const Offset(20, 0));
          await tester.pump();
          await left.moveBy(const Offset(-60, 0));
          await right.moveBy(const Offset(60, 0));
          await tester.pump();
          await left.up();
          await right.up();
        },
        inScrollable: true,
        controller: controller,
      );

      expect(viewport.isZoomed, isTrue);
      expect(controller.offset, 0.0,
          reason: 'a horizontal pinch must zoom, not scroll the page');
    });

    testWidgets('ONE finger dragging horizontally does not zoom', (tester) async {
      // The single-finger horizontal drag belongs to the scrubber. If the zoom took it,
      // scrubbing would be gone.
      final viewport = await pumpAndRead(tester, (tester) async {
        final center = tester.getCenter(find.byType(ChartZoom));
        final finger = await tester.startGesture(center);
        await tester.pump();
        await finger.moveBy(const Offset(-120, 0));
        await tester.pump();
        await finger.up();
      });

      expect(viewport, ChartViewport.full,
          reason: 'one finger is the scrubber, and must stay the scrubber');
    });

    testWidgets('the page still scrolls when dragged from inside a chart',
        (tester) async {
      // The reason the scrubber uses horizontal-drag-only rather than pan. A zoom that
      // claimed the vertical axis would freeze the page under the user's thumb.
      final controller = ScrollController();
      addTearDown(controller.dispose);

      await pumpAndRead(
        tester,
        (tester) async {
          await tester.drag(find.byType(ChartZoom), const Offset(0, -200));
        },
        inScrollable: true,
        controller: controller,
      );

      expect(controller.offset, greaterThan(0.0),
          reason: 'a chart must never be a hole the page cannot be scrolled from');
    });

    testWidgets('double tap returns the whole chart', (tester) async {
      final viewport = await pumpAndRead(tester, (tester) async {
        final center = tester.getCenter(find.byType(ChartZoom));
        final left = await tester.startGesture(center - const Offset(20, 0));
        final right = await tester.startGesture(center + const Offset(20, 0));
        await tester.pump();
        await left.moveBy(const Offset(-60, 0));
        await right.moveBy(const Offset(60, 0));
        await tester.pump();
        await left.up();
        await right.up();
        await tester.pumpAndSettle();

        // A chart you can zoom into and not get out of is worse than one that never
        // zoomed.
        await tester.tap(find.byType(ChartZoom));
        await tester.pump(const Duration(milliseconds: 50));
        await tester.tap(find.byType(ChartZoom));
      });

      expect(viewport, ChartViewport.full);
    });
  });
}
