package tech.mmarca.openvitals.healthconnect

import android.content.Context
import android.os.Build
import android.os.Process
import android.os.UserManager

internal class HealthConnectDiagnostics(private val context: Context) {

    fun summary(): String =
        "pkg=${context.packageName}, uid=${Process.myUid()}, sdk=${Build.VERSION.SDK_INT}, profile=${isRunningInUnsupportedProfile()}"

    // A process never changes user profile, so one binder call is enough.
    private val unsupportedProfile: Boolean by lazy {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.getSystemService(UserManager::class.java)?.isProfile == true
    }

    fun isRunningInUnsupportedProfile(): Boolean = unsupportedProfile
}
