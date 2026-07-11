package tech.mmarca.openvitals.health_connect_native

import android.content.Context
import android.os.Build
import android.os.Process
import android.os.UserManager

/**
 * Ported from the native OpenVitals app (`healthconnect/HealthConnectDiagnostics.kt`).
 *
 * Emits a privacy-safe field-debug summary appended to Health Connect log lines,
 * and detects work/managed profiles where Health Connect is unsupported.
 */
internal class HealthConnectDiagnostics(private val context: Context) {

  fun summary(): String =
    "pkg=${context.packageName}, uid=${Process.myUid()}, sdk=${Build.VERSION.SDK_INT}, " +
      "profile=${isRunningInUnsupportedProfile()}"

  fun isRunningInUnsupportedProfile(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
      context.getSystemService(UserManager::class.java)?.isProfile == true
}
