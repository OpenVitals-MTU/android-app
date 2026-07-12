import '../../data/repository/contract/hydration_repository.dart';

/// The daily hydration goal, in litres — **synchronously**.
///
/// Deliberately not a `Future`, and deliberately on its own. It is persisted
/// configuration, not a health read, and it is applied *before* a load starts: a
/// goal just changed in settings has to be on the goal card immediately, not a
/// round-trip later. An async read here would give every hydration screen a frame
/// of default goal to draw first.
class ReadHydrationDailyGoalUseCase {
  const ReadHydrationDailyGoalUseCase(this._hydrationRepository);

  final HydrationRepository _hydrationRepository;

  double call() => _hydrationRepository.hydrationDailyGoalLiters();
}
