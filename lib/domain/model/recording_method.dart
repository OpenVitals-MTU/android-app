/// How a Health Connect record came to exist — AndroidX `Metadata`'s
/// `RECORDING_METHOD_*` constants, which travel across the Pigeon bridge as
/// plain ints.
///
/// These values are defined by Health Connect and are not ours to choose. They
/// had been hand-copied into three separate files; two agreed and one did not,
/// which is exactly the bug this type exists to make impossible. Compare
/// against these, never against a literal.
abstract final class RecordingMethod {
  const RecordingMethod._();

  /// The provider did not say.
  static const int unknown = 0;

  /// The user started and stopped a recording in some app.
  static const int activelyRecorded = 1;

  /// A device inferred it without being asked (a watch detecting a walk).
  static const int automaticallyRecorded = 2;

  /// A person typed it in.
  static const int manualEntry = 3;
}
