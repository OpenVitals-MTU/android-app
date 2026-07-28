import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/data/prefs/preferences_repository.dart';
import 'package:openvitals/data/repository/contract/health_repository.dart';
import 'package:openvitals/data/repository/contract/hydration_repository.dart';
import 'package:openvitals/data/repository/contract/nutrition_repository.dart';
import 'package:openvitals/domain/model/health_connect_availability.dart';
import 'package:openvitals/domain/model/nutrition_models.dart';
import 'package:openvitals/domain/preferences/unit_system.dart';
import 'package:openvitals/features/hydration/reminders/hydration_reminder_quick_add.dart';
import 'package:openvitals/ui/theme/app_colors.dart';
import 'package:shared_preferences/shared_preferences.dart';

Future<PreferencesRepository> _newPreferences([
  Map<String, Object> initial = const {},
]) async {
  SharedPreferences.setMockInitialValues(initial);
  final prefs = await SharedPreferences.getInstance();
  return PreferencesRepository(prefs, localeName: 'en_IE');
}

class _FakeHydrationRepository implements HydrationRepository {
  _FakeHydrationRepository({this.canWrite = true});

  final bool canWrite;
  final List<HydrationWriteRequest> writes = [];
  final List<double> lastCustomAmounts = [];
  final List<double> recentAmounts = [];

  @override
  Future<Result<bool>> hasHydrationWritePermission() async => Ok(canWrite);

  @override
  Future<Result<String>> writeHydrationEntry(
    HydrationWriteRequest request,
  ) async {
    writes.add(request);
    return const Ok('openvitals_hydration_1_uuid');
  }

  @override
  void setLastCustomHydrationAmountMilliliters(double milliliters) =>
      lastCustomAmounts.add(milliliters);

  @override
  void recordRecentHydrationAmountMilliliters(double milliliters) =>
      recentAmounts.add(milliliters);

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

/// Plain water writes no nutrition record, so nothing here may be called.
class _FakeNutritionRepository implements NutritionRepository {
  @override
  dynamic noSuchMethod(Invocation invocation) =>
      throw UnimplementedError('${invocation.memberName} not stubbed');
}

class _FakeHealthRepository implements HealthRepository {
  int refreshCalls = 0;

  @override
  Future<Result<HealthConnectAvailability>> refreshAvailability() async {
    refreshCalls++;
    return const Ok(HealthConnectAvailability.available);
  }

  @override
  dynamic noSuchMethod(Invocation invocation) =>
      throw UnimplementedError('${invocation.memberName} not stubbed');
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  group('hydrationQuickAddAmountMilliliters', () {
    test('round-trips the volume through the action id', () {
      expect(
        hydrationQuickAddAmountMilliliters(hydrationQuickAddActionId(350.0)),
        350.0,
      );
    });

    test('ignores anything that is not a quick-add action', () {
      // The background handler is shared by every reminder the app schedules,
      // so a plain tap (null) or a foreign action must be inert.
      expect(hydrationQuickAddAmountMilliliters(null), isNull);
      expect(hydrationQuickAddAmountMilliliters('mindfulness_start'), isNull);
      expect(hydrationQuickAddAmountMilliliters('hydration_quick_add:'), isNull);
      expect(
        hydrationQuickAddAmountMilliliters('hydration_quick_add:nope'),
        isNull,
      );
    });

    test('rejects out-of-range volumes from a stale schedule', () {
      expect(hydrationQuickAddAmountMilliliters('hydration_quick_add:-5.0'),
          isNull);
      expect(hydrationQuickAddAmountMilliliters('hydration_quick_add:0.0'),
          isNull);
      expect(
        hydrationQuickAddAmountMilliliters('hydration_quick_add:900000.0'),
        isNull,
      );
    });
  });

  group('hydrationQuickAddAmountsMilliliters', () {
    test('falls back to a glass and a bottle for a fresh install', () async {
      final preferences = await _newPreferences();
      expect(
        hydrationQuickAddAmountsMilliliters(preferences),
        [250.0, 500.0],
      );
    });

    test('offers the last two used sizes, newest first', () async {
      final preferences = await _newPreferences();
      preferences.recordRecentHydrationAmountMilliliters(250.0);
      preferences.recordRecentHydrationAmountMilliliters(350.0);
      expect(
        hydrationQuickAddAmountsMilliliters(preferences),
        [350.0, 250.0],
      );
    });

    test('pads a single recent with the last custom amount, then defaults',
        () async {
      final preferences = await _newPreferences();
      preferences.recordRecentHydrationAmountMilliliters(330.0);

      // No last custom amount: the first default fills the second slot.
      expect(
        hydrationQuickAddAmountsMilliliters(preferences),
        [330.0, 250.0],
      );

      // With one (pre-recents installs have it), it wins over the default.
      preferences.setLastCustomHydrationAmountMilliliters(120.0);
      expect(
        hydrationQuickAddAmountsMilliliters(preferences),
        [330.0, 120.0],
      );
    });

    test('never offers the same size twice', () async {
      final preferences = await _newPreferences();
      preferences.recordRecentHydrationAmountMilliliters(250.0);
      preferences.setLastCustomHydrationAmountMilliliters(250.0);
      // 250 fills slot one; the duplicate last-custom and the duplicate 250
      // fallback are both skipped, so the bottle default lands in slot two.
      expect(
        hydrationQuickAddAmountsMilliliters(preferences),
        [250.0, 500.0],
      );
    });
  });

  group('hydrationReminderQuickAddActions', () {
    test('builds two silent actions labelled in millilitres for metric',
        () async {
      final preferences = await _newPreferences();
      preferences.unitSystem = UnitSystem.metric;
      preferences.recordRecentHydrationAmountMilliliters(250.0);
      preferences.recordRecentHydrationAmountMilliliters(350.0);

      final actions = hydrationReminderQuickAddActions(preferences);

      expect(actions, hasLength(2));
      expect(actions[0].title, 'Add 350 ml');
      expect(actions[0].id, hydrationQuickAddActionId(350.0));
      expect(actions[1].title, 'Add 250 ml');
      expect(actions[1].id, hydrationQuickAddActionId(250.0));
      for (final action in actions) {
        // A quick-add must log silently in the background isolate, not launch
        // the app, and the tapped reminder must dismiss itself.
        expect(action.showsUserInterface, isFalse);
        expect(action.cancelNotification, isTrue);
        // Accent-colored so the actions read as buttons, not plain shade text.
        expect(action.titleColor, AppColors.hydration);
      }
    });

    test('labels in fluid ounces for imperial', () async {
      final preferences = await _newPreferences();
      preferences.unitSystem = UnitSystem.imperial;
      preferences.recordRecentHydrationAmountMilliliters(350.0);

      final actions = hydrationReminderQuickAddActions(preferences);

      expect(actions.first.title, 'Add 12 fl oz');
      // The id still carries millilitres — the storage unit, not the display
      // unit — so a unit-system change cannot corrupt what a tap logs.
      expect(actions.first.id, hydrationQuickAddActionId(350.0));
    });
  });

  group('HydrationQuickAddLogger', () {
    test('resolves Health Connect access, then logs plain water', () async {
      final health = _FakeHealthRepository();
      final hydration = _FakeHydrationRepository();
      var reanchors = 0;

      await HydrationQuickAddLogger(
        health: health,
        hydrationRepository: hydration,
        nutritionRepository: _FakeNutritionRepository(),
        onHydrationLogged: () async => reanchors++,
      ).log(350.0);

      expect(health.refreshCalls, 1);
      expect(hydration.writes, hasLength(1));
      expect(hydration.writes.single.volumeLiters, closeTo(0.35, 1e-9));
      // Plain water: no drink identity on the record.
      expect(hydration.writes.single.drinkId, isNull);
      // The tapped size is remembered for the entry screen and re-offered by
      // the next reminder's actions.
      expect(hydration.lastCustomAmounts, [350.0]);
      expect(hydration.recentAmounts, [350.0]);
      // The reminder countdown re-anchors to this drink (which also refreshes
      // the scheduled batch's action labels).
      expect(reanchors, 1);
    });

    test('a missing write permission logs nothing and leaves the schedule',
        () async {
      final hydration = _FakeHydrationRepository(canWrite: false);
      var reanchors = 0;

      await HydrationQuickAddLogger(
        health: _FakeHealthRepository(),
        hydrationRepository: hydration,
        nutritionRepository: _FakeNutritionRepository(),
        onHydrationLogged: () async => reanchors++,
      ).log(350.0);

      expect(hydration.writes, isEmpty);
      // No drink landed, so the countdown must not re-anchor to it.
      expect(reanchors, 0);
    });

    test('a failing re-anchor never fails the logged drink', () async {
      final hydration = _FakeHydrationRepository();

      await HydrationQuickAddLogger(
        health: _FakeHealthRepository(),
        hydrationRepository: hydration,
        nutritionRepository: _FakeNutritionRepository(),
        onHydrationLogged: () async => throw StateError('no plugin here'),
      ).log(350.0);

      expect(hydration.writes, hasLength(1));
    });
  });
}
