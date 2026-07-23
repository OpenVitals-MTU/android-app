import 'package:flutter/material.dart';

import '../theme/chart_tokens.dart';

/// A labelled proportional bar: this much of that.
///
/// The app had NINE of these, and no two agreed. They ran 3, 6, 6, 6, 8, 8, 10,
/// 10 and 18 pixels tall, with corner radii of 3, 3, 3, 5, 6, 8 and 9 — the sleep
/// stage shares, the caffeine sources, the caffeine categories, the caffeine time
/// buckets, the hydration goal, the hydration drink breakdown, the split pace
/// bars, and two in achievements. Seven of them were a `LinearProgressIndicator`,
/// which is a Material *progress* control pressed into service as a chart: a fixed
/// track, an animation controller nobody wanted, and no way to say "this bar is a
/// measurement, not a download".
///
/// One bar. Every caller keeps its own height and colour; nobody keeps their own
/// idea of what a bar IS.
enum ChartBarRowLayout {
  /// Label and value above the bar, bar full width beneath. The distribution
  /// lists (caffeine sources, hydration drinks, achievements).
  stacked,

  /// Label | bar | value, all on one line. The sleep stage shares.
  inline,
}

class ChartBarRow extends StatelessWidget {
  const ChartBarRow({
    super.key,
    required this.fraction,
    required this.color,
    this.label,
    this.trailing,
    this.layout = ChartBarRowLayout.stacked,
    this.labelWidth,
    this.gap = 8,
    this.height = kChartBarRowHeight,
    this.radius,
    this.trackColor,
    this.minFraction = 0,
    this.labelStyle,
    this.trailingStyle,
  });

  /// `[0, 1]`. Clamped, because a value can exceed its target — you can drink two
  /// litres against a 1.5-litre goal — and a bar that ran past its track would
  /// paint outside the card.
  final double fraction;

  final Color color;

  /// Null for a bare bar (the pace bars label themselves in the row around it).
  final Widget? label;

  /// The value, at the end of the row (`inline`) or above the bar (`stacked`).
  final Widget? trailing;

  final ChartBarRowLayout layout;

  /// The fixed leading column an `inline` row gives its label, so that a column
  /// of bars starts at the same x and can be compared by eye — which is the only
  /// reason to put bars in a column.
  final double? labelWidth;

  /// Between an `inline` bar and its trailing value. A caller's own business: the
  /// sleep stage rows breathe at 12, and moving them to 8 would shift every bar
  /// end and every duration on the card.
  final double gap;

  final double height;

  /// Defaults to a pill. A bar this thin has no other honest corner.
  final double? radius;

  /// Defaults to [ChartTokens.track].
  final Color? trackColor;

  /// A floor under the drawn width, for a bar whose smallest real value must
  /// still be visible as a bar. The split pace bars use it: the fastest split in
  /// a session is a quarter-width bar, not an invisible one.
  final double minFraction;

  final TextStyle? labelStyle;
  final TextStyle? trailingStyle;

  @override
  Widget build(BuildContext context) {
    final tokens = ChartTokens.read(context);
    final effective = fraction.isFinite ? fraction.clamp(0.0, 1.0) : 0.0;
    final drawn = effective <= 0
        // Zero is drawn as zero. A bar with nothing in it and a bar with a little
        // in it are different claims, and minFraction must not blur them.
        ? 0.0
        : (effective < minFraction ? minFraction : effective);

    final bar = ClipRRect(
      borderRadius: BorderRadius.circular(radius ?? height / 2),
      child: SizedBox(
        // Full width explicitly: a `stacked` row sits in a Column, which hands its
        // children LOOSE width constraints, and under those the track — a
        // DecoratedBox — would size itself to its only child, the
        // FractionallySizedBox, i.e. to the FILLED part. The unfilled remainder of
        // the bar would simply not be painted, and every bar would read as 100%.
        width: double.infinity,
        height: height,
        child: DecoratedBox(
          decoration: BoxDecoration(color: trackColor ?? tokens.track),
          // centerLeft, because FractionallySizedBox centres by default: the bar
          // has to grow from the left edge, as Kotlin's
          // `fillMaxWidth(fraction).fillMaxHeight()` Box did.
          child: FractionallySizedBox(
            alignment: Alignment.centerLeft,
            widthFactor: drawn,
            child: DecoratedBox(decoration: BoxDecoration(color: color)),
          ),
        ),
      ),
    );

    // The inline layout budgets a fixed label column plus a natural-width
    // trailing value, sized for 1.0 text — at large font scales the two eat
    // the whole row and the Row overflows. The stacked layout was built for
    // exactly that shortage of horizontal room, so large-font users get it
    // automatically: full text on its own line, bars still full-width and
    // aligned. 1.4 is where the inline budget stops fitting a phone width.
    final scale = MediaQuery.textScalerOf(context).scale(14) / 14;
    final effectiveLayout =
        scale >= 1.4 ? ChartBarRowLayout.stacked : layout;

    return switch (effectiveLayout) {
      ChartBarRowLayout.inline => Row(
          children: [
            if (label case final label?)
              SizedBox(
                width: labelWidth,
                child: DefaultTextStyle.merge(style: labelStyle, child: label),
              ),
            Expanded(child: bar),
            if (trailing case final trailing?) ...[
              SizedBox(width: gap),
              DefaultTextStyle.merge(style: trailingStyle, child: trailing),
            ],
          ],
        ),
      ChartBarRowLayout.stacked => Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (label != null || trailing != null) ...[
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  if (label case final label?)
                    Expanded(
                      child:
                          DefaultTextStyle.merge(style: labelStyle, child: label),
                    ),
                  if (trailing case final trailing?)
                    DefaultTextStyle.merge(
                      style: trailingStyle,
                      child: trailing,
                    ),
                ],
              ),
              const SizedBox(height: 4),
            ],
            bar,
          ],
        ),
    };
  }
}
