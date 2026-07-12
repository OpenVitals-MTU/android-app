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
