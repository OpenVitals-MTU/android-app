import '../../core/period/period_load_query.dart';
import '../../data/repository/contract/mindfulness_repository.dart';
import '../model/refresh_mode.dart';
import '../query/mindfulness_period_data.dart';

/// Loads one period of mindfulness sessions.
///
/// Mindful minutes are only recorded by an app that asks for them, so a period
/// with nothing in it is the normal case rather than a failure — the read
/// returns an empty period and the screen shows its empty state.
class LoadMindfulnessPeriodUseCase {
  const LoadMindfulnessPeriodUseCase(this._mindfulnessRepository);

  final MindfulnessRepository _mindfulnessRepository;

  Future<MindfulnessPeriodData> call(
    PeriodLoadQuery query, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) =>
      _mindfulnessRepository.loadMindfulnessPeriod(
        query,
        refreshMode: refreshMode,
      );
}
