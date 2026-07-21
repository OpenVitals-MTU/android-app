package tech.mmarca.openvitals.health_connect_native

import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.CervicalMucusRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.MenstruationPeriodRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.PowerRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.SkinTemperatureRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.testing.FakeHealthConnectClient
import androidx.health.connect.client.time.TimeRangeFilter
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.reflect.KClass

/**
 * Write ↔ read symmetry for every record shape, on the JVM against Google's
 * FakeHealthConnectClient: build a Record from an [ImportRecordMsg] via
 * [ImportRecordsBuilder], insert it, read it back, convert via
 * [RecordToImportMsg], and assert the round-tripped message matches. This proves
 * the full-fidelity sync read/write without a device.
 */
class ImportRoundTripTest {
  private val start = 1_700_000_000_000L
  private val end = start + 3_600_000L

  private fun msg(
    type: String,
    endMs: Long? = null,
    doubles: Map<String, Double> = emptyMap(),
    ints: Map<String, Long> = emptyMap(),
    name: String? = null,
    notes: String? = null,
    samples: List<ImportSampleMsg> = emptyList(),
    sleepStages: List<ImportSleepStageMsg> = emptyList(),
    routePoints: List<ExerciseRoutePointMsg> = emptyList(),
    segments: List<ExerciseSegmentMsg> = emptyList(),
    laps: List<ExerciseLapMsg> = emptyList(),
  ) = ImportRecordMsg(
    recordType = type, clientRecordId = "cid_$type", startEpochMs = start, endEpochMs = endMs,
    startZoneOffsetSeconds = 3600L, endZoneOffsetSeconds = if (endMs != null) 3600L else null,
    doubleFields = doubles, intFields = ints, name = name, samples = samples,
    sleepStages = sleepStages, routePoints = routePoints, dataOriginPackage = null, notes = notes,
    segments = segments, laps = laps, plannedExerciseId = null, plannedBlocks = emptyList(),
  )

  private suspend fun roundTrip(source: ImportRecordMsg, type: KClass<out Record>): ImportRecordMsg {
    val client = FakeHealthConnectClient()
    client.insertRecords(listOf(ImportRecordsBuilder.build(source)))
    val record = client.readRecords(
      ReadRecordsRequest(type, TimeRangeFilter.between(java.time.Instant.ofEpochMilli(0), java.time.Instant.ofEpochMilli(end + 1))),
    ).records.first()
    return RecordToImportMsg.convert(record)!!
  }

  @Test fun `steps round-trips`() = runTest {
    // count rides in doubleFields (the write path's req("count")), like the mapper.
    val out = roundTrip(msg("Steps", endMs = end, doubles = mapOf("count" to 1234.0)), StepsRecord::class)
    assertThat(out.recordType).isEqualTo("Steps")
    assertThat(out.startEpochMs).isEqualTo(start)
    assertThat(out.endEpochMs).isEqualTo(end)
    assertThat(out.doubleFields["count"]).isWithin(1e-6).of(1234.0)
  }

  @Test fun `weight round-trips`() = runTest {
    val out = roundTrip(msg("Weight", doubles = mapOf("weightKg" to 72.4)), WeightRecord::class)
    assertThat(out.endEpochMs).isNull()
    assertThat(out.doubleFields["weightKg"]).isWithin(1e-6).of(72.4)
  }

  @Test fun `total calories round-trips`() = runTest {
    val out = roundTrip(msg("TotalCaloriesBurned", endMs = end, doubles = mapOf("energyKcal" to 512.0)), TotalCaloriesBurnedRecord::class)
    assertThat(out.doubleFields["energyKcal"]).isWithin(1e-6).of(512.0)
  }

  @Test fun `heart rate series round-trips`() = runTest {
    val samples = listOf(ImportSampleMsg(start, 60.0), ImportSampleMsg(start + 1000, 65.0))
    val out = roundTrip(msg("HeartRate", endMs = end, samples = samples), HeartRateRecord::class)
    assertThat(out.samples.map { it.value }).containsExactly(60.0, 65.0).inOrder()
  }

  @Test fun `power series round-trips`() = runTest {
    val out = roundTrip(msg("Power", endMs = end, samples = listOf(ImportSampleMsg(start, 210.0))), PowerRecord::class)
    assertThat(out.samples.single().value).isWithin(1e-6).of(210.0)
  }

  @Test fun `sleep with stages round-trips`() = runTest {
    val stages = listOf(ImportSleepStageMsg(start, start + 1800_000, 4L), ImportSleepStageMsg(start + 1800_000, end, 5L))
    val out = roundTrip(msg("Sleep", endMs = end, name = "Night", notes = "well rested", sleepStages = stages), SleepSessionRecord::class)
    assertThat(out.name).isEqualTo("Night")
    assertThat(out.notes).isEqualTo("well rested")
    assertThat(out.sleepStages.map { it.stage }).containsExactly(4L, 5L).inOrder()
  }

  @Test fun `nutrition round-trips its nutrients`() = runTest {
    val out = roundTrip(
      msg("Nutrition", endMs = end, name = "Lunch", doubles = mapOf("energyKcal" to 600.0, "protein" to 30.5, "totalCarbohydrate" to 45.0)),
      NutritionRecord::class,
    )
    assertThat(out.name).isEqualTo("Lunch")
    assertThat(out.doubleFields["protein"]).isWithin(1e-6).of(30.5)
    assertThat(out.doubleFields["totalCarbohydrate"]).isWithin(1e-6).of(45.0)
  }

  @Test fun `exercise with route and notes round-trips`() = runTest {
    // Segments/laps have Health-Connect session-type compatibility rules (real
    // records already satisfy them); here we round-trip type + notes + route.
    val out = roundTrip(
      msg("ExerciseSession", endMs = end, name = "Run", notes = "morning",
        ints = mapOf("exerciseType" to 56L),
        routePoints = listOf(ExerciseRoutePointMsg(start, 41.1, 2.1, 12.0, null, null))),
      ExerciseSessionRecord::class,
    )
    assertThat(out.intFields["exerciseType"]).isEqualTo(56L)
    assertThat(out.name).isEqualTo("Run")
    assertThat(out.notes).isEqualTo("morning")
    assertThat(out.routePoints).hasSize(1)
    assertThat(out.routePoints.single().latitude).isWithin(1e-6).of(41.1)
  }

  @Test fun `skin temperature baseline and deltas round-trip`() = runTest {
    val out = roundTrip(
      msg("SkinTemperature", endMs = end, doubles = mapOf("baselineCelsius" to 33.5),
        ints = mapOf("measurementLocation" to 1L),
        samples = listOf(ImportSampleMsg(start, 0.3), ImportSampleMsg(start + 1000, -0.2))),
      SkinTemperatureRecord::class,
    )
    assertThat(out.doubleFields["baselineCelsius"]).isWithin(1e-6).of(33.5)
    assertThat(out.samples.map { it.value }).containsExactly(0.3, -0.2).inOrder()
  }

  @Test fun `blood pressure round-trips both values`() = runTest {
    val out = roundTrip(msg("BloodPressure", doubles = mapOf("systolicMmHg" to 118.0, "diastolicMmHg" to 76.0)), BloodPressureRecord::class)
    assertThat(out.doubleFields["systolicMmHg"]).isWithin(1e-6).of(118.0)
    assertThat(out.doubleFields["diastolicMmHg"]).isWithin(1e-6).of(76.0)
  }

  @Test fun `cervical mucus round-trips two ints`() = runTest {
    val out = roundTrip(msg("CervicalMucus", ints = mapOf("appearance" to 5L, "sensation" to 2L)), CervicalMucusRecord::class)
    assertThat(out.intFields["appearance"]).isEqualTo(5L)
    assertThat(out.intFields["sensation"]).isEqualTo(2L)
  }

  @Test fun `menstruation period round-trips as interval`() = runTest {
    val out = roundTrip(msg("MenstruationPeriod", endMs = end), MenstruationPeriodRecord::class)
    assertThat(out.recordType).isEqualTo("MenstruationPeriod")
    assertThat(out.endEpochMs).isEqualTo(end)
  }
}
