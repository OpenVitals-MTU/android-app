import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../core/time/local_date.dart';
import '../../../domain/insights/sleep_score.dart';
import '../../../domain/model/sleep_models.dart';
import '../../../domain/usecase/load_recovery_days_use_case.dart';

part 'recovery_detail_display.freezed.dart';

/// The screen-ready derivation of a loaded recovery week: the selected day
/// picked out of it, its score, and the night the schedule line reads.
///
/// Built once per load by [buildRecoveryDetailDisplay] and stored on the state —
/// the two detail screens (sleep score, sleep efficiency) render it and scan
/// nothing.
@freezed
abstract class RecoveryDetailDisplay with _$RecoveryDetailDisplay {
  const RecoveryDetailDisplay._();

  const factory RecoveryDetailDisplay({
    required RecoveryDay day,
    required SleepScoreEstimate estimate,

    /// The session with the most stage-derived sleep, or null on a blank night.
    required SleepData? mainSleepSession,
  }) = _RecoveryDetailDisplay;

  bool get hasScore => estimate.confidence != SleepScoreConfidence.noData;
}

/// Pure derivation from the loaded week to the selected day's display model. No
/// clock, no I/O — [selectedDate] is handed in, not read off `LocalDate.now()`.
///
/// Kotlin `RecoveryUiState.today`: a day the lookback window did not reach is a
/// blank [RecoveryDay], not an error.
RecoveryDetailDisplay buildRecoveryDetailDisplay(
  List<RecoveryDay> days,
  LocalDate selectedDate,
) {
  var day = RecoveryDay(date: selectedDate);
  for (final candidate in days) {
    if (candidate.date == selectedDate) {
      day = candidate;
      break;
    }
  }
  return RecoveryDetailDisplay(
    day: day,
    estimate: day.sleepScore,
    mainSleepSession: day.mainSleepSession,
  );
}
