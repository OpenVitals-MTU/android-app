import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/data/repository/contract/mindfulness_repository.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/health_connect_availability.dart';
import 'package:openvitals/domain/model/mindfulness_models.dart';
import 'package:openvitals/features/manualentry/mindfulness/mindfulness_sound_player.dart';
import 'package:openvitals/features/manualentry/presentation/mindfulness_entry_screen.dart';
import 'package:openvitals/l10n/app_localizations.dart';
import 'package:openvitals/ui/components/health_connect_gate.dart';

class _FakeRepository implements MindfulnessRepository {
  final List<MindfulnessSessionWriteRequest> writes = [];

  @override
  bool isMindfulnessAvailable() => true;

  @override
  Future<Result<bool>> hasMindfulnessWritePermission() async => const Ok(true);

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

/// Records what the timer asks to play, without any audio host.
class _RecordingSoundPlayer implements MindfulnessSoundPlayer {
  final List<MindfulnessBellSound> bells = [];
  final List<MindfulnessBackgroundSound> previews = [];
  final List<MindfulnessBackgroundSound> loops = [];
  int stops = 0;

  @override
  Future<void> playBell(MindfulnessBellSound sound, {int? previewMillis}) async {
    bells.add(sound);
  }

  @override
  Future<void> previewBackground(
    MindfulnessBackgroundSound sound,
    int previewMillis,
  ) async {
    previews.add(sound);
  }

  @override
  Future<void> startBackgroundLoop(MindfulnessBackgroundSound sound) async {
    loops.add(sound);
  }

  @override
  Future<void> stopBackground() async => stops++;

  @override
  Future<void> dispose() async {}
}

void main() {
  late _FakeRepository repo;
  late _RecordingSoundPlayer player;

  Future<void> pumpScreen(WidgetTester tester) async {
    tester.view.physicalSize = const Size(950, 2100);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    SharedPreferences.setMockInitialValues(const <String, Object>{});
    final prefs = await SharedPreferences.getInstance();
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          sharedPreferencesProvider.overrideWithValue(prefs),
          healthConnectAvailabilityProvider
              .overrideWith((ref) async => HealthConnectAvailability.available),
          grantedHealthPermissionsProvider
              .overrideWith((ref) async => {'write.mindfulness'}),
          mindfulnessRepositoryProvider.overrideWithValue(repo),
          mindfulnessSoundPlayerProvider.overrideWithValue(player),
        ],
        child: const MaterialApp(
          localizationsDelegates: AppLocalizations.localizationsDelegates,
          supportedLocales: AppLocalizations.supportedLocales,
          home: MindfulnessEntryScreen(),
        ),
      ),
    );
    await tester.pumpAndSettle();
  }

  setUp(() {
    repo = _FakeRepository();
    player = _RecordingSoundPlayer();
  });

  testWidgets('shows the timer, its sound pickers and the manual card',
      (tester) async {
    await pumpScreen(tester);

    // The countdown seeds from the persisted duration (default 10 minutes).
    expect(find.text('10:00'), findsOneWidget);
    expect(find.text('Start'), findsOneWidget);
    // Five bells + five ambient options.
    expect(find.byType(ChoiceChip), findsNWidgets(10));
    expect(tester.takeException(), isNull);
  });

  testWidgets('picking a bell previews it', (tester) async {
    await pumpScreen(tester);

    await tester.tap(find.widgetWithText(ChoiceChip, 'Temple bowl'));
    await tester.pumpAndSettle();

    expect(player.bells, [MindfulnessBellSound.temple]);
  });

  testWidgets('picking an ambient sound previews it; "none" stops it',
      (tester) async {
    await pumpScreen(tester);

    await tester.tap(find.widgetWithText(ChoiceChip, 'Chimes'));
    await tester.pumpAndSettle();
    expect(player.previews, [MindfulnessBackgroundSound.chimes]);
  });

  testWidgets('starting the timer swaps the controls and loops the ambient',
      (tester) async {
    await pumpScreen(tester);
    // Choose an ambient sound so a loop is expected.
    await tester.tap(find.widgetWithText(ChoiceChip, 'Chimes'));
    await tester.pumpAndSettle();

    await tester.tap(find.text('Start'));
    await tester.pump();

    // Schedule controls give way to the transport.
    expect(find.text('Start'), findsNothing);
    expect(find.text('Stop'), findsOneWidget);
    expect(find.byType(ChoiceChip), findsNothing);
    expect(player.loops, [MindfulnessBackgroundSound.chimes]);

    // Stopping pauses and offers Resume / Save / Discard, and kills the loop.
    await tester.tap(find.text('Stop'));
    await tester.pumpAndSettle();
    expect(find.text('Resume'), findsOneWidget);
    expect(find.text('Save session'), findsOneWidget);
    expect(find.text('Discard'), findsOneWidget);
    expect(player.stops, greaterThan(0));
  });

  testWidgets('discarding rewinds the countdown and restores the pickers',
      (tester) async {
    await pumpScreen(tester);

    await tester.tap(find.text('Start'));
    await tester.pump();
    await tester.tap(find.text('Stop'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Discard'));
    await tester.pumpAndSettle();

    expect(find.text('10:00'), findsOneWidget);
    expect(find.text('Start'), findsOneWidget);
    expect(find.byType(ChoiceChip), findsNWidgets(10));
  });

  testWidgets('a session shorter than a minute is refused', (tester) async {
    await pumpScreen(tester);

    await tester.tap(find.text('Start'));
    await tester.pump();
    await tester.tap(find.text('Stop'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Save session'));
    await tester.pumpAndSettle();

    expect(repo.writes, isEmpty);
    // The timer-too-short error is surfaced, not a silent no-op.
    expect(
      find.text('Meditation must be at least 1 minute to save.'),
      findsOneWidget,
    );
  });
}
