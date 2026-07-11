import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/reminders/reminder_time_zone.dart';
import 'package:timezone/timezone.dart' as tz;

void main() {
  test('points tz.local at the device time zone', () async {
    final ok = await initializeReminderTimeZone(
      readLocalTimeZoneName: () async => 'Europe/Madrid',
    );

    expect(ok, isTrue);
    expect(tz.local.name, 'Europe/Madrid');

    // The whole point: a wall-clock time now resolves against the device zone,
    // not UTC. Madrid is UTC+2 on this summer date.
    final scheduled = tz.TZDateTime(tz.local, 2026, 6, 1, 9);
    expect(scheduled.toUtc().hour, 7);
  });

  test('an unknown zone name reports failure and keeps the previous zone',
      () async {
    await initializeReminderTimeZone(
      readLocalTimeZoneName: () async => 'Europe/Madrid',
    );

    final ok = await initializeReminderTimeZone(
      readLocalTimeZoneName: () async => 'Not/AZone',
    );

    expect(ok, isFalse);
    // Reloading the database would have reset this to UTC.
    expect(tz.local.name, 'Europe/Madrid');
  });

  test('a platform-channel failure is swallowed', () async {
    // What a unit test or a headless run actually hits.
    final ok = await initializeReminderTimeZone(
      readLocalTimeZoneName: () async => throw StateError('no platform'),
    );

    expect(ok, isFalse);
    // Never throws, so app startup survives it.
  });

  test('is idempotent', () async {
    await initializeReminderTimeZone(
      readLocalTimeZoneName: () async => 'Europe/Madrid',
    );
    final ok = await initializeReminderTimeZone(
      readLocalTimeZoneName: () async => 'Europe/Madrid',
    );
    expect(ok, isTrue);
    expect(tz.local.name, 'Europe/Madrid');
  });
}
