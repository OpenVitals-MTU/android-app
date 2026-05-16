package tech.mmarca.openvitals.healthconnect

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import tech.mmarca.openvitals.data.model.HealthConnectAvailability

internal class HealthConnectAvailabilityService(
    private val context: Context,
    private val diagnostics: HealthConnectDiagnostics,
    private val sdkStatusProvider: (Context) -> Int = HealthConnectClient::getSdkStatus,
    private val diagnosticsSummaryProvider: () -> String = diagnostics::summary,
    private val unsupportedProfileProvider: () -> Boolean = diagnostics::isRunningInUnsupportedProfile,
    private val sdkIntProvider: () -> Int = { Build.VERSION.SDK_INT },
    private val packageInstalledProvider: (String) -> Boolean = { packageName ->
        packageInstalled(context, packageName)
    },
) {
    fun availability(): HealthConnectAvailability {
        if (unsupportedProfileProvider()) {
            Log.w(TAG, "Health Connect unavailable in current profile: ${diagnosticsSummaryProvider()}")
            return HealthConnectAvailability.NOT_SUPPORTED
        }

        if (standaloneHealthConnectNeedsPlayStore()) {
            Log.w(TAG, "Standalone Health Connect is installed without Play Store: ${diagnosticsSummaryProvider()}")
            return HealthConnectAvailability.NEEDS_PLAY_STORE
        }

        val sdkStatus = sdkStatusProvider(context)
        val availability = when (sdkStatus) {
            HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.AVAILABLE
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED ->
                HealthConnectAvailability.NEEDS_PROVIDER_UPDATE
            else -> HealthConnectAvailability.NOT_SUPPORTED
        }
        Log.d(TAG, "availability=$availability sdkStatus=$sdkStatus ${diagnosticsSummaryProvider()}")
        return availability
    }

    private fun standaloneHealthConnectNeedsPlayStore(): Boolean =
        sdkIntProvider() <= Build.VERSION_CODES.TIRAMISU &&
            packageInstalledProvider(HC_PACKAGE) &&
            !packageInstalledProvider(PLAY_STORE_PACKAGE)

    private companion object {
        private const val TAG = "HealthConnectAvailability"
        private const val HC_PACKAGE = "com.google.android.apps.healthdata"
        private const val PLAY_STORE_PACKAGE = "com.android.vending"
    }
}

private fun packageInstalled(context: Context, packageName: String): Boolean =
    runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(packageName, 0)
        }
    }.isSuccess
