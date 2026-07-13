import 'package:flutter/material.dart';

/// What a chart says when there is nothing to draw.
///
/// The app had four answers. `MetricDayChart` wrote a muted sentence. The caffeine
/// curve wrote a muted sentence of its own, in its own file. `MetricSessionChart`
/// and `MetricLineChart` returned `SizedBox.shrink()` — they do not say "no data",
/// they VANISH, and the card around them vanishes with them, so the screen silently
/// reflows and you are left to notice that a thing you were looking for is not
/// there.
///
/// This is the one, and in this commit it renders exactly what the sentence-writers
/// already rendered — same style, same colour, same words — so nothing moves. It
/// exists to be a single place to change, because "give the charts a real empty
/// state" is a job that must be done once and is otherwise done four times and
/// badly.
///
/// The vanishing charts are NOT converted here. Turning "absent" into "an empty
/// card" changes the layout of the heart, vitals and activity screens — a card
/// appears where there was none — and that is a user-visible behaviour change, not
/// a refactor. It gets its own commit, with its own screen tests.
class ChartEmptyState extends StatelessWidget {
  const ChartEmptyState({
    super.key,
    required this.message,
    this.height,
  });

  final String message;

  /// Reserve the chart's footprint, so a card that resolves from empty to full
  /// does not make the page jump. Null keeps the current behaviour: the text takes
  /// only the room it needs.
  final double? height;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final text = Text(
      message,
      style: theme.textTheme.bodyMedium
          ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
    );
    if (height == null) return text;
    return SizedBox(
      height: height,
      width: double.infinity,
      child: Center(child: text),
    );
  }
}
