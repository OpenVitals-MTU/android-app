/// Tri-state availability of an optional Health Connect feature on the device,
/// mapped from the native `HealthConnectFeatures.getFeatureStatus` call.
///
/// The app builds against the latest `connect-client` alpha, so the SDK knows
/// about feature constants the *installed* Health Connect provider may not
/// support yet. [unknown] means the provider is too old to even report the
/// feature's status; gating treats it the same as [unavailable] (see
/// [FeatureStatus.isAvailable]).
enum FeatureStatus {
  unknown,
  available,
  unavailable;

  /// Whether the feature is usable — only [available] gates a feature on. Both
  /// [unavailable] and [unknown] mean "don't use it on this device".
  bool get isAvailable => this == FeatureStatus.available;
}
