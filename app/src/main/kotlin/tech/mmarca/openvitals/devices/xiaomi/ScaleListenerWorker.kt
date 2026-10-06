package tech.mmarca.openvitals.devices.xiaomi

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.Duration
import java.time.Instant
import tech.mmarca.openvitals.data.repository.contract.ScaleWeighInRepository

/**
 * The scale listener's safety net, run by `WorkManager`: arms the scan
 * again in case the system dropped it, retries the weigh-ins Health
 * Connect has not taken, and clears out the ones that were only half
 * heard. Not a foreground worker: nobody asked for this
 * run, so it must not take the app's single foreground slot.
 */
class ScaleListenerWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            ScaleListenerWorkerEntryPoint::class.java,
        )
        // The scale was removed while this run was queued.
        if (!entryPoint.xiaomiScaleStore().config.value.hasKey) {
            WorkManager.getInstance(applicationContext).cancelUniqueWork(XiaomiScaleListener.WORK_NAME)
            return Result.success()
        }
        entryPoint.xiaomiScaleListener().arm()
        entryPoint.scaleWeighInWriter().writePending()
        entryPoint.scaleWeighInRepository().dropWithoutWeight(Instant.now().minus(HalfHeardKeptFor))
        return Result.success()
    }

    private companion object {
        /** Both frames of a weigh-in come within seconds. An hour on, the missing one is not coming. */
        val HalfHeardKeptFor: Duration = Duration.ofHours(1)
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ScaleListenerWorkerEntryPoint {
    fun xiaomiScaleStore(): XiaomiScaleStore
    fun xiaomiScaleListener(): XiaomiScaleListener
    fun scaleWeighInWriter(): ScaleWeighInWriter
    fun scaleWeighInRepository(): ScaleWeighInRepository
}
