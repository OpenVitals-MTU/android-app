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
import tech.mmarca.openvitals.core.fit.FitBaseType
import tech.mmarca.openvitals.core.fit.FitEncoder
import tech.mmarca.openvitals.core.fit.FitEncoderField
import tech.mmarca.openvitals.core.fit.fitTimestamp
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.domain.model.HeartRateSample
import java.io.File
import java.io.OutputStream
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * Workout export WITHOUT the GPS route: the session's metrics — type, times,
 * distance, calories, heart rate — and nothing about where it happened.
 *
 * This is deliberately not a variant of the route export. The route export
 * exists only when a route does; this one exists for every workout, including
 * a treadmill run that never had a route and a session whose route the user
 * does not want to hand out. TCX is the interchange format built for exactly
 * that (`Position` is optional — see [TcxRouteParser]), and CSV is for
 * spreadsheets.
 */
internal enum class ActivityWorkoutExportFormat(
    val mimeType: String,
    val extension: String,
) {
    TCX(TcxMimeType, "tcx"),
    CSV(CsvMimeType, "csv"),
    FIT(FitMimeType, "fit"),
}

internal fun Context.saveActivityWorkoutExport(
    workout: ExerciseData,
    heartRateSamples: List<HeartRateSample>,
    format: ActivityWorkoutExportFormat,
    destination: Uri,
): Result<Unit> =
    runCatching {
        contentResolver.openOutputStream(destination)?.use { output ->
            writeActivityWorkoutExport(
                workout = workout,
                heartRateSamples = heartRateSamples,
                format = format,
                output = output,
            )
        } ?: error("Unable to open export destination.")
    }

/**
 * Same staging contract as [shareActivityRoute]: cache file, FileProvider URI,
 * system share sheet, no success toast — the chooser appearing IS the feedback.
 */
internal fun Context.shareActivityWorkout(
    workout: ExerciseData,
    heartRateSamples: List<HeartRateSample>,
    format: ActivityWorkoutExportFormat,
): Result<Unit> =
    runCatching {
        val exportFile = File(cacheDir, WorkoutExportCacheDirectory)
            .stageExport(workout.workoutExportFileName(format)) { output ->
                writeActivityWorkoutExport(
                    workout = workout,
                    heartRateSamples = heartRateSamples,
                    format = format,
                    output = output,
                )
            }
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
    heartRateSamples: List<HeartRateSample>,
    format: ActivityWorkoutExportFormat,
    output: OutputStream,
) {
    when (format) {
        ActivityWorkoutExportFormat.TCX -> writeActivityWorkoutTcx(workout, heartRateSamples, output)
        ActivityWorkoutExportFormat.CSV -> writeActivityWorkoutCsv(workout, heartRateSamples, output)
        ActivityWorkoutExportFormat.FIT -> writeActivityWorkoutFit(workout, heartRateSamples, output)
    }
}

/**
 * A routeless TCX: one `Lap` carrying the session totals, and a `Track` of
 * heart-rate-only trackpoints — `Position` never written, which is the whole
 * point. The app's own [TcxRouteParser] reads this back as "the indoor case:
 * no route, and a complete activity all the same", and so does everything
 * else that accepted a treadmill TCX before us.
 *
 * The session is written as a single lap even when the workout recorded laps:
 * calories and heart rate exist only as session totals here, and TCX requires
 * `Calories` on every lap — splitting totals across laps would be invented
 * data, and importers that sum laps would double it.
 *
 * [serializer] is a parameter only so a JVM unit test can supply one:
 * `android.util.Xml` is a throwing stub off-device. Production never passes it.
 */
internal fun writeActivityWorkoutTcx(
    workout: ExerciseData,
    heartRateSamples: List<HeartRateSample>,
    output: OutputStream,
    serializer: XmlSerializer = Xml.newSerializer(),
) {
    val samples = workout.heartRateSamplesForExport(heartRateSamples)
    val averageBpm = workout.averageHeartRateForExport(samples)
    val maximumBpm = samples.maxOfOrNull { it.beatsPerMinute }

    serializer.setOutput(output, Charsets.UTF_8.name())
    serializer.startDocument(Charsets.UTF_8.name(), true)
    serializer.startTag(null, "TrainingCenterDatabase")
    serializer.attribute(null, "xmlns", TcxNamespace)

    serializer.startTag(null, "Activities")
    serializer.startTag(null, "Activity")
    serializer.attribute(null, "Sport", workout.exerciseType.toTcxSport())
    serializer.textElement("Id", workout.startTime.toString())

    serializer.startTag(null, "Lap")
    serializer.attribute(null, "StartTime", workout.startTime.toString())
    serializer.textElement("TotalTimeSeconds", (workout.durationMs / 1000.0).toWorkoutDecimal())
    // `DistanceMeters` and `Calories` are REQUIRED by the TCX schema; a workout
    // without them writes zero, exactly as watches write a lap without either.
    serializer.textElement("DistanceMeters", (workout.totalDistanceMeters ?: 0.0).toWorkoutDecimal())
    // TCX `Calories` is the session total and TCX has no active-calorie field
    // (see TcxRouteParser); active fills in only when no total was recorded.
    val calories = workout.totalCaloriesKcal ?: workout.activeCaloriesKcal ?: 0.0
    serializer.textElement("Calories", calories.roundToInt().coerceAtLeast(0).toString())
    averageBpm?.let { serializer.heartRateElement("AverageHeartRateBpm", it) }
    maximumBpm?.let { serializer.heartRateElement("MaximumHeartRateBpm", it) }
    serializer.textElement("Intensity", "Active")
    serializer.textElement("TriggerMethod", "Manual")
    if (samples.isNotEmpty()) {
        serializer.startTag(null, "Track")
        samples.forEach { sample ->
            serializer.startTag(null, "Trackpoint")
            serializer.textElement("Time", sample.time.toString())
            serializer.heartRateElement("HeartRateBpm", sample.beatsPerMinute)
            serializer.endTag(null, "Trackpoint")
        }
        serializer.endTag(null, "Track")
    }
    serializer.endTag(null, "Lap")

    // TCX has no title field; `Notes` on the Activity is where every exporter
    // puts free text, so title and notes both land there.
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
 * One header row and one value row: the shape a spreadsheet pivots over when
 * several of these land in one folder. Machine-stable on purpose — SI units,
 * ISO-8601 times, `Locale.US` decimals, English headers — because a CSV that
 * follows the phone's locale breaks the moment two users share a sheet.
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

/**
 * A routeless FIT Activity file: file_id, heart-rate-only records, one lap,
 * one session, one activity message — and no position field DEFINED anywhere,
 * which is this format's whole promise. Framing (header, sentinels, CRCs) is
 * [FitEncoder]'s; this function owns the message vocabulary, the same split
 * the read side keeps between [FitDecoder] and `FitRouteParser`.
 *
 * Two field choices look asymmetric on purpose, because our own re-importer
 * reads field 21 as `total_ascent` on BOTH the session and the lap, while the
 * real FIT profile numbers ascent 22 on the session (21 there is `max_power`):
 * ascent is written as session 22 (what Garmin/Strava read) AND lap 21 (what
 * `FitRouteParser` reads — also correct FIT). For the same reason the session
 * power fields (20/21) and the lap position fields (3-6) are never written.
 *
 * Everything numeric is a scaled integer: our decoder's `fitLong` drops float
 * fields, and scaled integers are what watches write anyway.
 */
internal fun writeActivityWorkoutFit(
    workout: ExerciseData,
    heartRateSamples: List<HeartRateSample>,
    output: OutputStream,
) {
    val samples = workout.heartRateSamplesForExport(heartRateSamples)
    val averageBpm = workout.averageHeartRateForExport(samples)?.coerceIn(1, 255)
    val maximumBpm = samples.maxOfOrNull { it.beatsPerMinute }?.coerceIn(1, 255)
    val startTimestamp = fitTimestamp(workout.startTime)
    val endTimestamp = fitTimestamp(workout.endTime)
    val (sport, subSport) = workout.exerciseType.toFitSport()
    val durationScaled = workout.durationMs // total_elapsed/timer_time carry ms directly (scale 1000)
    val distanceScaled = workout.totalDistanceMeters?.let { (it * FitDistanceScale).roundToLong() }
    val calories = (workout.totalCaloriesKcal ?: workout.activeCaloriesKcal)
        ?.roundToLong()
        ?.coerceIn(0, 0xFFFE)
    val ascentMeters = workout.elevationGainedMeters?.roundToLong()?.coerceIn(0, 0xFFFE)
    val title = workout.title?.takeIf { it.isNotBlank() }

    val encoder = FitEncoder()

    encoder.defineMessage(
        FitLocalFileId, FitFileIdMessage,
        listOf(
            FitEncoderField(FitFileIdTypeField, FitBaseType.ENUM),
            FitEncoderField(FitFileIdManufacturerField, FitBaseType.UINT16),
            FitEncoderField(FitFileIdProductField, FitBaseType.UINT16),
            FitEncoderField(FitFileIdTimeCreatedField, FitBaseType.UINT32),
        ),
    )
    encoder.writeMessage(
        FitLocalFileId,
        mapOf(
            FitFileIdTypeField to FitFileTypeActivity,
            FitFileIdManufacturerField to FitManufacturerDevelopment,
            FitFileIdProductField to 1L,
            FitFileIdTimeCreatedField to startTimestamp,
        ),
    )

    if (samples.isNotEmpty()) {
        encoder.defineMessage(
            FitLocalRecord, FitRecordMessage,
            listOf(
                FitEncoderField(FitTimestampField, FitBaseType.UINT32),
                FitEncoderField(FitRecordHeartRateField, FitBaseType.UINT8),
            ),
        )
        samples.forEach { sample ->
            encoder.writeMessage(
                FitLocalRecord,
                mapOf(
                    FitTimestampField to fitTimestamp(sample.time),
                    FitRecordHeartRateField to sample.beatsPerMinute,
                ),
            )
        }
    }

    encoder.defineMessage(
        FitLocalLap, FitLapMessage,
        listOf(
            FitEncoderField(FitTimestampField, FitBaseType.UINT32),
            FitEncoderField(FitMessageIndexField, FitBaseType.UINT16),
            FitEncoderField(FitEventField, FitBaseType.ENUM),
            FitEncoderField(FitEventTypeField, FitBaseType.ENUM),
            FitEncoderField(FitStartTimeField, FitBaseType.UINT32),
            FitEncoderField(FitTotalElapsedTimeField, FitBaseType.UINT32),
            FitEncoderField(FitTotalTimerTimeField, FitBaseType.UINT32),
            FitEncoderField(FitTotalDistanceField, FitBaseType.UINT32),
            FitEncoderField(FitTotalCaloriesField, FitBaseType.UINT16),
            FitEncoderField(FitLapAvgHeartRateField, FitBaseType.UINT8),
            FitEncoderField(FitLapMaxHeartRateField, FitBaseType.UINT8),
            FitEncoderField(FitLapTotalAscentField, FitBaseType.UINT16),
        ),
    )
    encoder.writeMessage(
        FitLocalLap,
        mapOf(
            FitTimestampField to endTimestamp,
            FitMessageIndexField to 0L,
            FitEventField to FitEventLap,
            FitEventTypeField to FitEventTypeStop,
            FitStartTimeField to startTimestamp,
            FitTotalElapsedTimeField to durationScaled,
            FitTotalTimerTimeField to durationScaled,
            FitTotalDistanceField to distanceScaled,
            FitTotalCaloriesField to calories,
            FitLapAvgHeartRateField to averageBpm,
            FitLapMaxHeartRateField to maximumBpm,
            FitLapTotalAscentField to ascentMeters,
        ),
    )

    encoder.defineMessage(
        FitLocalSession, FitSessionMessage,
        listOf(
            FitEncoderField(FitTimestampField, FitBaseType.UINT32),
            FitEncoderField(FitMessageIndexField, FitBaseType.UINT16),
            FitEncoderField(FitEventField, FitBaseType.ENUM),
            FitEncoderField(FitEventTypeField, FitBaseType.ENUM),
            FitEncoderField(FitStartTimeField, FitBaseType.UINT32),
            FitEncoderField(FitSessionSportField, FitBaseType.ENUM),
            FitEncoderField(FitSessionSubSportField, FitBaseType.ENUM),
            FitEncoderField(FitTotalElapsedTimeField, FitBaseType.UINT32),
            FitEncoderField(FitTotalTimerTimeField, FitBaseType.UINT32),
            FitEncoderField(FitTotalDistanceField, FitBaseType.UINT32),
            FitEncoderField(FitTotalCaloriesField, FitBaseType.UINT16),
            FitEncoderField(FitSessionAvgHeartRateField, FitBaseType.UINT8),
            FitEncoderField(FitSessionMaxHeartRateField, FitBaseType.UINT8),
            FitEncoderField(FitSessionTotalAscentField, FitBaseType.UINT16),
            FitEncoderField(FitSessionFirstLapIndexField, FitBaseType.UINT16),
            FitEncoderField(FitSessionNumLapsField, FitBaseType.UINT16),
        ),
    )
    encoder.writeMessage(
        FitLocalSession,
        mapOf(
            FitTimestampField to endTimestamp,
            FitMessageIndexField to 0L,
            FitEventField to FitEventSession,
            FitEventTypeField to FitEventTypeStop,
            FitStartTimeField to startTimestamp,
            FitSessionSportField to sport,
            FitSessionSubSportField to subSport,
            FitTotalElapsedTimeField to durationScaled,
            FitTotalTimerTimeField to durationScaled,
            FitTotalDistanceField to distanceScaled,
            FitTotalCaloriesField to calories,
            FitSessionAvgHeartRateField to averageBpm,
            FitSessionMaxHeartRateField to maximumBpm,
            FitSessionTotalAscentField to ascentMeters,
            FitSessionFirstLapIndexField to 0L,
            FitSessionNumLapsField to 1L,
        ),
    )

    val activityFields = mutableListOf(
        FitEncoderField(FitTimestampField, FitBaseType.UINT32),
        FitEncoderField(FitActivityTotalTimerTimeField, FitBaseType.UINT32),
        FitEncoderField(FitActivityNumSessionsField, FitBaseType.UINT16),
        FitEncoderField(FitActivityTypeField, FitBaseType.ENUM),
        FitEncoderField(FitActivityEventField, FitBaseType.ENUM),
        FitEncoderField(FitActivityEventTypeField, FitBaseType.ENUM),
    )
    if (title != null) {
        // FIT strings are fixed-size; size the field to this title, capped.
        val titleSize = minOf(title.toByteArray(Charsets.UTF_8).size + 1, MaxFitActivityNameBytes)
        activityFields += FitEncoderField(FitActivityNameField, FitBaseType.STRING, titleSize)
    }
    encoder.defineMessage(FitLocalActivity, FitActivityMessage, activityFields)
    encoder.writeMessage(
        FitLocalActivity,
        values = mapOf(
            FitTimestampField to endTimestamp,
            FitActivityTotalTimerTimeField to durationScaled,
            FitActivityNumSessionsField to 1L,
            FitActivityTypeField to FitActivityTypeManual,
            FitActivityEventField to FitEventActivity,
            FitActivityEventTypeField to FitEventTypeStop,
        ),
        strings = if (title != null) mapOf(FitActivityNameField to title) else emptyMap(),
    )

    encoder.writeTo(output)
}

internal fun ExerciseData.workoutExportFileName(format: ActivityWorkoutExportFormat): String =
    exportFileName(extension = format.extension, fallbackName = "workout")

/**
 * The samples worth exporting: within the session, time-ordered, and inside
 * TCX's `unsignedByte` range — a 0 bpm is a sensor dropout, not a heart.
 */
private fun ExerciseData.heartRateSamplesForExport(
    heartRateSamples: List<HeartRateSample>,
): List<HeartRateSample> =
    heartRateSamples
        .filter { it.beatsPerMinute in 1..255 }
        .filter { !it.time.isBefore(startTime) && !it.time.isAfter(endTime) }
        .sortedBy { it.time }

/** The recorded average wins; a computed one only stands in when there is none. */
private fun ExerciseData.averageHeartRateForExport(
    heartRateSamples: List<HeartRateSample>,
): Long? =
    averageHeartRateBpm
        ?: heartRateSamples
            .takeIf { it.isNotEmpty() }
            ?.map { it.beatsPerMinute }
            ?.average()
            ?.roundToLong()

/**
 * TCX's `Sport` vocabulary is three words wide — Running, Biking, Other — and
 * a treadmill run is still a run (the import side documents the same reading).
 */
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
 * Health Connect exercise type → FIT `(sport, sub_sport)`. The sub-sport is
 * only set where it IS the activity (a treadmill run, a trainer ride) or where
 * FIT files the sport under a generic bucket (fitness equipment, training) and
 * the sub-sport carries the identity. Everything unmapped is (generic, generic),
 * which importers treat as "a workout" — the same fallback the CSV takes.
 */
private fun Int.toFitSport(): Pair<Long, Long> = when (this) {
    ExerciseSessionRecord.EXERCISE_TYPE_RUNNING -> 1L to 0L
    ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL -> 1L to 1L
    ExerciseSessionRecord.EXERCISE_TYPE_BIKING -> 2L to 0L
    ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY -> 2L to 6L
    ExerciseSessionRecord.EXERCISE_TYPE_WALKING -> 11L to 0L
    ExerciseSessionRecord.EXERCISE_TYPE_HIKING -> 17L to 0L
    ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL -> 5L to 17L
    ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER -> 5L to 18L
    ExerciseSessionRecord.EXERCISE_TYPE_ROWING -> 15L to 0L
    ExerciseSessionRecord.EXERCISE_TYPE_ROWING_MACHINE -> 15L to 14L
    ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
    ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING,
    ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS,
    -> 10L to 20L
    ExerciseSessionRecord.EXERCISE_TYPE_ELLIPTICAL -> 4L to 15L
    ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING,
    ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING_MACHINE,
    -> 4L to 16L
    ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING -> 62L to 0L
    ExerciseSessionRecord.EXERCISE_TYPE_BASKETBALL -> 6L to 0L
    ExerciseSessionRecord.EXERCISE_TYPE_SOCCER -> 7L to 0L
    ExerciseSessionRecord.EXERCISE_TYPE_TENNIS -> 8L to 0L
    ExerciseSessionRecord.EXERCISE_TYPE_FOOTBALL_AMERICAN -> 9L to 0L
    ExerciseSessionRecord.EXERCISE_TYPE_SKIING -> 13L to 0L
    ExerciseSessionRecord.EXERCISE_TYPE_SNOWBOARDING -> 14L to 0L
    ExerciseSessionRecord.EXERCISE_TYPE_PADDLING -> 19L to 0L
    ExerciseSessionRecord.EXERCISE_TYPE_GOLF -> 25L to 0L
    ExerciseSessionRecord.EXERCISE_TYPE_ROCK_CLIMBING -> 31L to 0L
    ExerciseSessionRecord.EXERCISE_TYPE_SAILING -> 32L to 0L
    ExerciseSessionRecord.EXERCISE_TYPE_ICE_SKATING,
    ExerciseSessionRecord.EXERCISE_TYPE_SKATING,
    -> 33L to 0L
    ExerciseSessionRecord.EXERCISE_TYPE_SNOWSHOEING -> 35L to 0L
    ExerciseSessionRecord.EXERCISE_TYPE_SURFING -> 38L to 0L
    ExerciseSessionRecord.EXERCISE_TYPE_BOXING -> 47L to 0L
    else -> 0L to 0L
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

private fun Double.toWorkoutDecimal(): String =
    "%.1f".format(Locale.US, this)

private fun Double?.toCsvDecimal(): String =
    this?.let { "%.2f".format(Locale.US, it) }.orEmpty()

private fun String.csvEscaped(): String =
    if (any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
        "\"${replace("\"", "\"\"")}\""
    } else {
        this
    }

/**
 * The machine-stable activity name for the CSV: Health Connect's own constant
 * vocabulary in snake_case, never the localized label — a CSV that follows the
 * phone's language breaks the moment two users share a sheet. Covers the same
 * types as [exerciseTypeLabelRes], and falls back the same way.
 */
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
internal const val WorkoutExportCacheDirectory = "workout_exports"

// The FIT vocabulary this exporter writes. Message and field numbers are from
// Garmin's FIT profile; only what the writer emits is named here — the read
// side keeps its own table in FitRouteParser, deliberately (interpretation
// stays with each consumer).
private const val FitLocalFileId = 0
private const val FitLocalRecord = 1
private const val FitLocalLap = 2
private const val FitLocalSession = 3
private const val FitLocalActivity = 4
private const val FitFileIdMessage = 0
private const val FitSessionMessage = 18
private const val FitLapMessage = 19
private const val FitRecordMessage = 20
private const val FitActivityMessage = 34
private const val FitTimestampField = 253
private const val FitMessageIndexField = 254
private const val FitFileIdTypeField = 0
private const val FitFileIdManufacturerField = 1
private const val FitFileIdProductField = 2
private const val FitFileIdTimeCreatedField = 4
private const val FitRecordHeartRateField = 3
private const val FitEventField = 0
private const val FitEventTypeField = 1
private const val FitStartTimeField = 2
private const val FitSessionSportField = 5
private const val FitSessionSubSportField = 6
private const val FitTotalElapsedTimeField = 7
private const val FitTotalTimerTimeField = 8
private const val FitTotalDistanceField = 9
private const val FitTotalCaloriesField = 11
private const val FitLapAvgHeartRateField = 15
private const val FitLapMaxHeartRateField = 16
/** Lap numbering; also what our own importer reads on BOTH messages. */
private const val FitLapTotalAscentField = 21
private const val FitSessionAvgHeartRateField = 16
private const val FitSessionMaxHeartRateField = 17
/** Session numbering — 21 there is max_power and must never be written. */
private const val FitSessionTotalAscentField = 22
private const val FitSessionFirstLapIndexField = 25
private const val FitSessionNumLapsField = 26
private const val FitActivityTotalTimerTimeField = 0
private const val FitActivityNumSessionsField = 1
private const val FitActivityTypeField = 2
private const val FitActivityEventField = 3
private const val FitActivityEventTypeField = 4
private const val FitActivityNameField = 8
private const val FitFileTypeActivity = 4L
private const val FitManufacturerDevelopment = 255L
private const val FitEventLap = 9L
private const val FitEventSession = 8L
private const val FitEventActivity = 26L
private const val FitEventTypeStop = 1L
private const val FitActivityTypeManual = 0L
private const val FitDistanceScale = 100.0
private const val MaxFitActivityNameBytes = 64
