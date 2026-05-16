package tech.mmarca.openvitals.data.repository

import android.util.Log
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.StepsRecord
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import tech.mmarca.openvitals.data.model.HealthConnectAvailability
import tech.mmarca.openvitals.healthconnect.HealthConnectManager

class HealthRepositoryDashboardTest {

    private val stepsPermission = HealthPermission.getReadPermission(StepsRecord::class)
    private val distancePermission = HealthPermission.getReadPermission(DistanceRecord::class)

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test fun `loadDashboard keeps successful metrics when another metric is rate limited`() = runTest {
        val date = LocalDate.of(2026, 5, 16)
        val hc = mockk<HealthConnectManager>()
        every { hc.availability() } returns HealthConnectAvailability.AVAILABLE
        every { hc.requestableAllPermissions } returns setOf(stepsPermission, distancePermission)
        coEvery { hc.grantedPermissions() } returns setOf(stepsPermission, distancePermission)
        coEvery { hc.readSteps(date) } throws RuntimeException(
            "Request rejected. Rate limited request quota has been exceeded.",
        )
        coEvery { hc.readDistanceMeters(date) } returns 1234.0

        val data = HealthRepository(hc).loadDashboard(date)

        assertEquals(0L, data.steps)
        assertEquals(1234.0, data.distanceMeters, 0.01)
        assertNull(data.workout)
    }
}
