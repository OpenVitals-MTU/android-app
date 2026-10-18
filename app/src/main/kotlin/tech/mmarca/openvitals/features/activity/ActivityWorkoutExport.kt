package tech.mmarca.openvitals.features.activity

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Xml
import androidx.core.content.FileProvider
import androidx.health.connect.client.records.ExerciseSessionRecord
import org.xmlpull.v1.XmlSerializer
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.export.stageExport
import tech.mmarca.openvitals.domain.model.ActivityCadenceKind
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.domain.model.HeartRateSample
import java.io.File
import java.io.OutputStream
import tech.mmarca.openvitals.core.performance.offMainIo
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Workout export with the session's metrics and samples. TCX, CSV and FIT
 * leave out the GPS route and exist for every workout, route or not: TCX
 * makes `Position` optional; CSV is for spreadsheets. [FIT_WITH_ROUTE] is
 * the whole recording, the file a FIT viewer or training site expects, and
 * is offered only beside the route formats.
 */
internal enum class ActivityWorkoutExportFormat(
    val mimeType: String,
    val extension: String,
) {
    TCX(TcxMimeType, "tcx"),
    CSV(CsvMimeType, "csv"),
    FIT(FitMimeType, "fit"),
    FIT_WITH_ROUTE(FitMimeType, "fit"),
}

internal suspend fun Context.saveActivityWorkoutExport(
    workout: ExerciseData,
    series: ActivityWorkoutSeries,
    format: ActivityWorkoutExportFormat,
    destination: Uri,
): Result<Unit> =
    offMainIo {
        contentResolver.openOutputStream(destination)?.use { output ->
            writeActivityWorkoutExport(
                workout = workout,
                series = series,
                format = format,
                output = output,
            )
        } ?: error("Unable to open export destination.")
    }

/** Same staging as [shareActivityRoute]: cache file, FileProvider URI, share sheet, no toast. */
internal suspend fun Context.shareActivityWorkout(
    workout: ExerciseData,
    series: ActivityWorkoutSeries,
    format: ActivityWorkoutExportFormat,
): Result<Unit> =
    offMainIo {
        File(cacheDir, WorkoutExportCacheDirectory)
            .stageExport(workout.workoutExportFileName(format)) { output ->
                writeActivityWorkoutExport(
                    workout = workout,
                    series = series,
                    format = format,
                    output = output,
                )
            }
    }.mapCatching { exportFile ->
        val uri = FileProvider.getUriForFile(
            this,
            "$packageName.fileprovider",
            exportFile,
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = format.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = ClipData.newUri(contentResolver, exportFile.name, uri)
        }
        startActivity(
            Intent.createChooser(
                shareIntent,
                getString(R.string.activity_workout_share_chooser_title),
            )
        )
    }

private fun writeActivityWorkoutExport(
    workout: ExerciseData,
    series: ActivityWorkoutSeries,
    format: ActivityWorkoutExportFormat,
    output: OutputStream,
) {
    when (format) {
        ActivityWorkoutExportFormat.TCX -> writeActivityWorkoutTcx(workout, series, output)
        ActivityWorkoutExportFormat.CSV -> writeActivityWorkoutCsv(workout, series.heartRate, output)
        ActivityWorkoutExportFormat.FIT -> writeActivityWorkoutFit(workout, series, output)
        ActivityWorkoutExportFormat.FIT_WITH_ROUTE -> writeActivityWorkoutFit(
            workout = workout,
            series = series,
            output = output,
            routePoints = workout.sortedRoutePointsForExport(),
        )
    }
}

/**
 * A routeless TCX: one `Lap` with the session totals and a `Track` with a
 * trackpoint for every second that has heart rate, speed or cadence.
 * Speed and running cadence go in Garmin's `TPX` extension, pedal cadence
 * in `Cadence`. A single lap even when laps were recorded: TCX requires
 * `Calories` per lap, and splitting totals would invent data.
 * [serializer] is a parameter only so a JVM test can supply one.
 */
internal fun writeActivityWorkoutTcx(
    workout: ExerciseData,
    series: ActivityWorkoutSeries,
    output: OutputStream,
    serializer: XmlSerializer = Xml.newSerializer(),
) {
    val samples = workout.heartRateSamplesForExport(series.heartRate)
    val averageBpm = workout.averageHeartRateForExport(samples)
    val maximumBpm = samples.maxOfOrNull { it.beatsPerMinute }
    val timeline = workout.workoutExportTimeline(series)
    val maximumSpeed = timeline.mapNotNull { it.speedMetersPerSecond }.maxOrNull()
    val isCycling = workout.exportCadenceKind() == ActivityCadenceKind.CYCLING
    val averageBikeCadence = workout.averageCadenceForExport(timeline)?.takeIf { isCycling }

    serializer.setOutput(output, Charsets.UTF_8.name())
    serializer.startDocument(Charsets.UTF_8.name(), true)
    serializer.startTag(null, "TrainingCenterDatabase")
    serializer.attribute(null, "xmlns", TcxNamespace)
    serializer.attribute(null, "xmlns:$TcxExtensionPrefix", TcxActivityExtensionNamespace)

    serializer.startTag(null, "Activities")
    serializer.startTag(null, "Activity")
    serializer.attribute(null, "Sport", workout.exerciseType.toTcxSport())
    serializer.textElement("Id", workout.startTime.toString())

    serializer.startTag(null, "Lap")
    serializer.attribute(null, "StartTime", workout.startTime.toString())
    serializer.textElement("TotalTimeSeconds", (workout.durationMs / 1000.0).toWorkoutDecimal())
    // `DistanceMeters` and `Calories` are required by the schema; zero when unknown.
    serializer.textElement("DistanceMeters", (workout.totalDistanceMeters ?: 0.0).toWorkoutDecimal())
    maximumSpeed?.let { serializer.textElement("MaximumSpeed", it.toSpeedDecimal()) }
    // TCX `Calories` is the session total; active only stands in without a total.
    val calories = workout.totalCaloriesKcal ?: workout.activeCaloriesKcal ?: 0.0
    serializer.textElement("Calories", calories.roundToInt().coerceAtLeast(0).toString())
    averageBpm?.let { serializer.heartRateElement("AverageHeartRateBpm", it) }
    maximumBpm?.let { serializer.heartRateElement("MaximumHeartRateBpm", it) }
    serializer.textElement("Intensity", "Active")
    averageBikeCadence?.let { serializer.textElement("Cadence", it.toTcxCadence().toString()) }
    serializer.textElement("TriggerMethod", "Manual")
    if (timeline.isNotEmpty()) {
        serializer.startTag(null, "Track")
        timeline.forEach { point -> serializer.trackpoint(point, isCycling) }
        serializer.endTag(null, "Track")
    }
    serializer.endTag(null, "Lap")

    // TCX has no title field; title and notes both land in `Notes`.
    val notesText = listOfNotNull(
        workout.title?.takeIf { it.isNotBlank() },
        workout.notes?.takeIf { it.isNotBlank() },
    ).joinToString("\n\n")
    if (notesText.isNotBlank()) {
        serializer.textElement("Notes", notesText)
    }

    serializer.endTag(null, "Activity")
    serializer.endTag(null, "Activities")
    serializer.endTag(null, "TrainingCenterDatabase")
    serializer.endDocument()
    serializer.flush()
}

/**
 * One header row and one value row. Machine-stable: SI units, ISO-8601
 * times, `Locale.US` decimals, English headers.
 */
internal fun writeActivityWorkoutCsv(
    workout: ExerciseData,
    heartRateSamples: List<HeartRateSample>,
    output: OutputStream,
) {
    val samples = workout.heartRateSamplesForExport(heartRateSamples)
    val columns = listOf(
        "title" to workout.title.orEmpty(),
        "activity_type" to exerciseTypeExportName(workout.exerciseType),
        "start_time" to workout.startTime.toString(),
        "end_time" to workout.endTime.toString(),
        "duration_seconds" to (workout.durationMs / 1000).toString(),
        "distance_meters" to workout.totalDistanceMeters.toCsvDecimal(),
        "elevation_gained_meters" to workout.elevationGainedMeters.toCsvDecimal(),
        "steps" to workout.steps?.toString().orEmpty(),
        "total_calories_kcal" to workout.totalCaloriesKcal.toCsvDecimal(),
        "active_calories_kcal" to workout.activeCaloriesKcal.toCsvDecimal(),
        "average_heart_rate_bpm" to workout.averageHeartRateForExport(samples)?.toString().orEmpty(),
        "max_heart_rate_bpm" to samples.maxOfOrNull { it.beatsPerMinute }?.toString().orEmpty(),
        "average_speed_meters_per_second" to workout.averageSpeedMetersPerSecond.toCsvDecimal(),
        "average_power_watts" to workout.averagePowerWatts.toCsvDecimal(),
        "average_steps_cadence_spm" to workout.averageStepsCadenceRate.toCsvDecimal(),
        "average_cycling_cadence_rpm" to workout.averageCyclingCadenceRpm.toCsvDecimal(),
        "floors_climbed" to workout.floorsClimbed?.toString().orEmpty(),
        "wheelchair_pushes" to workout.wheelchairPushes?.toString().orEmpty(),
        "source" to workout.source,
        "notes" to workout.notes.orEmpty(),
    )
    val csv = buildString {
        append(columns.joinToString(",") { (header, _) -> header })
        append("\r\n")
        append(columns.joinToString(",") { (_, value) -> value.csvEscaped() })
        append("\r\n")
    }
    output.write(csv.toByteArray(Charsets.UTF_8))
}

internal fun ExerciseData.workoutExportFileName(format: ActivityWorkoutExportFormat): String =
    exportFileName(extension = format.extension, fallbackName = "workout")

/** TCX's `Sport` is Running, Biking or Other. A treadmill run is a run. */
private fun Int.toTcxSport(): String = when (this) {
    ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
    ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL,
    -> "Running"
    ExerciseSessionRecord.EXERCISE_TYPE_BIKING,
    ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY,
    -> "Biking"
    else -> "Other"
}

/**
 * Children in the schema's order: Time, HeartRateBpm, Cadence, Extensions.
 * TCX running cadence counts one foot, as FIT does: 170 steps is 85.
 */
private fun XmlSerializer.trackpoint(point: WorkoutExportPoint, isCycling: Boolean) {
    startTag(null, "Trackpoint")
    textElement("Time", point.time.toString())
    point.heartRateBpm?.let { heartRateElement("HeartRateBpm", it) }
    val cadence = point.cadence
    if (isCycling && cadence != null) textElement("Cadence", cadence.toTcxCadence().toString())
    val runCadence = cadence?.takeUnless { isCycling }?.let { (it / 2.0).toTcxCadence() }
    val speed = point.speedMetersPerSecond
    if (speed != null || runCadence != null) {
        startTag(null, "Extensions")
        startTag(null, "$TcxExtensionPrefix:TPX")
        speed?.let { textElement("$TcxExtensionPrefix:Speed", it.toSpeedDecimal()) }
        runCadence?.let { textElement("$TcxExtensionPrefix:RunCadence", it.toString()) }
        endTag(null, "$TcxExtensionPrefix:TPX")
        endTag(null, "Extensions")
    }
    endTag(null, "Trackpoint")
}

/** `<HeartRateBpm><Value>128</Value></HeartRateBpm>` — the value is a child, not text. */
private fun XmlSerializer.heartRateElement(name: String, beatsPerMinute: Long) {
    startTag(null, name)
    textElement("Value", beatsPerMinute.coerceIn(1, 255).toString())
    endTag(null, name)
}

private fun XmlSerializer.textElement(name: String, value: String) {
    startTag(null, name)
    text(value)
    endTag(null, name)
}

/** TCX `CadenceValue_t`: a whole number up to 254. */
private fun Double.toTcxCadence(): Int = roundToInt().coerceIn(0, 254)

private fun Double.toWorkoutDecimal(): String =
    "%.1f".format(Locale.US, this)

private fun Double.toSpeedDecimal(): String =
    "%.3f".format(Locale.US, this)

private fun Double?.toCsvDecimal(): String =
    this?.let { "%.2f".format(Locale.US, it) }.orEmpty()

private fun String.csvEscaped(): String =
    if (any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
        "\"${replace("\"", "\"\"")}\""
    } else {
        this
    }

/** The machine-stable activity name for the CSV: Health Connect's constants in snake_case. */
internal fun exerciseTypeExportName(type: Int): String = when (type) {
    ExerciseSessionRecord.EXERCISE_TYPE_BADMINTON -> "badminton"
    ExerciseSessionRecord.EXERCISE_TYPE_BASEBALL -> "baseball"
    ExerciseSessionRecord.EXERCISE_TYPE_BASKETBALL -> "basketball"
    ExerciseSessionRecord.EXERCISE_TYPE_BIKING -> "biking"
    ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY -> "biking_stationary"
    ExerciseSessionRecord.EXERCISE_TYPE_BOOT_CAMP -> "boot_camp"
    ExerciseSessionRecord.EXERCISE_TYPE_BOXING -> "boxing"
    ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS -> "calisthenics"
    ExerciseSessionRecord.EXERCISE_TYPE_CRICKET -> "cricket"
    ExerciseSessionRecord.EXERCISE_TYPE_DANCING -> "dancing"
    ExerciseSessionRecord.EXERCISE_TYPE_ELLIPTICAL -> "elliptical"
    ExerciseSessionRecord.EXERCISE_TYPE_EXERCISE_CLASS -> "exercise_class"
    ExerciseSessionRecord.EXERCISE_TYPE_FENCING -> "fencing"
    ExerciseSessionRecord.EXERCISE_TYPE_FOOTBALL_AMERICAN -> "football_american"
    ExerciseSessionRecord.EXERCISE_TYPE_FOOTBALL_AUSTRALIAN -> "football_australian"
    ExerciseSessionRecord.EXERCISE_TYPE_FRISBEE_DISC -> "frisbee_disc"
    ExerciseSessionRecord.EXERCISE_TYPE_GOLF -> "golf"
    ExerciseSessionRecord.EXERCISE_TYPE_GUIDED_BREATHING -> "guided_breathing"
    ExerciseSessionRecord.EXERCISE_TYPE_GYMNASTICS -> "gymnastics"
    ExerciseSessionRecord.EXERCISE_TYPE_HANDBALL -> "handball"
    ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING -> "high_intensity_interval_training"
    ExerciseSessionRecord.EXERCISE_TYPE_HIKING -> "hiking"
    ExerciseSessionRecord.EXERCISE_TYPE_ICE_HOCKEY -> "ice_hockey"
    ExerciseSessionRecord.EXERCISE_TYPE_ICE_SKATING -> "ice_skating"
    ExerciseSessionRecord.EXERCISE_TYPE_MARTIAL_ARTS -> "martial_arts"
    ExerciseSessionRecord.EXERCISE_TYPE_PADDLING -> "paddling"
    ExerciseSessionRecord.EXERCISE_TYPE_PARAGLIDING -> "paragliding"
    ExerciseSessionRecord.EXERCISE_TYPE_PILATES -> "pilates"
    ExerciseSessionRecord.EXERCISE_TYPE_RACQUETBALL -> "racquetball"
    ExerciseSessionRecord.EXERCISE_TYPE_ROCK_CLIMBING -> "rock_climbing"
    ExerciseSessionRecord.EXERCISE_TYPE_ROLLER_HOCKEY -> "roller_hockey"
    ExerciseSessionRecord.EXERCISE_TYPE_ROWING -> "rowing"
    ExerciseSessionRecord.EXERCISE_TYPE_ROWING_MACHINE -> "rowing_machine"
    ExerciseSessionRecord.EXERCISE_TYPE_RUGBY -> "rugby"
    ExerciseSessionRecord.EXERCISE_TYPE_RUNNING -> "running"
    ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL -> "running_treadmill"
    ExerciseSessionRecord.EXERCISE_TYPE_SAILING -> "sailing"
    ExerciseSessionRecord.EXERCISE_TYPE_SCUBA_DIVING -> "scuba_diving"
    ExerciseSessionRecord.EXERCISE_TYPE_SKATING -> "skating"
    ExerciseSessionRecord.EXERCISE_TYPE_SKIING -> "skiing"
    ExerciseSessionRecord.EXERCISE_TYPE_SNOWBOARDING -> "snowboarding"
    ExerciseSessionRecord.EXERCISE_TYPE_SNOWSHOEING -> "snowshoeing"
    ExerciseSessionRecord.EXERCISE_TYPE_SOCCER -> "soccer"
    ExerciseSessionRecord.EXERCISE_TYPE_SOFTBALL -> "softball"
    ExerciseSessionRecord.EXERCISE_TYPE_SQUASH -> "squash"
    ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING -> "stair_climbing"
    ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING_MACHINE -> "stair_climbing_machine"
    ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING -> "strength_training"
    ExerciseSessionRecord.EXERCISE_TYPE_STRETCHING -> "stretching"
    ExerciseSessionRecord.EXERCISE_TYPE_SURFING -> "surfing"
    ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER -> "swimming_open_water"
    ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL -> "swimming_pool"
    ExerciseSessionRecord.EXERCISE_TYPE_TABLE_TENNIS -> "table_tennis"
    ExerciseSessionRecord.EXERCISE_TYPE_TENNIS -> "tennis"
    ExerciseSessionRecord.EXERCISE_TYPE_VOLLEYBALL -> "volleyball"
    ExerciseSessionRecord.EXERCISE_TYPE_WALKING -> "walking"
    ExerciseSessionRecord.EXERCISE_TYPE_WATER_POLO -> "water_polo"
    ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING -> "weightlifting"
    ExerciseSessionRecord.EXERCISE_TYPE_WHEELCHAIR -> "wheelchair"
    ExerciseSessionRecord.EXERCISE_TYPE_YOGA -> "yoga"
    else -> "workout"
}

// TCX has no IANA registration; this is the type Garmin's own tools use.
private const val TcxMimeType = "application/vnd.garmin.tcx+xml"
private const val CsvMimeType = "text/csv"
// FIT has none either; this is the de-facto ANT/Garmin type share targets match.
private const val FitMimeType = "application/vnd.ant.fit"
private const val TcxNamespace = "http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v2"
// The prefix Garmin Connect's own TCX uses; the importer matches by local name.
private const val TcxExtensionPrefix = "ns3"
private const val TcxActivityExtensionNamespace = "http://www.garmin.com/xmlschemas/ActivityExtension/v2"
internal const val WorkoutExportCacheDirectory = "workout_exports"
