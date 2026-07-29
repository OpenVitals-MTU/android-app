import 'dart:io';

import '../../../core/export/export_staging.dart';
import '../../../domain/model/activity_models.dart';
import 'activity_route_export.dart';

/// Writes a route export into the app cache so another app can be handed a
/// file — the Kotlin `cacheDir/route_exports/` + `FileProvider` staging area.
///
/// Two callers need exactly this: "Open route in map app" (ACTION_VIEW, through
/// open_filex's provider) and the share sheet (ACTION_SEND, through share_plus's
/// own provider). Neither wants the save picker; both want a real path on disk
/// that survives long enough for the receiving app to read it.
///
/// What is left here after the staging mechanics moved to [ExportStagingCache]
/// (shared with the diagnostics log and the import reports) is the route-shaped
/// half: building the bytes for a format, and naming the file after the
/// activity. The Kotlin original's `File.deleteOldRouteExports` is now the
/// shared cache's pruning, which every stage runs.
class ActivityRouteExportCache {
  const ActivityRouteExportCache({ExportStagingCache? cache})
      : cache = cache ?? const ExportStagingCache(directoryName: directoryName);

  final ExportStagingCache cache;

  /// Kotlin `RouteExportCacheDirectory`.
  static const String directoryName = 'route_exports';

  /// Stages [workout]'s route as [format] and returns the file.
  ///
  /// Throws on any failure — an empty route ([sortedRoutePointsForExport]), a
  /// directory that cannot be created, a write that fails. Callers catch and
  /// surface one message, mirroring Kotlin's `runCatching`.
  Future<File> write(
    ExerciseData workout,
    ActivityRouteExportFormat format,
  ) async {
    final points = sortedRoutePointsForExport(workout);
    return cache.stageBytes(
      activityRouteExportFileName(workout, format),
      buildActivityRouteExport(workout, points, format),
    );
  }
}
