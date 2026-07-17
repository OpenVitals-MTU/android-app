/// Persists the last Apple Health import report, ported from the Kotlin
/// `AppleHealthImportReportStore.kt`.
///
/// Written to a FILE under the app-support dir, exactly like Kotlin's `filesDir`
/// report — NOT SharedPreferences. A large import's report reaches tens of
/// megabytes (a stage line per batch, ~47k batches for a 14M-record export), and
/// a value that size is not something SharedPreferences can reliably hold; the
/// prior prefs-backed store silently came back empty, so the finished import had
/// no report to copy or save. A file has no such ceiling and is visible to the
/// UI isolate the moment the foreground-service isolate writes it — no snapshot
/// to reload.
library;

import 'dart:io';

import 'apple_health_import_error_formatter.dart';
import 'apple_health_import_staging_store.dart'
    show AppleHealthImportDirectoryResolver, defaultAppleHealthImportDirectory;

class AppleHealthImportReportStore {
  AppleHealthImportReportStore({
    AppleHealthImportDirectoryResolver? directoryResolver,
  }) : _directoryResolver =
            directoryResolver ?? defaultAppleHealthImportDirectory;

  final AppleHealthImportDirectoryResolver _directoryResolver;

  static const String _reportFileName = 'import_report.txt';
  static const String _errorReportFileName = 'import_error_report.txt';

  Future<File> _file(String name) async {
    final directory = await _directoryResolver();
    return File('${directory.path}/$name');
  }

  Future<void> writeReport(String reportText) async {
    final file = await _file(_reportFileName);
    await file.parent.create(recursive: true);
    await file.writeAsString(reportText, flush: true);
  }

  Future<String> readReport() => _read(_reportFileName);

  Future<void> writeFailure(String reportText) async {
    final file = await _file(_errorReportFileName);
    await file.parent.create(recursive: true);
    await file.writeAsString(reportText, flush: true);
  }

  Future<String> readFailure() => _read(_errorReportFileName);

  Future<String> _read(String name) async {
    // Reading the last report is best-effort — it feeds the Copy/Save actions,
    // not correctness — so a missing file, an I/O error, or an unavailable
    // directory (e.g. no path_provider host under test) degrades to "no report"
    // rather than throwing into the caller.
    try {
      final file = await _file(name);
      return await file.exists() ? await file.readAsString() : '';
    } catch (_) {
      return '';
    }
  }
}

/// The report header lines (Kotlin `appendAppleHealthReportHeader`). The Kotlin
/// version emits the app version from `BuildConfig`; the Dart port does not have
/// one at this layer, so it names the data layer instead.
String appleHealthReportHeader() {
  final buffer = StringBuffer()
    ..writeln('OpenVitals Apple Health Import Report')
    ..writeln('Generated: ${DateTime.now().toUtc().toIso8601String()}')
    ..writeln('App: OpenVitals (Flutter)')
    ..write('Health data layer: package:health');
  return buffer.toString();
}

/// Builds the failure report (Kotlin `AppleHealthImportWorker.buildFailureReportText`).
String buildAppleHealthFailureReportText(
  Object error, {
  List<String> workerLogs = const [],
  StackTrace? stackTrace,
}) {
  final buffer = StringBuffer()
    ..writeln(appleHealthReportHeader())
    ..writeln()
    ..writeln('Summary')
    ..writeln('Status: failed')
    ..writeln('Error: ${AppleHealthImportErrorFormatter.summary(error)}')
    ..writeln()
    ..writeln('Logs');
  if (workerLogs.isEmpty) {
    buffer.writeln('No worker log entries were recorded before failure.');
  } else {
    for (final entry in workerLogs) {
      buffer.writeln(entry);
    }
  }
  buffer
    ..writeln()
    ..writeln('Exception')
    ..writeln(AppleHealthImportErrorFormatter.details(error, stackTrace));
  return buffer.toString();
}
