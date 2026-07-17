import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/features/imports/applehealth/apple_health_import_error_formatter.dart';
import 'package:openvitals/features/imports/applehealth/apple_health_import_report_store.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  group('AppleHealthImportErrorFormatter', () {
    test('summary includes the exception type when a message is missing', () {
      expect(
        AppleHealthImportErrorFormatter.summary(AppleHealthImportException(null)),
        'AppleHealthImportException',
      );
    });

    test('details includes the exception message and its cause chain', () {
      final details = AppleHealthImportErrorFormatter.details(
        AppleHealthImportException(
          'Bad export zip',
          cause: AppleHealthImportException('Missing export.xml'),
        ),
      );

      expect(details, contains('AppleHealthImportException: Bad export zip'));
      expect(
        details,
        contains('Caused by: AppleHealthImportException: Missing export.xml'),
      );
    });

    test('isPermissionDenied is true for a direct permission exception', () {
      expect(
        AppleHealthImportErrorFormatter.isPermissionDenied(
          AppleHealthImportPermissionException('Permission Denial'),
        ),
        isTrue,
      );
    });

    test('isPermissionDenied is true when the permission exception is a wrapped '
        'cause', () {
      final error = AppleHealthImportException(
        'Import failed',
        cause: AppleHealthImportPermissionException('Permission Denial'),
      );

      expect(AppleHealthImportErrorFormatter.isPermissionDenied(error), isTrue);
    });

    test('isPermissionDenied is false for unrelated errors', () {
      expect(
        AppleHealthImportErrorFormatter.isPermissionDenied(
          AppleHealthImportException('Bad export zip'),
        ),
        isFalse,
      );
    });
  });

  group('AppleHealthImportReportStore', () {
    test('failure report includes summary, logs, and the full exception chain',
        () {
      final report = buildAppleHealthFailureReportText(
        AppleHealthImportException(
          'Top level failure',
          cause: AppleHealthImportException('Root cause'),
        ),
        workerLogs: ['2026-01-01T08:00:00Z [WORKER] test log'],
      );

      expect(report, contains('Summary'));
      expect(report, contains('Logs'));
      expect(report, contains('2026-01-01T08:00:00Z [WORKER] test log'));
      expect(report, contains('Exception'));
      expect(report, contains('AppleHealthImportException: Top level failure'));
      expect(report, contains('Caused by: AppleHealthImportException: Root cause'));
    });

    test('round-trips the last report and failure via its file store',
        () async {
      final dir = await Directory.systemTemp.createTemp('ah_report_fmt_test');
      addTearDown(() => dir.delete(recursive: true));
      final store =
          AppleHealthImportReportStore(directoryResolver: () async => dir);

      expect(await store.readReport(), '');
      expect(await store.readFailure(), '');
      await store.writeReport('hello report');
      await store.writeFailure('boom');
      expect(await store.readReport(), 'hello report');
      expect(await store.readFailure(), 'boom');
    });
  });
}
