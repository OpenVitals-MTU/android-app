/// Persists the last phone-to-phone sync report to a FILE (not SharedPreferences),
/// mirroring the Apple Health import report store: a sync of a large dataset can
/// produce a long per-type report, and a file has no size ceiling and survives an
/// app restart so the user can still copy or save it later.
library;

import 'dart:io';

import 'package:path_provider/path_provider.dart';

import 'sync_report.dart';

typedef SyncReportDirectoryResolver = Future<Directory> Function();

Future<Directory> _defaultSyncReportDirectory() async {
  final support = await getApplicationSupportDirectory();
  return Directory('${support.path}/device_sync');
}

class DeviceSyncReportStore {
  DeviceSyncReportStore({SyncReportDirectoryResolver? directoryResolver})
      : _directoryResolver = directoryResolver ?? _defaultSyncReportDirectory;

  final SyncReportDirectoryResolver _directoryResolver;

  static const String _reportFileName = 'sync_report.txt';

  Future<void> writeReport(String reportText) async {
    try {
      final dir = await _directoryResolver();
      await dir.create(recursive: true);
      await File('${dir.path}/$_reportFileName')
          .writeAsString(reportText, flush: true);
    } catch (_) {
      // Persisting the report is best-effort — it feeds Copy/Save, not
      // correctness — so an I/O or no-path_provider failure degrades silently.
    }
  }

  Future<String> readReport() async {
    try {
      final dir = await _directoryResolver();
      final file = File('${dir.path}/$_reportFileName');
      return await file.exists() ? await file.readAsString() : '';
    } catch (_) {
      return '';
    }
  }
}

/// Formats a [SyncReport] as the shareable plain-text report shown on the report
/// screen and written to [DeviceSyncReportStore]. [generatedAt] is injected so the
/// output is deterministic in tests.
String buildSyncReportText(SyncReport report, {required DateTime generatedAt}) {
  final buffer = StringBuffer()
    ..writeln('OpenVitals — Sync With Another Phone report')
    ..writeln('Generated: ${generatedAt.toUtc().toIso8601String()}')
    ..writeln('Peer: ${report.peerDeviceName}')
    ..writeln('Status: ${report.completed ? 'completed' : 'aborted'}');
  if (!report.completed && report.abortReason != null) {
    buffer.writeln('Reason: ${report.abortReason}');
  }
  buffer
    ..writeln()
    ..writeln('Summary')
    ..writeln('Sent: ${report.itemsSent}')
    ..writeln('Received: ${report.itemsReceived}')
    ..writeln('Imported: ${report.imported}')
    ..writeln('Already had (skipped): ${report.duplicateSkipped}')
    ..writeln()
    ..writeln('By data type');
  if (report.typeSummaries.isEmpty) {
    buffer.writeln('(none)');
  } else {
    for (final s in report.typeSummaries) {
      buffer.writeln(
        '${s.recordType}: received ${s.received}, imported ${s.imported}, '
        'skipped ${s.duplicateSkipped}',
      );
    }
  }
  return buffer.toString();
}
