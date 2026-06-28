package tech.mmarca.openvitals.features.activity.maps

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

class OfflineMapImportWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun getForegroundInfo(): ForegroundInfo =
        foregroundInfo(OfflineMapImportProgress())

    override suspend fun doWork(): Result {
        val uri = inputData.getString(KeyInputUri)?.let(Uri::parse)
            ?: return Result.failure(errorData("Missing offline map URI."))
        setForeground(foregroundInfo(OfflineMapImportProgress()))

        return try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                OfflineMapImportWorkerEntryPoint::class.java,
            )
            val repository = entryPoint.offlineMapRepository()
            var lastNotificationUpdateMillis = 0L
            val pack = repository.importMap(uri) { bytesCopied, totalBytes ->
                val progress = OfflineMapImportProgress(
                    phase = OfflineMapImportPhase.COPYING,
                    bytesCopied = bytesCopied,
                    totalBytes = totalBytes,
                )
                setProgress(progress.toData())
                val now = System.currentTimeMillis()
                if (now - lastNotificationUpdateMillis >= ForegroundNotificationUpdateMillis) {
                    setForeground(foregroundInfo(progress))
                    lastNotificationUpdateMillis = now
                }
            }
            val completeProgress = OfflineMapImportProgress(
                phase = OfflineMapImportPhase.COMPLETE,
                bytesCopied = pack.sizeBytes,
                totalBytes = pack.sizeBytes,
            )
            setProgress(completeProgress.toData())
            Result.success(pack.toOutputData())
        } catch (error: Throwable) {
            Result.failure(errorData(error.localizedMessage ?: "Offline map import failed."))
        } finally {
            releaseReadPermission(uri)
        }
    }

    private fun foregroundInfo(progress: OfflineMapImportProgress): ForegroundInfo {
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

    private fun buildNotification(progress: OfflineMapImportProgress): Notification {
        createNotificationChannel()
        val contentIntent = PendingIntent.getActivity(
            applicationContext,
            RequestOpenApp,
            Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val phaseLabel = applicationContext.getString(progress.phase.labelRes)
        val contentText = progress.percent?.let { percent ->
            applicationContext.getString(
                R.string.settings_offline_maps_import_notification_text_with_percent,
                phaseLabel,
                percent,
            )
        } ?: applicationContext.getString(
            R.string.settings_offline_maps_import_notification_text,
            phaseLabel,
        )
        return NotificationCompat.Builder(applicationContext, ChannelId)
            .setSmallIcon(R.drawable.ic_stat_activity_recording)
            .setContentTitle(applicationContext.getString(R.string.settings_offline_maps_import_notification_title))
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setContentIntent(contentIntent)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .setOngoing(progress.phase != OfflineMapImportPhase.COMPLETE)
            .setAutoCancel(progress.phase == OfflineMapImportPhase.COMPLETE)
            .setSilent(true)
            .setProgress(100, progress.percent ?: 0, progress.percent == null)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            ChannelId,
            applicationContext.getString(R.string.settings_offline_maps_import_notification_channel),
            NotificationManager.IMPORTANCE_LOW,
        )
        applicationContext.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun releaseReadPermission(uri: Uri) {
        runCatching {
            applicationContext.contentResolver.releasePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
    }

    companion object {
        const val UniqueWorkName = "offline_map_import"
        const val KeyInputUri = "input_uri"
        const val KeyError = "error"

        private const val KeyPhase = "phase"
        private const val KeyBytesCopied = "bytes_copied"
        private const val KeyTotalBytes = "total_bytes"
        private const val KeyMapId = "map_id"
        private const val KeyDisplayName = "display_name"
        private const val KeySizeBytes = "size_bytes"
        private const val KeyFormat = "format"

        private const val ChannelId = "offline_map_imports"
        private const val NotificationId = 4081
        private const val RequestOpenApp = 4082
        private const val ForegroundNotificationUpdateMillis = 1_000L

        fun inputData(uri: Uri): Data =
            Data.Builder()
                .putString(KeyInputUri, uri.toString())
                .build()

        fun progressFromData(data: Data): OfflineMapImportProgress? {
            val phase = data.getString(KeyPhase)
                ?.let { value -> runCatching { OfflineMapImportPhase.valueOf(value) }.getOrNull() }
                ?: return null
            return OfflineMapImportProgress(
                phase = phase,
                bytesCopied = data.getLong(KeyBytesCopied, 0L),
                totalBytes = data.getLong(KeyTotalBytes, 0L),
            )
        }

        fun resultFromData(data: Data): OfflineMapImportResult? {
            val mapId = data.getString(KeyMapId) ?: return null
            val displayName = data.getString(KeyDisplayName) ?: return null
            return OfflineMapImportResult(
                mapId = mapId,
                displayName = displayName,
                sizeBytes = data.getLong(KeySizeBytes, 0L),
                format = data.getString(KeyFormat)
                    ?.let { value -> runCatching { OfflineMapPackFormat.valueOf(value) }.getOrNull() }
                    ?: OfflineMapPackFormat.PMTILES,
            )
        }

        fun errorData(message: String): Data =
            Data.Builder()
                .putString(KeyError, message)
                .build()

        private fun OfflineMapImportProgress.toData(): Data =
            Data.Builder()
                .putString(KeyPhase, phase.name)
                .putLong(KeyBytesCopied, bytesCopied)
                .putLong(KeyTotalBytes, totalBytes)
                .build()

        private fun OfflineMapPack.toOutputData(): Data =
            Data.Builder()
                .putString(KeyPhase, OfflineMapImportPhase.COMPLETE.name)
                .putLong(KeyBytesCopied, sizeBytes)
                .putLong(KeyTotalBytes, sizeBytes)
                .putString(KeyMapId, id)
                .putString(KeyDisplayName, displayName)
                .putLong(KeySizeBytes, sizeBytes)
                .putString(KeyFormat, format.name)
                .build()
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface OfflineMapImportWorkerEntryPoint {
    fun offlineMapRepository(): OfflineMapRepository
}
