import '../../data/repository/contract/sleep_repository.dart';
import '../model/sleep_models.dart';

/// Loads one sleep session by id.
///
/// Null when no session has that id — a detail screen reached from a stale
/// deep link, or from a record another app has since deleted. That is a "not
/// found", not an error, and the two are kept apart here so the screen can say
/// which one it is.
class LoadSleepDetailUseCase {
  const LoadSleepDetailUseCase(this._sleepRepository);

  final SleepRepository _sleepRepository;

  Future<SleepData?> call(String sleepId) =>
      _sleepRepository.loadSleepSession(sleepId);
}
