import 'dart:typed_data';

import 'package:file_selector/file_selector.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/data/repository/contract/activity_repository.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/activity_models.dart';
import 'package:openvitals/domain/model/health_connect_availability.dart';
import 'package:openvitals/features/imports/application/pending_route_import.dart';
import 'package:openvitals/features/manualentry/activity/activity_entry_providers.dart';
import 'package:openvitals/features/manualentry/activity/activity_entry_notifier.dart';
import 'package:openvitals/features/manualentry/activity/routeimport/route_file_parser.dart';
import 'package:openvitals/features/settings/presentation/cards/fit_import_card.dart';
import 'package:openvitals/features/settings/presentation/cards/route_import_card.dart';
import 'package:openvitals/data/source/health/health_data_source.dart';
import 'package:openvitals/l10n/app_localizations.dart';

/// Grants every permission and reports Health Connect available, so the bulk
/// button is enabled.
class _FakeHealthDataSource extends HealthDataSource {
  @override
  Future<HealthConnectAvailability> availability() async =>
      HealthConnectAvailability.available;
  @override
  Future<Set<String>> grantedPermissions() async => _allWrite;
}

const _allWrite = {'write-exercise'};

/// A route importer that returns a canned parse (or throws for the flagged
/// file), so the bulk path never touches a real parser.
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

class _FakeActivityRepository implements ActivityRepository {
  final List<ActivityWriteRequest> writes = [];

  @override
  Set<String> activityWritePermissions() => _allWrite;
  @override
  Set<String> activityWritePermissionsForRequest(ActivityWriteRequest r) =>
      _allWrite;
  @override
  Future<Result<bool>> hasActivityWritePermission() async => const Ok(true);
  @override
  Future<Result<bool>> hasActivityWritePermissionForRequest(
          ActivityWriteRequest r) async =>
      const Ok(true);
  @override
  Future<Result<String>> writeActivityEntry(ActivityWriteRequest request) async {
    writes.add(request);
    return Ok('id-${writes.length}');
  }

  @override
  dynamic noSuchMethod(Invocation invocation) =>
      throw UnimplementedError('${invocation.memberName} not stubbed');
}

/// An in-memory [XFile] (so `readAsBytes` completes under the widget-test fake
/// clock — real file IO does not) whose `path` basename gives `.name` = [name].
XFile _file(String name) =>
    XFile.fromData(Uint8List.fromList([1, 2, 3]), path: name);

Future<ProviderContainer> _pump(
  WidgetTester tester, {
  required Widget child,
  _FakeActivityRepository? repo,
  _FakeRouteFileImporter? importer,
}) async {
  SharedPreferences.setMockInitialValues(const <String, Object>{});
  final prefs = await SharedPreferences.getInstance();
  final container = ProviderContainer(
    overrides: [
      sharedPreferencesProvider.overrideWithValue(prefs),
      healthDataSourceProvider.overrideWithValue(_FakeHealthDataSource()),
      if (repo != null) activityRepositoryProvider.overrideWithValue(repo),
      if (importer != null)
        routeFileImporterProvider.overrideWithValue(importer),
    ],
  );
  addTearDown(container.dispose);
  await tester.pumpWidget(
    UncontrolledProviderScope(
      container: container,
      child: MaterialApp(
        localizationsDelegates: AppLocalizations.localizationsDelegates,
        supportedLocales: AppLocalizations.supportedLocales,
        home: Scaffold(body: SingleChildScrollView(child: child)),
      ),
    ),
  );
  await tester.pumpAndSettle();
  return container;
}

void main() {
  testWidgets('single route import stores the pending handle and navigates',
      (tester) async {
    var navigated = false;
    final container = await _pump(
      tester,
      child: RouteImportCard(
        pickRouteFile: () async => _file('run.gpx'),
        onNavigateToEntry: (_) => navigated = true,
      ),
    );

    await tester.tap(find.text('Import GPX/KML/KMZ file'));
    await tester.pumpAndSettle();

    final pending = container.read(pendingRouteImportProvider);
    expect(pending, isNotNull);
    expect(pending!.fileName, 'run.gpx');
    expect(navigated, isTrue);
  });

  testWidgets('FIT import stores the pending handle and navigates',
      (tester) async {
    var navigated = false;
    final container = await _pump(
      tester,
      child: FitImportCard(
        pickFitFile: () async => _file('ride.fit'),
        onNavigateToEntry: (_) => navigated = true,
      ),
    );

    await tester.tap(find.text('Import FIT file'));
    await tester.pumpAndSettle();

    expect(container.read(pendingRouteImportProvider)?.fileName, 'ride.fit');
    expect(navigated, isTrue);
  });

  testWidgets('bulk import writes one activity per file', (tester) async {
    final repo = _FakeActivityRepository();
    await _pump(
      tester,
      repo: repo,
      importer: _FakeRouteFileImporter(),
      child: RouteImportCard(
        pickRouteFiles: () async => [_file('a.gpx'), _file('b.gpx')],
      ),
    );

    await tester.tap(find.text('Bulk import GPX/KML/KMZ files'));
    await tester.pumpAndSettle();

    expect(repo.writes.length, 2);
    expect(find.textContaining('Imported 2'), findsOneWidget);
  });

  testWidgets('bulk import tolerates a bad file (imported/failed counts)',
      (tester) async {
    final repo = _FakeActivityRepository();
    await _pump(
      tester,
      repo: repo,
      importer: _FakeRouteFileImporter(failFileName: 'bad.gpx'),
      child: RouteImportCard(
        pickRouteFiles: () async => [_file('good.gpx'), _file('bad.gpx')],
      ),
    );

    await tester.tap(find.text('Bulk import GPX/KML/KMZ files'));
    await tester.pumpAndSettle();

    // One good file writes; the bad one is counted as failed, not aborted.
    expect(repo.writes.length, 1);
    expect(find.textContaining('Imported 1'), findsOneWidget);
    expect(find.textContaining('Failed 1'), findsOneWidget);
  });
}
