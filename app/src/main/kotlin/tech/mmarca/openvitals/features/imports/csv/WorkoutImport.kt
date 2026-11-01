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
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.util.Locale
import kotlin.math.roundToLong
import tech.mmarca.openvitals.core.privacy.PrivacyRedactor
import tech.mmarca.openvitals.domain.model.ActivityRecordSource
import tech.mmarca.openvitals.domain.model.ActivityWriteLimits
import tech.mmarca.openvitals.domain.model.ActivityWriteRequest
import tech.mmarca.openvitals.domain.model.BleHeartRateSample
import tech.mmarca.openvitals.domain.model.BleRecordingSampleBuffer
import tech.mmarca.openvitals.domain.model.ExerciseTypeNames
import tech.mmarca.openvitals.features.manualentry.activity.MaxActivityDurationMinutes
import tech.mmarca.openvitals.features.manualentry.activity.activityEntryTypeForExerciseType
import tech.mmarca.openvitals.features.manualentry.activity.matchExerciseType

/**
 * The app's own workout CSV export, recognised by its header and read without a
 * column mapping. The activity detail screen writes one file per workout with
 * English snake_case headers, SI units and ISO-8601 times; a file assembled by
 * hand from other apps' exports carries the same headers and, often, local
 * times, so the date and time settings stay in the user's hands.
 *
 * Each row becomes one [ActivityWriteRequest], written through the activity
 * repository like a route or FIT file: the session plus the totals beside it,
 * and a flat heart-rate series when the file gives an average. Pure: a row in,
 * a request and diagnostics out, like the row converter.
 */

/** The export's header names. [Imported] become part of the workout; [Ignored] have no field a session can hold. */
object WorkoutCsvColumns {
    const val Title = "title"
    const val ActivityType = "activity_type"
    const val StartTime = "start_time"
    const val EndTime = "end_time"
    const val DurationSeconds = "duration_seconds"
    const val DistanceMeters = "distance_meters"
    const val ElevationGainedMeters = "elevation_gained_meters"
    const val Steps = "steps"
    const val TotalCaloriesKcal = "total_calories_kcal"
    const val ActiveCaloriesKcal = "active_calories_kcal"
    const val AverageHeartRateBpm = "average_heart_rate_bpm"
    const val FloorsClimbed = "floors_climbed"
    const val WheelchairPushes = "wheelchair_pushes"
    const val Notes = "notes"

    /** In the export's order. */
    val Imported: List<String> = listOf(
        Title,
        ActivityType,
        StartTime,
        EndTime,
        DurationSeconds,
        DistanceMeters,
        ElevationGainedMeters,
        Steps,
        TotalCaloriesKcal,
        ActiveCaloriesKcal,
        AverageHeartRateBpm,
        FloorsClimbed,
        WheelchairPushes,
        Notes,
    )

    /** Averages of series the file does not carry, and the writing app's name. */
    val Ignored: List<String> = listOf(
        "max_heart_rate_bpm",
        "average_speed_meters_per_second",
        "average_power_watts",
        "average_steps_cadence_spm",
        "average_cycling_cadence_rpm",
        "source",
    )
}

/** A workout export's columns, by header name, and how its timestamps are read. */
data class WorkoutImportPlan(
    /** Header name, lowercased and trimmed, to column index; the first of a repeated name wins. */
    val columns: Map<String, Int>,
    /** Columns in the header, so the report can account for every one. */
    val columnCount: Int,
    val dateTime: CsvDateTimeSettings = CsvDateTimeSettings(),
) {
    /** The columns that become part of the workout, in the export's order. */
    val recognisedColumns: Map<String, Int>
        get() = WorkoutCsvColumns.Imported.mapNotNull { name -> columns[name]?.let { name to it } }.toMap()

    /** The export's columns a session has no field for. */
    val ignoredColumns: Map<String, Int>
        get() = WorkoutCsvColumns.Ignored.mapNotNull { name -> columns[name]?.let { name to it } }.toMap()

    val startTimeColumn: Int get() = columns.getValue(WorkoutCsvColumns.StartTime)

    /** The session write always; a total's write only when its column is in the file. */
    val requiredWritePermissions: Set<String>
        get() = buildSet {
            add(HealthPermission.getWritePermission(ExerciseSessionRecord::class))
            if (WorkoutCsvColumns.DistanceMeters in columns) add(HealthPermission.getWritePermission(DistanceRecord::class))
            if (WorkoutCsvColumns.ElevationGainedMeters in columns) {
                add(HealthPermission.getWritePermission(ElevationGainedRecord::class))
            }
            if (WorkoutCsvColumns.ActiveCaloriesKcal in columns) {
                add(HealthPermission.getWritePermission(ActiveCaloriesBurnedRecord::class))
            }
            if (WorkoutCsvColumns.TotalCaloriesKcal in columns) {
                add(HealthPermission.getWritePermission(TotalCaloriesBurnedRecord::class))
            }
            if (WorkoutCsvColumns.Steps in columns) add(HealthPermission.getWritePermission(StepsRecord::class))
            if (WorkoutCsvColumns.FloorsClimbed in columns) add(HealthPermission.getWritePermission(FloorsClimbedRecord::class))
            if (WorkoutCsvColumns.WheelchairPushes in columns) {
                add(HealthPermission.getWritePermission(WheelchairPushesRecord::class))
            }
            if (WorkoutCsvColumns.AverageHeartRateBpm in columns) add(HealthPermission.getWritePermission(HeartRateRecord::class))
        }

    fun withDateTime(settings: CsvDateTimeSettings): WorkoutImportPlan = copy(dateTime = settings)
}

/**
 * [headerRow] as a workout plan, or null when it is not a workout export. The
 * signature is the activity type, the start, and an end or a duration; case
 * and order do not matter, and extra columns are allowed.
 */
fun detectWorkoutExport(
    headerRow: List<String>,
    dateTime: CsvDateTimeSettings = CsvDateTimeSettings(),
): WorkoutImportPlan? {
    val columns = mutableMapOf<String, Int>()
    headerRow.forEachIndexed { index, name ->
        val key = name.trim().lowercase(Locale.US)
        if (key.isNotEmpty() && key !in columns) columns[key] = index
    }
    if (WorkoutCsvColumns.ActivityType !in columns || WorkoutCsvColumns.StartTime !in columns) return null
    if (WorkoutCsvColumns.EndTime !in columns && WorkoutCsvColumns.DurationSeconds !in columns) return null
    return WorkoutImportPlan(columns = columns, columnCount = headerRow.size, dateTime = dateTime)
}

/**
 * The exercise type [text] names: the export's own name first, then the words
 * the route importer understands, with `_` read as a space so
 * `functional_strength_training` finds strength training. Null when neither does.
 */
internal fun resolveWorkoutExerciseType(text: String): Int? {
    ExerciseTypeNames.typeForExportName(text)?.let { return it }
    val words = text.trim().lowercase(Locale.US).replace('_', ' ')
    return words.takeIf { it.isNotEmpty() }?.let(::matchExerciseType)
}

/**
 * The identity of an imported workout: its start instant, as a metric's identity is
 * its type and instant. A corrected distance, title or type replaces the earlier
 * record; two workouts starting at the same instant collapse into one.
 */
fun workoutImportKey(startTime: Instant): String =
    csvStableDigest("ExerciseSessionRecord|${startTime.toEpochMilli()}")

/**
 * Converts one row under [plan]. A missing or unreadable start, a missing or
 * unreadable end, or an end not after the start costs the row; a bad total
 * costs that total only. A type the app cannot name is stored as an "other"
 * workout, with a diagnostic so the row is accounted for.
 */
fun convertWorkoutRow(row: CsvRow, plan: WorkoutImportPlan): CsvRowConversion {
    val cells = WorkoutCells(row, plan.columns)
    val diagnostics = mutableListOf<CsvImportDiagnostic>()

    val startColumn = plan.startTimeColumn
    val startText = cells.text(WorkoutCsvColumns.StartTime)
        ?: return CsvRowConversion(diagnostics = listOf(cells.diagnostic(CsvImportDiagnosticReason.MISSING_TIMESTAMP, startColumn)))
    val start = resolveCsvInstant(startText, plan.dateTime)
        ?: return CsvRowConversion(
            diagnostics = listOf(
                cells.diagnostic(CsvImportDiagnosticReason.UNPARSABLE_TIMESTAMP, startColumn, PrivacyRedactor.shapeOf(startText)),
            ),
        )
    val end = cells.workoutEnd(plan, start, diagnostics) ?: return CsvRowConversion(diagnostics = diagnostics)

    val typeText = cells.text(WorkoutCsvColumns.ActivityType)
    val resolvedType = typeText?.let(::resolveWorkoutExerciseType)
    val exerciseType = resolvedType ?: ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
    if (resolvedType == null) {
        diagnostics += cells.diagnostic(
            CsvImportDiagnosticReason.ACTIVITY_TYPE_DEFAULTED,
            cells.index(WorkoutCsvColumns.ActivityType),
            typeText?.let(PrivacyRedactor::shapeOf),
        )
    }

    val entryType = activityEntryTypeForExerciseType(exerciseType)
    val distance = cells.total(WorkoutCsvColumns.DistanceMeters, ActivityWriteLimits.MaxDistanceMeters, diagnostics)
        ?.takeIf { cells.supported(WorkoutCsvColumns.DistanceMeters, entryType.supportsDistance, diagnostics) }
    val elevation = cells.total(WorkoutCsvColumns.ElevationGainedMeters, ActivityWriteLimits.MaxElevationMeters, diagnostics)
        ?.takeIf { cells.supported(WorkoutCsvColumns.ElevationGainedMeters, entryType.supportsElevation, diagnostics) }
    val steps = cells.total(WorkoutCsvColumns.Steps, ActivityWriteLimits.MaxSteps.toDouble(), diagnostics)?.roundToLong()
    val total = cells.total(WorkoutCsvColumns.TotalCaloriesKcal, ActivityWriteLimits.MaxCaloriesKcal, diagnostics)
    val active = cells.total(WorkoutCsvColumns.ActiveCaloriesKcal, ActivityWriteLimits.MaxCaloriesKcal, diagnostics)
        ?.takeIf { active ->
            // Health Connect refuses a total below the active part; the total is the one to keep.
            val fits = total == null || active <= total
            if (!fits) {
                diagnostics += cells.diagnostic(
                    CsvImportDiagnosticReason.ACTIVE_ABOVE_TOTAL_CALORIES,
                    cells.index(WorkoutCsvColumns.ActiveCaloriesKcal),
                )
            }
            fits
        }
    val floors = cells.total(WorkoutCsvColumns.FloorsClimbed, ActivityWriteLimits.MaxFloors, diagnostics)
    val pushes = cells.total(WorkoutCsvColumns.WheelchairPushes, ActivityWriteLimits.MaxWheelchairPushes.toDouble(), diagnostics)
        ?.roundToLong()

    return CsvRowConversion(
        diagnostics = diagnostics,
        activity = ActivityWriteRequest(
            exerciseType = exerciseType,
            startTime = start.utc,
            endTime = end.utc,
            title = cells.text(WorkoutCsvColumns.Title),
            notes = cells.text(WorkoutCsvColumns.Notes),
            stepsCount = steps,
            distanceMeters = distance,
            elevationGainedMeters = elevation,
            activeCaloriesKcal = active,
            totalCaloriesKcal = total,
            floorsClimbed = floors,
            wheelchairPushes = pushes,
            bleSamples = cells.heartRateSeries(start.utc, end.utc, diagnostics),
            source = ActivityRecordSource.FILE,
            importKey = workoutImportKey(start.utc),
        ),
    )
}

/** The sampled rows' first and last start as wall clock, or null when none resolves. For the confirm step. */
fun workoutSampleDateRange(rows: List<List<String>>, plan: WorkoutImportPlan): Pair<LocalDateTime, LocalDateTime>? {
    val column = plan.startTimeColumn
    val starts = rows.mapNotNull { fields ->
        val text = fields.getOrNull(column)?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
        val instant = resolveCsvInstant(text, plan.dateTime) ?: return@mapNotNull null
        LocalDateTime.ofInstant(instant.utc, instant.offset)
    }
    if (starts.isEmpty()) return null
    return starts.min() to starts.max()
}

/** How many of the sampled [rows] become a workout under [plan]. */
fun workoutSampleRowsWithContent(rows: List<List<String>>, plan: WorkoutImportPlan): Int =
    rows.count { fields -> convertWorkoutRow(CsvRow(rowNumber = 0, fields = fields), plan).activity != null }

/** Typed access to one row's cells by header name. A missing column reads as empty. */
private class WorkoutCells(private val row: CsvRow, private val columns: Map<String, Int>) {
    fun text(name: String): String? = columns[name]?.let { row.cell(it) }

    fun index(name: String): Int? = columns[name]

    fun diagnostic(reason: CsvImportDiagnosticReason, columnIndex: Int?, detail: String? = null): CsvImportDiagnostic =
        CsvImportDiagnostic(rowNumber = row.rowNumber, reason = reason, columnIndex = columnIndex, detail = detail)

    /**
     * Where the workout ends: the end column when it holds something, else the start
     * plus the duration. Neither, an end that will not read, an end not after the
     * start, or a span past what an activity may last: null, with the reason added.
     */
    fun workoutEnd(plan: WorkoutImportPlan, start: CsvInstant, diagnostics: MutableList<CsvImportDiagnostic>): CsvInstant? {
        val endColumn = index(WorkoutCsvColumns.EndTime)
        val durationColumn = index(WorkoutCsvColumns.DurationSeconds)
        val endText = text(WorkoutCsvColumns.EndTime)
        val end: CsvInstant = if (endText != null) {
            resolveCsvInstant(endText, plan.dateTime) ?: run {
                diagnostics += diagnostic(CsvImportDiagnosticReason.UNPARSABLE_END_TIMESTAMP, endColumn, PrivacyRedactor.shapeOf(endText))
                return null
            }
        } else {
            val durationText = text(WorkoutCsvColumns.DurationSeconds) ?: run {
                diagnostics += diagnostic(CsvImportDiagnosticReason.MISSING_END, endColumn ?: durationColumn)
                return null
            }
            val seconds = parseCsvNumber(durationText)
            if (seconds == null || seconds <= 0.0) {
                diagnostics += diagnostic(CsvImportDiagnosticReason.UNPARSABLE_NUMBER, durationColumn, PrivacyRedactor.shapeOf(durationText))
                return null
            }
            CsvInstant(utc = start.utc.plusMillis((seconds * 1000).roundToLong()), offset = start.offset)
        }
        val spanColumn = if (endText != null) endColumn else durationColumn
        if (!end.utc.isAfter(start.utc)) {
            diagnostics += diagnostic(CsvImportDiagnosticReason.END_NOT_AFTER_START, spanColumn)
            return null
        }
        if (Duration.between(start.utc, end.utc).toMinutes() > MaxActivityDurationMinutes) {
            diagnostics += diagnostic(CsvImportDiagnosticReason.OUT_OF_RANGE, spanColumn)
            return null
        }
        return end
    }

    /**
     * The total in column [name], or null: blank is a gap, zero says nothing, a value
     * that is not a number or is outside 0..[max] is rejected with its reason.
     */
    fun total(name: String, max: Double, diagnostics: MutableList<CsvImportDiagnostic>): Double? {
        val text = text(name) ?: return null
        val column = index(name)
        val value = parseCsvNumber(text)
        if (value == null) {
            diagnostics += diagnostic(CsvImportDiagnosticReason.UNPARSABLE_NUMBER, column, PrivacyRedactor.shapeOf(text))
            return null
        }
        if (value < 0.0 || value > max) {
            diagnostics += diagnostic(CsvImportDiagnosticReason.OUT_OF_RANGE, column)
            return null
        }
        return value.takeIf { it > 0.0 }
    }

    /** True when the type carries this total; otherwise the reason is added and the total is dropped. */
    fun supported(name: String, supported: Boolean, diagnostics: MutableList<CsvImportDiagnostic>): Boolean {
        if (!supported) {
            diagnostics += diagnostic(CsvImportDiagnosticReason.UNSUPPORTED_FOR_ACTIVITY_TYPE, index(name))
        }
        return supported
    }

    /**
     * The average heart rate as a flat series: one sample at the start and one a
     * second before the end, both at the average, so the workout's heart-rate
     * chart shows a line at the figure the file gave. A one-second session gets
     * the start sample only.
     */
    fun heartRateSeries(start: Instant, end: Instant, diagnostics: MutableList<CsvImportDiagnostic>): BleRecordingSampleBuffer {
        val text = text(WorkoutCsvColumns.AverageHeartRateBpm) ?: return BleRecordingSampleBuffer()
        val column = index(WorkoutCsvColumns.AverageHeartRateBpm)
        val value = parseCsvNumber(text)
        if (value == null) {
            diagnostics += diagnostic(CsvImportDiagnosticReason.UNPARSABLE_NUMBER, column, PrivacyRedactor.shapeOf(text))
            return BleRecordingSampleBuffer()
        }
        val bpm = value.roundToLong()
        if (bpm < ActivityWriteLimits.MinHeartRateBpm || bpm > ActivityWriteLimits.MaxHeartRateBpm) {
            diagnostics += diagnostic(CsvImportDiagnosticReason.OUT_OF_RANGE, column)
            return BleRecordingSampleBuffer()
        }
        val last = end.minusSeconds(1)
        val samples = if (last.isAfter(start)) {
            listOf(BleHeartRateSample(start, bpm), BleHeartRateSample(last, bpm))
        } else {
            listOf(BleHeartRateSample(start, bpm))
        }
        return BleRecordingSampleBuffer(heartRateSamples = samples)
    }
}
