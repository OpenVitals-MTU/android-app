package tech.mmarca.openvitals.features.devicesync.store

import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.CervicalMucusRecord
import androidx.health.connect.client.records.CyclingPedalingCadenceRecord
import androidx.health.connect.client.records.ExerciseCompletionGoal
import androidx.health.connect.client.records.ExerciseRoute
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.MenstruationPeriodRecord
import androidx.health.connect.client.records.MindfulnessSessionRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.PlannedExerciseBlock
import androidx.health.connect.client.records.PlannedExerciseSessionRecord
import androidx.health.connect.client.records.PlannedExerciseStep
import androidx.health.connect.client.records.PowerRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.SkinTemperatureRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsCadenceRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Length
import androidx.health.connect.client.units.Mass
import androidx.health.connect.client.units.Power
import androidx.health.connect.client.units.Pressure
import androidx.health.connect.client.units.Temperature
import androidx.health.connect.client.units.TemperatureDelta
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Round-trip and fingerprint-stability tests for the sync record codec, one record per structural family. */
class SyncRecordCodecTest {

    private fun utc(day: Int, hour: Int = 0, minute: Int = 0): Instant =
        Instant.parse("2026-01-%02dT%02d:%02d:00Z".format(day, hour, minute))

    private fun meta(clientRecordId: String = "x"): Metadata =
        Metadata.manualEntry(
            device = Device(type = Device.TYPE_PHONE),
            clientRecordId = clientRecordId,
        )

    private val plusOne: ZoneOffset = ZoneOffset.ofHours(1)

    /** One record of each structural family, to exercise the codec breadth. */
    private val samples: List<Record> = listOf(
        StepsRecord(
            startTime = utc(1, 8),
            startZoneOffset = plusOne,
            endTime = utc(1, 9),
            endZoneOffset = plusOne,
            count = 1200,
            metadata = meta(),
        ),
        WeightRecord(
            time = utc(2),
            zoneOffset = null,
            weight = Mass.kilograms(72.4),
            metadata = meta(),
        ),
        HeartRateRecord(
            startTime = utc(3, 6),
            startZoneOffset = ZoneOffset.ofHours(-5),
            endTime = utc(3, 7),
            endZoneOffset = ZoneOffset.ofHours(-5),
            samples = listOf(
                HeartRateRecord.Sample(utc(3, 6), 60),
                HeartRateRecord.Sample(utc(3, 6, 30), 65),
            ),
            metadata = meta(),
        ),
        SleepSessionRecord(
            startTime = utc(4, 23),
            startZoneOffset = null,
            endTime = utc(5, 7),
            endZoneOffset = null,
            metadata = meta(),
            title = "Night",
            stages = listOf(
                SleepSessionRecord.Stage(
                    startTime = utc(4, 23),
                    endTime = utc(5, 1),
                    stage = SleepSessionRecord.STAGE_TYPE_LIGHT,
                ),
                SleepSessionRecord.Stage(
                    startTime = utc(5, 1),
                    endTime = utc(5, 3),
                    stage = SleepSessionRecord.STAGE_TYPE_DEEP,
                ),
            ),
        ),
        NutritionRecord(
            startTime = utc(6, 12),
            startZoneOffset = null,
            endTime = utc(6, 12, 30),
            endZoneOffset = null,
            metadata = meta(),
            protein = Mass.grams(30.5),
            totalCarbohydrate = Mass.grams(45.0),
            energy = Energy.kilocalories(600.0),
            name = "Lunch",
        ),
        ExerciseSessionRecord(
            startTime = utc(7, 18),
            startZoneOffset = null,
            endTime = utc(7, 19),
            endZoneOffset = null,
            metadata = meta(),
            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
            title = "Evening run",
            notes = null,
            segments = emptyList(),
            laps = emptyList(),
            exerciseRoute = ExerciseRoute(
                listOf(
                    ExerciseRoute.Location(
                        time = utc(7, 18, 1),
                        latitude = 41.1,
                        longitude = 2.1,
                        altitude = Length.meters(12.0),
                    ),
                ),
            ),
        ),
        CervicalMucusRecord(
            time = utc(8),
            zoneOffset = null,
            metadata = meta(),
            appearance = CervicalMucusRecord.APPEARANCE_EGG_WHITE,
            sensation = CervicalMucusRecord.SENSATION_MEDIUM,
        ),
        BloodPressureRecord(
            time = utc(9),
            zoneOffset = null,
            metadata = meta(),
            systolic = Pressure.millimetersOfMercury(118.0),
            diastolic = Pressure.millimetersOfMercury(76.0),
        ),
        TotalCaloriesBurnedRecord(
            startTime = utc(10),
            startZoneOffset = null,
            endTime = utc(10, 23, 59),
            endZoneOffset = null,
            energy = Energy.kilocalories(2200.0),
            metadata = meta(),
        ),
        PowerRecord(
            startTime = utc(11, 6),
            startZoneOffset = null,
            endTime = utc(11, 7),
            endZoneOffset = null,
            samples = listOf(
                PowerRecord.Sample(utc(11, 6), Power.watts(210.0)),
                PowerRecord.Sample(utc(11, 6, 30), Power.watts(195.0)),
            ),
            metadata = meta(),
        ),
        StepsCadenceRecord(
            startTime = utc(12, 6),
            startZoneOffset = null,
            endTime = utc(12, 7),
            endZoneOffset = null,
            samples = listOf(StepsCadenceRecord.Sample(utc(12, 6), 165.5)),
            metadata = meta(),
        ),
        CyclingPedalingCadenceRecord(
            startTime = utc(13, 6),
            startZoneOffset = null,
            endTime = utc(13, 7),
            endZoneOffset = null,
            samples = listOf(CyclingPedalingCadenceRecord.Sample(utc(13, 6), 90.0)),
            metadata = meta(),
        ),
        SkinTemperatureRecord(
            startTime = utc(14, 2),
            startZoneOffset = null,
            endTime = utc(14, 7),
            endZoneOffset = null,
            metadata = meta(),
            deltas = listOf(
                SkinTemperatureRecord.Delta(utc(14, 2), TemperatureDelta.celsius(0.2)),
                SkinTemperatureRecord.Delta(utc(14, 3), TemperatureDelta.celsius(-0.1)),
            ),
            baseline = Temperature.celsius(33.4),
            measurementLocation = SkinTemperatureRecord.MEASUREMENT_LOCATION_FINGER,
        ),
        MenstruationPeriodRecord(
            startTime = utc(15),
            startZoneOffset = null,
            endTime = utc(19),
            endZoneOffset = null,
            metadata = meta(),
        ),
        MindfulnessSessionRecord(
            startTime = utc(19, 7),
            startZoneOffset = null,
            endTime = utc(19, 8),
            endZoneOffset = null,
            metadata = meta(),
            mindfulnessSessionType =
            MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MEDITATION,
            title = "Morning sit",
            notes = null,
        ),
        PlannedExerciseSessionRecord(
            startTime = utc(20, 7),
            startZoneOffset = null,
            endTime = utc(20, 8),
            endZoneOffset = null,
            metadata = meta(),
            blocks = listOf(
                PlannedExerciseBlock(
                    repetitions = 4,
                    description = "sprints",
                    steps = listOf(
                        PlannedExerciseStep(
                            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
                            exercisePhase = PlannedExerciseStep.EXERCISE_PHASE_ACTIVE,
                            description = "fast",
                            completionGoal =
                            ExerciseCompletionGoal.DurationGoal(Duration.ofSeconds(60)),
                            performanceTargets = emptyList(),
                        ),
                        PlannedExerciseStep(
                            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
                            exercisePhase = PlannedExerciseStep.EXERCISE_PHASE_REST,
                            description = "recover",
                            completionGoal = ExerciseCompletionGoal.RepetitionsGoal(10),
                            performanceTargets = emptyList(),
                        ),
                    ),
                ),
            ),
            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
            title = "Interval run",
            notes = "track day",
        ),
    )

    // Round-trip encode/decode.

    @Test
    fun `every sample record survives an encode-decode round trip`() {
        samples.forEach { original ->
            val type = syncRecordTypeName(original)
            val fingerprint = syncFingerprint(original)
            val payload = encodeSyncRecordPayload(original)

            val decoded = decodeSyncRecord(
                recordType = type,
                clientRecordId = fingerprint,
                payload = payload,
            )

            assertEquals(type, syncRecordTypeName(decoded))
            assertEquals(fingerprint, decoded.metadata.clientRecordId)
            // Re-fingerprinting the decoded record yields the same id.
            assertEquals("re-fingerprint of $type", fingerprint, syncFingerprint(decoded))
        }
    }

    @Test
    fun `every syncable type name resolves to a record class`() {
        samples.forEach { record ->
            val type = syncRecordTypeName(record)
            assertEquals(record::class, syncRecordClassFor(type))
        }
    }

    // Fingerprint.

    @Test
    fun `fingerprint is stable and prefixed sync_`() {
        val fingerprint = syncFingerprint(samples.first())

        assertTrue(fingerprint.startsWith(SYNC_CLIENT_RECORD_ID_PREFIX))
        assertEquals(fingerprint, syncFingerprint(samples.first()))
    }

    @Test
    fun `fingerprint differs when identifying content differs`() {
        val a = WeightRecord(
            time = utc(2),
            zoneOffset = null,
            weight = Mass.kilograms(72.4),
            metadata = meta(),
        )
        val b = WeightRecord(
            time = utc(2),
            zoneOffset = null,
            weight = Mass.kilograms(72.5),
            metadata = meta(),
        )

        assertNotEquals(syncFingerprint(a), syncFingerprint(b))
    }

    @Test
    fun `fingerprint ignores the current clientRecordId (content-only)`() {
        val a = WeightRecord(
            time = utc(2),
            zoneOffset = null,
            weight = Mass.kilograms(80.0),
            metadata = meta("apple_health_1"),
        )
        val b = WeightRecord(
            time = utc(2),
            zoneOffset = null,
            weight = Mass.kilograms(80.0),
            metadata = meta("sync_zzz"),
        )

        assertEquals(syncFingerprint(a), syncFingerprint(b))
    }

    @Test
    fun `unit round-trip drift within quantization does not change the fingerprint`() {
        // Mass round-trips through grams; a few ulps of drift must hash identically.
        val exact = WeightRecord(
            time = utc(2),
            zoneOffset = null,
            weight = Mass.kilograms(72.4),
            metadata = meta(),
        )
        val drifted = WeightRecord(
            time = utc(2),
            zoneOffset = null,
            weight = Mass.grams(72.4 * 1000.0).let { Mass.kilograms(it.inKilograms) },
            metadata = meta(),
        )

        assertEquals(syncFingerprint(exact), syncFingerprint(drifted))
    }

    // Out-of-range peer data is clamped, not crashed.

    @Test
    fun `an out-of-range completionKind maps to unknown, not RangeError`() {
        // A corrupt or newer completion-goal kind from another phone must not abort the batch.
        val plan = samples.filterIsInstance<PlannedExerciseSessionRecord>().single()
        val payload = encodeSyncRecordPayload(plan)
            .toString(Charsets.UTF_8)
            .replace(Regex("\"ck\":\\d+"), "\"ck\":99")
            .toByteArray(Charsets.UTF_8)

        val decoded = decodeSyncRecord(
            recordType = syncRecordTypeName(plan),
            clientRecordId = syncFingerprint(plan),
            payload = payload,
        ) as PlannedExerciseSessionRecord

        decoded.blocks.single().steps.forEach { step ->
            assertEquals(ExerciseCompletionGoal.UnknownGoal, step.completionGoal)
        }
    }
}
