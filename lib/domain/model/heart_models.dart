import 'package:freezed_annotation/freezed_annotation.dart';

import '../../core/time/local_date.dart';

part 'heart_models.freezed.dart';

@freezed
abstract class HeartRateSample with _$HeartRateSample {
  const factory HeartRateSample({
    required DateTime time,
    required int beatsPerMinute,
    required String source,
  }) = _HeartRateSample;
}

@freezed
abstract class RestingHeartRateSample with _$RestingHeartRateSample {
  const factory RestingHeartRateSample({
    required DateTime time,
    required int beatsPerMinute,
    required String source,
  }) = _RestingHeartRateSample;
}

@freezed
abstract class HrvSample with _$HrvSample {
  const factory HrvSample({
    required DateTime time,
    required double rmssdMs,
    required String source,
  }) = _HrvSample;
}

@freezed
abstract class HeartRateSummary with _$HeartRateSummary {
  const factory HeartRateSummary({
    required LocalDate date,
    required int avgBpm,
    required int minBpm,
    required int maxBpm,
  }) = _HeartRateSummary;
}

@freezed
abstract class DailyRestingHR with _$DailyRestingHR {
  const factory DailyRestingHR({
    required LocalDate date,
    required int bpm,
  }) = _DailyRestingHR;
}

@freezed
abstract class DailyHrv with _$DailyHrv {
  const factory DailyHrv({
    required LocalDate date,
    required double rmssdMs,
  }) = _DailyHrv;
}
