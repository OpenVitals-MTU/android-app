import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/reminders/local_notifications_reminder_device.dart';
import 'package:openvitals/core/reminders/reminder_controller.dart';
import 'package:openvitals/features/hydration/reminders/hydration_reminder_device.dart';
import 'package:openvitals/features/mindfulness/reminders/mindfulness_reminder_device.dart';

/// Every reminder feature registered in the app. A new feature adds its spec
/// here so the uniqueness guard below covers it.
const List<ReminderNotificationSpec> _allSpecs = [
  hydrationReminderNotificationSpec,
  mindfulnessReminderNotificationSpec,
];

void main() {
  test('notification ids are unique across reminder features', () {
    // Sharing an id would make one feature's reminder cancel the other's.
    final ids = [for (final spec in _allSpecs) spec.notificationId];
    expect(ids.toSet(), hasLength(ids.length));
  });

  test('channel ids are unique across reminder features', () {
    final channels = [for (final spec in _allSpecs) spec.channelId];
    expect(channels.toSet(), hasLength(channels.length));
  });

  group('body copy', () {
    test('hydration reports progress against a real goal', () {
      final body = hydrationReminderNotificationSpec.body(
        const ReminderGoalProgress(current: 1.25, target: 2.0),
      );
      expect(body, contains('1.3 L'));
      expect(body, contains('2.0 L'));
    });

    test('hydration falls back when no goal is set', () {
      final body = hydrationReminderNotificationSpec.body(
        const ReminderGoalProgress.none(),
      );
      expect(body, 'Log some water to stay hydrated.');
    });

    test('mindfulness reports whole minutes', () {
      final body = mindfulnessReminderNotificationSpec.body(
        const ReminderGoalProgress(current: 4.6, target: 10),
      );
      expect(body, contains('5 of 10 mindful minutes'));
    });

    test('mindfulness falls back when no goal is set', () {
      final body = mindfulnessReminderNotificationSpec.body(
        const ReminderGoalProgress.none(),
      );
      expect(body, 'Take a few mindful minutes.');
    });
  });
}
