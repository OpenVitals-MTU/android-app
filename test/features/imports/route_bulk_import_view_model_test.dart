import 'dart:typed_data';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/core/result/app_failure.dart';
import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/data/repository/contract/activity_repository.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/activity_models.dart';
import 'package:openvitals/domain/preferences/unit_system.dart';
import 'package:openvitals/domain/usecase/write_imported_activity_use_case.dart';
import 'package:openvitals/features/imports/application/route_bulk_import_view_model.dart';
import 'package:openvitals/features/manualentry/activity/activity_entry_providers.dart';
import 'package:openvitals/features/manualentry/activity/activity_entry_view_model.dart';
import 'package:openvitals/features/manualentry/activity/routeimport/route_file_parser.dart';

const _allWrite = {'write-exercise'};

/// Returns a canned two-point route, or throws for the flagged file name.
class _FakeRouteFileImporter implements RouteFileImporter {
  _FakeRouteFileImporter({this.failFileName});

  final String? failFileName;

  @override
  Future<RouteFileImport> import(ActivityRouteFileHandle handle) async {
    if (handle.fileName == failFileName) {
      throw const RouteImportException('bad file');
    }
    final start = DateTime.utc(2026, 6, 1, 8);
    return RouteFileImport(
      fileName: handle.fileName,
      points: [
        ExerciseRoutePoint(
          time: start,
          latitude: 52.5,
          longitude: 13.4,
          altitudeMeters: null,
          horizontalAccuracyMeters: null,
          verticalAccuracyMeters: null,
        ),
        ExerciseRoutePoint(
          time: start.add(const Duration(minutes: 30)),
          latitude: 52.51,
          longitude: 13.41,
          altitudeMeters: null,
          horizontalAccuracyMeters: null,
          verticalAccuracyMeters: null,
        ),
      ],
      distanceMeters: 5000,
      elevationGainedMeters: 0,
      startTime: start,
      endTime: start.add(const Duration(minutes: 30)),
    );
  }
}

/// The write surface the bulk importer drives, with each failure the
/// [WriteImportedActivityUseCase] can produce: a refused permission (an `Err`
/// carrying [MissingActivityWritePermissionException] as its cause) and a failed
/// write.
class _FakeActivityRepository implements ActivityRepository {
  _FakeActivityRepository({this.granted = true, this.writeFailure});

  final bool granted;
  final AppFailure? writeFailure;
  final List<ActivityWriteRequest> writes = [];

  @override
  Set<String> activityWritePermissions() => _allWrite;

  @override
  Set<String> activityWritePermissionsForRequest(ActivityWriteRequest r) =>
      _allWrite;

  @override
  Future<Result<bool>> hasActivityWritePermission() async => Ok(granted);

  @override
  Future<Result<bool>> hasActivityWritePermissionForRequest(
          ActivityWriteRequest r) async =>
      Ok(granted);

  @override
  Future<Result<String>> writeActivityEntry(ActivityWriteRequest request) async {
    final failure = writeFailure;
    if (failure != null) return Err(failure);
    writes.add(request);
    return Ok('id-${writes.length}');
  }

  @override
  dynamic noSuchMethod(Invocation invocation) =>
      throw UnimplementedError('${invocation.memberName} not stubbed');
}

ActivityRouteFileHandle _handle(String name) => ActivityRouteFileHandle(
      bytes: Uint8List.fromList([1, 2, 3]),
      fileName: name,
    );

Future<ProviderContainer> _container({
  required _FakeActivityRepository repository,
  _FakeRouteFileImporter? importer,
}) async {
  SharedPreferences.setMockInitialValues(const <String, Object>{});
  final prefs = await SharedPreferences.getInstance();
  final container = ProviderContainer(
    overrides: [
      sharedPreferencesProvider.overrideWithValue(prefs),
      activityRepositoryProvider.overrideWithValue(repository),
      routeFileImporterProvider
          .overrideWithValue(importer ?? _FakeRouteFileImporter()),
    ],
  );
  addTearDown(container.dispose);
  return container;
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  test('imports every file and reports the counts', () async {
    final repository = _FakeActivityRepository();
    final container = await _container(repository: repository);

    await container
        .read(routeBulkImportProvider.notifier)
        .importRouteFiles([_handle('a.gpx'), _handle('b.gpx')], UnitSystem.metric);

    final state = container.read(routeBulkImportProvider);
    expect(repository.writes.length, 2);
    expect(state.isImporting, isFalse);
    expect(
      state.result,
      const RouteBulkImportResult(
        totalFiles: 2,
        importedFiles: 2,
        failedFiles: 0,
      ),
    );
    expect(state.error, isNull);
  });

  test('progress counts the files as they land', () async {
    final container = await _container(repository: _FakeActivityRepository());
    final progress = <RouteBulkImportProgress>[];
    container.listen(
      routeBulkImportProvider,
      (_, next) {
        final snapshot = next.progress;
        if (snapshot != null) progress.add(snapshot);
      },
      fireImmediately: true,
    );

    await container
        .read(routeBulkImportProvider.notifier)
        .importRouteFiles([_handle('a.gpx'), _handle('b.gpx')], UnitSystem.metric);

    // Queued (0 of 2), then one snapshot per file, the second one already
    // carrying the first file's import.
    expect(
      progress,
      containsAllInOrder(const [
        RouteBulkImportProgress(totalFiles: 2),
        RouteBulkImportProgress(totalFiles: 2, currentFileIndex: 1),
        RouteBulkImportProgress(
          totalFiles: 2,
          importedFiles: 1,
          currentFileIndex: 2,
        ),
      ]),
    );
    // The finished state drops the progress and publishes the result.
    expect(container.read(routeBulkImportProvider).progress, isNull);
  });

  test('a refused write permission fails one file, not the batch', () async {
    final repository = _FakeActivityRepository(granted: false);
    final container = await _container(repository: repository);

    await container
        .read(routeBulkImportProvider.notifier)
        .importRouteFiles([_handle('a.gpx'), _handle('b.gpx')], UnitSystem.metric);

    final state = container.read(routeBulkImportProvider);
    expect(repository.writes, isEmpty);
    expect(state.result?.failedFiles, 2);
    expect(state.result?.importedFiles, 0);
    // PermissionFailure → ScreenErrorPermissionDenied → this line.
    expect(state.error, 'Activity import write permissions are missing.');
  });

  test('a failed write surfaces the failure message', () async {
    final container = await _container(
      repository: _FakeActivityRepository(
        writeFailure: const UnexpectedFailure('Health Connect said no'),
      ),
    );

    await container
        .read(routeBulkImportProvider.notifier)
        .importRouteFiles([_handle('a.gpx')], UnitSystem.metric);

    final state = container.read(routeBulkImportProvider);
    expect(state.result?.failedFiles, 1);
    expect(state.error, 'Health Connect said no');
  });

  test('a malformed file is tolerated and its parse error reported', () async {
    final repository = _FakeActivityRepository();
    final container = await _container(
      repository: repository,
      importer: _FakeRouteFileImporter(failFileName: 'bad.gpx'),
    );

    await container.read(routeBulkImportProvider.notifier).importRouteFiles(
      [_handle('good.gpx'), _handle('bad.gpx')],
      UnitSystem.metric,
    );

    final state = container.read(routeBulkImportProvider);
    expect(repository.writes.length, 1);
    expect(state.result?.importedFiles, 1);
    expect(state.result?.failedFiles, 1);
    expect(state.error, 'bad file');
  });

  test('an empty pick does nothing', () async {
    final container = await _container(repository: _FakeActivityRepository());

    await container
        .read(routeBulkImportProvider.notifier)
        .importRouteFiles(const [], UnitSystem.metric);

    expect(container.read(routeBulkImportProvider),
        const RouteBulkImportState());
  });
}
