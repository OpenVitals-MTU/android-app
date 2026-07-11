import 'package:flutter_timezone/flutter_timezone.dart';
import 'package:timezone/data/latest_all.dart' as tz_data;
import 'package:timezone/timezone.dart' as tz;

/// Loads the IANA time-zone database and points `tz.local` at the device's zone.
///
/// Required before any `zonedSchedule` call: `tz.local` defaults to UTC, so a
/// reminder set for 08:00 would fire at 08:00 UTC. On Android the reminder path
/// goes through the alarm manager and never touches `tz`, but the notification
/// fallback (iOS, and Android when an exact alarm cannot be armed) does.
///
/// Safe to call more than once. Never throws: if the platform channel is
/// unavailable — a unit test, a headless run — [tz.local] is left at UTC and
/// [initializeReminderTimeZone] reports false, so a startup failure here can
/// never take the app down with it.
Future<bool> initializeReminderTimeZone({
  Future<String> Function()? readLocalTimeZoneName,
}) async {
  // Guarded: `initializeTimeZones` resets `tz.local` back to UTC, so an
  // unguarded second call followed by a failed lookup would silently drop an
  // already-correct local zone.
  if (!tz.timeZoneDatabase.isInitialized) {
    tz_data.initializeTimeZones();
  }
  try {
    final name = await (readLocalTimeZoneName ?? _platformTimeZoneName)();
    tz.setLocalLocation(tz.getLocation(name));
    return true;
  } catch (_) {
    // `tz.local` stays UTC. Reminders still fire, just against UTC wall-clock,
    // which is the same behaviour as before this was wired at all.
    return false;
  }
}

Future<String> _platformTimeZoneName() async =>
    (await FlutterTimezone.getLocalTimezone()).identifier;
