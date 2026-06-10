package tech.mmarca.openvitals.features.imports.applehealth

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.just
import io.mockk.runs
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.data.repository.HealthRepository
import tech.mmarca.openvitals.data.repository.PreferencesRepository

class AppleHealthImportServiceTest {

    @Test
    fun `parser and converter import sleep category values as sleep stages`() {
        val parsed = parseXml(
            """
            <HealthData>
                <Record type="HKCategoryTypeIdentifierSleepAnalysis" sourceName="Apple Watch"
                    startDate="2026-01-01 22:00:00 +0000" endDate="2026-01-02 06:00:00 +0000"
                    value="HKCategoryValueSleepAnalysisAsleepCore" />
            </HealthData>
            """.trimIndent(),
        )

        val result = AppleHealthImportConverter(trackCycle = false, mindfulnessAvailable = true).convert(parsed)

        assertEquals(1, parsed.parsedRecords)
        assertEquals(1, result.converted.size)
        val sleep = result.converted.single().record as SleepSessionRecord
        assertEquals(SleepSessionRecord.STAGE_TYPE_LIGHT, sleep.stages.single().stage)
    }

    @Test
    fun `parser and converter prefer blood pressure correlations`() {
        val parsed = parseXml(
            """
            <HealthData>
                <Correlation type="HKCorrelationTypeIdentifierBloodPressure" sourceName="Test"
                    startDate="2026-01-01 08:00:00 +0000" endDate="2026-01-01 08:00:00 +0000">
                    <Record type="HKQuantityTypeIdentifierBloodPressureSystolic" sourceName="Test"
                        startDate="2026-01-01 08:00:00 +0000" endDate="2026-01-01 08:00:00 +0000"
                        unit="mmHg" value="120" />
                    <Record type="HKQuantityTypeIdentifierBloodPressureDiastolic" sourceName="Test"
                        startDate="2026-01-01 08:00:00 +0000" endDate="2026-01-01 08:00:00 +0000"
                        unit="mmHg" value="80" />
                </Correlation>
            </HealthData>
            """.trimIndent(),
        )

        val result = AppleHealthImportConverter(trackCycle = false, mindfulnessAvailable = true).convert(parsed)

        assertEquals(2, parsed.parsedRecords)
        assertEquals(1, parsed.parsedCorrelations)
        assertEquals(1, result.converted.size)
        val bloodPressure = result.converted.single().record as BloodPressureRecord
        assertEquals(120.0, bloodPressure.systolic.inMillimetersOfMercury, 0.0)
        assertEquals(80.0, bloodPressure.diastolic.inMillimetersOfMercury, 0.0)
    }

    @Test
    fun `synthetic export fixture covers supported converter targets`() {
        val parsed = parseFixture("synthetic_supported_export.xml")

        val result = AppleHealthImportConverter(trackCycle = true, mindfulnessAvailable = true).convert(parsed)
        val targetTypes = result.converted.mapTo(mutableSetOf()) { it.targetType }

        assertEquals(41, parsed.parsedRecords)
        assertEquals(1, parsed.parsedCorrelations)
        assertEquals(1, parsed.parsedWorkouts)
        assertEquals(1, parsed.parsedActivitySummaries)
        assertEquals(35, result.converted.size)
        assertTrue(
            targetTypes.containsAll(
                setOf(
                    "StepsRecord",
                    "DistanceRecord",
                    "ActiveCaloriesBurnedRecord",
                    "BasalMetabolicRateRecord",
                    "FloorsClimbedRecord",
                    "ElevationGainedRecord",
                    "WheelchairPushesRecord",
                    "HeartRateRecord",
                    "RestingHeartRateRecord",
                    "WeightRecord",
                    "HeightRecord",
                    "BodyFatRecord",
                    "LeanBodyMassRecord",
                    "BoneMassRecord",
                    "BodyWaterMassRecord",
                    "HydrationRecord",
                    "OxygenSaturationRecord",
                    "RespiratoryRateRecord",
                    "BodyTemperatureRecord",
                    "BloodGlucoseRecord",
                    "Vo2MaxRecord",
                    "BasalBodyTemperatureRecord",
                    "MindfulnessSessionRecord",
                    "MenstruationFlowRecord",
                    "OvulationTestRecord",
                    "CervicalMucusRecord",
                    "IntermenstrualBleedingRecord",
                    "SexualActivityRecord",
                    "BloodPressureRecord",
                    "SleepSessionRecord",
                    "NutritionRecord",
                    "ExerciseSessionRecord",
                ),
            ),
        )
        assertTrue(result.diagnostics.any { it.appleType == "ActivitySummary" && it.reasonCode == "unsupported" })
    }

    @Test
    fun `parser reads zipped apple export`() {
        val xml =
            """
            <HealthData>
                <Record type="HKQuantityTypeIdentifierStepCount" sourceName="Phone"
                    startDate="2026-01-01 08:00:00 +0000" endDate="2026-01-01 08:10:00 +0000"
                    unit="count" value="100" />
            </HealthData>
            """.trimIndent()
        val parsed = AppleHealthImportParser.parse(BufferedInputStream(ByteArrayInputStream(zipExport(xml))))

        assertEquals(1, parsed.parsedRecords)
        val result = AppleHealthImportConverter(trackCycle = false, mindfulnessAvailable = true).convert(parsed)
        assertEquals("StepsRecord", result.converted.single().targetType)
    }

    @Test
    fun `cycle records are skipped when cycle tracking is disabled`() {
        val parsed = parseFixture("synthetic_supported_export.xml")

        val result = AppleHealthImportConverter(trackCycle = false, mindfulnessAvailable = true).convert(parsed)

        assertTrue(result.converted.none { it.targetType == "MenstruationFlowRecord" })
        assertTrue(result.converted.none { it.targetType == "BasalBodyTemperatureRecord" })
        assertTrue(result.diagnostics.any { it.reasonCode == "cycle_disabled" })
    }

    @Test
    fun `service skips duplicate records inside same export and includes report`() = runTest {
        val xml =
            """
            <HealthData>
                <Record type="HKQuantityTypeIdentifierStepCount" sourceName="Phone"
                    startDate="2026-01-01 08:00:00 +0000" endDate="2026-01-01 08:10:00 +0000"
                    unit="count" value="100" />
                <Record type="HKQuantityTypeIdentifierStepCount" sourceName="Phone"
                    startDate="2026-01-01 08:00:00 +0000" endDate="2026-01-01 08:10:00 +0000"
                    unit="count" value="100" />
            </HealthData>
            """.trimIndent()
        val uri = mockk<Uri>()
        val resolver = mockk<ContentResolver>()
        val context = mockk<Context>()
        val repository = mockk<HealthRepository>()
        val preferences = mockk<PreferencesRepository>()
        val insertedRecords = slot<List<androidx.health.connect.client.records.Record>>()

        every { context.contentResolver } returns resolver
        every { resolver.openInputStream(uri) } returns ByteArrayInputStream(xml.toByteArray())
        every { preferences.trackCycle } returns false
        every { repository.isMindfulnessAvailable() } returns true
        coEvery { repository.readImportedClientRecordIds(any(), any(), any()) } returns emptySet()
        coEvery { repository.insertImportedRecords(capture(insertedRecords)) } just runs

        val result = AppleHealthImportService(context, repository, preferences).importAppleHealthExport(uri)

        assertEquals(2, result.parsedRecords)
        assertEquals(1, result.importedRecords)
        assertEquals(1, result.duplicateSkippedRecords)
        assertTrue(result.shareableReportText.contains("duplicate_in_file"))
        assertEquals(1, insertedRecords.captured.size)
        assertTrue(insertedRecords.captured.single() is StepsRecord)
        coVerify(exactly = 1) { repository.insertImportedRecords(any()) }
    }

    private fun parseXml(xml: String): AppleParsedExport =
        AppleHealthImportParser.parse(BufferedInputStream(ByteArrayInputStream(xml.toByteArray())))

    private fun parseFixture(name: String): AppleParsedExport {
        val input = requireNotNull(javaClass.getResourceAsStream("/apple_health/$name")) {
            "Missing fixture $name"
        }
        return input.use { AppleHealthImportParser.parse(BufferedInputStream(it)) }
    }

    private fun zipExport(xml: String): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry("apple_health_export/export.xml"))
            zip.write(xml.toByteArray())
            zip.closeEntry()
        }
        return output.toByteArray()
    }
}
