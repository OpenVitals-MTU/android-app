import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/core/reminders/alarm_manager_reminder_scheduler.dart';
import 'package:openvitals/core/reminders/local_notifications_reminder_device.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/features/hydration/reminders/hydration_reminder_alarm.dart';
import 'package:openvitals/features/mindfulness/reminders/mindfulness_reminder_alarm.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  Future<ProviderContainer> newContainer() async {
    SharedPreferences.setMockInitialValues(const <String, Object>{});
    final container = ProviderContainer(
      overrides: [
        sharedPreferencesProvider
            .overrideWithValue(await SharedPreferences.getInstance()),
      ],
    );
    addTearDown(container.dispose);
    return container;
  }

  tearDown(() => debugDefaultTargetPlatformOverride = null);

  test('Android hydration reminders go through the exact alarm manager',
      () async {
    debugDefaultTargetPlatformOverride = TargetPlatform.android;
    final container = await newContainer();

    final scheduler = container.read(hydrationReminderSchedulerProvider);

    // The alarm wakes the app so the reminder can re-check today's intake — a
    // pre-scheduled notification cannot.
    expect(scheduler, isA<AlarmManagerReminderScheduler>());
    expect(
      (scheduler as AlarmManagerReminderScheduler).alarmId,
      hydrationReminderAlarmId,
    );
    expect(scheduler.callback, same(hydrationReminderAlarmCallback));
  });

  test('off Android hydration reminders fall back to a scheduled notification',
      () async {
    debugDefaultTargetPlatformOverride = TargetPlatform.iOS;
    final container = await newContainer();

    expect(
      container.read(hydrationReminderSchedulerProvider),
      isA<ZonedNotificationReminderScheduler>(),
    );
  });

  test('Android mindfulness reminders also go through the alarm manager',
      () async {
    debugDefaultTargetPlatformOverride = TargetPlatform.android;
    final container = await newContainer();

    final scheduler = container.read(mindfulnessReminderSchedulerProvider);
    expect(scheduler, isA<AlarmManagerReminderScheduler>());
    expect(
      (scheduler as AlarmManagerReminderScheduler).alarmId,
      mindfulnessReminderAlarmId,
    );
    expect(scheduler.callback, same(mindfulnessReminderAlarmCallback));
  });

  test('the two reminders use distinct alarm ids', () async {
    // Sharing an id would make one feature's alarm cancel the other's.
    expect(hydrationReminderAlarmId, isNot(mindfulnessReminderAlarmId));
  });

  test('off Android mindfulness falls back to a scheduled notification',
      () async {
    debugDefaultTargetPlatformOverride = TargetPlatform.iOS;
    final container = await newContainer();

    expect(
      container.read(mindfulnessReminderSchedulerProvider),
      isA<ZonedNotificationReminderScheduler>(),
    );
  });
}
