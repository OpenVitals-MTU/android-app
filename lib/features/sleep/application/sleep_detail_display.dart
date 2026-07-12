import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../domain/model/sleep_models.dart';

part 'sleep_detail_display.freezed.dart';

/// One stage type's total across the night, and its share of the recorded time.
class SleepStageTotal {
  const SleepStageTotal({
    required this.stageType,
    required this.durationMs,
    required this.sharePercent,
  });

  final int stageType;
  final int durationMs;
  final double sharePercent;
}

/// The screen-ready derivation of one loaded sleep session: the stages in clock
/// order and the per-stage totals the breakdown card lists.
///
/// Built once per load by [buildSleepDetailDisplay] and stored on the state —
/// the detail screen renders it and sorts nothing.
@freezed
abstract class SleepDetailDisplay with _$SleepDetailDisplay {
  const SleepDetailDisplay._();

  const factory SleepDetailDisplay({
    required SleepData session,

    /// The night's stages, earliest first.
    required List<SleepStage> sortedStages,

    /// Longest stage type first, as the breakdown card lists them.
    required List<SleepStageTotal> stageTotals,
  }) = _SleepDetailDisplay;

  bool get hasStages => sortedStages.isNotEmpty;
}

/// Pure derivation from a loaded session to its display model. No clock, no
/// I/O — unit-testable with a fixture session.
SleepDetailDisplay buildSleepDetailDisplay(SleepData session) {
  final sortedStages = [...session.stages]
    ..sort((a, b) => a.startTime.compareTo(b.startTime));
  final stageTotalMs = sortedStages.fold<int>(
    0,
    (sum, stage) => sum + (stage.durationMs > 0 ? stage.durationMs : 0),
  );
  final totals = <int, int>{};
  for (final stage in sortedStages) {
    totals[stage.stageType] = (totals[stage.stageType] ?? 0) + stage.durationMs;
  }
  final entries = totals.entries.toList()
    ..sort((a, b) => b.value.compareTo(a.value));
  return SleepDetailDisplay(
    session: session,
    sortedStages: sortedStages,
    stageTotals: [
      for (final entry in entries)
        SleepStageTotal(
          stageType: entry.key,
          durationMs: entry.value,
          sharePercent:
              stageTotalMs > 0 ? entry.value * 100.0 / stageTotalMs : 0.0,
        ),
    ],
  );
}
