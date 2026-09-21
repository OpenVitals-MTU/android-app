package tech.mmarca.openvitals.healthconnect

import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.ExerciseSegment
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.PlannedExerciseSessionRecord
import androidx.health.connect.client.records.PlannedExerciseStep
import androidx.health.connect.client.records.Record
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import java.time.Instant
import kotlin.reflect.KClass
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import tech.mmarca.openvitals.core.performance.DispatcherProvider
import tech.mmarca.openvitals.domain.model.PlannedExerciseBlockData
import tech.mmarca.openvitals.domain.model.PlannedExerciseCompletion
import tech.mmarca.openvitals.domain.model.PlannedExerciseStepData
import tech.mmarca.openvitals.domain.model.PlannedExerciseWriteRequest

/**
 * Saving an edited plan used to delete it and insert a copy. A failed insert lost the plan,
 * and the copy's new id cut the link from every session that had completed the plan.
 */
class PlannedWorkoutWriteTest {

    // Google's fake client cannot store a planned session ("Unsupported yet!"), so the calls are checked on a mock.
    private val client = mockk<HealthConnectClient>()

    @Before
    fun setUp() {
        HealthConnectRateLimitBackoff.resetForTest()
        mockkStatic(Log::class)
        every { Log.d(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>(), any()) } returns 0
        every { Log.e(any(), any<String>(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `an edited plan is updated in place and keeps its id`() = onTheTestClock {
        val updated = slot<List<Record>>()
        coEvery { client.updateRecords(capture(updated)) } returns Unit

        val savedId = reader().writePlannedExerciseSession(plan(title = "Intervals, longer", id = "plan-1"))

        assertThat(savedId).isEqualTo("plan-1")
        val record = updated.captured.single() as PlannedExerciseSessionRecord
        assertThat(record.metadata.id).isEqualTo("plan-1")
        assertThat(record.title).isEqualTo("Intervals, longer")
        // No delete: a failed write must leave the old plan, and sessions that completed it keep their link.
        coVerify(exactly = 0) { client.deleteRecords(any<KClass<out Record>>(), any<List<String>>(), any()) }
        coVerify(exactly = 0) { client.insertRecords(any()) }
    }

    @Test
    fun `a failed save deletes nothing`() = onTheTestClock {
        coEvery { client.updateRecords(any()) } throws IllegalStateException("rate limited")

        val failure = runCatching { reader().writePlannedExerciseSession(plan(title = "Intervals", id = "plan-1")) }

        assertThat(failure.isFailure).isTrue()
        coVerify(exactly = 0) { client.deleteRecords(any<KClass<out Record>>(), any<List<String>>(), any()) }
    }

    private fun plan(title: String, id: String? = null) = PlannedExerciseWriteRequest(
        id = id,
        exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
        startTime = NOON,
        endTime = NOON.plusSeconds(1_800),
        title = title,
        blocks = listOf(
            PlannedExerciseBlockData(
                repetitions = 1,
                description = null,
                steps = listOf(
                    PlannedExerciseStepData(
                        exerciseType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_RUNNING,
                        exercisePhase = PlannedExerciseStep.EXERCISE_PHASE_ACTIVE,
                        description = null,
                        completion = PlannedExerciseCompletion.DurationSeconds(1_800),
                    ),
                ),
            ),
        ),
    )

    private fun reader(): ActivityHealthReader {
        val diagnostics = mockk<HealthConnectDiagnostics>()
        every { diagnostics.summary() } returns "test"
        val support = HealthConnectReaderSupport(
            clientProvider = { client },
            diagnostics = diagnostics,
            rateLimitMessage = { "rate limited" },
            dispatchers = testDispatchers,
        )
        return ActivityHealthReader(support, APP_PACKAGE)
    }

    private val testDispatcher = StandardTestDispatcher()
    private val testDispatchers = object : DispatcherProvider {
        override val main = testDispatcher
        override val io = testDispatcher
        override val default = testDispatcher
    }

    /** Every read hops to the support's dispatcher, so on this one the whole read is test time. */
    private fun onTheTestClock(body: suspend CoroutineScope.() -> Unit) = runTest(testDispatcher) { body() }

    private companion object {
        const val APP_PACKAGE = "tech.mmarca.openvitals"
        val NOON: Instant = Instant.parse("2026-03-10T12:00:00Z")
    }
}
