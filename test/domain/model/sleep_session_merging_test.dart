import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/domain/model/sleep_models.dart';
import 'package:openvitals/domain/model/sleep_session_merging.dart';

SleepData _sleep({
  required String id,
  String source = 'gadgetbridge',
  String start = '2026-05-06T01:00:00Z',
  String end = '2026-05-06T02:00:00Z',
  List<SleepStage> stages = const [],
}) {
  final startTime = DateTime.parse(start);
  final endTime = DateTime.parse(end);
  return SleepData(
    id: id,
    startTime: startTime,
    endTime: endTime,
    durationMs: endTime.difference(startTime).inMilliseconds,
    source: source,
    stages: stages,
  );
}

SleepStage _stage(String start, String end, int type) => SleepStage(
      startTime: DateTime.parse(start),
      endTime: DateTime.parse(end),
      stageType: type,
    );

void main() {
  test('mergeSleepSessions merges same-source sessions separated by a short gap',
      () {
    final first = _sleep(
      id: 'first',
      start: '2026-05-06T00:22:00Z',
      end: '2026-05-06T04:07:00Z',
      stages: [
        _stage('2026-05-06T00:22:00Z', '2026-05-06T04:07:00Z',
            SleepStage.stageLight),
      ],
    );
    final second = _sleep(
      id: 'second',
      start: '2026-05-06T04:22:00Z',
      end: '2026-05-06T07:03:00Z',
      stages: [
        _stage('2026-05-06T04:22:00Z', '2026-05-06T07:03:00Z',
            SleepStage.stageRem),
      ],
    );

    final merged = mergeSleepSessions([second, first]);

    expect(merged.length, 1);
    expect(merged.single.startTime, first.startTime);
    expect(merged.single.endTime, second.endTime);
    expect(merged.single.durationMs, first.durationMs + second.durationMs);
    expect(merged.single.id, isNot('first'));
    expect(
      mergedSleepSessionComponentIds(merged.single.id),
      ['first', 'second'],
    );
    expect(
      merged.single.stages.map((stage) => stage.stageType).toList(),
      [SleepStage.stageLight, SleepStage.stageAwake, SleepStage.stageRem],
    );
  });

  test(
      'mergeSleepSessions merges same-source sessions separated by up to sixty minutes',
      () {
    final first = _sleep(
      id: 'first',
      start: '2026-05-06T00:22:00Z',
      end: '2026-05-06T04:07:00Z',
    );
    final second = _sleep(
      id: 'second',
      start: '2026-05-06T05:07:00Z',
      end: '2026-05-06T07:03:00Z',
    );

    final merged = mergeSleepSessions([first, second]);

    expect(merged.length, 1);
    expect(merged.single.startTime, first.startTime);
    expect(merged.single.endTime, second.endTime);
  });

  test(
      'mergeSleepSessions carries a pre-midnight split into the final sleep-ending day',
      () {
    final first = _sleep(
      id: 'before-midnight',
      start: '2026-05-05T22:45:00Z',
      end: '2026-05-05T23:59:00Z',
    );
    final second = _sleep(
      id: 'after-midnight',
      start: '2026-05-06T00:03:00Z',
      end: '2026-05-06T06:50:00Z',
    );

    final merged = mergeSleepSessions([first, second]);

    expect(merged.length, 1);
    expect(merged.single.startTime, DateTime.parse('2026-05-05T22:45:00Z'));
    expect(merged.single.endTime, DateTime.parse('2026-05-06T06:50:00Z'));
  });

  test('mergeSleepSessions excludes bridged gaps from displayed sleep duration',
      () {
    final first = _sleep(
      id: 'first',
      start: '2026-05-06T00:00:00Z',
      end: '2026-05-06T04:00:00Z',
    );
    final second = _sleep(
      id: 'second',
      start: '2026-05-06T04:30:00Z',
      end: '2026-05-06T06:30:00Z',
    );

    final merged = mergeSleepSessions([first, second]);

    expect(merged.length, 1);
    expect(merged.single.durationMs, const Duration(hours: 6).inMilliseconds);
    expect(
      second.endTime.difference(first.startTime).inMilliseconds,
      const Duration(hours: 6, minutes: 30).inMilliseconds,
    );
  });

  test('sleepDurationMsFromStages excludes awake stages when sleep present', () {
    final stages = [
      _stage('2026-05-06T00:00:00Z', '2026-05-06T04:00:00Z',
          SleepStage.stageLight),
      _stage('2026-05-06T04:00:00Z', '2026-05-06T04:30:00Z',
          SleepStage.stageAwake),
      _stage('2026-05-06T04:30:00Z', '2026-05-06T06:30:00Z',
          SleepStage.stageRem),
    ];

    final durationMs = sleepDurationMsFromStages(
      stages,
      const Duration(hours: 6, minutes: 30).inMilliseconds,
    );

    expect(durationMs, const Duration(hours: 6).inMilliseconds);
  });

  test('mergeSleepSessions does not merge sessions from different sources', () {
    final first = _sleep(
      id: 'gadgetbridge',
      source: 'gadgetbridge',
      start: '2026-05-06T00:22:00Z',
      end: '2026-05-06T04:07:00Z',
    );
    final second = _sleep(
      id: 'watch',
      source: 'watch',
      start: '2026-05-06T04:22:00Z',
      end: '2026-05-06T07:03:00Z',
    );

    final merged = mergeSleepSessions([first, second]);

    expect(merged.length, 2);
  });

  test(
      'mergeSleepSessions removes overlapping duplicate sessions from different sources',
      () {
    final googleFit = _sleep(
      id: 'google-fit',
      source: 'google-fit',
      start: '2026-05-06T22:00:00Z',
      end: '2026-05-07T06:00:00Z',
    );
    final watch = _sleep(
      id: 'watch',
      source: 'watch',
      start: '2026-05-06T22:05:00Z',
      end: '2026-05-07T06:05:00Z',
      stages: [
        _stage('2026-05-06T22:05:00Z', '2026-05-07T06:05:00Z',
            SleepStage.stageLight),
      ],
    );

    final merged = mergeSleepSessions([googleFit, watch]);

    expect(merged.length, 1);
    expect(merged.single.id, 'watch');
  });

  test(
      'mergeSleepSessions removes a near-total cross-source duplicate whose end drifts past the old tolerance',
      () {
    // The reported bug: Fitbit + Sleep-as-Android autodetect for one night.
    // ~100% overlap of the shorter session, but the ends differ by 48 min —
    // the old 30-min boundary gate wrongly kept both, summing to ~11h26m.
    final fitbit = _sleep(
      id: 'fitbit',
      source: 'com.fitbit.FitbitMobile',
      start: '2026-07-14T01:16:00Z',
      end: '2026-07-14T07:28:00Z',
      stages: [
        _stage('2026-07-14T01:16:00Z', '2026-07-14T07:28:00Z',
            SleepStage.stageLight),
      ],
    );
    final sleepAsAndroid = _sleep(
      id: 'sleep-as-android',
      source: 'com.urbandroid.sleep',
      start: '2026-07-14T01:15:00Z',
      end: '2026-07-14T06:40:00Z',
    );

    final merged = mergeSleepSessions([sleepAsAndroid, fitbit]);

    expect(merged.length, 1);
    // The richer session (Fitbit, with a hypnogram) is kept.
    expect(merged.single.id, 'fitbit');
    expect(merged.single.durationMs, fitbit.durationMs);
  });

  test(
      'mergeSleepSessions keeps two different-source sessions that overlap under the ratio',
      () {
    // Only ~50% of the shorter session overlaps — genuinely distinct, kept.
    final first = _sleep(
      id: 'source-a',
      source: 'source-a',
      start: '2026-05-06T00:00:00Z',
      end: '2026-05-06T02:00:00Z',
    );
    final second = _sleep(
      id: 'source-b',
      source: 'source-b',
      start: '2026-05-06T01:00:00Z',
      end: '2026-05-06T03:00:00Z',
    );

    final merged = mergeSleepSessions([first, second]);

    expect(merged.length, 2);
  });

  test('mergeSleepSessions does not merge sessions beyond the max gap', () {
    final first = _sleep(
      id: 'nap',
      start: '2026-05-06T18:00:00Z',
      end: '2026-05-06T19:00:00Z',
    );
    final second = _sleep(
      id: 'night',
      start: '2026-05-06T22:30:00Z',
      end: '2026-05-07T06:30:00Z',
    );

    final merged = mergeSleepSessions([first, second]);

    expect(merged.length, 2);
  });

  test('mergedSleepSessionComponentIds returns null for raw or invalid ids', () {
    expect(mergedSleepSessionComponentIds('raw-id'), isNull);
    expect(mergedSleepSessionComponentIds('merged:not valid base64'), isNull);
  });

  test('mergedSleepSessionComponentIds returns encoded raw ids', () {
    final merged = mergeSleepSessions(
      [
        _sleep(id: 'alpha'),
        _sleep(
          id: 'beta',
          start: '2026-05-06T02:10:00Z',
          end: '2026-05-06T03:00:00Z',
        ),
      ],
      maxGap: const Duration(hours: 2),
    ).single;

    final componentIds = mergedSleepSessionComponentIds(merged.id);
    expect(componentIds, isNotNull);
    expect(componentIds, containsAll(['alpha', 'beta']));
  });
}
