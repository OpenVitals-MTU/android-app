import '../../core/time/local_date.dart';
import '../preferences/sleep_range_mode.dart';
import 'sleep_models.dart';

class SleepRangeWindow {
  const SleepRangeWindow({required this.start, required this.end});

  final DateTime start;
  final DateTime end;
}

SleepRangeWindow sleepRangeWindowFor(
  LocalDate selectedDate,
  SleepRangeMode sleepRangeMode,
) =>
    SleepRangeWindow(
      start: sleepRangeStartFor(selectedDate, sleepRangeMode),
      end: sleepRangeEndFor(selectedDate, sleepRangeMode),
    );

/// The device-local instant at which the sleep window for [selectedDate]
/// begins. In Kotlin this returns a `LocalDateTime`; here it is resolved to the
/// device-zone instant directly.
DateTime sleepRangeStartFor(
  LocalDate selectedDate,
  SleepRangeMode sleepRangeMode,
) {
  switch (sleepRangeMode) {
    case SleepRangeMode.rolling24h:
      return selectedDate.atTimeInstant(0);
    case SleepRangeMode.noon:
      return selectedDate.minusDays(1).atTimeInstant(12);
    case SleepRangeMode.evening18h:
      return selectedDate.minusDays(1).atTimeInstant(18);
  }
}

DateTime sleepRangeEndFor(
  LocalDate selectedDate,
  SleepRangeMode sleepRangeMode,
) {
  switch (sleepRangeMode) {
    case SleepRangeMode.rolling24h:
      return selectedDate.plusDays(1).atTimeInstant(0);
    case SleepRangeMode.noon:
      return selectedDate.atTimeInstant(12);
    case SleepRangeMode.evening18h:
      return selectedDate.atTimeInstant(18);
  }
}

List<SleepData> sleepSessionsForRange(
  List<SleepData> sessions,
  LocalDate selectedDate,
  SleepRangeMode sleepRangeMode,
) {
  final window = sleepRangeWindowFor(selectedDate, sleepRangeMode);
  final filtered =
      sessions.where((session) => _containsEnd(window, session)).toList();
  filtered.sort((a, b) {
    final byStart = a.startTime.compareTo(b.startTime);
    if (byStart != 0) return byStart;
    return a.endTime.compareTo(b.endTime);
  });
  return filtered;
}

/// The gap that separates the night's sleep from a daytime nap. A short
/// early-morning wake (well under this) keeps one night together; a clearly
/// daytime session sits on the far side of a gap this large and reads as a nap.
const Duration kSleepNapGap = Duration(hours: 3);

/// A day's sleep split into the main night and any daytime naps.
class SleepNightSplit {
  const SleepNightSplit({required this.night, required this.naps});

  /// The sessions that make up the main nocturnal sleep, sorted by start. May be
  /// several segments if the night was broken by a wake.
  final List<SleepData> night;

  /// Sessions outside the night — daytime naps — sorted by start.
  final List<SleepData> naps;
}

/// Splits already-windowed sessions into the main night and daytime naps.
///
/// Sessions are clustered by time: a gap larger than [napGap] between one and the
/// next starts a new cluster. The night is the cluster with the greatest total
/// wall-clock time; every other cluster is naps. So a night broken by a
/// 1h40m wake stays one night, while an afternoon nap 8h later splits off.
SleepNightSplit splitNightAndNaps(
  List<SleepData> windowedSessions, {
  Duration napGap = kSleepNapGap,
}) {
  if (windowedSessions.isEmpty) {
    return const SleepNightSplit(night: [], naps: []);
  }

  final sorted = [...windowedSessions]..sort(_byStartThenEnd);
  final clusters = <List<SleepData>>[];
  var current = <SleepData>[sorted.first];
  var clusterEnd = sorted.first.endTime;
  for (final session in sorted.skip(1)) {
    if (session.startTime.difference(clusterEnd) > napGap) {
      clusters.add(current);
      current = <SleepData>[session];
      clusterEnd = session.endTime;
    } else {
      current.add(session);
      if (session.endTime.isAfter(clusterEnd)) clusterEnd = session.endTime;
    }
  }
  clusters.add(current);

  List<SleepData> night = const [];
  var bestTotal = -1;
  for (final cluster in clusters) {
    final total = sleepSessionsUnionMs(cluster);
    if (total > bestTotal) {
      bestTotal = total;
      night = cluster;
    }
  }

  final naps = <SleepData>[
    for (final cluster in clusters)
      if (!identical(cluster, night)) ...cluster,
  ]..sort(_byStartThenEnd);

  return SleepNightSplit(night: night, naps: naps);
}

int _byStartThenEnd(SleepData a, SleepData b) {
  final byStart = a.startTime.compareTo(b.startTime);
  if (byStart != 0) return byStart;
  return a.endTime.compareTo(b.endTime);
}

/// The daytime naps for [selectedDate], reported separately from the night.
List<SleepData> dailyNaps(
  List<SleepData> sessions,
  LocalDate selectedDate, {
  SleepRangeMode sleepRangeMode = SleepRangeMode.evening18h,
}) =>
    splitNightAndNaps(
      sleepSessionsForRange(sessions, selectedDate, sleepRangeMode),
    ).naps;

SleepData? dailySleepSummary(
  List<SleepData> sessions,
  LocalDate selectedDate, {
  SleepRangeMode sleepRangeMode = SleepRangeMode.evening18h,
}) {
  final windowed =
      sleepSessionsForRange(sessions, selectedDate, sleepRangeMode);
  // Naps are reported separately; the night's summary is the night only, and its
  // duration is wall-clock time in bed — the union of the night's segments, so
  // two overlapping sessions from different sources count their shared time once
  // instead of summing into an impossible total.
  final dailySessions = splitNightAndNaps(windowed).night;
  final nightDurationMs = sleepSessionsUnionMs(dailySessions);

  if (dailySessions.isEmpty) return null;
  if (dailySessions.length == 1) {
    final single = dailySessions.single;
    final sortedStages = [...single.stages]
      ..sort((a, b) => a.startTime.compareTo(b.startTime));
    return single.copyWith(stages: sortedStages, durationMs: nightDurationMs);
  }

  final first = dailySessions.first;
  final last = dailySessions
      .reduce((a, b) => b.endTime.isAfter(a.endTime) ? b : a);
  final distinctSources =
      _distinct(dailySessions.map((session) => session.source).toList());

  final titles = _distinct(
    dailySessions
        .map((session) => session.title)
        .whereType<String>()
        .where((title) => title.trim().isNotEmpty)
        .toList(),
  );
  final notes = _distinct(
    dailySessions
        .map((session) => session.notes)
        .whereType<String>()
        .where((note) => note.trim().isNotEmpty)
        .toList(),
  );
  final modifiedTimes = dailySessions
      .map((session) => session.lastModifiedTime)
      .whereType<DateTime>()
      .toList();
  final recordingMethods = _distinct(
    dailySessions
        .map((session) => session.recordingMethod)
        .whereType<int>()
        .toList(),
  );
  final devices = _distinct(
    dailySessions
        .map((session) => session.device)
        .whereType<SleepDeviceData>()
        .toList(),
  );

  // A night can be several segments split by a wake (05:18–07:34 here). Combine
  // them into one continuous stage timeline with the wake filled as Awake, so
  // the span is covered by stages: otherwise the un-slept gap counts against
  // sleepSessionHasReliableStages and the day view hides the hypnogram, and the
  // schedule bar shows a hole. Gaps stay within the night by splitNightAndNaps
  // (<= kSleepNapGap), so filling up to that bound never bridges a daytime nap.
  final mergedStages =
      combineNightStages(dailySessions, maxGap: kSleepNapGap);

  return SleepData(
    id: 'daily:$selectedDate',
    startTime: first.startTime,
    endTime: last.endTime,
    durationMs: nightDurationMs,
    source: _singleOrNull(distinctSources) ?? first.source,
    title: _singleOrNull(titles) ?? first.title,
    notes: _singleOrNull(notes),
    startZoneOffset: first.startZoneOffset,
    endZoneOffset: last.endZoneOffset,
    lastModifiedTime: modifiedTimes.isEmpty
        ? null
        : modifiedTimes.reduce((a, b) => a.isAfter(b) ? a : b),
    clientRecordId: null,
    clientRecordVersion: null,
    recordingMethod: _singleOrNull(recordingMethods),
    device: _singleOrNull(devices),
    stages: mergedStages,
  );
}

bool _containsEnd(SleepRangeWindow window, SleepData session) =>
    !session.endTime.isBefore(window.start) &&
    session.endTime.isBefore(window.end);

List<T> _distinct<T>(List<T> items) {
  final seen = <T>{};
  final result = <T>[];
  for (final item in items) {
    if (seen.add(item)) result.add(item);
  }
  return result;
}

T? _singleOrNull<T>(List<T> items) => items.length == 1 ? items.first : null;
