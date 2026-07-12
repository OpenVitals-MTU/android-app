import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/core/reminders/reminder_controller.dart';
import 'package:openvitals/core/reminders/reminder_notifications.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/data/prefs/preferences_repository.dart';
import 'package:openvitals/data/repository/contract/hydration_repository.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/hydration_reminder_config.dart';
import 'package:openvitals/domain/model/nutrition_models.dart';
import 'package:openvitals/features/hydration/reminders/hydration_reminder_card.dart';
import 'package:openvitals/features/hydration/reminders/hydration_reminder_controller.dart';
import 'package:openvitals/l10n/app_localizations.dart';

class _FakeHydrationRepository implements HydrationRepository {
  @override
  Future<Result<List<DailyHydration>>> loadDailyHydration(
    LocalDate start,
    LocalDate end,
  ) async =>
      const Ok(<DailyHydration>[]);

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class _NoopScheduler implements ReminderScheduler {
  @override
  Future<void> schedule(DateTime triggerAt) async {}

  @override
  Future<void> cancel() async {}
}

class _NoopNotifier implements ReminderNotifier {
  @override
  Future<void> show(ReminderGoalProgress progress) async {}

  @override
  Future<void> cancel() async {}
}

class _FakePermissions implements ReminderNotificationPermissions {
  _FakePermissions({this.enabled = true, this.grantOnRequest = true});

  bool enabled;
  bool grantOnRequest;
  int requestCount = 0;

  @override
  Future<bool> isEnabled() async => enabled;

  @override
  Future<bool> request() async {
    requestCount++;
    enabled = grantOnRequest;
    return enabled;
  }
}

void main() {
  late _FakePermissions permissions;
  late PreferencesRepository prefs;

  Future<void> pumpCard(
    WidgetTester tester, {
    HydrationReminderConfig? initial,
  }) async {
    tester.view.physicalSize = const Size(800, 1200);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    SharedPreferences.setMockInitialValues(const <String, Object>{});
    prefs = PreferencesRepository(await SharedPreferences.getInstance());
    if (initial != null) prefs.setHydrationReminderConfig(initial);

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          preferencesRepositoryProvider.overrideWithValue(prefs),
          reminderNotificationPermissionsProvider.overrideWithValue(permissions),
          hydrationReminderControllerProvider.overrideWith(
            (ref) => HydrationReminderController(
              preferences: prefs,
              hydrationRepository: _FakeHydrationRepository(),
              notifier: _NoopNotifier(),
              scheduler: _NoopScheduler(),
            ),
          ),
        ],
        child: const MaterialApp(
          localizationsDelegates: AppLocalizations.localizationsDelegates,
          supportedLocales: AppLocalizations.supportedLocales,
          home: Scaffold(
            body: SingleChildScrollView(child: HydrationReminderCard()),
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();
  }

  setUp(() => permissions = _FakePermissions());

  testWidgets('off: shows only the switch and the off summary', (tester) async {
    await pumpCard(tester);

    expect(find.text('Beverage reminders'), findsOneWidget);
    expect(find.byType(Switch), findsOneWidget);
    expect(tester.widget<Switch>(find.byType(Switch)).value, isFalse);
    // No schedule controls until it is on.
    expect(find.text('Reminder interval'), findsNothing);
    expect(find.text('Active from'), findsNothing);
  });

  testWidgets('on: shows the interval, the window and the goal note',
      (tester) async {
    await pumpCard(
      tester,
      initial: const HydrationReminderConfig(enabled: true, intervalMinutes: 90),
    );

    expect(tester.widget<Switch>(find.byType(Switch)).value, isTrue);
    expect(find.text('Reminder interval'), findsOneWidget);
    expect(find.text('Every 90 min'), findsOneWidget);
    expect(find.text('Active from'), findsOneWidget);
    expect(find.text('Active until'), findsOneWidget);
    // The summary line reflects the schedule.
    expect(find.textContaining('Every 90 min •'), findsOneWidget);
  });

  testWidgets('the stepper edits the interval and persists it', (tester) async {
    await pumpCard(
      tester,
      initial: const HydrationReminderConfig(enabled: true, intervalMinutes: 120),
    );

    await tester.tap(find.byTooltip('Increase hydration reminder interval'));
    await tester.pumpAndSettle();
    expect(find.text('Every 150 min'), findsOneWidget);

    await tester.tap(find.byTooltip('Decrease hydration reminder interval'));
    await tester.pumpAndSettle();
    expect(find.text('Every 120 min'), findsOneWidget);
    expect(prefs.hydrationReminderConfig().intervalMinutes, 120);
  });

  testWidgets('the stepper buttons disable at the interval bounds',
      (tester) async {
    await pumpCard(
      tester,
      initial: const HydrationReminderConfig(
        enabled: true,
        intervalMinutes: HydrationReminderConfig.maxIntervalMinutes,
      ),
    );

    // `byTooltip` matches the Tooltip, not the button it wraps.
    final increase = tester.widget<IconButton>(
      find.widgetWithIcon(IconButton, Icons.add),
    );
    final decrease = tester.widget<IconButton>(
      find.widgetWithIcon(IconButton, Icons.remove),
    );
    expect(increase.onPressed, isNull, reason: 'at the maximum interval');
    expect(decrease.onPressed, isNotNull);
  });

  testWidgets('toggling on persists the config', (tester) async {
    await pumpCard(tester);

    await tester.tap(find.byType(Switch));
    await tester.pumpAndSettle();

    expect(prefs.hydrationReminderConfig().enabled, isTrue);
    expect(find.text('Reminder interval'), findsOneWidget);
  });

  testWidgets('blocked by permission: warns and offers to grant', (tester) async {
    permissions = _FakePermissions(enabled: false, grantOnRequest: true);
    await pumpCard(
      tester,
      initial: const HydrationReminderConfig(enabled: true),
    );

    expect(
      find.text('Grant notification permission to enable beverage reminders.'),
      findsOneWidget,
    );
    final grant = find.widgetWithText(OutlinedButton, 'Grant permission');
    expect(grant, findsOneWidget);

    await tester.tap(grant);
    await tester.pumpAndSettle();

    expect(permissions.requestCount, 1);
    // Warning and button clear once granted.
    expect(find.widgetWithText(OutlinedButton, 'Grant permission'), findsNothing);
  });

  testWidgets('flipping the switch on without permission asks instead',
      (tester) async {
    permissions = _FakePermissions(enabled: false, grantOnRequest: false);
    await pumpCard(tester);

    await tester.tap(find.byType(Switch));
    await tester.pumpAndSettle();

    expect(permissions.requestCount, 1);
    // Denied, so the reminder stays off rather than being enabled-but-dead.
    expect(tester.widget<Switch>(find.byType(Switch)).value, isFalse);
    expect(prefs.hydrationReminderConfig().enabled, isFalse);
  });

  testWidgets('tapping a time row opens a time picker', (tester) async {
    await pumpCard(
      tester,
      initial: const HydrationReminderConfig(enabled: true),
    );

    await tester.tap(find.text('Active from'));
    await tester.pumpAndSettle();

    expect(find.byType(TimePickerDialog), findsOneWidget);
    // Cancel: nothing changes.
    await tester.tap(find.text('Cancel'));
    await tester.pumpAndSettle();
    expect(
      prefs.hydrationReminderConfig().activeStartTime,
      HydrationReminderConfig.defaultActiveStartTime,
    );
  });

  testWidgets('re-reads the permission when the app resumes', (tester) async {
    // Granting in system settings is not reported back to the app, so the card
    // must re-check on resume or it keeps showing the stale warning.
    permissions = _FakePermissions(enabled: false, grantOnRequest: false);
    await pumpCard(
      tester,
      initial: const HydrationReminderConfig(enabled: true),
    );
    expect(find.widgetWithText(OutlinedButton, 'Grant permission'), findsOneWidget);

    permissions.enabled = true;
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.inactive);
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
    await tester.pumpAndSettle();

    expect(find.widgetWithText(OutlinedButton, 'Grant permission'), findsNothing);
    expect(find.text('Reminder interval'), findsOneWidget);
  });
}
