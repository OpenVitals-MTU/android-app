import 'package:flutter/widgets.dart';

import '../../core/time/local_date.dart';

/// Provided by the metric-detail scaffold so a month heatmap cell — buried deep
/// inside a screen's content — can drill into that day's Day view without every
/// screen threading a navigation callback down through its charts.
///
/// When present, a tapped month cell opens the day; when absent (isolated widget
/// tests, or a chart used outside the scaffold) the cell falls back to whatever
/// `onDateSelected` the host passed.
class MetricDetailDayOpener extends InheritedWidget {
  const MetricDetailDayOpener({
    super.key,
    required this.openDay,
    required super.child,
  });

  final ValueChanged<LocalDate> openDay;

  static ValueChanged<LocalDate>? maybeOf(BuildContext context) => context
      .dependOnInheritedWidgetOfExactType<MetricDetailDayOpener>()
      ?.openDay;

  @override
  bool updateShouldNotify(MetricDetailDayOpener oldWidget) =>
      oldWidget.openDay != openDay;
}
