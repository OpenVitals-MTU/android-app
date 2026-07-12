import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/data/prefs/preferences_repository.dart';
import 'package:openvitals/data/repository/contract/mindfulness_repository.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/mindfulness_models.dart';
import 'package:openvitals/features/manualentry/application/mindfulness_entry_view_model.dart';

class _FakeMindfulnessRepository implements MindfulnessRepository {
  final List<MindfulnessSessionWriteRequest> writes = [];
  bool available = true;
  bool canWrite = true;

  @override
  bool isMindfulnessAvailable() => available;

  @override
  Future<Result<bool>> hasMindfulnessWritePermission() async => Ok(canWrite);

  @override
  Set<String> get mindfulnessWritePermissions => const {'write.mindfulness'};

  @override
  Future<Result<String>> writeMindfulnessSessionEntry(
    MindfulnessSessionWriteRequest request,
  ) async {
    writes.add(request);
    return const Ok('id');
  }

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

/// One millisecond per timer tick, so a 1-minute session runs in ~60 ms.
const _fastTick = Duration(milliseconds: 1);

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late _FakeMindfulnessRepository repo;
  late PreferencesRepository prefs;
  late ProviderContainer container;
  late NotifierProvider<MindfulnessEntryViewModel, MindfulnessEntryState> provider;

  Future<void> setUpWith({MindfulnessTimerConfig? config}) async {
    repo = _FakeMindfulnessRepository();
    SharedPreferences.setMockInitialValues(const <String, Object>{});
    prefs = PreferencesRepository(await SharedPreferences.getInstance());
    if (config != null) prefs.setMindfulnessTimerConfig(config);

    provider = NotifierProvider<MindfulnessEntryViewModel, MindfulnessEntryState>(
      () => MindfulnessEntryViewModel(tick: _fastTick),
    );
    container = ProviderContainer(
      overrides: [
        preferencesRepositoryProvider.overrideWithValue(prefs),
        mindfulnessRepositoryProvider.overrideWithValue(repo),
      ],
    );
    addTearDown(container.dispose);
  }

  MindfulnessEntryViewModel notifier() => container.read(provider.notifier);
  MindfulnessEntryState state() => container.read(provider);

  /// Lets the build microtask (permission + edit load) settle.
  Future<void> settle() async {
    container.read(provider);
    for (var i = 0; i < 6; i++) {
      await Future<void>.delayed(Duration.zero);
    }
  }

  /// Waits until [predicate] holds, or fails.
  Future<void> waitUntil(bool Function() predicate) async {
    for (var i = 0; i < 4000; i++) {
      if (predicate()) return;
      await Future<void>.delayed(const Duration(milliseconds: 1));
    }
    fail('condition never held; state: ${state()}');
  }

  group('formattedTimer', () {
    test('pads to mm:ss and clamps below zero', () {
      expect(formattedTimer(0), '00:00');
      expect(formattedTimer(65), '01:05');
      expect(formattedTimer(600), '10:00');
      expect(formattedTimer(-5), '00:00');
    });
  });

  test('seeds its fields from the persisted timer config', () async {
    await setUpWith(
      config: const MindfulnessTimerConfig(
        durationMinutes: 12,
        intervalMinutes: 3,
        bellSound: MindfulnessBellSound.temple,
        backgroundSound: MindfulnessBackgroundSound.chimes,
      ),
    );
    await settle();

    expect(state().durationMinutesText, '12');
    expect(state().intervalEnabled, isTrue);
    expect(state().intervalMinutesText, '3');
    expect(state().bellSound, MindfulnessBellSound.temple);
    expect(state().backgroundSound, MindfulnessBackgroundSound.chimes);
    expect(state().totalSeconds, 12 * 60);
    expect(state().remainingSeconds, 12 * 60);
  });

  group('timer config validation', () {
    test('a non-positive duration cannot start the timer', () async {
      await setUpWith();
      await settle();
      notifier().updateDurationMinutes('0');

      notifier().startTimer();

      expect(state().entryError, MindfulnessEntryError.invalidTimer);
      expect(state().isTimerRunning, isFalse);
    });

    test('an interval at or above the duration is rejected', () async {
      await setUpWith();
      await settle();
      notifier()
        ..updateDurationMinutes('5')
        ..updateIntervalEnabled(true)
        ..updateIntervalMinutes('5');

      notifier().startTimer();

      expect(state().entryError, MindfulnessEntryError.invalidTimer);
      expect(state().isTimerRunning, isFalse);
    });

    test('an interval shorter than the duration starts and persists', () async {
      await setUpWith();
      await settle();
      notifier()
        ..updateDurationMinutes('5')
        ..updateIntervalEnabled(true)
        ..updateIntervalMinutes('2');

      notifier().startTimer();

      expect(state().isTimerRunning, isTrue);
      // Starting the timer persists the config for next time.
      expect(prefs.mindfulnessTimerConfig().durationMinutes, 5);
      expect(prefs.mindfulnessTimerConfig().intervalMinutes, 2);
      notifier().discardTimer();
    });

    test('the fields are frozen while the timer runs', () async {
      await setUpWith();
      await settle();
      notifier().updateDurationMinutes('1');
      notifier().startTimer();

      notifier().updateDurationMinutes('99');
      notifier().updateBellSound(MindfulnessBellSound.harmony);

      expect(state().durationMinutesText, '1');
      expect(state().bellSound, isNot(MindfulnessBellSound.harmony));
      notifier().discardTimer();
    });
  });

  group('sound events', () {
    test('picking a bell emits a preview event with a fresh id', () async {
      await setUpWith();
      await settle();

      notifier().updateBellSound(MindfulnessBellSound.bright);
      final first = state().bellEvent!;
      expect(first.sound, MindfulnessBellSound.bright);
      expect(first.previewMillis, kMindfulnessBellPreviewMillis);

      // Re-picking the same bell must still re-ring it, hence the id.
      notifier().updateBellSound(MindfulnessBellSound.bright);
      expect(state().bellEvent!.id, greaterThan(first.id));
    });

    test('picking "none" clears the background preview', () async {
      await setUpWith();
      await settle();

      notifier().updateBackgroundSound(MindfulnessBackgroundSound.chimes);
      expect(state().backgroundEvent, isNotNull);

      notifier().updateBackgroundSound(MindfulnessBackgroundSound.none);
      expect(state().backgroundEvent, isNull);
    });

    test('an interval bell rings mid-session but not at the end', () async {
      await setUpWith();
      await settle();
      // 2 minutes, bell every 1 minute: one interval bell at 60 s, then the
      // completion bell at 120 s — not two bells at the end.
      notifier()
        ..updateDurationMinutes('2')
        ..updateIntervalEnabled(true)
        ..updateIntervalMinutes('1');

      final ids = <int>{};
      final sub = container.listen(provider, (previous, next) {
        if (next.bellEvent != null) ids.add(next.bellEvent!.id);
      });
      addTearDown(sub.close);

      notifier().startTimer();
      await waitUntil(() => state().timerCompleted);

      // Exactly two rings: the 60-second interval and the completion.
      expect(ids, hasLength(2));
    });
  });

  group('transport', () {
    test('runs down to completion and banks the session', () async {
      await setUpWith();
      await settle();
      notifier().updateDurationMinutes('1');

      notifier().startTimer();
      expect(state().isTimerRunning, isTrue);
      await waitUntil(() => state().timerCompleted);

      expect(state().isTimerRunning, isFalse);
      expect(state().isTimerPaused, isFalse);
      expect(state().remainingSeconds, 0);
    });

    test('stop banks the elapsed span and pauses', () async {
      await setUpWith();
      await settle();
      notifier().updateDurationMinutes('5');

      notifier().startTimer();
      await waitUntil(() => state().remainingSeconds < 5 * 60);
      notifier().stopTimer();

      expect(state().isTimerRunning, isFalse);
      expect(state().isTimerPaused, isTrue);
      expect(state().remainingSeconds, lessThan(5 * 60));
    });

    test('resume continues from where it paused', () async {
      await setUpWith();
      await settle();
      notifier().updateDurationMinutes('5');
      notifier().startTimer();
      await waitUntil(() => state().remainingSeconds <= 5 * 60 - 2);
      notifier().stopTimer();
      final atPause = state().remainingSeconds;

      notifier().resumeTimer();

      expect(state().isTimerRunning, isTrue);
      expect(state().remainingSeconds, lessThanOrEqualTo(atPause));
      notifier().discardTimer();
    });

    test('resume on a finished countdown is rejected', () async {
      await setUpWith();
      await settle();
      notifier().updateDurationMinutes('5');
      notifier().startTimer();
      notifier().stopTimer();
      // Force the paused state to zero remaining, as a completed run would.
      notifier().discardTimer();

      notifier().resumeTimer();
      // Not paused, so it is a no-op rather than an error.
      expect(state().isTimerRunning, isFalse);
    });

    test('discard rewinds to the configured duration', () async {
      await setUpWith();
      await settle();
      notifier().updateDurationMinutes('3');
      notifier().startTimer();
      await waitUntil(() => state().remainingSeconds < 3 * 60);

      notifier().discardTimer();

      expect(state().isTimerRunning, isFalse);
      expect(state().isTimerPaused, isFalse);
      expect(state().timerCompleted, isFalse);
      expect(state().remainingSeconds, 3 * 60);
      expect(state().totalSeconds, 3 * 60);
    });
  });

  group('saving', () {
    test('a completed session writes its full duration', () async {
      await setUpWith();
      await settle();
      notifier().updateDurationMinutes('1');
      notifier().startTimer();
      await waitUntil(() => state().timerCompleted);

      await notifier().saveTimerSession();

      expect(repo.writes, hasLength(1));
      final written = repo.writes.single;
      expect(
        written.endTime.difference(written.startTime).inMinutes,
        1,
      );
      expect(state().saveCompleted, isTrue);
      // Rewound, ready for the next session.
      expect(state().remainingSeconds, 60);
    });

    test('a session under a minute is rejected, not rounded to zero', () async {
      await setUpWith();
      await settle();
      notifier().updateDurationMinutes('5');
      notifier().startTimer();
      await waitUntil(() => state().remainingSeconds < 5 * 60);
      notifier().stopTimer();

      await notifier().saveTimerSession();

      expect(state().entryError, MindfulnessEntryError.timerTooShort);
      expect(repo.writes, isEmpty);
    });

    test('saving without a banked session is a no-op', () async {
      await setUpWith();
      await settle();

      await notifier().saveTimerSession();

      expect(repo.writes, isEmpty);
      expect(state().entryError, isNull);
    });

    test('an unavailable device reports unavailable, not a permission error',
        () async {
      await setUpWith();
      repo.available = false;
      await settle();
      notifier().updateDurationMinutes('1');
      notifier().startTimer();
      await waitUntil(() => state().timerCompleted);

      await notifier().saveTimerSession();

      expect(state().entryError, MindfulnessEntryError.unavailable);
      expect(repo.writes, isEmpty);
    });

    test('a missing write permission blocks the save', () async {
      await setUpWith();
      repo.canWrite = false;
      await settle();
      notifier().updateDurationMinutes('1');
      notifier().startTimer();
      await waitUntil(() => state().timerCompleted);

      await notifier().saveTimerSession();

      expect(state().entryError, MindfulnessEntryError.missingWritePermission);
      expect(repo.writes, isEmpty);
    });
  });
}
