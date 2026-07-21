import 'dart:convert';
import 'dart:math' as math;

import 'sleep_models.dart';

const String _mergedSleepSessionIdPrefix = 'merged:';
const String _mergedSleepSessionIdSeparator = '.';
const double _duplicateSleepOverlapRatio = 0.85;
// Mirrors Gadgetbridge's sleep-session analysis: short quiet wake/no-data gaps
// keep one night together.
const Duration _defaultSleepSessionMergeGap = Duration(minutes: 60);

final DateTime _epoch = DateTime.fromMillisecondsSinceEpoch(0, isUtc: true);

List<SleepData> mergeSleepSessions(
  List<SleepData> sessions, {
  Duration maxGap = _defaultSleepSessionMergeGap,
}) {
  if (sessions.length < 2) {
    return [...sessions]..sort((a, b) => b.endTime.compareTo(a.endTime));
  }

  final groups = <List<SleepData>>[];
  final currentGroup = <SleepData>[];

  final sorted = [...sessions]..sort(_byStartThenEnd);
  for (final session in sorted) {
    final currentEnd = currentGroup.isEmpty
        ? null
        : currentGroup
            .map((entry) => entry.endTime)
            .reduce((a, b) => a.isAfter(b) ? a : b);
    if (currentGroup.isEmpty ||
        (currentEnd != null &&
            _shouldMergeSleepSessions(
              currentGroup,
              currentEnd,
              session,
              maxGap,
            ))) {
      currentGroup.add(session);
    } else {
      groups.add([...currentGroup]);
      currentGroup.clear();
      currentGroup.add(session);
    }
  }

  if (currentGroup.isNotEmpty) groups.add([...currentGroup]);

  final merged =
      groups.map((group) => _toMergedSleepSession(group, maxGap)).toList();
  final deduplicated = _deduplicateOverlappingSleepSessions(merged);
  return deduplicated..sort((a, b) => b.endTime.compareTo(a.endTime));
}

List<String>? mergedSleepSessionComponentIds(String id) {
  if (!id.startsWith(_mergedSleepSessionIdPrefix)) return null;
  final encodedIds = id
      .substring(_mergedSleepSessionIdPrefix.length)
      .split(_mergedSleepSessionIdSeparator)
      .where((value) => value.trim().isNotEmpty)
      .toList();

  if (encodedIds.isEmpty) return null;

  try {
    return encodedIds
        .map((encodedId) => utf8.decode(base64Url.decode(_padBase64(encodedId))))
        .toList();
  } catch (_) {
    return null;
  }
}

bool _shouldMergeSleepSessions(
  List<SleepData> currentGroup,
  DateTime currentEnd,
  SleepData nextSession,
  Duration maxGap,
) {
  if (currentGroup.isEmpty) return false;
  final source = currentGroup.first.source;
  if (nextSession.source != source) return false;

  final gap = nextSession.startTime.difference(currentEnd);
  return gap <= maxGap;
}

SleepData _toMergedSleepSession(List<SleepData> group, Duration maxGap) {
  if (group.length == 1) {
    final single = group.single;
    final sortedStages = [...single.stages]
      ..sort((a, b) => a.startTime.compareTo(b.startTime));
    return single.copyWith(stages: sortedStages);
  }

  final ordered = [...group]..sort(_byStartThenEnd);
  final first = ordered.first;
  final last = ordered.reduce((a, b) => b.endTime.isAfter(a.endTime) ? b : a);
  final distinctSources =
      _distinct(ordered.map((session) => session.source).toList());

  final titles = _distinct(
    ordered
        .map((session) => session.title)
        .whereType<String>()
        .where((title) => title.trim().isNotEmpty)
        .toList(),
  );
  final notes = _distinct(
    ordered
        .map((session) => session.notes)
        .whereType<String>()
        .where((note) => note.trim().isNotEmpty)
        .toList(),
  );
  final modifiedTimes = ordered
      .map((session) => session.lastModifiedTime)
      .whereType<DateTime>()
      .toList();
  final recordingMethods = _distinct(
    ordered
        .map((session) => session.recordingMethod)
        .whereType<int>()
        .toList(),
  );
  final devices = _distinct(
    ordered
        .map((session) => session.device)
        .whereType<SleepDeviceData>()
        .toList(),
  );

  return SleepData(
    id: _mergedSleepSessionId(ordered.map((session) => session.id).toList()),
    startTime: first.startTime,
    endTime: last.endTime,
    durationMs: ordered.fold<int>(
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
    stages: combineNightStages(ordered, maxGap: maxGap),
  );
}

List<SleepData> _deduplicateOverlappingSleepSessions(List<SleepData> sessions) {
  if (sessions.length < 2) return sessions;

  final kept = <SleepData>[];
  final sorted = [...sessions]..sort(_byStartThenEnd);
  for (final session in sorted) {
    final duplicateIndex = kept.indexWhere(
      (existing) => _isDuplicateSleepSession(existing, session),
    );
    if (duplicateIndex == -1) {
      kept.add(session);
    } else {
      kept[duplicateIndex] = _richerSleepSession(kept[duplicateIndex], session);
    }
  }
  return kept;
}

bool _isDuplicateSleepSession(SleepData session, SleepData other) {
  if (session.source == other.source) return false;

  final shorterDuration =
      math.min(math.max(session.durationMs, 0), math.max(other.durationMs, 0));
  if (shorterDuration <= 0) return false;

  final overlapMs = math.min(
        session.endTime.millisecondsSinceEpoch,
        other.endTime.millisecondsSinceEpoch,
      ) -
      math.max(
        session.startTime.millisecondsSinceEpoch,
        other.startTime.millisecondsSinceEpoch,
      );
  if (overlapMs <= 0) return false;

  // A high overlap of the *shorter* session is enough on its own: two
  // different-source sessions covering the same physical sleep are duplicates
  // even when their boundaries drift (accelerometer autodetect and a wearable
  // rarely agree on the exact wake time). The old symmetric boundary tolerance
  // wrongly kept such pairs, so their durations were summed.
  return overlapMs / shorterDuration >= _duplicateSleepOverlapRatio;
}

SleepData _richerSleepSession(SleepData first, SleepData second) =>
    _compareSleepRichness(first, second) >= 0 ? first : second;

int _compareSleepRichness(SleepData a, SleepData b) {
  final byScore = _sleepRichnessScore(a).compareTo(_sleepRichnessScore(b));
  if (byScore != 0) return byScore;
  final byDuration = a.durationMs.compareTo(b.durationMs);
  if (byDuration != 0) return byDuration;
  return (a.lastModifiedTime ?? _epoch).compareTo(b.lastModifiedTime ?? _epoch);
}

int _sleepRichnessScore(SleepData session) {
  final metadata = <Object?>[
    session.device,
    session.recordingMethod,
    session.clientRecordId,
    session.clientRecordVersion,
  ].where((value) => value != null).length;

  return math.min(session.stages.length, 200) * 10 +
      metadata * 5 +
      (_isNotBlank(session.title) ? 3 : 0) +
      (_isNotBlank(session.notes) ? 3 : 0);
}

String _mergedSleepSessionId(List<String> ids) {
  final encoded = _distinct(ids)
      .map((id) => base64Url.encode(utf8.encode(id)).replaceAll('=', ''))
      .join(_mergedSleepSessionIdSeparator);
  return '$_mergedSleepSessionIdPrefix$encoded';
}

String _padBase64(String value) {
  final remainder = value.length % 4;
  if (remainder == 0) return value;
  return value + ('=' * (4 - remainder));
}

int _byStartThenEnd(SleepData a, SleepData b) {
  final byStart = a.startTime.compareTo(b.startTime);
  if (byStart != 0) return byStart;
  return a.endTime.compareTo(b.endTime);
}

bool _isNotBlank(String? value) => value != null && value.trim().isNotEmpty;

List<T> _distinct<T>(List<T> items) {
  final seen = <T>{};
  final result = <T>[];
  for (final item in items) {
    if (seen.add(item)) result.add(item);
  }
  return result;
}

T? _singleOrNull<T>(List<T> items) => items.length == 1 ? items.first : null;
