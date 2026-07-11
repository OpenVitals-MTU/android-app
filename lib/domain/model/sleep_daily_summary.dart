import 'dart:math' as math;

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

SleepData? dailySleepSummary(
  List<SleepData> sessions,
  LocalDate selectedDate, {
  SleepRangeMode sleepRangeMode = SleepRangeMode.evening18h,
}) {
  final dailySessions =
      sleepSessionsForRange(sessions, selectedDate, sleepRangeMode);

  if (dailySessions.isEmpty) return null;
  if (dailySessions.length == 1) {
    final single = dailySessions.single;
    final sortedStages = [...single.stages]
      ..sort((a, b) => a.startTime.compareTo(b.startTime));
    return single.copyWith(stages: sortedStages);
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

  final seenStageKeys = <(DateTime, DateTime, int)>{};
  final mergedStages = <SleepStage>[];
  for (final stage in dailySessions.expand((session) => session.stages)) {
    final key = (stage.startTime, stage.endTime, stage.stageType);
    if (seenStageKeys.add(key)) mergedStages.add(stage);
  }
  mergedStages.sort((a, b) {
    final byStart = a.startTime.compareTo(b.startTime);
    if (byStart != 0) return byStart;
    return a.endTime.compareTo(b.endTime);
  });

  return SleepData(
    id: 'daily:$selectedDate',
    startTime: first.startTime,
    endTime: last.endTime,
    durationMs: dailySessions.fold<int>(
      0,
      (sum, session) => sum + math.max(session.durationMs, 0),
    ),
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
