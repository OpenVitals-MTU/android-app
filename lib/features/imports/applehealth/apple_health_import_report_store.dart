/// Persists the last Apple Health import report, ported from the Kotlin
/// `AppleHealthImportReportStore.kt`.
///
/// The Kotlin version writes the report to a file under `filesDir`; the Dart
/// port persists it (and the last failure report) in [SharedPreferences], which
/// the Settings "Data import" screen can read back to show the last result.
library;

import 'package:shared_preferences/shared_preferences.dart';

import 'apple_health_import_error_formatter.dart';

class AppleHealthImportReportStore {
  AppleHealthImportReportStore(this._prefs);

  final SharedPreferences _prefs;

  static const String _reportKey = 'apple_health_import_report';
  static const String _errorReportKey = 'apple_health_import_error_report';

  /// Re-reads the backing store. The background import writes its report from
  /// the foreground-service **isolate**, whose [SharedPreferences] is a separate
  /// in-memory snapshot — without this the UI would keep reading the stale
  /// report it loaded at startup (Kotlin reads the report back from a file path
  /// the worker returns, so it never had this problem).
  Future<void> refresh() => _prefs.reload();

  Future<void> writeReport(String reportText) =>
      _prefs.setString(_reportKey, reportText);

  String readReport() => _prefs.getString(_reportKey) ?? '';

  Future<void> writeFailure(String reportText) =>
      _prefs.setString(_errorReportKey, reportText);

  String readFailure() => _prefs.getString(_errorReportKey) ?? '';
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
