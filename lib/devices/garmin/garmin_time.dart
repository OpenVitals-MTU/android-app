/// Garmin's device epoch: 1989-12-31T00:00:00Z, i.e. 631065600 seconds after
/// the Unix epoch. Port of `GarminTimeUtils.GARMIN_TIME_EPOCH`.
///
/// Directory entries timestamp files in seconds since this epoch. A wire value
/// of 0 is the watch's "no date" sentinel and is surfaced as null by the caller,
/// never as a real instant at the Garmin epoch.
class GarminTime {
  const GarminTime._();

  static const int garminEpochSeconds = 631065600;

  /// A Garmin device timestamp (seconds since the Garmin epoch) as a UTC
  /// [DateTime].
  static DateTime toDateTime(int garminTimestamp) =>
      DateTime.fromMillisecondsSinceEpoch(
        (garminTimestamp + garminEpochSeconds) * 1000,
        isUtc: true,
      );

  /// The inverse — a UTC instant as a Garmin timestamp. Needed when the sync
  /// tells the watch the current time.
  static int fromDateTime(DateTime time) =>
      (time.toUtc().millisecondsSinceEpoch ~/ 1000) - garminEpochSeconds;
}
