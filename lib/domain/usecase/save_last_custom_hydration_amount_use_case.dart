import '../../data/repository/contract/hydration_repository.dart';

/// Remembers the last free-form amount that was logged, so the entry screen can
/// re-offer it as a one-tap chip. Synchronous, like the read that picks it up
/// again (`ReadHydrationSettingsUseCase`).
///
/// Written before the entry itself, and independently of whether that entry
/// succeeds: the chip is a memory of what the user *asked for*, and a save that
/// failed on a missing permission is exactly the case where they will want to try
/// the same amount again.
class SaveLastCustomHydrationAmountUseCase {
  const SaveLastCustomHydrationAmountUseCase(this._hydrationRepository);

  final HydrationRepository _hydrationRepository;

  void call(double milliliters) =>
      _hydrationRepository.setLastCustomHydrationAmountMilliliters(milliliters);
}
