package tech.mmarca.openvitals.features.imports.csv

import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ElevationGainedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WheelchairPushesRecord
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.core.privacy.PrivacyRedactor
import tech.mmarca.openvitals.domain.model.ActivityRecordSource
import tech.mmarca.openvitals.domain.model.ActivityWriteRequest
import tech.mmarca.openvitals.domain.model.BleHeartRateSample

/** The header the activity detail screen writes, verbatim from ActivityWorkoutExport. */
private const val ExportHeader =
    "title,activity_type,start_time,end_time,duration_seconds,distance_meters,elevation_gained_meters,steps," +
        "total_calories_kcal,active_calories_kcal,average_heart_rate_bpm,max_heart_rate_bpm," +
        "average_speed_meters_per_second,average_power_watts,average_steps_cadence_spm,average_cycling_cadence_rpm," +
        "floors_climbed,wheelchair_pushes,source,notes"

private val Header = ExportHeader.split(",")
private val Utc = CsvDateTimeSettings(zone = CsvTimeZoneMode.UTC)
private val Start: Instant = Instant.parse("2026-05-26T08:30:00Z")
private val End: Instant = Instant.parse("2026-05-26T09:30:00Z")

private fun plan(dateTime: CsvDateTimeSettings = Utc): WorkoutImportPlan = detectWorkoutExport(Header, dateTime)!!

/** A row with every column empty except the named ones. */
private fun row(vararg cells: Pair<String, String>, rowNumber: Int = 2): CsvRow {
    val fields = MutableList(Header.size) { "" }
    for ((name, value) in cells) fields[Header.indexOf(name)] = value
    return CsvRow(rowNumber = rowNumber, fields = fields)
}

/** A running hour, with whatever [cells] add or override. */
private fun runRow(vararg cells: Pair<String, String>, plan: WorkoutImportPlan = plan()): CsvRowConversion {
    val base = mapOf("activity_type" to "running", "start_time" to Start.toString(), "end_time" to End.toString())
    return convertWorkoutRow(row(*(base + cells).toList().toTypedArray()), plan)
}

private fun CsvRowConversion.reasons(): List<CsvImportDiagnosticReason> = diagnostics.map { it.reason }

private fun CsvRowConversion.request(): ActivityWriteRequest = activity ?: error("no workout: ${reasons()}")

class WorkoutImportTest {

    @Test
    fun `the export header is recognised and a scale export or a drip export is not`() {
        assertNotNull(detectWorkoutExport(Header))
        assertNull(detectWorkoutExport(listOf("Date", "Weight (kg)", "Fat mass (kg)")))
        assertNull(detectWorkoutExport(listOf("date", "bleeding.value", "temperature.value", "mucus.feeling")))
        assertNull(detectDripExport(Header))
    }

    @Test
    fun `detection ignores case and space and reads the columns by name, so a reordered file still imports`() {
        val reordered = detectWorkoutExport(listOf("Activity_Type ", "Notes", "extra", "START_TIME", "duration_seconds"))!!
        assertEquals(0, reordered.columns["activity_type"])
        assertEquals(3, reordered.startTimeColumn)
        assertEquals(listOf("activity_type", "start_time", "duration_seconds", "notes"), reordered.recognisedColumns.keys.toList())
        assertEquals(5, reordered.columnCount)
        assertTrue(reordered.ignoredColumns.isEmpty())
    }

    @Test
    fun `a start and a type are required, and an end or a duration`() {
        assertNotNull(detectWorkoutExport(listOf("activity_type", "start_time", "duration_seconds")))
        assertNotNull(detectWorkoutExport(listOf("activity_type", "start_time", "end_time")))
        assertNull(detectWorkoutExport(listOf("activity_type", "start_time")))
        assertNull(detectWorkoutExport(listOf("activity_type", "end_time", "duration_seconds")))
        assertNull(detectWorkoutExport(listOf("title", "start_time", "end_time")))
    }

    @Test
    fun `the required write permissions follow the columns in the file`() {
        val full = plan().requiredWritePermissions
        assertEquals(
            setOf(
                HealthPermission.getWritePermission(ExerciseSessionRecord::class),
                HealthPermission.getWritePermission(DistanceRecord::class),
                HealthPermission.getWritePermission(ElevationGainedRecord::class),
                HealthPermission.getWritePermission(ActiveCaloriesBurnedRecord::class),
                HealthPermission.getWritePermission(TotalCaloriesBurnedRecord::class),
                HealthPermission.getWritePermission(StepsRecord::class),
                HealthPermission.getWritePermission(FloorsClimbedRecord::class),
                HealthPermission.getWritePermission(WheelchairPushesRecord::class),
                HealthPermission.getWritePermission(HeartRateRecord::class),
            ),
            full,
        )
        val bare = detectWorkoutExport(listOf("activity_type", "start_time", "duration_seconds"))!!.requiredWritePermissions
        assertEquals(setOf(HealthPermission.getWritePermission(ExerciseSessionRecord::class)), bare)
    }

    @Test
    fun `an export row becomes a workout with every total the file gives`() {
        val conversion = runRow(
            "title" to "Morning, \"Run\"",
            "distance_meters" to "10250.50",
            "steps" to "9800",
            "total_calories_kcal" to "640.25",
            "average_heart_rate_bpm" to "151",
            "max_heart_rate_bpm" to "172",
            "floors_climbed" to "3",
            "source" to "tech.mmarca.openvitals",
            "notes" to "line1",
        )

        assertTrue(conversion.diagnostics.isEmpty())
        val request = conversion.request()
        assertEquals(ExerciseSessionRecord.EXERCISE_TYPE_RUNNING, request.exerciseType)
        assertEquals(Start, request.startTime)
        assertEquals(End, request.endTime)
        assertEquals("Morning, \"Run\"", request.title)
        assertEquals("line1", request.notes)
        assertEquals(10_250.5, request.distanceMeters!!, 1e-9)
        assertNull(request.elevationGainedMeters)
        assertEquals(9_800L, request.stepsCount)
        assertEquals(640.25, request.totalCaloriesKcal!!, 1e-9)
        assertNull(request.activeCaloriesKcal)
        assertEquals(3.0, request.floorsClimbed!!, 1e-9)
        assertNull(request.wheelchairPushes)
        assertEquals(ActivityRecordSource.FILE, request.source)
        assertEquals(workoutImportKey(Start), request.importKey)
        // The average as a flat line: one sample at the start, one a second before the end.
        assertEquals(
            listOf(BleHeartRateSample(Start, 151L), BleHeartRateSample(End.minusSeconds(1), 151L)),
            request.bleSamples.heartRateSamples,
        )
    }

    @Test
    fun `a free-text type is matched by its words and an unknown one is stored as a workout with a reason`() {
        assertEquals(ExerciseSessionRecord.EXERCISE_TYPE_RUNNING, runRow("activity_type" to "Outdoor Run").request().exerciseType)
        assertEquals(
            ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
            runRow("activity_type" to "functional_strength_training").request().exerciseType,
        )
        assertEquals(ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY, runRow("activity_type" to "Biking_Stationary").request().exerciseType)

        val unknown = runRow("activity_type" to "Zorbing")
        assertEquals(ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT, unknown.request().exerciseType)
        val reason = unknown.diagnostics.single()
        assertEquals(CsvImportDiagnosticReason.ACTIVITY_TYPE_DEFAULTED, reason.reason)
        assertEquals(Header.indexOf("activity_type"), reason.columnIndex)
        assertEquals(PrivacyRedactor.shapeOf("Zorbing"), reason.detail)

        val blank = runRow("activity_type" to "")
        assertEquals(ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT, blank.request().exerciseType)
        assertNull(blank.diagnostics.single().detail)
    }

    @Test
    fun `the start is required and must read, or the row is lost`() {
        val missing = runRow("start_time" to "")
        assertNull(missing.activity)
        assertEquals(listOf(CsvImportDiagnosticReason.MISSING_TIMESTAMP), missing.reasons())

        val garbage = runRow("start_time" to "yesterday morning")
        assertNull(garbage.activity)
        assertEquals(CsvImportDiagnosticReason.UNPARSABLE_TIMESTAMP, garbage.diagnostics.single().reason)
        assertEquals(PrivacyRedactor.shapeOf("yesterday morning"), garbage.diagnostics.single().detail)
    }

    @Test
    fun `a time that carries its offset wins and a local time takes the chosen zone`() {
        val device = plan(CsvDateTimeSettings(zone = CsvTimeZoneMode.DEVICE))
        assertEquals(Start, runRow(plan = device).request().startTime)

        val local = "2026-05-26 08:30:00"
        val localEnd = "2026-05-26 09:30:00"
        assertEquals(Start, runRow("start_time" to local, "end_time" to localEnd, plan = plan(Utc)).request().startTime)
        val fixed = plan(CsvDateTimeSettings(zone = CsvTimeZoneMode.FIXED_OFFSET, fixedOffset = ZoneOffset.ofHours(2)))
        assertEquals(Start.minusSeconds(2 * 3600), runRow("start_time" to local, "end_time" to localEnd, plan = fixed).request().startTime)
    }

    @Test
    fun `the end comes from the end column, else from the duration, and must follow the start`() {
        // Both given and disagreeing: the end column is the one the export writes exactly.
        assertEquals(End, runRow("duration_seconds" to "60").request().endTime)
        assertEquals(Start.plusSeconds(3_600), runRow("end_time" to "", "duration_seconds" to "3600").request().endTime)
        assertEquals(Start.plusMillis(1_500), runRow("end_time" to "", "duration_seconds" to "1.5").request().endTime)

        val neither = runRow("end_time" to "")
        assertNull(neither.activity)
        assertEquals(CsvImportDiagnosticReason.MISSING_END, neither.diagnostics.single().reason)
        assertEquals(Header.indexOf("end_time"), neither.diagnostics.single().columnIndex)

        val backwards = runRow("end_time" to Start.minusSeconds(60).toString())
        assertNull(backwards.activity)
        assertEquals(listOf(CsvImportDiagnosticReason.END_NOT_AFTER_START), backwards.reasons())

        val garbage = runRow("end_time" to "soon")
        assertNull(garbage.activity)
        assertEquals(CsvImportDiagnosticReason.UNPARSABLE_END_TIMESTAMP, garbage.diagnostics.single().reason)
        assertEquals(PrivacyRedactor.shapeOf("soon"), garbage.diagnostics.single().detail)

        val badDuration = runRow("end_time" to "", "duration_seconds" to "an hour")
        assertNull(badDuration.activity)
        assertEquals(CsvImportDiagnosticReason.UNPARSABLE_NUMBER, badDuration.diagnostics.single().reason)
        assertEquals(Header.indexOf("duration_seconds"), badDuration.diagnostics.single().columnIndex)

        val week = runRow("end_time" to Start.plusSeconds(8 * 24 * 3600).toString())
        assertNull(week.activity)
        assertEquals(listOf(CsvImportDiagnosticReason.OUT_OF_RANGE), week.reasons())
    }

    @Test
    fun `a bad total costs only that total and a zero or a blank is a gap`() {
        val unreadable = runRow("distance_meters" to "ten k", "steps" to "9800")
        assertNull(unreadable.request().distanceMeters)
        assertEquals(9_800L, unreadable.request().stepsCount)
        assertEquals(CsvImportDiagnosticReason.UNPARSABLE_NUMBER, unreadable.diagnostics.single().reason)
        assertEquals(Header.indexOf("distance_meters"), unreadable.diagnostics.single().columnIndex)
        assertEquals(PrivacyRedactor.shapeOf("ten k"), unreadable.diagnostics.single().detail)

        assertEquals(listOf(CsvImportDiagnosticReason.OUT_OF_RANGE), runRow("distance_meters" to "-5").reasons())
        assertEquals(listOf(CsvImportDiagnosticReason.OUT_OF_RANGE), runRow("total_calories_kcal" to "2000000").reasons())

        val zero = runRow("distance_meters" to "0", "steps" to "")
        assertTrue(zero.diagnostics.isEmpty())
        assertNull(zero.request().distanceMeters)
        assertNull(zero.request().stepsCount)
    }

    @Test
    fun `a total the activity type cannot carry is dropped with a reason`() {
        val yoga = runRow("activity_type" to "yoga", "distance_meters" to "1000", "elevation_gained_meters" to "20", "total_calories_kcal" to "200")
        val request = yoga.request()
        assertNull(request.distanceMeters)
        assertNull(request.elevationGainedMeters)
        assertEquals(200.0, request.totalCaloriesKcal!!, 1e-9)
        assertEquals(
            listOf(CsvImportDiagnosticReason.UNSUPPORTED_FOR_ACTIVITY_TYPE, CsvImportDiagnosticReason.UNSUPPORTED_FOR_ACTIVITY_TYPE),
            yoga.reasons(),
        )
        assertEquals(
            listOf(Header.indexOf("distance_meters"), Header.indexOf("elevation_gained_meters")),
            yoga.diagnostics.map { it.columnIndex },
        )

        val running = runRow("distance_meters" to "1000", "elevation_gained_meters" to "20")
        assertTrue(running.diagnostics.isEmpty())
        assertEquals(20.0, running.request().elevationGainedMeters!!, 1e-9)
    }

    @Test
    fun `active calories above the total are dropped and the total is kept`() {
        val conversion = runRow("total_calories_kcal" to "600", "active_calories_kcal" to "700")
        assertEquals(600.0, conversion.request().totalCaloriesKcal!!, 1e-9)
        assertNull(conversion.request().activeCaloriesKcal)
        assertEquals(CsvImportDiagnosticReason.ACTIVE_ABOVE_TOTAL_CALORIES, conversion.diagnostics.single().reason)
        assertEquals(Header.indexOf("active_calories_kcal"), conversion.diagnostics.single().columnIndex)

        val fits = runRow("total_calories_kcal" to "600", "active_calories_kcal" to "500")
        assertEquals(500.0, fits.request().activeCaloriesKcal!!, 1e-9)
        // Active alone is fine: nothing to compare against.
        assertEquals(500.0, runRow("active_calories_kcal" to "500").request().activeCaloriesKcal!!, 1e-9)
    }

    @Test
    fun `the average heart rate is a flat series, one sample when the session is a second long, nothing when absent or impossible`() {
        val short = runRow("end_time" to Start.plusSeconds(1).toString(), "average_heart_rate_bpm" to "140")
        assertEquals(listOf(BleHeartRateSample(Start, 140L)), short.request().bleSamples.heartRateSamples)

        val none = runRow("average_heart_rate_bpm" to "", "max_heart_rate_bpm" to "190")
        assertTrue(none.request().bleSamples.isEmpty())
        assertTrue(none.diagnostics.isEmpty())

        val impossible = runRow("average_heart_rate_bpm" to "300")
        assertTrue(impossible.request().bleSamples.isEmpty())
        assertEquals(CsvImportDiagnosticReason.OUT_OF_RANGE, impossible.diagnostics.single().reason)
        assertEquals(Header.indexOf("average_heart_rate_bpm"), impossible.diagnostics.single().columnIndex)

        val unreadable = runRow("average_heart_rate_bpm" to "high")
        assertTrue(unreadable.request().bleSamples.isEmpty())
        assertEquals(CsvImportDiagnosticReason.UNPARSABLE_NUMBER, unreadable.diagnostics.single().reason)
    }

    @Test
    fun `floors are read as a decimal and pushes as a count`() {
        val request = runRow("activity_type" to "wheelchair", "floors_climbed" to "12", "wheelchair_pushes" to "340.4").request()
        assertEquals(12.0, request.floorsClimbed!!, 1e-9)
        assertEquals(340L, request.wheelchairPushes)
    }

    @Test
    fun `title and notes are trimmed, blank ones are left out, and the source column is ignored`() {
        val request = runRow("title" to "  Hill repeats ", "notes" to " ", "source" to "com.example.other").request()
        assertEquals("Hill repeats", request.title)
        assertNull(request.notes)
    }

    @Test
    fun `the import key is the start instant, so a corrected row replaces and the same instant collapses`() {
        val first = runRow("distance_meters" to "10000").request().importKey
        assertEquals(first, runRow("distance_meters" to "10500").request().importKey)
        assertEquals(first, runRow("activity_type" to "hiking").request().importKey)
        assertEquals(first, runRow("activity_type" to "").request().importKey)
        assertNotEquals(first, runRow("start_time" to Start.plusSeconds(60).toString()).request().importKey)
        assertEquals(workoutImportKey(Start), first)
        assertEquals(32, first!!.length)
    }

    @Test
    fun `the sample helpers give the span of the sampled starts and the rows that hold a workout`() {
        val rows = listOf(
            row("activity_type" to "running", "start_time" to Start.toString(), "end_time" to End.toString()).fields,
            row("activity_type" to "yoga", "start_time" to "2026-05-28T07:00:00Z", "duration_seconds" to "1800").fields,
            row("activity_type" to "yoga", "start_time" to "", "duration_seconds" to "1800").fields,
            row("activity_type" to "walking", "start_time" to "2026-05-20T18:00:00Z", "duration_seconds" to "abc").fields,
        )

        assertEquals(
            LocalDateTime.of(2026, 5, 20, 18, 0) to LocalDateTime.of(2026, 5, 28, 7, 0),
            workoutSampleDateRange(rows, plan()),
        )
        assertEquals(2, workoutSampleRowsWithContent(rows, plan()))
        assertNull(workoutSampleDateRange(listOf(rows[2]), plan()))
    }

    @Test
    fun `resolving a type tries the export name before the free-text words`() {
        assertEquals(ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT, resolveWorkoutExerciseType("workout"))
        assertEquals(ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL, resolveWorkoutExerciseType("swimming_pool"))
        assertEquals(ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL, resolveWorkoutExerciseType("Treadmill run"))
        assertNull(resolveWorkoutExerciseType("   "))
    }
}
