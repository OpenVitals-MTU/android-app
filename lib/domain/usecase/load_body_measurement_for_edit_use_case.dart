import '../../data/repository/contract/body_repository.dart';
import '../model/body_models.dart';

/// Loads the measurement an edit screen is about to prefill itself from.
///
/// A read with a rule attached: Health Connect only lets an app update the
/// records it wrote itself, so a record OpenVitals does not own is not editable
/// no matter that it is on screen and has an id.
///
/// A missing record and someone else's record therefore come back the same way —
/// as null. They are one answer ("there is nothing here for you to edit"), and
/// splitting them would only make the screen phrase the same sentence twice. A
/// failed *read*, on the other hand, still throws: that is a broken connection to
/// Health Connect, not a verdict about the record.
class LoadBodyMeasurementForEditUseCase {
  const LoadBodyMeasurementForEditUseCase(this._bodyRepository);

  final BodyRepository _bodyRepository;

  /// Null when no such record exists, or when it is not OpenVitals-authored.
  Future<BodyMeasurementEntry?> call(
    BodyMeasurementType type,
    String recordId,
  ) async {
    final entry = await _bodyRepository.loadBodyMeasurementEntry(type, recordId);
    if (entry == null || !entry.isOpenVitalsEntry) return null;
    return entry;
  }
}
