import '../../core/result/result.dart';
import '../../data/repository/contract/hydration_repository.dart';
import '../model/nutrition_models.dart';

/// Loads the hydration entry an edit screen is about to prefill itself from.
///
/// Same rule as every other edit prefill: only records OpenVitals wrote can be
/// updated, so "not found" and "not ours" are the single answer null — see
/// `LoadBodyMeasurementForEditUseCase` for why they are not two. A failed read
/// is a failure `Result`.
class LoadHydrationEntryForEditUseCase {
  const LoadHydrationEntryForEditUseCase(this._hydrationRepository);

  final HydrationRepository _hydrationRepository;

  /// Null when no such entry exists, or when it is not OpenVitals-authored.
  Future<Result<HydrationEntry?>> call(String recordId) async {
    final loaded = await _hydrationRepository.loadHydrationEntry(recordId);
    return loaded.map((entry) {
      if (entry == null || !entry.isOpenVitalsEntry) return null;
      return entry;
    });
  }
}
