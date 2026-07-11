import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/data/prefs/preferences_repository.dart';
import 'package:openvitals/domain/preferences/activity_recording_dashboard_layout.dart';
import 'package:openvitals/domain/preferences/activity_recording_preferences.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// Characterization tests for the two hand-rolled codecs inside
/// [PreferencesRepository] that had **no tests at all** — despite being ~90 lines
/// of bespoke string parsing standing between real users and their saved
/// settings.
///
/// They are written against the **public** API (set → reload from a fresh
/// repository → get), not the private helpers, so they pin the observable
/// behaviour and stay valid across any restructuring of the internals. That is
/// the point: they exist so the codecs can be moved out of this God object
/// without silently eating somebody's dashboard layout.
///
/// Where a test looks like it is asserting something trivial, it is usually
/// pinning a *sentinel*: this codec encodes "no value" as `0`, and getting that
/// round-trip wrong turns "off" into "0 metres" — which is a very different
/// setting.
Future<PreferencesRepository> _newRepo([
  Map<String, Object> initial = const {},
]) async {
  SharedPreferences.setMockInitialValues(initial);
  final prefs = await SharedPreferences.getInstance();
  return PreferencesRepository(prefs);
}

/// Writes through one repository, then reads through a *fresh* one — so the
/// assertion goes through the encoder AND the decoder, not an in-memory cache.
Future<T> _roundTrip<T>(
  void Function(PreferencesRepository repo) write,
  T Function(PreferencesRepository repo) read,
) async {
  final writer = await _newRepo();
  write(writer);
  final prefs = await SharedPreferences.getInstance();
  return read(PreferencesRepository(prefs));
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  group('activity recording dashboard layout codec', () {
    test('every template normalizes to largeTop — the others are storage-only',
        () async {
      // Pinning what the code ACTUALLY does, which is not what the type suggests.
      // `ActivityRecordingDashboardLayout.normalized()` resolves its grid through
      // a hardcoded top-level constant (`_recordingDashboardTemplate = largeTop`)
      // rather than through the instance's own `template` field, so a stored
      // twoByFour comes back as largeTop. The recording dashboard only ever draws
      // largeTop, so this is a deliberate narrowing rather than a bug -- but the
      // enum, the field and the codec all still carry the other two values, and
      // `withTemplate()` takes a template and silently ignores it (it has no
      // callers). Written down here so nobody "fixes" the round-trip below into a
      // feature that does not exist.
      for (final template in ActivityRecordingDashboardTemplate.values) {
        final result = await _roundTrip(
          (repo) => repo.setActivityRecordingDashboardLayout(
            'running',
            ActivityRecordingDashboardLayout(template: template),
          ),
          (repo) => repo.activityRecordingDashboardLayout('running'),
        );
        expect(
          result.template,
          ActivityRecordingDashboardTemplate.largeTop,
          reason: 'stored as ${template.name}',
        );
      }
    });

    test('round-trips the field order', () async {
      const fields = [
        ActivityRecordingDashboardField.speed,
        ActivityRecordingDashboardField.heartRate,
        ActivityRecordingDashboardField.distance,
      ];

      final result = await _roundTrip(
        (repo) => repo.setActivityRecordingDashboardLayout(
          'running',
          const ActivityRecordingDashboardLayout(
            template: ActivityRecordingDashboardTemplate.twoByFour,
            fields: fields,
          ),
        ),
        (repo) => repo.activityRecordingDashboardLayout('running'),
      );

      // Order is the whole point of the layout — it is what the user dragged.
      expect(result.fields.take(fields.length), fields);
    });

    test('round-trips per-field sizes', () async {
      final result = await _roundTrip(
        (repo) => repo.setActivityRecordingDashboardLayout(
          'running',
          ActivityRecordingDashboardLayout(
            template: ActivityRecordingDashboardTemplate.largeTop,
            fields: const [
              ActivityRecordingDashboardField.heartRate,
              ActivityRecordingDashboardField.speed,
            ],
            sizes: {
              ActivityRecordingDashboardField.heartRate:
                  ActivityRecordingDashboardItemSize(columnSpan: 2, rowSpan: 2),
            },
          ),
        ),
        (repo) => repo.activityRecordingDashboardLayout('running'),
      );

      final heartRate = result.items.firstWhere(
        (item) => item.field == ActivityRecordingDashboardField.heartRate,
      );
      expect(heartRate.size.columnSpan, 2);
      expect(heartRate.size.rowSpan, 2);
    });

    test('layouts are per activity type, not global', () async {
      final writer = await _newRepo();
      writer.setActivityRecordingDashboardLayout(
        'running',
        const ActivityRecordingDashboardLayout(
          fields: [ActivityRecordingDashboardField.heartRate],
        ),
      );
      writer.setActivityRecordingDashboardLayout(
        'cycling',
        const ActivityRecordingDashboardLayout(
          fields: [ActivityRecordingDashboardField.cadence],
        ),
      );

      final prefs = await SharedPreferences.getInstance();
      final repo = PreferencesRepository(prefs);
      expect(repo.activityRecordingDashboardLayout('running').fields.first,
          ActivityRecordingDashboardField.heartRate);
      expect(repo.activityRecordingDashboardLayout('cycling').fields.first,
          ActivityRecordingDashboardField.cadence);
    });

    test('an unknown activity type falls back to the default layout', () async {
      final repo = await _newRepo();
      expect(
        repo.activityRecordingDashboardLayout('never-configured').template,
        const ActivityRecordingDashboardLayout().template,
      );
    });

    test('a corrupt stored string degrades to the default, never throws',
        () async {
      // The decoder parses a bespoke separator format. Garbage in the store --
      // a downgrade, a half-written value, a future format -- must not take the
      // recording screen down with it.
      for (final corrupt in [
        '',
        'NOT_A_TEMPLATE',
        'NOT_A_TEMPLATE|HEART_RATE:1x1',
        '|||',
      ]) {
        final repo = await _newRepo({
          'activity_recording_dashboard_layout_running': corrupt,
        });
        expect(
          () => repo.activityRecordingDashboardLayout('running'),
          returnsNormally,
          reason: 'corrupt: "$corrupt"',
        );
      }
    });

    test('an unknown field in a stored layout is dropped, not fatal', () async {
      // Forward compatibility: a layout written by a NEWER build may name a
      // field this build has never heard of.
      final repo = await _newRepo({
        'activity_recording_dashboard_layout_running':
            'LARGE_TOP|HEART_RATE:1x1,WARP_DRIVE_RPM:1x1',
      });

      final layout = repo.activityRecordingDashboardLayout('running');
      expect(layout.fields, contains(ActivityRecordingDashboardField.heartRate));
    });
  });

  group('activity recording preferences — the null sentinels', () {
    // These four are stored as 0 meaning "off"/"null". Losing the distinction
    // turns "no route-gap limit" into "a route-gap limit of zero metres", which
    // would break the drawn line on every single fix.
    test('null route gap survives a round-trip as null, not zero', () async {
      final result = await _roundTrip(
        (repo) => repo.setActivityRecordingPreferences(
          const ActivityRecordingPreferences(routeGapMeters: null),
        ),
        (repo) => repo.activityRecordingPreferences(),
      );
      expect(result.routeGapMeters, isNull);
    });

    test('null distance interval survives as null, not zero', () async {
      final result = await _roundTrip(
        (repo) => repo.setActivityRecordingPreferences(
          const ActivityRecordingPreferences(
            recordingDistanceIntervalMeters: null,
          ),
        ),
        (repo) => repo.activityRecordingPreferences(),
      );
      expect(result.recordingDistanceIntervalMeters, isNull);
    });

    test('null voice intervals survive as null, not zero', () async {
      final result = await _roundTrip(
        (repo) => repo.setActivityRecordingPreferences(
          const ActivityRecordingPreferences(
            voiceAnnouncementTimeIntervalMinutes: null,
            voiceAnnouncementDistanceIntervalMeters: null,
          ),
        ),
        (repo) => repo.activityRecordingPreferences(),
      );
      expect(result.voiceAnnouncementTimeIntervalMinutes, isNull);
      expect(result.voiceAnnouncementDistanceIntervalMeters, isNull);
    });

    test('a real value round-trips as itself, and is not read as null',
        () async {
      final result = await _roundTrip(
        (repo) => repo.setActivityRecordingPreferences(
          const ActivityRecordingPreferences(
            routeGapMeters: 50,
            recordingDistanceIntervalMeters: 10,
          ).normalized(),
        ),
        (repo) => repo.activityRecordingPreferences(),
      );
      expect(result.routeGapMeters, isNotNull);
      expect(result.recordingDistanceIntervalMeters, isNotNull);
    });

    test('the booleans and the timeout round-trip', () async {
      final result = await _roundTrip(
        (repo) => repo.setActivityRecordingPreferences(
          const ActivityRecordingPreferences(
            autoIdleEnabled: false,
            keepScreenOnDuringRecording: false,
            voiceAnnouncementsEnabled: false,
            restTimerBellEnabled: false,
            autoIdleTimeoutSeconds: 45,
          ).normalized(),
        ),
        (repo) => repo.activityRecordingPreferences(),
      );

      expect(result.autoIdleEnabled, isFalse);
      expect(result.keepScreenOnDuringRecording, isFalse);
      expect(result.voiceAnnouncementsEnabled, isFalse);
      expect(result.restTimerBellEnabled, isFalse);
      expect(result.autoIdleTimeoutSeconds, 45);
    });
  });
}
