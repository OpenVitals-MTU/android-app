package tech.mmarca.openvitals.healthconnect

import tech.mmarca.openvitals.domain.model.NutritionWriteRequest
import tech.mmarca.openvitals.domain.model.NutritionNutrient
import androidx.health.connect.client.records.NutritionRecord
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.BasalBodyTemperatureRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BodyTemperatureMeasurementLocation
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.IntermenstrualBleedingRecord
import androidx.health.connect.client.records.MenstruationFlowRecord
import androidx.health.connect.client.records.MindfulnessSessionRecord
import androidx.health.connect.client.records.OvulationTestRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.testing.FakeHealthConnectClient
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Mass
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import java.time.Instant
import java.time.ZoneOffset
import kotlin.reflect.KClass
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import tech.mmarca.openvitals.core.performance.DispatcherProvider
import tech.mmarca.openvitals.domain.model.BodyMeasurementType
import tech.mmarca.openvitals.domain.model.BodyMeasurementWriteRequest
import tech.mmarca.openvitals.domain.model.CycleEntryKind
import tech.mmarca.openvitals.domain.model.CycleEntryWriteRequest
import tech.mmarca.openvitals.domain.model.HydrationWriteRequest
import tech.mmarca.openvitals.domain.model.MindfulnessSessionWriteRequest
import tech.mmarca.openvitals.domain.model.VitalsMeasurementType
import tech.mmarca.openvitals.domain.model.VitalsMeasurementWriteRequest

/**
 * Write, edit and delete of one entry, per reader, on Google's fake client. The activity
 * paths have their own class. These are the calls that change a user's health records, and
 * until the audit none of them ran in a test on a real reader.
 */
class EntryWritePathsTest {

    private val fake = FakeHealthConnectClient().apply { setPackageName(APP_PACKAGE) }
    private val client: HealthConnectClient = AggregatingFakeHealthConnectClient(fake, platformTimeRange = true)

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

    // Body.

    @Test
    fun `a weight entry is written, edited in place and deleted`() = onTheTestClock {
        val reader = BodyHealthReader(support(), APP_PACKAGE)
        reader.writeBodyMeasurementEntry(BodyMeasurementWriteRequest(BodyMeasurementType.WEIGHT, NOON, 72.5))
        val id = all(WeightRecord::class).single().metadata.id

        reader.updateBodyMeasurementEntry(id, BodyMeasurementWriteRequest(BodyMeasurementType.WEIGHT, NOON, 71.9))

        val edited = all(WeightRecord::class).single()
        assertThat(edited.weight.inKilograms).isWithin(1e-9).of(71.9)
        assertThat(edited.metadata.id).isEqualTo(id)

        reader.deleteBodyMeasurementEntry(BodyMeasurementType.WEIGHT, id)
        assertThat(all(WeightRecord::class)).isEmpty()
    }

    @Test
    fun `another app's weight can be neither edited nor deleted`() = onTheTestClock {
        val foreignId = insertAsAnotherApp(
            WeightRecord(NOON, ZoneOffset.UTC, Mass.kilograms(80.0), Metadata.manualEntry()),
        )
        val reader = BodyHealthReader(support(), APP_PACKAGE)

        val edit = runCatching {
            reader.updateBodyMeasurementEntry(foreignId, BodyMeasurementWriteRequest(BodyMeasurementType.WEIGHT, NOON, 1.0))
        }
        val delete = runCatching { reader.deleteBodyMeasurementEntry(BodyMeasurementType.WEIGHT, foreignId) }

        assertThat(edit.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(delete.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(all(WeightRecord::class).single().weight.inKilograms).isWithin(1e-9).of(80.0)
    }

    // Vitals.

    @Test
    fun `an oxygen saturation entry is written, edited in place and deleted`() = onTheTestClock {
        val reader = VitalsHealthReader(support(), APP_PACKAGE)
        reader.writeVitalsMeasurementEntry(VitalsMeasurementWriteRequest(VitalsMeasurementType.SPO2, NOON, 97.0))
        val id = all(OxygenSaturationRecord::class).single().metadata.id

        reader.updateVitalsMeasurementEntry(id, VitalsMeasurementWriteRequest(VitalsMeasurementType.SPO2, NOON, 95.0))

        assertThat(all(OxygenSaturationRecord::class).single().percentage.value).isWithin(1e-9).of(95.0)

        reader.deleteVitalsMeasurementEntry(VitalsMeasurementType.SPO2, id)
        assertThat(all(OxygenSaturationRecord::class)).isEmpty()
    }

    @Test
    fun `editing a blood pressure reading leaves exactly one reading`() = onTheTestClock {
        val reader = VitalsHealthReader(support(), APP_PACKAGE)
        reader.writeVitalsMeasurementEntry(
            VitalsMeasurementWriteRequest(VitalsMeasurementType.BLOOD_PRESSURE, NOON, 128.0, secondaryValue = 84.0),
        )
        val id = all(BloodPressureRecord::class).single().metadata.id

        reader.updateVitalsMeasurementEntry(
            id,
            VitalsMeasurementWriteRequest(VitalsMeasurementType.BLOOD_PRESSURE, NOON, 121.0, secondaryValue = 79.0),
        )

        // This edit is an insert under a client id, then a delete only when the id changed.
        val reading = all(BloodPressureRecord::class).single()
        assertThat(reading.systolic.inMillimetersOfMercury).isWithin(1e-9).of(121.0)
        assertThat(reading.diastolic.inMillimetersOfMercury).isWithin(1e-9).of(79.0)
    }

    // Mindfulness.

    @Test
    fun `a mindfulness session is written, edited in place and deleted`() = onTheTestClock {
        val reader = MindfulnessHealthReader(support(), APP_PACKAGE)
        reader.writeMindfulnessSessionEntry(MindfulnessSessionWriteRequest("Breathing", NOON, NOON.plusSeconds(600)))
        val id = all(MindfulnessSessionRecord::class).single().metadata.id

        reader.updateMindfulnessSessionEntry(
            id,
            MindfulnessSessionWriteRequest("Evening breathing", NOON, NOON.plusSeconds(900), notes = "calm"),
        )

        val edited = all(MindfulnessSessionRecord::class).single()
        assertThat(edited.title).isEqualTo("Evening breathing")
        assertThat(edited.endTime).isEqualTo(NOON.plusSeconds(900))
        assertThat(edited.notes).isEqualTo("calm")

        reader.deleteMindfulnessSessionEntry(id)
        assertThat(all(MindfulnessSessionRecord::class)).isEmpty()
    }

    // A logged drink is two records. They are written together, so they must be edited together.

    @Test
    fun `an edited coffee takes its caffeine with it`() = onTheTestClock {
        val hydration = HydrationHealthReader(support(), APP_PACKAGE)
        val nutrition = NutritionHealthReader(support(), APP_PACKAGE)
        val clientId = hydration.writeHydrationEntry(HydrationWriteRequest(NOON, volumeLiters = 0.25))
        nutrition.writeNutritionEntry(
            NutritionWriteRequest(
                time = NOON,
                nutrientValues = mapOf(NutritionNutrient.CAFFEINE to 0.080),
                name = "Coffee",
                associatedHydrationClientRecordId = clientId,
                endTime = NOON.plusSeconds(600),
            ),
        )
        val id = all(HydrationRecord::class).single().metadata.id

        // An hour later, and a double.
        val change = hydration.updateHydrationEntry(id, HydrationWriteRequest(NOON.plusSeconds(3_600), volumeLiters = 0.5))
        nutrition.updateHydrationNutritionEntry(change)

        val coffee = all(NutritionRecord::class).single()
        assertThat(coffee.startTime).isEqualTo(NOON.plusSeconds(3_600))
        assertThat(coffee.endTime).isEqualTo(NOON.plusSeconds(3_600 + 600))
        assertThat(coffee.caffeine!!.inGrams).isWithin(1e-9).of(0.160)
        assertThat(coffee.name).isEqualTo("Coffee")
        // The link survives the edit, so a later delete still finds the caffeine.
        val edited = all(HydrationRecord::class).single()
        assertThat(edited.metadata.clientRecordId).isEqualTo(clientId)
        assertThat(edited.metadata.id).isEqualTo(id)
        assertThat(edited.volume.inLiters).isWithin(1e-9).of(0.5)
    }

    @Test
    fun `editing plain water touches no nutrition record`() = onTheTestClock {
        val hydration = HydrationHealthReader(support(), APP_PACKAGE)
        val nutrition = NutritionHealthReader(support(), APP_PACKAGE)
        hydration.writeHydrationEntry(HydrationWriteRequest(NOON, volumeLiters = 0.25))
        nutrition.writeNutritionEntry(
            NutritionWriteRequest(time = NOON, nutrientValues = mapOf(NutritionNutrient.CAFFEINE to 0.080), name = "Espresso"),
        )
        val id = all(HydrationRecord::class).single().metadata.id

        val change = hydration.updateHydrationEntry(id, HydrationWriteRequest(NOON.plusSeconds(60), volumeLiters = 0.5))
        nutrition.updateHydrationNutritionEntry(change)

        // The espresso was logged on its own. It is not this glass of water's other half.
        val espresso = all(NutritionRecord::class).single()
        assertThat(espresso.startTime).isEqualTo(NOON)
        assertThat(espresso.caffeine!!.inGrams).isWithin(1e-9).of(0.080)
    }

    // Food.

    @Test
    fun `a logged food is marked as food, so the beverage screens can skip it`() = onTheTestClock {
        val nutrition = NutritionHealthReader(support(), APP_PACKAGE)

        nutrition.writeNutritionEntry(
            NutritionWriteRequest(
                time = NOON,
                nutrientValues = mapOf(
                    NutritionNutrient.ENERGY to 105.0,
                    NutritionNutrient.TOTAL_CARBOHYDRATE to 27.0,
                ),
                name = "Banana",
                foodId = "banana-id",
            ),
        )

        val banana = all(NutritionRecord::class).single()
        assertThat(banana.metadata.clientRecordId).startsWith("openvitals_food_")
        assertThat(banana.metadata.clientRecordId).contains("_banana-id_")
        assertThat(banana.name).isEqualTo("Banana")
        assertThat(banana.energy!!.inKilocalories).isWithin(1e-9).of(105.0)
        assertThat(banana.totalCarbohydrate!!.inGrams).isWithin(1e-9).of(27.0)
    }

    // Hydration.

    @Test
    fun `a drink is written, edited to one record and deleted`() = onTheTestClock {
        val reader = HydrationHealthReader(support(), APP_PACKAGE)
        reader.writeHydrationEntry(HydrationWriteRequest(NOON, volumeLiters = 0.25))
        val id = all(HydrationRecord::class).single().metadata.id

        reader.updateHydrationEntry(id, HydrationWriteRequest(NOON, volumeLiters = 0.5))

        assertThat(all(HydrationRecord::class).single().volume.inLiters).isWithin(1e-9).of(0.5)

        reader.deleteHydrationEntry(all(HydrationRecord::class).single().metadata.id)
        assertThat(all(HydrationRecord::class)).isEmpty()
    }

    // Cycle entries.

    @Test
    fun `a flow entry is written, read back, edited in place and deleted`() = onTheTestClock {
        val reader = CycleHealthReader(support(), APP_PACKAGE)
        reader.writeCycleEntry(CycleEntryWriteRequest(CycleEntryKind.MENSTRUATION_FLOW, NOON, flow = MenstruationFlowRecord.FLOW_LIGHT))
        val id = all(MenstruationFlowRecord::class).single().metadata.id

        val loaded = reader.readCycleEntry(CycleEntryKind.MENSTRUATION_FLOW, id)
        assertThat(loaded?.flow).isEqualTo(MenstruationFlowRecord.FLOW_LIGHT)
        assertThat(loaded?.isOpenVitalsEntry).isTrue()

        reader.updateCycleEntry(id, CycleEntryWriteRequest(CycleEntryKind.MENSTRUATION_FLOW, NOON, flow = MenstruationFlowRecord.FLOW_HEAVY))

        val edited = all(MenstruationFlowRecord::class).single()
        assertThat(edited.flow).isEqualTo(MenstruationFlowRecord.FLOW_HEAVY)
        assertThat(edited.metadata.id).isEqualTo(id)

        reader.deleteCycleEntry(CycleEntryKind.MENSTRUATION_FLOW, id)
        assertThat(all(MenstruationFlowRecord::class)).isEmpty()
    }

    @Test
    fun `a basal temperature keeps its value and where it was measured`() = onTheTestClock {
        val reader = CycleHealthReader(support(), APP_PACKAGE)
        reader.writeCycleEntry(
            CycleEntryWriteRequest(
                kind = CycleEntryKind.BASAL_BODY_TEMPERATURE,
                time = NOON,
                temperatureCelsius = 36.6,
                measurementLocation = BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_MOUTH,
            ),
        )
        val id = all(BasalBodyTemperatureRecord::class).single().metadata.id

        val loaded = reader.readCycleEntry(CycleEntryKind.BASAL_BODY_TEMPERATURE, id)

        assertThat(loaded?.temperatureCelsius).isWithin(1e-9).of(36.6)
        assertThat(loaded?.measurementLocation).isEqualTo(BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_MOUTH)
    }

    @Test
    fun `deleting one kind of cycle entry leaves the others of that moment alone`() = onTheTestClock {
        val reader = CycleHealthReader(support(), APP_PACKAGE)
        reader.writeCycleEntry(CycleEntryWriteRequest(CycleEntryKind.SPOTTING, NOON))
        reader.writeCycleEntry(
            CycleEntryWriteRequest(CycleEntryKind.OVULATION_TEST, NOON, ovulationTestResult = OvulationTestRecord.RESULT_POSITIVE),
        )
        val spottingId = all(IntermenstrualBleedingRecord::class).single().metadata.id

        reader.deleteCycleEntry(CycleEntryKind.SPOTTING, spottingId)

        assertThat(all(IntermenstrualBleedingRecord::class)).isEmpty()
        assertThat(all(OvulationTestRecord::class).single().result).isEqualTo(OvulationTestRecord.RESULT_POSITIVE)
    }

    @Test
    fun `another app's cycle entry can be neither edited nor deleted`() = onTheTestClock {
        val foreignId = insertAsAnotherApp(
            MenstruationFlowRecord(NOON, ZoneOffset.UTC, Metadata.manualEntry(), MenstruationFlowRecord.FLOW_MEDIUM),
        )
        val reader = CycleHealthReader(support(), APP_PACKAGE)

        val edit = runCatching {
            reader.updateCycleEntry(
                foreignId,
                CycleEntryWriteRequest(CycleEntryKind.MENSTRUATION_FLOW, NOON, flow = MenstruationFlowRecord.FLOW_LIGHT),
            )
        }
        val delete = runCatching { reader.deleteCycleEntry(CycleEntryKind.MENSTRUATION_FLOW, foreignId) }

        assertThat(edit.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(delete.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(all(MenstruationFlowRecord::class).single().flow).isEqualTo(MenstruationFlowRecord.FLOW_MEDIUM)
    }

    // Step-derived distance: the one reader that deletes records nobody asked it to.

    @Test
    fun `a second reconcile adds nothing, and a day another app covers is left alone`() = onTheTestClock {
        val zone = java.time.ZoneId.systemDefault()
        val covered = DAY.minusDays(1)
        insertAsAnotherApp(
            androidx.health.connect.client.records.DistanceRecord(
                startTime = covered.atStartOfDay(zone).toInstant().plusSeconds(3_600),
                startZoneOffset = null,
                endTime = covered.atStartOfDay(zone).toInstant().plusSeconds(7_200),
                endZoneOffset = null,
                distance = androidx.health.connect.client.units.Length.meters(4_000.0),
                metadata = Metadata.manualEntry(),
            ),
        )
        val reader = StepDistanceHealthReader(support(), APP_PACKAGE)
        val steps = mapOf(DAY to 10_000L, covered to 8_000L)

        reader.reconcileStepDerivedDistance(covered..DAY, steps, strideMeters = 0.7)
        reader.reconcileStepDerivedDistance(covered..DAY, steps, strideMeters = 0.7)

        val distances = allDistances()
        val own = distances.filter { it.metadata.dataOrigin.packageName == APP_PACKAGE }
        assertThat(own).hasSize(1)
        assertThat(own.single().distance.inMeters).isWithin(1e-6).of(7_000.0)
        assertThat(distances.filter { it.metadata.dataOrigin.packageName != APP_PACKAGE }).hasSize(1)
    }

    @Test
    fun `a purge removes the derived records and nothing else`() = onTheTestClock {
        val reader = StepDistanceHealthReader(support(), APP_PACKAGE)
        reader.reconcileStepDerivedDistance(DAY..DAY, mapOf(DAY to 10_000L), strideMeters = 0.7)
        val zone = java.time.ZoneId.systemDefault()
        insertAsAnotherApp(
            androidx.health.connect.client.records.DistanceRecord(
                startTime = DAY.minusDays(2).atStartOfDay(zone).toInstant().plusSeconds(3_600),
                startZoneOffset = null,
                endTime = DAY.minusDays(2).atStartOfDay(zone).toInstant().plusSeconds(7_200),
                endZoneOffset = null,
                distance = androidx.health.connect.client.units.Length.meters(4_000.0),
                metadata = Metadata.manualEntry(),
            ),
        )

        reader.purgeStepDerivedDistance(DAY.minusDays(5)..DAY)

        assertThat(allDistances().map { it.metadata.dataOrigin.packageName }).containsExactly("com.example.otherapp")
    }

    private suspend fun allDistances() = client.readRecords(
        ReadRecordsRequest(
            androidx.health.connect.client.records.DistanceRecord::class,
            TimeRangeFilter.between(DAY_START.minusSeconds(10 * 86_400), DAY_END.plusSeconds(86_400)),
        ),
    ).records

    private val testDispatcher = StandardTestDispatcher()
    private val testDispatchers = object : DispatcherProvider {
        override val main = testDispatcher
        override val io = testDispatcher
        override val default = testDispatcher
    }

    /** Every read hops to the support's dispatcher, so on this one the whole read is test time. */
    private fun onTheTestClock(body: suspend CoroutineScope.() -> Unit) = runTest(testDispatcher) { body() }

    private suspend fun <T : Record> all(type: KClass<T>): List<T> =
        client.readRecords(ReadRecordsRequest(type, TimeRangeFilter.between(DAY_START, DAY_END))).records

    /** The fake stamps the origin from its package name at insert. */
    private suspend fun insertAsAnotherApp(record: Record): String {
        fake.setPackageName("com.example.otherapp")
        fake.insertRecords(listOf(record))
        fake.setPackageName(APP_PACKAGE)
        return client.readRecords(ReadRecordsRequest(record::class, TimeRangeFilter.after(Instant.EPOCH))).records
            .single { it.metadata.dataOrigin.packageName != APP_PACKAGE }.metadata.id
    }

    private fun support(): HealthConnectReaderSupport {
        val diagnostics = mockk<HealthConnectDiagnostics>()
        every { diagnostics.summary() } returns "test"
        return HealthConnectReaderSupport(
            clientProvider = { client },
            diagnostics = diagnostics,
            rateLimitMessage = { "rate limited" },
            dispatchers = testDispatchers,
        )
    }

    private companion object {
        const val APP_PACKAGE = "tech.mmarca.openvitals"
        val DAY_START: Instant = Instant.parse("2026-03-10T00:00:00Z")
        val DAY_END: Instant = Instant.parse("2026-03-11T00:00:00Z")
        val NOON: Instant = Instant.parse("2026-03-10T12:00:00Z")
        val DAY: java.time.LocalDate = java.time.LocalDate.of(2026, 3, 10)
    }
}
