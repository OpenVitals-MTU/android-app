import '../../data/repository/contract/mindfulness_repository.dart';
import '../model/mindfulness_models.dart';

/// Loads the mindfulness session an edit screen is about to prefill itself from.
///
/// Same rule as every other edit prefill: only records OpenVitals wrote can be
/// updated, so "not found" and "not ours" are the single answer null — see
/// [LoadBodyMeasurementForEditUseCase] for why they are not two. A failed read
/// throws.
class LoadMindfulnessSessionForEditUseCase {
  const LoadMindfulnessSessionForEditUseCase(this._mindfulnessRepository);

  final MindfulnessRepository _mindfulnessRepository;

  /// Null when no such session exists, or when it is not OpenVitals-authored.
  Future<MindfulnessSession?> call(String recordId) async {
    final session = await _mindfulnessRepository.loadMindfulnessSession(recordId);
    if (session == null || !session.isOpenVitalsEntry) return null;
    return session;
  }
}
