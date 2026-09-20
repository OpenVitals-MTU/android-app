package tech.mmarca.openvitals.healthconnect

import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.testing.FakeHealthConnectClient
import androidx.health.connect.client.testing.stubs.MutableStub
import androidx.health.connect.client.time.TimeRangeFilter
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import java.time.Instant
import kotlin.reflect.KClass
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import tech.mmarca.openvitals.domain.model.ActivityRecordSource
import tech.mmarca.openvitals.domain.model.ActivityWriteRequest
import tech.mmarca.openvitals.domain.model.BleHeartRateSample
import tech.mmarca.openvitals.domain.model.BleRecordingSampleBuffer

/** Write, edit and delete of an activity entry, on Google's fake client. */
class ActivityEntryWriteTest {

    private val fake = FakeHealthConnectClient().apply { setPackageName(APP_PACKAGE) }

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
    fun `deleting a workout keeps the metrics of one that starts inside its window`() = onARealClock {
        val client = client()
        val reader = reader(client)
        // Minute rounding makes this overlap easy: A ends 10:31, B starts 10:30.
        reader.writeActivityEntry(request(T10_00, T10_31, distanceMeters = 5_000.0, steps = 6_000))
        reader.writeActivityEntry(request(T10_30, T11_00, distanceMeters = 3_000.0, heartRate = listOf(T10_40 to 150L)))

        reader.deleteActivityEntry(client.sessionIdAt(T10_00))

        assertThat(client.all(ExerciseSessionRecord::class).map { it.startTime }).containsExactly(T10_30)
        assertThat(client.all(DistanceRecord::class).map { it.distance.inMeters }).containsExactly(3_000.0)
        assertThat(client.all(HeartRateRecord::class)).hasSize(1)
        assertThat(client.all(StepsRecord::class)).isEmpty()
    }

    @Test
    fun `editing a workout keeps the metrics of one that starts inside its window`() = onARealClock {
        val client = client()
        val reader = reader(client)
        reader.writeActivityEntry(request(T10_00, T10_31, distanceMeters = 5_000.0))
        reader.writeActivityEntry(request(T10_30, T11_00, distanceMeters = 3_000.0, heartRate = listOf(T10_40 to 150L)))

        reader.updateActivityEntry(client.sessionIdAt(T10_00), request(T10_00, T10_31, distanceMeters = 5_500.0))

        assertThat(client.all(DistanceRecord::class).map { it.distance.inMeters }).containsExactly(5_500.0, 3_000.0)
        assertThat(client.all(HeartRateRecord::class)).hasSize(1)
    }

    @Test
    fun `editing the title keeps the recorded heart rate`() = onARealClock {
        val client = client()
        val reader = reader(client)
        reader.writeActivityEntry(
            request(T10_00, T11_00, distanceMeters = 5_000.0, heartRate = listOf(T10_10 to 140L, T10_40 to 150L)),
        )

        // The edit form never carries the stored series.
        reader.updateActivityEntry(
            client.sessionIdAt(T10_00),
            request(T10_00, T11_00, distanceMeters = 5_200.0, title = "Morning run"),
        )

        val heartRate = client.all(HeartRateRecord::class).single()
        assertThat(heartRate.samples.map { it.beatsPerMinute }).containsExactly(140L, 150L).inOrder()
        assertThat(client.all(DistanceRecord::class).map { it.distance.inMeters }).containsExactly(5_200.0)
        assertThat(client.all(ExerciseSessionRecord::class).single().title).isEqualTo("Morning run")
    }

    @Test
    fun `moving the window rebuilds the series inside it and a delete still finds it`() = onARealClock {
        val client = client()
        val reader = reader(client)
        reader.writeActivityEntry(request(T10_00, T11_00, heartRate = listOf(T10_10 to 140L, T10_40 to 150L)))

        reader.updateActivityEntry(client.sessionIdAt(T10_00), request(T10_05, T10_30))

        val heartRate = client.all(HeartRateRecord::class).single()
        assertThat(heartRate.startTime).isEqualTo(T10_05)
        assertThat(heartRate.endTime).isEqualTo(T10_30)
        assertThat(heartRate.samples.map { it.beatsPerMinute }).containsExactly(140L)

        reader.deleteActivityEntry(client.sessionIdAt(T10_05))
        assertThat(client.all(HeartRateRecord::class)).isEmpty()
    }

    @Test
    fun `an edit that carries samples replaces the series`() = onARealClock {
        val client = client()
        val reader = reader(client)
        reader.writeActivityEntry(request(T10_00, T11_00, heartRate = listOf(T10_10 to 140L)))

        reader.updateActivityEntry(
            client.sessionIdAt(T10_00),
            request(T10_00, T11_00, heartRate = listOf(T10_40 to 160L)),
        )

        assertThat(client.all(HeartRateRecord::class).single().samples.map { it.beatsPerMinute })
            .containsExactly(160L)
    }

    @Test
    fun `a failed write leaves the workout as it was`() = onARealClock {
        val client = client()
        val reader = reader(client)
        reader.writeActivityEntry(request(T10_00, T11_00, distanceMeters = 5_000.0, title = "Run"))
        fake.overrides.insertRecords = MutableStub { throw IllegalStateException("rate limited") }

        val failure = runCatching {
            reader.updateActivityEntry(
                client.sessionIdAt(T10_00),
                request(T10_00, T11_00, distanceMeters = 5_500.0, title = "Morning run"),
            )
        }.exceptionOrNull()

        assertThat(failure).hasMessageThat().contains("rate limited")
        // The old order updated the session and deleted the distance before this insert ran.
        assertThat(client.all(DistanceRecord::class).map { it.distance.inMeters }).containsExactly(5_000.0)
        assertThat(client.all(ExerciseSessionRecord::class).single().title).isEqualTo("Run")
    }

    @Test
    fun `importing the same file twice stores the workout once`() = onARealClock {
        val client = client()
        val reader = reader(client)
        val imported = request(T10_00, T11_00, distanceMeters = 5_000.0, heartRate = listOf(T10_10 to 140L))
            .copy(importKey = "9f2c1a7b3e5d4f60a1b2c3d4", source = ActivityRecordSource.WATCH)

        reader.writeActivityEntries(listOf(imported))
        reader.writeActivityEntries(listOf(imported))

        // A random client id stored a second session, distance and heart rate each time.
        assertThat(client.all(ExerciseSessionRecord::class)).hasSize(1)
        assertThat(client.all(DistanceRecord::class)).hasSize(1)
        assertThat(client.all(HeartRateRecord::class)).hasSize(1)
    }

    @Test
    fun `a watch file is stored as recorded by a watch, not typed in on a phone`() = onARealClock {
        val client = client()
        val reader = reader(client)

        reader.writeActivityEntry(
            request(T10_00, T11_00, distanceMeters = 5_000.0).copy(importKey = "k1", source = ActivityRecordSource.WATCH),
        )
        reader.writeActivityEntry(request(T10_00.plusSeconds(7_200), T11_00.plusSeconds(7_200)))

        val (watch, typed) = client.all(ExerciseSessionRecord::class).sortedBy { it.startTime }
        assertThat(watch.metadata.recordingMethod).isEqualTo(Metadata.RECORDING_METHOD_ACTIVELY_RECORDED)
        assertThat(watch.metadata.device?.type).isEqualTo(Device.TYPE_WATCH)
        assertThat(typed.metadata.recordingMethod).isEqualTo(Metadata.RECORDING_METHOD_MANUAL_ENTRY)
    }

    @Test
    fun `an imported workout can still be deleted with all its records`() = onARealClock {
        val client = client()
        val reader = reader(client)
        reader.writeActivityEntry(
            request(T10_00, T11_00, distanceMeters = 5_000.0, heartRate = listOf(T10_10 to 140L))
                .copy(importKey = "k1", source = ActivityRecordSource.WATCH),
        )

        reader.deleteActivityEntry(client.sessionIdAt(T10_00))

        assertThat(client.all(ExerciseSessionRecord::class)).isEmpty()
        assertThat(client.all(DistanceRecord::class)).isEmpty()
        assertThat(client.all(HeartRateRecord::class)).isEmpty()
    }

    private fun onARealClock(body: suspend CoroutineScope.() -> Unit) = runBlocking(block = body)

    private fun request(
        start: Instant,
        end: Instant,
        distanceMeters: Double? = null,
        steps: Long? = null,
        heartRate: List<Pair<Instant, Long>> = emptyList(),
        title: String? = null,
    ) = ActivityWriteRequest(
        exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
        startTime = start,
        endTime = end,
        title = title,
        distanceMeters = distanceMeters,
        stepsCount = steps,
        bleSamples = BleRecordingSampleBuffer(
            heartRateSamples = heartRate.map { (time, bpm) -> BleHeartRateSample(time, bpm) },
        ),
    )

    private suspend fun <T : Record> HealthConnectClient.all(type: KClass<T>): List<T> =
        readRecords(ReadRecordsRequest(type, TimeRangeFilter.between(DAY_START, DAY_END))).records

    private suspend fun HealthConnectClient.sessionIdAt(start: Instant): String =
        all(ExerciseSessionRecord::class).single { it.startTime == start }.metadata.id

    /** The wrapper reads by record id and matches ranges on start time, as Health Connect does. */
    private fun client(): HealthConnectClient =
        AggregatingFakeHealthConnectClient(fake, platformTimeRange = true)

    private fun reader(client: HealthConnectClient): ActivityHealthReader {
        val diagnostics = mockk<HealthConnectDiagnostics>()
        every { diagnostics.summary() } returns "test"
        val support = HealthConnectReaderSupport(
            clientProvider = { client },
            diagnostics = diagnostics,
            rateLimitMessage = { "rate limited" },
        )
        return ActivityHealthReader(support, APP_PACKAGE)
    }

    private companion object {
        const val APP_PACKAGE = "tech.mmarca.openvitals"
        val DAY_START: Instant = Instant.parse("2026-03-10T00:00:00Z")
        val DAY_END: Instant = Instant.parse("2026-03-11T00:00:00Z")
        val T10_00: Instant = Instant.parse("2026-03-10T10:00:00Z")
        val T10_05: Instant = Instant.parse("2026-03-10T10:05:00Z")
        val T10_10: Instant = Instant.parse("2026-03-10T10:10:00Z")
        val T10_30: Instant = Instant.parse("2026-03-10T10:30:00Z")
        val T10_31: Instant = Instant.parse("2026-03-10T10:31:00Z")
        val T10_40: Instant = Instant.parse("2026-03-10T10:40:00Z")
        val T11_00: Instant = Instant.parse("2026-03-10T11:00:00Z")
    }
}
