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

class AppleHealthImportWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun getForegroundInfo(): ForegroundInfo =
        foregroundInfo(AppleHealthImportProgress())

    override suspend fun doWork(): Result {
        val uri = inputData.getString(KeyInputUri)?.let(Uri::parse)
            ?: return Result.failure(errorData("Missing Apple Health export URI."))
        setForeground(foregroundInfo(AppleHealthImportProgress()))

        return runCatching {
            val appContext = applicationContext
            val entryPoint = EntryPointAccessors.fromApplication(
                appContext,
                AppleHealthImportWorkerEntryPoint::class.java,
            )
            val service = entryPoint.appleHealthImportService()
            var lastNotificationUpdateMillis = 0L
            val result = service.importAppleHealthExport(uri) { progress ->
                setProgress(progress.toData())
                val now = System.currentTimeMillis()
                if (now - lastNotificationUpdateMillis >= ForegroundNotificationUpdateMillis ||
                    progress.phase != AppleHealthImportPhase.PARSING
                ) {
                    setForeground(foregroundInfo(progress))
                    lastNotificationUpdateMillis = now
                }
            }
            val reportPath = AppleHealthImportReportStore.write(appContext, result.shareableReportText)
            val completeProgress = result.toProgress(AppleHealthImportPhase.COMPLETE)
            setProgress(completeProgress.toData())
            Result.success(result.toOutputData(reportPath))
        }.getOrElse { error ->
            Result.failure(errorData(error.localizedMessage ?: "Apple Health import failed."))
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
        val contentText = applicationContext.getString(
            R.string.settings_apple_health_import_notification_text,
            applicationContext.getString(progress.phase.labelRes),
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
            .setProgress(0, 0, progress.phase != AppleHealthImportPhase.COMPLETE)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            ChannelId,
            applicationContext.getString(R.string.settings_apple_health_import_notification_channel),
            NotificationManager.IMPORTANCE_LOW,
        )
        applicationContext.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        const val UniqueWorkName = "apple_health_import"
        const val KeyInputUri = "input_uri"
        const val KeyError = "error"

        private const val KeyReportPath = "report_path"
        private const val KeyPhase = "phase"
        private const val KeyParsedRecords = "parsed_records"
        private const val KeyParsedWorkouts = "parsed_workouts"
        private const val KeyParsedCorrelations = "parsed_correlations"
        private const val KeyParsedActivitySummaries = "parsed_activity_summaries"
        private const val KeyConvertedRecords = "converted_records"
        private const val KeyImportedRecords = "imported_records"
        private const val KeyDuplicateSkippedRecords = "duplicate_skipped_records"
        private const val KeyUnsupportedElements = "unsupported_elements"
        private const val KeySkippedRecords = "skipped_records"
        private const val KeyFailedRecords = "failed_records"

        private const val ChannelId = "apple_health_imports"
        private const val NotificationId = 4071
        private const val RequestOpenApp = 4072
        private const val ForegroundNotificationUpdateMillis = 1_000L

        fun inputData(uri: Uri): Data =
            Data.Builder()
                .putString(KeyInputUri, uri.toString())
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
                unsupportedElements = data.getInt(KeyUnsupportedElements, 0),
                skippedRecords = data.getInt(KeySkippedRecords, 0),
                failedRecords = data.getInt(KeyFailedRecords, 0),
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
                unsupportedElements = progress.unsupportedElements,
                skippedRecords = progress.skippedRecords,
                failedRecords = progress.failedRecords,
                typeSummaries = emptyList(),
                diagnostics = emptyList(),
                shareableReportText = reportText,
            )
        }

        fun reportPathFromData(data: Data): String? =
            data.getString(KeyReportPath)

        fun errorData(message: String): Data =
            Data.Builder()
                .putString(KeyError, message)
                .build()

        private fun AppleHealthImportProgress.toData(reportPath: String? = null): Data {
            val builder = Data.Builder()
                .putString(KeyPhase, phase.name)
                .putInt(KeyParsedRecords, parsedRecords)
                .putInt(KeyParsedWorkouts, parsedWorkouts)
                .putInt(KeyParsedCorrelations, parsedCorrelations)
                .putInt(KeyParsedActivitySummaries, parsedActivitySummaries)
                .putInt(KeyConvertedRecords, convertedRecords)
                .putInt(KeyImportedRecords, importedRecords)
                .putInt(KeyDuplicateSkippedRecords, duplicateSkippedRecords)
                .putInt(KeyUnsupportedElements, unsupportedElements)
                .putInt(KeySkippedRecords, skippedRecords)
                .putInt(KeyFailedRecords, failedRecords)
            if (reportPath != null) {
                builder.putString(KeyReportPath, reportPath)
            }
            return builder.build()
        }

        private fun AppleHealthImportResult.toOutputData(reportPath: String): Data =
            toProgress(AppleHealthImportPhase.COMPLETE).toData(reportPath)

        private fun AppleHealthImportResult.toProgress(phase: AppleHealthImportPhase): AppleHealthImportProgress =
            AppleHealthImportProgress(
                phase = phase,
                parsedRecords = parsedRecords,
                parsedWorkouts = parsedWorkouts,
                parsedCorrelations = parsedCorrelations,
                parsedActivitySummaries = parsedActivitySummaries,
                convertedRecords = convertedRecords,
                importedRecords = importedRecords,
                duplicateSkippedRecords = duplicateSkippedRecords,
                unsupportedElements = unsupportedElements,
                skippedRecords = skippedRecords,
                failedRecords = failedRecords,
            )

    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppleHealthImportWorkerEntryPoint {
    fun appleHealthImportService(): AppleHealthImportService
}
