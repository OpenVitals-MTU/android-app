package tech.mmarca.openvitals.health_connect_native

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.health.connect.client.HealthConnectClient

/**
 * Adapted from the native OpenVitals app (`healthconnect/HealthConnectAvailabilityService.kt`).
 *
 * The native service returns a domain `HealthConnectAvailability` enum. The plugin
 * instead exposes the raw signals — SDK status plus the two overrides
 * (unsupported profile, standalone-without-Play-Store) — and lets Dart map them
 * to its own `HealthConnectAvailability` enum via the `availabilityDetail` Pigeon
 * call. This keeps the enum a single source of truth on the Dart side.
 */
internal class HealthConnectAvailabilityService(
  private val context: Context,
  private val diagnostics: HealthConnectDiagnostics,
) {
  fun sdkStatus(): Int = HealthConnectClient.getSdkStatus(context)

  fun isUnsupportedProfile(): Boolean = diagnostics.isRunningInUnsupportedProfile()

  /**
   * On Android 13 and below, Health Connect is a standalone APK updated via the
   * Play Store. If that APK is present but the Play Store is not, it can never be
   * updated — surfaced to the user as NEEDS_PLAY_STORE.
   */
  fun standaloneNeedsPlayStore(): Boolean =
    Build.VERSION.SDK_INT <= Build.VERSION_CODES.TIRAMISU &&
      packageInstalled(HC_PACKAGE) &&
      !packageInstalled(PLAY_STORE_PACKAGE)

  private fun packageInstalled(packageName: String): Boolean =
    runCatching {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
      } else {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(packageName, 0)
      }
    }.isSuccess

  private companion object {
    private const val HC_PACKAGE = "com.google.android.apps.healthdata"
    private const val PLAY_STORE_PACKAGE = "com.android.vending"
  }
}
