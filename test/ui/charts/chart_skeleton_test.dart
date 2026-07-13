import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/ui/charts/chart_reveal.dart';
import 'package:openvitals/ui/charts/chart_skeleton.dart';

/// The two animations, and the one switch that keeps them from hanging the suite.
///
/// `ChartSkeleton` REPEATS, which is the only kind of animation that can hang
/// `pumpAndSettle` forever — it waits for a frame that never comes. `ChartReveal`
/// runs once, which is milder but would still make every single-`pump()` assertion
/// in the app see a half-drawn chart.
///
/// `MediaQuery.disableAnimations` is the answer to both, and it is not a test hack:
/// it is the reduce-motion contract, and someone who has asked their phone to stop
/// moving things has asked these to stop too.
void main() {
  Future<void> pumpIn(
    WidgetTester tester,
    Widget child, {
    required bool disableAnimations,
  }) =>
      tester.pumpWidget(
        MaterialApp(
          home: MediaQuery(
            data: MediaQueryData(disableAnimations: disableAnimations),
            child: Scaffold(body: child),
          ),
        ),
      );

  testWidgets('the skeleton settles when motion is off', (tester) async {
    await pumpIn(
      tester,
      const ChartSkeleton(shape: ChartSkeletonShape.bars),
      disableAnimations: true,
    );

    // If the repeat were running, this would spin until the test timed out.
    await tester.pumpAndSettle();
    expect(find.byType(ChartSkeleton), findsOneWidget);
  });

  testWidgets('the skeleton does animate when motion is on', (tester) async {
    await pumpIn(
      tester,
      const ChartSkeleton(),
      disableAnimations: false,
    );

    await tester.pump(const Duration(milliseconds: 100));
    // A repeating animation never settles — asserting that IS the assertion, and
    // it is why the switch above has to exist.
    expect(tester.hasRunningAnimations, isTrue);
  });

  testWidgets('the reveal is fully drawn on the first frame when motion is off',
      (tester) async {
    final seen = <double>[];
    await pumpIn(
      tester,
      ChartReveal(
        builder: (context, t) {
          seen.add(t);
          return const SizedBox(height: 10);
        },
      ),
      disableAnimations: true,
    );

    // Not 0-and-then-1: 1 immediately. A test that pumps once and asserts on a
    // chart must see the whole chart.
    expect(seen.last, 1.0);
    expect(tester.hasRunningAnimations, isFalse);
  });

  testWidgets('the reveal animates from nothing when motion is on',
      (tester) async {
    final seen = <double>[];
    await pumpIn(
      tester,
      ChartReveal(
        builder: (context, t) {
          seen.add(t);
          return const SizedBox(height: 10);
        },
      ),
      disableAnimations: false,
    );

    expect(seen.first, 0.0);
    await tester.pumpAndSettle();
    // And it finishes — unlike the skeleton, this one is allowed to settle.
    expect(seen.last, 1.0);
  });
}
