import '../../core/result/result.dart';
import '../../data/repository/contract/mindfulness_repository.dart';
import '../model/mindfulness_models.dart';

/// Writes a mindfulness session, or updates one already written.
///
/// Both the guided timer and the manual duration field end here: whatever the
/// user did to produce it, a session is a title and a span, and it is written
/// the same way. The branch on [editRecordId] is the insert-versus-update one.
class SaveMindfulnessSessionUseCase {
  const SaveMindfulnessSessionUseCase(this._mindfulnessRepository);

  final MindfulnessRepository _mindfulnessRepository;

  Future<Result<void>> call(
    MindfulnessSessionWriteRequest request, {
    String? editRecordId,
  }) async {
    if (editRecordId == null) {
      final written =
          await _mindfulnessRepository.writeMindfulnessSessionEntry(request);
      return written.map((_) {});
    }
    return _mindfulnessRepository.updateMindfulnessSessionEntry(
      editRecordId,
      request,
    );
  }
}
