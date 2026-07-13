import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../theme/chart_tokens.dart';

/// A point a scrub can land on, in the plot's own fraction space.
typedef ScrubTarget = ({
  double xFraction,
  double yFraction,
  String primary,
  String? secondary,
});

/// Drag across a chart to read it.
///
/// Every chart in this app was inert. You could look at a line and see that it
/// went up at some point in the afternoon, and there was no way to ask WHEN, or
/// HOW MUCH — the numbers were in the data and not on the screen, and the only
/// answer was to squint at a 56px axis label. This is the single biggest thing
/// separating these charts from every health app written in the last five years,
/// and it is one widget, because Phase A left one place to put it.
///
/// ## The gesture arena, which is the whole difficulty
///
/// Every one of these charts lives inside a vertically scrolling screen. A
/// `GestureDetector` with `onPanUpdate` claims BOTH axes the moment a drag starts
/// inside it — so a user trying to scroll the page with their thumb on the chart
/// (which is most of the page) would find the page frozen. That is not a subtle
/// regression; it is the app not working.
///
/// `onHorizontalDrag*` enters the arena for horizontal movement only. A vertical
/// drag is left to the `Scrollable` above, a horizontal one comes here, and the
/// arena decides from the first few pixels of movement. It is pinned by a test
/// that drags vertically from inside a chart and asserts the page still scrolled.
class ChartScrubber extends StatefulWidget {
  const ChartScrubber({
    super.key,
    required this.targets,
    required this.accentColor,
    required this.child,
    this.enabled = true,
    this.onScrub,
  });

  /// What the scrub can land on, ordered by [ScrubTarget.xFraction].
  final List<ScrubTarget> targets;

  final Color accentColor;

  /// The plot, unchanged. The scrubber draws over it and never inside it.
  final Widget child;

  final bool enabled;

  /// The landed-on index, or null when the finger lifts.
  final ValueChanged<int?>? onScrub;

  @override
  State<ChartScrubber> createState() => _ChartScrubberState();
}

class _ChartScrubberState extends State<ChartScrubber> {
  int? _index;

  void _land(double dx, double width) {
    if (widget.targets.isEmpty || width <= 0) return;
    final fraction = (dx / width).clamp(0.0, 1.0);

    // Nearest by x, which is what a finger means. Snapping to the nearest SAMPLE
    // rather than reading the curve at the finger's exact x is deliberate: the
    // curve between two samples is an interpolation the app invented, and a
    // tooltip must only ever report a number that was actually measured.
    var best = 0;
    var bestDistance = double.infinity;
    for (var i = 0; i < widget.targets.length; i++) {
      final distance = (widget.targets[i].xFraction - fraction).abs();
      if (distance < bestDistance) {
        bestDistance = distance;
        best = i;
      }
    }
    if (best == _index) return;
    setState(() => _index = best);
    HapticFeedback.selectionClick();
    widget.onScrub?.call(best);
  }

  void _lift() {
    if (_index == null) return;
    setState(() => _index = null);
    widget.onScrub?.call(null);
  }

  @override
  Widget build(BuildContext context) {
    if (!widget.enabled || widget.targets.isEmpty) return widget.child;

    return LayoutBuilder(
      builder: (context, constraints) {
        final width = constraints.maxWidth;
        return GestureDetector(
          behavior: HitTestBehavior.opaque,
          // HORIZONTAL only. See the class doc: onPanUpdate would claim the
          // vertical axis too and freeze the page this chart is sitting on.
          onHorizontalDragStart: (details) =>
              _land(details.localPosition.dx, width),
          onHorizontalDragUpdate: (details) =>
              _land(details.localPosition.dx, width),
          onHorizontalDragEnd: (_) => _lift(),
          onHorizontalDragCancel: _lift,
          child: Stack(
            children: [
              widget.child,
              if (_index case final index?)
                Positioned.fill(
                  child: IgnorePointer(
                    child: CustomPaint(
                      painter: _CrosshairPainter(
                        target: widget.targets[index],
                        accentColor: widget.accentColor,
                        crosshairColor: ChartTokens.read(context).crosshair,
                      ),
                    ),
                  ),
                ),
              if (_index case final index?)
                Positioned.fill(
                  child: IgnorePointer(
                    child: _ScrubTooltip(
                      target: widget.targets[index],
                      accentColor: widget.accentColor,
                    ),
                  ),
                ),
            ],
          ),
        );
      },
    );
  }
}

class _CrosshairPainter extends CustomPainter {
  const _CrosshairPainter({
    required this.target,
    required this.accentColor,
    required this.crosshairColor,
  });

  final ScrubTarget target;
  final Color accentColor;
  final Color crosshairColor;

  @override
  void paint(Canvas canvas, Size size) {
    final x = target.xFraction.clamp(0.0, 1.0) * size.width;
    final y = (1.0 - target.yFraction.clamp(0.0, 1.0)) * size.height;

    canvas.drawLine(
      Offset(x, 0),
      Offset(x, size.height),
      Paint()
        ..color = crosshairColor
        ..strokeWidth = 1,
    );
    // A ring, not a dot: a filled dot in the accent colour is indistinguishable
    // from the sample dots the plot already draws.
    canvas.drawCircle(Offset(x, y), 5, Paint()..color = accentColor);
    canvas.drawCircle(
      Offset(x, y),
      5,
      Paint()
        ..color = Colors.white
        ..style = PaintingStyle.stroke
        ..strokeWidth = 2,
    );
  }

  @override
  bool shouldRepaint(_CrosshairPainter old) =>
      old.target != target || old.accentColor != accentColor;
}

/// The value, floated above the landed sample and kept inside the plot.
class _ScrubTooltip extends StatelessWidget {
  const _ScrubTooltip({required this.target, required this.accentColor});

  final ScrubTarget target;
  final Color accentColor;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final tokens = ChartTokens.read(context);

    return LayoutBuilder(
      builder: (context, constraints) {
        const width = 132.0;
        final x = target.xFraction.clamp(0.0, 1.0) * constraints.maxWidth;
        // Clamped to the plot: a tooltip that hangs off the card is a tooltip you
        // cannot read, and the samples at the very start and end of a day are
        // exactly the ones a user scrubs to first.
        final left = (x - width / 2).clamp(0.0, constraints.maxWidth - width);

        return Stack(
          children: [
            Positioned(
              left: left,
              top: 0,
              width: width,
              child: DecoratedBox(
                decoration: BoxDecoration(
                  color: tokens.tooltipSurface,
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Padding(
                  padding:
                      const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        target.primary,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: theme.textTheme.labelLarge?.copyWith(
                          color: tokens.onTooltipSurface,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      if (target.secondary case final secondary?)
                        Text(
                          secondary,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: theme.textTheme.labelSmall?.copyWith(
                            color: tokens.onTooltipSurface
                                .withValues(alpha: 0.75),
                          ),
                        ),
                    ],
                  ),
                ),
              ),
            ),
          ],
        );
      },
    );
  }
}
