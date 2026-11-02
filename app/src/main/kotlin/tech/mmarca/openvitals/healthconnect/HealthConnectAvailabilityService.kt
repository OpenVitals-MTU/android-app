package tech.mmarca.openvitals.healthconnect

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability

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
    /** Monotonic clock for the status cache, off the JVM so a test needs no Android stub. */
    private val elapsedMillis: () -> Long = { System.nanoTime() / 1_000_000L },
    /** The app's foreground epoch; a status read in an earlier one is stale (the provider may have been installed). */
    private val foregroundEpoch: () -> Int = { 0 },
) {
    private class CachedStatus(val status: Int, val atMillis: Long, val epoch: Int)

    @Volatile
    private var cachedStatus: CachedStatus? = null

    fun availability(): HealthConnectAvailability {
        if (unsupportedProfileProvider()) {
            Log.w(TAG, "Health Connect unavailable in current profile: ${diagnosticsSummaryProvider()}")
            return HealthConnectAvailability.NOT_SUPPORTED
        }

        if (standaloneHealthConnectNeedsPlayStore()) {
            Log.w(TAG, "Standalone Health Connect is installed without Play Store: ${diagnosticsSummaryProvider()}")
            return HealthConnectAvailability.NEEDS_PLAY_STORE
        }

        val sdkStatus = sdkStatusCached()
        val availability = when (sdkStatus) {
            HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.AVAILABLE
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED ->
                HealthConnectAvailability.NEEDS_PROVIDER_UPDATE
            else -> HealthConnectAvailability.NOT_SUPPORTED
        }
        Log.d(TAG, "availability=$availability sdkStatus=$sdkStatus ${diagnosticsSummaryProvider()}")
        return availability
    }

    /** Forgets the cached status: the next call asks again. */
    fun invalidate() {
        cachedStatus = null
    }

    /**
     * A dashboard load asks this some thirty times. The answer is a package
     * query that changes only with an install, so it is kept for a while and
     * dropped when the app comes back to the foreground.
     */
    private fun sdkStatusCached(): Int {
        val now = elapsedMillis()
        val epoch = foregroundEpoch()
        cachedStatus
            ?.takeIf { it.epoch == epoch && now - it.atMillis <= SdkStatusCacheMillis }
            ?.let { return it.status }
        return sdkStatusProvider(context).also { status ->
            cachedStatus = CachedStatus(status, now, epoch)
        }
    }

    private fun standaloneHealthConnectNeedsPlayStore(): Boolean =
        sdkIntProvider() <= Build.VERSION_CODES.TIRAMISU &&
            packageInstalledProvider(HC_PACKAGE) &&
            !packageInstalledProvider(PLAY_STORE_PACKAGE)

    private companion object {
        private const val TAG = "HealthConnectAvailability"
        private const val SdkStatusCacheMillis = 30_000L
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
