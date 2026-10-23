package tech.mmarca.openvitals.features.imports.csv

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.Record
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.io.ByteArrayInputStream
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.data.repository.AppleHealthImportRepository
import tech.mmarca.openvitals.data.repository.contract.CsvImportPreferences
import tech.mmarca.openvitals.data.repository.contract.HealthRepository
import tech.mmarca.openvitals.domain.model.BpRecordValues
import tech.mmarca.openvitals.domain.model.NutritionNutrient
import tech.mmarca.openvitals.healthconnect.HealthConnectManager
import tech.mmarca.openvitals.util.MainDispatcherRule

private const val WriteWeight = "android.permission.health.WRITE_WEIGHT"
private const val WriteBodyWaterMass = "android.permission.health.WRITE_BODY_WATER_MASS"
private const val WriteNutrition = "android.permission.health.WRITE_NUTRITION"
private const val WriteBloodPressure = "android.permission.health.WRITE_BLOOD_PRESSURE"

/** The Withings shape, three rows. */
private const val WithingsCsv =
    "Date,\"Weight (kg)\",\"Fat mass (kg)\",\"Bone mass (kg)\"\n" +
        "2026-07-01 08:12:00,78.4,15.2,3.1\n" +
        "2026-07-02 08:14:00,78.6,15.3,3.1\n" +
        "2026-07-03 08:11:00,78.2,15.1,3.1\n"

/** A home monitor's log: the position in words, the cuff never written down. */
private const val MonitorCsv =
    "timestamp,systolic,diastolic,position\n" +
        "2026-10-03 00:03,123,88,Sitting\n" +
        "2026-10-03 10:40,149,97,seated\n"

/** The two preferences in memory, as the store would keep them between imports. */
private class InMemoryCsvImportPreferences : CsvImportPreferences {
    override var defaultBodyPosition: Int = BpRecordValues.BODY_POSITION_UNKNOWN
    override var defaultCuffLocation: Int = BpRecordValues.MEASUREMENT_LOCATION_UNKNOWN
}

/** A food log kept in a spreadsheet: energy in kJ, as some labels print it. */
private const val FoodLogCsv =
    "Date,Food,\"Energy (kJ)\",\"Sodium (mg)\"\n" +
        "2026-10-04 08:00:00,Oatmeal,1464,120\n" +
        "2026-10-04 12:30:00,Soup,753,800\n"

@OptIn(ExperimentalCoroutinesApi::class)
class CsvImportViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val context = mockk<Context>()
    private val resolver = mockk<ContentResolver>()
    private val importRepository = mockk<AppleHealthImportRepository>()
    private val healthRepository = mockk<HealthRepository>()
    private val inserted = mutableListOf<Record>()
    private val preferences = InMemoryCsvImportPreferences()
    private val bloodPressureLabels = mockk<CsvBloodPressureLabelSource>()

    /** What Health Connect currently reports as granted; mutable so a test can grant mid-flight. */
    private var granted: Set<String> = emptySet()

    private val allWritePermissions: Set<String> =
        CsvMetricCatalog.values.mapTo(mutableSetOf()) { it.writePermission } +
            CsvNutritionWritePermission +
            CsvBloodPressureWritePermission

    @Before
    fun setUp() {
        every { context.contentResolver } returns resolver
        coEvery { healthRepository.grantedPermissions() } answers { granted }
        coEvery {
            importRepository.findMatchingImportedClientRecordIds(any(), any(), any(), any())
        } returns emptySet()
        coEvery { importRepository.insertImportedRecords(any()) } answers {
            inserted.addAll(arg<List<Record>>(0))
        }
        every { bloodPressureLabels.labels() } returns CsvBloodPressureLabels(
            bodyPositions = listOf(mapOf(BpRecordValues.BODY_POSITION_SITTING_DOWN to "Sitting")),
        )
    }

    /** [unsupported] is what the installed provider does not define; the supported set is the catalog minus those. */
    private fun viewModel(
        granted: Set<String> = emptySet(),
        unsupported: Set<String> = emptySet(),
    ): CsvImportViewModel {
        this.granted = granted
        val manager = mockk<HealthConnectManager>()
        every { manager.dataImportWritePermissions } returns (allWritePermissions - unsupported)
        return CsvImportViewModel(
            context = context,
            importService = CsvImportService(importRepository, mockk(relaxed = true)),
            healthRepository = healthRepository,
            preferences = preferences,
            bloodPressureLabels = bloodPressureLabels,
            healthConnectManager = manager,
        )
    }

    /** A SAF-style document: a name, a size, and a fresh stream per open. */
    private fun documentUri(name: String, content: String): Uri {
        val uri = mockk<Uri>()
        val bytes = content.toByteArray(Charsets.UTF_8)
        every { resolver.openInputStream(uri) } answers { ByteArrayInputStream(bytes) }
        val cursor = mockk<Cursor>(relaxed = true)
        every { cursor.moveToFirst() } returns true
        every { cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME) } returns 0
        every { cursor.getColumnIndex(OpenableColumns.SIZE) } returns 1
        every { cursor.isNull(any()) } returns false
        every { cursor.getString(0) } returns name
        every { cursor.getLong(1) } returns bytes.size.toLong()
        every { resolver.query(uri, any(), null, null, null) } returns cursor
        return uri
    }

    private fun withingsUri(): Uri = documentUri("withings.csv", WithingsCsv)

    private suspend fun CsvImportViewModel.awaitLoaded(): CsvImportState =
        uiState.first { !it.isLoadingFile }

    private suspend fun CsvImportViewModel.awaitDone(): CsvImportState =
        uiState.first { it.step == CsvImportStep.DONE }

    @Test
    fun `picking a file advances to the mapping step and exposes its columns`() = runTest {
        val vm = viewModel()

        vm.pickFile(withingsUri())

        val state = vm.awaitLoaded()
        assertEquals(CsvImportStep.MAPPING, state.step)
        assertEquals(
            listOf("Date", "Weight (kg)", "Fat mass (kg)", "Bone mass (kg)"),
            state.sample?.headerRow,
        )
    }

    @Test
    fun `a permission granted after the file was picked still reaches the state`() = runTest {
        val vm = viewModel()
        vm.pickFile(withingsUri())
        vm.awaitLoaded()
        vm.setColumnRole(1, role = CsvColumnRole.METRIC, metric = CsvImportMetric.WEIGHT)
        assertEquals(setOf(WriteWeight), vm.uiState.value.missingPermissions)

        // The user grants it in the dialog; the post-request refresh re-resolves the granted set.
        granted = setOf(WriteWeight)
        vm.refreshPermissions()
        vm.uiState.first { WriteWeight in it.granted }

        assertTrue(vm.uiState.value.missingPermissions.isEmpty())
        // ...and the mapping is still there.
        assertNotNull(vm.uiState.value.mapping)
    }

    @Test
    fun `the date column is pre-selected but no metric is guessed`() = runTest {
        val vm = viewModel()

        vm.pickFile(withingsUri())
        vm.awaitLoaded()

        val mapping = vm.uiState.value.mapping!!
        assertEquals(0, mapping.timestampColumn?.columnIndex)
        assertTrue(mapping.metricColumns.isEmpty())
    }

    @Test
    fun `a freshly picked file cannot continue until a metric is mapped`() = runTest {
        val vm = viewModel()

        vm.pickFile(withingsUri())
        vm.awaitLoaded()
        assertFalse(vm.uiState.value.canContinue)

        vm.setColumnRole(1, role = CsvColumnRole.METRIC, metric = CsvImportMetric.WEIGHT)

        assertTrue(vm.uiState.value.canContinue)
    }

    @Test
    fun `mapping a column defaults its unit from the column's own header`() = runTest {
        val vm = viewModel()

        vm.pickFile(withingsUri())
        vm.awaitLoaded()
        vm.setColumnRole(1, role = CsvColumnRole.METRIC, metric = CsvImportMetric.WEIGHT)

        val column = vm.uiState.value.mapping!!.columns.first { it.columnIndex == 1 }
        assertEquals(CsvDirectValue(CsvUnit.KILOGRAMS), column.effectiveInterpretation)
    }

    @Test
    fun `mapping fat mass in kg to body fat without a weight column blocks continuing`() = runTest {
        val vm = viewModel()

        vm.pickFile(withingsUri())
        vm.awaitLoaded()
        vm.setColumnRole(2, role = CsvColumnRole.METRIC, metric = CsvImportMetric.BODY_FAT)

        val state = vm.uiState.value
        assertTrue(CsvMappingIssue.MASS_SHARE_NEEDS_WEIGHT_COLUMN in state.issues)
        assertFalse(state.canContinue)
    }

    @Test
    fun `mapping the weight column too clears the derivation issue`() = runTest {
        val vm = viewModel()

        vm.pickFile(withingsUri())
        vm.awaitLoaded()
        vm.setColumnRole(2, role = CsvColumnRole.METRIC, metric = CsvImportMetric.BODY_FAT)
        vm.setColumnRole(1, role = CsvColumnRole.METRIC, metric = CsvImportMetric.WEIGHT)

        assertTrue(vm.uiState.value.canContinue)
    }

    @Test
    fun `only the mapped metrics permissions are reported missing`() = runTest {
        val vm = viewModel()

        vm.pickFile(withingsUri())
        vm.awaitLoaded()
        vm.setColumnRole(1, role = CsvColumnRole.METRIC, metric = CsvImportMetric.WEIGHT)

        assertEquals(setOf(WriteWeight), vm.uiState.value.missingPermissions)
    }

    @Test
    fun `a permission the installed provider does not define is never requested`() = runTest {
        // Requesting an unsupported permission throws, so missingPermissions must intersect with the supported set.
        val vm = viewModel(unsupported = setOf(WriteBodyWaterMass))

        vm.pickFile(withingsUri())
        vm.awaitLoaded()
        vm.setColumnRole(3, role = CsvColumnRole.METRIC, metric = CsvImportMetric.BODY_WATER_MASS)

        assertTrue(vm.uiState.value.missingPermissions.isEmpty())
    }

    @Test
    fun `a supported permission is still requested when another is unsupported`() = runTest {
        val vm = viewModel(unsupported = setOf(WriteBodyWaterMass))

        vm.pickFile(withingsUri())
        vm.awaitLoaded()
        vm.setColumnRole(1, role = CsvColumnRole.METRIC, metric = CsvImportMetric.WEIGHT)
        vm.setColumnRole(3, role = CsvColumnRole.METRIC, metric = CsvImportMetric.BODY_WATER_MASS)

        assertEquals(setOf(WriteWeight), vm.uiState.value.missingPermissions)
    }

    @Test
    fun `an already-granted permission is not reported missing`() = runTest {
        val vm = viewModel(granted = setOf(WriteWeight))
        // Resolve the granted set before the mapping asks for it.
        vm.uiState.first { WriteWeight in it.granted }

        vm.pickFile(withingsUri())
        vm.awaitLoaded()
        vm.setColumnRole(1, role = CsvColumnRole.METRIC, metric = CsvImportMetric.WEIGHT)

        assertTrue(vm.uiState.value.missingPermissions.isEmpty())
    }

    @Test
    fun `a food log maps its nutrients in the units its headers name and imports a record per row`() = runTest {
        val vm = viewModel()
        vm.pickFile(documentUri("food.csv", FoodLogCsv))
        vm.awaitLoaded()
        assertFalse(vm.uiState.value.canContinue)

        vm.setColumnRole(1, role = CsvColumnRole.FOOD_NAME)
        vm.setColumnNutrient(2, NutritionNutrient.ENERGY)
        vm.setColumnNutrient(3, NutritionNutrient.SODIUM)
        val energy = vm.uiState.value.mapping!!.columns.first { it.columnIndex == 2 }
        assertEquals(CsvDirectValue(CsvUnit.KILOJOULES), energy.effectiveInterpretation)
        assertTrue(vm.uiState.value.canContinue)
        assertEquals(setOf(WriteNutrition), vm.uiState.value.missingPermissions)

        vm.startImport()
        vm.awaitDone()

        val foods = inserted.filterIsInstance<NutritionRecord>()
        assertEquals(listOf("Oatmeal", "Soup"), foods.map { it.name })
        assertEquals(349.9, foods.first().energy!!.inKilocalories, 0.1)
        assertEquals(0.8, foods.last().sodium!!.inGrams, 1e-9)
    }

    @Test
    fun `a monitor log maps to readings that take the chosen default, which the next file starts with`() = runTest {
        val vm = viewModel()
        vm.pickFile(documentUri("monitor.csv", MonitorCsv))
        vm.awaitLoaded()

        vm.setColumnBloodPressurePart(1, CsvBloodPressurePart.SYSTOLIC)
        vm.setColumnBloodPressurePart(2, CsvBloodPressurePart.DIASTOLIC)
        vm.setColumnBloodPressurePart(3, CsvBloodPressurePart.BODY_POSITION)
        vm.setDefaultBodyPosition(BpRecordValues.BODY_POSITION_RECLINING)
        assertEquals(setOf(WriteBloodPressure), vm.uiState.value.missingPermissions)
        assertEquals(1, vm.uiState.value.mapping?.recordsPerRow)

        vm.startImport()
        vm.awaitDone()

        // "seated" is no word the app knows, so its reading takes the default rather than being lost.
        val readings = inserted.filterIsInstance<BloodPressureRecord>()
        assertEquals(
            listOf(BpRecordValues.BODY_POSITION_SITTING_DOWN, BpRecordValues.BODY_POSITION_RECLINING),
            readings.map { it.bodyPosition },
        )
        assertEquals(BpRecordValues.BODY_POSITION_RECLINING, preferences.defaultBodyPosition)

        vm.reset()
        vm.pickFile(documentUri("monitor.csv", MonitorCsv))
        assertEquals(
            BpRecordValues.BODY_POSITION_RECLINING,
            vm.awaitLoaded().mapping?.bloodPressure?.defaultBodyPosition,
        )
    }

    /** Picks the Withings file and maps its weight column, ready to import. */
    private suspend fun CsvImportViewModel.mapWeight(uri: Uri) {
        pickFile(uri)
        awaitLoaded()
        setColumnRole(1, role = CsvColumnRole.METRIC, metric = CsvImportMetric.WEIGHT)
        setDateTimeSettings(
            CsvDateTimeSettings(
                format = CsvDateTimeFormat.YEAR_FIRST,
                zone = CsvTimeZoneMode.UTC,
            ),
        )
    }

    @Test
    fun `a completed import writes one record per mapped metric per row`() = runTest {
        val vm = viewModel()
        vm.mapWeight(withingsUri())

        vm.startImport()

        val state = vm.awaitDone()
        assertEquals(CsvImportOutcome.COMPLETED, state.result?.outcome)
        assertEquals(3, state.result?.progress?.written)
        assertEquals(3, inserted.size)
    }

    @Test
    fun `the Withings fat-mass derivation writes body-fat percentages`() = runTest {
        val vm = viewModel()
        vm.mapWeight(withingsUri())
        vm.setColumnRole(2, role = CsvColumnRole.METRIC, metric = CsvImportMetric.BODY_FAT)

        vm.startImport()
        vm.awaitDone()

        val bodyFat = inserted.filterIsInstance<BodyFatRecord>()
        assertEquals(3, bodyFat.size)
        assertEquals(19.39, bodyFat.first().percentage.value, 0.01)
    }

    @Test
    fun `the report describes the run that just finished`() = runTest {
        val vm = viewModel()
        vm.mapWeight(withingsUri())
        vm.startImport()
        vm.awaitDone()

        val report = vm.reportText()

        assertNotNull(report)
        assertTrue(report!!.contains("File: withings.csv"))
        assertTrue(report.contains("Records written:     3, of which"))
        assertTrue(report.contains("[1] Weight (kg) -> weight (kilograms)"))
    }

    @Test
    fun `there is nothing to report before an import has run`() = runTest {
        val vm = viewModel()

        assertNull(vm.reportText())
    }

    @Test
    fun `an empty file lands on the mapping step with no mapping to edit`() = runTest {
        val vm = viewModel()

        vm.pickFile(documentUri("empty.csv", "Date,Weight\n"))

        val state = vm.awaitLoaded()
        assertEquals(CsvImportStep.MAPPING, state.step)
        assertEquals(true, state.sample?.isEmpty)
        assertNull(state.mapping)
    }

    @Test
    fun `resetting returns to the pick step and drops the previous run`() = runTest {
        val vm = viewModel()

        vm.pickFile(withingsUri())
        vm.awaitLoaded()
        vm.reset()

        val state = vm.uiState.value
        assertEquals(CsvImportStep.PICK, state.step)
        assertNull(state.sample)
        assertNull(state.result)
    }

    @Test
    fun `changing the separator re-reads the file under the new dialect`() = runTest {
        val vm = viewModel()
        val uri = documentUri("euro.csv", "Datum;Gewicht\n2026-07-01 08:12:00;78,4\n")

        vm.pickFile(uri)
        vm.awaitLoaded()
        // Sniffed correctly to begin with: two columns, semicolon-delimited.
        assertEquals(";", vm.uiState.value.sample?.dialect?.fieldDelimiter)
        assertEquals(listOf("Datum", "Gewicht"), vm.uiState.value.sample?.headerRow)

        val sniffed = vm.uiState.value.sample!!.dialect
        vm.setDialect(sniffed.copy(fieldDelimiter = ","))
        vm.uiState.first { !it.isLoadingFile && it.sample?.dialect?.fieldDelimiter == "," }

        // Forced onto a comma, the header is one cell still holding its semicolon: the file was re-read.
        assertEquals("Datum;Gewicht", vm.uiState.value.sample?.headerRow?.first())
    }
}
