import '../../core/time/local_date.dart';
import '../../data/repository/contract/hydration_repository.dart';

/// How much has been drunk today, as a single number.
///
/// Health Connect answers a *range* query with a list of days, so even "today"
/// comes back as a list that happens to have one entry in it — or none, when
/// nothing has been logged. Folding it to litres here is what lets the tracker
/// card add its own just-saved drink to the total without a re-read: the screen
/// holds a number, not a shape it has to reduce again.
class LoadTodayHydrationUseCase {
  const LoadTodayHydrationUseCase(this._hydrationRepository);

  final HydrationRepository _hydrationRepository;

  Future<double> call() async {
    final today = LocalDate.now();
    final days = await _hydrationRepository.loadDailyHydration(today, today);
    return days.fold<double>(0.0, (sum, day) => sum + day.liters);
  }
}
