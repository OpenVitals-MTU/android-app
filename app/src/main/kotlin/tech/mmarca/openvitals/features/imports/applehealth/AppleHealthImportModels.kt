package tech.mmarca.openvitals.features.imports.applehealth

import androidx.health.connect.client.records.Record
import androidx.annotation.StringRes
import java.time.Instant
import java.time.ZoneOffset
import kotlin.math.roundToInt
import kotlin.reflect.KClass
import tech.mmarca.openvitals.R

data class AppleHealthImportResult(
    val parsedRecords: Int,
    val parsedWorkouts: Int,
    val parsedCorrelations: Int,
    val parsedActivitySummaries: Int,
    val convertedRecords: Int,
    val importedRecords: Int,
    val duplicateSkippedRecords: Int,
    val notSelectedRecords: Int,
    val unsupportedElements: Int,
    val skippedRecords: Int,
    val failedRecords: Int,
    val workoutRoutesIncomplete: Boolean = false,
    val typeSummaries: List<AppleHealthImportTypeSummary>,
    val diagnostics: List<AppleHealthImportDiagnostic>,
    val shareableReportText: String,
) {
    val unsupportedRecords: Int get() = unsupportedElements
}

data class AppleHealthImportProgress(
    val phase: AppleHealthImportPhase = AppleHealthImportPhase.QUEUED,
    val parsedRecords: Int = 0,
    val parsedWorkouts: Int = 0,
    val parsedCorrelations: Int = 0,
    val parsedActivitySummaries: Int = 0,
    val convertedRecords: Int = 0,
    val importedRecords: Int = 0,
    val duplicateSkippedRecords: Int = 0,
    val notSelectedRecords: Int = 0,
    val unsupportedElements: Int = 0,
    val skippedRecords: Int = 0,
    val failedRecords: Int = 0,
    val expectedSelectedRecords: Int = 0,
) {
    val parsedElements: Int
        get() = parsedRecords + parsedWorkouts + parsedCorrelations + parsedActivitySummaries

    val selectedPreparedRecords: Int
        get() = (convertedRecords - notSelectedRecords).coerceAtLeast(0)

    val percent: Int?
        get() {
            val total = expectedSelectedRecords.takeIf { it > 0 } ?: return null
            if (phase == AppleHealthImportPhase.COMPLETE) return 100
            val selectedProgress = selectedPreparedRecords.coerceAtMost(total)
            val selectedPercent = (selectedProgress.toDouble() / total * SelectedRecordsPercentCeiling).roundToInt()
            val phaseFloor = when (phase) {
                AppleHealthImportPhase.QUEUED,
                AppleHealthImportPhase.PARSING,
                AppleHealthImportPhase.CONVERTING,
                -> 0
                AppleHealthImportPhase.CHECKING_DUPLICATES -> if (selectedProgress >= total) 88 else 0
                AppleHealthImportPhase.WRITING -> if (selectedProgress >= total) 92 else 0
                AppleHealthImportPhase.FINISHING -> 95
                AppleHealthImportPhase.BUILDING_REPORT -> 98
                AppleHealthImportPhase.COMPLETE -> 100
            }
            return maxOf(selectedPercent, phaseFloor).coerceIn(0, 99)
        }
}

private const val SelectedRecordsPercentCeiling = 88

enum class AppleHealthImportPhase {
    QUEUED,
    PARSING,
    CONVERTING,
    CHECKING_DUPLICATES,
    WRITING,
    FINISHING,
    BUILDING_REPORT,
    COMPLETE,
}

@get:StringRes
val AppleHealthImportPhase.labelRes: Int
    get() = when (this) {
        AppleHealthImportPhase.QUEUED -> R.string.settings_apple_health_import_progress_queued
        AppleHealthImportPhase.PARSING -> R.string.settings_apple_health_import_progress_parsing
        AppleHealthImportPhase.CONVERTING -> R.string.settings_apple_health_import_progress_converting
        AppleHealthImportPhase.CHECKING_DUPLICATES -> R.string.settings_apple_health_import_progress_checking_duplicates
        AppleHealthImportPhase.WRITING -> R.string.settings_apple_health_import_progress_writing
        AppleHealthImportPhase.FINISHING -> R.string.settings_apple_health_import_progress_finishing
        AppleHealthImportPhase.BUILDING_REPORT -> R.string.settings_apple_health_import_progress_building_report
        AppleHealthImportPhase.COMPLETE -> R.string.settings_apple_health_import_progress_complete
    }

data class AppleHealthImportTypeSummary(
    val appleType: String,
    val parsed: Int,
    val converted: Int,
    val imported: Int,
    val duplicateSkipped: Int,
    val notSelected: Int,
    val unsupported: Int,
    val skipped: Int,
    val failed: Int,
)

data class AppleHealthImportAnalysisResult(
    val parsedRecords: Int,
    val parsedWorkouts: Int,
    val parsedCorrelations: Int,
    val parsedActivitySummaries: Int,
    val convertedRecords: Int,
    val unsupportedElements: Int,
    val skippedRecords: Int,
    val failedRecords: Int,
    val categorySummaries: List<AppleHealthImportCategorySummary>,
    val typeSummaries: List<AppleHealthImportTypeSummary>,
    val diagnostics: List<AppleHealthImportDiagnostic>,
    val shareableReportText: String,
) {
    val parsedElements: Int
        get() = parsedRecords + parsedWorkouts + parsedCorrelations + parsedActivitySummaries
}

data class AppleHealthImportCategorySummary(
    val category: AppleHealthImportCategory,
    val convertedRecords: Int,
    val routeSessions: Int = 0,
)

data class AppleHealthExportFingerprint(
    val displayName: String?,
    val size: Long?,
) {
    fun isIdentifiable(): Boolean = displayName != null || size != null
}

enum class AppleHealthImportCategory(
    @param:StringRes val titleRes: Int,
    @param:StringRes val descriptionRes: Int,
) {
    WORKOUTS(
        R.string.settings_apple_health_import_category_workouts,
        R.string.settings_apple_health_import_category_workouts_desc,
    ),
    ACTIVITY(
        R.string.settings_apple_health_import_category_activity,
        R.string.settings_apple_health_import_category_activity_desc,
    ),
    HEART(
        R.string.settings_apple_health_import_category_heart,
        R.string.settings_apple_health_import_category_heart_desc,
    ),
    SLEEP(
        R.string.settings_apple_health_import_category_sleep,
        R.string.settings_apple_health_import_category_sleep_desc,
    ),
    BODY(
        R.string.settings_apple_health_import_category_body,
        R.string.settings_apple_health_import_category_body_desc,
    ),
    VITALS(
        R.string.settings_apple_health_import_category_vitals,
        R.string.settings_apple_health_import_category_vitals_desc,
    ),
    NUTRITION(
        R.string.settings_apple_health_import_category_nutrition,
        R.string.settings_apple_health_import_category_nutrition_desc,
    ),
    HYDRATION(
        R.string.settings_apple_health_import_category_hydration,
        R.string.settings_apple_health_import_category_hydration_desc,
    ),
    MINDFULNESS(
        R.string.settings_apple_health_import_category_mindfulness,
        R.string.settings_apple_health_import_category_mindfulness_desc,
    ),
    CYCLE(
        R.string.settings_apple_health_import_category_cycle,
        R.string.settings_apple_health_import_category_cycle_desc,
    ),
}

data class AppleHealthImportDiagnostic(
    val appleType: String,
    val targetType: String?,
    val reasonCode: String,
    val timeRange: String?,
    val unit: String?,
    val value: String?,
    val detail: String,
)

internal data class AppleHealthImportDiagnosticSummary(
    val appleType: String,
    val targetType: String?,
    val reasonCode: String,
    val detail: String,
    val count: Int,
    val exampleTimeRange: String?,
    val exampleUnit: String?,
    val exampleValue: String?,
)

internal data class AppleHealthDiagnosticSummaryKey(
    val appleType: String,
    val targetType: String?,
    val reasonCode: String,
    val detail: String,
)

internal data class MutableAppleHealthImportDiagnosticSummary(
    val appleType: String,
    val targetType: String?,
    val reasonCode: String,
    val detail: String,
    var count: Int,
    val exampleTimeRange: String?,
    val exampleUnit: String?,
    val exampleValue: String?,
) {
    fun toSummary(): AppleHealthImportDiagnosticSummary =
        AppleHealthImportDiagnosticSummary(
            appleType = appleType,
            targetType = targetType,
            reasonCode = reasonCode,
            detail = detail,
            count = count,
            exampleTimeRange = exampleTimeRange,
            exampleUnit = exampleUnit,
            exampleValue = exampleValue,
        )
}

internal fun MutableMap<AppleHealthDiagnosticSummaryKey, MutableAppleHealthImportDiagnosticSummary>.add(
    diagnostic: AppleHealthImportDiagnostic,
) {
    val key = AppleHealthDiagnosticSummaryKey(
        appleType = diagnostic.appleType,
        targetType = diagnostic.targetType,
        reasonCode = diagnostic.reasonCode,
        detail = diagnostic.detail,
    )
    val existing = this[key]
    if (existing != null) {
        existing.count += 1
    } else {
        this[key] = MutableAppleHealthImportDiagnosticSummary(
            appleType = diagnostic.appleType,
            targetType = diagnostic.targetType,
            reasonCode = diagnostic.reasonCode,
            detail = diagnostic.detail,
            count = 1,
            exampleTimeRange = diagnostic.timeRange,
            exampleUnit = diagnostic.unit,
            exampleValue = diagnostic.value,
        )
    }
}

internal data class AppleParsedExport(
    val records: List<AppleRecord>,
    val workouts: List<AppleWorkout>,
    val correlations: List<AppleCorrelation>,
    val parsedRecords: Int,
    val parsedWorkouts: Int,
    val parsedCorrelations: Int,
    val parsedActivitySummaries: Int,
    val parsedTypeCounts: Map<String, Int>,
    /** Raw control characters removed from export.xml because XML 1.0 forbids them as literal text. */
    val sanitizedControlChars: Int = 0,
    /** Bare `&` characters in export.xml that were auto-escaped to `&amp;` because they weren't part of an entity reference. */
    val sanitizedAmpersands: Int = 0,
    /** A truncated workout-route entry was ignored after export.xml had already been read intact. */
    val workoutRouteArchiveFailure: AppleWorkoutRouteArchiveFailure? = null,
)

internal data class AppleWorkoutRouteArchiveFailure(
    val entryName: String,
    val decompressedBytesRead: Long?,
) {
    val detail: String
        get() = buildString {
            append("The ZIP ended unexpectedly while reading ")
            append(entryName)
            if (decompressedBytesRead != null) {
                append(" after ")
                append(decompressedBytesRead)
                append(" decompressed byte(s)")
            }
            append(". Health records were imported from the intact export.xml, but this route and any remaining ZIP entries were unavailable.")
        }
}

internal data class AppleRecord(
    val type: String,
    val sourceName: String?,
    val sourceVersion: String?,
    val device: String?,
    val unit: String?,
    val creationDate: AppleDateTime?,
    val startDate: AppleDateTime?,
    val endDate: AppleDateTime?,
    val rawValue: String?,
    val numericValue: Double?,
    val metadata: Map<String, String>,
    val correlationType: String? = null,
) {
    val valueForReport: String?
        get() = rawValue?.take(80)
}

internal data class AppleWorkout(
    val workoutActivityType: String,
    val sourceName: String?,
    val sourceVersion: String?,
    val device: String?,
    val creationDate: AppleDateTime?,
    val startDate: AppleDateTime?,
    val endDate: AppleDateTime?,
    val duration: Double?,
    val durationUnit: String?,
    val totalDistance: Double?,
    val totalDistanceUnit: String?,
    val totalEnergyBurned: Double?,
    val totalEnergyBurnedUnit: String?,
    val metadata: Map<String, String>,
    val events: List<AppleWorkoutEvent>,
    val routes: List<AppleWorkoutRouteFile> = emptyList(),
    val routeReferences: Int = 0,
    val routeReferencePaths: List<String> = emptyList(),
) {
    val unavailableRoutePaths: List<String>
        get() {
            val availablePaths = routes.mapTo(hashSetOf()) { it.path }
            return routeReferencePaths.filterNot(availablePaths::contains)
        }
}

internal data class AppleWorkoutRouteFile(
    val path: String,
    val points: List<AppleWorkoutRoutePoint>,
)

internal data class AppleWorkoutRoutePoint(
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double?,
    val horizontalAccuracyMeters: Double?,
    val verticalAccuracyMeters: Double?,
)

internal data class AppleWorkoutEvent(
    val type: String?,
    val date: AppleDateTime?,
    val duration: Double?,
    val durationUnit: String?,
)

internal data class AppleCorrelation(
    val type: String,
    val sourceName: String?,
    val sourceVersion: String?,
    val device: String?,
    val creationDate: AppleDateTime?,
    val startDate: AppleDateTime?,
    val endDate: AppleDateTime?,
    val metadata: Map<String, String>,
    val records: List<AppleRecord>,
)

internal data class AppleDateTime(
    val instant: Instant,
    val offset: ZoneOffset?,
) {
    override fun toString(): String = instant.toString()
}

internal data class ConvertedAppleRecord(
    val appleType: String,
    val targetType: String,
    val fingerprint: String,
    val recordType: KClass<out Record>,
    val record: Record,
    val sourceTimeRange: AppleImportTimeRange,
    val unit: String?,
    val value: String?,
)

internal data class AppleImportTimeRange(
    val start: Instant,
    val end: Instant,
) {
    override fun toString(): String =
        if (start == end) start.toString() else "$start..$end"
}
