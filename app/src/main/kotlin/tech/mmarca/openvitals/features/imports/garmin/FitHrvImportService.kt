package tech.mmarca.openvitals.features.imports.garmin

import android.util.Log
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import tech.mmarca.openvitals.data.repository.AppleHealthImportRepository
import tech.mmarca.openvitals.data.repository.contract.HealthRepository
import tech.mmarca.openvitals.features.imports.applehealth.isDuplicateClientRecordFailure
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.FitHrvReading
import tech.mmarca.openvitals.healthconnect.HealthConnectRateLimitBackoff

/** File counts from one HRV write flush; rate-limited files are left unattempted. */
internal data class FitHrvImportOutcome(
    val importedFiles: Int = 0,
    val failedFiles: Int = 0,
    val rateLimited: Boolean = false,
)

/**
 * Writes Garmin nightly HRV readings to Health Connect. The clientRecordId is
 * deterministic per reading time, so a re-imported wellness file dedups against
 * its earlier self instead of doubling the night — Health Connect's duplicate
 * rejection counts as success here.
 */
@Singleton
class FitHrvImportService @Inject constructor(
    private val importRepository: AppleHealthImportRepository,
    private val healthRepository: HealthRepository,
) {
    private val writeHrvPermission =
        HealthPermission.getWritePermission(HeartRateVariabilityRmssdRecord::class)

    /**
     * Writes each file's readings, all files batched into ONE insert call
     * (Health Connect rate-limits per call). The batch is atomic, so a failed
     * batch retries file by file; a rate limit stops the run without blaming
     * the files it never attempted.
     */
    internal suspend fun writeFiles(files: List<List<FitHrvReading>>): FitHrvImportOutcome {
        if (files.isEmpty()) return FitHrvImportOutcome()
        if (writeHrvPermission !in healthRepository.grantedPermissions()) {
            throw SecurityException("Missing Health Connect HRV write permission.")
        }

        try {
            importRepository.insertImportedRecords(files.flatten().map { it.toRecord() })
            return FitHrvImportOutcome(importedFiles = files.size)
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            if (HealthConnectRateLimitBackoff.isRateLimitFailure(error)) {
                return FitHrvImportOutcome(rateLimited = true)
            }
            Log.w(TAG, "HRV batch insert failed; retrying file by file", error)
        }

        var imported = 0
        var failed = 0
        for (readings in files) {
            try {
                importRepository.insertImportedRecords(readings.map { it.toRecord() })
                imported += 1
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                if (HealthConnectRateLimitBackoff.isRateLimitFailure(error)) {
                    return FitHrvImportOutcome(importedFiles = imported, failedFiles = failed, rateLimited = true)
                }
                if (error.isDuplicateClientRecordFailure()) {
                    imported += 1
                } else {
                    failed += 1
                    Log.w(TAG, "HRV file insert failed", error)
                }
            }
        }
        return FitHrvImportOutcome(importedFiles = imported, failedFiles = failed)
    }

    private fun FitHrvReading.toRecord(): HeartRateVariabilityRmssdRecord =
        HeartRateVariabilityRmssdRecord(
            time = time,
            zoneOffset = null,
            heartRateVariabilityMillis = rmssdMillis,
            metadata = Metadata.manualEntry(
                clientRecordId = "garmin_fit_hrv_${time.toEpochMilli()}",
                device = Device(type = Device.TYPE_PHONE),
            ),
        )

    private companion object {
        private const val TAG = "FitHrvImportService"
    }
}
