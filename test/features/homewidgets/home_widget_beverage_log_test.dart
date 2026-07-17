import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/presentation/unit_formatter.dart';
import 'package:openvitals/core/reminders/reminder_controller.dart';
import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/data/repository/contract/hydration_repository.dart';
import 'package:openvitals/data/repository/contract/nutrition_repository.dart';
import 'package:openvitals/domain/model/nutrition_models.dart';
import 'package:openvitals/domain/preferences/unit_system.dart';
import 'package:openvitals/features/homewidgets/home_widget_beverage.dart';
import 'package:openvitals/features/homewidgets/home_widget_beverage_log.dart';
import 'package:openvitals/features/homewidgets/home_widget_service.dart';
import 'package:openvitals/l10n/app_localizations.dart';

import 'fake_home_widget_client.dart';

const int _appWidgetId = 7;
const String _oneTapReceiver =
    'tech.mmarca.openvitals.features.homewidgets.HomeQuickBeverageOneTapWidgetReceiver';
const String _beverageReceiver =
    'tech.mmarca.openvitals.features.homewidgets.HomeQuickBeverageWidgetReceiver';

/// An espresso with caffeine: the drink that catches a naive log path, which
/// would write the hydration record and silently drop the nutrition one.
final _espresso = CustomHydrationDrink(
  id: 'espresso',
  name: 'Espresso',
  volumeMilliliters: 30,
  hydrationMultiplier: 0.5,
  nutrientValues: const {NutritionNutrient.caffeine: 63.0},
);

class _FakeHydrationRepository implements HydrationRepository {
  _FakeHydrationRepository({this.canWrite = true});

  final bool canWrite;
  final List<HydrationWriteRequest> writes = [];
  final List<double> lastCustomAmounts = [];

  @override
  Future<Result<bool>> hasHydrationWritePermission() async => Ok(canWrite);

  @override
  Future<Result<String>> writeHydrationEntry(
    HydrationWriteRequest request,
  ) async {
    writes.add(request);
    return Ok('openvitals_hydration_1_drink_${request.drinkId}_uuid');
  }

  @override
  void setLastCustomHydrationAmountMilliliters(double milliliters) =>
      lastCustomAmounts.add(milliliters);

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class _FakeNutritionRepository implements NutritionRepository {
  _FakeNutritionRepository({this.canWrite = true});

  final bool canWrite;
  final List<NutritionWriteRequest> writes = [];

  @override
  Future<Result<bool>> hasNutritionWritePermission() async => Ok(canWrite);

  @override
  Future<Result<String>> writeNutritionEntry(
    NutritionWriteRequest request,
  ) async {
    writes.add(request);
    return const Ok('nutrition_1');
  }

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

/// A hydration repository whose write blows up — Health Connect is gone.
class _ThrowingHydrationRepository extends _FakeHydrationRepository {
  @override
  Future<Result<String>> writeHydrationEntry(
    HydrationWriteRequest request,
  ) async =>
      throw StateError('Health Connect is unavailable');
}

/// Records how often the hydration reminder was dismissed after a log.
class _RecordingReminderNotifier implements ReminderNotifier {
  int cancels = 0;

  @override
  Future<void> cancel() async => cancels++;

  @override
  Future<void> show(ReminderGoalProgress progress) async {}
}

void main() {
  late AppLocalizations l10n;

  setUpAll(() async {
    l10n = await AppLocalizations.delegate.load(const Locale('en'));
  });

  /// A placed 1x1 instance, configured with [drink].
  FakeHomeWidgetClient configuredClient({
    CustomHydrationDrink? drink,
    String className = _oneTapReceiver,
  }) =>
      FakeHomeWidgetClient(
        installed: [
          HomeWidgetInstance(appWidgetId: _appWidgetId, className: className),
        ],
        stored: {
          if (drink != null) ...{
            'beverage.$_appWidgetId.selection_id': drink.id,
            'beverage.$_appWidgetId.$homeWidgetDrinkPayloadKey':
                encodeQuickBeverageDrink(drink),
          },
        },
      );

  QuickBeverageWidgetLogger logger(
    FakeHomeWidgetClient client, {
    HydrationRepository? hydration,
    NutritionRepository? nutrition,
    FakeHealthRepository? health,
    ReminderNotifier? hydrationReminder,
  }) =>
      QuickBeverageWidgetLogger(
        service: HomeWidgetService(client: client),
        health: health ?? FakeHealthRepository(),
        hydrationRepository: hydration ?? _FakeHydrationRepository(),
        nutritionRepository: nutrition ?? _FakeNutritionRepository(),
        unitFormatter:
            UnitFormatter(unitSystemProvider: () => UnitSystem.metric),
        localizations: l10n,
        hydrationReminder: hydrationReminder ?? _RecordingReminderNotifier(),
        // The real 1200ms confirmation would make every test wait for it.
        savedConfirmationDuration: Duration.zero,
      );

  group('quickBeverageLogAppWidgetId', () {
    test('reads the appWidgetId off the background URI', () {
      expect(
        quickBeverageLogAppWidgetId(
          Uri.parse('openvitals://beverage_log?appWidgetId=42'),
        ),
        42,
      );
    });

    test('ignores anything that is not a beverage-log broadcast', () {
      // The broadcast is exported, so a foreign URI must be inert.
      expect(quickBeverageLogAppWidgetId(null), isNull);
      expect(
        quickBeverageLogAppWidgetId(Uri.parse('openvitals://widget?route=x')),
        isNull,
      );
      expect(
        quickBeverageLogAppWidgetId(
          Uri.parse('evil://beverage_log?appWidgetId=42'),
        ),
        isNull,
      );
      expect(
        quickBeverageLogAppWidgetId(Uri.parse('openvitals://beverage_log')),
        isNull,
      );
      expect(
        quickBeverageLogAppWidgetId(
          Uri.parse('openvitals://beverage_log?appWidgetId=nope'),
        ),
        isNull,
      );
    });
  });

  group('QuickBeverageWidgetLogger', () {
    // Regression: shipped to a device where tapping the widget did nothing at
    // all. This isolate builds its own HealthDataSource, whose cachedAvailability
    // starts at `notSupported` — and the repositories report NO granted
    // permissions while it does. So hasHydrationWritePermission() was false, the
    // write was discarded as "missing permission", and on the 2x1 (which renders
    // no subtitle) the failure was completely invisible.
    test('resolves Health Connect access before writing', () async {
      final client = configuredClient(drink: _espresso);
      final health = FakeHealthRepository();
      final hydration = _FakeHydrationRepository();

      await logger(client, health: health, hydration: hydration)
          .log(_appWidgetId);

      expect(health.refreshCalls, 1);
      expect(hydration.writes, hasLength(1));
    });

    test('logs the hydration AND the nutrition entry for a drink with caffeine',
        () async {
      final client = configuredClient(drink: _espresso);
      final hydration = _FakeHydrationRepository();
      final nutrition = _FakeNutritionRepository();

      await logger(client, hydration: hydration, nutrition: nutrition)
          .log(_appWidgetId);

      // volumeLiters * hydrationMultiplier — 30ml of espresso at 50% impact.
      expect(hydration.writes, hasLength(1));
      expect(hydration.writes.single.volumeLiters, closeTo(0.015, 1e-9));
      expect(hydration.writes.single.drinkId, 'espresso');
      // The bug the naive path (writeHydrationEntry alone) would cause: the
      // caffeine is dropped. It must ride along, paired to the hydration record.
      expect(nutrition.writes, hasLength(1));
      expect(
        nutrition.writes.single.nutrientValues,
        {NutritionNutrient.caffeine: 63.0},
      );
      expect(nutrition.writes.single.name, 'Espresso');
      expect(
        nutrition.writes.single.associatedHydrationClientRecordId,
        'openvitals_hydration_1_drink_espresso_uuid',
      );
      // Kotlin remembers the tapped volume for the entry screen.
      expect(hydration.lastCustomAmounts, [30.0]);
    });

    test('dismisses an active hydration reminder once water is logged',
        () async {
      // The day-view save hides the reminder; the widget tap must do the same,
      // or the "time to hydrate" notification lingers after you have logged.
      final client = configuredClient(drink: _espresso);
      final reminder = _RecordingReminderNotifier();

      await logger(client, hydrationReminder: reminder).log(_appWidgetId);

      expect(reminder.cancels, 1);
    });

    test('leaves the reminder alone for a nutrition-only drink', () async {
      // No water was recorded, so a HYDRATION reminder has nothing to clear.
      final client = configuredClient(
        drink: _espresso.copyWith(hydrationMultiplier: 0.0),
      );
      final reminder = _RecordingReminderNotifier();

      await logger(client, hydrationReminder: reminder).log(_appWidgetId);

      expect(reminder.cancels, 0);
    });

    test('confirms with "Saved now", then falls back to "Tap to log"', () async {
      final client = configuredClient(drink: _espresso);

      await logger(client).log(_appWidgetId);

      // Both pushes landed, in order, and the tile is left at rest.
      expect(
        client.subtitles,
        [l10n.homeQuickBeverageWidgetSaved, l10n.homeQuickBeverageWidgetTapToLog],
      );
      expect(client.saved['beverage.$_appWidgetId.title'], 'Espresso');
      expect(client.saved['beverage.$_appWidgetId.value'], '30ml');
      expect(
        client.saved['beverage.$_appWidgetId.route'],
        'manual_entry/hydration/log/espresso',
      );
      // The resting subtitle rides along in `unit`: it is what the 1x1 compares
      // its subtitle against to decide whether to spend a third line on it, and
      // comparing against the English native string would never match a
      // localized push.
      expect(
        client.saved['beverage.$_appWidgetId.unit'],
        l10n.homeQuickBeverageWidgetTapToLog,
      );
      // Only the receiver that owns this instance is redrawn.
      expect(client.updated, everyElement(_oneTapReceiver));
    });

    test('a nutrition-only drink confirms with "Saved as nutrition"', () async {
      // A zero-multiplier drink writes no hydration at all.
      final client = configuredClient(
        drink: _espresso.copyWith(hydrationMultiplier: 0.0),
      );
      final hydration = _FakeHydrationRepository();
      final nutrition = _FakeNutritionRepository();

      await logger(client, hydration: hydration, nutrition: nutrition)
          .log(_appWidgetId);

      expect(hydration.writes, isEmpty);
      expect(nutrition.writes, hasLength(1));
      expect(
        client.subtitles.first,
        l10n.homeQuickBeverageWidgetSavedNutrition,
      );
    });

    test('a missing permission writes nothing and is not auto-cleared',
        () async {
      final client = configuredClient(drink: _espresso);
      final hydration = _FakeHydrationRepository(canWrite: false);
      final nutrition = _FakeNutritionRepository();

      await logger(client, hydration: hydration, nutrition: nutrition)
          .log(_appWidgetId);

      expect(hydration.writes, isEmpty);
      expect(nutrition.writes, isEmpty);
      // The error stands: nothing reverts it to "Tap to log".
      expect(client.subtitles, [l10n.homeMetricWidgetPermissionNeeded]);
    });

    test('a missing nutrition permission blocks the whole drink', () async {
      // Kotlin rejects the entry rather than logging the water and losing the
      // caffeine.
      final client = configuredClient(drink: _espresso);
      final hydration = _FakeHydrationRepository();
      final nutrition = _FakeNutritionRepository(canWrite: false);

      await logger(client, hydration: hydration, nutrition: nutrition)
          .log(_appWidgetId);

      expect(hydration.writes, isEmpty);
      expect(client.subtitles, [l10n.homeMetricWidgetPermissionNeeded]);
    });

    test('a failed write reports "Unable to update" and does not throw',
        () async {
      final client = configuredClient(drink: _espresso);

      await logger(client, hydration: _ThrowingHydrationRepository())
          .log(_appWidgetId);

      expect(client.subtitles, [l10n.homeMetricWidgetUpdateFailed]);
    });

    test('an unconfigured instance is told to pick a beverage, and writes nothing',
        () async {
      final client = configuredClient();
      final hydration = _FakeHydrationRepository();

      await logger(client, hydration: hydration).log(_appWidgetId);

      expect(hydration.writes, isEmpty);
      expect(client.saved['beverage.$_appWidgetId.title'],
          l10n.homeQuickBeverageWidgetConfigTitle);
      expect(client.subtitles, [l10n.homeQuickBeverageWidgetNotConfigured]);
      expect(
        client.saved['beverage.$_appWidgetId.route'],
        'manual_entry/hydration',
      );
    });

    test('a stale payload naming another drink is refused', () async {
      // Android recycles appWidgetIds; logging the previous tenant's drink would
      // be worse than doing nothing.
      final client = configuredClient(drink: _espresso);
      client.saved['beverage.$_appWidgetId.selection_id'] = 'green_tea';
      final hydration = _FakeHydrationRepository();

      await logger(client, hydration: hydration).log(_appWidgetId);

      expect(hydration.writes, isEmpty);
      expect(client.subtitles, [l10n.homeQuickBeverageWidgetNotConfigured]);
    });

    test('redraws the 2x1 receiver when the 2x1 owns the instance', () async {
      final client = configuredClient(
        drink: _espresso,
        className: _beverageReceiver,
      );

      await logger(client).log(_appWidgetId);

      expect(client.updated, everyElement(_beverageReceiver));
    });

    test('an appWidgetId belonging to no beverage widget is ignored', () async {
      final client = FakeHomeWidgetClient(
        installed: const [
          HomeWidgetInstance(
            appWidgetId: _appWidgetId,
            className:
                'tech.mmarca.openvitals.features.homewidgets.HomeMetricWidgetReceiver',
          ),
        ],
      );
      final hydration = _FakeHydrationRepository();

      await logger(client, hydration: hydration).log(_appWidgetId);

      expect(hydration.writes, isEmpty);
      expect(client.saved, isEmpty);
      expect(client.updated, isEmpty);
    });
  });
}
