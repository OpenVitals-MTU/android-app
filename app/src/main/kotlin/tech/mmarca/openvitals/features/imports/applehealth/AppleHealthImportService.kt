package tech.mmarca.openvitals.features.imports.applehealth

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.health.connect.client.records.Record
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedInputStream
import java.time.Instant
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import tech.mmarca.openvitals.BuildConfig
import tech.mmarca.openvitals.data.repository.AppleHealthImportRepository

@Singleton
class AppleHealthImportService
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val importRepository: AppleHealthImportRepository,
    ) {
        suspend fun analyzeAppleHealthExport(
            uri: Uri,
            progress: suspend (AppleHealthImportProgress) -> Unit = {},
        ): AppleHealthImportAnalysisResult =
            withContext(Dispatchers.IO) {
                val importLogs = mutableListOf<String>()
                fun log(message: String) {
                    importLogs.addImportInfo(message)
                }
                log("Apple Health analysis requested uri=$uri")
                val analysisState = StreamingAppleHealthScanAnalysisState(
                    mindfulnessAvailable = importRepository.isMindfulnessAvailable(),
                    onProgress = progress,
                    importLogs = importLogs,
                )
                val parsed = parseExport(
                    uri,
                    analysisState,
                    importLogs,
                    progress,
                    options = AppleHealthParseOptions(parseRouteFiles = false, parseRecordDetails = false),
                )

                progress(analysisState.progressSnapshot(AppleHealthImportPhase.CONVERTING))
                log("Stage started: Summarizing compatible categories")
                val summaries = analysisState.typeSummaries()
                val diagnostics = analysisState.diagnostics()
                val diagnosticSummaries = analysisState.diagnosticSummaries()
                val categorySummaries = analysisState.categorySummaries()
                log(
                    "Stage finished: Summarizing compatible categories compatible=${summaries.sumOf { it.converted }} " +
                        "categories=${categorySummaries.size} unsupported=${summaries.sumOf { it.unsupported }} " +
                        "skipped=${summaries.sumOf { it.skipped }} failed=${summaries.sumOf { it.failed }}",
                )
                log(
                    "Analysis completed converted=${summaries.sumOf { it.converted }} " +
                        "categories=${categorySummaries.size} unsupported=${summaries.sumOf { it.unsupported }} " +
                        "skipped=${summaries.sumOf { it.skipped }} failed=${summaries.sumOf { it.failed }} " +
                        "diagnostics=${diagnostics.size}",
                )
                progress(analysisState.progressSnapshot(AppleHealthImportPhase.BUILDING_REPORT))
                log(
                    "Stage started: Building report diagnostics=${diagnostics.size} " +
                        "diagnosticGroups=${diagnosticSummaries.size} typeSummaries=${summaries.size}",
                )
                log("Stage finished: Building report")
                val reportText = buildReportText(
                    parsed = parsed,
                    imported = 0,
                    selectedCategories = null,
                    summaries = summaries,
                    categorySummaries = categorySummaries,
                    diagnostics = diagnostics,
                    diagnosticSummaries = diagnosticSummaries,
                    importLogs = importLogs,
                )
                AppleHealthImportAnalysisResult(
                    parsedRecords = parsed.parsedRecords,
                    parsedWorkouts = parsed.parsedWorkouts,
                    parsedCorrelations = parsed.parsedCorrelations,
                    parsedActivitySummaries = parsed.parsedActivitySummaries,
                    convertedRecords = summaries.sumOf { it.converted },
                    unsupportedElements = summaries.sumOf { it.unsupported },
                    skippedRecords = summaries.sumOf { it.skipped },
                    failedRecords = summaries.sumOf { it.failed },
                    categorySummaries = categorySummaries,
                    typeSummaries = summaries,
                    diagnostics = diagnostics,
                    shareableReportText = reportText,
                )
            }

        suspend fun importAppleHealthExport(
            uri: Uri,
            selectedCategories: Set<AppleHealthImportCategory> = AllAppleHealthImportCategories,
            progress: suspend (AppleHealthImportProgress) -> Unit = {},
        ): AppleHealthImportResult =
            withContext(Dispatchers.IO) {
                // Appended from both the parse thread and the batch-writer coroutine.
                val importLogs: MutableList<String> = Collections.synchronizedList(mutableListOf())
                fun log(message: String) {
                    importLogs.addImportInfo(message)
                }
                log("Apple Health import requested uri=$uri selectedCategories=${selectedCategories.joinToString { it.name }}")
                val converter = AppleHealthImportConverter(
                    mindfulnessAvailable = importRepository.isMindfulnessAvailable(),
                    diagnosticLimit = MaxRawDiagnostics,
                )
                // Serializes every onProgress invocation: the worker's progress callback mutates
                // shared state and must never run concurrently from parse thread + writer.
                val progressMutex = Mutex()
                val batchChannel = Channel<List<ConvertedAppleRecord>>(capacity = BatchChannelCapacity)
                val writer = ConvertedBatchWriter(importLogs)
                val importState = StreamingAppleHealthWritingState(
                    converter = converter,
                    onProgress = progress,
                    importLogs = importLogs,
                    progressMutex = progressMutex,
                    selectedCategories = selectedCategories,
                    writer = writer,
                    // The SAX parse thread cannot suspend; blocking here applies backpressure when
                    // the writer falls behind (same runBlocking pattern as progress publishing).
                    sendBatch = { batch -> runBlocking { batchChannel.send(batch) } },
                )
                writer.publishProgress = { phase ->
                    progressMutex.withLock { progress(importState.progressSnapshot(phase)) }
                }

                // Pipeline: parse+convert (this coroutine) overlaps duplicate-check+insert (writer).
                val parsed = coroutineScope {
                    val writerJob = launch {
                        for (batch in batchChannel) {
                            writer.process(batch)
                        }
                    }
                    // If the writer fails or is cancelled, unblock a producer stuck in send().
                    writerJob.invokeOnCompletion { cause ->
                        if (cause != null) batchChannel.cancel()
                    }
                    try {
                        val parsedExport = parseExport(uri, importState, importLogs, progress)

                        progressMutex.withLock { progress(importState.progressSnapshot(AppleHealthImportPhase.CONVERTING)) }
                        log("Stage started: Converting records")
                        importState.finishBufferedGroups()
                        val conversionSummaries = converter.typeStats.toTypeSummaries()
                        log(
                            "Stage finished: Converting records converted=${conversionSummaries.sumOf { it.converted }} " +
                                "notSelected=${conversionSummaries.sumOf { it.notSelected }} unsupported=${conversionSummaries.sumOf { it.unsupported }} " +
                                "skipped=${conversionSummaries.sumOf { it.skipped }} failed=${conversionSummaries.sumOf { it.failed }}",
                        )
                        log("Stage started: Flushing final converted records")
                        importState.finishConverted()
                        parsedExport
                    } finally {
                        batchChannel.close()
                    }
                }
                // The writer has completed (coroutineScope joined it): merge its per-type
                // accounting into the converter's stats and assemble the final results.
                writer.mergeInto(converter.typeStats)
                val summaries = converter.typeStats.toTypeSummaries()
                val diagnostics = converter.diagnosticsSnapshot() + writer.diagnostics()
                val diagnosticSummaries =
                    (converter.diagnosticSummariesSnapshot() + writer.diagnosticSummaries()).mergeDiagnosticSummaries()
                val categorySummaries = importState.categorySummaries()
                log(
                    "Stage finished: Flushing final converted records imported=${importState.importedRecords} " +
                        "duplicates=${summaries.sumOf { it.duplicateSkipped }} failed=${summaries.sumOf { it.failed }}",
                )
                log(
                    "Import completed converted=${summaries.sumOf { it.converted }} imported=${importState.importedRecords} " +
                        "duplicates=${summaries.sumOf { it.duplicateSkipped }} unsupported=${summaries.sumOf { it.unsupported }} " +
                        "notSelected=${summaries.sumOf { it.notSelected }} skipped=${summaries.sumOf { it.skipped }} failed=${summaries.sumOf { it.failed }} " +
                        "diagnostics=${diagnostics.size}",
                )
                progress(importState.progressSnapshot(AppleHealthImportPhase.BUILDING_REPORT))
                log(
                    "Stage started: Building report diagnostics=${diagnostics.size} " +
                        "diagnosticGroups=${diagnosticSummaries.size} typeSummaries=${summaries.size}",
                )
                log("Stage finished: Building report")
                val reportText = buildReportText(
                    parsed = parsed,
                    imported = importState.importedRecords,
                    selectedCategories = selectedCategories,
                    summaries = summaries,
                    categorySummaries = categorySummaries,
                    diagnostics = diagnostics,
                    diagnosticSummaries = diagnosticSummaries,
                    importLogs = importLogs,
                )
                AppleHealthImportResult(
                    parsedRecords = parsed.parsedRecords,
                    parsedWorkouts = parsed.parsedWorkouts,
                    parsedCorrelations = parsed.parsedCorrelations,
                    parsedActivitySummaries = parsed.parsedActivitySummaries,
                    convertedRecords = summaries.sumOf { it.converted },
                    importedRecords = importState.importedRecords,
                    duplicateSkippedRecords = summaries.sumOf { it.duplicateSkipped },
                    notSelectedRecords = summaries.sumOf { it.notSelected },
                    unsupportedElements = summaries.sumOf { it.unsupported },
                    skippedRecords = summaries.sumOf { it.skipped },
                    failedRecords = summaries.sumOf { it.failed },
                    typeSummaries = summaries,
                    diagnostics = diagnostics,
                    shareableReportText = reportText,
                )
            }

        private suspend fun parseExport(
            uri: Uri,
            state: StreamingAppleHealthProgressState,
            importLogs: MutableList<String>,
            progress: suspend (AppleHealthImportProgress) -> Unit,
            options: AppleHealthParseOptions = AppleHealthParseOptions(),
        ): AppleParsedExport {
            val input =
                context.contentResolver.openInputStream(uri)
                    ?: throw IllegalArgumentException("Unable to open Apple Health export.")

            val startedAtMillis = System.currentTimeMillis()
            importLogs.addImportInfo(
                "Stage started: Scanning export uri=$uri bufferSize=$ImportInputBufferSize " +
                    "parseRouteFiles=${options.parseRouteFiles} parseRecordDetails=${options.parseRecordDetails}",
            )
            val parsed =
                input.use { rawInput ->
                    BufferedInputStream(rawInput, ImportInputBufferSize).use { bufferedInput ->
                        progress(state.progressSnapshot(AppleHealthImportPhase.PARSING))
                        AppleHealthImportParser.parse(bufferedInput, state, options)
                    }
                }
            importLogs.addImportInfo(
                "Stage finished: Scanning export durationMs=${System.currentTimeMillis() - startedAtMillis} " +
                    "records=${parsed.parsedRecords} workouts=${parsed.parsedWorkouts} " +
                    "correlations=${parsed.parsedCorrelations} activitySummaries=${parsed.parsedActivitySummaries}",
            )
            return parsed
        }

        private interface StreamingAppleHealthProgressState : AppleHealthXmlEventConsumer {
            fun progressSnapshot(phase: AppleHealthImportPhase): AppleHealthImportProgress
        }

        private inner class StreamingAppleHealthScanAnalysisState(
            private val mindfulnessAvailable: Boolean,
            private val onProgress: suspend (AppleHealthImportProgress) -> Unit,
            private val importLogs: MutableList<String>,
        ) : StreamingAppleHealthProgressState {
            private val typeStats = linkedMapOf<String, MutableAppleImportTypeStats>()
            private val categoryStats = linkedMapOf<AppleHealthImportCategory, MutableAppleHealthImportCategorySummary>()
            private val diagnostics = mutableListOf<AppleHealthImportDiagnostic>()
            private val diagnosticSummaries = linkedMapOf<AppleHealthDiagnosticSummaryKey, MutableAppleHealthImportDiagnosticSummary>()
            private val rawDiagnosticTypes = mutableSetOf<String>()
            private var parsedRecords = 0
            private var parsedWorkouts = 0
            private var parsedCorrelations = 0
            private var parsedActivitySummaries = 0
            private var lastProgressParsedElements = 0

            override fun onParsedType(type: String) {
                typeStats.getOrPut(type) { MutableAppleImportTypeStats() }.parsed += 1
            }

            override fun onRecord(record: AppleRecord) {
                parsedRecords += 1
                val category = record.analysisCategory(mindfulnessAvailable)
                if (category != null) {
                    typeStats.getOrPut(record.type) { MutableAppleImportTypeStats() }.converted += 1
                    categoryStats.add(category = category, convertedRecords = 1)
                } else {
                    markUnsupported(record.type, "No direct Health Connect mapping is implemented for this Apple record type.")
                }
                maybeReportProgress()
            }

            override fun onWorkout(workout: AppleWorkout) {
                parsedWorkouts += 1
                typeStats.getOrPut(workout.workoutActivityType) { MutableAppleImportTypeStats() }.converted += 1
                categoryStats.add(
                    category = AppleHealthImportCategory.WORKOUTS,
                    convertedRecords = 1,
                    routeSessions = if (workout.routeReferences > 0) 1 else 0,
                )
                maybeReportProgress()
            }

            override fun onCorrelation(correlation: AppleCorrelation) {
                parsedCorrelations += 1
                if (correlation.type == AppleBloodPressureCorrelation) {
                    typeStats.getOrPut(correlation.type) { MutableAppleImportTypeStats() }.converted += 1
                    categoryStats.add(category = AppleHealthImportCategory.VITALS, convertedRecords = 1)
                } else {
                    markUnsupported(correlation.type, "Correlation type has no direct Health Connect import mapping.")
                }
                maybeReportProgress()
            }

            override fun onActivitySummary() {
                parsedActivitySummaries += 1
                markUnsupported(
                    appleType = "ActivitySummary",
                    detail = "Apple activity rings and stand hours have no direct writable Health Connect record.",
                )
                maybeReportProgress()
            }

            override fun progressSnapshot(phase: AppleHealthImportPhase): AppleHealthImportProgress =
                AppleHealthImportProgress(
                    phase = phase,
                    parsedRecords = parsedRecords,
                    parsedWorkouts = parsedWorkouts,
                    parsedCorrelations = parsedCorrelations,
                    parsedActivitySummaries = parsedActivitySummaries,
                    convertedRecords = typeStats.values.sumOf { it.converted },
                    importedRecords = 0,
                    duplicateSkippedRecords = 0,
                    notSelectedRecords = 0,
                    unsupportedElements = typeStats.values.sumOf { it.unsupported },
                    skippedRecords = 0,
                    failedRecords = 0,
                )

            fun typeSummaries(): List<AppleHealthImportTypeSummary> =
                typeStats.toTypeSummaries()

            fun diagnostics(): List<AppleHealthImportDiagnostic> =
                diagnostics.toList()

            fun diagnosticSummaries(): List<AppleHealthImportDiagnosticSummary> =
                diagnosticSummaries.values.map { it.toSummary() }

            fun categorySummaries(): List<AppleHealthImportCategorySummary> =
                categoryStats.toCategorySummaries()

            private fun markUnsupported(appleType: String, detail: String) {
                typeStats.getOrPut(appleType) { MutableAppleImportTypeStats() }.unsupported += 1
                val diagnostic = AppleHealthImportDiagnostic(
                    appleType = appleType,
                    targetType = null,
                    reasonCode = "unsupported",
                    timeRange = null,
                    unit = null,
                    value = null,
                    detail = detail,
                )
                diagnosticSummaries.add(diagnostic)
                if (rawDiagnosticTypes.add(appleType)) {
                    diagnostics += diagnostic
                }
            }

            private fun maybeReportProgress() {
                val parsedElements = parsedRecords + parsedWorkouts + parsedCorrelations + parsedActivitySummaries
                if (parsedElements - lastProgressParsedElements >= ProgressReportElementInterval) {
                    lastProgressParsedElements = parsedElements
                    runBlocking { onProgress(progressSnapshot(AppleHealthImportPhase.PARSING)) }
                    importLogs.addImportInfo(
                        "Analysis scan progress parsedElements=$parsedElements compatible=${typeStats.values.sumOf { it.converted }} " +
                            "unsupported=${typeStats.values.sumOf { it.unsupported }}",
                    )
                }
            }
        }

        private abstract inner class StreamingAppleHealthConversionState(
            private val converter: AppleHealthImportConverter,
            protected val onProgress: suspend (AppleHealthImportProgress) -> Unit,
            protected val importLogs: MutableList<String>,
            private val progressMutex: Mutex,
        ) : StreamingAppleHealthProgressState {
            private val bufferedRecords = mutableListOf<AppleRecord>()
            private val overlapDedupRecords = mutableListOf<AppleRecord>()
            private val bufferedWorkouts = mutableListOf<AppleWorkout>()

            // Parse-side counters are volatile so the concurrent batch writer coroutine can read
            // consistent progress totals without touching converter.typeStats (which the parse
            // thread mutates via getOrPut while the writer runs).
            @Volatile
            private var parsedRecords = 0

            @Volatile
            private var parsedWorkouts = 0

            @Volatile
            private var parsedCorrelations = 0

            @Volatile
            private var parsedActivitySummaries = 0

            @Volatile
            private var convertedRecords = 0

            @Volatile
            protected var notSelectedCount = 0

            private var lastProgressParsedElements = 0

            open val importedRecords: Int get() = 0
            protected open val duplicateSkippedCount: Int get() = 0
            protected open val writerFailedCount: Int get() = 0

            private fun log(message: String) {
                importLogs.addImportInfo(message)
            }

            override fun onParsedType(type: String) {
                converter.markParsed(type)
            }

            override fun onRecord(record: AppleRecord) {
                parsedRecords += 1
                converter.noteWorkoutOverlap(record)
                when {
                    converter.shouldBufferForOverlapDedup(record) -> overlapDedupRecords += record
                    converter.shouldBufferRecord(record) -> {
                        bufferedRecords += record
                        if (bufferedRecords.size >= BufferedRecordBatchSize) {
                            flushBufferedRecords()
                        }
                    }
                    else -> converter.convertStreamingRecord(record)?.let(::acceptConverted)
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
                converter.convertBufferedGroups(
                    records = emptyList(),
                    workouts = emptyList(),
                    correlations = listOf(correlation),
                    parsedActivitySummaries = 0,
                    emit = ::acceptConverted,
                )
                maybeReportProgress()
            }

            override fun onActivitySummary() {
                parsedActivitySummaries += 1
                maybeReportProgress()
            }

            fun finishBufferedGroups() {
                val convertedBefore = convertedRecords
                publishProgress(AppleHealthImportPhase.CONVERTING)
                log(
                    "Stage started: Converting final buffered groups bufferedRecords=${bufferedRecords.size} " +
                        "overlapDedupRecords=${overlapDedupRecords.size} workouts=${bufferedWorkouts.size} " +
                        "activitySummaries=$parsedActivitySummaries",
                )
                flushBufferedRecords()
                // Emit-based conversion: converted records flow straight into the batch pipeline
                // (with a periodic heartbeat log) instead of materializing one giant list. For
                // step/energy-heavy exports this phase covers hundreds of thousands of records.
                var emittedFromGroups = 0
                converter.convertBufferedGroups(
                    records = overlapDedupRecords,
                    workouts = bufferedWorkouts,
                    correlations = emptyList(),
                    parsedActivitySummaries = parsedActivitySummaries,
                ) { converted ->
                    acceptConverted(converted)
                    emittedFromGroups += 1
                    if (emittedFromGroups % BufferedGroupHeartbeatInterval == 0) {
                        log("Converting final buffered groups progress emitted=$emittedFromGroups")
                    }
                }
                overlapDedupRecords.clear()
                bufferedRecords.clear()
                bufferedWorkouts.clear()
                log(
                    "Stage finished: Converting final buffered groups convertedDelta=${convertedRecords - convertedBefore} " +
                        "convertedTotal=$convertedRecords",
                )
            }

            open fun finishConverted() = Unit

            override fun progressSnapshot(phase: AppleHealthImportPhase): AppleHealthImportProgress =
                AppleHealthImportProgress(
                    phase = phase,
                    parsedRecords = parsedRecords,
                    parsedWorkouts = parsedWorkouts,
                    parsedCorrelations = parsedCorrelations,
                    parsedActivitySummaries = parsedActivitySummaries,
                    convertedRecords = convertedRecords,
                    importedRecords = importedRecords,
                    duplicateSkippedRecords = duplicateSkippedCount,
                    notSelectedRecords = notSelectedCount,
                    unsupportedElements = converter.unsupportedCount,
                    skippedRecords = converter.skippedCount,
                    failedRecords = converter.invalidCount + writerFailedCount,
                )

            protected abstract fun onConverted(converted: ConvertedAppleRecord)

            protected fun mutableTypeStats(): MutableMap<String, MutableAppleImportTypeStats> =
                converter.typeStats

            protected fun logFromState(message: String) {
                log(message)
            }

            protected fun publishProgress(phase: AppleHealthImportPhase) {
                runBlocking { progressMutex.withLock { onProgress(progressSnapshot(phase)) } }
            }

            private fun flushBufferedRecords() {
                if (bufferedRecords.isEmpty()) return
                val recordCount = bufferedRecords.size
                val convertedBefore = convertedRecords
                publishProgress(AppleHealthImportPhase.CONVERTING)
                log("Stage started: Converting buffered records count=$recordCount")
                converter.convertBufferedGroups(
                    records = bufferedRecords,
                    workouts = emptyList(),
                    correlations = emptyList(),
                    parsedActivitySummaries = 0,
                    emit = ::acceptConverted,
                )
                bufferedRecords.clear()
                log(
                    "Stage finished: Converting buffered records count=$recordCount " +
                        "convertedDelta=${convertedRecords - convertedBefore} convertedTotal=$convertedRecords",
                )
            }

            private fun acceptConverted(converted: ConvertedAppleRecord) {
                convertedRecords += 1
                onConverted(converted)
            }

            private fun maybeReportProgress() {
                val parsedElements = parsedRecords + parsedWorkouts + parsedCorrelations + parsedActivitySummaries
                if (parsedElements - lastProgressParsedElements >= ProgressReportElementInterval) {
                    lastProgressParsedElements = parsedElements
                    publishProgress(AppleHealthImportPhase.PARSING)
                }
            }
        }

        private inner class StreamingAppleHealthWritingState(
            converter: AppleHealthImportConverter,
            onProgress: suspend (AppleHealthImportProgress) -> Unit,
            importLogs: MutableList<String>,
            progressMutex: Mutex,
            private val selectedCategories: Set<AppleHealthImportCategory>,
            private val writer: ConvertedBatchWriter,
            private val sendBatch: (List<ConvertedAppleRecord>) -> Unit,
        ) : StreamingAppleHealthConversionState(converter, onProgress, importLogs, progressMutex) {
            private val convertedBatch = mutableListOf<ConvertedAppleRecord>()
            private val categoryStats = linkedMapOf<AppleHealthImportCategory, MutableAppleHealthImportCategorySummary>()

            override val importedRecords: Int
                get() = writer.importedCount.get()

            override val duplicateSkippedCount: Int
                get() = writer.duplicateCount.get()

            override val writerFailedCount: Int
                get() = writer.failedCount.get()

            override fun onConverted(converted: ConvertedAppleRecord) {
                categoryStats.add(converted)
                if (converted.importCategory() !in selectedCategories) {
                    mutableTypeStats().getOrPut(converted.appleType) { MutableAppleImportTypeStats() }.notSelected += 1
                    notSelectedCount += 1
                    return
                }
                convertedBatch += converted
                if (convertedBatch.size >= ConvertedBatchSize) {
                    sendConvertedBatch()
                }
            }

            override fun finishConverted() {
                sendConvertedBatch()
            }

            fun categorySummaries(): List<AppleHealthImportCategorySummary> =
                categoryStats.toCategorySummaries()

            private fun sendConvertedBatch() {
                if (convertedBatch.isEmpty()) return
                sendBatch(convertedBatch.toList())
                convertedBatch.clear()
            }
        }

        /**
         * Consumes converted-record batches from the import pipeline and performs the Health
         * Connect I/O (duplicate lookup + insert) concurrently with parsing/conversion.
         *
         * Runs as a single sequential coroutine: batch N's insert must complete before batch N+1's
         * duplicate lookup so cross-batch duplicates inside one export are detected. All state in
         * this class is owned by that coroutine; the atomics exist only so the parse thread can
         * read progress totals. Final per-type accounting is merged back into the converter's
         * typeStats via [mergeInto] after the writer has completed.
         */
        private inner class ConvertedBatchWriter(
            private val importLogs: MutableList<String>,
        ) {
            lateinit var publishProgress: suspend (AppleHealthImportPhase) -> Unit
            private val writerTypeStats = linkedMapOf<String, MutableAppleImportTypeStats>()
            private val serviceDiagnostics = mutableListOf<AppleHealthImportDiagnostic>()
            private val serviceDiagnosticSummaries =
                linkedMapOf<AppleHealthDiagnosticSummaryKey, MutableAppleHealthImportDiagnosticSummary>()
            val importedCount = AtomicInteger(0)
            val duplicateCount = AtomicInteger(0)
            val failedCount = AtomicInteger(0)

            suspend fun process(batch: List<ConvertedAppleRecord>) {
                if (batch.isEmpty()) return
                val batchCount = batch.size
                publishProgress(AppleHealthImportPhase.CHECKING_DUPLICATES)
                log("Stage started: Checking duplicates batchRecords=$batchCount")
                val duplicateDiagnostics = mutableListOf<AppleHealthImportDiagnostic>()
                val duplicateSummaries = linkedMapOf<AppleHealthDiagnosticSummaryKey, MutableAppleHealthImportDiagnosticSummary>()
                val deduplicated = batch.deduplicateWithinImport(
                    diagnostics = duplicateDiagnostics,
                    diagnosticSummaries = duplicateSummaries,
                    typeStats = writerTypeStats,
                )
                val inFileDuplicates = batch.size - deduplicated.size
                if (inFileDuplicates > 0) {
                    duplicateCount.addAndGet(inFileDuplicates)
                    log("Skipped duplicate records inside export count=$inFileDuplicates")
                }
                duplicateDiagnostics.forEach(::addDiagnostic)
                val existingClientRecordIds = findExistingClientRecordIds(deduplicated, importLogs)
                val toInsert = deduplicated.filterNot { converted ->
                    val clientRecordId = converted.clientRecordId
                    val duplicate = clientRecordId != null && clientRecordId in existingClientRecordIds
                    if (duplicate) {
                        addDiagnostic(
                            converted.diagnostic(
                                "duplicate_existing",
                                "A matching Health Connect clientRecordId already exists.",
                            ),
                        )
                        writerTypeStats.getOrPut(converted.appleType) { MutableAppleImportTypeStats() }.duplicateSkipped += 1
                        duplicateCount.incrementAndGet()
                    }
                    duplicate
                }
                val existingDuplicates = deduplicated.size - toInsert.size
                if (existingDuplicates > 0) {
                    log("Skipped records already present in Health Connect count=$existingDuplicates")
                }
                log(
                    "Stage finished: Checking duplicates batchRecords=$batchCount unique=${deduplicated.size} " +
                        "toInsert=${toInsert.size} inFileDuplicates=$inFileDuplicates existingDuplicates=$existingDuplicates",
                )

                publishProgress(AppleHealthImportPhase.WRITING)
                log("Stage started: Writing records attempted=${toInsert.size}")
                val insertionResult = insertConvertedRecords(toInsert, ::addDiagnostic, writerTypeStats, importLogs)
                importedCount.addAndGet(insertionResult.imported)
                duplicateCount.addAndGet(insertionResult.duplicates)
                failedCount.addAndGet(insertionResult.failed)
                log(
                    "Stage finished: Writing records attempted=${toInsert.size} imported=${insertionResult.imported} " +
                        "duplicates=${insertionResult.duplicates} failed=${insertionResult.failed}",
                )
                publishProgress(AppleHealthImportPhase.WRITING)
            }

            fun diagnostics(): List<AppleHealthImportDiagnostic> = serviceDiagnostics.toList()

            fun diagnosticSummaries(): List<AppleHealthImportDiagnosticSummary> =
                serviceDiagnosticSummaries.values.map { it.toSummary() }

            fun mergeInto(typeStats: MutableMap<String, MutableAppleImportTypeStats>) {
                writerTypeStats.forEach { (type, stats) ->
                    val target = typeStats.getOrPut(type) { MutableAppleImportTypeStats() }
                    target.imported += stats.imported
                    target.duplicateSkipped += stats.duplicateSkipped
                    target.failed += stats.failed
                }
            }

            private fun addDiagnostic(diagnostic: AppleHealthImportDiagnostic) {
                serviceDiagnostics.addDiagnostic(diagnostic, serviceDiagnosticSummaries)
            }

            private fun log(message: String) {
                importLogs.addImportInfo(message)
            }
        }

        private suspend fun findExistingClientRecordIds(
            records: List<ConvertedAppleRecord>,
            importLogs: MutableList<String>,
        ): Set<String> {
            // Chunks are disjoint (recordType, timeRange) queries whose results are unioned, so
            // they can run concurrently. Parallelism stays low to respect Health Connect rate limits.
            val queries = records
                .filter { it.clientRecordId != null }
                .groupBy { it.recordType }
                .flatMap { (recordType, grouped) ->
                    grouped.chunkForDuplicateCheck(MaxDuplicateCheckSpanSeconds).map { chunk -> recordType to chunk }
                }
            val semaphore = Semaphore(MaxConcurrentDuplicateCheckQueries)
            return coroutineScope {
                queries.map { (recordType, chunk) ->
                    async {
                        semaphore.withPermit {
                            val wantedIds = chunk.mapNotNullTo(mutableSetOf()) { it.clientRecordId }
                            val start = chunk.minOfOrNull { it.sourceTimeRange.start }?.minusSeconds(1)
                                ?: return@withPermit emptySet()
                            val end = chunk.maxOfOrNull { it.sourceTimeRange.end }?.plusSeconds(1)
                                ?: return@withPermit emptySet()
                            runCatching {
                                importRepository.findMatchingImportedClientRecordIds(
                                    recordType = recordType,
                                    start = start,
                                    end = end,
                                    wantedIds = wantedIds,
                                )
                            }.getOrElse { error ->
                                importLogs += "${Instant.now()} [ERROR] Existing clientRecordId lookup failed " +
                                    "recordType=${recordType.qualifiedName} wanted=${wantedIds.size} range=$start..$end\n" +
                                    AppleHealthImportErrorFormatter.details(error)
                                emptySet()
                            }
                        }
                    }
                }.awaitAll().flatMapTo(mutableSetOf()) { it }
            }
        }

        private suspend fun insertConvertedRecords(
            records: List<ConvertedAppleRecord>,
            addDiagnostic: (AppleHealthImportDiagnostic) -> Unit,
            typeStats: MutableMap<String, MutableAppleImportTypeStats>,
            importLogs: MutableList<String>,
        ): AppleHealthInsertionResult {
            if (records.isEmpty()) return AppleHealthInsertionResult()
            val batchResult = runCatching {
                importRepository.insertImportedRecords(records.map { it.record })
            }
            if (batchResult.isSuccess) {
                records.forEach { converted ->
                    typeStats.getOrPut(converted.appleType) { MutableAppleImportTypeStats() }.imported += 1
                }
                return AppleHealthInsertionResult(imported = records.size)
            }
            batchResult.exceptionOrNull()?.let { error ->
                importLogs += "${Instant.now()} [ERROR] Batch insert failed count=${records.size}; retrying individually\n" +
                    AppleHealthImportErrorFormatter.details(error)
            }

            return records.fold(AppleHealthInsertionResult()) { result, converted ->
                runCatching { importRepository.insertImportedRecords(listOf(converted.record)) }
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
                                addDiagnostic(
                                    converted.diagnostic(
                                        "duplicate_rejected",
                                        "Health Connect rejected this as an existing clientRecordId.",
                                    ),
                                )
                                result.copy(duplicates = result.duplicates + 1)
                            } else {
                                stats.failed += 1
                                importLogs += "${Instant.now()} [ERROR] Record insert failed " +
                                    "appleType=${converted.appleType} target=${converted.targetType} " +
                                    "timeRange=${converted.sourceTimeRange}\n" +
                                    AppleHealthImportErrorFormatter.details(error)
                                addDiagnostic(
                                    converted.diagnostic(
                                        reasonCode = "insert_failed",
                                        detail = AppleHealthImportErrorFormatter.details(error),
                                    ),
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

private data class MutableAppleHealthImportCategorySummary(
    val category: AppleHealthImportCategory,
    var convertedRecords: Int = 0,
    var routeSessions: Int = 0,
) {
    fun toSummary(): AppleHealthImportCategorySummary =
        AppleHealthImportCategorySummary(
            category = category,
            convertedRecords = convertedRecords,
            routeSessions = routeSessions,
        )
}

private fun MutableMap<AppleHealthImportCategory, MutableAppleHealthImportCategorySummary>.add(
    converted: ConvertedAppleRecord,
) {
    val category = converted.importCategory()
    add(
        category = category,
        convertedRecords = 1,
        routeSessions = if (converted.hasExerciseRoute()) 1 else 0,
    )
}

private fun MutableMap<AppleHealthImportCategory, MutableAppleHealthImportCategorySummary>.add(
    category: AppleHealthImportCategory,
    convertedRecords: Int,
    routeSessions: Int = 0,
) {
    val summary = getOrPut(category) { MutableAppleHealthImportCategorySummary(category) }
    summary.convertedRecords += convertedRecords
    summary.routeSessions += routeSessions
}

private fun AppleRecord.analysisCategory(mindfulnessAvailable: Boolean): AppleHealthImportCategory? =
    when (type) {
        AppleStepCount,
        AppleDistanceWalkingRunning,
        AppleDistanceCycling,
        AppleDistanceSwimming,
        AppleDistanceWheelchair,
        AppleActiveEnergyBurned,
        AppleBasalEnergyBurned,
        AppleFlightsClimbed,
        AppleElevationAscended,
        ApplePushCount,
        AppleWalkingSpeed,
        -> AppleHealthImportCategory.ACTIVITY
        AppleHeartRate,
        AppleRestingHeartRate,
        -> AppleHealthImportCategory.HEART
        AppleSleepAnalysis -> AppleHealthImportCategory.SLEEP
        AppleBodyMass,
        AppleHeight,
        AppleBodyFatPercentage,
        AppleLeanBodyMass,
        AppleBoneMass,
        AppleBodyWaterMass,
        -> AppleHealthImportCategory.BODY
        AppleBloodPressureSystolic,
        AppleBloodPressureDiastolic,
        AppleOxygenSaturation,
        AppleRespiratoryRate,
        AppleBodyTemperature,
        AppleBloodGlucose,
        AppleVo2Max,
        -> AppleHealthImportCategory.VITALS
        AppleBasalBodyTemperature -> AppleHealthImportCategory.CYCLE
        AppleDietaryWater -> AppleHealthImportCategory.HYDRATION
        in AppleNutritionTypes -> AppleHealthImportCategory.NUTRITION
        AppleMindfulSession -> AppleHealthImportCategory.MINDFULNESS.takeIf { mindfulnessAvailable }
        in AppleCycleCategoryTypes -> AppleHealthImportCategory.CYCLE
        else -> null
    }

private fun Map<AppleHealthImportCategory, MutableAppleHealthImportCategorySummary>.toCategorySummaries():
    List<AppleHealthImportCategorySummary> =
    AppleHealthImportCategory.entries
        .mapNotNull { category -> this[category]?.toSummary() }
        .filter { it.convertedRecords > 0 }

private fun MutableList<String>.addImportInfo(message: String) {
    add("${Instant.now()} [INFO] $message")
    debugImportLog(message)
}

private fun debugImportLog(message: String) {
    runCatching { Log.d(AppleHealthImportWorker.LogTag, message) }
}

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
    // Summaries stay complete (grouped counts), but the raw list is capped so re-importing an
    // already-imported export cannot accumulate one diagnostic per record and OOM the report builder.
    diagnosticSummaries.add(diagnostic)
    if (size < MaxRawDiagnostics) {
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
        .sortedWith(
            compareByDescending<Map.Entry<String, MutableAppleImportTypeStats>> { it.value.parsed }
                .thenBy { it.key },
        )
        .map { (type, stats) ->
            AppleHealthImportTypeSummary(
                appleType = type,
                parsed = stats.parsed,
                converted = stats.converted,
                imported = stats.imported,
                duplicateSkipped = stats.duplicateSkipped,
                notSelected = stats.notSelected,
                unsupported = stats.unsupported,
                skipped = stats.skipped,
                failed = stats.failed,
            )
        }

private fun buildReportText(
    parsed: AppleParsedExport,
    imported: Int,
    selectedCategories: Set<AppleHealthImportCategory>?,
    summaries: List<AppleHealthImportTypeSummary>,
    categorySummaries: List<AppleHealthImportCategorySummary>,
    diagnostics: List<AppleHealthImportDiagnostic>,
    diagnosticSummaries: List<AppleHealthImportDiagnosticSummary>,
    importLogs: List<String>,
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
    appendLine("Not selected: ${summaries.sumOf { it.notSelected }}")
    appendLine("Unsupported: ${summaries.sumOf { it.unsupported }}")
    appendLine("Skipped: ${summaries.sumOf { it.skipped }}")
    appendLine("Failed: ${summaries.sumOf { it.failed }}")
    if (selectedCategories != null) {
        appendLine("Selected categories: ${selectedCategories.joinToString { it.reportName }}")
    }
    appendLine()
    appendLine("Logs")
    if (importLogs.isEmpty()) {
        appendLine("No import log entries were recorded.")
    } else {
        importLogs.forEach { entry -> appendLine(entry) }
    }
    appendLine()
    appendLine("By Import Category")
    if (categorySummaries.isEmpty()) {
        appendLine("No Health Connect-compatible categories were detected.")
    } else {
        categorySummaries.forEach { summary ->
            val routeText = if (summary.routeSessions > 0) ", routeSessions=${summary.routeSessions}" else ""
            appendLine("- ${summary.category.reportName}: converted=${summary.convertedRecords}$routeText")
        }
    }
    appendLine()
    appendLine("By Apple Type")
    summaries.forEach { summary ->
        appendLine(
            "- ${summary.appleType}: parsed=${summary.parsed}, converted=${summary.converted}, " +
                "imported=${summary.imported}, duplicate=${summary.duplicateSkipped}, " +
                "notSelected=${summary.notSelected}, unsupported=${summary.unsupported}, " +
                "skipped=${summary.skipped}, failed=${summary.failed}",
        )
    }
    appendLine()
    appendLine("Diagnostic Summary")
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
    }
    appendLine()
    appendLine("Raw Diagnostic Log")
    if (diagnostics.isEmpty()) {
        appendLine("No raw diagnostics were recorded.")
    } else {
        if (diagnostics.size >= MaxRawDiagnostics) {
            appendLine(
                "Raw diagnostics were capped at $MaxRawDiagnostics per source; " +
                    "see Diagnostic Summary above for complete counts.",
            )
        }
        diagnostics.forEachIndexed { index, diagnostic ->
            appendLine(
                "${index + 1}. reason=${diagnostic.reasonCode}; appleType=${diagnostic.appleType}; " +
                    "target=${diagnostic.targetType ?: "none"}; timeRange=${diagnostic.timeRange ?: "none"}; " +
                    "unit=${diagnostic.unit ?: "none"}; value=${diagnostic.value ?: "none"}; detail=${diagnostic.detail}",
            )
        }
    }
}

private const val ConvertedBatchSize = 300
private const val BufferedRecordBatchSize = 2_000
private const val MaxRawDiagnostics = 1_000
private const val BatchChannelCapacity = 2
private const val MaxConcurrentDuplicateCheckQueries = 4
private const val BufferedGroupHeartbeatInterval = 10_000
private const val ProgressReportElementInterval = 5_000
private const val MaxDuplicateCheckSpanSeconds = 6L * 60L * 60L
private const val ImportInputBufferSize = 256 * 1024

private fun List<ConvertedAppleRecord>.chunkForDuplicateCheck(maxSpanSeconds: Long): List<List<ConvertedAppleRecord>> {
    if (isEmpty()) return emptyList()
    val sorted = sortedBy { it.sourceTimeRange.start }
    val chunks = mutableListOf<MutableList<ConvertedAppleRecord>>()
    var current = mutableListOf(sorted.first())
    var chunkStart = sorted.first().sourceTimeRange.start

    for (index in 1 until sorted.size) {
        val record = sorted[index]
        if (record.sourceTimeRange.start.epochSecond - chunkStart.epochSecond > maxSpanSeconds) {
            chunks += current
            current = mutableListOf(record)
            chunkStart = record.sourceTimeRange.start
        } else {
            current += record
        }
    }
    chunks += current
    return chunks
}
