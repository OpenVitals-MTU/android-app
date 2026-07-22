import 'dart:io';
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
class FakeRouteFileImporter implements RouteFileImporter {
  FakeRouteFileImporter({this.failFileName});

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
class FakeActivityRepository implements ActivityRepository {
  FakeActivityRepository({
    this.granted = true,
    this.writeFailure,
    this.events,
    this.batchFailure,
    this.failSingleWriteAt,
  });

  final bool granted;

  /// Fails every SINGLE write. (The importer only reaches single writes as the
  /// fallback after a batch was rejected.)
  final AppFailure? writeFailure;

  /// Fails every BATCH write. Health Connect's insert is atomic, so the fake
  /// writes nothing when it fails — which is what forces the importer to retry
  /// the batch one file at a time to find the guilty one.
  final AppFailure? batchFailure;

  /// Fails the Nth single write (1-based), and only that one.
  final int? failSingleWriteAt;

  /// Shared log with the file sources, when a test wants to see the ORDER of
  /// reads against writes rather than just the outcome.
  final List<String>? events;

  final List<ActivityWriteRequest> writes = [];

  /// One entry per `writeActivityEntries` CALL, holding that call's batch. The
  /// count is the point of the whole exercise: Health Connect charges its quota
  /// per call, so this is what a bulk import actually spends.
  final List<List<ActivityWriteRequest>> batches = [];

  int _singleWrites = 0;

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
    events?.add('write');
    _singleWrites += 1;
    final failure = writeFailure;
    if (failure != null) return Err(failure);
    if (failSingleWriteAt == _singleWrites) {
      return const Err(UnexpectedFailure('bad record'));
    }
    writes.add(request);
    return Ok('id-${writes.length}');
  }

  @override
  Future<Result<List<String>>> writeActivityEntries(
    List<ActivityWriteRequest> requests,
  ) async {
    events?.add('writeBatch');
    batches.add(List<ActivityWriteRequest>.of(requests));
    if (!granted) {
      return const Err(PermissionFailure(
        'Missing Health Connect activity write permission for this record.',
        cause: MissingActivityWritePermissionException(),
      ));
    }
    // [writeFailure] means "writes fail", so it fails the batch as well — otherwise
    // a test that asks for a failing write would silently get a successful batch.
    // [batchFailure] is the narrower case: the BATCH is rejected while the single
    // writes it falls back to still work.
    final failure = batchFailure ?? writeFailure;
    // Atomic: a rejected batch writes NOTHING.
    if (failure != null) return Err(failure);
    writes.addAll(requests);
    return Ok([for (var i = 0; i < requests.length; i++) 'id-$i']);
  }

  @override
  dynamic noSuchMethod(Invocation invocation) =>
      throw UnimplementedError('${invocation.memberName} not stubbed');
}

/// A file whose bytes are already in hand — the single-pick shape.
ActivityRouteFileSource _handle(String name) => ActivityRouteFileSource.ofBytes(
      bytes: Uint8List.fromList([1, 2, 3]),
      fileName: name,
    );

/// A real FIT ACTIVITY file (`file_id.type` 4) that also carries a wellness
/// message — `physiological_metrics` with only `recovery_time`, exactly as a
/// Garmin writes into the workout it has just recorded.
///
/// The point is that it yields wellness data but NO Health Connect record, which
/// is the combination that once diverted a workout into the wellness path.
Uint8List _activityFitWithMetrics() {
  final b = <int>[];
  void u8(int v) => b.add(v & 0xFF);
  void u16(int v) => b.addAll([v & 0xFF, (v >> 8) & 0xFF]);
  void u32(int v) => b.addAll(
      [v & 0xFF, (v >> 8) & 0xFF, (v >> 16) & 0xFF, (v >> 24) & 0xFF]);

  // file_id: type = 4 (activity)
  u8(0x40);
  u8(0);
  u8(0);
  u16(0);
  u8(1);
  b.addAll([0, 1, 0]);
  u8(0);
  u8(4);
  // physiological_metrics (140): timestamp + recovery_time only.
  u8(0x41);
  u8(0);
  u8(0);
  u16(140);
  u8(2);
  b.addAll([253, 4, 134]);
  b.addAll([9, 2, 132]);
  u8(0x01);
  u32(1153639209);
  u16(180);

  final data = Uint8List.fromList(b);
  final out = <int>[];
  out.addAll([14, 16]);
  out.addAll([0, 0]);
  out.addAll([
    data.length & 0xFF,
    (data.length >> 8) & 0xFF,
    (data.length >> 16) & 0xFF,
    (data.length >> 24) & 0xFF,
  ]);
  out.addAll([0x2E, 0x46, 0x49, 0x54]);
  out.addAll([0, 0]);
  out.addAll(data);
  out.addAll([0, 0]);
  return Uint8List.fromList(out);
}

Future<ProviderContainer> _container({
  required FakeActivityRepository repository,
  FakeRouteFileImporter? importer,
}) async {
  SharedPreferences.setMockInitialValues(const <String, Object>{});
  final prefs = await SharedPreferences.getInstance();
  final container = ProviderContainer(
    overrides: [
      sharedPreferencesProvider.overrideWithValue(prefs),
      activityRepositoryProvider.overrideWithValue(repository),
      routeFileImporterProvider
          .overrideWithValue(importer ?? FakeRouteFileImporter()),
    ],
  );
  addTearDown(container.dispose);
  return container;
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  test('imports every file and reports the counts', () async {
    final repository = FakeActivityRepository();
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
    final container = await _container(repository: FakeActivityRepository());
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

    // Queued (0 of 2), then a snapshot per file as it is read and parsed. The
    // import COUNT lands with the batch, not with the file, because the files are
    // written together — so the last snapshot is the one that carries both.
    expect(
      progress,
      containsAllInOrder(const [
        RouteBulkImportProgress(totalFiles: 2),
        RouteBulkImportProgress(totalFiles: 2, currentFileIndex: 1),
        RouteBulkImportProgress(totalFiles: 2, currentFileIndex: 2),
        RouteBulkImportProgress(
          totalFiles: 2,
          importedFiles: 2,
          currentFileIndex: 2,
        ),
      ]),
    );
    // The finished state drops the progress and publishes the result.
    expect(container.read(routeBulkImportProvider).progress, isNull);
  });

  test('a refused write permission fails one file, not the batch', () async {
    final repository = FakeActivityRepository(granted: false);
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
      repository: FakeActivityRepository(
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
    final repository = FakeActivityRepository();
    final container = await _container(
      repository: repository,
      importer: FakeRouteFileImporter(failFileName: 'bad.gpx'),
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

  test('opens files as it reaches them, never the whole folder up front',
      () async {
    // The memory contract of a FOLDER import. Activities are now written in
    // batches — Health Connect charges its quota per call, so writing one file at
    // a time exhausts it — which means the BATCH is what has to fit in memory, not
    // the file. The contract survives only because the batch is bounded: the
    // importer must still never read a whole folder before writing anything, or a
    // big folder OOMs before the first import.
    final events = <String>[];
    final repository = FakeActivityRepository(events: events);
    final container = await _container(repository: repository);
    ActivityRouteFileSource source(String name) => ActivityRouteFileSource(
          fileName: name,
          read: () async {
            events.add('read:$name');
            return Uint8List.fromList([1, 2, 3]);
          },
        );

    // Comfortably more files than one batch holds.
    final files = [for (var i = 0; i < 60; i++) source('f$i.fit')];
    await container
        .read(routeBulkImportProvider.notifier)
        .importRouteFiles(files, UnitSystem.metric);

    // Something was WRITTEN before the last file was ever OPENED. That is the whole
    // guarantee: reads are bounded by the batch, not by the size of the folder.
    final firstWrite = events.indexOf('writeBatch');
    final lastRead = events.lastIndexOf('read:f59.fit');
    expect(firstWrite, isNonNegative);
    expect(
      firstWrite,
      lessThan(lastRead),
      reason: 'the whole folder was read before anything was written',
    );
    // And every file still got opened exactly once, in order.
    expect(
      events.where((e) => e.startsWith('read:')).length,
      60,
    );
  });

  test('writes activities in batches, not one Health Connect call per file',
      () async {
    // The reason batching exists. Health Connect charges its API-call quota PER
    // CALL, not per record ("requested: 1", however many records the call carried),
    // so a call per file spends a unit of quota per file and a folder of a couple
    // of thousand dies partway through on "API call quota exceeded". This asserts
    // the CALL COUNT, because the call count IS the quota bill.
    final repository = FakeActivityRepository();
    final container = await _container(repository: repository);

    final files = [for (var i = 0; i < 60; i++) _handle('f$i.fit')];
    await container
        .read(routeBulkImportProvider.notifier)
        .importRouteFiles(files, UnitSystem.metric);

    // 60 files, 25 per batch: three calls, not sixty.
    expect(repository.batches.length, 3);
    expect(repository.batches.map((b) => b.length), [25, 25, 10]);
    expect(repository.writes.length, 60);
    expect(container.read(routeBulkImportProvider).result?.importedFiles, 60);
  });

  test('a rejected batch is retried file by file, so one bad file fails alone',
      () async {
    // Health Connect's insert is ATOMIC: one bad record and nothing in the batch is
    // written, and the failure does not say which record was at fault. Without the
    // single-write fallback, one malformed activity would take 24 good ones down
    // with it.
    final repository = FakeActivityRepository(
      batchFailure: const UnexpectedFailure('bad record somewhere'),
      failSingleWriteAt: 2,
    );
    final container = await _container(repository: repository);

    await container.read(routeBulkImportProvider.notifier).importRouteFiles(
      [_handle('a.fit'), _handle('b.fit'), _handle('c.fit')],
      UnitSystem.metric,
    );

    final state = container.read(routeBulkImportProvider);
    // The batch was tried once, then each file singly.
    expect(repository.batches.length, 1);
    // Only the guilty file failed; the other two were still written.
    expect(state.result?.importedFiles, 2);
    expect(state.result?.failedFiles, 1);
    expect(repository.writes.length, 2);
  });

  test('a spent Health Connect quota stops the run instead of failing every file',
      () async {
    // The bug this was written for: when the quota runs out mid-import, every
    // REMAINING file fails for the same reason. Treating that as "one bad file and
    // carry on" marched through the rest of the folder and reported hundreds of
    // perfectly good files as failures. The data is fine and the quota refills, so
    // the run stops and says so.
    final repository = FakeActivityRepository(
      batchFailure: const RateLimitFailure('API call quota exceeded'),
    );
    final container = await _container(repository: repository);

    final files = [for (var i = 0; i < 60; i++) _handle('f$i.fit')];
    await container
        .read(routeBulkImportProvider.notifier)
        .importRouteFiles(files, UnitSystem.metric);

    final state = container.read(routeBulkImportProvider);
    // Stopped at the first refusal: one batch attempted, and NOT retried file by
    // file (a quota refusal is not a bad record — retrying singly would only spend
    // more of a quota that is already gone).
    expect(repository.batches.length, 1);
    expect(repository.writes, isEmpty);
    // Crucially: nothing is blamed on the files.
    expect(state.result?.failedFiles, 0);
    expect(state.result?.importedFiles, 0);
    expect(state.error, contains('later'));
  });

  test('a file that cannot be opened fails that file, not the batch', () async {
    // A folder scanned a minute ago can name a file that has since been moved.
    final repository = FakeActivityRepository();
    final container = await _container(repository: repository);

    await container.read(routeBulkImportProvider.notifier).importRouteFiles(
      [
        ActivityRouteFileSource(
          fileName: 'gone.fit',
          read: () async => throw const FileSystemException('no such file'),
        ),
        _handle('here.fit'),
      ],
      UnitSystem.metric,
    );

    final state = container.read(routeBulkImportProvider);
    expect(repository.writes.length, 1);
    expect(state.result?.importedFiles, 1);
    expect(state.result?.failedFiles, 1);
  });

  test('an activity FIT file is imported as an activity, not skipped as wellness',
      () async {
    // Regression: a Garmin writes VO2 max and recovery time INTO the activity it
    // just recorded. Once those messages were parsed, the file started yielding
    // wellness data, the importer branched on that rather than on the file type,
    // and a real 6.5 KB workout was silently skipped instead of imported.
    final repository = FakeActivityRepository();
    final container = await _container(repository: repository);

    await container.read(routeBulkImportProvider.notifier).importRouteFiles(
      [
        ActivityRouteFileSource.ofBytes(
          bytes: _activityFitWithMetrics(),
          fileName: 'activity_120.fit',
        ),
      ],
      UnitSystem.metric,
    );

    final state = container.read(routeBulkImportProvider);
    expect(repository.writes.length, 1, reason: 'the workout must be written');
    expect(state.result?.importedFiles, 1);
    expect(state.result?.skippedFiles, 0);
    expect(state.result?.failedFiles, 0);
  });

  test('an empty pick does nothing', () async {
    final container = await _container(repository: FakeActivityRepository());

    await container
        .read(routeBulkImportProvider.notifier)
        .importRouteFiles(const [], UnitSystem.metric);

    expect(container.read(routeBulkImportProvider),
        const RouteBulkImportState());
  });
}
