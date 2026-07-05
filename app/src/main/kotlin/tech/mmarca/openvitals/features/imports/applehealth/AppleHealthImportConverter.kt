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
    var notSelected: Int = 0,
    var unsupported: Int = 0,
    var skipped: Int = 0,
    var failed: Int = 0,
)

internal class AppleHealthImportConverter(
    internal val mindfulnessAvailable: Boolean,
    private val diagnosticLimit: Int = Int.MAX_VALUE,
) {
    private val diagnostics = mutableListOf<AppleHealthImportDiagnostic>()
    private val diagnosticSummaries = linkedMapOf<AppleHealthDiagnosticSummaryKey, MutableAppleHealthImportDiagnosticSummary>()
    internal val typeStats = linkedMapOf<String, MutableAppleImportTypeStats>()

    // Aggregate counters mirroring typeStats sums. All mutation happens on the parse thread; these
    // volatiles let a concurrent writer coroutine read progress totals without iterating typeStats
    // (which the parse thread mutates via getOrPut).
    @Volatile
    internal var unsupportedCount: Int = 0
        private set

    @Volatile
    internal var skippedCount: Int = 0
        private set

    @Volatile
    internal var invalidCount: Int = 0
        private set
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
            convertAdditiveOverlapSensitiveRecords(export.records, ::add)
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

    fun shouldBufferForOverlapDedup(record: AppleRecord): Boolean =
        record.type in AppleAdditiveOverlapSensitiveTypes

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
            convertBufferedGroups(records, workouts, correlations, parsedActivitySummaries, ::add)
        }

    /**
     * Emit-based variant: converted records are handed to [emit] as they are produced instead of
     * being materialized in one list. For step/energy-heavy exports the additive group can span
     * hundreds of thousands of records, so streaming keeps memory bounded and lets the import
     * pipeline start writing while conversion is still running.
     */
    fun convertBufferedGroups(
        records: List<AppleRecord>,
        workouts: List<AppleWorkout>,
        correlations: List<AppleCorrelation>,
        parsedActivitySummaries: Int,
        emit: (ConvertedAppleRecord) -> Unit,
    ) {
        convertBloodPressureCorrelations(correlations).forEach(emit)
        convertStandaloneBloodPressure(records).forEach(emit)
        convertSleep(records, trackConsumedRecords = false).forEach(emit)
        convertNutrition(records, trackConsumedRecords = false).forEach(emit)
        convertWorkouts(workouts, workoutOverlapCandidates, workoutOverlapCandidatesLimitReached).forEach(emit)
        convertAdditiveOverlapSensitiveRecords(records, emit)
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

    private fun convertAdditiveOverlapSensitiveRecords(
        records: List<AppleRecord>,
        emit: (ConvertedAppleRecord) -> Unit,
    ) {
        val additiveRecords = records.filter { it.type in AppleAdditiveOverlapSensitiveTypes }
        if (additiveRecords.isEmpty()) return

        // Single pass: records without a usable start date cannot participate in overlap dedup
        // (and can never share a fingerprint with a candidate, since startDate is part of the
        // fingerprint), so they convert directly; the rest are deduplicated in deterministic order.
        val candidates = ArrayList<AppleAdditiveOverlapCandidate>(additiveRecords.size)
        additiveRecords.forEach { record ->
            val candidate = record.toAdditiveOverlapCandidate()
            if (candidate == null) {
                consumedRecordFingerprints += record.sourceFingerprint
                convertSingleRecord(record)?.let(emit)
            } else {
                candidates += candidate
            }
        }
        candidates.sortWith(
            compareBy<AppleAdditiveOverlapCandidate> { it.record.type }
                .thenBy { it.sourcePriority }
                .thenBy { it.start }
                .thenBy { it.end },
        )
        val accepted = AppleAdditiveOverlapIndex()
        candidates.forEach { candidate ->
            consumedRecordFingerprints += candidate.record.sourceFingerprint
            if (accepted.isMostlyCovered(candidate)) {
                skippedNull(
                    candidate.record,
                    reasonCode = "overlap_cross_source",
                    detail = "Skipped because another source already contributed an overlapping additive sample.",
                )
                return@forEach
            }
            accepted.add(candidate)
            convertSingleRecord(candidate.record)?.let(emit)
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
        invalidCount += 1
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
        unsupportedCount += 1
        return null
    }

    internal fun skippedNull(record: AppleRecord, reasonCode: String, detail: String): Nothing? {
        skipped(
            appleType = record.type,
            reasonCode = reasonCode,
            detail = detail,
            timeRange = record.timeRangeOrNull()?.toString(),
            unit = record.unit,
            value = record.valueForReport,
        )
        return null
    }

    internal fun skipped(
        appleType: String,
        reasonCode: String,
        detail: String,
        timeRange: String?,
        unit: String? = null,
        value: String? = null,
    ) {
        addDiagnostic(AppleHealthImportDiagnostic(appleType, null, reasonCode, timeRange, unit, value, detail))
        typeStats.getOrPut(appleType) { MutableAppleImportTypeStats() }.skipped += 1
        skippedCount += 1
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
