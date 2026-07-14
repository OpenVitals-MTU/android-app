import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/features/manualentry/activity/recording/activity_heart_rate_recovery_banner.dart';
import 'package:openvitals/features/manualentry/activity/recording/activity_recording.dart';
import 'package:openvitals/l10n/app_localizations.dart';

final DateTime _start = DateTime.utc(2026, 7, 14, 18, 0);

ActivityRecordingState _state({
  required ActivityRecordingHrrPhase phase,
  DateTime? effortEndedAt,
  int warmupSeconds = 180,
  int recoverySeconds = 300,
  int? heartRateBpm,
}) =>
    ActivityRecordingState(
      status: ActivityRecordingStatus.recording,
      recordingKind: ActivityRecordingKind.timed,
      startTime: _start,
      hrrPhase: phase,
      hrrEffortEndedAt: effortEndedAt,
      currentHeartRateBpm: heartRateBpm,
      hrrConfig: HeartRateRecoveryTestConfig(
        warmupSeconds: warmupSeconds,
        recoverySeconds: recoverySeconds,
      ),
    );

Future<void> _pump(WidgetTester tester, ActivityRecordingState state,
    {required DateTime now}) async {
  await tester.pumpWidget(
    MaterialApp(
      localizationsDelegates: AppLocalizations.localizationsDelegates,
      supportedLocales: AppLocalizations.supportedLocales,
      home: Scaffold(
        body: ActivityHeartRateRecoveryPhaseBanner(
          state: state,
          now: now,
          onEndEffort: () {},
        ),
      ),
    ),
  );
  await tester.pumpAndSettle();
}

void main() {
  group('phase countdown', () {
    test('the warmup counts down from the start of the recording', () {
      final state = _state(phase: ActivityRecordingHrrPhase.warmup);

      expect(
        state.hrrPhaseRemaining(_start.add(const Duration(seconds: 60))),
        const Duration(seconds: 120),
      );
      expect(state.isHeartRateRecoveryTest, isTrue);
    });

    test('the effort has no deadline — it ends when the rider does', () {
      final state = _state(phase: ActivityRecordingHrrPhase.effort);

      expect(state.hrrPhaseRemaining(_start), isNull,
          reason: 'nothing counts the effort down; the rider or their heart rate '
              'ends it');
    });

    test('the recovery counts down from the instant effort stopped', () {
      final effortEnded = _start.add(const Duration(minutes: 10));
      final state = _state(
        phase: ActivityRecordingHrrPhase.recovery,
        effortEndedAt: effortEnded,
      );

      expect(
        state.hrrPhaseRemaining(effortEnded.add(const Duration(seconds: 90))),
        const Duration(seconds: 210),
      );
    });

    test('a countdown never runs negative', () {
      final effortEnded = _start.add(const Duration(minutes: 10));
      final state = _state(
        phase: ActivityRecordingHrrPhase.recovery,
        effortEndedAt: effortEnded,
      );

      expect(
        state.hrrPhaseRemaining(effortEnded.add(const Duration(minutes: 9))),
        Duration.zero,
      );
    });

    test('an ordinary recording is not a test', () {
      expect(
        _state(phase: ActivityRecordingHrrPhase.none).isHeartRateRecoveryTest,
        isFalse,
      );
    });
  });

  group('phase banner', () {
    testWidgets('offers a way out of the effort by hand', (tester) async {
      await _pump(
        tester,
        _state(phase: ActivityRecordingHrrPhase.effort, heartRateBpm: 176),
        now: _start.add(const Duration(minutes: 5)),
      );

      // The target heart rate is a convenience. On a day when the legs are not there
      // the rider still has to be able to end the effort — and the measurement is
      // just as good, because it only asks that the stop be abrupt.
      expect(find.text('End effort'), findsOneWidget);
      expect(find.text('Go hard'), findsOneWidget);
      expect(find.text('176 bpm'), findsOneWidget);
    });

    testWidgets('during the recovery there is nothing to press, only to keep still',
        (tester) async {
      final effortEnded = _start.add(const Duration(minutes: 10));
      await _pump(
        tester,
        _state(
          phase: ActivityRecordingHrrPhase.recovery,
          effortEndedAt: effortEnded,
        ),
        now: effortEnded.add(const Duration(seconds: 30)),
      );

      expect(find.text('End effort'), findsNothing,
          reason: 'ending the effort again mid-recovery would move the instant the '
              'whole measurement hangs on');
      expect(find.textContaining('keep still'), findsOneWidget);
      expect(find.text('4:30'), findsOneWidget);
    });
  });
}
