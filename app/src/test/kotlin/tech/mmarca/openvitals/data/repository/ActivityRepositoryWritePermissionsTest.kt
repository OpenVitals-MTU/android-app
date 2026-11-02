package tech.mmarca.openvitals.data.repository

import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.CyclingPedalingCadenceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.PowerRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsCadenceRecord
import androidx.health.connect.client.records.WheelchairPushesRecord
import io.mockk.every
import io.mockk.mockk
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.model.ActivityWriteRequest
import tech.mmarca.openvitals.domain.model.BleHeartRateSample
import tech.mmarca.openvitals.domain.model.BlePowerSample
import tech.mmarca.openvitals.domain.model.BleRecordingSampleBuffer
import tech.mmarca.openvitals.domain.model.BleStepsCadenceSample
import tech.mmarca.openvitals.healthconnect.HealthConnectManager

/**
 * The session and every sensor series go in one atomic insert, so a series without
 * permission takes the whole save down. The gate must ask for exactly what the writer writes.
 */
class ActivityRepositoryWritePermissionsTest {

    private val writeExercise = HealthPermission.getWritePermission(ExerciseSessionRecord::class)
    private val writeHeartRate = HealthPermission.getWritePermission(HeartRateRecord::class)
    private val writePower = HealthPermission.getWritePermission(PowerRecord::class)
    private val writeSpeed = HealthPermission.getWritePermission(SpeedRecord::class)
    private val writeStepsCadence = HealthPermission.getWritePermission(StepsCadenceRecord::class)
    private val writeCyclingCadence = HealthPermission.getWritePermission(CyclingPedalingCadenceRecord::class)

    private val start: Instant = Instant.parse("2026-07-14T18:00:00Z")

    private val repository: ActivityRepositoryImpl
        get() = ActivityRepositoryImpl(
            mockk<HealthConnectManager>().also { hc ->
                every { hc.isPlannedExerciseAvailable() } returns false
            },
        )

    @Test
    fun `a bare session asks only for exercise`() {
        val permissions = repository.activityWritePermissions(request())

        assertEquals(setOf(writeExercise), permissions)
    }

    @Test
    fun `a recording with heart rate asks to write heart rate`() {
        // A user with WRITE_EXERCISE but not WRITE_HEART_RATE was told the save was permitted, then the insert threw.
        val permissions = repository.activityWritePermissions(
            request(
                bleSamples = BleRecordingSampleBuffer(
                    heartRateSamples = listOf(BleHeartRateSample(time = start, beatsPerMinute = 150)),
                ),
            ),
        )

        assertTrue(writeHeartRate in permissions)
        assertTrue(writeExercise in permissions)
    }

    @Test
    fun `each series is asked for only when it has samples`() {
        val permissions = repository.activityWritePermissions(
            request(
                bleSamples = BleRecordingSampleBuffer(
                    heartRateSamples = listOf(BleHeartRateSample(time = start, beatsPerMinute = 150)),
                    powerSamples = listOf(BlePowerSample(time = start, watts = 220.0)),
                ),
            ),
        )

        // Asked for, because they were recorded.
        assertTrue(writeHeartRate in permissions)
        assertTrue(writePower in permissions)
        // Not asked for: the writer skips an empty series.
        assertFalse(writeSpeed in permissions)
        assertFalse(writeStepsCadence in permissions)
        // The cycling-cadence write permission is an alias of WRITE_EXERCISE, so nothing to assert.
        assertEquals(writeExercise, writeCyclingCadence)
    }

    @Test
    fun `a device that defines the permission is still asked for it`() {
        val permissions = repository.activityWritePermissions(
            request(
                bleSamples = BleRecordingSampleBuffer(
                    stepsCadenceSamples = listOf(BleStepsCadenceSample(time = start, stepsPerMinute = 112)),
                ),
            ),
        )

        assertTrue(writeStepsCadence in permissions)
    }

    @Test
    fun `floors and pushes are asked for only when an import carries them`() {
        val writeFloors = HealthPermission.getWritePermission(FloorsClimbedRecord::class)
        val writePushes = HealthPermission.getWritePermission(WheelchairPushesRecord::class)

        val bare = repository.activityWritePermissions(request())
        assertFalse(writeFloors in bare)
        assertFalse(writePushes in bare)

        val floors = repository.activityWritePermissions(request().copy(floorsClimbed = 12.0))
        assertTrue(writeFloors in floors)
        assertFalse(writePushes in floors)

        val pushes = repository.activityWritePermissions(request().copy(wheelchairPushes = 340L))
        assertTrue(writePushes in pushes)
    }

    private fun request(
        bleSamples: BleRecordingSampleBuffer = BleRecordingSampleBuffer(),
    ): ActivityWriteRequest = ActivityWriteRequest(
        exerciseType = 8,
        startTime = start,
        endTime = start.plusSeconds(30 * 60),
        bleSamples = bleSamples,
    )
}
