package tech.mmarca.openvitals.features.imports.applehealth

import androidx.health.connect.client.records.Record
import androidx.annotation.StringRes
import java.time.Instant
import java.time.ZoneOffset
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
    val unsupportedElements: Int,
    val skippedRecords: Int,
    val failedRecords: Int,
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
    val unsupportedElements: Int = 0,
    val skippedRecords: Int = 0,
    val failedRecords: Int = 0,
) {
    val parsedElements: Int
        get() = parsedRecords + parsedWorkouts + parsedCorrelations + parsedActivitySummaries
}

enum class AppleHealthImportPhase {
    QUEUED,
    PARSING,
    WRITING,
    FINISHING,
    COMPLETE,
}

@get:StringRes
val AppleHealthImportPhase.labelRes: Int
    get() = when (this) {
        AppleHealthImportPhase.QUEUED -> R.string.settings_apple_health_import_progress_queued
        AppleHealthImportPhase.PARSING -> R.string.settings_apple_health_import_progress_parsing
        AppleHealthImportPhase.WRITING -> R.string.settings_apple_health_import_progress_writing
        AppleHealthImportPhase.FINISHING -> R.string.settings_apple_health_import_progress_finishing
        AppleHealthImportPhase.COMPLETE -> R.string.settings_apple_health_import_progress_complete
    }

data class AppleHealthImportTypeSummary(
    val appleType: String,
    val parsed: Int,
    val converted: Int,
    val imported: Int,
    val duplicateSkipped: Int,
    val unsupported: Int,
    val skipped: Int,
    val failed: Int,
)

data class AppleHealthImportDiagnostic(
    val appleType: String,
    val targetType: String?,
    val reasonCode: String,
    val timeRange: String?,
    val unit: String?,
    val value: String?,
    val detail: String,
)

internal data class AppleParsedExport(
    val records: List<AppleRecord>,
    val workouts: List<AppleWorkout>,
    val correlations: List<AppleCorrelation>,
    val parsedRecords: Int,
    val parsedWorkouts: Int,
    val parsedCorrelations: Int,
    val parsedActivitySummaries: Int,
    val parsedTypeCounts: Map<String, Int>,
)

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
