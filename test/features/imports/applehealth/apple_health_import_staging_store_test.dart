import 'dart:io';
import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/features/imports/applehealth/apple_health_import_staging_store.dart';

void main() {
  late Directory root;
  late Directory importDirectory;

  setUp(() {
    root = Directory.systemTemp.createTempSync('apple_health_staging_test');
    importDirectory = Directory('${root.path}/apple_health_import');
  });

  tearDown(() {
    if (root.existsSync()) root.deleteSync(recursive: true);
  });

  AppleHealthImportStagingStore store() => AppleHealthImportStagingStore(
        directory: () async => importDirectory,
      );

  File sourceFile(List<int> bytes, {String name = 'export.zip'}) =>
      File('${root.path}/$name')..writeAsBytesSync(bytes);

  AppleHealthExportSource source(
    File file, {
    int? reportedSize,
    String displayName = 'export.zip',
  }) =>
      AppleHealthExportSource(
        sourceId: 'content://downloads/${file.path}',
        fingerprint: AppleHealthExportFingerprint(
          displayName: displayName,
          size: reportedSize ?? file.lengthSync(),
        ),
        openRead: file.openRead,
      );

  group('AppleHealthImportStagingStore', () {
    test('stages the picked export and verifies the copied byte count',
        () async {
      final bytes = Uint8List.fromList(List.generate(4096, (i) => i % 256));
      final staged = await store().stage(source(sourceFile(bytes)));

      expect(staged.reused, isFalse);
      expect(staged.bytes, bytes.length);
      expect(staged.file.path, endsWith('/staged_export.bin'));
      expect(staged.file.readAsBytesSync(), bytes);
      // The interrupted-copy temp file must never survive a successful stage.
      expect(File('${importDirectory.path}/staged_export.bin.tmp').existsSync(),
          isFalse);
    });

    test('throws AppleHealthExportCopyException on a short provider copy',
        () async {
      final bytes = Uint8List.fromList(List.filled(1024, 7));
      // The picker claims more bytes than the stream actually yields — the
      // "ZIP is still syncing from iCloud" failure.
      final short = source(sourceFile(bytes), reportedSize: bytes.length + 512);

      await expectLater(
        store().stage(short),
        throwsA(
          isA<AppleHealthExportCopyException>()
              .having((e) => e.expectedBytes, 'expectedBytes', 1536)
              .having((e) => e.copiedBytes, 'copiedBytes', 1024)
              .having(
                (e) => e.message,
                'message',
                contains('Download the ZIP fully to local storage'),
              ),
        ),
      );

      // Nothing usable may be left behind: a retry must not reuse a bad copy.
      expect(File('${importDirectory.path}/staged_export.bin').existsSync(),
          isFalse);
      expect(File('${importDirectory.path}/staged_export.bin.tmp').existsSync(),
          isFalse);
    });

    test('reuses an existing staged copy when the fingerprint matches',
        () async {
      final file = sourceFile(Uint8List.fromList(List.filled(2048, 3)));
      final first = await store().stage(source(file));
      expect(first.reused, isFalse);

      final second = await store().stage(source(file));
      expect(second.reused, isTrue);
      expect(second.bytes, 2048);
    });

    test('re-stages when the fingerprint no longer matches', () async {
      final file = sourceFile(Uint8List.fromList(List.filled(2048, 3)));
      await store().stage(source(file));

      // Same path, different size/name: a different export was picked.
      final other = sourceFile(
        Uint8List.fromList(List.filled(4096, 9)),
        name: 'other.zip',
      );
      final restaged = await store().stage(
        source(other, displayName: 'other.zip'),
      );

      expect(restaged.reused, isFalse);
      expect(restaged.bytes, 4096);
      expect(restaged.file.readAsBytesSync().first, 9);
    });

    test('clear removes the staged export, metadata, leftover tmp and the dir',
        () async {
      final file = sourceFile(Uint8List.fromList(List.filled(64, 1)));
      final staged = await store().stage(source(file));
      final leftoverTemp = File('${importDirectory.path}/staged_export.bin.tmp')
        ..writeAsStringSync('interrupted copy');

      final cleared = await store().clear();

      expect(cleared, isTrue);
      expect(staged.file.existsSync(), isFalse);
      expect(leftoverTemp.existsSync(), isFalse);
      expect(File('${importDirectory.path}/staged_export.properties').existsSync(),
          isFalse);
      expect(importDirectory.existsSync(), isFalse);
      // The user's own file is never touched.
      expect(file.existsSync(), isTrue);
    });

    test('source key is uri|displayName|size', () {
      final key = appleHealthImportSourceKey(
        'content://downloads/1',
        const AppleHealthExportFingerprint(displayName: 'export.zip', size: 42),
      );
      expect(key, 'content://downloads/1|export.zip|42');
    });
  });
}
