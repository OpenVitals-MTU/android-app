package tech.mmarca.openvitals.healthconnect

import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.CyclingPedalingCadenceRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.PowerRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsCadenceRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.testing.FakeHealthConnectClient
import androidx.health.connect.client.units.Length
import androidx.health.connect.client.units.Mass
import androidx.health.connect.client.units.Power
import androidx.health.connect.client.units.Pressure
import androidx.health.connect.client.units.Velocity
import androidx.health.connect.client.units.Volume
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import tech.mmarca.openvitals.domain.model.ActivityCadenceKind

/**
 * Record -> domain mapping, through the REAL readers.
 *
 * Dart counterpart: the `reads` and `writes` groups of
 * test/data/source/health/health_connect_native_data_source_test.dart.
 *
 * Flutter's fake sits at the Pigeon boundary and hands back typed messages, so
 * those tests assert that a `*Msg` becomes a domain object. There is no Msg layer
 * here: the readers map a Health Connect `Record` straight to the domain model, so
 * the equivalent assertion seeds a real record into Google's fake client and reads
 * it back through the reader the app ships.
 */
class HealthConnectReadMappingTest {

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
        HealthConnectRateLimitBackoff.resetForTest()
    }

    // ── exercise sessions with aggregate metrics ────────────────────────────

    @Test
    fun `readExerciseSessionsWithMetrics maps aggregate distance and speed through`() =
        runTest {
            val client = seeded(
                session(),
                DistanceRecord(
                    startTime = sessionStart,
                    startZoneOffset = ZoneOffset.UTC,
                    endTime = sessionEnd,
                    endZoneOffset = ZoneOffset.UTC,
                    distance = Length.meters(5_000.0),
                    metadata = Metadata.autoRecorded(watch),
                ),
                SpeedRecord(
                    startTime = sessionStart,
                    startZoneOffset = ZoneOffset.UTC,
                    endTime = sessionEnd,
                    endZoneOffset = ZoneOffset.UTC,
                    samples = listOf(
                        SpeedRecord.Sample(sessionStart, Velocity.metersPerSecond(2.5)),
                        SpeedRecord.Sample(sessionStart.plusSeconds(600), Velocity.metersPerSecond(2.9)),
                    ),
                    metadata = Metadata.autoRecorded(watch),
                ),
            )

            val sessions = activity(client).readExerciseSessionsWithMetrics(
                start = sessionStart.minusSeconds(3_600),
                end = sessionEnd.plusSeconds(3_600),
                includeDistance = true,
                includeSpeed = true,
            )

            assertThat(sessions).hasSize(1)
            assertThat(sessions.single().totalDistanceMeters).isWithin(1e-6).of(5_000.0)
            assertThat(sessions.single().averageSpeedMetersPerSecond).isWithin(1e-6).of(2.7)
        }

    @Test
    fun `readExerciseSessionsWithMetrics degrades to null metrics without the permissions`() =
        runTest {
            // Health Connect reads are permission-gated: an ungranted
            // distance/speed must degrade to null metrics, never throw or drop
            // the session.
            val client = seeded(
                session(),
                DistanceRecord(
                    startTime = sessionStart,
                    startZoneOffset = ZoneOffset.UTC,
                    endTime = sessionEnd,
                    endZoneOffset = ZoneOffset.UTC,
                    distance = Length.meters(5_000.0),
                    metadata = Metadata.autoRecorded(watch),
                ),
            )

            val sessions = activity(client).readExerciseSessionsWithMetrics(
                start = sessionStart.minusSeconds(3_600),
                end = sessionEnd.plusSeconds(3_600),
                includeDistance = false,
                includeSpeed = false,
            )

            assertThat(sessions).hasSize(1)
            assertThat(sessions.single().title).isEqualTo("Morning run")
            assertThat(sessions.single().totalDistanceMeters).isNull()
            assertThat(sessions.single().averageSpeedMetersPerSecond).isNull()
        }

    // Power was the last metric nobody wired up, and the app had every OTHER
    // piece of it: it asks Health Connect for READ_POWER, tells you so during
    // onboarding, writes PowerRecord from BLE sensors, and has an "Average power"
    // row. It just never READ power back — and because that row only earns its
    // place by HAVING a value, it never appeared at all. Not "Not available":
    // absent. (Dart also pins the wire NAME the metric is asked for; that half is
    // Pigeon-specific — Kotlin asks with the typed AggregateMetric itself.)
    @Test
    fun `session metrics carry average power`() = runTest {
        val client = FakeHealthConnectClient()
        client.setPackageName(APP_PACKAGE)
        val id = client.insertRecords(listOf(session())).recordIdsList.single()
        client.insertRecords(
            listOf(
                PowerRecord(
                    startTime = sessionStart,
                    startZoneOffset = ZoneOffset.UTC,
                    endTime = sessionEnd,
                    endZoneOffset = ZoneOffset.UTC,
                    samples = listOf(
                        PowerRecord.Sample(sessionStart, Power.watts(200.0)),
                        PowerRecord.Sample(sessionStart.plusSeconds(600), Power.watts(229.0)),
                    ),
                    metadata = Metadata.autoRecorded(watch),
                ),
            ),
        )

        val read = activity(AggregatingFakeHealthConnectClient(client)).readExerciseSession(
            id = id,
            includeSteps = false,
            includeDistance = false,
            includeTotalCalories = false,
            includeActiveCalories = false,
            includeWheelchairPushes = false,
            includeFloors = false,
            includeElevation = false,
            includeSpeed = false,
            includePower = true,
            includeStepsCadence = false,
            includeCyclingCadence = false,
        )

        assertThat(read!!.averagePowerWatts).isWithin(1e-6).of(214.5)
        // A metric that was not asked for stays null rather than reading zero.
        assertThat(read.averageSpeedMetersPerSecond).isNull()
    }

    // ── vitals ──────────────────────────────────────────────────────────────

    @Test
    fun `BloodPressure entries map systolic, diastolic and ownership`() = runTest {
        val client = FakeHealthConnectClient()
        client.setPackageName(APP_PACKAGE)
        client.insertRecords(
            listOf(
                BloodPressureRecord(
                    time = sessionStart,
                    zoneOffset = ZoneOffset.UTC,
                    systolic = Pressure.millimetersOfMercury(120.0),
                    diastolic = Pressure.millimetersOfMercury(80.0),
                    metadata = Metadata.manualEntry(),
                ),
            ),
        )
        client.setPackageName(OTHER_PACKAGE)
        client.insertRecords(
            listOf(
                BloodPressureRecord(
                    time = sessionStart.plusSeconds(60),
                    zoneOffset = ZoneOffset.UTC,
                    systolic = Pressure.millimetersOfMercury(131.0),
                    diastolic = Pressure.millimetersOfMercury(85.0),
                    metadata = Metadata.manualEntry(),
                ),
            ),
        )

        val entries = VitalsHealthReader(support(AggregatingFakeHealthConnectClient(client)), APP_PACKAGE)
            .readBloodPressureEntries(sessionStart.minusSeconds(60), sessionEnd)
            .sortedBy { it.time }

        assertThat(entries).hasSize(2)
        assertThat(entries.first().systolicMmHg).isEqualTo(120)
        assertThat(entries.first().diastolicMmHg).isEqualTo(80)
        assertThat(entries.first().isOpenVitalsEntry).isTrue()
        // A reading written by another app is readable but never ours to edit.
        assertThat(entries.last().systolicMmHg).isEqualTo(131)
        assertThat(entries.last().isOpenVitalsEntry).isFalse()
        assertThat(entries.last().source).isEqualTo(OTHER_PACKAGE)
    }

    // ── body ────────────────────────────────────────────────────────────────

    @Test
    fun `Weight entries map from records and preserve ownership`() = runTest {
        val client = FakeHealthConnectClient()
        client.setPackageName(OTHER_PACKAGE)
        client.insertRecords(
            listOf(
                WeightRecord(
                    time = sessionStart,
                    zoneOffset = ZoneOffset.UTC,
                    weight = Mass.kilograms(79.0),
                    metadata = Metadata.manualEntry(),
                ),
            ),
        )
        client.setPackageName(APP_PACKAGE)
        client.insertRecords(
            listOf(
                WeightRecord(
                    time = sessionStart.plusSeconds(86_400),
                    zoneOffset = ZoneOffset.UTC,
                    weight = Mass.kilograms(80.5),
                    metadata = Metadata.manualEntry(),
                ),
            ),
        )

        val entries = BodyHealthReader(support(AggregatingFakeHealthConnectClient(client)), APP_PACKAGE)
            .readWeightEntries(sessionStart.minusSeconds(60), sessionStart.plusSeconds(172_800))

        // Ascending by time, exactly as Dart's fixture expects.
        assertThat(entries.map { it.weightKg }).containsExactly(79.0, 80.5).inOrder()
        assertThat(entries.first().isOpenVitalsEntry).isFalse()
        assertThat(entries.first().source).isEqualTo(OTHER_PACKAGE)
        assertThat(entries.last().isOpenVitalsEntry).isTrue()
    }

    // ── sleep ───────────────────────────────────────────────────────────────

    @Test
    fun `a single Sleep session maps its stages`() = runTest {
        val client = FakeHealthConnectClient()
        client.setPackageName(WATCH_PACKAGE)
        val id = client.insertRecords(
            listOf(
                SleepSessionRecord(
                    startTime = nightStart,
                    startZoneOffset = ZoneOffset.UTC,
                    endTime = nightEnd,
                    endZoneOffset = ZoneOffset.UTC,
                    title = "Night",
                    stages = listOf(
                        SleepSessionRecord.Stage(
                            startTime = nightStart,
                            endTime = nightStart.plusSeconds(7_200),
                            stage = SleepSessionRecord.STAGE_TYPE_LIGHT,
                        ),
                        SleepSessionRecord.Stage(
                            startTime = nightStart.plusSeconds(7_200),
                            endTime = nightEnd,
                            stage = SleepSessionRecord.STAGE_TYPE_DEEP,
                        ),
                    ),
                    metadata = Metadata.autoRecorded(watch),
                ),
            ),
        ).recordIdsList.single()

        val session = SleepHealthReader(support(AggregatingFakeHealthConnectClient(client))).readSleepSession(id)

        assertThat(session).isNotNull()
        assertThat(session!!.id).isEqualTo(id)
        assertThat(session.stages).hasSize(2)
        assertThat(session.stages.first().stageType).isEqualTo(SleepSessionRecord.STAGE_TYPE_LIGHT)
        assertThat(session.stages.last().stageType).isEqualTo(SleepSessionRecord.STAGE_TYPE_DEEP)
    }

    // Start zone, End zone, Recording, Last modified and Client version all read
    // "Not available" on the sleep detail screen, for every session ever
    // recorded. The domain model had the fields and the screen rendered them —
    // the message they crossed on simply never carried them.
    @Test
    fun `a Sleep session carries the record provenance the detail screen shows`() =
        runTest {
            val client = FakeHealthConnectClient()
            client.setPackageName(GADGETBRIDGE_PACKAGE)
            val id = client.insertRecords(
                listOf(
                    SleepSessionRecord(
                        startTime = nightStart,
                        // The zone the WRITER recorded the night in, not ours.
                        startZoneOffset = ZoneOffset.ofHours(2),
                        endTime = nightEnd,
                        endZoneOffset = ZoneOffset.ofHours(2),
                        stages = emptyList(),
                        metadata = Metadata.autoRecorded(watch, "gb-sleep-1", 3L),
                    ),
                ),
            ).recordIdsList.single()

            val session = SleepHealthReader(support(AggregatingFakeHealthConnectClient(client))).readSleepSession(id)!!

            assertThat(session.startZoneOffset).isEqualTo(ZoneOffset.ofHours(2))
            assertThat(session.endZoneOffset).isEqualTo(ZoneOffset.ofHours(2))
            assertThat(session.clientRecordId).isEqualTo("gb-sleep-1")
            assertThat(session.clientRecordVersion).isEqualTo(3L)
            assertThat(session.recordingMethod)
                .isEqualTo(Metadata.RECORDING_METHOD_AUTOMATICALLY_RECORDED)
            assertThat(session.lastModifiedTime).isNotNull()
            assertThat(session.source).isEqualTo(GADGETBRIDGE_PACKAGE)
        }

    // Null means "the writer recorded no offset"; zero means UTC. Collapsing the
    // two would print "UTC" for a record that never claimed one.
    @Test
    fun `a session with no zone offsets keeps them null, not zero`() = runTest {
        val client = FakeHealthConnectClient()
        client.setPackageName(WATCH_PACKAGE)
        val id = client.insertRecords(
            listOf(
                SleepSessionRecord(
                    startTime = nightStart,
                    startZoneOffset = null,
                    endTime = nightEnd,
                    endZoneOffset = null,
                    stages = emptyList(),
                    metadata = Metadata.autoRecorded(watch),
                ),
            ),
        ).recordIdsList.single()

        val session = SleepHealthReader(support(AggregatingFakeHealthConnectClient(client))).readSleepSession(id)!!

        assertThat(session.startZoneOffset).isNull()
        assertThat(session.endZoneOffset).isNull()
        // Dart also asserts a null lastModifiedTime here. Health Connect's Kotlin
        // Metadata carries a NON-null lastModifiedTime that the provider stamps,
        // so there is no "absent" to assert — only the zone offsets are optional.
        assertThat(session.clientRecordId).isNull()
    }

    // ── cadence ─────────────────────────────────────────────────────────────

    @Test
    fun `readActivityCadenceSamples maps cycling and steps samples to their own kinds`() =
        runTest {
            val client = FakeHealthConnectClient()
            client.setPackageName("garmin")
            client.insertRecords(
                listOf(
                    CyclingPedalingCadenceRecord(
                        startTime = sessionStart,
                        startZoneOffset = ZoneOffset.UTC,
                        endTime = sessionStart.plusSeconds(600),
                        endZoneOffset = ZoneOffset.UTC,
                        samples = listOf(
                            CyclingPedalingCadenceRecord.Sample(sessionStart, 82.0),
                        ),
                        metadata = Metadata.autoRecorded(watch),
                    ),
                ),
            )
            client.setPackageName("phone")
            client.insertRecords(
                listOf(
                    StepsCadenceRecord(
                        startTime = sessionStart.plusSeconds(1_800),
                        startZoneOffset = ZoneOffset.UTC,
                        endTime = sessionStart.plusSeconds(2_400),
                        endZoneOffset = ZoneOffset.UTC,
                        samples = listOf(
                            StepsCadenceRecord.Sample(sessionStart.plusSeconds(1_800), 164.0),
                        ),
                        metadata = Metadata.autoRecorded(watch),
                    ),
                ),
            )

            val samples = activity(client)
                .readActivityCadenceSamples(sessionStart, sessionEnd)

            assertThat(samples).hasSize(2)
            // 82 rpm on a bike; 164 steps/min running. Same shape, different unit.
            assertThat(samples[0].kind).isEqualTo(ActivityCadenceKind.CYCLING)
            assertThat(samples[0].rate).isWithin(1e-6).of(82.0)
            assertThat(samples[0].source).isEqualTo("garmin")
            assertThat(samples[1].kind).isEqualTo(ActivityCadenceKind.STEPS)
            assertThat(samples[1].rate).isWithin(1e-6).of(164.0)
            assertThat(samples[1].source).isEqualTo("phone")
        }

    @Test
    fun `readActivityCadenceSamples is empty when the device records no cadence`() =
        runTest {
            val samples = activity(FakeHealthConnectClient())
                .readActivityCadenceSamples(sessionStart, sessionEnd)

            assertThat(samples).isEmpty()
        }

    // ── deletes ─────────────────────────────────────────────────────────────

    // The bug this pins on the Dart side: swallowing the failure turned a refused
    // delete into the same null the "no clientRecordId" SUCCESS returns, so the
    // use case never rolled back and the user lost the paired record instead.
    @Test
    fun `deleteHydrationEntry propagates provider failures rather than swallowing them`() =
        runTest {
            val client = FakeHealthConnectClient()
            client.setPackageName(OTHER_PACKAGE)
            val id = client.insertRecords(
                listOf(
                    HydrationRecord(
                        startTime = sessionStart,
                        startZoneOffset = ZoneOffset.UTC,
                        endTime = sessionStart.plusSeconds(1),
                        endZoneOffset = ZoneOffset.UTC,
                        volume = Volume.milliliters(250.0),
                        metadata = Metadata.manualEntry(),
                    ),
                ),
            ).recordIdsList.single()
            val reader = HydrationHealthReader(support(AggregatingFakeHealthConnectClient(client)), APP_PACKAGE)

            val failure = runCatching { reader.deleteHydrationEntry(id) }.exceptionOrNull()

            assertThat(failure).isInstanceOf(IllegalArgumentException::class.java)
        }

    @Test
    fun `deleteHydrationEntry still returns null for a record with no clientRecordId`() =
        runTest {
            val client = FakeHealthConnectClient()
            client.setPackageName(APP_PACKAGE)
            val id = client.insertRecords(
                listOf(
                    HydrationRecord(
                        startTime = sessionStart,
                        startZoneOffset = ZoneOffset.UTC,
                        endTime = sessionStart.plusSeconds(1),
                        endZoneOffset = ZoneOffset.UTC,
                        volume = Volume.milliliters(250.0),
                        metadata = Metadata.manualEntry(),
                    ),
                ),
            ).recordIdsList.single()

            val paired = HydrationHealthReader(support(AggregatingFakeHealthConnectClient(client)), APP_PACKAGE)
                .deleteHydrationEntry(id)

            // Null here is a SUCCESS with nothing to pair-delete, which is exactly
            // why a swallowed failure returning null was so dangerous.
            assertThat(paired).isNull()
        }

    @Test
    fun `deleteNutritionEntry propagates provider failures rather than swallowing them`() =
        runTest {
            val client = FakeHealthConnectClient()
            client.setPackageName(OTHER_PACKAGE)
            val id = client.insertRecords(
                listOf(
                    NutritionRecord(
                        startTime = sessionStart,
                        startZoneOffset = ZoneOffset.UTC,
                        endTime = sessionStart.plusSeconds(1),
                        endZoneOffset = ZoneOffset.UTC,
                        metadata = Metadata.manualEntry(),
                    ),
                ),
            ).recordIdsList.single()
            val reader = NutritionHealthReader(support(AggregatingFakeHealthConnectClient(client)), APP_PACKAGE)

            val failure = runCatching { reader.deleteNutritionEntry(id) }.exceptionOrNull()

            assertThat(failure).isInstanceOf(IllegalArgumentException::class.java)
        }

    // ── harness ─────────────────────────────────────────────────────────────

    private val sessionStart: Instant = Instant.parse("2026-01-02T08:00:00Z")
    private val sessionEnd: Instant = Instant.parse("2026-01-02T09:00:00Z")
    private val nightStart: Instant = Instant.parse("2026-01-02T23:00:00Z")
    private val nightEnd: Instant = Instant.parse("2026-01-03T06:00:00Z")
    private val watch = Device(type = Device.TYPE_WATCH)

    private fun support(client: HealthConnectClient): HealthConnectReaderSupport {
        val diagnostics = mockk<HealthConnectDiagnostics>()
        every { diagnostics.summary() } returns "test"
        return HealthConnectReaderSupport(
            clientProvider = { client },
            diagnostics = diagnostics,
            rateLimitMessage = { "rate limited" },
        )
    }

    private fun activity(client: HealthConnectClient) =
        ActivityHealthReader(support(client), APP_PACKAGE)

    private fun session(): ExerciseSessionRecord = ExerciseSessionRecord(
        startTime = sessionStart,
        startZoneOffset = ZoneOffset.UTC,
        endTime = sessionEnd,
        endZoneOffset = ZoneOffset.UTC,
        exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
        title = "Morning run",
        metadata = Metadata.autoRecorded(watch),
    )

    private suspend fun seeded(vararg records: Record): AggregatingFakeHealthConnectClient {
        val client = FakeHealthConnectClient()
        client.setPackageName(APP_PACKAGE)
        client.insertRecords(records.toList())
        return AggregatingFakeHealthConnectClient(client)
    }

    private companion object {
        const val APP_PACKAGE = "tech.mmarca.openvitals"
        const val OTHER_PACKAGE = "com.other"
        const val WATCH_PACKAGE = "com.watch"
        const val GADGETBRIDGE_PACKAGE = "nodomain.freeyourgadget.gadgetbridge"
    }
}
