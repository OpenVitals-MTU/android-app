part of 'apple_health_import_converter.dart';

/// Sleep-stage grouping and session synthesis, ported from the Kotlin
/// `AppleHealthImportSleepConversions.kt`.
extension AppleHealthImportSleepConversions on AppleHealthImportConverter {
  List<ConvertedAppleRecord> convertSleep(
    List<AppleRecord> records, {
    bool trackConsumedRecords = true,
  }) {
    final sleepRecords =
        records.where((it) => it.type == appleSleepAnalysis).toList();
    if (sleepRecords.isEmpty) return const [];
    if (trackConsumedRecords) {
      for (final record in sleepRecords) {
        consumedRecordFingerprints.add(record.sourceFingerprint);
      }
    }

    final groups = <String, List<SleepStageCandidate>>{};
    for (final record in sleepRecords) {
      final start = record.startDate;
      if (start == null) {
        invalidRecord(record, 'Sleep record is missing startDate.');
        continue;
      }
      final end = record.endDate;
      if (end == null) {
        invalidRecord(record, 'Sleep record is missing endDate.');
        continue;
      }
      final stage = mapSleepStageType(record.rawValue);
      if (stage == null) {
        invalidRecord(record, 'Sleep stage value is unsupported.');
        continue;
      }
      final key = '${record.sourceName ?? ''}|${record.device ?? ''}';
      groups.putIfAbsent(key, () => []).add(
            SleepStageCandidate(
              record: record,
              start: start,
              end: end,
              stage: stage,
              inBedOnly: record.rawValue == appleSleepInBed,
            ),
          );
    }

    final result = <ConvertedAppleRecord>[];
    for (final candidates in groups.values) {
      final sorted = List<SleepStageCandidate>.of(candidates)
        ..sort((a, b) => a.start.instant.compareTo(b.start.instant));
      for (final session in splitSleepSessions(sorted)) {
        final converted = _buildSleepSession(session);
        if (converted != null) result.add(converted);
      }
    }
    return result;
  }

  ConvertedAppleRecord? _buildSleepSession(List<SleepStageCandidate> session) {
    var sessionStart = session.first.start.instant;
    var sessionEnd = session.first.end.instant;
    for (final candidate in session) {
      if (candidate.start.instant.isBefore(sessionStart)) {
        sessionStart = candidate.start.instant;
      }
      if (candidate.end.instant.isAfter(sessionEnd)) {
        sessionEnd = candidate.end.instant;
      }
    }
    if (!sessionEnd.isAfter(sessionStart)) {
      invalid(
        appleSleepAnalysis,
        'Sleep session has no positive duration.',
        '${sessionStart.toIso8601String()}..${sessionEnd.toIso8601String()}',
      );
      return null;
    }

    var detailedStages = session.where((it) => !it.inBedOnly).toList();
    if (detailedStages.isEmpty) detailedStages = session;
    detailedStages.sort((a, b) => a.start.instant.compareTo(b.start.instant));

    final stages = <SleepStageValue>[];
    for (final candidate in detailedStages) {
      final lowerBound = stages.isEmpty ? sessionStart : stages.last.endTime;
      final clippedStart = candidate.start.instant.isAfter(lowerBound)
          ? candidate.start.instant
          : lowerBound;
      final clippedEnd = candidate.end.instant.isBefore(sessionEnd)
          ? candidate.end.instant
          : sessionEnd;
      if (clippedEnd.isAfter(clippedStart)) {
        stages.add(SleepStageValue(
          startTime: clippedStart,
          endTime: clippedEnd,
          stage: candidate.stage,
        ));
      }
    }
    if (stages.isEmpty) {
      invalid(
        appleSleepAnalysis,
        'Sleep session did not contain any valid non-overlapping stages.',
        '${sessionStart.toIso8601String()}..${sessionEnd.toIso8601String()}',
      );
      return null;
    }

    final first = session.first.record;
    final fingerprint = buildStableClientRecordId('sleep', [
      'sleep',
      sessionStart.toIso8601String(),
      sessionEnd.toIso8601String(),
      session.map((it) => it.record.stableParts()).join(';'),
    ]);
    markConverted(appleSleepAnalysis);
    return ConvertedAppleRecord(
      appleType: appleSleepAnalysis,
      targetType: 'SleepSessionRecord',
      fingerprint: fingerprint,
      record: SleepSessionImportRecord(
        clientRecordId: fingerprint,
        startTime: sessionStart,
        startZoneOffset: first.startDate?.offset,
        endTime: sessionEnd,
        endZoneOffset: first.endDate?.offset ?? first.startDate?.offset,
        title: 'Apple Health sleep',
        stages: stages,
      ),
      sourceTimeRange: AppleImportTimeRange(sessionStart, sessionEnd),
      unit: null,
      value: 'stages=${stages.length}',
    );
  }
}
