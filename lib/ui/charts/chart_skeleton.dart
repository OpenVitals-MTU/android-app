import 'package:flutter/material.dart';

import '../theme/chart_tokens.dart';

/// The shape of what is coming, while it comes.
///
/// A spinner in the middle of a card says "something is happening". A skeleton says
/// "a chart is happening, and it will be about this big" — so the page does not
/// jump when the data lands, and the eye has already found the place to look. It is
/// also the difference between a screen that feels like it is loading and one that
/// feels like it is broken: eleven screens used to answer every load with the same
/// centred circle, and a circle is what you show when you have no idea what is
/// coming.
enum ChartSkeletonShape { line, bars }

class ChartSkeleton extends StatefulWidget {
  const ChartSkeleton({
    super.key,
    this.shape = ChartSkeletonShape.line,
    this.height = kChartHeightDay,
    this.barCount = 7,
  });

  final ChartSkeletonShape shape;
  final double height;
  final int barCount;

  @override
  State<ChartSkeleton> createState() => _ChartSkeletonState();
}

class _ChartSkeletonState extends State<ChartSkeleton>
    with SingleTickerProviderStateMixin {
  late final AnimationController _controller = AnimationController(
    vsync: this,
    duration: const Duration(milliseconds: 1200),
  );

  bool _started = false;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (_started) return;
    _started = true;
    // A REPEATING animation, which is the one kind that can hang a test suite
    // forever: `pumpAndSettle` waits for a frame that never comes. Reduce-motion
    // pins it to a still frame, and the golden harness and every widget test that
    // settles are all downstream of that one line.
    if (!(MediaQuery.maybeDisableAnimationsOf(context) ?? false)) {
      _controller.repeat(reverse: true);
    }
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final tokens = ChartTokens.read(context);

    return SizedBox(
      height: widget.height,
      width: double.infinity,
      child: AnimatedBuilder(
        animation: _controller,
        builder: (context, _) {
          // Breathes between two alphas rather than sweeping a gradient across the
          // card: a shimmer that travels is a thing to watch, and this is a thing
          // to stop noticing.
          final alpha = 0.35 + 0.25 * _controller.value;
          final color = tokens.track.withValues(alpha: alpha);
          return switch (widget.shape) {
            ChartSkeletonShape.bars => Row(
                crossAxisAlignment: CrossAxisAlignment.end,
                mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                children: [
                  for (var i = 0; i < widget.barCount; i++)
                    _Bar(
                      color: color,
                      // Uneven, because a row of identical bars reads as data — a
                      // very boring week — rather than as an absence of it.
                      fraction: const [0.45, 0.7, 0.35, 0.85, 0.55, 0.75, 0.4][
                          i % 7],
                    ),
                ],
              ),
            ChartSkeletonShape.line => Center(
                child: Container(
                  height: 3,
                  margin: const EdgeInsets.symmetric(horizontal: 8),
                  decoration: BoxDecoration(
                    color: color,
                    borderRadius: BorderRadius.circular(2),
                  ),
                ),
              ),
          };
        },
      ),
    );
  }
}

class _Bar extends StatelessWidget {
  const _Bar({required this.color, required this.fraction});

  final Color color;
  final double fraction;

  @override
  Widget build(BuildContext context) => LayoutBuilder(
        builder: (context, constraints) => Container(
          width: 14,
          height: constraints.maxHeight * fraction,
          decoration: BoxDecoration(
            color: color,
            borderRadius: BorderRadius.vertical(
              top: Radius.circular(chartBarRadius(14)),
            ),
          ),
        ),
      );
}
