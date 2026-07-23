import 'dart:io';
import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/devices/garmin/garmin_directory.dart';
import 'package:openvitals/devices/garmin/garmin_file_store.dart';
import 'package:openvitals/devices/garmin/garmin_file_types.dart';
import 'package:openvitals/devices/garmin/garmin_session.dart';

GarminDownloadedFile _file({
  int index = 5,
  GarminFileType type = GarminFileType.sleep,
  List<int> bytes = const [1, 2, 3],
}) =>
    GarminDownloadedFile(
      entry: GarminDirectoryEntry(
        fileIndex: index,
        type: type,
        fileNumber: 0xFFFF,
        specificFlags: 0,
        fileFlags: 0,
        fileSize: bytes.length,
        fileDate: null,
      ),
      bytes: Uint8List.fromList(bytes),
    );

void main() {
  late Directory temp;
  late GarminFileStore store;

  setUp(() {
    temp = Directory.systemTemp.createTempSync('garmin_store_test');
    store = GarminFileStore(resolveDirectory: () async => temp);
    addTearDown(() {
      if (temp.existsSync()) temp.deleteSync(recursive: true);
    });
  });

  test('writes the raw bytes, creating the directory', () async {
    final nested = Directory('${temp.path}${Platform.pathSeparator}sub');
    final nestedStore = GarminFileStore(resolveDirectory: () async => nested);

    final path = await nestedStore.save(
      _file(bytes: [9, 8, 7]),
      now: DateTime.utc(2026, 7, 22, 10),
    );

    expect(File(path).readAsBytesSync(), [9, 8, 7]);
  });

  test('names files by type and index, not the 65535 file number', () async {
    final path = await store.save(
      _file(index: 113),
      now: DateTime.utc(2026, 7, 22, 10),
    );

    // Several files share file number 65535, so it identifies nothing.
    expect(path, contains('sleep_113_'));
    expect(path, endsWith('.fit'));
  });

  test('a re-download does not clobber the earlier copy', () async {
    final first = await store.save(_file(), now: DateTime.utc(2026, 7, 22, 10));
    final second = await store.save(_file(), now: DateTime.utc(2026, 7, 22, 11));

    expect(first, isNot(second));
    expect(temp.listSync().whereType<File>(), hasLength(2));
  });

  test('prune removes files past the retention window, keeping recent ones',
      () async {
    final old = File('${temp.path}${Platform.pathSeparator}old.fit')
      ..writeAsBytesSync([1]);
    final recent = File('${temp.path}${Platform.pathSeparator}recent.fit')
      ..writeAsBytesSync([1]);
    final now = DateTime.now();
    old.setLastModifiedSync(now.subtract(const Duration(days: 60)));
    recent.setLastModifiedSync(now.subtract(const Duration(days: 2)));

    await store.prune(now: now);

    expect(old.existsSync(), isFalse);
    expect(recent.existsSync(), isTrue);
  });

  test('prune leaves non-FIT files alone', () async {
    final other = File('${temp.path}${Platform.pathSeparator}notes.txt')
      ..writeAsBytesSync([1]);
    other.setLastModifiedSync(DateTime.now().subtract(const Duration(days: 60)));

    await store.prune(now: DateTime.now());

    expect(other.existsSync(), isTrue);
  });

  test('prune on a directory that does not exist is a no-op', () async {
    final missing = GarminFileStore(
      resolveDirectory: () async =>
          Directory('${temp.path}${Platform.pathSeparator}nope'),
    );
    await expectLater(missing.prune(now: DateTime.now()), completes);
  });
}
