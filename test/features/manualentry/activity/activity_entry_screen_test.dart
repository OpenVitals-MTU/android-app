import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/data/repository/contract/activity_repository.dart';
import 'package:openvitals/data/repository/contract/health_repository.dart';
import 'package:openvitals/data/repository/contract/heart_repository.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/activity_models.dart';
import 'package:openvitals/domain/preferences/unit_system.dart';
import 'package:openvitals/features/manualentry/activity/activity_entry_form.dart';
import 'package:openvitals/features/manualentry/activity/activity_entry_providers.dart';
import 'package:openvitals/features/manualentry/activity/activity_entry_source_card.dart';
import 'package:openvitals/domain/model/activity_entry_types.dart';
import 'package:openvitals/features/manualentry/activity/activity_plan_picker_cards.dart';
import 'package:openvitals/features/manualentry/activity/recording/activity_recording.dart';
import 'package:openvitals/features/manualentry/activity/recording/activity_recording_screen.dart';
import 'package:openvitals/features/manualentry/activity/recording/activity_recording_setup_screen.dart';
import 'package:openvitals/features/manualentry/presentation/activity_entry_screen.dart';
import 'package:openvitals/navigation/app_routes.dart';
import 'package:openvitals/l10n/app_localizations.dart';
import 'package:openvitals/state/app_providers.dart';

/// Widget-level parity checks for the ported Kotlin `ActivityEntryFormContent`
/// and `ActivityEntryCard`.
void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  const writePermissions = {'write_activity', 'write_route'};

  Future<void> pumpScreen(
    WidgetTester tester, {
    bool canWrite = true,
    List<PlannedExerciseData> plannedWorkouts = const [],
    _FakeHealthRepository? health,
    UnitSystem unitSystem = UnitSystem.metric,
    String? activityEntryId,
    ActivityEntryMode? mode,
    _FakeRecordingController? recorder,
  }) async {
    tester.view.physicalSize = const Size(1000, 2400);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    SharedPreferences.setMockInitialValues(const <String, Object>{});
    final prefs = await SharedPreferences.getInstance();

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          sharedPreferencesProvider.overrideWithValue(prefs),
          unitSystemProvider.overrideWithValue(unitSystem),
          activityRepositoryProvider.overrideWithValue(
            _FakeActivityRepository(
              canWrite: canWrite,
              plannedWorkouts: plannedWorkouts,
            ),
          ),
          heartRepositoryProvider.overrideWithValue(_FakeHeartRepository()),
          activityMarkerRepositoryProvider
              .overrideWithValue(_FakeMarkerRepository()),
          healthRepositoryProvider
              .overrideWithValue(health ?? _FakeHealthRepository()),
          activityRecordingControllerProvider
              .overrideWithValue(recorder ?? _FakeRecordingController()),
        ],
        child: MaterialApp(
          localizationsDelegates: AppLocalizations.localizationsDelegates,
          supportedLocales: AppLocalizations.supportedLocales,
          home: ActivityEntryScreen(
            activityEntryId: activityEntryId,
            mode: mode,
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();
  }

  /// Walks the source card into the manual-entry form.
  Future<void> openManualForm(WidgetTester tester) async {
    await tester.tap(find.text('Create manually'));
    await tester.pumpAndSettle();
  }

  group('source card', () {
    testWidgets('offers the three Kotlin sources; route import is not here',
        (tester) async {
      await pumpScreen(tester);

      expect(find.byType(ActivityEntrySourceCard), findsOneWidget);
      expect(find.text('Create manually'), findsOneWidget);
      expect(find.text('Create from existing plan'), findsOneWidget);
      expect(find.text('Record activity'), findsOneWidget);
      // Route/FIT import moved to Settings → Data import, matching Kotlin
      // (the activity-entry source card has no route-file picker).
      expect(find.text('Import GPX/KML/KMZ'), findsNothing);
    });

    testWidgets('shows the permission explainer and a Grant action when the '
        'write permission is missing', (tester) async {
      await pumpScreen(tester, canWrite: false);

      expect(find.text('Grant'), findsOneWidget);
      expect(
        find.textContaining('we need Health Connect write permissions'),
        findsOneWidget,
      );
    });

    testWidgets('a source action requests the write permission first',
        (tester) async {
      final health = _FakeHealthRepository();
      await pumpScreen(tester, canWrite: false, health: health);

      await tester.tap(find.text('Create manually'));
      await tester.pumpAndSettle();

      expect(health.requested, [writePermissions]);
      // The repository still reports no permission, so the form stays closed.
      expect(find.byType(ActivityEntryCard), findsNothing);
    });

    testWidgets('Grant does not itself open a form', (tester) async {
      final health = _FakeHealthRepository();
      await pumpScreen(tester, canWrite: false, health: health);

      await tester.tap(find.text('Grant'));
      await tester.pumpAndSettle();

      expect(health.requested, [writePermissions]);
      expect(find.byType(ActivityEntrySourceCard), findsOneWidget);
    });
  });

  group('entry card', () {
    testWidgets('renders the Kotlin sections in order', (tester) async {
      await pumpScreen(tester);
      await openManualForm(tester);

      expect(find.byType(ActivityEntryCard), findsOneWidget);
      // The type selector is a dropdown, not the chip row the scaffold used.
      expect(find.byType(DropdownButtonFormField<ActivityEntryType>),
          findsOneWidget);
      expect(find.widgetWithText(TextField, 'Title'), findsOneWidget);
      expect(find.text('Start date'), findsOneWidget);
      expect(find.text('Start time'), findsOneWidget);
      expect(find.widgetWithText(TextField, 'Duration min'), findsOneWidget);
      expect(find.widgetWithText(TextField, 'Active calories'), findsOneWidget);
      expect(
          find.widgetWithText(TextField, 'Total calories burned'), findsOneWidget);
      expect(find.text('How did it feel?'), findsOneWidget);
      expect(find.widgetWithText(TextField, 'Notes'), findsOneWidget);
      expect(find.text('Save activity'), findsOneWidget);
      expect(find.text('Choose another method'), findsOneWidget);
    });

    testWidgets('the feeling chips are the four emoji, and toggle off',
        (tester) async {
      await pumpScreen(tester);
      await openManualForm(tester);

      for (final emoji in ['😀', '🙂', '😓', '😖']) {
        expect(find.text(emoji), findsOneWidget);
      }

      FilterChip greatChip() =>
          tester.widget<FilterChip>(find.ancestor(
            of: find.text('😀'),
            matching: find.byType(FilterChip),
          ));

      expect(greatChip().selected, isFalse);
      await tester.tap(find.text('😀'));
      await tester.pumpAndSettle();
      expect(greatChip().selected, isTrue);

      // Kotlin clears the selection when the selected chip is tapped again.
      await tester.tap(find.text('😀'));
      await tester.pumpAndSettle();
      expect(greatChip().selected, isFalse);
    });

    testWidgets('distance and elevation follow the unit system', (tester) async {
      await pumpScreen(tester, unitSystem: UnitSystem.imperial);
      await openManualForm(tester);

      expect(find.widgetWithText(TextField, 'Distance mi'), findsOneWidget);
      expect(find.widgetWithText(TextField, 'Climb ft'), findsOneWidget);
      expect(find.widgetWithText(TextField, 'Distance km'), findsNothing);
    });

    testWidgets('a validation error surfaces on its own field', (tester) async {
      await pumpScreen(tester);
      await openManualForm(tester);

      await tester.enterText(
          find.widgetWithText(TextField, 'Duration min'), '0');
      await tester.tap(find.text('Save activity'));
      await tester.pumpAndSettle();

      // The duration field carries its own message, and the card carries the
      // screen-level one — matching Kotlin's supportingText + entryError pair.
      expect(find.text('Duration must be between 1 minute and 7 days.'),
          findsOneWidget);
      expect(find.text('Fix the highlighted fields before saving the activity.'),
          findsOneWidget);
    });
  });

  group('repetitions', () {
    /// Running is not repetition-like; push-ups are.
    Future<void> selectType(WidgetTester tester, String label) async {
      await tester.tap(find.byType(DropdownButtonFormField<ActivityEntryType>));
      await tester.pumpAndSettle();
      await tester.tap(find.text(label).last);
      await tester.pumpAndSettle();
    }

    testWidgets('are hidden for a plain GPS activity', (tester) async {
      await pumpScreen(tester);
      await openManualForm(tester);

      expect(find.text('Repetitions'), findsNothing);
      expect(find.text('Steps'), findsNothing);
    });

    testWidgets('a step-counted type gets a single total field, no mode switch',
        (tester) async {
      await pumpScreen(tester);
      await openManualForm(tester);
      await selectType(tester, 'Walking');

      expect(find.text('Steps'), findsWidgets);
      expect(find.text('Total'), findsNothing);
      expect(find.text('Sets'), findsNothing);
    });

    testWidgets('a rep-counted type switches between Total and Sets',
        (tester) async {
      await pumpScreen(tester);
      await openManualForm(tester);
      await selectType(tester, 'Push-ups');

      expect(find.text('Repetitions'), findsWidgets);
      expect(find.widgetWithText(TextField, 'Reps'), findsOneWidget);

      await tester.tap(find.text('Sets'));
      await tester.pumpAndSettle();

      // One set to start with, and its delete button is disabled.
      expect(find.widgetWithText(TextField, 'Set 1 reps'), findsOneWidget);
      expect(find.widgetWithText(TextField, 'Rest time'), findsOneWidget);
      expect(
        tester.widget<IconButton>(find.byType(IconButton).last).onPressed,
        isNull,
      );

      await tester.tap(find.text('Add set'));
      await tester.pumpAndSettle();
      expect(find.widgetWithText(TextField, 'Set 2 reps'), findsOneWidget);

      // With two sets, deleting is allowed again.
      await tester.tap(find.byIcon(Icons.delete_outline).last);
      await tester.pumpAndSettle();
      expect(find.widgetWithText(TextField, 'Set 2 reps'), findsNothing);
    });

    testWidgets('typing in one set does not bleed into another', (tester) async {
      await pumpScreen(tester);
      await openManualForm(tester);
      await selectType(tester, 'Push-ups');
      await tester.tap(find.text('Sets'));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Add set'));
      await tester.pumpAndSettle();

      await tester.enterText(
          find.widgetWithText(TextField, 'Set 1 reps'), '12');
      await tester.enterText(find.widgetWithText(TextField, 'Set 2 reps'), '8');
      await tester.pumpAndSettle();

      expect(
        tester.widget<TextField>(find.widgetWithText(TextField, 'Set 1 reps')).controller?.text,
        '12',
      );
      expect(
        tester.widget<TextField>(find.widgetWithText(TextField, 'Set 2 reps')).controller?.text,
        '8',
      );
    });

    testWidgets('the training plan section only shows for rep-counted types',
        (tester) async {
      await pumpScreen(tester);
      await openManualForm(tester);
      expect(find.text('Save plan'), findsNothing);

      await selectType(tester, 'Push-ups');
      expect(find.text('Save plan'), findsOneWidget);
      expect(find.text('Training plan'), findsOneWidget);
    });
  });

  group('plan pickers', () {
    testWidgets('an empty plan list still offers a way back', (tester) async {
      await pumpScreen(tester);

      await tester.tap(find.text('Create from existing plan'));
      await tester.pumpAndSettle();

      expect(find.byType(ActivityPlanActivityPickerCard), findsOneWidget);
      expect(find.text('No Health Connect plans found'), findsOneWidget);
      await tester.tap(find.text('Choose another method'));
      await tester.pumpAndSettle();
      expect(find.byType(ActivityEntrySourceCard), findsOneWidget);
    });
  });

  group('the live recording dashboard', () {
    // It used to be a child of the form's ListView. A scrollable gives its
    // children unbounded height, so the dashboard's Expanded regions could not
    // lay out, and it was boxed at 0.82 * the screen height instead. That
    // fraction WAS the reported empty space -- a dead band under the controls,
    // the form's padding above -- and outdoor mode painted it pure black.

    _FakeRecordingController recordingOn() => _FakeRecordingController()
      ..state.value = const ActivityRecordingState(
        // A timed type, so this exercises the dashboard without a map.
        activityTypeId: 'treadmill',
        recordingKind: ActivityRecordingKind.timed,
      );

    testWidgets('fills the body instead of a fraction of the screen',
        (tester) async {
      await pumpScreen(
        tester,
        mode: ActivityEntryMode.record,
        recorder: recordingOn(),
      );

      expect(find.byType(ActivityRecordingScreen), findsOneWidget);

      final screenHeight =
          tester.view.physicalSize.height / tester.view.devicePixelRatio;
      final appBarHeight = tester.getSize(find.byType(AppBar)).height;
      final dashboardHeight =
          tester.getSize(find.byType(ActivityRecordingScreen)).height;

      // Every pixel under the app bar, not 82% of them.
      expect(dashboardHeight, closeTo(screenHeight - appBarHeight, 1.0));
    });

    testWidgets('focus mode takes the whole screen, app bar and all',
        (tester) async {
      // Focus mode is read at arm's length, mid-ride. It already carries its own
      // exit button, and the Android back gesture still leaves it, so the app
      // bar's Back arrow was a third way to do the same thing -- and it cost a
      // toolbar of height that could have been another row of metrics.
      await pumpScreen(
        tester,
        mode: ActivityEntryMode.record,
        recorder: _FakeRecordingController()
          ..state.value = ActivityRecordingState(
            activityTypeId: 'treadmill',
            recordingKind: ActivityRecordingKind.timed,
            status: ActivityRecordingStatus.recording,
            startTime: DateTime.now().toUtc(),
          ),
      );

      expect(find.byType(AppBar), findsOneWidget);
      final screenHeight =
          tester.view.physicalSize.height / tester.view.devicePixelRatio;

      await tester.tap(find.text('Focus'));
      await tester.pump();

      expect(find.byTooltip('Exit focus mode'), findsOneWidget);
      expect(find.byType(AppBar), findsNothing,
          reason: 'the app bar -- and its redundant Back arrow -- must go');
      // And the height it was using goes to the dashboard, not to a gap.
      expect(
        tester.getSize(find.byType(ActivityRecordingScreen)).height,
        closeTo(screenHeight, 1.0),
      );

      // Leaving focus mode brings it back: this is the only way out of the
      // screen once the bar is gone, so it had better return.
      await tester.tap(find.byTooltip('Exit focus mode'));
      await tester.pump();
      expect(find.byType(AppBar), findsOneWidget);
    });

    testWidgets('is not inside the form scroll view', (tester) async {
      await pumpScreen(
        tester,
        mode: ActivityEntryMode.record,
        recorder: recordingOn(),
      );

      // The root cause, asserted directly: inside a Scrollable it CANNOT fill the
      // height, so any future re-wrap brings the empty space straight back.
      expect(
        find.ancestor(
          of: find.byType(ActivityRecordingScreen),
          matching: find.byType(Scrollable),
        ),
        findsNothing,
      );
    });

    testWidgets('the setup card, by contrast, stays in the scroll view',
        (tester) async {
      // Only the LIVE dashboard is full-bleed. The setup card is a card like any
      // other and must keep the form's padding and scrolling.
      await pumpScreen(tester, mode: ActivityEntryMode.record);

      expect(find.byType(ActivityRecordingSetupScreen), findsOneWidget);
      expect(find.byType(ActivityRecordingScreen), findsNothing);
      expect(
        find.ancestor(
          of: find.byType(ActivityRecordingSetupScreen),
          matching: find.byType(Scrollable),
        ),
        findsWidgets,
      );
    });
  });
}

class _FakeHealthRepository implements HealthRepository {
  final List<Set<String>> requested = [];

  @override
  Future<bool> requestPermissions(Set<String> permissions) async {
    requested.add(permissions);
    return false;
  }

  @override
  dynamic noSuchMethod(Invocation invocation) =>
      throw UnimplementedError('${invocation.memberName} not stubbed');
}

class _FakeHeartRepository implements HeartRepository {
  @override
  dynamic noSuchMethod(Invocation invocation) =>
      throw UnimplementedError('${invocation.memberName} not stubbed');
}

class _FakeMarkerRepository implements ActivityMarkerRepository {
  @override
  dynamic noSuchMethod(Invocation invocation) =>
      throw UnimplementedError('${invocation.memberName} not stubbed');
}

class _FakeRecordingController implements ActivityRecordingController {
  @override
  final ValueNotifier<ActivityRecordingState> state =
      ValueNotifier(const ActivityRecordingState());

  @override
  void prepareRecordingDashboard(ActivityEntryType activityType) {}
  @override
  void clearPreparedRecording() {}
  @override
  void previewBleConnections() {}
  @override
  void stopBlePreview() {}

  @override
  dynamic noSuchMethod(Invocation invocation) =>
      throw UnimplementedError('${invocation.memberName} not stubbed');
}

class _FakeActivityRepository implements ActivityRepository {
  _FakeActivityRepository({
    required this.canWrite,
    required this.plannedWorkouts,
  });

  final bool canWrite;
  final List<PlannedExerciseData> plannedWorkouts;
  final List<ActivityWriteRequest> writes = [];

  @override
  Set<String> activityWritePermissions() => {'write_activity', 'write_route'};

  @override
  Set<String> activityWritePermissionsForRequest(ActivityWriteRequest request) =>
      activityWritePermissions();

  @override
  Set<String> plannedWorkoutWritePermissions() => {'write_planned'};

  @override
  Future<Result<bool>> hasActivityWritePermission() async => Ok(canWrite);

  @override
  Future<Result<bool>> hasActivityWritePermissionForRequest(
          ActivityWriteRequest request) async =>
      Ok(canWrite);

  @override
  Future<Result<String>> writeActivityEntry(ActivityWriteRequest request) async {
    writes.add(request);
    return const Ok('activity-id');
  }

  @override
  Future<Result<void>> updateActivityEntry(
          String id, ActivityWriteRequest request) async =>
      const Ok(null);

  @override
  Future<Result<ExerciseData?>> loadWorkout(String id) async => const Ok(null);

  @override
  Future<Result<List<PlannedExerciseData>>> loadPlannedWorkoutOptions(
    LocalDate date,
    int exerciseType,
  ) async =>
      Ok(plannedWorkouts);

  @override
  Future<Result<List<PlannedExerciseData>>> loadExistingPlannedWorkouts({
    LocalDate? anchorDate,
  }) async =>
      Ok(plannedWorkouts);

  @override
  Future<Result<String>> writePlannedWorkout(
          PlannedExerciseWriteRequest request) async =>
      const Ok('saved-plan-id');

  @override
  dynamic noSuchMethod(Invocation invocation) =>
      throw UnimplementedError('${invocation.memberName} not stubbed');
}
