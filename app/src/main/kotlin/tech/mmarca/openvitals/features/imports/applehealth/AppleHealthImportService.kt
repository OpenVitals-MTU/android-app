package tech.mmarca.openvitals.features.imports.applehealth

import android.content.Context
import android.net.Uri
import androidx.health.connect.client.records.Record
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedInputStream
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import tech.mmarca.openvitals.BuildConfig
import tech.mmarca.openvitals.data.repository.HealthRepository

@Singleton
class AppleHealthImportService
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val healthRepository: HealthRepository,
    ) {
        suspend fun importAppleHealthExport(
            uri: Uri,
            progress: suspend (AppleHealthImportProgress) -> Unit = {},
        ): AppleHealthImportResult =
            withContext(Dispatchers.IO) {
                val converter = AppleHealthImportConverter(
                    mindfulnessAvailable = healthRepository.isMindfulnessAvailable(),
                    diagnosticLimit = DiagnosticReportLimit,
                )
                val importState = StreamingAppleHealthImportState(
                    converter = converter,
                    onProgress = progress,
                )
                val input =
                    context.contentResolver.openInputStream(uri)
                        ?: throw IllegalArgumentException("Unable to open Apple Health export.")

                val parsed =
                    input.use { rawInput ->
                        BufferedInputStream(rawInput).use { bufferedInput ->
                            progress(importState.progressSnapshot(AppleHealthImportPhase.PARSING))
                            AppleHealthImportParser.parse(bufferedInput, importState)
                        }
                    }

                progress(importState.progressSnapshot(AppleHealthImportPhase.FINISHING))
                importState.finishBufferedGroups()
                importState.flushConverted()
                val summaries = converter.typeStats.toTypeSummaries()
                val diagnostics = importState.diagnostics()
                val diagnosticSummaries = importState.diagnosticSummaries()
                AppleHealthImportResult(
                    parsedRecords = parsed.parsedRecords,
                    parsedWorkouts = parsed.parsedWorkouts,
                    parsedCorrelations = parsed.parsedCorrelations,
                    parsedActivitySummaries = parsed.parsedActivitySummaries,
                    convertedRecords = summaries.sumOf { it.converted },
                    importedRecords = importState.importedRecords,
                    duplicateSkippedRecords = summaries.sumOf { it.duplicateSkipped },
                    unsupportedElements = summaries.sumOf { it.unsupported },
                    skippedRecords = summaries.sumOf { it.skipped },
                    failedRecords = summaries.sumOf { it.failed },
                    typeSummaries = summaries,
                    diagnostics = diagnostics,
                    shareableReportText = buildReportText(
                        parsed = parsed,
                        imported = importState.importedRecords,
                        summaries = summaries,
                        diagnostics = diagnostics,
                        diagnosticSummaries = diagnosticSummaries,
                    ),
                )
            }

        private inner class StreamingAppleHealthImportState(
            private val converter: AppleHealthImportConverter,
            private val onProgress: suspend (AppleHealthImportProgress) -> Unit,
        ) : AppleHealthXmlEventConsumer {
            private val bufferedRecords = mutableListOf<AppleRecord>()
            private val bufferedWorkouts = mutableListOf<AppleWorkout>()
            private val bufferedCorrelations = mutableListOf<AppleCorrelation>()
            private val convertedBatch = mutableListOf<ConvertedAppleRecord>()
            private val serviceDiagnostics = mutableListOf<AppleHealthImportDiagnostic>()
            private val serviceDiagnosticSummaries = linkedMapOf<AppleHealthDiagnosticSummaryKey, MutableAppleHealthImportDiagnosticSummary>()
            private var parsedRecords = 0
            private var parsedWorkouts = 0
            private var parsedCorrelations = 0
            private var parsedActivitySummaries = 0
            private var convertedRecords = 0
            private var lastProgressParsedElements = 0

            var importedRecords: Int = 0
                private set

            override fun onParsedType(type: String) {
                converter.markParsed(type)
            }

            override fun onRecord(record: AppleRecord) {
                parsedRecords += 1
                converter.noteWorkoutOverlap(record)
                if (converter.shouldBufferRecord(record)) {
                    bufferedRecords += record
                    if (bufferedRecords.size >= BufferedRecordBatchSize) {
                        flushBufferedRecords()
                    }
                } else {
                    converter.convertStreamingRecord(record)?.let(::acceptConverted)
                }
                maybeReportProgress()
            }

            override fun onWorkout(workout: AppleWorkout) {
                parsedWorkouts += 1
                bufferedWorkouts += workout
                maybeReportProgress()
            }

            override fun onCorrelation(correlation: AppleCorrelation) {
                parsedCorrelations += 1
                bufferedCorrelations += correlation
                maybeReportProgress()
            }

            override fun onActivitySummary() {
                parsedActivitySummaries += 1
                maybeReportProgress()
            }

            fun finishBufferedGroups() {
                flushBufferedRecords()
                converter.convertBufferedGroups(
                    records = emptyList(),
                    workouts = bufferedWorkouts,
                    correlations = bufferedCorrelations,
                    parsedActivitySummaries = parsedActivitySummaries,
                ).forEach(::acceptConverted)
                bufferedRecords.clear()
                bufferedWorkouts.clear()
                bufferedCorrelations.clear()
            }

            private fun flushBufferedRecords() {
                if (bufferedRecords.isEmpty()) return
                converter.convertBufferedGroups(
                    records = bufferedRecords,
                    workouts = emptyList(),
                    correlations = emptyList(),
                    parsedActivitySummaries = 0,
                ).forEach(::acceptConverted)
                bufferedRecords.clear()
            }

            fun flushConverted() {
                if (convertedBatch.isEmpty()) return
                val typeStats = converter.typeStats
                val uniqueConverted = convertedBatch.deduplicateWithinImport(
                    diagnostics = serviceDiagnostics,
                    diagnosticSummaries = serviceDiagnosticSummaries,
                    typeStats = typeStats,
                )
                val existingClientRecordIds = runBlocking { findExistingClientRecordIds(uniqueConverted) }
                val toInsert = uniqueConverted.filterNot { converted ->
                    val clientRecordId = converted.clientRecordId
                    val duplicate = clientRecordId != null && clientRecordId in existingClientRecordIds
                    if (duplicate) {
                        serviceDiagnostics.addDiagnostic(
                            converted.diagnostic(
                                "duplicate_existing",
                                "A matching Health Connect clientRecordId already exists.",
                            ),
                            serviceDiagnosticSummaries,
                        )
                        typeStats.getOrPut(converted.appleType) { MutableAppleImportTypeStats() }.duplicateSkipped += 1
                    }
                    duplicate
                }

                val insertionResult = runBlocking {
                    insertConvertedRecords(toInsert, serviceDiagnostics, serviceDiagnosticSummaries, typeStats)
                }
                importedRecords += insertionResult.imported
                convertedBatch.clear()
                runBlocking { onProgress(progressSnapshot(AppleHealthImportPhase.WRITING)) }
            }

            fun diagnostics(): List<AppleHealthImportDiagnostic> =
                (converter.diagnosticsSnapshot() + serviceDiagnostics).take(DiagnosticReportLimit)

            fun diagnosticSummaries(): List<AppleHealthImportDiagnosticSummary> =
                (converter.diagnosticSummariesSnapshot() + serviceDiagnosticSummaries.values.map { it.toSummary() })
                    .mergeDiagnosticSummaries()

            fun progressSnapshot(phase: AppleHealthImportPhase): AppleHealthImportProgress =
                AppleHealthImportProgress(
                    phase = phase,
                    parsedRecords = parsedRecords,
                    parsedWorkouts = parsedWorkouts,
                    parsedCorrelations = parsedCorrelations,
                    parsedActivitySummaries = parsedActivitySummaries,
                    convertedRecords = convertedRecords,
                    importedRecords = importedRecords,
                    duplicateSkippedRecords = converter.typeStats.values.sumOf { it.duplicateSkipped },
                    unsupportedElements = converter.typeStats.values.sumOf { it.unsupported },
                    skippedRecords = converter.typeStats.values.sumOf { it.skipped },
                    failedRecords = converter.typeStats.values.sumOf { it.failed },
                )

            private fun acceptConverted(converted: ConvertedAppleRecord) {
                convertedBatch += converted
                convertedRecords += 1
                if (convertedBatch.size >= ConvertedBatchSize) {
                    flushConverted()
                }
            }

            private fun maybeReportProgress() {
                val parsedElements = parsedRecords + parsedWorkouts + parsedCorrelations + parsedActivitySummaries
                if (parsedElements - lastProgressParsedElements >= ProgressReportElementInterval) {
                    lastProgressParsedElements = parsedElements
                    runBlocking { onProgress(progressSnapshot(AppleHealthImportPhase.PARSING)) }
                }
            }
        }

        private suspend fun findExistingClientRecordIds(records: List<ConvertedAppleRecord>): Set<String> =
            records
                .filter { it.clientRecordId != null }
                .groupBy { it.recordType }
                .flatMapTo(mutableSetOf()) { (recordType, grouped) ->
                    val start = grouped.minOfOrNull { it.sourceTimeRange.start }?.minusSeconds(1) ?: return@flatMapTo emptySet()
                    val end = grouped.maxOfOrNull { it.sourceTimeRange.end }?.plusSeconds(1) ?: return@flatMapTo emptySet()
                    val wantedIds = grouped.mapNotNullTo(mutableSetOf()) { it.clientRecordId }
                    runCatching {
                        healthRepository.readImportedClientRecordIds(recordType, start, end)
                            .intersect(wantedIds)
                    }.getOrElse {
                        emptySet()
                    }
                }

        private suspend fun insertConvertedRecords(
            records: List<ConvertedAppleRecord>,
            diagnostics: MutableList<AppleHealthImportDiagnostic>,
            diagnosticSummaries: MutableMap<AppleHealthDiagnosticSummaryKey, MutableAppleHealthImportDiagnosticSummary>,
            typeStats: MutableMap<String, MutableAppleImportTypeStats>,
        ): AppleHealthInsertionResult {
            if (records.isEmpty()) return AppleHealthInsertionResult()
            val batchResult = runCatching {
                healthRepository.insertImportedRecords(records.map { it.record })
            }
            if (batchResult.isSuccess) {
                records.forEach { converted ->
                    typeStats.getOrPut(converted.appleType) { MutableAppleImportTypeStats() }.imported += 1
                }
                return AppleHealthInsertionResult(imported = records.size)
            }

            return records.fold(AppleHealthInsertionResult()) { result, converted ->
                runCatching { healthRepository.insertImportedRecords(listOf(converted.record)) }
                    .fold(
                        onSuccess = {
                            typeStats.getOrPut(converted.appleType) { MutableAppleImportTypeStats() }.imported += 1
                            result.copy(imported = result.imported + 1)
                        },
                        onFailure = { error ->
                            val duplicate = error.isDuplicateClientRecordFailure()
                            val stats = typeStats.getOrPut(converted.appleType) { MutableAppleImportTypeStats() }
                            if (duplicate) {
                                stats.duplicateSkipped += 1
                                diagnostics.addDiagnostic(
                                    converted.diagnostic("duplicate_rejected", "Health Connect rejected this as an existing clientRecordId."),
                                    diagnosticSummaries,
                                )
                                result.copy(duplicates = result.duplicates + 1)
                            } else {
                                stats.failed += 1
                                diagnostics.addDiagnostic(
                                    converted.diagnostic(
                                        reasonCode = "insert_failed",
                                        detail = error.localizedMessage ?: error::class.simpleName.orEmpty().ifBlank { "Health Connect insert failed." },
                                    ),
                                    diagnosticSummaries,
                                )
                                result.copy(failed = result.failed + 1)
                            }
                        },
                    )
            }
        }
    }

private data class AppleHealthInsertionResult(
    val imported: Int = 0,
    val duplicates: Int = 0,
    val failed: Int = 0,
)

private val ConvertedAppleRecord.clientRecordId: String?
    get() = record.metadata.clientRecordId?.takeIf { it.isNotBlank() }

private fun List<ConvertedAppleRecord>.deduplicateWithinImport(
    diagnostics: MutableList<AppleHealthImportDiagnostic>,
    diagnosticSummaries: MutableMap<AppleHealthDiagnosticSummaryKey, MutableAppleHealthImportDiagnosticSummary>,
    typeStats: MutableMap<String, MutableAppleImportTypeStats>,
): List<ConvertedAppleRecord> {
    val seen = mutableSetOf<String>()
    return filter { converted ->
        val clientRecordId = converted.clientRecordId ?: converted.fingerprint
        if (clientRecordId in seen) {
            diagnostics.addDiagnostic(
                converted.diagnostic(
                    "duplicate_in_file",
                    "The export contained another object with the same deterministic import fingerprint.",
                ),
                diagnosticSummaries,
            )
            typeStats.getOrPut(converted.appleType) { MutableAppleImportTypeStats() }.duplicateSkipped += 1
            false
        } else {
            seen += clientRecordId
            true
        }
    }
}

private fun MutableList<AppleHealthImportDiagnostic>.addDiagnostic(
    diagnostic: AppleHealthImportDiagnostic,
    diagnosticSummaries: MutableMap<AppleHealthDiagnosticSummaryKey, MutableAppleHealthImportDiagnosticSummary>,
) {
    diagnosticSummaries.add(diagnostic)
    if (size < DiagnosticReportLimit) {
        add(diagnostic)
    }
}

private fun List<AppleHealthImportDiagnosticSummary>.mergeDiagnosticSummaries(): List<AppleHealthImportDiagnosticSummary> {
    val merged = linkedMapOf<AppleHealthDiagnosticSummaryKey, MutableAppleHealthImportDiagnosticSummary>()
    forEach { summary ->
        val diagnostic = AppleHealthImportDiagnostic(
            appleType = summary.appleType,
            targetType = summary.targetType,
            reasonCode = summary.reasonCode,
            timeRange = summary.exampleTimeRange,
            unit = summary.exampleUnit,
            value = summary.exampleValue,
            detail = summary.detail,
        )
        val key = AppleHealthDiagnosticSummaryKey(
            appleType = summary.appleType,
            targetType = summary.targetType,
            reasonCode = summary.reasonCode,
            detail = summary.detail,
        )
        val existing = merged[key]
        if (existing != null) {
            existing.count += summary.count
        } else {
            merged.add(diagnostic)
            merged[key]?.count = summary.count
        }
    }
    return merged.values.map { it.toSummary() }
}

private fun ConvertedAppleRecord.diagnostic(reasonCode: String, detail: String): AppleHealthImportDiagnostic =
    AppleHealthImportDiagnostic(
        appleType = appleType,
        targetType = targetType,
        reasonCode = reasonCode,
        timeRange = sourceTimeRange.toString(),
        unit = unit,
        value = value,
        detail = detail,
    )

private fun Throwable.isDuplicateClientRecordFailure(): Boolean {
    val text = generateSequence(this) { it.cause }
        .joinToString(" ") { listOfNotNull(it.message, it::class.simpleName).joinToString(" ") }
        .lowercase()
    return "clientrecordid" in text ||
        "client record id" in text ||
        (("duplicate" in text || "already exist" in text || "already exists" in text) && "record" in text)
}

private fun Map<String, MutableAppleImportTypeStats>.toTypeSummaries(): List<AppleHealthImportTypeSummary> =
    entries
        .sortedWith(compareByDescending<Map.Entry<String, MutableAppleImportTypeStats>> { it.value.parsed }
            .thenBy { it.key })
        .map { (type, stats) ->
            AppleHealthImportTypeSummary(
                appleType = type,
                parsed = stats.parsed,
                converted = stats.converted,
                imported = stats.imported,
                duplicateSkipped = stats.duplicateSkipped,
                unsupported = stats.unsupported,
                skipped = stats.skipped,
                failed = stats.failed,
            )
        }

private fun buildReportText(
    parsed: AppleParsedExport,
    imported: Int,
    summaries: List<AppleHealthImportTypeSummary>,
    diagnostics: List<AppleHealthImportDiagnostic>,
    diagnosticSummaries: List<AppleHealthImportDiagnosticSummary>,
): String = buildString {
    appendLine("OpenVitals Apple Health Import Report")
    appendLine("Generated: ${Instant.now()}")
    appendLine("App version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
    appendLine("Health Connect client: androidx.health.connect:connect-client (runtime version unavailable)")
    appendLine()
    appendLine("Summary")
    appendLine("Parsed records: ${parsed.parsedRecords}")
    appendLine("Parsed workouts: ${parsed.parsedWorkouts}")
    appendLine("Parsed correlations: ${parsed.parsedCorrelations}")
    appendLine("Parsed activity summaries: ${parsed.parsedActivitySummaries}")
    appendLine("Converted Health Connect records: ${summaries.sumOf { it.converted }}")
    appendLine("Imported Health Connect records: $imported")
    appendLine("Duplicate skipped: ${summaries.sumOf { it.duplicateSkipped }}")
    appendLine("Unsupported: ${summaries.sumOf { it.unsupported }}")
    appendLine("Skipped: ${summaries.sumOf { it.skipped }}")
    appendLine("Failed: ${summaries.sumOf { it.failed }}")
    appendLine()
    appendLine("By Apple Type")
    summaries.forEach { summary ->
        appendLine(
            "- ${summary.appleType}: parsed=${summary.parsed}, converted=${summary.converted}, " +
                "imported=${summary.imported}, duplicate=${summary.duplicateSkipped}, " +
                "unsupported=${summary.unsupported}, skipped=${summary.skipped}, failed=${summary.failed}",
        )
    }
    appendLine()
    appendLine("Diagnostics")
    if (diagnosticSummaries.isEmpty()) {
        appendLine("No failures, skips, duplicates, or unsupported entries were recorded.")
    } else {
        val reasonSummaries = diagnosticSummaries.groupBy { it.reasonCode }
            .mapValues { (_, grouped) -> grouped.sumOf { it.count } }
            .toSortedMap()
        appendLine(
            "Grouped diagnostic types: ${diagnosticSummaries.size}; " +
                reasonSummaries.entries.joinToString(", ") { (reason, count) -> "$reason=$count" },
        )
        diagnosticSummaries
            .sortedWith(
                compareByDescending<AppleHealthImportDiagnosticSummary> { it.count }
                    .thenBy { it.reasonCode }
                    .thenBy { it.appleType },
            )
            .forEachIndexed { index, diagnostic ->
                val exampleParts = listOfNotNull(
                    diagnostic.exampleTimeRange?.let { "exampleTime=$it" },
                    diagnostic.exampleUnit?.let { "unit=$it" },
                    diagnostic.exampleValue?.let { "value=$it" },
                )
                val exampleText = if (exampleParts.isEmpty()) "" else exampleParts.joinToString(prefix = "; ", separator = "; ")
                appendLine(
                    "${index + 1}. count=${diagnostic.count}; reason=${diagnostic.reasonCode}; appleType=${diagnostic.appleType}; " +
                        "target=${diagnostic.targetType ?: "none"}; detail=${diagnostic.detail}$exampleText",
                )
            }
        if (diagnostics.size == DiagnosticReportLimit) {
            appendLine("Diagnostic examples were capped at $DiagnosticReportLimit rows; grouped counts include all recorded diagnostics.")
        }
    }
}

private const val DiagnosticReportLimit = 200
private const val ConvertedBatchSize = 300
private const val BufferedRecordBatchSize = 2_000
private const val ProgressReportElementInterval = 500
