package tech.mmarca.openvitals.data.repository

import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.WeightRecord
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import tech.mmarca.openvitals.domain.model.BodyMeasurementType
import tech.mmarca.openvitals.domain.model.BodyMeasurementWriteRequest
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.healthconnect.HealthConnectManager

class BodyRepositoryTest {
    @Test
    fun `body measurement mutations delegate to health connect`() = runTest {
        val writeWeightPermission = HealthPermission.getWritePermission(WeightRecord::class)
        val hc = mockk<HealthConnectManager>()
        val request = BodyMeasurementWriteRequest(
            type = BodyMeasurementType.WEIGHT,
            time = Instant.parse("2026-06-27T09:00:00Z"),
            value = 77.0,
        )
        every { hc.availability() } returns HealthConnectAvailability.AVAILABLE
        coEvery { hc.grantedPermissions() } returns setOf(writeWeightPermission)
        coEvery { hc.writeBodyMeasurementEntry(request) } returns "weight-id"
        coEvery { hc.updateBodyMeasurementEntry("weight-id", request) } returns Unit
        coEvery { hc.deleteBodyMeasurementEntry(BodyMeasurementType.WEIGHT, "weight-id") } returns Unit

        val repository = BodyRepositoryImpl(hc)

        val id = repository.writeBodyMeasurementEntry(request)
        repository.updateBodyMeasurementEntry("weight-id", request)
        repository.deleteBodyMeasurementEntry(BodyMeasurementType.WEIGHT, "weight-id")

        assertEquals("weight-id", id)
        coVerify(exactly = 1) { hc.writeBodyMeasurementEntry(request) }
        coVerify(exactly = 1) { hc.updateBodyMeasurementEntry("weight-id", request) }
        coVerify(exactly = 1) { hc.deleteBodyMeasurementEntry(BodyMeasurementType.WEIGHT, "weight-id") }
    }
}
