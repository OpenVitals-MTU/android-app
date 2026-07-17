import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:geolocator/geolocator.dart';

import 'package:openvitals/core/presentation/unit_formatter.dart';
import 'package:openvitals/domain/preferences/unit_system.dart';
import 'package:openvitals/features/manualentry/activity/activity_entry_state.dart';
import 'package:openvitals/domain/model/activity_entry_types.dart';
import 'package:openvitals/features/manualentry/activity/recording/activity_recording.dart';
import 'package:openvitals/features/manualentry/activity/recording/activity_recording_device_support.dart';
import 'package:openvitals/features/manualentry/activity/recording/activity_recording_focus_mode.dart';
import 'package:openvitals/features/manualentry/activity/recording/activity_recording_screen.dart';
import 'package:openvitals/features/manualentry/activity/recording/activity_recording_setup_screen.dart';
import 'package:openvitals/l10n/app_localizations.dart';
import 'package:openvitals/ui/theme/activity_recording_theme.dart';

void main() {
  UnitFormatter formatter([UnitSystem system = UnitSystem.metric]) =>
      UnitFormatter(unitSystemProvider: () => system);

  ActivityEntryType typeById(String id) =>
      defaultActivityEntryTypes.firstWhere((type) => type.id == id);

  ActivityEntryType gpsType() =>
      defaultActivityEntryTypes.firstWhere((type) => type.supportsGpsRoute);

  Position position({
    double accuracy = 5,
    Duration age = Duration.zero,
  }) =>
      Position(
        latitude: 59.4,
        longitude: 24.7,
        timestamp: DateTime.now().toUtc().subtract(age),
        accuracy: accuracy,
        altitude: 30,
        altitudeAccuracy: 1,
        heading: 0,
        headingAccuracy: 0,
        speed: 0,
        speedAccuracy: 0,
      );

  Future<void> pump(
    WidgetTester tester,
    Widget child, {
    ActivityRecordingDeviceSupport? support,
  }) async {
    tester.view.physicalSize = const Size(1200, 2400);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          if (support != null)
            activityRecordingDeviceSupportProvider.overrideWithValue(support),
        ],
        child: MaterialApp(
          localizationsDelegates: AppLocalizations.localizationsDelegates,
          supportedLocales: AppLocalizations.supportedLocales,
          home: Scaffold(body: child),
        ),
      ),
    );
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 50));
  }

  group('activityGpsFixQuality', () {
    test('a fresh, accurate fix is precise', () {
      final quality =
          activityGpsFixQuality(position(), now: DateTime.now().toUtc());
      expect(quality.isPrecise, isTrue);
      expect(quality.accuracyMeters, 5);
    });

    test('a fix worse than the required accuracy is not precise', () {
      final quality = activityGpsFixQuality(
        position(accuracy: 100),
        now: DateTime.now().toUtc(),
      );
      expect(quality.isPrecise, isFalse);
    });

    test('a stale fix is not precise, however accurate', () {
      final quality = activityGpsFixQuality(
        position(age: const Duration(seconds: 30)),
        now: DateTime.now().toUtc(),
      );
      expect(quality.isPrecise, isFalse);
    });

    test('a fix from before the session started is not precise', () {
      final now = DateTime.now().toUtc();
      final quality = activityGpsFixQuality(
        position(),
        startTime: now.add(const Duration(minutes: 1)),
        now: now,
      );
      expect(quality.isPrecise, isFalse);
    });
  });

  group('PreRecordingGpsFixState', () {
    test('withholds the fix without permission or with GPS off', () {
      final quality = activityGpsFixQuality(
        position(),
        now: DateTime.now().toUtc(),
      );
      expect(
        PreRecordingGpsFixState(
          hasPrecisePermission: false,
          gpsProviderEnabled: true,
          latestPosition: position(),
          fixQuality: quality,
        ).latestPreciseFix,
        isNull,
      );
      expect(
        PreRecordingGpsFixState(
          hasPrecisePermission: true,
          gpsProviderEnabled: false,
          latestPosition: position(),
          fixQuality: quality,
        ).latestPreciseFix,
        isNull,
      );
    });

    test('exposes an initial fix once everything lines up', () {
      final state = PreRecordingGpsFixState(
        hasPrecisePermission: true,
        gpsProviderEnabled: true,
        latestPosition: position(),
        fixQuality:
            activityGpsFixQuality(position(), now: DateTime.now().toUtc()),
      );
      expect(state.latestPreciseFix, isNotNull);
      expect(state.initialFix!.latitude, 59.4);
      expect(state.initialFix!.accuracyMeters, 5);
    });
  });

  group('setup screen', () {
    Widget setup({
      required ActivityEntryType type,
      bool canWrite = true,
    }) =>
        ActivityRecordingSetupScreen(
          onStartHeartRateRecoveryTest: (_) {},
          state: ActivityEntryUiState(
            mode: ActivityEntryFormMode.recording,
            selectedActivityType: type,
            canWrite: canWrite,
            isCheckingPermission: false,
          ),
          recordingState: const ActivityRecordingState(),
          unitFormatter: formatter(),
          onSelectActivityType: (_) {},
          onStartRecording: (_, _, _) {},
          onRequestLocationPermission: () {},
          onRequestActivityRecognitionPermission: () {},
          onChooseSource: () {},
          onRequestWritePermission: () {},
        );

    testWidgets('a GPS activity cannot start until a precise fix arrives',
        (tester) async {
      final support = _FakeDeviceSupport(hasLocationPermission: true);
      await pump(
        tester,
        setup(type: gpsType()),
        support: support,
      );

      // Permission held, but no fix yet: the fix indicator is red and Start is
      // disabled, so a run never starts from an unknown position.
      expect(_startButton(tester).onPressed, isNull);

      support.emit(position());
      await tester.pump(const Duration(seconds: 1));
      await tester.pump();

      expect(_startButton(tester).onPressed, isNotNull);
    });

    testWidgets(
        'switched to record without GPS, a run starts at once — no fix, no permission',
        (tester) async {
      // GPS held but no fix: without the switch this run cannot start at all (the test
      // above). The whole point of the switch is that there is nothing to wait for.
      final support = _FakeDeviceSupport(hasLocationPermission: true);
      var askedForLocation = false;
      ActivityRecordingInitialFix? startedWithFix;
      bool? startedWithoutGps;

      await pump(
        tester,
        ActivityRecordingSetupScreen(
          state: ActivityEntryUiState(
            mode: ActivityEntryFormMode.recording,
            selectedActivityType: gpsType(),
            canWrite: true,
            isCheckingPermission: false,
          ),
          recordingState: const ActivityRecordingState(),
          unitFormatter: formatter(),
          onSelectActivityType: (_) {},
          onStartRecording: (fix, _, withoutGps) {
            startedWithFix = fix;
            startedWithoutGps = withoutGps;
          },
          onStartHeartRateRecoveryTest: (_) {},
          onRequestLocationPermission: () => askedForLocation = true,
          onRequestActivityRecognitionPermission: () {},
          onChooseSource: () {},
          onRequestWritePermission: () {},
        ),
        support: support,
      );

      expect(_startButton(tester).onPressed, isNull, reason: 'no fix yet');
      expect(find.byType(RecordingWithoutGpsWarning), findsNothing,
          reason: 'no warning before the user has chosen anything to be warned about');

      await tester.tap(find.byType(SwitchListTile));
      await tester.pump();

      // The cost, stated before the run rather than discovered after it. A recording that
      // quietly came back missing half its statistics would feel like the app had failed,
      // and the user would have no way of knowing they had asked for it.
      expect(find.byType(RecordingWithoutGpsWarning), findsOneWidget);
      expect(find.textContaining('No map'), findsOneWidget);
      // Elevation is NOT in the list of losses, and must never creep back into it: the
      // barometer reads air pressure and never needed a position. Same for the step
      // detector and the heart-rate strap. Only what is genuinely derived from location
      // is lost.
      expect(find.textContaining('elevation gain are still recorded'), findsOneWidget);

      // Nothing to wait for: no satellites are being asked for.
      expect(_startButton(tester).onPressed, isNotNull);

      await tester.tap(find.byType(FilledButton));
      await tester.pump();

      expect(startedWithoutGps, isTrue);
      expect(startedWithFix, isNull,
          reason: 'a recording that will never look at a location must not carry one');
      expect(askedForLocation, isFalse,
          reason: 'asking for the location permission for a recording that will never '
              'use it is exactly what makes people distrust a health app');
    });

    testWidgets('without the location permission Start is enabled, to ask for it',
        (tester) async {
      final support = _FakeDeviceSupport(hasLocationPermission: false);
      var askedForLocation = false;
      await pump(
        tester,
        ActivityRecordingSetupScreen(
          onStartHeartRateRecoveryTest: (_) {},
          state: ActivityEntryUiState(
            mode: ActivityEntryFormMode.recording,
            selectedActivityType: gpsType(),
            canWrite: true,
            isCheckingPermission: false,
          ),
          recordingState: const ActivityRecordingState(),
          unitFormatter: formatter(),
          onSelectActivityType: (_) {},
          onStartRecording: (_, _, _) {},
          onRequestLocationPermission: () => askedForLocation = true,
          onRequestActivityRecognitionPermission: () {},
          onChooseSource: () {},
          onRequestWritePermission: () {},
        ),
        support: support,
      );

      expect(_startButton(tester).onPressed, isNotNull);
      await tester.tap(find.text('Start'));
      await tester.pump();
      expect(askedForLocation, isTrue);
    });

    testWidgets('push-ups explain that the proximity sensor is unusable',
        (tester) async {
      final support = _FakeDeviceSupport(hasLocationPermission: true);
      await pump(
        tester,
        setup(type: typeById('push_ups')),
        support: support,
      );

      expect(find.text('How recording works'), findsOneWidget);
      expect(find.textContaining('proximity sensor'), findsOneWidget);
      expect(
        find.text('Live counting is unavailable on this device. '
            'Manual entry is still available.'),
        findsOneWidget,
      );
      // Kotlin disables Start when the counting sensor is missing.
      expect(_startButton(tester).onPressed, isNull);
    });

    testWidgets('a rep activity with a usable sensor is ready and startable',
        (tester) async {
      final support = _FakeDeviceSupport(hasLocationPermission: true);
      await pump(
        tester,
        setup(type: typeById('pull_ups')),
        support: support,
      );

      expect(find.text('Sensor ready'), findsOneWidget);
      expect(_startButton(tester).onPressed, isNotNull);
      // Rep activities get the rest-seconds field, GPS ones do not.
      expect(find.widgetWithText(TextField, 'Rest seconds'), findsOneWidget);
    });
  });

  group('recording screen', () {
    // Through the host, because focus mode is the HOST's state now: the real one
    // (ActivityEntryScreen) owns it so it can drop its app bar for it. Driving
    // the screen directly here would test a shape the app does not have.
    Widget screen(ActivityRecordingState state, {VoidCallback? onFinish}) =>
        _RecordingHost(
          state: state,
          unitFormatter: formatter(),
          onFinishRecording: onFinish ?? () {},
        );

    testWidgets('an idle GPS session offers Start and Cancel only',
        (tester) async {
      await pump(
        tester,
        screen(const ActivityRecordingState(
          recordingKind: ActivityRecordingKind.gpsRoute,
          activityTypeId: 'running',
        )),
        support: _FakeDeviceSupport(hasLocationPermission: false),
      );

      expect(find.text('Start'), findsOneWidget);
      expect(find.text('Cancel'), findsOneWidget);
      expect(find.text('Pause'), findsNothing);
      expect(find.text('Finish'), findsNothing);
    });

    // Kotlin exposes the outdoor toggle whenever the recording dashboard is
    // visible and focus mode is off (ActivityEntryScreen.kt:139-141). Flutter
    // used to render it only inside focus mode, so outdoor mode was unreachable
    // from the normal dashboard.
    testWidgets('the outdoor toggle is reachable from normal recording mode',
        (tester) async {
      await pump(
        tester,
        screen(ActivityRecordingState(
          recordingKind: ActivityRecordingKind.gpsRoute,
          activityTypeId: 'running',
          status: ActivityRecordingStatus.recording,
          startTime: DateTime.now().subtract(const Duration(minutes: 5)),
        )),
        support: _FakeDeviceSupport(hasLocationPermission: true),
      );

      expect(find.byType(ActivityRecordingOutdoorModeToggle), findsOneWidget);
      // It toggles without needing focus mode first.
      await tester.tap(find.byType(ActivityRecordingOutdoorModeToggle));
      await tester.pump();
      expect(find.byType(ActivityRecordingOutdoorModeToggle), findsOneWidget);
    });

    // The regression that mattered most: repetition recordings cannot enter
    // focus mode at all (_canUseFocusMode), so before this the toggle was
    // completely unreachable for them.
    testWidgets('the outdoor toggle is reachable for repetition recordings',
        (tester) async {
      await pump(
        tester,
        screen(ActivityRecordingState(
          recordingKind: ActivityRecordingKind.repetition,
          activityTypeId: 'pull_ups',
          status: ActivityRecordingStatus.recording,
          startTime: DateTime.now().subtract(const Duration(minutes: 2)),
        )),
        support: _FakeDeviceSupport(hasLocationPermission: false),
      );

      expect(find.byType(ActivityRecordingOutdoorModeToggle), findsOneWidget);
    });

    testWidgets('a running GPS session shows the tabs, pause, lap and marker',
        (tester) async {
      await pump(
        tester,
        screen(ActivityRecordingState(
          recordingKind: ActivityRecordingKind.gpsRoute,
          activityTypeId: 'running',
          status: ActivityRecordingStatus.recording,
          startTime: DateTime.now().toUtc(),
        )),
        support: _FakeDeviceSupport(hasLocationPermission: false),
      );

      expect(find.text('Stats'), findsOneWidget);
      expect(find.text('Map'), findsOneWidget);
      expect(find.text('Intervals'), findsOneWidget);
      expect(find.text('Pause'), findsOneWidget);
      expect(find.text('Focus'), findsOneWidget);
      expect(find.text('Finish'), findsOneWidget);

      // A lap needs two points; a marker needs one.
      expect(
        tester.widget<OutlinedButton>(
          find.widgetWithText(OutlinedButton, 'Lap'),
        ).onPressed,
        isNull,
      );
    });

    testWidgets('the dashboard edit toggle only appears while idle or paused',
        (tester) async {
      final support = _FakeDeviceSupport(hasLocationPermission: false);

      await pump(
        tester,
        screen(ActivityRecordingState(
          recordingKind: ActivityRecordingKind.gpsRoute,
          activityTypeId: 'running',
          status: ActivityRecordingStatus.recording,
          startTime: DateTime.now().toUtc(),
        )),
        support: support,
      );
      expect(find.text('Dashboard layout'), findsNothing);

      await pump(
        tester,
        screen(ActivityRecordingState(
          recordingKind: ActivityRecordingKind.gpsRoute,
          activityTypeId: 'running',
          status: ActivityRecordingStatus.paused,
          startTime: DateTime.now().toUtc(),
          pausedStartedAt: DateTime.now().toUtc(),
        )),
        support: support,
      );
      expect(find.text('Dashboard layout'), findsOneWidget);
    });

    testWidgets('editing the dashboard shows the add-field chips',
        (tester) async {
      await pump(
        tester,
        screen(ActivityRecordingState(
          recordingKind: ActivityRecordingKind.gpsRoute,
          activityTypeId: 'running',
          status: ActivityRecordingStatus.paused,
          startTime: DateTime.now().toUtc(),
          pausedStartedAt: DateTime.now().toUtc(),
        )),
        support: _FakeDeviceSupport(hasLocationPermission: false),
      );

      expect(find.text('Add widget'), findsNothing);
      await tester.tap(find.text('Dashboard layout'));
      await tester.pump();

      expect(find.text('Add widget'), findsOneWidget);
      // The tab row is replaced by the editor while editing.
      expect(find.text('Map'), findsNothing);
    });

    testWidgets('a repetition session shows the counter, +/- and End set',
        (tester) async {
      await pump(
        tester,
        screen(ActivityRecordingState(
          recordingKind: ActivityRecordingKind.repetition,
          activityTypeId: 'pull_ups',
          status: ActivityRecordingStatus.recording,
          startTime: DateTime.now().toUtc(),
          currentSetRepetitionCount: 7,
        )),
      );

      expect(find.text('Repetitions'), findsOneWidget);
      expect(find.text('7'), findsOneWidget);
      expect(find.text('End set'), findsOneWidget);
      expect(find.text('Finish session'), findsOneWidget);
      // Focus mode is meaningless for reps, so it is not offered.
      expect(find.text('Focus'), findsNothing);
    });

    testWidgets('a repetition set cannot end before a rep is counted',
        (tester) async {
      await pump(
        tester,
        screen(ActivityRecordingState(
          recordingKind: ActivityRecordingKind.repetition,
          activityTypeId: 'pull_ups',
          status: ActivityRecordingStatus.recording,
          startTime: DateTime.now().toUtc(),
        )),
      );

      expect(
        tester
            .widget<FilledButton>(find.widgetWithText(FilledButton, 'End set'))
            .onPressed,
        isNull,
      );
    });

    testWidgets('Focus enters and exits full-screen mode', (tester) async {
      await pump(
        tester,
        screen(ActivityRecordingState(
          recordingKind: ActivityRecordingKind.gpsRoute,
          activityTypeId: 'running',
          status: ActivityRecordingStatus.recording,
          startTime: DateTime.now().toUtc(),
        )),
        support: _FakeDeviceSupport(hasLocationPermission: false),
      );

      await tester.tap(find.text('Focus'));
      await tester.pump();

      // Focus mode replaces the tabs with the clock + one big Pause.
      expect(find.text('Stats'), findsNothing);
      expect(find.byTooltip('Exit focus mode'), findsOneWidget);
      expect(find.byTooltip('Toggle outdoor readability mode'), findsOneWidget);

      await tester.tap(find.byTooltip('Exit focus mode'));
      await tester.pump();
      expect(find.text('Stats'), findsOneWidget);
    });

    testWidgets('the outdoor toggle applies the high-contrast theme',
        (tester) async {
      await pump(
        tester,
        screen(ActivityRecordingState(
          recordingKind: ActivityRecordingKind.gpsRoute,
          activityTypeId: 'running',
          status: ActivityRecordingStatus.recording,
          startTime: DateTime.now().toUtc(),
        )),
        support: _FakeDeviceSupport(hasLocationPermission: false),
      );

      await tester.tap(find.text('Focus'));
      await tester.pump();
      await tester.tap(find.byTooltip('Toggle outdoor readability mode'));
      await tester.pump();

      // The test platform brightness is light, so system theme mode resolves
      // to the black-on-white outdoor scheme with the burnt-orange accent.
      final focusBackground = tester.widget<ColoredBox>(
        find
            .ancestor(
              of: find.byType(SafeArea),
              matching: find.byType(ColoredBox),
            )
            .first,
      );
      expect(focusBackground.color, Colors.white);

      final toggleIcon = tester.widget<Icon>(
        find.descendant(
          of: find.byTooltip('Toggle outdoor readability mode'),
          matching: find.byType(Icon),
        ),
      );
      expect(toggleIcon.color, recordingOutdoorLightColorScheme.primary);

      // The outdoor scheme is installed as the ambient theme inside the
      // recording surface.
      final context = tester.element(find.byTooltip('Exit focus mode'));
      expect(
        Theme.of(context).colorScheme.primary,
        recordingOutdoorLightColorScheme.primary,
      );

      // Outdoor mode survives leaving focus mode: the normal layout gets the
      // high-contrast background too.
      await tester.tap(find.byTooltip('Exit focus mode'));
      await tester.pump();
      final statsContext = tester.element(find.text('Stats'));
      expect(
        Theme.of(statsContext).colorScheme.primary,
        recordingOutdoorLightColorScheme.primary,
      );
    });
  });
}

FilledButton _startButton(WidgetTester tester) =>
    tester.widget<FilledButton>(find.widgetWithText(FilledButton, 'Start'));

/// A device that never touches a platform channel.
class _FakeDeviceSupport implements ActivityRecordingDeviceSupport {
  _FakeDeviceSupport({required this.hasLocationPermission});

  final bool hasLocationPermission;
  final _positions = StreamController<Position>.broadcast();
  Position? _latest;

  /// Pushes a fix to whoever is already listening, as the OS would.
  void emit(Position position) {
    _latest = position;
    _positions.add(position);
  }

  @override
  Future<bool> hasPreciseLocationPermission() async => hasLocationPermission;

  @override
  Future<bool> requestPreciseLocationPermission() async => hasLocationPermission;

  @override
  Future<bool> isLocationServiceEnabled() async => true;

  @override
  Future<bool> hasNotificationPermission() async => true;

  @override
  Future<bool> requestNotificationPermission() async => true;

  @override
  Future<bool> hasActivityRecognitionPermission() async => false;

  @override
  Future<bool> requestActivityRecognitionPermission() async => false;

  /// A device with an accelerometer but neither a proximity sensor nor a step
  /// detector (the real implementation now asks the platform via
  /// `SensorManager.getDefaultSensor`, which a widget test cannot answer).
  @override
  Future<bool> hasSensorFor(ActivityEntryType activityType) async =>
      switch (activityType.recordingSensor) {
        ActivityRecordingSensor.proximity => false,
        ActivityRecordingSensor.stepDetector => false,
        ActivityRecordingSensor.accelerometer => true,
        ActivityRecordingSensor.ble => true,
        ActivityRecordingSensor.gps ||
        ActivityRecordingSensor.none =>
          !activityType.supportsStepCounting,
      };

  @override
  Stream<Position> watchPosition() => _positions.stream;

  @override
  Future<Position?> lastKnownPosition() async => _latest;
}

/// Stands in for `ActivityEntryScreen`, which owns focus mode so that it can
/// drop its app bar while focus mode is on.
///
/// The screen under test only *reports* the toggle; something above it has to
/// hold the answer. Keeping that here means the Focus tests exercise the same
/// ownership the app has, rather than a self-contained version of it that no
/// longer exists.
class _RecordingHost extends StatefulWidget {
  const _RecordingHost({
    required this.state,
    required this.unitFormatter,
    required this.onFinishRecording,
  });

  final ActivityRecordingState state;
  final UnitFormatter unitFormatter;
  final VoidCallback onFinishRecording;

  @override
  State<_RecordingHost> createState() => _RecordingHostState();
}

class _RecordingHostState extends State<_RecordingHost> {
  bool _isFocusMode = false;

  @override
  Widget build(BuildContext context) => ActivityRecordingScreen(
        state: widget.state,
        unitFormatter: widget.unitFormatter,
        isFocusMode: _isFocusMode,
        onFocusModeChanged: (value) => setState(() => _isFocusMode = value),
        onStartRecording: (_) {},
      onEndHeartRateRecoveryEffort: () {},
        onPauseRecording: () {},
        onResumeRecording: () {},
        onAddLap: () {},
        onAddMarker: () {},
        onUpdateMarker: (_) {},
        onDeleteMarker: (_) {},
        onUpdateDashboardLayout: (_) {},
        onChooseSource: () {},
        onAdjustRepetitionCount: (_) {},
        onEndRepetitionSet: () {},
        onStartNextRepetitionSet: () {},
        onFinishRecording: widget.onFinishRecording,
      );
}
