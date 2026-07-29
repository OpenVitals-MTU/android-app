/// Staging a generated file in the app cache so another app can be handed it.
///
/// Android hands a file to another app by URI, not by content, so every export
/// that leaves this app — a route as GPX/KMZ, the diagnostics log, an import
/// report — first has to exist as a real file the receiving app can read. That
/// is what this is: `cacheDir/<directoryName>/<fileName>`, the Kotlin
/// `FileProvider` staging area.
///
/// Three features had grown their own copy of this (`route_exports/`,
/// `diagnostics_exports/`, `report_exports/`); they now share this one and
/// differ only in the directory name they pass. The FileProvider half is
/// absorbed by the plugins: share_plus copies whatever it is given into
/// `cacheDir/share_plus/` under its own authority, and open_filex grants
/// through its own — so the Android host declares no `file_paths.xml` of its
/// own for any of these.
library;

import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:path_provider/path_provider.dart';

/// Writes exports into a named subdirectory of the app cache, pruning copies
/// the receiving app has long since read.
class ExportStagingCache {
  const ExportStagingCache({
    required this.directoryName,
    this.temporaryDirectory,
  });

  /// Subdirectory of the cache root this feature stages into.
  final String directoryName;

  /// Test seam for the cache root; defaults to [getTemporaryDirectory].
  final Future<Directory> Function()? temporaryDirectory;

  /// How long a staged copy is worth keeping. The receiving app reads it within
  /// seconds of the sheet closing; after that it is dead weight in the cache.
  static const Duration staleAfter = Duration(hours: 24);

  /// Stages [bytes] as [fileName] and returns the file.
  ///
  /// Throws on any failure — a directory that cannot be created, a write that
  /// fails. Callers catch and surface one message, mirroring Kotlin's
  /// `runCatching`.
  Future<File> stageBytes(String fileName, Uint8List bytes) async {
    final file = File('${(await _directory()).path}/$fileName');
    await file.writeAsBytes(bytes, flush: true);
    return file;
  }

  /// Stages [content] as [fileName] and returns the file.
  Future<File> stageText(String fileName, String content) async {
    final file = File('${(await _directory()).path}/$fileName');
    await file.writeAsString(content, flush: true);
    return file;
  }

  /// Creates the staging directory and prunes it. Every stage goes through
  /// here, so no export path can forget to prune — which is how the
  /// diagnostics log used to accumulate a copy per share, forever.
  Future<Directory> _directory() async {
    final root = await (temporaryDirectory ?? getTemporaryDirectory)();
    final directory = Directory('${root.path}/$directoryName');
    await directory.create(recursive: true);
    pruneStale(directory);
    return directory;
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
        // Locked, already gone, or unreadable — leave it for the next stage.
      }
    }
  }
}
