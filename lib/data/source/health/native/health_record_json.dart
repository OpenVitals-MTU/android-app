/// Residual Health Connect import helper.
///
/// The record JSON bridge (the `*Json` Pigeon methods and the per-record
/// mappers) was removed in the typed 1:1 port — records now cross the bridge as
/// typed Pigeon `*Msg` classes. The only surviving helper is the schema
/// record-type lookup used by the import dedup path
/// (`filterExistingClientIds`).
class HealthRecordJson {
  const HealthRecordJson._();

  /// The schema record-type string for an [ImportRecord.targetType] (an
  /// AndroidX record class name), for `filterExistingClientIds`.
  static String? schemaTypeForImport(String targetType) {
    if (targetType == 'SleepSessionRecord') return 'Sleep';
    if (targetType.endsWith('Record')) {
      return targetType.substring(0, targetType.length - 'Record'.length);
    }
    return null;
  }
}
