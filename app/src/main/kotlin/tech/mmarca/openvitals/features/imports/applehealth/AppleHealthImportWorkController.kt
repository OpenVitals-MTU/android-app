package tech.mmarca.openvitals.features.imports.applehealth

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
import java.util.UUID

@Singleton
class AppleHealthImportWorkController @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val workManager = WorkManager.getInstance(context)

    val workInfos: Flow<List<WorkInfo>> =
        workManager.getWorkInfosForUniqueWorkFlow(AppleHealthImportWorker.UniqueWorkName)

    fun enqueue(
        uri: Uri,
        selectedCategories: Set<AppleHealthImportCategory> = AllAppleHealthImportCategories,
        expectedSelectedRecords: Int = 0,
        expectedParsedElements: Int = 0,
    ): UUID {
        persistReadPermission(uri)
        val request = OneTimeWorkRequestBuilder<AppleHealthImportWorker>()
            .setInputData(
                AppleHealthImportWorker.inputData(
                    uri,
                    selectedCategories,
                    expectedSelectedRecords,
                    expectedParsedElements,
                ),
            )
            .build()
        workManager.enqueueUniqueWork(
            AppleHealthImportWorker.UniqueWorkName,
            ExistingWorkPolicy.KEEP,
            request,
        )
        return request.id
    }

    fun progressFor(workInfo: WorkInfo): AppleHealthImportProgress? =
        AppleHealthImportWorker.progressFromData(
            if (workInfo.state.isFinished) workInfo.outputData else workInfo.progress,
        )

    fun resultFor(workInfo: WorkInfo): AppleHealthImportResult? {
        val reportText = AppleHealthImportReportStore.read(
            AppleHealthImportWorker.reportPathFromData(workInfo.outputData),
        )
        return AppleHealthImportWorker.resultFromData(workInfo.outputData, reportText)
    }

    fun errorFor(workInfo: WorkInfo): String? {
        val reportError = AppleHealthImportReportStore.read(
            AppleHealthImportWorker.errorReportPathFromData(workInfo.outputData),
        )
        return reportError.ifBlank {
            workInfo.outputData.getString(AppleHealthImportWorker.KeyError)
        }
    }

    fun permissionDeniedFor(workInfo: WorkInfo): Boolean =
        workInfo.outputData.getBoolean(AppleHealthImportWorker.KeyPermissionDenied, false)

    fun persistReadPermission(uri: Uri) {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
    }
}
