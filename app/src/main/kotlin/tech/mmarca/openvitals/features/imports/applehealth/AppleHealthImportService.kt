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
import kotlinx.coroutines.withContext
import tech.mmarca.openvitals.BuildConfig
import tech.mmarca.openvitals.data.repository.HealthRepository
import tech.mmarca.openvitals.data.repository.PreferencesRepository

@Singleton
class AppleHealthImportService
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val healthRepository: HealthRepository,
        private val preferencesRepository: PreferencesRepository,
    ) {
        suspend fun importAppleHealthExport(uri: Uri): AppleHealthImportResult =
            withContext(Dispatchers.IO) {
                val input =
                    context.contentResolver.openInputStream(uri)
                        ?: throw IllegalArgumentException("Unable to open Apple Health export.")

                val parsed =
                    input.use { rawInput ->
                        BufferedInputStream(rawInput).use { bufferedInput ->
                            AppleHealthImportParser.parse(bufferedInput)
                        }
                    }

                val conversion = AppleHealthImportConverter(
                    trackCycle = preferencesRepository.trackCycle,
                    mindfulnessAvailable = healthRepository.isMindfulnessAvailable(),
                ).convert(parsed)

                val mutableDiagnostics = conversion.diagnostics.toMutableList()
                val typeStats = conversion.typeStats
                val uniqueConverted = conversion.converted.deduplicateWithinImport(mutableDiagnostics, typeStats)
                val existingClientRecordIds = findExistingClientRecordIds(uniqueConverted)
                val toInsert = uniqueConverted.filterNot { converted ->
                    val clientRecordId = converted.clientRecordId
                    val duplicate = clientRecordId != null && clientRecordId in existingClientRecordIds
                    if (duplicate) {
                        mutableDiagnostics += converted.diagnostic("duplicate_existing", "A matching Health Connect clientRecordId already exists.")
                        typeStats.getOrPut(converted.appleType) { MutableAppleImportTypeStats() }.duplicateSkipped += 1
                    }
                    duplicate
                }

                val insertionResult = insertConvertedRecords(toInsert, mutableDiagnostics, typeStats)
                val summaries = typeStats.toTypeSummaries()
                val diagnostics = mutableDiagnostics.take(DiagnosticReportLimit)
                AppleHealthImportResult(
                    parsedRecords = parsed.parsedRecords,
                    parsedWorkouts = parsed.parsedWorkouts,
                    parsedCorrelations = parsed.parsedCorrelations,
                    parsedActivitySummaries = parsed.parsedActivitySummaries,
                    convertedRecords = conversion.converted.size,
                    importedRecords = insertionResult.imported,
                    duplicateSkippedRecords = summaries.sumOf { it.duplicateSkipped },
                    unsupportedElements = summaries.sumOf { it.unsupported },
                    skippedRecords = summaries.sumOf { it.skipped },
                    failedRecords = summaries.sumOf { it.failed },
                    typeSummaries = summaries,
                    diagnostics = diagnostics,
                    shareableReportText = buildReportText(
                        parsed = parsed,
                        imported = insertionResult.imported,
                        summaries = summaries,
                        diagnostics = diagnostics,
                    ),
                )
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
                                diagnostics += converted.diagnostic("duplicate_rejected", "Health Connect rejected this as an existing clientRecordId.")
                                result.copy(duplicates = result.duplicates + 1)
                            } else {
                                stats.failed += 1
                                diagnostics += converted.diagnostic(
                                    reasonCode = "insert_failed",
                                    detail = error.localizedMessage ?: error::class.simpleName.orEmpty().ifBlank { "Health Connect insert failed." },
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
    typeStats: MutableMap<String, MutableAppleImportTypeStats>,
): List<ConvertedAppleRecord> {
    val seen = mutableSetOf<String>()
    return filter { converted ->
        val clientRecordId = converted.clientRecordId ?: converted.fingerprint
        if (clientRecordId in seen) {
            diagnostics += converted.diagnostic("duplicate_in_file", "The export contained another object with the same deterministic import fingerprint.")
            typeStats.getOrPut(converted.appleType) { MutableAppleImportTypeStats() }.duplicateSkipped += 1
            false
        } else {
            seen += clientRecordId
            true
        }
    }
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
    if (diagnostics.isEmpty()) {
        appendLine("No failures, skips, duplicates, or unsupported entries were recorded.")
    } else {
        diagnostics.forEachIndexed { index, diagnostic ->
            appendLine(
                "${index + 1}. reason=${diagnostic.reasonCode}; appleType=${diagnostic.appleType}; " +
                    "target=${diagnostic.targetType ?: "none"}; time=${diagnostic.timeRange ?: "unknown"}; " +
                    "unit=${diagnostic.unit ?: "none"}; value=${diagnostic.value ?: "none"}; detail=${diagnostic.detail}",
            )
        }
        if (diagnostics.size == DiagnosticReportLimit) {
            appendLine("Diagnostics were truncated at $DiagnosticReportLimit entries.")
        }
    }
}

private const val DiagnosticReportLimit = 200
