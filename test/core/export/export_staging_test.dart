import 'dart:io';
import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/core/export/export_staging.dart';

/// The staging area every export passes through on its way to another app.
///
/// Route exports, the diagnostics log and the import reports each used to carry
/// their own copy of this; the pruning test in particular only existed on the
/// route side, which is how the diagnostics log came to leak a copy per share.
void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late Directory tempRoot;

  setUp(() {
    tempRoot = Directory.systemTemp.createTempSync('export_staging_test');
  });

  tearDown(() => tempRoot.deleteSync(recursive: true));

  ExportStagingCache cache([String directoryName = 'test_exports']) =>
      ExportStagingCache(
        directoryName: directoryName,
        temporaryDirectory: () async => tempRoot,
      );

  test('a staged file lands under the feature directory, named as asked',
      () async {
    final file = await cache('route_exports').stageText('morning-run.gpx', 'x');

    expect(file.path, endsWith('/route_exports/morning-run.gpx'));
    expect(file.existsSync(), isTrue,
        reason: 'the receiving app is handed a path, so it must exist');
  });

  test('two features staging the same name do not collide', () async {
    // The directory name is the only thing separating them, so this is the
    // property that keeps a report from overwriting a route export.
    final a = await cache('report_exports').stageText('export.txt', 'report');
    final b = await cache('diagnostics_exports').stageText('export.txt', 'log');

    expect(a.path, isNot(b.path));
    expect(a.readAsStringSync(), 'report');
    expect(b.readAsStringSync(), 'log');
  });

  test('bytes are staged verbatim, not decoded', () async {
    // A KMZ is a zip; anything that round-tripped it through a string would
    // corrupt it.
    final bytes = Uint8List.fromList([0x50, 0x4b, 0x03, 0x04, 0x00, 0xff]);

    final file = await cache().stageBytes('route.kmz', bytes);

    expect(file.readAsBytesSync(), bytes);
  });

  test('staging prunes copies older than a day and keeps fresh ones', () async {
    final directory = Directory('${tempRoot.path}/test_exports')
      ..createSync(recursive: true);
    final stale = File('${directory.path}/stale.txt')..writeAsStringSync('x');
    final fresh = File('${directory.path}/fresh.txt')..writeAsStringSync('x');
    stale.setLastModifiedSync(
      DateTime.now().subtract(const Duration(hours: 25)),
    );

    await cache().stageText('new.txt', 'x');

    expect(stale.existsSync(), isFalse);
    expect(fresh.existsSync(), isTrue);
  });

  test('pruning runs for byte exports too, not only text', () async {
    // The two entry points must not diverge: a leak on either is a leak.
    final directory = Directory('${tempRoot.path}/test_exports')
      ..createSync(recursive: true);
    final stale = File('${directory.path}/stale.gpx')..writeAsStringSync('x');
    stale.setLastModifiedSync(
      DateTime.now().subtract(const Duration(hours: 25)),
    );

    await cache().stageBytes('new.gpx', Uint8List.fromList([1, 2, 3]));

    expect(stale.existsSync(), isFalse);
  });

  test('re-staging the same name overwrites rather than stacking up', () async {
    await cache().stageText('report.txt', 'first');
    await cache().stageText('report.txt', 'second');

    final directory = Directory('${tempRoot.path}/test_exports');
    expect(directory.listSync().whereType<File>(), hasLength(1));
    expect(
      File('${directory.path}/report.txt').readAsStringSync(),
      'second',
    );
  });

  test('a locked or vanished file does not abort the export', () async {
    // Best-effort pruning: the user asked for an export, not for cache hygiene.
    final directory = Directory('${tempRoot.path}/test_exports')
      ..createSync(recursive: true);
    Directory('${directory.path}/a-subdirectory').createSync();

    await expectLater(cache().stageText('report.txt', 'x'), completes);
  });
}
