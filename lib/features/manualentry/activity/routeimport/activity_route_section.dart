import 'package:flutter/material.dart';

import '../../../../core/presentation/unit_formatter.dart';
import '../../../../domain/model/activity_models.dart';
import '../../../../l10n/app_localizations.dart';
import '../../../../ui/components/ov_surface.dart';
import '../../../activity/maps/route_map_view.dart';
import '../activity_entry_state.dart';
import 'route_file_parser.dart';

/// Port of the Kotlin `routeimport/ActivityRouteSection.kt`.

/// Kotlin `RouteAverageMetrics`.
class RouteAverageMetrics {
  const RouteAverageMetrics({
    required this.averagePace,
    required this.averageSpeed,
  });

  final String averagePace;
  final String averageSpeed;
}

/// Kotlin `routeMovingDurationMs`: the route's wall-clock span minus every
/// recorded pause, floored at zero and never exceeding the span itself.
int routeMovingDurationMs(
  RouteFileImport route,
  List<ActivityPauseInterval> pauseIntervals,
) {
  final durationMs = _atLeastZero(
    route.endTime.difference(route.startTime).inMilliseconds,
  );
  var pausedMs = 0;
  for (final interval in pauseIntervals) {
    pausedMs += _atLeastZero(
      interval.endTime.difference(interval.startTime).inMilliseconds,
    );
  }
  if (pausedMs > durationMs) pausedMs = durationMs;
  return _atLeastZero(durationMs - pausedMs);
}

int _atLeastZero(int value) => value < 0 ? 0 : value;

/// Kotlin `routeAverageMetrics`. Null when the route has no moving time or the
/// formatter cannot express a pace (a zero-distance route).
RouteAverageMetrics? routeAverageMetrics({
  required RouteFileImport route,
  required List<ActivityPauseInterval> pauseIntervals,
  required UnitFormatter unitFormatter,
}) {
  final movingDurationMs = routeMovingDurationMs(route, pauseIntervals);
  if (movingDurationMs <= 0) return null;
  final pace = unitFormatter.averagePace(route.distanceMeters, movingDurationMs);
  if (pace == null) return null;
  final speed = unitFormatter.averageSpeed(route.distanceMeters, movingDurationMs);
  return RouteAverageMetrics(
    averagePace: pace.text,
    averageSpeed: speed.text,
  );
}

/// Kotlin `ImportedActivityRouteSection`: the map preview, the route summary
/// line, and — when the route carries timestamps — its average pace and speed.
class ImportedActivityRouteSection extends StatelessWidget {
  const ImportedActivityRouteSection({
    super.key,
    required this.state,
    required this.unitFormatter,
    required this.onClearRoute,
  });

  final ActivityEntryUiState state;
  final UnitFormatter unitFormatter;
  final VoidCallback onClearRoute;

  @override
  Widget build(BuildContext context) {
    final route = state.importedRoute;
    if (route == null || route.points.isEmpty) return const SizedBox.shrink();

    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    final averageMetrics = routeAverageMetrics(
      route: route,
      pauseIntervals: state.recordedPauseIntervals,
      unitFormatter: unitFormatter,
    );
    final distance = unitFormatter.distance(route.distanceMeters);
    final elevation = unitFormatter.elevation(route.elevationGainedMeters);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      spacing: 8,
      children: [
        Row(
          children: [
            Expanded(
              child: Text(
                l10n.activityEntryImportedRoute,
                style: theme.textTheme.labelMedium
                    ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
              ),
            ),
            OutlinedButton(
              onPressed: state.isSavingEntry ? null : onClearRoute,
              child: const Icon(Icons.delete_outline, size: 18),
            ),
          ],
        ),
        OpenVitalsSurface(
          style: OpenVitalsSurfaceStyle.metric,
          contentPadding: const EdgeInsets.all(12),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            spacing: 8,
            children: [
              RouteMapView(points: route.points, height: 160),
              Text(
                l10n.activityEntryRouteSummary(
                  route.name ?? route.fileName ?? l10n.activityEntryImportedRoute,
                  distance.text,
                  elevation.text,
                  route.points.length,
                ),
                style: theme.textTheme.bodySmall
                    ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
              ),
              if (averageMetrics != null)
                Text(
                  l10n.activityEntryRouteAverageMetrics(
                    averageMetrics.averagePace,
                    averageMetrics.averageSpeed,
                  ),
                  style: theme.textTheme.bodySmall
                      ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                ),
            ],
          ),
        ),
      ],
    );
  }
}
