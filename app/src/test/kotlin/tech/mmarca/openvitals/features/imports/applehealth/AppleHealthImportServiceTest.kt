package tech.mmarca.openvitals.features.imports.applehealth

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseRouteResult
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsRecord
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.nio.file.Files
import java.time.Instant
import java.time.ZoneOffset
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.data.repository.AppleHealthImportRepository

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

        val result = AppleHealthImportConverter(mindfulnessAvailable = true).convert(parsed)

        assertEquals(1, parsed.parsedRecords)
        assertEquals(1, result.converted.size)
        val sleep = result.converted.single().record as SleepSessionRecord
        assertEquals(SleepSessionRecord.STAGE_TYPE_LIGHT, sleep.stages.single().stage)
    }

    @Test
    fun `parser handles apple export doctype without loading dtd grammar`() {
        val parsed = parseXml(
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE HealthData [
                <!ELEMENT HealthData (Record*)>
                <!ELEMENT Record EMPTY>
                <!ATTLIST Record type CDATA #IMPLIED>
                <!ATTLIST Record type CDATA #IMPLIED>
            ]>
            <HealthData>
                <Record type="HKQuantityTypeIdentifierStepCount" sourceName="Phone"
                    startDate="2026-01-01 08:00:00 +0000" endDate="2026-01-01 08:10:00 +0000"
                    unit="count" value="100" />
            </HealthData>
            """.trimIndent(),
        )

        assertEquals(1, parsed.parsedRecords)
    }

    @Test
    fun `parser repairs raw control characters and unescaped ampersands in attribute values`() {
        val xml = "<HealthData><Record type=\"HKQuantityTypeIdentifierStepCount\" " +
            "sourceName=\"NotesApp\" device=\"AT&T Watch\" " +
            "startDate=\"2026-01-01 00:00:00 +0000\" endDate=\"2026-01-01 00:01:00 +0000\" " +
            "unit=\"count\" value=\"10\" /></HealthData>"

        val parsed = parseXml(xml)

        assertEquals(1, parsed.parsedRecords)
        assertEquals(1, parsed.sanitizedControlChars)
        assertEquals(1, parsed.sanitizedAmpersands)
        val record = parsed.records.single()
        assertEquals("NotesApp", record.sourceName)
        assertEquals("AT&T Watch", record.device)
    }

    @Test
    fun `parser wraps a genuine well-formedness failure with the surrounding text`() {
        val xml = "<HealthData><Record type=\"HKQuantityTypeIdentifierStepCount\" " +
            "startDate=\"2026-01-01 00:00:00 +0000\" endDate=\"2026-01-01 00:01:00 +0000\">" +
            "</MismatchedClosingTag></HealthData>"

        val error = assertThrows(AppleHealthXmlParseException::class.java) { parseXml(xml) }

        assertTrue(error.message.orEmpty().contains("not well-formed") || error.cause is org.xml.sax.SAXParseException)
        assertTrue(error.message.orEmpty().contains("Text leading up to the error"))
    }

    @Test
    fun `parser preserves timezone offsets on apple date strings`() {
        val parsed = parseXml(
            """
            <HealthData>
                <Record type="HKQuantityTypeIdentifierStepCount" sourceName="Phone"
                    startDate="2023-12-13 20:48:49 +0100" endDate="2023-12-13 20:58:49 +0100"
                    unit="count" value="100" />
            </HealthData>
            """.trimIndent(),
        )

        val record = parsed.records.single()
        assertEquals(Instant.parse("2023-12-13T19:48:49Z"), record.startDate?.instant)
        assertEquals(ZoneOffset.ofHours(1), record.startDate?.offset)
        assertEquals(Instant.parse("2023-12-13T19:58:49Z"), record.endDate?.instant)
        assertEquals(ZoneOffset.ofHours(1), record.endDate?.offset)
    }

    @Test
    fun `parser and converter import walking speed as speed samples`() {
        val parsed = parseXml(
            """
            <HealthData>
                <Record type="HKQuantityTypeIdentifierWalkingSpeed" sourceName="Phone"
                    startDate="2026-01-01 08:00:00 +0000" endDate="2026-01-01 08:00:05 +0000"
                    unit="km/hr" value="3.6" />
            </HealthData>
            """.trimIndent(),
        )

        val result = AppleHealthImportConverter(mindfulnessAvailable = true).convert(parsed)

        assertEquals(1, parsed.parsedRecords)
        assertEquals(1, result.converted.size)
        val speed = result.converted.single().record as SpeedRecord
        assertEquals(1.0, speed.samples.single().speed.inMetersPerSecond, 0.0)
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

        val result = AppleHealthImportConverter(mindfulnessAvailable = true).convert(parsed)

        assertEquals(2, parsed.parsedRecords)
        assertEquals(1, parsed.parsedCorrelations)
        assertEquals(1, result.converted.size)
        val bloodPressure = result.converted.single().record as BloodPressureRecord
        assertEquals(120.0, bloodPressure.systolic.inMillimetersOfMercury, 0.0)
        assertEquals(80.0, bloodPressure.diastolic.inMillimetersOfMercury, 0.0)
    }

    @Test
    fun `parser and converter read workout statistics as workout distance and energy`() {
        val parsed = parseXml(
            """
            <HealthData>
                <Workout workoutActivityType="HKWorkoutActivityTypeCycling" sourceName="Apple Watch"
                    startDate="2026-01-01 08:00:00 +0000" endDate="2026-01-01 08:45:00 +0000"
                    duration="45" durationUnit="min">
                    <WorkoutStatistics type="HKQuantityTypeIdentifierActiveEnergyBurned"
                        startDate="2026-01-01 08:00:00 +0000" endDate="2026-01-01 08:45:00 +0000"
                        sum="123.4" unit="kcal" />
                    <WorkoutStatistics type="HKQuantityTypeIdentifierDistanceCycling"
                        startDate="2026-01-01 08:00:00 +0000" endDate="2026-01-01 08:45:00 +0000"
                        sum="6.5" unit="km" />
                </Workout>
            </HealthData>
            """.trimIndent(),
        )

        val workout = parsed.workouts.single()
        assertEquals(6.5, workout.totalDistance ?: 0.0, 0.0)
        assertEquals("km", workout.totalDistanceUnit)
        assertEquals(123.4, workout.totalEnergyBurned ?: 0.0, 0.0)
        assertEquals("kcal", workout.totalEnergyBurnedUnit)

        val result = AppleHealthImportConverter(mindfulnessAvailable = true).convert(parsed)
        val convertedByTarget = result.converted.associateBy { it.targetType }

        assertEquals(1, parsed.parsedWorkouts)
        assertEquals(3, result.converted.size)
        assertTrue(convertedByTarget.containsKey("ExerciseSessionRecord"))
        val distance = convertedByTarget.getValue("DistanceRecord").record as DistanceRecord
        val energy = convertedByTarget.getValue("ActiveCaloriesBurnedRecord").record as ActiveCaloriesBurnedRecord
        assertEquals(6500.0, distance.distance.inMeters, 0.0)
        assertEquals(123.4, energy.energy.inKilocalories, 0.0)
        assertEquals(3, result.typeStats.getValue("HKWorkoutActivityTypeCycling").converted)
    }

    @Test
    fun `converter does not import workout energy totals when overlapping records exist from another source`() {
        val parsed = parseXml(
            """
            <HealthData>
                <Record type="HKQuantityTypeIdentifierActiveEnergyBurned" sourceName="iPhone"
                    startDate="2026-01-01 08:00:00 +0000" endDate="2026-01-01 08:45:00 +0000"
                    unit="kcal" value="123" />
                <Workout workoutActivityType="HKWorkoutActivityTypeCycling" sourceName="Apple Watch"
                    startDate="2026-01-01 08:00:00 +0000" endDate="2026-01-01 08:45:00 +0000"
                    duration="45" durationUnit="min">
                    <WorkoutStatistics type="HKQuantityTypeIdentifierActiveEnergyBurned"
                        startDate="2026-01-01 08:00:00 +0000" endDate="2026-01-01 08:45:00 +0000"
                        sum="123" unit="kcal" />
                </Workout>
            </HealthData>
            """.trimIndent(),
        )

        val result = AppleHealthImportConverter(mindfulnessAvailable = true).convert(parsed)

        assertEquals(
            listOf("ExerciseSessionRecord", "ActiveCaloriesBurnedRecord"),
            result.converted.map { it.targetType },
        )
    }

    @Test
    fun `converter skips lower priority additive records mostly covered by another source`() {
        val parsed = parseXml(
            """
            <HealthData>
                <Record type="HKQuantityTypeIdentifierStepCount" sourceName="Alesia's iPhone"
                    startDate="2026-01-01 08:00:00 +0000" endDate="2026-01-01 08:10:00 +0000"
                    unit="count" value="100" />
                <Record type="HKQuantityTypeIdentifierStepCount" sourceName="Health CoPilot"
                    startDate="2026-01-01 08:01:00 +0000" endDate="2026-01-01 08:09:00 +0000"
                    unit="count" value="95" />
            </HealthData>
            """.trimIndent(),
        )

        val result = AppleHealthImportConverter(mindfulnessAvailable = true).convert(parsed)

        assertEquals(1, result.converted.size)
        assertEquals("StepsRecord", result.converted.single().targetType)
        assertEquals(1, result.typeStats.getValue("HKQuantityTypeIdentifierStepCount").converted)
        assertEquals(1, result.typeStats.getValue("HKQuantityTypeIdentifierStepCount").skipped)
        assertEquals("overlap_cross_source", result.diagnostics.single().reasonCode)
    }

    @Test
    fun `synthetic export fixture covers supported converter targets`() {
        val parsed = parseFixture("synthetic_supported_export.xml")

        val result = AppleHealthImportConverter(mindfulnessAvailable = true).convert(parsed)
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
        val result = AppleHealthImportConverter(mindfulnessAvailable = true).convert(parsed)
        assertEquals("StepsRecord", result.converted.single().targetType)
    }

    @Test
    fun `parser reports truncated zip exports with actionable apple health context`() {
        val xml = buildString {
            appendLine("<HealthData>")
            repeat(5_000) { index ->
                val minute = (index % 60).toString().padStart(2, '0')
                val second = (index % 60).toString().padStart(2, '0')
                appendLine(
                    """<Record type="HKQuantityTypeIdentifierStepCount" sourceName="Phone $index" """ +
                        """startDate="2026-01-01 08:$minute:$second +0000" """ +
                        """endDate="2026-01-01 08:$minute:$second +0000" unit="count" value="$index" />""",
                )
            }
            appendLine("</HealthData>")
        }
        val zip = zipExport(xml)
        val failingZipStream = FailingAfterBytesInputStream(zip, maxBytesBeforeFailure = zip.size / 2)

        val error = assertThrows(AppleHealthZipReadException::class.java) {
            AppleHealthImportParser.parse(
                BufferedInputStream(failingZipStream),
                options = AppleHealthParseOptions(parseRouteFiles = false),
            )
        }

        assertTrue(error.message.orEmpty().contains("apple_health_export/export.xml"))
        assertTrue(error.message.orEmpty().contains("incomplete, corrupt, not fully downloaded"))
        assertTrue(error.message.orEmpty().contains("extract export.xml and import that file directly"))
    }

    @Test
    fun `service imports intact health records when zip ends during workout route`() = runTest {
        val xml =
            """
            <HealthData>
                <Record type="HKQuantityTypeIdentifierStepCount" sourceName="Phone"
                    startDate="2026-01-01 08:00:00 +0000" endDate="2026-01-01 08:10:00 +0000"
                    unit="count" value="100" />
                <Workout workoutActivityType="HKWorkoutActivityTypeRunning" sourceName="Apple Watch"
                    startDate="2022-06-09 16:13:00 +0000" endDate="2022-06-09 16:43:00 +0000"
                    duration="30" durationUnit="min">
                    <WorkoutRoute sourceName="Apple Watch"
                        startDate="2022-06-09 16:13:00 +0000" endDate="2022-06-09 16:43:00 +0000">
                        <FileReference path="/workout-routes/route_2022-06-09_4.13pm.gpx" />
                    </WorkoutRoute>
                </Workout>
            </HealthData>
            """.trimIndent()
        val routePath = "apple_health_export/workout-routes/route_2022-06-09_4.13pm.gpx"
        val gpx = buildString {
            appendLine("<gpx><trk><trkseg>")
            repeat(2_000) { index ->
                appendLine("<trkpt lat=\"59.${index.toString().padStart(6, '0')}\" lon=\"24.000000\"><ele>$index</ele></trkpt>")
            }
            appendLine("</trkseg></trk></gpx>")
        }
        val truncatedZip = zipExport(xml, mapOf(routePath to gpx)).truncateInsideEntry(routePath)
        val uri = mockk<Uri>()
        val resolver = mockk<ContentResolver>()
        val context = mockk<Context>()
        val repository = mockk<AppleHealthImportRepository>()
        val insertedRecords = slot<List<androidx.health.connect.client.records.Record>>()

        every { context.contentResolver } returns resolver
        every { resolver.openInputStream(uri) } returns ByteArrayInputStream(truncatedZip)
        every { repository.isMindfulnessAvailable() } returns true
        coEvery { repository.findMatchingImportedClientRecordIds(any(), any(), any(), any()) } returns emptySet()
        coEvery { repository.insertImportedRecords(capture(insertedRecords)) } just runs

        val result = AppleHealthImportService(context, repository).importAppleHealthExport(uri)

        assertEquals(1, result.parsedRecords)
        assertEquals(1, result.parsedWorkouts)
        assertEquals(2, result.importedRecords)
        assertTrue(result.workoutRoutesIncomplete)
        assertTrue(insertedRecords.captured.any { it is StepsRecord })
        assertTrue(insertedRecords.captured.any { it is ExerciseSessionRecord })
        assertTrue(result.diagnostics.any { it.reasonCode == "route_archive_truncated" })
        val affectedWorkout = result.diagnostics.single { it.reasonCode == "workout_route_unavailable" }
        assertEquals("HKWorkoutActivityTypeRunning", affectedWorkout.appleType)
        assertTrue(affectedWorkout.timeRange.orEmpty().contains("2022-06-09T16:13:00Z"))
        assertTrue(affectedWorkout.detail.contains("route_2022-06-09_4.13pm.gpx"))
        assertTrue(affectedWorkout.detail.contains("Import or recreate this activity manually"))
        assertTrue(result.shareableReportText.contains("Workout routes incomplete: true"))
        assertTrue(result.shareableReportText.contains("[WARN] Workout route archive recovery"))
        assertTrue(result.shareableReportText.contains(routePath))
        assertTrue(result.shareableReportText.contains("workout_route_unavailable"))
        assertTrue(result.shareableReportText.contains("HKWorkoutActivityTypeRunning"))
        assertTrue(result.shareableReportText.contains("Activities Requiring Manual Route Import"))
        assertTrue(result.shareableReportText.contains("timeRange=2022-06-09T16:13:00Z"))
        coVerify(exactly = 1) { repository.insertImportedRecords(any()) }
    }

    @Test
    fun `parser does not recover a truncated route before export xml`() {
        val routePath = "apple_health_export/workout-routes/route_before_export.gpx"
        val zip = zipExport(
            xml = "<HealthData />",
            extraFiles = mapOf(routePath to "<gpx><trk><trkseg>${"x".repeat(4_000)}</trkseg></trk></gpx>"),
            extraFilesBeforeXml = true,
        ).truncateInsideEntry(routePath)

        assertThrows(Exception::class.java) {
            AppleHealthImportParser.parse(BufferedInputStream(ByteArrayInputStream(zip)))
        }
    }

    @Test
    fun `parser and converter import apple workout route with synthesized times`() {
        val xml =
            """
            <HealthData>
                <Workout workoutActivityType="HKWorkoutActivityTypeRunning" sourceName="Apple Watch"
                    startDate="2026-01-01 08:00:00 +0000" endDate="2026-01-01 08:30:00 +0000"
                    duration="30" durationUnit="min">
                    <WorkoutRoute sourceName="Apple Watch"
                        startDate="2026-01-01 09:00:00 +0000" endDate="2026-01-01 09:00:00 +0000">
                        <FileReference path="/workout-routes/route_2026-01-01_8.00am.gpx" />
                    </WorkoutRoute>
                </Workout>
            </HealthData>
            """.trimIndent()
        val gpx =
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <gpx version="1.1" creator="Apple Health Export">
                <trk>
                    <trkseg>
                        <trkpt lat="59.000000" lon="24.000000">
                            <ele>0</ele><time>2026-07-05T08:34:11Z</time>
                        </trkpt>
                        <trkpt lat="59.000000" lon="24.010000">
                            <ele>0</ele><time>2026-07-05T08:34:11Z</time>
                        </trkpt>
                        <trkpt lat="59.010000" lon="24.010000">
                            <ele>0</ele><time>2026-07-05T08:34:11Z</time>
                        </trkpt>
                    </trkseg>
                </trk>
            </gpx>
            """.trimIndent()

        val parsed = AppleHealthImportParser.parse(
            BufferedInputStream(
                ByteArrayInputStream(
                    zipExport(
                        xml,
                        mapOf("apple_health_export/workout-routes/route_2026-01-01_8.00am.gpx" to gpx),
                    ),
                ),
            ),
        )
        val result = AppleHealthImportConverter(mindfulnessAvailable = true).convert(parsed)

        assertEquals(1, parsed.parsedWorkouts)
        assertEquals(1, parsed.workouts.single().routes.size)
        assertEquals(1, parsed.workouts.single().routeReferences)
        val session = result.converted.single().record as ExerciseSessionRecord
        val route = (session.exerciseRouteResult as ExerciseRouteResult.Data).exerciseRoute
        val locations = route.route
        assertEquals(3, locations.size)
        assertEquals(Instant.parse("2026-01-01T08:00:00Z"), locations.first().time)
        assertTrue(locations[0].time.isBefore(locations[1].time))
        assertTrue(locations[1].time.isBefore(locations[2].time))
        assertTrue(locations.last().time.isBefore(Instant.parse("2026-01-01T08:30:00Z")))
        assertEquals(59.0, locations.first().latitude, 0.0)
        assertEquals(24.01, locations[1].longitude, 0.0)
        assertEquals(null, locations.first().altitude)
    }

    @Test
    fun `synthesized route times stay strictly increasing at millisecond precision`() {
        // Paused GPS produces runs of identical coordinates (zero distance progress). Offsets must
        // still advance by >= 1ms per point: Health Connect stores route times at millisecond
        // precision, and ExerciseRoute requires strictly increasing times when the session is read
        // back. Sub-millisecond spacing breaks duplicate detection and session reads.
        val pausedPoints = (0 until 50).joinToString("\n") {
            """<trkpt lat="59.000000" lon="24.000000"><ele>0</ele><time>2026-07-05T08:34:11Z</time></trkpt>"""
        }
        val xml =
            """
            <HealthData>
                <Workout workoutActivityType="HKWorkoutActivityTypeRunning" sourceName="Apple Watch"
                    startDate="2026-01-01 08:00:00 +0000" endDate="2026-01-01 08:30:00 +0000"
                    duration="30" durationUnit="min">
                    <WorkoutRoute sourceName="Apple Watch"
                        startDate="2026-01-01 08:00:00 +0000" endDate="2026-01-01 08:30:00 +0000">
                        <FileReference path="/workout-routes/route_2026-01-01_8.00am.gpx" />
                    </WorkoutRoute>
                </Workout>
            </HealthData>
            """.trimIndent()
        val gpx =
            """<gpx version="1.1" creator="Apple Health Export"><trk><trkseg>
            $pausedPoints
            <trkpt lat="59.010000" lon="24.010000"><ele>0</ele><time>2026-07-05T08:35:11Z</time></trkpt>
            </trkseg></trk></gpx>"""

        val parsed = AppleHealthImportParser.parse(
            BufferedInputStream(
                ByteArrayInputStream(
                    zipExport(
                        xml,
                        mapOf("apple_health_export/workout-routes/route_2026-01-01_8.00am.gpx" to gpx),
                    ),
                ),
            ),
        )
        val result = AppleHealthImportConverter(mindfulnessAvailable = true).convert(parsed)

        val session = result.converted.single().record as ExerciseSessionRecord
        val locations = (session.exerciseRouteResult as ExerciseRouteResult.Data).exerciseRoute.route
        assertEquals(51, locations.size)
        locations.zipWithNext().forEach { (previous, next) ->
            assertTrue(
                "route times must differ by >= 1ms: ${previous.time} -> ${next.time}",
                previous.time.toEpochMilli() < next.time.toEpochMilli(),
            )
        }
        assertTrue(locations.last().time.isBefore(Instant.parse("2026-01-01T08:30:00Z")))
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
        val repository = mockk<AppleHealthImportRepository>()
        val insertedRecords = slot<List<androidx.health.connect.client.records.Record>>()

        every { context.contentResolver } returns resolver
        every { resolver.openInputStream(uri) } returns ByteArrayInputStream(xml.toByteArray())
        every { repository.isMindfulnessAvailable() } returns true
        coEvery { repository.findMatchingImportedClientRecordIds(any(), any(), any(), any()) } returns emptySet()
        coEvery { repository.insertImportedRecords(capture(insertedRecords)) } just runs

        val phases = mutableListOf<AppleHealthImportPhase>()
        val result = AppleHealthImportService(context, repository).importAppleHealthExport(
            uri,
            progress = { progress -> phases += progress.phase },
        )

        assertEquals(2, result.parsedRecords)
        assertEquals(1, result.importedRecords)
        assertEquals(1, result.duplicateSkippedRecords)
        assertTrue(phases.contains(AppleHealthImportPhase.PARSING))
        assertTrue(phases.contains(AppleHealthImportPhase.CONVERTING))
        assertTrue(phases.contains(AppleHealthImportPhase.CHECKING_DUPLICATES))
        assertTrue(phases.contains(AppleHealthImportPhase.WRITING))
        assertTrue(phases.contains(AppleHealthImportPhase.BUILDING_REPORT))
        assertTrue(result.shareableReportText.contains("Stage started: Scanning export"))
        assertTrue(result.shareableReportText.contains("Stage finished: Scanning export"))
        assertTrue(result.shareableReportText.contains("Stage started: Checking duplicates"))
        assertTrue(result.shareableReportText.contains("Stage finished: Checking duplicates"))
        assertTrue(result.shareableReportText.contains("Stage started: Writing records"))
        assertTrue(result.shareableReportText.contains("Stage finished: Writing records"))
        assertTrue(result.shareableReportText.contains("Stage started: Building report"))
        assertTrue(result.shareableReportText.contains("Stage finished: Building report"))
        assertTrue(result.shareableReportText.contains("duplicate_in_file"))
        assertEquals(1, insertedRecords.captured.size)
        assertTrue(insertedRecords.captured.single() is StepsRecord)
        coVerify(exactly = 1) { repository.insertImportedRecords(any()) }
    }

    @Test
    fun `service analysis detects import categories without writing`() = runTest {
        val xml =
            """
            <HealthData>
                <Record type="HKQuantityTypeIdentifierStepCount" sourceName="Phone"
                    startDate="2026-01-01 08:00:00 +0000" endDate="2026-01-01 08:10:00 +0000"
                    unit="count" value="100" />
                <Record type="HKQuantityTypeIdentifierBodyMass" sourceName="Scale"
                    startDate="2026-01-01 08:00:00 +0000" endDate="2026-01-01 08:00:00 +0000"
                    unit="kg" value="70" />
            </HealthData>
            """.trimIndent()
        val uri = mockk<Uri>()
        val resolver = mockk<ContentResolver>()
        val context = mockk<Context>()
        val repository = mockk<AppleHealthImportRepository>()

        every { context.contentResolver } returns resolver
        every { resolver.openInputStream(uri) } returns ByteArrayInputStream(xml.toByteArray())
        every { repository.isMindfulnessAvailable() } returns true

        val result = AppleHealthImportService(context, repository).analyzeAppleHealthExport(uri)

        assertEquals(2, result.parsedRecords)
        assertEquals(2, result.convertedRecords)
        assertEquals(
            setOf(AppleHealthImportCategory.ACTIVITY, AppleHealthImportCategory.BODY),
            result.categorySummaries.mapTo(mutableSetOf()) { it.category },
        )
        coVerify(exactly = 0) { repository.insertImportedRecords(any()) }
    }

    @Test
    fun `service analysis detects route categories without parsing gpx geometry`() = runTest {
        val xml =
            """
            <HealthData>
                <Workout workoutActivityType="HKWorkoutActivityTypeRunning" sourceName="Apple Watch"
                    startDate="2026-01-01 08:00:00 +0000" endDate="2026-01-01 08:30:00 +0000"
                    duration="30" durationUnit="min">
                    <WorkoutRoute sourceName="Apple Watch"
                        startDate="2026-01-01 08:00:00 +0000" endDate="2026-01-01 08:30:00 +0000">
                        <FileReference path="/workout-routes/route_2026-01-01_8.00am.gpx" />
                    </WorkoutRoute>
                </Workout>
            </HealthData>
            """.trimIndent()
        val uri = mockk<Uri>()
        val resolver = mockk<ContentResolver>()
        val context = mockk<Context>()
        val repository = mockk<AppleHealthImportRepository>()

        every { context.contentResolver } returns resolver
        every { resolver.openInputStream(uri) } returns ByteArrayInputStream(
            zipExport(
                xml,
                mapOf("apple_health_export/workout-routes/route_2026-01-01_8.00am.gpx" to "<not-gpx>"),
            ),
        )
        every { repository.isMindfulnessAvailable() } returns true

        val result = AppleHealthImportService(context, repository).analyzeAppleHealthExport(uri)

        val workoutSummary = result.categorySummaries.single { it.category == AppleHealthImportCategory.WORKOUTS }
        assertEquals(1, result.parsedWorkouts)
        assertEquals(1, workoutSummary.convertedRecords)
        assertEquals(1, workoutSummary.routeSessions)
        assertTrue(result.shareableReportText.contains("parseRouteFiles=false"))
        coVerify(exactly = 0) { repository.insertImportedRecords(any()) }
    }

    @Test
    fun `service imports only selected categories after analysis`() = runTest {
        val xml =
            """
            <HealthData>
                <Record type="HKQuantityTypeIdentifierStepCount" sourceName="Phone"
                    startDate="2026-01-01 08:00:00 +0000" endDate="2026-01-01 08:10:00 +0000"
                    unit="count" value="100" />
                <Record type="HKQuantityTypeIdentifierBodyMass" sourceName="Scale"
                    startDate="2026-01-01 08:00:00 +0000" endDate="2026-01-01 08:00:00 +0000"
                    unit="kg" value="70" />
            </HealthData>
            """.trimIndent()
        val uri = mockk<Uri>()
        val resolver = mockk<ContentResolver>()
        val context = mockk<Context>()
        val repository = mockk<AppleHealthImportRepository>()
        val insertedRecords = slot<List<androidx.health.connect.client.records.Record>>()

        every { context.contentResolver } returns resolver
        every { resolver.openInputStream(uri) } returns ByteArrayInputStream(xml.toByteArray())
        every { repository.isMindfulnessAvailable() } returns true
        coEvery { repository.findMatchingImportedClientRecordIds(any(), any(), any(), any()) } returns emptySet()
        coEvery { repository.insertImportedRecords(capture(insertedRecords)) } just runs

        val result = AppleHealthImportService(context, repository).importAppleHealthExport(
            uri,
            selectedCategories = setOf(AppleHealthImportCategory.BODY),
        )

        assertEquals(2, result.convertedRecords)
        assertEquals(1, result.importedRecords)
        assertEquals(1, result.notSelectedRecords)
        assertTrue(insertedRecords.captured.single() is androidx.health.connect.client.records.WeightRecord)
        assertTrue(result.shareableReportText.contains("Not selected: 1"))
        coVerify(exactly = 1) { repository.insertImportedRecords(any()) }
    }

    @Test
    fun `service report aggregates repeated diagnostics and keeps later distinct groups`() = runTest {
        val xml = buildString {
            appendLine("<HealthData>")
            repeat(205) { index ->
                val day = (1 + index / 60).toString().padStart(2, '0')
                val minute = (index % 60).toString().padStart(2, '0')
                appendLine(
                    """<Record type="HKQuantityTypeIdentifierUnsupportedA" sourceName="Phone" """ +
                        """startDate="2026-01-$day 08:$minute:00 +0000" """ +
                        """endDate="2026-01-$day 08:$minute:01 +0000" unit="count" value="1" />""",
                )
            }
            appendLine(
                """<Record type="HKQuantityTypeIdentifierUnsupportedB" sourceName="Phone" """ +
                    """startDate="2026-01-01 09:00:00 +0000" endDate="2026-01-01 09:00:01 +0000" """ +
                    """unit="count" value="1" />""",
            )
            appendLine("</HealthData>")
        }
        val uri = mockk<Uri>()
        val resolver = mockk<ContentResolver>()
        val context = mockk<Context>()
        val repository = mockk<AppleHealthImportRepository>()

        every { context.contentResolver } returns resolver
        every { resolver.openInputStream(uri) } returns ByteArrayInputStream(xml.toByteArray())
        every { repository.isMindfulnessAvailable() } returns true

        val result = AppleHealthImportService(context, repository).importAppleHealthExport(uri)

        assertEquals(206, result.unsupportedElements)
        assertTrue(result.shareableReportText.contains("Logs"))
        assertTrue(result.shareableReportText.contains("Raw Diagnostic Log"))
        assertTrue(result.shareableReportText.contains("Grouped diagnostic types: 2; unsupported=206"))
        assertTrue(result.shareableReportText.contains("count=205; reason=unsupported; appleType=HKQuantityTypeIdentifierUnsupportedA"))
        assertTrue(result.shareableReportText.contains("count=1; reason=unsupported; appleType=HKQuantityTypeIdentifierUnsupportedB"))
        assertEquals(
            205,
            Regex("""(?m)^\d+\. reason=unsupported; appleType=HKQuantityTypeIdentifierUnsupportedA""")
                .findAll(result.shareableReportText)
                .count(),
        )
        assertFalse(result.shareableReportText.contains("Diagnostics were truncated at 200 entries."))
    }

    @Test
    fun `worker failure report includes summary logs and full exception stack`() {
        val context = mockk<Context>()
        val filesDir = Files.createTempDirectory("apple-health-report").toFile()
        every { context.filesDir } returns filesDir
        val error = IllegalStateException("Top level failure", IllegalArgumentException("Root cause"))

        val data = AppleHealthImportWorker.errorData(
            context,
            error,
            workerLogs = listOf("2026-01-01T08:00:00Z [WORKER] test log"),
        )
        val report = AppleHealthImportReportStore.read(AppleHealthImportWorker.errorReportPathFromData(data))

        assertTrue(report.contains("Summary"))
        assertTrue(report.contains("Logs"))
        assertTrue(report.contains("2026-01-01T08:00:00Z [WORKER] test log"))
        assertTrue(report.contains("Exception"))
        assertTrue(report.contains("java.lang.IllegalStateException: Top level failure"))
        assertTrue(report.contains("Caused by: java.lang.IllegalArgumentException: Root cause"))
    }

    @Test
    fun `staging store reuses matching local export copy`() {
        val xml =
            """
            <HealthData>
                <Record type="HKQuantityTypeIdentifierStepCount" sourceName="Phone"
                    startDate="2026-01-01 08:00:00 +0000" endDate="2026-01-01 08:10:00 +0000"
                    unit="count" value="100" />
            </HealthData>
            """.trimIndent()
        val zip = zipExport(xml)
        val uri = mockk<Uri>()
        val context = mockk<Context>()
        val resolver = mockk<ContentResolver>()
        val filesDir = Files.createTempDirectory("apple-health-stage").toFile()
        val fingerprint = AppleHealthExportFingerprint(displayName = "export.zip", size = zip.size.toLong())

        every { uri.toString() } returns "content://example/apple-health-export.zip"
        every { context.filesDir } returns filesDir
        every { context.contentResolver } returns resolver
        every { resolver.openInputStream(uri) } returns ByteArrayInputStream(zip)

        val first = AppleHealthImportStagingStore.stage(context, uri, fingerprint)
        val second = AppleHealthImportStagingStore.stage(context, uri, fingerprint)

        assertFalse(first.reused)
        assertTrue(second.reused)
        assertEquals(zip.size.toLong(), second.bytes)
        assertEquals(first.file, second.file)
        verify(exactly = 1) { resolver.openInputStream(uri) }
    }

    @Test
    fun `service pipelines multiple batches in order and imports all records`() = runTest {
        val xml = heartRateExport(count = 700)
        val uri = mockk<Uri>()
        val resolver = mockk<ContentResolver>()
        val context = mockk<Context>()
        val repository = mockk<AppleHealthImportRepository>()
        val insertedBatches = mutableListOf<List<androidx.health.connect.client.records.Record>>()

        every { context.contentResolver } returns resolver
        every { resolver.openInputStream(uri) } returns ByteArrayInputStream(xml.toByteArray())
        every { repository.isMindfulnessAvailable() } returns true
        coEvery { repository.findMatchingImportedClientRecordIds(any(), any(), any(), any()) } returns emptySet()
        coEvery { repository.insertImportedRecords(any()) } coAnswers {
            insertedBatches += firstArg<List<androidx.health.connect.client.records.Record>>()
        }

        val progressSnapshots = mutableListOf<AppleHealthImportProgress>()
        val result = AppleHealthImportService(context, repository).importAppleHealthExport(
            uri,
            progress = { progress -> progressSnapshots += progress },
        )

        assertEquals(700, result.parsedRecords)
        assertEquals(700, result.importedRecords)
        assertEquals(0, result.duplicateSkippedRecords)
        assertEquals(listOf(300, 300, 100), insertedBatches.map { it.size })
        val batchStartTimes = insertedBatches.map { batch ->
            (batch.first() as androidx.health.connect.client.records.HeartRateRecord).startTime
        }
        assertTrue(batchStartTimes[0].isBefore(batchStartTimes[1]))
        assertTrue(batchStartTimes[1].isBefore(batchStartTimes[2]))
        assertTrue(result.shareableReportText.contains("Imported Health Connect records: 700"))

        val phases = progressSnapshots.map { it.phase }
        assertTrue(phases.contains(AppleHealthImportPhase.PARSING))
        assertTrue(phases.contains(AppleHealthImportPhase.CONVERTING))
        assertTrue(phases.contains(AppleHealthImportPhase.CHECKING_DUPLICATES))
        assertTrue(phases.contains(AppleHealthImportPhase.WRITING))
        assertTrue(phases.contains(AppleHealthImportPhase.BUILDING_REPORT))
        val importedCounts = progressSnapshots.map { it.importedRecords }
        assertEquals(importedCounts, importedCounts.sorted())
        assertEquals(700, importedCounts.last())
    }

    @Test
    fun `service resumes from selected record checkpoint and writes remaining batches`() = runTest {
        val xml = heartRateExport(count = 700)
        val uri = mockk<Uri>()
        val resolver = mockk<ContentResolver>()
        val context = mockk<Context>()
        val repository = mockk<AppleHealthImportRepository>()
        val insertedBatches = mutableListOf<List<androidx.health.connect.client.records.Record>>()
        val checkpoints = mutableListOf<AppleHealthImportCheckpoint>()
        val selectedCategories = setOf(AppleHealthImportCategory.HEART)
        val checkpoint = AppleHealthImportCheckpoint(
            sourceKey = "same-export",
            selectedCategories = selectedCategories,
            committedSelectedRecords = 300,
            importedRecords = 300,
            duplicateSkippedRecords = 0,
            failedRecords = 0,
            typeStats = mapOf(
                "HKQuantityTypeIdentifierHeartRate" to AppleHealthImportCheckpointTypeStats(
                    imported = 300,
                    duplicateSkipped = 0,
                    failed = 0,
                ),
            ),
        )

        every { context.contentResolver } returns resolver
        every { resolver.openInputStream(uri) } returns ByteArrayInputStream(xml.toByteArray())
        every { repository.isMindfulnessAvailable() } returns true
        coEvery { repository.findMatchingImportedClientRecordIds(any(), any(), any(), any()) } returns emptySet()
        coEvery { repository.insertImportedRecords(any()) } coAnswers {
            insertedBatches += firstArg<List<androidx.health.connect.client.records.Record>>()
        }

        val result = AppleHealthImportService(context, repository).importAppleHealthExport(
            uri = uri,
            selectedCategories = selectedCategories,
            resumeCheckpoint = checkpoint,
            onCheckpoint = { checkpoints += it },
        )

        assertEquals(700, result.parsedRecords)
        assertEquals(700, result.importedRecords)
        assertEquals(listOf(300, 100), insertedBatches.map { it.size })
        assertEquals(listOf(600, 700), checkpoints.map { it.committedSelectedRecords })
        assertEquals(700, checkpoints.last().importedRecords)
        assertEquals(700, result.typeSummaries.single { it.appleType == "HKQuantityTypeIdentifierHeartRate" }.imported)
        assertTrue(result.shareableReportText.contains("Resuming Apple Health import checkpoint"))
    }

    @Test
    fun `service skips duplicates that appear in a later batch of the same export`() = runTest {
        // 400 records: record 350 duplicates record 1, so they land in different 300-record batches.
        val xml = heartRateExport(count = 400, duplicateIndexOf = 350 to 0)
        val uri = mockk<Uri>()
        val resolver = mockk<ContentResolver>()
        val context = mockk<Context>()
        val repository = mockk<AppleHealthImportRepository>()
        val insertedClientRecordIds = mutableSetOf<String>()

        every { context.contentResolver } returns resolver
        every { resolver.openInputStream(uri) } returns ByteArrayInputStream(xml.toByteArray())
        every { repository.isMindfulnessAvailable() } returns true
        coEvery { repository.findMatchingImportedClientRecordIds(any(), any(), any(), any()) } coAnswers {
            arg<Set<String>>(3).intersect(insertedClientRecordIds)
        }
        coEvery { repository.insertImportedRecords(any()) } coAnswers {
            firstArg<List<androidx.health.connect.client.records.Record>>().forEach { record ->
                record.metadata.clientRecordId?.let { insertedClientRecordIds += it }
            }
        }

        val result = AppleHealthImportService(context, repository).importAppleHealthExport(uri)

        assertEquals(400, result.parsedRecords)
        assertEquals(399, result.importedRecords)
        assertEquals(1, result.duplicateSkippedRecords)
        assertTrue(result.shareableReportText.contains("duplicate_existing"))
    }

    @Test
    fun `service unions parallel duplicate check chunks across types and time spans`() = runTest {
        // Two record types, each spanning far more than the 6h duplicate-check window,
        // producing multiple disjoint lookup chunks that run concurrently.
        val xml =
            """
            <HealthData>
                <Record type="HKQuantityTypeIdentifierHeartRate" sourceName="Watch"
                    startDate="2026-01-01 08:00:00 +0000" endDate="2026-01-01 08:00:00 +0000"
                    unit="count/min" value="62" />
                <Record type="HKQuantityTypeIdentifierHeartRate" sourceName="Watch"
                    startDate="2026-01-02 08:00:00 +0000" endDate="2026-01-02 08:00:00 +0000"
                    unit="count/min" value="63" />
                <Record type="HKQuantityTypeIdentifierBodyMass" sourceName="Scale"
                    startDate="2026-01-01 09:00:00 +0000" endDate="2026-01-01 09:00:00 +0000"
                    unit="kg" value="70" />
                <Record type="HKQuantityTypeIdentifierBodyMass" sourceName="Scale"
                    startDate="2026-01-03 09:00:00 +0000" endDate="2026-01-03 09:00:00 +0000"
                    unit="kg" value="71" />
            </HealthData>
            """.trimIndent()
        val uri = mockk<Uri>()
        val resolver = mockk<ContentResolver>()
        val context = mockk<Context>()
        val repository = mockk<AppleHealthImportRepository>()
        val queriedRanges = java.util.Collections.synchronizedList(mutableListOf<Pair<String, Set<String>>>())
        val insertedRecords = java.util.Collections.synchronizedList(
            mutableListOf<androidx.health.connect.client.records.Record>(),
        )

        every { context.contentResolver } returns resolver
        every { resolver.openInputStream(uri) } returns ByteArrayInputStream(xml.toByteArray())
        every { repository.isMindfulnessAvailable() } returns true
        coEvery { repository.findMatchingImportedClientRecordIds(any(), any(), any(), any()) } coAnswers {
            val recordType = arg<kotlin.reflect.KClass<*>>(0)
            val wantedIds = arg<Set<String>>(3)
            queriedRanges += recordType.simpleName.orEmpty() to wantedIds
            // Report the first wanted id of every heart-rate chunk as already imported.
            if (recordType.simpleName == "HeartRateRecord") setOf(wantedIds.first()) else emptySet()
        }
        coEvery { repository.insertImportedRecords(any()) } coAnswers {
            insertedRecords += firstArg<List<androidx.health.connect.client.records.Record>>()
        }

        val result = AppleHealthImportService(context, repository).importAppleHealthExport(uri)

        // 4 disjoint chunks queried: 2 heart-rate (1 day apart) + 2 body-mass (2 days apart).
        assertEquals(4, queriedRanges.size)
        assertEquals(2, queriedRanges.count { it.first == "HeartRateRecord" })
        assertEquals(2, queriedRanges.count { it.first == "WeightRecord" })
        // Both heart-rate chunks were marked duplicate; both weight records imported.
        assertEquals(2, result.duplicateSkippedRecords)
        assertEquals(2, result.importedRecords)
        assertTrue(insertedRecords.all { it is androidx.health.connect.client.records.WeightRecord })
    }

    private fun heartRateExport(count: Int, duplicateIndexOf: Pair<Int, Int>? = null): String =
        buildString {
            appendLine("<HealthData>")
            repeat(count) { index ->
                val effectiveIndex = if (duplicateIndexOf != null && index == duplicateIndexOf.first) {
                    duplicateIndexOf.second
                } else {
                    index
                }
                val hour = (effectiveIndex / 60).toString().padStart(2, '0')
                val minute = (effectiveIndex % 60).toString().padStart(2, '0')
                appendLine(
                    """<Record type="HKQuantityTypeIdentifierHeartRate" sourceName="Watch" """ +
                        """startDate="2026-01-01 $hour:$minute:00 +0000" """ +
                        """endDate="2026-01-01 $hour:$minute:00 +0000" unit="count/min" value="62" />""",
                )
            }
            appendLine("</HealthData>")
        }

    @Test
    fun `parser light mode keeps counts but skips dates metadata and numeric values`() {
        val xml =
            """
            <HealthData>
                <Record type="HKQuantityTypeIdentifierHeartRate" sourceName="Watch"
                    creationDate="2026-01-01 08:00:00 +0000"
                    startDate="2026-01-01 08:00:00 +0000" endDate="2026-01-01 08:00:00 +0000"
                    unit="count/min" value="62">
                    <MetadataEntry key="HKMetadataKeyHeartRateMotionContext" value="1" />
                </Record>
                <Workout workoutActivityType="HKWorkoutActivityTypeRunning" sourceName="Watch"
                    startDate="2026-01-01 09:00:00 +0000" endDate="2026-01-01 09:30:00 +0000"
                    duration="30" durationUnit="min" />
            </HealthData>
            """.trimIndent()

        val fullRecords = mutableListOf<AppleRecord>()
        val lightRecords = mutableListOf<AppleRecord>()
        val lightWorkouts = mutableListOf<AppleWorkout>()

        fun consumer(records: MutableList<AppleRecord>, workouts: MutableList<AppleWorkout>? = null) =
            object : AppleHealthXmlEventConsumer {
                override fun onParsedType(type: String) = Unit
                override fun onRecord(record: AppleRecord) { records += record }
                override fun onWorkout(workout: AppleWorkout) { workouts?.add(workout) }
                override fun onCorrelation(correlation: AppleCorrelation) = Unit
                override fun onActivitySummary() = Unit
            }

        val fullParsed = AppleHealthImportParser.parse(
            BufferedInputStream(ByteArrayInputStream(xml.toByteArray())),
            consumer(fullRecords),
            AppleHealthParseOptions(),
        )
        val lightParsed = AppleHealthImportParser.parse(
            BufferedInputStream(ByteArrayInputStream(xml.toByteArray())),
            consumer(lightRecords, lightWorkouts),
            AppleHealthParseOptions(parseRouteFiles = false, parseRecordDetails = false),
        )

        assertEquals(fullParsed.parsedRecords, lightParsed.parsedRecords)
        assertEquals(fullParsed.parsedWorkouts, lightParsed.parsedWorkouts)
        assertEquals(fullParsed.parsedTypeCounts, lightParsed.parsedTypeCounts)

        val full = fullRecords.single()
        val light = lightRecords.single()
        assertEquals(full.type, light.type)
        assertEquals(full.rawValue, light.rawValue)
        assertTrue(full.startDate != null && full.numericValue != null && full.metadata.isNotEmpty())
        assertEquals(null, light.startDate)
        assertEquals(null, light.endDate)
        assertEquals(null, light.creationDate)
        assertEquals(null, light.numericValue)
        assertTrue(light.metadata.isEmpty())
        val lightWorkout = lightWorkouts.single()
        assertEquals("HKWorkoutActivityTypeRunning", lightWorkout.workoutActivityType)
        assertEquals(null, lightWorkout.startDate)
    }

    @Test
    fun `service analysis streams zip when route files precede export xml`() = runTest {
        val xml =
            """
            <HealthData>
                <Workout workoutActivityType="HKWorkoutActivityTypeRunning" sourceName="Apple Watch"
                    startDate="2026-01-01 08:00:00 +0000" endDate="2026-01-01 08:30:00 +0000"
                    duration="30" durationUnit="min">
                    <WorkoutRoute sourceName="Apple Watch"
                        startDate="2026-01-01 08:00:00 +0000" endDate="2026-01-01 08:30:00 +0000">
                        <FileReference path="/workout-routes/route_2026-01-01_8.00am.gpx" />
                    </WorkoutRoute>
                </Workout>
            </HealthData>
            """.trimIndent()
        val uri = mockk<Uri>()
        val resolver = mockk<ContentResolver>()
        val context = mockk<Context>()
        val repository = mockk<AppleHealthImportRepository>()

        every { context.contentResolver } returns resolver
        every { resolver.openInputStream(uri) } returns ByteArrayInputStream(
            zipExport(
                xml,
                mapOf("apple_health_export/workout-routes/route_2026-01-01_8.00am.gpx" to "<not-gpx>"),
                extraFilesBeforeXml = true,
            ),
        )
        every { repository.isMindfulnessAvailable() } returns true

        val result = AppleHealthImportService(context, repository).analyzeAppleHealthExport(uri)

        val workoutSummary = result.categorySummaries.single { it.category == AppleHealthImportCategory.WORKOUTS }
        assertEquals(1, result.parsedWorkouts)
        assertEquals(1, workoutSummary.convertedRecords)
        assertEquals(1, workoutSummary.routeSessions)
        coVerify(exactly = 0) { repository.insertImportedRecords(any()) }
    }

    private fun parseXml(xml: String): AppleParsedExport =
        AppleHealthImportParser.parse(BufferedInputStream(ByteArrayInputStream(xml.toByteArray())))

    private fun parseFixture(name: String): AppleParsedExport {
        val input = requireNotNull(javaClass.getResourceAsStream("/apple_health/$name")) {
            "Missing fixture $name"
        }
        return input.use { AppleHealthImportParser.parse(BufferedInputStream(it)) }
    }

    private fun zipExport(
        xml: String,
        extraFiles: Map<String, String> = emptyMap(),
        extraFilesBeforeXml: Boolean = false,
    ): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            fun writeExtras() = extraFiles.forEach { (path, contents) ->
                zip.putNextEntry(ZipEntry(path))
                zip.write(contents.toByteArray())
                zip.closeEntry()
            }
            if (extraFilesBeforeXml) writeExtras()
            zip.putNextEntry(ZipEntry("apple_health_export/export.xml"))
            zip.write(xml.toByteArray())
            zip.closeEntry()
            if (!extraFilesBeforeXml) writeExtras()
        }
        return output.toByteArray()
    }

    private fun ByteArray.truncateInsideEntry(entryName: String): ByteArray {
        val nameBytes = entryName.toByteArray()
        val nameOffset = indexOfSequence(nameBytes)
        require(nameOffset >= ZipLocalHeaderSize) { "ZIP entry not found: $entryName" }
        val headerOffset = nameOffset - ZipLocalHeaderSize
        require(
            this[headerOffset] == 0x50.toByte() &&
                this[headerOffset + 1] == 0x4b.toByte() &&
                this[headerOffset + 2] == 0x03.toByte() &&
                this[headerOffset + 3] == 0x04.toByte(),
        ) { "ZIP local header not found for: $entryName" }
        val extraLength = littleEndianUnsignedShort(headerOffset + 28)
        val compressedDataOffset = nameOffset + nameBytes.size + extraLength
        return copyOf(compressedDataOffset + TruncatedRouteCompressedBytes)
    }

    private fun ByteArray.indexOfSequence(sequence: ByteArray): Int {
        for (start in 0..size - sequence.size) {
            if (sequence.indices.all { offset -> this[start + offset] == sequence[offset] }) return start
        }
        return -1
    }

    private fun ByteArray.littleEndianUnsignedShort(offset: Int): Int =
        (this[offset].toInt() and 0xff) or ((this[offset + 1].toInt() and 0xff) shl 8)

    private class FailingAfterBytesInputStream(
        data: ByteArray,
        private val maxBytesBeforeFailure: Int,
    ) : ByteArrayInputStream(data) {
        private var bytesRead = 0

        override fun read(): Int {
            throwIfPastLimit()
            val value = super.read()
            if (value != -1) bytesRead++
            return value
        }

        override fun read(
            b: ByteArray,
            off: Int,
            len: Int,
        ): Int {
            throwIfPastLimit()
            val allowed = minOf(len, maxBytesBeforeFailure - bytesRead)
            val count = super.read(b, off, allowed)
            if (count > 0) bytesRead += count
            return count
        }

        private fun throwIfPastLimit() {
            if (bytesRead >= maxBytesBeforeFailure) {
                throw EOFException("Simulated document provider EOF")
            }
        }
    }

    private companion object {
        const val ZipLocalHeaderSize = 30
        const val TruncatedRouteCompressedBytes = 96
    }
}
