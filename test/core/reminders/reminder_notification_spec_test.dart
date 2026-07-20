import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/reminders/local_notifications_reminder_device.dart';
import 'package:openvitals/core/reminders/reminder_controller.dart';
import 'package:openvitals/features/hydration/reminders/hydration_reminder_device.dart';
import 'package:openvitals/features/mindfulness/reminders/mindfulness_reminder_device.dart';

/// Every reminder feature registered in the app. A new feature adds its spec
/// here so the uniqueness guards below cover it.
const List<ReminderNotificationSpec> _allSpecs = [
  hydrationReminderNotificationSpec,
  mindfulnessReminderNotificationSpec,
];

void main() {
  test('base notification ids are unique across reminder features', () {
    final ids = [for (final spec in _allSpecs) spec.baseNotificationId];
    expect(ids.toSet(), hasLength(ids.length));
  });

  test('reserved id ranges do not overlap across features', () {
    // Each feature owns [base, base + batchSize); an overlap would let one
    // batch's range-cancel wipe another feature's pending reminders.
    for (final a in _allSpecs) {
      for (final b in _allSpecs) {
        if (identical(a, b)) continue;
        final aEnd = a.baseNotificationId + a.batchSize;
        final overlaps =
            b.baseNotificationId >= a.baseNotificationId &&
                b.baseNotificationId < aEnd;
        expect(overlaps, isFalse,
            reason: '${a.channelId} and ${b.channelId} id ranges overlap');
      }
    }
  });

  test('channel ids are unique across reminder features', () {
    final channels = [for (final spec in _allSpecs) spec.channelId];
    expect(channels.toSet(), hasLength(channels.length));
  });

  test('every spec sets a monochrome small icon', () {
    // A missing/blank icon falls back to the launcher icon, which Android
    // renders as a solid white square in the status bar.
    for (final spec in _allSpecs) {
      expect(spec.androidIcon, isNotEmpty, reason: spec.channelId);
    }
  });

  test('every spec has a non-empty scheduled body', () {
    for (final spec in _allSpecs) {
      expect(spec.scheduledBody, isNotEmpty, reason: spec.channelId);
    }
  });

  group('same-day progress body', () {
    test('hydration shows "x.x L / y.y L"', () {
      expect(
        hydrationReminderNotificationSpec.body(
          const ReminderGoalProgress(current: 1.25, target: 2.0),
        ),
        '1.3 L / 2.0 L',
      );
    });

    test('mindfulness shows whole minutes', () {
      expect(
        mindfulnessReminderNotificationSpec.body(
          const ReminderGoalProgress(current: 4.6, target: 10),
        ),
        '5 / 10 min',
      );
    });
  });

  test('the hydration reminder opens the hydration entry when tapped', () {
    expect(hydrationReminderNotificationSpec.tapRoute, '/manual_entry/hydration');
  });
}
