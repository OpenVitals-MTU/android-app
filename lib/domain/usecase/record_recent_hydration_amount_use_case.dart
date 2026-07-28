import '../../data/repository/contract/hydration_repository.dart';

/// Remembers a logged volume as one of the last used cup sizes, which the
/// hydration reminder notification re-offers as one-tap "Add …" actions.
///
/// Unlike `SaveLastCustomHydrationAmountUseCase` — which only tracks the
/// free-form amount field — this records every non-edit log, container taps
/// included: a "cup size" is whatever the user actually drinks from. Written
/// before the entry itself and independently of whether it succeeds, for the
/// same reason as the last custom amount.
class RecordRecentHydrationAmountUseCase {
  const RecordRecentHydrationAmountUseCase(this._hydrationRepository);

  final HydrationRepository _hydrationRepository;

  void call(double milliliters) =>
      _hydrationRepository.recordRecentHydrationAmountMilliliters(milliliters);
}
