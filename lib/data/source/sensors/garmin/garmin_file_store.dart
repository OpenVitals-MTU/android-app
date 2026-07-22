import 'dart:io';

import 'garmin_log.dart';
import 'garmin_session.dart';

/// Keeps the raw FIT files pulled off a watch.
///
/// **This exists because archiving is destructive from our side.** Telling the
/// watch a file is archived makes it stop offering that file forever; if the
/// importer then mis-reads it — as it demonstrably can — the data cannot be
/// fetched again to re-import, because it is gone from the watch and was never
/// kept. Exactly that happened to a night of sleep during development.
///
/// So the contract is: bytes land on disk BEFORE the archive flag is sent, and a
/// failure to write means the file is not archived and the watch offers it again
/// next time. Gadgetbridge keeps the same guarantee by exporting every download.
///
/// A side benefit is diagnosis: a parser bug can be reproduced against the exact
/// bytes that caused it, with no watch in the loop.
class GarminFileStore {
  const GarminFileStore({
    required this.resolveDirectory,
    this.retention = const Duration(days: 30),
  });

  /// Resolves where files are written, on first use.
  ///
  /// A callback rather than a [Directory] because the app's documents directory
  /// is only available asynchronously, and making the whole provider async to
  /// get it would push a `FutureProvider` through every caller. Injected so a
  /// test can point it at a temp dir.
  final Future<Directory> Function() resolveDirectory;

  /// How long a file is kept before [prune] removes it. FIT files are small
  /// (a night of sleep is under a kilobyte, a day of monitoring a few) but the
  /// watch produces them daily and forever is not a retention policy.
  final Duration retention;

  /// Writes [file] and returns its path.
  ///
  /// Throws on failure — deliberately, because the caller's whole reason to
  /// await this is to decide whether archiving is safe.
  Future<String> save(GarminDownloadedFile file, {required DateTime now}) async {
    final directory = await resolveDirectory();
    await directory.create(recursive: true);
    // Type and index identify it; the timestamp keeps a re-download from
    // clobbering an earlier copy, since several files share file number 65535.
    final name = '${file.entry.type.name}_${file.entry.fileIndex}_'
        '${now.toUtc().millisecondsSinceEpoch}.fit';
    final path = '${directory.path}${Platform.pathSeparator}$name';
    await File(path).writeAsBytes(file.bytes, flush: true);
    garminLog('[GARMIN-STORE] saved $name (${file.bytes.length}B)');
    return path;
  }

  /// Deletes files older than [retention]. Best-effort: housekeeping must never
  /// fail a sync.
  Future<void> prune({required DateTime now}) async {
    final cutoff = now.subtract(retention);
    try {
      final directory = await resolveDirectory();
      if (!directory.existsSync()) return;
      await for (final entity in directory.list()) {
        if (entity is! File || !entity.path.endsWith('.fit')) continue;
        final stat = await entity.stat();
        if (stat.modified.isBefore(cutoff)) {
          await entity.delete();
          garminLog('[GARMIN-STORE] pruned ${entity.path}');
        }
      }
    } catch (error) {
      garminLog('[GARMIN-STORE] prune failed: $error');
    }
  }
}
