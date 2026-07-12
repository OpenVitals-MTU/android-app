import 'dart:io';
import 'dart:typed_data';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/data/source/imports/route_folder_source.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/features/imports/application/route_bulk_import_view_model.dart';
import 'package:openvitals/features/manualentry/activity/activity_entry_providers.dart';
import 'package:openvitals/features/settings/application/fit_import_view_model.dart';

import '../imports/route_bulk_import_view_model_test.dart'
    show FakeActivityRepository, FakeRouteFileImporter;

/// "Import a folder of FIT files": pick a folder once, import everything under
/// it.
///
/// The folder arrives as a SAF tree the native side walks — this is the Dart
/// half, so the source is faked and what is pinned here is the decision-making:
/// what a cancelled pick does, what an empty folder does, and above all that the
/// files are opened ONE AT A TIME rather than read into memory as a batch.
class _FakeFolderSource implements RouteFolderSource {
  _FakeFolderSource({this.pick, this.pickError, this.unreadable = const {}});

  final RouteFolderPick? pick;
  final Object? pickError;

  /// URIs whose read throws — the file moved between the scan and its turn.
  final Set<String> unreadable;

  final List<String> reads = [];
  List<String>? requestedExtensions;

  @override
  Future<RouteFolderPick?> pickFolder({required List<String> extensions}) async {
    requestedExtensions = extensions;
    final error = pickError;
    if (error != null) throw error;
    return pick;
  }

  @override
  Future<Uint8List> readFile(String uri) async {
    reads.add(uri);
    if (unreadable.contains(uri)) {
      throw const FileSystemException('the file moved');
    }
    return Uint8List.fromList([1, 2, 3]);
  }
}

RouteFolderPick _folder(List<String> names, {bool truncated = false}) => (
      files: [for (final name in names) (uri: 'content://tree/$name', name: name)],
      truncated: truncated,
    );

Future<ProviderContainer> _container({
  required _FakeFolderSource folder,
  FakeActivityRepository? repository,
}) async {
  SharedPreferences.setMockInitialValues(const <String, Object>{});
  final prefs = await SharedPreferences.getInstance();
  final container = ProviderContainer(
    overrides: [
      sharedPreferencesProvider.overrideWithValue(prefs),
      activityRepositoryProvider
          .overrideWithValue(repository ?? FakeActivityRepository()),
      routeFileImporterProvider.overrideWithValue(FakeRouteFileImporter()),
      routeFolderSourceProvider.overrideWithValue(folder),
    ],
  );
  addTearDown(container.dispose);
  return container;
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  test('imports every FIT file the folder held', () async {
    final folder = _FakeFolderSource(pick: _folder(['a.fit', 'b.fit', 'c.fit']));
    final repository = FakeActivityRepository();
    final container = await _container(folder: folder, repository: repository);

    await container.read(fitImportCardProvider.notifier).importFolder();

    expect(folder.requestedExtensions, ['fit']);
    expect(repository.writes.length, 3);
    expect(
      container.read(fitBulkImportProvider).result,
      const RouteBulkImportResult(
        totalFiles: 3,
        importedFiles: 3,
        failedFiles: 0,
      ),
    );
  });

  test('opens the files one at a time, not the whole folder at once', () async {
    // The reason this feature has a native tree-walk instead of handing Dart a
    // list of bytes: a year of rides is hundreds of megabytes, and the heap must
    // only ever hold the file being imported.
    final folder = _FakeFolderSource(pick: _folder(['a.fit', 'b.fit']));
    final container = await _container(folder: folder);

    final importing =
        container.read(fitImportCardProvider.notifier).importFolder();
    // Nothing is read just by picking the folder.
    expect(folder.reads, isEmpty);
    await importing;

    expect(folder.reads, [
      'content://tree/a.fit',
      'content://tree/b.fit',
    ]);
  });

  test('a cancelled pick leaves the card exactly as it was', () async {
    // Backing out of the picker is a normal thing to do: no error, no result.
    final container = await _container(folder: _FakeFolderSource(pick: null));

    await container.read(fitImportCardProvider.notifier).importFolder();

    expect(container.read(fitImportCardProvider), const FitImportState());
    expect(container.read(fitBulkImportProvider), const RouteBulkImportState());
  });

  test('a folder with no FIT files says so, and is not an error', () async {
    final container = await _container(folder: _FakeFolderSource(pick: _folder([])));

    await container.read(fitImportCardProvider.notifier).importFolder();

    final state = container.read(fitImportCardProvider);
    expect(state.folderHadNoFitFiles, isTrue);
    expect(state.error, isNull);
    // Nothing ran: an empty batch is not an import.
    expect(container.read(fitBulkImportProvider).result, isNull);
  });

  test('a folder too big to list says how much of it was taken', () async {
    // Silently importing the first N of a folder would read to the user like
    // importing all of it.
    final container = await _container(
      folder: _FakeFolderSource(pick: _folder(['a.fit', 'b.fit'], truncated: true)),
    );

    await container.read(fitImportCardProvider.notifier).importFolder();

    expect(container.read(fitImportCardProvider).truncatedAt, 2);
    expect(container.read(fitBulkImportProvider).result?.importedFiles, 2);
  });

  test('one unreadable file fails that file, not the folder', () async {
    final folder = _FakeFolderSource(
      pick: _folder(['a.fit', 'gone.fit', 'c.fit']),
      unreadable: {'content://tree/gone.fit'},
    );
    final repository = FakeActivityRepository();
    final container = await _container(folder: folder, repository: repository);

    await container.read(fitImportCardProvider.notifier).importFolder();

    expect(repository.writes.length, 2);
    final result = container.read(fitBulkImportProvider).result;
    expect(result?.importedFiles, 2);
    expect(result?.failedFiles, 1);
  });

  test('a failed scan surfaces, and imports nothing', () async {
    final container = await _container(
      folder: _FakeFolderSource(pickError: Exception('tree unreadable')),
    );

    await container.read(fitImportCardProvider.notifier).importFolder();

    expect(container.read(fitImportCardProvider).error, contains('tree unreadable'));
    expect(container.read(fitBulkImportProvider).result, isNull);
  });
}
