import '../../core/period/time_range.dart';
import '../../core/time/local_date.dart';
import '../../data/repository/contract/body_energy_repository.dart';
import '../model/refresh_mode.dart';

/// Loads one day's Body Energy timeline.
///
/// Body Energy is not a metric anyone records — it is derived, minute by minute,
/// from sleep, heart rate, activity and stress. That derivation (and its cache)
/// lives in [BodyEnergyRepository]; what this use case pins is the *shape of the
/// question*: Body Energy is only ever asked about a single day, as a
/// [TimeRange.day] query over a one-day period, because the 5-minute buckets it
/// produces have no meaning spread across a week.
class LoadBodyEnergyTimelineUseCase {
  const LoadBodyEnergyTimelineUseCase(this._bodyEnergyRepository);

  final BodyEnergyRepository _bodyEnergyRepository;

  Future<BodyEnergyTimelineResult> call(
    LocalDate date, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) =>
      _bodyEnergyRepository.loadTimeline(
        BodyEnergyTimelineQuery(
          period: DatePeriod(date, date),
          range: TimeRange.day,
          refreshMode: refreshMode,
        ),
      );
}
