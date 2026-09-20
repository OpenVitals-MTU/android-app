package tech.mmarca.openvitals.devices.garmin

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.data.repository.contract.ActivityRepository
import tech.mmarca.openvitals.domain.model.ActivityRecordSource
import tech.mmarca.openvitals.domain.model.ActivityWriteRequest
import tech.mmarca.openvitals.features.manualentry.activity.DefaultActivityEntryTypes
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.RouteElevationCorrector
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.RouteFileParser
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.toImportWriteRequest

/**
 * What an activity import did with the files it was given. A file that did not convert is
 * done with: parsing is deterministic, so a retry cannot help.
 */
data class GarminActivityImportResult(
    val written: Int = 0,
    /** Skipped because Health Connect write access is missing. */
    val missingPermission: List<GarminDownloadedFile> = emptyList(),
    /** Converted, but the Health Connect write failed. */
    val failedWrites: List<GarminDownloadedFile> = emptyList(),
) {
    /** Files the next sync must fetch again, so their keys must not be recorded. */
    val retry: List<GarminDownloadedFile> get() = missingPermission + failedWrites
}

/**
 * Imports the activity FIT files a sync pulled, down the same path a folder
 * import uses ([toImportWriteRequest]), so the two cannot differ. Never throws except cancellation:
 * per-file failures are reported in the result, not thrown.
 */
@Singleton
class GarminActivityImporter @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val preferencesRepository: PreferencesRepository,
    private val elevationCorrector: RouteElevationCorrector,
) {

    /** Imports the activity files in [files] and says which ones must be fetched again. */
    suspend fun import(files: List<GarminDownloadedFile>): GarminActivityImportResult {
        val activityFiles = files.filter { it.entry.type == GarminFileType.ACTIVITY }
        if (activityFiles.isEmpty()) return GarminActivityImportResult()

        val pending = mutableListOf<Pair<GarminDownloadedFile, ActivityWriteRequest>>()
        val missingPermission = mutableListOf<GarminDownloadedFile>()
        val failedWrites = mutableListOf<GarminDownloadedFile>()
        for (file in activityFiles) {
            val request = try {
                buildRequest(file)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                GarminLog.log(
                    "[GARMIN-IMPORT] activity index=${file.entry.fileIndex} " +
                        "did not convert: $error",
                )
                continue
            } ?: continue
            try {
                if (!activityRepository.hasActivityWritePermission(request)) {
                    GarminLog.log(
                        "[GARMIN-IMPORT] activity index=${file.entry.fileIndex} " +
                            "skipped: write permissions are missing",
                    )
                    missingPermission += file
                    continue
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                GarminLog.log("[GARMIN-IMPORT] permission check failed: $error")
                failedWrites += file
                continue
            }
            pending += file to request
        }

        var written = 0
        if (pending.isNotEmpty()) {
            try {
                activityRepository.writeActivityEntries(pending.map { it.second })
                written = pending.size
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                // The batch is atomic; retry file by file so only the bad one fails.
                GarminLog.log("[GARMIN-IMPORT] activity batch failed, retrying singly: $error")
                for ((file, request) in pending) {
                    try {
                        activityRepository.writeActivityEntry(request)
                        written += 1
                    } catch (retryError: CancellationException) {
                        throw retryError
                    } catch (retryError: Exception) {
                        GarminLog.log("[GARMIN-IMPORT] activity write failed: $retryError")
                        failedWrites += file
                    }
                }
            }
        }
        if (written > 0) {
            // The last imported type seeds the next manual entry's default.
            preferencesRepository.lastActivityExerciseType = pending.last().second.exerciseType
            GarminLog.log("[GARMIN-IMPORT] wrote $written watch activities")
        }
        return GarminActivityImportResult(written, missingPermission, failedWrites)
    }

    private suspend fun buildRequest(file: GarminDownloadedFile): ActivityWriteRequest? {
        // Indexed, not numbered: several files share the 65535 "unset" number.
        val routeImport = elevationCorrector.correct(
            RouteFileParser.parseFile(
                file.bytes,
                fileName = "${file.entry.type.label}_${file.entry.fileIndex}.fit",
            ),
        )
        // Straight from the file, not through the entry form: exact times, a client id
        // that is a function of the file, and a record that says a watch made it.
        return routeImport.toImportWriteRequest(
            fallbackType = preferredActivityType(requireGpsRoute = routeImport.points.isNotEmpty()),
            source = ActivityRecordSource.WATCH,
        )
    }

    /** Mirror of the settings importer's preferred-type resolution. */
    private fun preferredActivityType(requireGpsRoute: Boolean) =
        DefaultActivityEntryTypes
            .filter { !requireGpsRoute || it.supportsGpsRoute }
            .ifEmpty { DefaultActivityEntryTypes }
            .let { activityTypes ->
                val preferredExerciseType = preferencesRepository.favoriteActivityExerciseType
                    ?.takeIf { exerciseType -> activityTypes.any { it.exerciseType == exerciseType } }
                    ?: preferencesRepository.lastActivityExerciseType
                        ?.takeIf { exerciseType -> activityTypes.any { it.exerciseType == exerciseType } }
                activityTypes.firstOrNull { it.exerciseType == preferredExerciseType }
                    ?: activityTypes.first()
            }
}
