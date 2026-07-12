import '../../data/repository/contract/hydration_repository.dart';
import '../model/nutrition_models.dart';

/// Loads the hydration entry an edit screen is about to prefill itself from.
///
/// Same rule as every other edit prefill: only records OpenVitals wrote can be
/// updated, so "not found" and "not ours" are the single answer null — see
/// `LoadBodyMeasurementForEditUseCase` for why they are not two. A failed read
/// throws.
class LoadHydrationEntryForEditUseCase {
  const LoadHydrationEntryForEditUseCase(this._hydrationRepository);

  final HydrationRepository _hydrationRepository;

  /// Null when no such entry exists, or when it is not OpenVitals-authored.
  Future<HydrationEntry?> call(String recordId) async {
    final entry = await _hydrationRepository.loadHydrationEntry(recordId);
    if (entry == null || !entry.isOpenVitalsEntry) return null;
    return entry;
  }
}
