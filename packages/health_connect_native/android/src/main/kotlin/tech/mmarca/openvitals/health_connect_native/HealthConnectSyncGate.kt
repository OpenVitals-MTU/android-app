package tech.mmarca.openvitals.health_connect_native

/**
 * Adapted from the native OpenVitals app (`healthconnect/HealthConnectSyncGate.kt`).
 *
 * In the native app the "pause Health Connect sync" toggle is persisted in
 * `PreferencesRepository`. The plugin has no preferences store, so Dart owns the
 * toggle and mirrors it here via [setEnabled] (Pigeon `setSyncEnabled`). Reads
 * short-circuit to their fallback and writes throw [HealthConnectSyncDisabledException]
 * while paused.
 */
internal class HealthConnectSyncDisabledException :
  IllegalStateException("Health Connect sync is paused")

internal class HealthConnectSyncGate {
  @Volatile
  private var enabled: Boolean = true

  val isEnabled: Boolean
    get() = enabled

  fun setEnabled(value: Boolean) {
    enabled = value
  }

  fun requireEnabled() {
    if (!enabled) throw HealthConnectSyncDisabledException()
  }
}
