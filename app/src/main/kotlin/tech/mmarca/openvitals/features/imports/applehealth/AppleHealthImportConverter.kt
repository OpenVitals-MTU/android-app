package tech.mmarca.openvitals.features.imports.applehealth

internal data class AppleHealthConversionResult(
    val converted: List<ConvertedAppleRecord>,
    val diagnostics: List<AppleHealthImportDiagnostic>,
    val typeStats: MutableMap<String, MutableAppleImportTypeStats>,
)

internal data class MutableAppleImportTypeStats(
    var parsed: Int = 0,
    var converted: Int = 0,
    var imported: Int = 0,
    var duplicateSkipped: Int = 0,
    var unsupported: Int = 0,
    var skipped: Int = 0,
    var failed: Int = 0,
)

internal class AppleHealthImportConverter(
    internal val mindfulnessAvailable: Boolean,
    private val diagnosticLimit: Int = 200,
) {
    private val diagnostics = mutableListOf<AppleHealthImportDiagnostic>()
    private val diagnosticSummaries = linkedMapOf<AppleHealthDiagnosticSummaryKey, MutableAppleHealthImportDiagnosticSummary>()
    internal val typeStats = linkedMapOf<String, MutableAppleImportTypeStats>()
    internal val consumedRecordFingerprints = mutableSetOf<String>()
    private val workoutOverlapCandidates = mutableListOf<AppleWorkoutOverlapCandidate>()
    private var workoutOverlapCandidatesLimitReached = false

    fun convert(export: AppleParsedExport): AppleHealthConversionResult {
        export.parsedTypeCounts.forEach { (type, count) ->
            typeStats.getOrPut(type) { MutableAppleImportTypeStats() }.parsed += count
        }

        val workoutOverlaps = export.records.toBoundedWorkoutOverlapCandidates()
        if (workoutOverlaps.limitReached) {
            workoutOverlapCandidatesLimitReached = true
        }

        val converted = buildList {
            addAll(convertBloodPressureCorrelations(export.correlations))
            addAll(convertStandaloneBloodPressure(export.records))
            addAll(convertSleep(export.records))
            addAll(convertNutrition(export.records))
            addAll(convertWorkouts(export.workouts, workoutOverlaps.candidates, workoutOverlapCandidatesLimitReached))
            export.records.forEach { record ->
                if (record.sourceFingerprint in consumedRecordFingerprints) return@forEach
                convertSingleRecord(record)?.let(::add)
            }
            export.correlations
                .filterNot { it.type == AppleBloodPressureCorrelation }
                .forEach { correlation ->
                    unsupported(
                        appleType = correlation.type,
                        detail = "Correlation type has no direct Health Connect import mapping.",
                        timeRange = correlation.timeRangeOrNull()?.toString(),
                    )
                }
            if (export.parsedActivitySummaries > 0) {
                unsupported(
                    appleType = "ActivitySummary",
                    detail = "Apple activity rings and stand hours have no direct writable Health Connect record.",
                    timeRange = null,
                )
            }
        }

        return AppleHealthConversionResult(
            converted = converted,
            diagnostics = diagnostics.toList(),
            typeStats = typeStats,
        )
    }

    fun markParsed(appleType: String) {
        typeStats.getOrPut(appleType) { MutableAppleImportTypeStats() }.parsed += 1
    }

    fun diagnosticsSnapshot(): List<AppleHealthImportDiagnostic> = diagnostics.toList()

    fun diagnosticSummariesSnapshot(): List<AppleHealthImportDiagnosticSummary> =
        diagnosticSummaries.values.map { it.toSummary() }

    fun shouldBufferRecord(record: AppleRecord): Boolean =
        record.type == AppleBloodPressureSystolic ||
            record.type == AppleBloodPressureDiastolic ||
            record.type == AppleSleepAnalysis ||
            (record.type in AppleNutritionTypes && record.type != AppleDietaryWater)

    fun noteWorkoutOverlap(record: AppleRecord) {
        record.toWorkoutOverlapCandidate()?.let { candidate ->
            if (workoutOverlapCandidates.size < MaxWorkoutOverlapCandidates) {
                workoutOverlapCandidates += candidate
            } else {
                workoutOverlapCandidatesLimitReached = true
            }
        }
    }

    fun convertStreamingRecord(record: AppleRecord): ConvertedAppleRecord? =
        convertSingleRecord(record)

    fun convertBufferedGroups(
        records: List<AppleRecord>,
        workouts: List<AppleWorkout>,
        correlations: List<AppleCorrelation>,
        parsedActivitySummaries: Int,
    ): List<ConvertedAppleRecord> =
        buildList {
            addAll(convertBloodPressureCorrelations(correlations))
            addAll(convertStandaloneBloodPressure(records))
            addAll(convertSleep(records, trackConsumedRecords = false))
            addAll(convertNutrition(records, trackConsumedRecords = false))
            addAll(convertWorkouts(workouts, workoutOverlapCandidates, workoutOverlapCandidatesLimitReached))
            correlations
                .filterNot { it.type == AppleBloodPressureCorrelation }
                .forEach { correlation ->
                    unsupported(
                        appleType = correlation.type,
                        detail = "Correlation type has no direct Health Connect import mapping.",
                        timeRange = correlation.timeRangeOrNull()?.toString(),
                    )
                }
            if (parsedActivitySummaries > 0) {
                unsupported(
                    appleType = "ActivitySummary",
                    detail = "Apple activity rings and stand hours have no direct writable Health Connect record.",
                    timeRange = null,
                )
            }
        }

    internal fun invalid(record: AppleRecord, detail: String): Nothing? =
        invalid(record.type, detail, record.timeRangeOrNull()?.toString(), record.unit, record.valueForReport)

    internal fun invalid(
        appleType: String,
        detail: String,
        timeRange: String?,
        unit: String? = null,
        value: String? = null,
    ): Nothing? {
        addDiagnostic(AppleHealthImportDiagnostic(appleType, null, "invalid", timeRange, unit, value, detail))
        typeStats.getOrPut(appleType) { MutableAppleImportTypeStats() }.failed += 1
        return null
    }

    internal fun unsupportedNull(record: AppleRecord, detail: String): Nothing? =
        unsupported(record.type, detail, record.timeRangeOrNull()?.toString(), record.unit, record.valueForReport)

    private fun unsupported(
        appleType: String,
        detail: String,
        timeRange: String?,
        unit: String? = null,
        value: String? = null,
    ): Nothing? {
        addDiagnostic(AppleHealthImportDiagnostic(appleType, null, "unsupported", timeRange, unit, value, detail))
        typeStats.getOrPut(appleType) { MutableAppleImportTypeStats() }.unsupported += 1
        return null
    }

    internal fun skippedNull(record: AppleRecord, reasonCode: String, detail: String): Nothing? {
        addDiagnostic(
            AppleHealthImportDiagnostic(
                record.type,
                null,
                reasonCode,
                record.timeRangeOrNull()?.toString(),
                record.unit,
                record.valueForReport,
                detail,
            ),
        )
        typeStats.getOrPut(record.type) { MutableAppleImportTypeStats() }.skipped += 1
        return null
    }

    internal fun markConverted(appleType: String) {
        typeStats.getOrPut(appleType) { MutableAppleImportTypeStats() }.converted += 1
    }

    private fun addDiagnostic(diagnostic: AppleHealthImportDiagnostic) {
        diagnosticSummaries.add(diagnostic)
        if (diagnostics.size < diagnosticLimit) {
            diagnostics += diagnostic
        }
    }
}
