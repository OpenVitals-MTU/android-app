import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:path_provider/path_provider.dart';

import '../../../domain/model/activity_models.dart';
import 'activity_route_export.dart';

/// Writes a route export into the app cache so another app can be handed a
/// file — the Kotlin `cacheDir/route_exports/` + `FileProvider` staging area.
///
/// Two callers need exactly this: "Open route in map app" (ACTION_VIEW, through
/// open_filex's provider) and the share sheet (ACTION_SEND, through share_plus's
/// own provider). Neither wants the SAF save picker; both want a real path on
/// disk that survives long enough for the receiving app to read it.
///
/// The copies are dead weight once the other app has read them, so every write
/// first prunes anything older than [staleAfter]. The Kotlin original did the
/// same in `File.deleteOldRouteExports`.
class ActivityRouteExportCache {
  const ActivityRouteExportCache({this.temporaryDirectory});

  /// Test seam for the cache root; defaults to [getTemporaryDirectory].
  final Future<Directory> Function()? temporaryDirectory;

  /// Kotlin `RouteExportCacheDirectory`.
  static const String directoryName = 'route_exports';

  /// How long a staged copy is worth keeping.
  static const Duration staleAfter = Duration(hours: 24);

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
    final root = await (temporaryDirectory ?? getTemporaryDirectory)();
    final directory = Directory('${root.path}/$directoryName');
    await directory.create(recursive: true);
    pruneStale(directory);
    final file = File(
      '${directory.path}/${activityRouteExportFileName(workout, format)}',
    );
    await file.writeAsBytes(
      buildActivityRouteExport(workout, points, format),
      flush: true,
    );
    return file;
  }

  /// Deletes staged copies older than [staleAfter].
  ///
  /// Best-effort and never throws: a file the OS is still holding must not stop
  /// the export the user just asked for.
  @visibleForTesting
  void pruneStale(Directory directory) {
    final cutoff = DateTime.now().subtract(staleAfter);
    for (final entry in directory.listSync()) {
      if (entry is! File) continue;
      try {
        if (entry.statSync().modified.isBefore(cutoff)) entry.deleteSync();
      } catch (_) {
        // Locked, already gone, or unreadable — leave it for the next write.
      }
    }
  }
}
