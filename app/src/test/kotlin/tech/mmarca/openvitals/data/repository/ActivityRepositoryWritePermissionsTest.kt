package tech.mmarca.openvitals.data.repository

import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.CyclingPedalingCadenceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.PowerRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsCadenceRecord
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
 * The permissions a save actually needs.
 *
 * A recorded activity does not go to Health Connect as one record. The session
 * and every sensor series it carries go in ONE atomic insert call, so a series
 * the app never asked permission for does not silently go missing — it takes
 * the whole save down with it. The gate must therefore ask for exactly what the
 * writer will write.
 *
 * Ported from test/data/repository/activity_repository_write_permissions_test.dart.
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
        // The bug: it did not. A user who granted WRITE_EXERCISE but not
        // WRITE_HEART_RATE was told the save was permitted, and then the whole
        // insert was thrown.
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
        // NOT asked for: the writer skips an empty series rather than writing an
        // empty record, so asking would demand a permission we never use.
        assertFalse(writeSpeed in permissions)
        assertFalse(writeStepsCadence in permissions)
        // The cycling-cadence write permission is an alias of WRITE_EXERCISE in
        // the Health Connect client, so an unrecorded cadence series cannot be
        // distinguished from the session permission itself — nothing to assert.
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

    private fun request(
        bleSamples: BleRecordingSampleBuffer = BleRecordingSampleBuffer(),
    ): ActivityWriteRequest = ActivityWriteRequest(
        exerciseType = 8,
        startTime = start,
        endTime = start.plusSeconds(30 * 60),
        bleSamples = bleSamples,
    )
}
