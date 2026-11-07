package tech.mmarca.openvitals.devices.garmin

import javax.inject.Inject
import javax.inject.Singleton
import tech.mmarca.openvitals.devices.garmin.wellness.FitWellnessImporter

/**
 * Takes downloaded watch files into the app, then records them as done.
 *
 * The order is the point. The watch archives a file once it is downloaded, so
 * the recorded key is all that stops the next sync from fetching it again. A
 * key recorded before the import would hide a file the import never landed.
 */
@Singleton
class GarminDownloadImport @Inject constructor(
    private val importer: FitWellnessImporter,
    private val activityImporter: GarminActivityImporter,
    private val stateStore: GarminDeviceStateStore,
    private val fileStore: GarminFileStore,
) {
    /**
     * Throws when the write path is down. Nothing is recorded then, and the
     * saved copies stay pending for the next sync.
     */
    suspend fun import(deviceId: String, files: List<GarminDownloadedFile>): GarminActivityImportResult {
        importer.import(files)
        // Same path as a hand-picked FIT folder. Per-file failures come back in the result.
        val activities = activityImporter.import(files)
        // Files with no stable key are fetched again every sync by design. A workout that
        // did not reach Health Connect keeps no key either, so it comes back.
        stateStore.recordSyncedFileKeys(
            deviceId,
            (files - activities.retry.toSet()).mapNotNull { it.entry.dedupKey },
        )
        fileStore.markImported(files)
        return activities
    }
}
