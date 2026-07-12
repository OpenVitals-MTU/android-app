import '../../core/result/result.dart';
import '../../data/repository/contract/vitals_repository.dart';
import '../model/vitals_models.dart';

/// Loads the vitals measurement an edit screen is about to prefill itself from.
///
/// Same rule as every other edit prefill: only records OpenVitals wrote can be
/// updated, so "not found" and "not ours" are the single answer null — see
/// [LoadBodyMeasurementForEditUseCase] for why they are not two. A failed read
/// is a failure `Result`.
class LoadVitalsMeasurementForEditUseCase {
  const LoadVitalsMeasurementForEditUseCase(this._vitalsRepository);

  final VitalsRepository _vitalsRepository;

  /// Null when no such record exists, or when it is not OpenVitals-authored.
  Future<Result<VitalsMeasurementEntry?>> call(
    VitalsMeasurementType type,
    String recordId,
  ) async {
    final loaded =
        await _vitalsRepository.loadVitalsMeasurementEntry(type, recordId);
    return loaded.map((entry) {
      if (entry == null || !entry.isOpenVitalsEntry) return null;
      return entry;
    });
  }
}
