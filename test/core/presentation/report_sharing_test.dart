import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:share_plus/share_plus.dart';

import 'package:openvitals/core/export/export_staging.dart';
import 'package:openvitals/core/presentation/report_sharing.dart';

/// Sending an import report to another app — a WhatsApp or Signal message, an
/// email attachment. What matters here is that a real, named, readable file
/// reaches the share sheet: the receiving app is handed a path, not the text.

/// Captures what would have gone to `Intent.createChooser(ACTION_SEND)`.
class _RecordingShareSheet {
  _RecordingShareSheet({this.throwOnShare = false});

  final bool throwOnShare;
  final List<ShareParams> shared = [];

  Future<void> call(ShareParams params) async {
    if (throwOnShare) throw StateError('no share target');
    shared.add(params);
  }
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late Directory tempRoot;
  late _RecordingShareSheet sheet;

  setUp(() {
    tempRoot = Directory.systemTemp.createTempSync('report_share_test');
    sheet = _RecordingShareSheet();
  });

  tearDown(() => tempRoot.deleteSync(recursive: true));

  TextReportSharing sharing([_RecordingShareSheet? override]) =>
      TextReportSharing(
        cache: ExportStagingCache(
          directoryName: TextReportSharing.directoryName,
          temporaryDirectory: () async => tempRoot,
        ),
        share: (override ?? sheet).call,
      );

  group('TextReportSharing', () {
    test('the sheet is handed a real file holding the report', () async {
      await sharing().shareReport(
        content: 'OpenVitals CSV import report\nRecords written: 3\n',
        fileName: 'openvitals-csv-import-report.txt',
        chooserTitle: 'Share import report',
      );

      final file = sheet.shared.single.files!.single;
      expect(File(file.path).existsSync(), isTrue,
          reason: 'the receiving app is handed a path, so it must exist');
      expect(
        File(file.path).readAsStringSync(),
        contains('Records written: 3'),
      );
    });

    test('the attachment keeps the report file name and a text mime type',
        () async {
      // Both are what the recipient sees in their messenger: a named .txt they
      // can open, not an "unknown file".
      await sharing().shareReport(
        content: 'report',
        fileName: 'openvitals-csv-import-report.txt',
        chooserTitle: 'Share import report',
      );

      final file = sheet.shared.single.files!.single;
      expect(file.path, endsWith('/openvitals-csv-import-report.txt'));
      expect(file.mimeType, 'text/plain');
    });

    test('the chooser title and the email subject reach the share sheet',
        () async {
      await sharing().shareReport(
        content: 'report',
        fileName: 'openvitals-csv-import-report.txt',
        chooserTitle: 'Share import report',
      );

      final params = sheet.shared.single;
      expect(params.title, 'Share import report');
      expect(params.subject, 'openvitals-csv-import-report.txt');
    });

    test('the report goes as a file, never as inline share text', () async {
      // An import report runs to hundreds of lines; pasted into a composer as
      // ShareParams.text it would be one unsendable message.
      await sharing().shareReport(
        content: 'line\n' * 500,
        fileName: 'openvitals-csv-import-report.txt',
        chooserTitle: 'Share import report',
      );

      expect(sheet.shared.single.text, isNull);
      expect(sheet.shared.single.files, hasLength(1));
    });

    test('a share sheet with no target surfaces as a failure to the caller',
        () async {
      final failing = _RecordingShareSheet(throwOnShare: true);

      await expectLater(
        sharing(failing).shareReport(
          content: 'report',
          fileName: 'openvitals-csv-import-report.txt',
          chooserTitle: 'Share import report',
        ),
        throwsStateError,
      );
    });

    test('the report is staged in its own directory, not another export\'s',
        () async {
      // Staging, pruning and overwrite semantics belong to ExportStagingCache
      // and are tested there; what is this file's business is that the report
      // is wired onto it with its own directory name.
      await sharing().shareReport(
        content: 'report',
        fileName: 'openvitals-csv-import-report.txt',
        chooserTitle: 'Share import report',
      );

      expect(
        sheet.shared.single.files!.single.path,
        contains('/${TextReportSharing.directoryName}/'),
      );
    });
  });
}
