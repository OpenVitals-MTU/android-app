package tech.mmarca.openvitals.features.imports.applehealth

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import tech.mmarca.openvitals.MainActivity
import tech.mmarca.openvitals.R
import java.time.Instant

class AppleHealthImportWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun getForegroundInfo(): ForegroundInfo =
        foregroundInfo(
            AppleHealthImportProgress(
                expectedSelectedRecords = expectedSelectedRecordsFromData(inputData),
                expectedParsedElements = expectedParsedElementsFromData(inputData),
            ),
        )

    override suspend fun doWork(): Result {
        val workerLogs = mutableListOf<String>()
        fun log(message: String) {
            workerLogs += "${Instant.now()} [WORKER] $message"
        }
        fun logError(message: String, error: Throwable) {
            workerLogs += "${Instant.now()} [WORKER][ERROR] $message\n${AppleHealthImportErrorFormatter.details(error)}"
        }

        val uri = inputData.getString(KeyInputUri)?.let(Uri::parse)
        if (uri == null) {
            Log.w(LogTag, "Missing Apple Health export URI.")
            val error = IllegalArgumentException("Missing Apple Health export URI.")
            logError("Missing Apple Health export URI.", error)
            return Result.failure(errorData(applicationContext, error, workerLogs))
        }
        val expectedSelectedRecords = expectedSelectedRecordsFromData(inputData)
        val expectedParsedElements = expectedParsedElementsFromData(inputData)
        log(
            "Worker started uri=$uri expectedSelectedRecords=$expectedSelectedRecords " +
                "expectedParsedElements=$expectedParsedElements",
        )
        setForeground(
            foregroundInfo(
                AppleHealthImportProgress(
                    expectedSelectedRecords = expectedSelectedRecords,
                    expectedParsedElements = expectedParsedElements,
                ),
            ),
        )
        log("Foreground notification initialized")

        return runCatching {
            val appContext = applicationContext
            val entryPoint = EntryPointAccessors.fromApplication(
                appContext,
                AppleHealthImportWorkerEntryPoint::class.java,
            )
            val service = entryPoint.appleHealthImportService()
            log("AppleHealthImportService resolved")
            var lastProgressUpdateMillis = 0L
            var lastProgressPhase: AppleHealthImportPhase? = null
            var lastLoggedProgressPhase: AppleHealthImportPhase? = null
            var lastProgressHeartbeatMillis = 0L
            var lastNotificationUpdateMillis = 0L
            var lastNotificationPhase: AppleHealthImportPhase? = null
            val selectedCategories = selectedCategoriesFromData(inputData)
            log("Selected categories=${selectedCategories.joinToString { it.name }}")
            val fingerprint = service.fingerprintOf(uri)
            val sourceKey = AppleHealthImportCheckpointStore.sourceKey(uri, fingerprint)
            val storedCheckpoint = AppleHealthImportCheckpointStore.load(
                appContext,
                sourceKey,
                selectedCategories,
            )
            val resumeCheckpoint = storedCheckpoint ?: AppleHealthImportCheckpoint(
                sourceKey = sourceKey,
                selectedCategories = selectedCategories,
                committedSelectedRecords = 0,
                importedRecords = 0,
                duplicateSkippedRecords = 0,
                failedRecords = 0,
                typeStats = emptyMap(),
            )
            if (storedCheckpoint != null) {
                log(
                    "Resume checkpoint loaded committedSelectedRecords=${storedCheckpoint.committedSelectedRecords} " +
                        "imported=${storedCheckpoint.importedRecords} duplicates=${storedCheckpoint.duplicateSkippedRecords} " +
                        "failed=${storedCheckpoint.failedRecords}",
                )
            } else {
                log("No matching resume checkpoint found")
            }
            log("Stage started: Copying Apple Health export into app storage")
            val stagedExport = AppleHealthImportStagingStore.stage(appContext, uri, fingerprint)
            log(
                "Stage finished: Copying Apple Health export into app storage " +
                    "bytes=${stagedExport.bytes} reused=${stagedExport.reused}",
            )
            val result = service.importAppleHealthExport(
                uri,
                selectedCategories,
                progress = { progress ->
                    val importProgress = progress.copy(
                        expectedSelectedRecords = expectedSelectedRecords,
                        expectedParsedElements = expectedParsedElements,
                    )
                    val now = System.currentTimeMillis()
                    if (now - lastProgressUpdateMillis >= WorkManagerProgressUpdateMillis ||
                        importProgress.phase != lastProgressPhase
                    ) {
                        setProgress(importProgress.toData())
                        lastProgressUpdateMillis = now
                        lastProgressPhase = importProgress.phase
                    }
                    if (importProgress.phase != lastLoggedProgressPhase) {
                        log(
                            "Progress phase changed phase=${importProgress.phase.name} percent=${importProgress.percent ?: -1} " +
                                "selectedPrepared=${importProgress.selectedPreparedRecords}/$expectedSelectedRecords " +
                                "scanned=${importProgress.parsedElements} converted=${importProgress.convertedRecords} " +
                                "imported=${importProgress.importedRecords} duplicates=${importProgress.duplicateSkippedRecords} " +
                                "notSelected=${importProgress.notSelectedRecords} unsupported=${importProgress.unsupportedElements} " +
                                "skipped=${importProgress.skippedRecords} failed=${importProgress.failedRecords}",
                        )
                        lastLoggedProgressPhase = importProgress.phase
                    }
                    if (now - lastProgressHeartbeatMillis >= ProgressHeartbeatMillis) {
                        log(
                            "Progress heartbeat phase=${importProgress.phase.name} " +
                                "scanned=${importProgress.parsedElements}/$expectedParsedElements " +
                                "selectedPrepared=${importProgress.selectedPreparedRecords}/$expectedSelectedRecords " +
                                "imported=${importProgress.importedRecords} duplicates=${importProgress.duplicateSkippedRecords} " +
                                "notSelected=${importProgress.notSelectedRecords} failed=${importProgress.failedRecords}",
                        )
                        lastProgressHeartbeatMillis = now
                    }
                    if (now - lastNotificationUpdateMillis >= ForegroundNotificationUpdateMillis ||
                        importProgress.phase != lastNotificationPhase
                    ) {
                        setForeground(foregroundInfo(importProgress))
                        lastNotificationUpdateMillis = now
                        lastNotificationPhase = importProgress.phase
                    }
                },
                resumeCheckpoint = resumeCheckpoint,
                onCheckpoint = { checkpoint ->
                    AppleHealthImportCheckpointStore.save(appContext, checkpoint)
                },
                stagedFile = stagedExport.file,
            )
            log("Import service completed imported=${result.importedRecords} failed=${result.failedRecords}")
            val buildingReportProgress = result.toProgress(
                AppleHealthImportPhase.BUILDING_REPORT,
                expectedSelectedRecords,
                expectedParsedElements,
            )
            setProgress(buildingReportProgress.toData())
            setForeground(foregroundInfo(buildingReportProgress))
            log("Stage started: Building downloadable report")
            log("Stage finished: Building downloadable report")
            log("Stage started: Writing downloadable report file")
            // The store path is deterministic, so the completion log can be added to the worker
            // logs before the (potentially multi-MB) report is rendered and written exactly once.
            val reportPath = AppleHealthImportReportStore.reportPath(appContext)
            log("Stage finished: Writing downloadable report file path=$reportPath")
            val finalReportText = result.shareableReportText.withWorkerLogs(workerLogs)
            AppleHealthImportReportStore.write(appContext, finalReportText)
            val finalResult = result.copy(shareableReportText = finalReportText)
            val completeProgress = finalResult.toProgress(
                AppleHealthImportPhase.COMPLETE,
                expectedSelectedRecords,
                expectedParsedElements,
            )
            setProgress(completeProgress.toData())
            AppleHealthImportCheckpointStore.clear(appContext)
            val stagedExportDeleted = AppleHealthImportStagingStore.clear(appContext)
            log("Cleared staged Apple Health import state stagedExportDeleted=$stagedExportDeleted")
            if (!stagedExportDeleted) {
                Log.w(LogTag, "Unable to delete every staged Apple Health export file after successful import")
            }
            Result.success(finalResult.toOutputData(reportPath, expectedSelectedRecords, expectedParsedElements))
        }.getOrElse { error ->
            Log.e(LogTag, "Apple Health import failed", error)
            logError("Apple Health import failed.", error)
            Result.failure(errorData(applicationContext, error, workerLogs))
        }
    }

    private fun foregroundInfo(progress: AppleHealthImportProgress): ForegroundInfo {
        val notification = buildNotification(progress)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NotificationId,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            ForegroundInfo(NotificationId, notification)
        }
    }

    private fun buildNotification(progress: AppleHealthImportProgress): Notification {
        createNotificationChannel()
        val contentIntent = PendingIntent.getActivity(
            applicationContext,
            RequestOpenApp,
            Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val phaseText = applicationContext.getString(progress.phase.labelRes)
        val contentText = progress.percent?.let { percent ->
            if (progress.expectedParsedElements > 0) {
                applicationContext.getString(
                    R.string.settings_apple_health_import_notification_text_with_scan_percent,
                    percent,
                    phaseText,
                    progress.parsedElements,
                    progress.expectedParsedElements,
                    progress.importedRecords,
                )
            } else {
                applicationContext.getString(
                    R.string.settings_apple_health_import_notification_text_with_percent,
                    percent,
                    phaseText,
                    progress.selectedPreparedRecords,
                    progress.expectedSelectedRecords,
                    progress.importedRecords,
                )
            }
        } ?: applicationContext.getString(
            R.string.settings_apple_health_import_notification_text,
            phaseText,
            progress.parsedElements,
            progress.importedRecords,
        )
        return NotificationCompat.Builder(applicationContext, ChannelId)
            .setSmallIcon(R.drawable.ic_stat_activity_recording)
            .setContentTitle(applicationContext.getString(R.string.settings_apple_health_import_notification_title))
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setContentIntent(contentIntent)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .setOngoing(progress.phase != AppleHealthImportPhase.COMPLETE)
            .setAutoCancel(progress.phase == AppleHealthImportPhase.COMPLETE)
            .setSilent(true)
            .apply {
                val percent = progress.percent
                if (percent != null) {
                    setProgress(100, percent, false)
                } else {
                    setProgress(0, 0, progress.phase != AppleHealthImportPhase.COMPLETE)
                }
            }
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            ChannelId,
            applicationContext.getString(R.string.settings_apple_health_import_notification_channel),
            NotificationManager.IMPORTANCE_LOW,
        )
        applicationContext.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        const val LogTag = "AppleHealthImporter"
        const val UniqueWorkName = "apple_health_import"
        const val KeyInputUri = "input_uri"
        const val KeyError = "error"
        const val KeyPermissionDenied = "permission_denied"

        private const val KeyReportPath = "report_path"
        private const val KeyErrorReportPath = "error_report_path"
        private const val KeyPhase = "phase"
        private const val KeyParsedRecords = "parsed_records"
        private const val KeyParsedWorkouts = "parsed_workouts"
        private const val KeyParsedCorrelations = "parsed_correlations"
        private const val KeyParsedActivitySummaries = "parsed_activity_summaries"
        private const val KeyConvertedRecords = "converted_records"
        private const val KeyImportedRecords = "imported_records"
        private const val KeyDuplicateSkippedRecords = "duplicate_skipped_records"
        private const val KeyNotSelectedRecords = "not_selected_records"
        private const val KeyUnsupportedElements = "unsupported_elements"
        private const val KeySkippedRecords = "skipped_records"
        private const val KeyFailedRecords = "failed_records"
        private const val KeyWorkoutRoutesIncomplete = "workout_routes_incomplete"
        private const val KeySelectedCategories = "selected_categories"
        private const val KeyExpectedSelectedRecords = "expected_selected_records"
        private const val KeyExpectedParsedElements = "expected_parsed_elements"

        private const val ChannelId = "apple_health_imports"
        private const val NotificationId = 4071
        private const val RequestOpenApp = 4072
        private const val ForegroundNotificationUpdateMillis = 1_000L
        private const val WorkManagerProgressUpdateMillis = 1_000L
        private const val ProgressHeartbeatMillis = 30_000L
        private const val MaxInlineErrorCharacters = 2_000
        private const val MaxSummaryErrorCharacters = 1_000

        fun inputData(
            uri: Uri,
            selectedCategories: Set<AppleHealthImportCategory> = AllAppleHealthImportCategories,
            expectedSelectedRecords: Int = 0,
            expectedParsedElements: Int = 0,
        ): Data =
            Data.Builder()
                .putString(KeyInputUri, uri.toString())
                .putStringArray(KeySelectedCategories, selectedCategories.map { it.name }.toTypedArray())
                .putInt(KeyExpectedSelectedRecords, expectedSelectedRecords.coerceAtLeast(0))
                .putInt(KeyExpectedParsedElements, expectedParsedElements.coerceAtLeast(0))
                .build()

        fun progressFromData(data: Data): AppleHealthImportProgress? {
            val phase = data.getString(KeyPhase)
                ?.let { value -> runCatching { AppleHealthImportPhase.valueOf(value) }.getOrNull() }
                ?: return null
            return AppleHealthImportProgress(
                phase = phase,
                parsedRecords = data.getInt(KeyParsedRecords, 0),
                parsedWorkouts = data.getInt(KeyParsedWorkouts, 0),
                parsedCorrelations = data.getInt(KeyParsedCorrelations, 0),
                parsedActivitySummaries = data.getInt(KeyParsedActivitySummaries, 0),
                convertedRecords = data.getInt(KeyConvertedRecords, 0),
                importedRecords = data.getInt(KeyImportedRecords, 0),
                duplicateSkippedRecords = data.getInt(KeyDuplicateSkippedRecords, 0),
                notSelectedRecords = data.getInt(KeyNotSelectedRecords, 0),
                unsupportedElements = data.getInt(KeyUnsupportedElements, 0),
                skippedRecords = data.getInt(KeySkippedRecords, 0),
                failedRecords = data.getInt(KeyFailedRecords, 0),
                expectedSelectedRecords = expectedSelectedRecordsFromData(data),
                expectedParsedElements = expectedParsedElementsFromData(data),
            )
        }

        fun resultFromData(data: Data, reportText: String): AppleHealthImportResult? {
            val progress = progressFromData(data) ?: return null
            return AppleHealthImportResult(
                parsedRecords = progress.parsedRecords,
                parsedWorkouts = progress.parsedWorkouts,
                parsedCorrelations = progress.parsedCorrelations,
                parsedActivitySummaries = progress.parsedActivitySummaries,
                convertedRecords = progress.convertedRecords,
                importedRecords = progress.importedRecords,
                duplicateSkippedRecords = progress.duplicateSkippedRecords,
                notSelectedRecords = progress.notSelectedRecords,
                unsupportedElements = progress.unsupportedElements,
                skippedRecords = progress.skippedRecords,
                failedRecords = progress.failedRecords,
                workoutRoutesIncomplete = data.getBoolean(KeyWorkoutRoutesIncomplete, false),
                typeSummaries = emptyList(),
                diagnostics = emptyList(),
                shareableReportText = reportText,
            )
        }

        fun reportPathFromData(data: Data): String? =
            data.getString(KeyReportPath)

        fun errorReportPathFromData(data: Data): String? =
            data.getString(KeyErrorReportPath)

        fun errorData(context: Context, error: Throwable, workerLogs: List<String> = emptyList()): Data {
            val details = AppleHealthImportErrorFormatter.details(error)
            val fullReport = buildFailureReportText(error, workerLogs)
            val reportPath = runCatching {
                AppleHealthImportReportStore.writeFailure(context, fullReport)
            }.getOrNull()
            return Data.Builder()
                .putString(
                    KeyError,
                    if (reportPath != null) {
                        AppleHealthImportErrorFormatter.summary(error).inlineSummaryForWorkData()
                    } else {
                        details.inlineForWorkData()
                    }
                )
                .putBoolean(KeyPermissionDenied, AppleHealthImportErrorFormatter.isPermissionDenied(error))
                .apply {
                    if (reportPath != null) {
                        putString(KeyErrorReportPath, reportPath)
                    }
                }
                .build()
        }

        private fun String.inlineForWorkData(): String =
            if (length <= MaxInlineErrorCharacters) {
                this
            } else {
                take(MaxInlineErrorCharacters) +
                    "\n\n... error truncated because WorkManager output data is limited."
            }

        private fun String.inlineSummaryForWorkData(): String =
            if (length <= MaxSummaryErrorCharacters) {
                this
            } else {
                take(MaxSummaryErrorCharacters) +
                    "\n\n... full error stored in import error report."
            }

        private fun AppleHealthImportProgress.toData(
            reportPath: String? = null,
            workoutRoutesIncomplete: Boolean = false,
        ): Data {
            val builder = Data.Builder()
                .putString(KeyPhase, phase.name)
                .putInt(KeyParsedRecords, parsedRecords)
                .putInt(KeyParsedWorkouts, parsedWorkouts)
                .putInt(KeyParsedCorrelations, parsedCorrelations)
                .putInt(KeyParsedActivitySummaries, parsedActivitySummaries)
                .putInt(KeyConvertedRecords, convertedRecords)
                .putInt(KeyImportedRecords, importedRecords)
                .putInt(KeyDuplicateSkippedRecords, duplicateSkippedRecords)
                .putInt(KeyNotSelectedRecords, notSelectedRecords)
                .putInt(KeyUnsupportedElements, unsupportedElements)
                .putInt(KeySkippedRecords, skippedRecords)
                .putInt(KeyFailedRecords, failedRecords)
                .putBoolean(KeyWorkoutRoutesIncomplete, workoutRoutesIncomplete)
                .putInt(KeyExpectedSelectedRecords, expectedSelectedRecords)
                .putInt(KeyExpectedParsedElements, expectedParsedElements)
            if (reportPath != null) {
                builder.putString(KeyReportPath, reportPath)
            }
            return builder.build()
        }

        private fun AppleHealthImportResult.toOutputData(
            reportPath: String,
            expectedSelectedRecords: Int,
            expectedParsedElements: Int,
        ): Data =
            toProgress(
                phase = AppleHealthImportPhase.COMPLETE,
                expectedSelectedRecords = expectedSelectedRecords,
                expectedParsedElements = expectedParsedElements,
            ).toData(reportPath, workoutRoutesIncomplete)

        private fun AppleHealthImportResult.toProgress(
            phase: AppleHealthImportPhase,
            expectedSelectedRecords: Int,
            expectedParsedElements: Int,
        ): AppleHealthImportProgress =
            AppleHealthImportProgress(
                phase = phase,
                parsedRecords = parsedRecords,
                parsedWorkouts = parsedWorkouts,
                parsedCorrelations = parsedCorrelations,
                parsedActivitySummaries = parsedActivitySummaries,
                convertedRecords = convertedRecords,
                importedRecords = importedRecords,
                duplicateSkippedRecords = duplicateSkippedRecords,
                notSelectedRecords = notSelectedRecords,
                unsupportedElements = unsupportedElements,
                skippedRecords = skippedRecords,
                failedRecords = failedRecords,
                expectedSelectedRecords = expectedSelectedRecords,
                expectedParsedElements = expectedParsedElements,
            )

        private fun selectedCategoriesFromData(data: Data): Set<AppleHealthImportCategory> =
            data.getStringArray(KeySelectedCategories)
                ?.mapNotNull { name -> runCatching { AppleHealthImportCategory.valueOf(name) }.getOrNull() }
                ?.toSet()
                ?: AllAppleHealthImportCategories

        private fun expectedSelectedRecordsFromData(data: Data): Int =
            data.getInt(KeyExpectedSelectedRecords, 0).coerceAtLeast(0)

        private fun expectedParsedElementsFromData(data: Data): Int =
            data.getInt(KeyExpectedParsedElements, 0).coerceAtLeast(0)

    }
}

private fun String.withWorkerLogs(workerLogs: List<String>): String =
    buildString {
        appendLine(this@withWorkerLogs.trimEnd())
        appendLine()
        appendLine("Worker Logs")
        if (workerLogs.isEmpty()) {
            appendLine("No worker log entries were recorded.")
        } else {
            workerLogs.forEach { entry -> appendLine(entry) }
        }
    }

private fun buildFailureReportText(error: Throwable, workerLogs: List<String>): String =
    buildString {
        appendAppleHealthReportHeader()
        appendLine()
        appendLine("Summary")
        appendLine("Status: failed")
        appendLine("Error: ${AppleHealthImportErrorFormatter.summary(error)}")
        appendLine()
        appendLine("Logs")
        if (workerLogs.isEmpty()) {
            appendLine("No worker log entries were recorded before failure.")
        } else {
            workerLogs.forEach { entry -> appendLine(entry) }
        }
        appendLine()
        appendLine("Exception")
        appendLine(AppleHealthImportErrorFormatter.details(error))
    }

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppleHealthImportWorkerEntryPoint {
    fun appleHealthImportService(): AppleHealthImportService
}
