package tech.mmarca.openvitals.healthconnect

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import tech.mmarca.openvitals.data.model.HealthConnectAvailability

internal class HealthConnectAvailabilityService(
    private val context: Context,
    private val diagnostics: HealthConnectDiagnostics,
) {
    fun availability(): HealthConnectAvailability {
        if (diagnostics.isRunningInUnsupportedProfile()) {
            Log.w(TAG, "Health Connect unavailable in current profile: ${diagnostics.summary()}")
            return HealthConnectAvailability.NOT_SUPPORTED
        }

        val sdkStatus = HealthConnectClient.getSdkStatus(context)
        val availability = when (sdkStatus) {
            HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.AVAILABLE
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED ->
                HealthConnectAvailability.NEEDS_PROVIDER_UPDATE
            else -> HealthConnectAvailability.NOT_SUPPORTED
        }
        Log.d(TAG, "availability=$availability sdkStatus=$sdkStatus ${diagnostics.summary()}")
        return availability
    }

    private companion object {
        private const val TAG = "HealthConnectAvailability"
    }
}
