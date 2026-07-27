import 'package:openvitals/core/time/local_date.dart';

/// A moment [ago] before now, clamped so that it never falls out of *today*.
///
/// Fixtures that want "a couple of hours ago, which is still today" were
/// written as `DateTime.now().subtract(const Duration(hours: 2))`. That is not
/// today when the suite runs at 00:32 — it is yesterday, and the entry silently
/// drops out of the window the test is asserting on. Five tests failed that way,
/// every night between midnight and roughly 06:00, and passed again by morning.
///
/// This never walks past midnight: ask for two hours at 00:32 and you get
/// midnight, which is still today and still in the past.
DateTime earlierToday(Duration ago) {
  final now = DateTime.now();
  final sinceMidnight = Duration(
    hours: now.hour,
    minutes: now.minute,
    seconds: now.second,
    milliseconds: now.millisecond,
  );
  return now.subtract(ago <= sinceMidnight ? ago : sinceMidnight);
}

/// The same, in UTC — for fixtures whose model stores instants in UTC.
DateTime earlierTodayUtc(Duration ago) => earlierToday(ago).toUtc();

/// The local calendar day [days] before [now]'s own local day.
///
/// The rule this exists to make easy: **derive a test's days from [LocalDate],
/// never from hour arithmetic on an instant.**
///
/// A day window is a LOCAL calendar thing, but `now.subtract(Duration(hours: N))`
/// is an absolute one, and the two disagree by the runner's UTC offset. That is
/// not a rounding error — it moves a fixture across a day boundary, so the same
/// test lands on opposite sides of a day-count comparison depending on where it
/// runs. It passed on a UTC+3 laptop and failed on a UTC CI runner, which is the
/// worst way for it to fail: invisible to the person who wrote it.
///
/// [earlierToday] is the sibling for "still today"; this is the sibling for
/// "some whole number of days ago".
LocalDate localDayBefore(DateTime now, int days) =>
    LocalDate.fromDateTime(now.toLocal()).minusDays(days);

/// An instant inside the local day [days] before [now].
///
/// Noon by default, so it cannot drift into a neighbouring day whatever the zone
/// or however long that local day happens to be — a DST day is 23 or 25 hours,
/// and an instant near either edge of it is exactly what this avoids picking.
DateTime instantDaysBefore(DateTime now, int days, {int hour = 12}) =>
    localDayBefore(now, days).atTimeInstant(hour);
