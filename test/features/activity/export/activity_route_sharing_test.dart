import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:share_plus/share_plus.dart';

import 'package:openvitals/domain/model/activity_models.dart';
import 'package:openvitals/features/activity/export/activity_route_export.dart';
import 'package:openvitals/features/activity/export/activity_route_export_cache.dart';
import 'package:openvitals/features/activity/export/activity_route_sharing.dart';

/// Sending a route to another app — a Signal or WhatsApp message, an email
/// attachment. The bytes themselves are covered by the export unit tests; what
/// matters here is that a real file reaches the share sheet, carrying the type
/// and name the receiving app will show the user.

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

final DateTime _start = DateTime.utc(2026, 7, 10, 8);

ExerciseData _workout({List<ExerciseRoutePoint>? points}) => ExerciseData(
      id: 'w1',
      title: 'Morning run',
      exerciseType: 56,
      startTime: _start,
      endTime: _start.add(const Duration(minutes: 30)),
      durationMs: 30 * 60 * 1000,
      source: 'test',
      route: ExerciseRouteData(
        status: ExerciseRouteStatus.data,
        points: points ??
            [
              for (var index = 0; index < 3; index++)
                ExerciseRoutePoint(
                  time: _start.add(Duration(minutes: index)),
                  latitude: 59.43 + index * 0.001,
                  longitude: 24.75 + index * 0.001,
                  altitudeMeters: null,
                  horizontalAccuracyMeters: null,
                  verticalAccuracyMeters: null,
                ),
            ],
      ),
    );

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late Directory tempRoot;
  late _RecordingShareSheet sheet;
  late ActivityRouteExportCache cache;

  setUp(() {
    tempRoot = Directory.systemTemp.createTempSync('route_share_test');
    cache = ActivityRouteExportCache(temporaryDirectory: () async => tempRoot);
    sheet = _RecordingShareSheet();
  });

  tearDown(() => tempRoot.deleteSync(recursive: true));

  ActivityRouteSharing sharing([_RecordingShareSheet? override]) =>
      ActivityRouteSharing(cache: cache, share: (override ?? sheet).call);

  group('ActivityRouteSharing', () {
    test('sharing a route hands the sheet a real GPX file', () async {
      await sharing().shareRoute(
        workout: _workout(),
        format: ActivityRouteExportFormat.gpx,
        chooserTitle: 'Share route with',
      );

      final params = sheet.shared.single;
      final file = params.files!.single;
      expect(File(file.path).existsSync(), isTrue,
          reason: 'the receiving app is handed a path, so it must exist');
      expect(file.path, endsWith('.gpx'));
      expect(File(file.path).readAsStringSync(), contains('<gpx'));
    });

    test('the shared file carries the format mime type so the target app '
        'recognises it', () async {
      await sharing().shareRoute(
        workout: _workout(),
        format: ActivityRouteExportFormat.kmz,
        chooserTitle: 'Share route with',
      );

      expect(sheet.shared.single.files!.single.mimeType,
          ActivityRouteExportFormat.kmz.mimeType);
    });

    test('the chooser title and the email subject reach the share sheet',
        () async {
      final workout = _workout();

      await sharing().shareRoute(
        workout: workout,
        format: ActivityRouteExportFormat.gpx,
        chooserTitle: 'Share route with',
      );

      final params = sheet.shared.single;
      expect(params.title, 'Share route with');
      // The generated name carries the activity title and date, which is what
      // tells one emailed route from another.
      expect(
        params.subject,
        activityRouteExportFileName(workout, ActivityRouteExportFormat.gpx),
      );
      expect(params.subject, contains('morning-run'));
    });

    test('a route with no points fails instead of sharing an empty file',
        () async {
      expect(
        () => sharing().shareRoute(
          workout: _workout(points: const []),
          format: ActivityRouteExportFormat.gpx,
          chooserTitle: 'Share route with',
        ),
        throwsStateError,
      );
      expect(sheet.shared, isEmpty);
    });

    test('a share sheet with no target surfaces as a failure to the caller',
        () async {
      final failing = _RecordingShareSheet(throwOnShare: true);

      await expectLater(
        sharing(failing).shareRoute(
          workout: _workout(),
          format: ActivityRouteExportFormat.gpx,
          chooserTitle: 'Share route with',
        ),
        throwsStateError,
      );
    });
  });

  group('ActivityRouteExportCache', () {
    test('a staged export is named for the activity and its start time',
        () async {
      final workout = _workout();

      final file = await cache.write(workout, ActivityRouteExportFormat.gpx);

      expect(
        file.path,
        endsWith('/${ActivityRouteExportCache.directoryName}/'
            '${activityRouteExportFileName(workout, ActivityRouteExportFormat.gpx)}'),
      );
    });

    test('writing prunes copies older than a day and keeps fresh ones',
        () async {
      // The staged copies only exist for the app being handed the file, so
      // yesterday's are dead weight.
      final directory =
          Directory('${tempRoot.path}/${ActivityRouteExportCache.directoryName}')
            ..createSync(recursive: true);
      final stale = File('${directory.path}/stale.gpx')..writeAsStringSync('x');
      final fresh = File('${directory.path}/fresh.gpx')..writeAsStringSync('x');
      stale.setLastModifiedSync(
        DateTime.now().subtract(const Duration(hours: 25)),
      );

      await cache.write(_workout(), ActivityRouteExportFormat.gpx);

      expect(stale.existsSync(), isFalse);
      expect(fresh.existsSync(), isTrue);
    });
  });
}
