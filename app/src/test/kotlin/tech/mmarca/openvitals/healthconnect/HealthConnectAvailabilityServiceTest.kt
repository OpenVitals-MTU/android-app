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
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability

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

    @Test fun `the sdk status is asked once within the window, and again after it or a new foreground`() {
        var now = 0L
        var epoch = 0
        var asked = 0
        val service = service(sdkStatusProvider = { asked += 1; HealthConnectClient.SDK_AVAILABLE }, now = { now }, epoch = { epoch })

        // A dashboard load asks some thirty times; the package query answers once.
        repeat(30) { service.availability() }
        assertEquals(1, asked)

        now = 31_000L
        service.availability()
        assertEquals(2, asked)

        // Coming back from another app, the provider may have been installed meanwhile.
        epoch = 1
        service.availability()
        assertEquals(3, asked)

        service.invalidate()
        service.availability()
        assertEquals(4, asked)
    }

    private fun service(
        sdkStatus: Int = HealthConnectClient.SDK_AVAILABLE,
        sdkInt: Int = Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
        unsupportedProfile: Boolean = false,
        packageInstalled: (String) -> Boolean = { true },
        sdkStatusProvider: (Context) -> Int = { sdkStatus },
        now: () -> Long = { 0L },
        epoch: () -> Int = { 0 },
    ) = HealthConnectAvailabilityService(
        context = mockk<Context>(),
        diagnostics = HealthConnectDiagnostics(mockk()),
        sdkStatusProvider = sdkStatusProvider,
        diagnosticsSummaryProvider = { "diagnostics" },
        unsupportedProfileProvider = { unsupportedProfile },
        sdkIntProvider = { sdkInt },
        packageInstalledProvider = packageInstalled,
        elapsedMillis = now,
        foregroundEpoch = epoch,
    )

    private companion object {
        private const val HC_PACKAGE = "com.google.android.apps.healthdata"
        private const val PLAY_STORE_PACKAGE = "com.android.vending"
    }
}
