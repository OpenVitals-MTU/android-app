import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/core/reminders/local_notifications_reminder_device.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/features/hydration/reminders/hydration_reminder_device.dart';
import 'package:openvitals/features/mindfulness/reminders/mindfulness_reminder_device.dart';

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

  // Both platforms now use the same pre-scheduled notification batch — there is
  // no alarm-manager path any more, so reminders survive an app update.
  for (final platform in const [TargetPlatform.android, TargetPlatform.iOS]) {
    test('hydration reminders use the batch scheduler on $platform', () async {
      debugDefaultTargetPlatformOverride = platform;
      final container = await newContainer();

      final scheduler = container.read(hydrationReminderSchedulerProvider);
      expect(scheduler, isA<BatchZonedNotificationReminderScheduler>());
      expect(
        (scheduler as BatchZonedNotificationReminderScheduler)
            .spec
            .baseNotificationId,
        hydrationReminderNotificationSpec.baseNotificationId,
      );
    });

    test('mindfulness reminders use the batch scheduler on $platform', () async {
      debugDefaultTargetPlatformOverride = platform;
      final container = await newContainer();

      final scheduler = container.read(mindfulnessReminderSchedulerProvider);
      expect(scheduler, isA<BatchZonedNotificationReminderScheduler>());
      expect(
        (scheduler as BatchZonedNotificationReminderScheduler)
            .spec
            .baseNotificationId,
        mindfulnessReminderNotificationSpec.baseNotificationId,
      );
    });
  }

  test('the two reminders use distinct notification id ranges', () {
    // Sharing a range would make one feature's batch cancel the other's.
    expect(
      hydrationReminderNotificationSpec.baseNotificationId,
      isNot(mindfulnessReminderNotificationSpec.baseNotificationId),
    );
  });
}
