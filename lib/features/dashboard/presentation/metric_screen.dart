import 'package:flutter/material.dart';

import '../../../ui/components/placeholder_screen.dart';

/// Generic metric detail pushed over the shell (`/metric/:metricId`) for metric
/// ids without a dedicated feature screen (steps, distance, floors, elevation,
/// wheelchair pushes, workout, body energy, …).
// TODO(phase5): replace with the real per-metric detail UI.
class MetricScreen extends StatelessWidget {
  const MetricScreen({super.key, required this.metricId});

  /// The raw `metricId` route argument (a `DashboardWidgetId` storage name), or
  /// null when it could not be decoded.
  final String? metricId;

  @override
  Widget build(BuildContext context) => PlaceholderScreen(
        title: 'Metric',
        subtitle: metricId == null ? 'Unknown metric' : 'Metric: $metricId',
      );
}
