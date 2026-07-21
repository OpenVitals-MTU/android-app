import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/data/repository/impl/sleep_repository_impl.dart';
import 'package:openvitals/data/source/health/health_data_source.dart';
import 'package:openvitals/domain/model/sleep_models.dart';
import 'package:openvitals/domain/model/sleep_session_merging.dart';

/// Serves canned raw sleep records by their Health Connect id.
class _FakeSource extends HealthDataSource {
  _FakeSource(this._byId);

  final Map<String, SleepData> _byId;
  final calls = <String>[];

  @override
  Future<SleepData?> readSleepSession(String id) async {
    calls.add(id);
    return _byId[id];
  }
}

SleepData _raw(String id, DateTime start, DateTime end) => SleepData(
      id: id,
      startTime: start,
      endTime: end,
      durationMs: end.difference(start).inMilliseconds,
      source: 'nodomain.freeyourgadget.gadgetbridge',
      stages: [
        SleepStage(
          startTime: start,
          endTime: end,
          stageType: SleepStage.stageLight,
        ),
      ],
    );

void main() {
  group('SleepRepositoryImpl.loadSleepSession', () {
    test('reconstructs a merged night from its component records', () async {
      // Two same-source records a few minutes apart — exactly what the list
      // merges into one night with a synthetic `merged:…` id.
      final first = _raw(
        'hc-1',
        DateTime.utc(2026, 7, 20, 22, 30),
        DateTime.utc(2026, 7, 21, 2, 0),
      );
      final second = _raw(
        'hc-2',
        DateTime.utc(2026, 7, 21, 2, 30),
        DateTime.utc(2026, 7, 21, 7, 0),
      );
      final mergedId = mergeSleepSessions([first, second]).single.id;
      expect(mergedSleepSessionComponentIds(mergedId), ['hc-1', 'hc-2']);

      final source = _FakeSource({'hc-1': first, 'hc-2': second});
      final repository = SleepRepositoryImpl(source);

      final result = await repository.loadSleepSession(mergedId);

      expect(result, isA<Ok<SleepData?>>());
      final session = (result as Ok<SleepData?>).value;
      expect(session, isNotNull);
      expect(session!.id, mergedId);
      expect(session.startTime, first.startTime);
      expect(session.endTime, second.endTime);
      expect(source.calls, ['hc-1', 'hc-2']);
    });

    test('reads a plain record id straight through', () async {
      final record = _raw(
        's1',
        DateTime.utc(2026, 7, 20, 23, 0),
        DateTime.utc(2026, 7, 21, 7, 0),
      );
      final source = _FakeSource({'s1': record});
      final repository = SleepRepositoryImpl(source);

      final result = await repository.loadSleepSession('s1');

      expect((result as Ok<SleepData?>).value?.id, 's1');
      expect(source.calls, ['s1']);
    });

    test('is not-found when every component record has since vanished',
        () async {
      final first = _raw(
        'gone-1',
        DateTime.utc(2026, 7, 20, 22, 30),
        DateTime.utc(2026, 7, 21, 2, 0),
      );
      final second = _raw(
        'gone-2',
        DateTime.utc(2026, 7, 21, 2, 30),
        DateTime.utc(2026, 7, 21, 7, 0),
      );
      final mergedId = mergeSleepSessions([first, second]).single.id;

      final source = _FakeSource(const {});
      final repository = SleepRepositoryImpl(source);

      final result = await repository.loadSleepSession(mergedId);

      expect((result as Ok<SleepData?>).value, isNull);
    });
  });
}
