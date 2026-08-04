package tech.mmarca.openvitals.features.activity.maps

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class OfflineMapImportWorkController @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val workManager = WorkManager.getInstance(context)

    val workInfos: Flow<List<WorkInfo>> =
        workManager.getWorkInfosForUniqueWorkFlow(OfflineMapImportWorker.UniqueWorkName)

    fun enqueue(uri: Uri) {
        persistReadPermission(uri)
        val request = OneTimeWorkRequestBuilder<OfflineMapImportWorker>()
            .setInputData(OfflineMapImportWorker.inputData(uri))
            .build()
        workManager.enqueueUniqueWork(
            OfflineMapImportWorker.UniqueWorkName,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    fun progressFor(workInfo: WorkInfo): OfflineMapImportProgress? =
        OfflineMapImportWorker.progressFromData(
            if (workInfo.state.isFinished) workInfo.outputData else workInfo.progress,
        )

    fun resultFor(workInfo: WorkInfo): OfflineMapImportResult? =
        OfflineMapImportWorker.resultFromData(workInfo.outputData)

    fun errorFor(workInfo: WorkInfo): String? =
        workInfo.outputData.getString(OfflineMapImportWorker.KeyError)

    private fun persistReadPermission(uri: Uri) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
    }
}
