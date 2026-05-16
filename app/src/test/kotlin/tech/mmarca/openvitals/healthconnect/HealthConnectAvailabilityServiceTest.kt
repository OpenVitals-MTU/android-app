package tech.mmarca.openvitals.healthconnect

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import tech.mmarca.openvitals.data.model.HealthConnectAvailability

class HealthConnectAvailabilityServiceTest {

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test fun `availability needs Play Store on Android 13 when standalone Health Connect is installed without Play Store`() {
        val service = service(
            sdkInt = Build.VERSION_CODES.TIRAMISU,
            packageInstalled = { packageName ->
                packageName == HC_PACKAGE
            },
        )

        assertEquals(HealthConnectAvailability.NEEDS_PLAY_STORE, service.availability())
    }

    @Test fun `availability remains available on Android 13 when standalone Health Connect and Play Store are installed`() {
        val service = service(
            sdkInt = Build.VERSION_CODES.TIRAMISU,
            packageInstalled = { packageName ->
                packageName == HC_PACKAGE || packageName == PLAY_STORE_PACKAGE
            },
        )

        assertEquals(HealthConnectAvailability.AVAILABLE, service.availability())
    }

    @Test fun `availability remains available on Android 14 even when Play Store is missing`() {
        val service = service(
            sdkInt = Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
            packageInstalled = { packageName ->
                packageName == HC_PACKAGE
            },
        )

        assertEquals(HealthConnectAvailability.AVAILABLE, service.availability())
    }

    @Test fun `availability needs provider update when Health Connect reports update required`() {
        val service = service(sdkStatus = HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED)

        assertEquals(HealthConnectAvailability.NEEDS_PROVIDER_UPDATE, service.availability())
    }

    @Test fun `availability is not supported in unsupported profiles`() {
        val service = service(unsupportedProfile = true)

        assertEquals(HealthConnectAvailability.NOT_SUPPORTED, service.availability())
    }

    private fun service(
        sdkStatus: Int = HealthConnectClient.SDK_AVAILABLE,
        sdkInt: Int = Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
        unsupportedProfile: Boolean = false,
        packageInstalled: (String) -> Boolean = { true },
    ) = HealthConnectAvailabilityService(
        context = mockk<Context>(),
        diagnostics = HealthConnectDiagnostics(mockk()),
        sdkStatusProvider = { sdkStatus },
        diagnosticsSummaryProvider = { "diagnostics" },
        unsupportedProfileProvider = { unsupportedProfile },
        sdkIntProvider = { sdkInt },
        packageInstalledProvider = packageInstalled,
    )

    private companion object {
        private const val HC_PACKAGE = "com.google.android.apps.healthdata"
        private const val PLAY_STORE_PACKAGE = "com.android.vending"
    }
}
