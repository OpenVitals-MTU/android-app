package tech.mmarca.openvitals.data.repository

import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeightRecord
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
import tech.mmarca.openvitals.domain.model.WeightEntry
import tech.mmarca.openvitals.domain.preferences.BodyProfile
import tech.mmarca.openvitals.healthconnect.HealthConnectManager

class BodyRepositoryTest {

    private val readWeightPermission = HealthPermission.getReadPermission(WeightRecord::class)
    private val readHeightPermission = HealthPermission.getReadPermission(HeightRecord::class)

    private val declared = BodyProfile(
        birthYear = 1993,
        weightKg = 76.0,
        heightCm = 178.0,
    )

    /** A source with an optional measured weight and height, and a controllable permission set. */
    private fun source(
        weightKg: Double? = null,
        heightCm: Double? = null,
        granted: Set<String> = setOf(readWeightPermission, readHeightPermission),
    ): HealthConnectManager = mockk {
        every { availability() } returns HealthConnectAvailability.AVAILABLE
        coEvery { grantedPermissions() } returns granted
        coEvery { readLatestWeight() } returns weightKg?.let {
            WeightEntry(
                id = "weight-1",
                time = Instant.parse("2026-07-24T00:00:00Z"),
                weightKg = it,
                source = "scale",
            )
        }
        coEvery { readLatestHeight() } returns heightCm
    }

    private suspend fun resolve(hc: HealthConnectManager): BodyProfile =
        BodyRepositoryImpl(hc).resolveBodyProfile(declared)

    @Test fun `a measured weight beats the declared one`() = runTest {
        // The app used to be 76 kg on the caffeine screen and 81 kg on the body screen.
        val resolved = resolve(source(weightKg = 81.2, heightCm = 181.0))

        assertEquals(81.2, resolved.weightKg!!, 1e-9)
        assertEquals(181.0, resolved.heightCm!!, 1e-9)
    }

    @Test fun `the declared value survives when nothing is recorded`() = runTest {
        val resolved = resolve(source())

        assertEquals(76.0, resolved.weightKg!!, 1e-9)
        assertEquals(178.0, resolved.heightCm!!, 1e-9)
    }

    @Test fun `a missing permission falls back rather than blanking the value`() = runTest {
        // A filter that returns nothing must read as "no preference", not "you weigh nothing".
        val resolved = resolve(source(weightKg = 81.2, heightCm = 181.0, granted = emptySet()))

        assertEquals(76.0, resolved.weightKg!!, 1e-9)
        assertEquals(178.0, resolved.heightCm!!, 1e-9)
    }

    @Test fun `the rest of the profile is untouched by resolution`() = runTest {
        // Resolution is about body size only.
        val resolved = resolve(source(weightKg = 81.2))

        assertEquals(1993, resolved.birthYear)
    }

    @Test fun `a measured value out of range is normalised, not trusted blindly`() = runTest {
        val resolved = resolve(source(weightKg = 900.0))

        assertEquals(BodyProfile.MaxWeightKg, resolved.weightKg!!, 1e-9)
    }

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
