import '../../../core/period/time_range.dart';
import '../../../domain/insights/body_energy_timeline.dart';
import '../../../domain/model/refresh_mode.dart';

/// Query for [BodyEnergyRepository.loadTimeline]. Port of the Kotlin
/// `BodyEnergyTimelineQuery` (declared in the contract file).
class BodyEnergyTimelineQuery {
  const BodyEnergyTimelineQuery({
    required this.period,
    required this.range,
    this.refreshMode = RefreshMode.normal,
  });

  final DatePeriod period;
  final TimeRange range;
  final RefreshMode refreshMode;

  @override
  bool operator ==(Object other) =>
      other is BodyEnergyTimelineQuery &&
      other.period == period &&
      other.range == range &&
      other.refreshMode == refreshMode;

  @override
  int get hashCode => Object.hash(period, range, refreshMode);
}

/// Result of [BodyEnergyRepository.loadTimeline]. Port of the Kotlin
/// `BodyEnergyTimelineResult` including its derived getters.
class BodyEnergyTimelineResult {
  const BodyEnergyTimelineResult({required this.query, required this.days});

  final BodyEnergyTimelineQuery query;
  final List<BodyEnergyTimeline> days;

  BodyEnergyTimeline? get latestDay {
    for (final day in days.reversed) {
      if (day.points.isNotEmpty) return day;
    }
    return days.isNotEmpty ? days.last : null;
  }

  int? get currentScore => latestDay?.currentScore;

  int get charged => days.fold(0, (sum, day) => sum + day.charged);

  int get drained => days.fold(0, (sum, day) => sum + day.drained);
}

/// Port of the Kotlin `BodyEnergyRepository` contract.
abstract interface class BodyEnergyRepository {
  Future<BodyEnergyTimelineResult> loadTimeline(BodyEnergyTimelineQuery query);
}
