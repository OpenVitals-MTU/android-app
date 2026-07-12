import '../../core/result/result.dart';
import '../../core/time/local_date.dart';
import '../../data/repository/contract/activity_repository.dart';
import '../model/activity_models.dart';

/// The earliest date the legacy step history is scanned from, matching the
/// Kotlin `LegacyActivityStartDate` (2009-01-01).
final LocalDate legacyActivityStartDate = LocalDate(2009, 1, 1);

/// The scanned step history, with the window it was scanned over — the badges
/// are evaluated against both (a lifetime total needs the days; the stats header
/// needs the range).
class AchievementHistory {
  const AchievementHistory({
    required this.days,
    required this.start,
    required this.end,
  });

  final List<DailySteps> days;
  final LocalDate start;
  final LocalDate end;
}

/// Loads the whole step history the achievement badges are earned from.
///
/// Badges are lifetime awards, so the window is not the screen's period but
/// everything on record: from [legacyActivityStartDate] — early enough to cover
/// a history imported from a decade-old tracker — up to today. Where that window
/// begins is a property of the badges, not of the view, which is why it is
/// pinned here and not in the notifier.
class LoadAchievementHistoryUseCase {
  const LoadAchievementHistoryUseCase(this._activityRepository);

  final ActivityRepository _activityRepository;

  Future<Result<AchievementHistory>> call() async {
    final start = legacyActivityStartDate;
    final end = LocalDate.now();
    final loaded = await _activityRepository.loadDailySteps(start, end);
    return loaded.map(
      (days) => AchievementHistory(days: days, start: start, end: end),
    );
  }
}
