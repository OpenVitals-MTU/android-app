import 'package:freezed_annotation/freezed_annotation.dart';

import '../model/mindfulness_models.dart';

part 'mindfulness_period_data.freezed.dart';

@freezed
abstract class MindfulnessPeriodData with _$MindfulnessPeriodData {
  const factory MindfulnessPeriodData({
    @Default(<MindfulnessSession>[]) List<MindfulnessSession> sessions,
    @Default(<MindfulnessSession>[]) List<MindfulnessSession> previousSessions,
    @Default(<MindfulnessSession>[]) List<MindfulnessSession> baselineSessions,
  }) = _MindfulnessPeriodData;
}
