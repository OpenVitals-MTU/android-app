/// Resumable-import checkpointing, ported from the Kotlin
/// `AppleHealthImportCheckpointStore.kt` (1.9.0 `3d6b8dd`).
///
/// A checkpoint is written after every successful batch write, so an import that
/// dies part-way (the Flutter importer runs in the foreground, so it is *more*
/// likely to be killed than the Kotlin WorkManager worker) can be resumed: the
/// user re-picks the same export, the batch writer drops the first
/// [AppleHealthImportCheckpoint.committedSelectedRecords] converted+selected
/// records, and the running imported/duplicate/failed + per-type totals carry
/// over.
///
/// A checkpoint is only ever reused when BOTH the source key (uri|name|size) and
/// the selected-category set match; anything else starts clean.
library;

import 'dart:convert';
import 'dart:io';

import 'apple_health_import_models.dart';
import 'apple_health_import_staging_store.dart';

const String _checkpointFileName = 'checkpoint';
const String _checkpointTempFileName = '$_checkpointFileName.tmp';

/// Per-Apple-type write totals carried across a resume.
class AppleHealthImportCheckpointTypeStats {
  const AppleHealthImportCheckpointTypeStats({
    this.imported = 0,
    this.duplicateSkipped = 0,
    this.failed = 0,
  });

  final int imported;
  final int duplicateSkipped;
  final int failed;

  @override
  bool operator ==(Object other) =>
      other is AppleHealthImportCheckpointTypeStats &&
      other.imported == imported &&
      other.duplicateSkipped == duplicateSkipped &&
      other.failed == failed;

  @override
  int get hashCode => Object.hash(imported, duplicateSkipped, failed);
}

class AppleHealthImportCheckpoint {
  const AppleHealthImportCheckpoint({
    required this.sourceKey,
    required this.selectedCategories,
    this.committedSelectedRecords = 0,
    this.importedRecords = 0,
    this.duplicateSkippedRecords = 0,
    this.failedRecords = 0,
    this.typeStats = const {},
  });

  final String sourceKey;
  final Set<AppleHealthImportCategory> selectedCategories;

  /// How many converted+selected records earlier runs already committed. The
  /// batch writer drops exactly this many records before writing anything.
  final int committedSelectedRecords;
  final int importedRecords;
  final int duplicateSkippedRecords;
  final int failedRecords;
  final Map<String, AppleHealthImportCheckpointTypeStats> typeStats;
}

class AppleHealthImportCheckpointStore {
  AppleHealthImportCheckpointStore({
    AppleHealthImportDirectoryResolver? directory,
  }) : _directory = directory ?? defaultAppleHealthImportDirectory;

  final AppleHealthImportDirectoryResolver _directory;

  /// Returns the stored checkpoint only when it was written for exactly this
  /// [sourceKey] *and* this [selectedCategories] set; otherwise `null` (start
  /// clean).
  Future<AppleHealthImportCheckpoint?> load(
    String sourceKey,
    Set<AppleHealthImportCategory> selectedCategories,
  ) async {
    final file = await _checkpointFile();
    if (!file.existsSync()) return null;
    final Map<String, dynamic> json;
    try {
      final decoded = jsonDecode(await file.readAsString());
      if (decoded is! Map<String, dynamic>) return null;
      json = decoded;
    } catch (_) {
      return null;
    }
    if (json['sourceKey'] != sourceKey) return null;
    final savedCategories = _decodeCategories(json['selectedCategories']);
    if (!_setEquals(savedCategories, selectedCategories)) return null;
    final committed = _int(json['committedSelectedRecords']);
    if (committed == null) return null;
    return AppleHealthImportCheckpoint(
      sourceKey: sourceKey,
      selectedCategories: selectedCategories,
      committedSelectedRecords: committed,
      importedRecords: _int(json['importedRecords']) ?? 0,
      duplicateSkippedRecords: _int(json['duplicateSkippedRecords']) ?? 0,
      failedRecords: _int(json['failedRecords']) ?? 0,
      typeStats: _decodeTypeStats(json['typeStats']),
    );
  }

  /// Atomically (tmp + rename) persists [checkpoint].
  Future<void> save(AppleHealthImportCheckpoint checkpoint) async {
    final directory = await _directory();
    await directory.create(recursive: true);
    final file = File('${directory.path}/$_checkpointFileName');
    final temp = File('${directory.path}/$_checkpointTempFileName');
    await temp.writeAsString(
      jsonEncode(<String, dynamic>{
        'sourceKey': checkpoint.sourceKey,
        'selectedCategories': (checkpoint.selectedCategories
                .map((category) => category.name)
                .toList()
              ..sort())
            .join(','),
        'committedSelectedRecords': checkpoint.committedSelectedRecords,
        'importedRecords': checkpoint.importedRecords,
        'duplicateSkippedRecords': checkpoint.duplicateSkippedRecords,
        'failedRecords': checkpoint.failedRecords,
        'typeStats': checkpoint.typeStats.map(
          (appleType, stats) => MapEntry(appleType, <String, int>{
            'imported': stats.imported,
            'duplicateSkipped': stats.duplicateSkipped,
            'failed': stats.failed,
          }),
        ),
      }),
      flush: true,
    );
    try {
      await temp.rename(file.path);
    } catch (_) {
      await temp.copy(file.path);
      try {
        await temp.delete();
      } catch (_) {
        // Best effort.
      }
    }
  }

  Future<void> clear() async {
    try {
      final file = await _checkpointFile();
      if (file.existsSync()) await file.delete();
      final directory = await _directory();
      final temp = File('${directory.path}/$_checkpointTempFileName');
      if (temp.existsSync()) await temp.delete();
    } catch (_) {
      // Best effort; a stale checkpoint is rejected by its source key anyway.
    }
  }

  Future<File> _checkpointFile() async =>
      File('${(await _directory()).path}/$_checkpointFileName');
}

Set<AppleHealthImportCategory> _decodeCategories(Object? value) {
  if (value is! String || value.isEmpty) return const {};
  final result = <AppleHealthImportCategory>{};
  for (final name in value.split(',')) {
    if (name.isEmpty) continue;
    for (final category in AppleHealthImportCategory.values) {
      if (category.name == name) result.add(category);
    }
  }
  return result;
}

Map<String, AppleHealthImportCheckpointTypeStats> _decodeTypeStats(
  Object? value,
) {
  if (value is! Map) return const {};
  final result = <String, AppleHealthImportCheckpointTypeStats>{};
  value.forEach((appleType, stats) {
    if (stats is! Map) return;
    result['$appleType'] = AppleHealthImportCheckpointTypeStats(
      imported: _int(stats['imported']) ?? 0,
      duplicateSkipped: _int(stats['duplicateSkipped']) ?? 0,
      failed: _int(stats['failed']) ?? 0,
    );
  });
  return result;
}

int? _int(Object? value) {
  final parsed = value is int ? value : int.tryParse('$value');
  if (parsed == null) return null;
  return parsed < 0 ? 0 : parsed;
}

bool _setEquals(
  Set<AppleHealthImportCategory> a,
  Set<AppleHealthImportCategory> b,
) =>
    a.length == b.length && a.containsAll(b);
