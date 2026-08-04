package tech.mmarca.openvitals.devices.garmin

import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.data.repository.contract.ActivityRepository
import tech.mmarca.openvitals.domain.model.ActivityWriteRequest
import tech.mmarca.openvitals.features.manualentry.activity.ActivityEntryUnits
import tech.mmarca.openvitals.features.manualentry.activity.DefaultActivityEntryTypes
import tech.mmarca.openvitals.features.manualentry.activity.buildWriteRequest
import tech.mmarca.openvitals.features.manualentry.activity.initialActivityEntryState
import tech.mmarca.openvitals.features.manualentry.activity.withRouteImport
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.RouteFileParser

/**
 * Imports the ACTIVITY-type FIT files a watch sync pulled as exercises, down
 * the exact conversion path a hand-picked folder import uses (`RouteFileParser`
 * → activity entry state → `ActivityWriteRequest`), so a ride copied off the
 * watch and the same ride imported by hand cannot come out different.
 *
 * Mirror of the Flutter build's `GarminDeviceSyncPort` handing every download
 * to `RouteBulkImportViewModel.importRouteFiles` — there the one importer
 * splits wellness from activities internally; here the wellness half already
 * lives in [tech.mmarca.openvitals.devices.garmin.wellness.FitWellnessImporter],
 * so this class carries only the exercise half.
 *
 * Deliberately never throws (except cancellation): the Flutter bulk importer
 * tolerates every per-file failure and the sync completes around it, with the
 * raw bytes kept in [GarminFileStore] as the recovery net. Failing the sync
 * here would re-download files whose wellness data already landed.
 */
@Singleton
class GarminActivityImporter @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val preferencesRepository: PreferencesRepository,
) {

    private val clock: Clock = Clock.systemDefaultZone()

    /** Imports the activity files in [files]; returns how many were written. */
    suspend fun import(files: List<GarminDownloadedFile>): Int {
        val activityFiles = files.filter { it.entry.type == GarminFileType.ACTIVITY }
        if (activityFiles.isEmpty()) return 0

        val requests = mutableListOf<ActivityWriteRequest>()
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
                    continue
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                GarminLog.log("[GARMIN-IMPORT] permission check failed: $error")
                continue
            }
            requests += request
        }
        if (requests.isEmpty()) return 0

        var written = 0
        try {
            activityRepository.writeActivityEntries(requests)
            written = requests.size
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            // The batched insert is atomic, so one bad file sinks the whole
            // batch — retry file by file so only the guilty one fails.
            GarminLog.log("[GARMIN-IMPORT] activity batch failed, retrying singly: $error")
            for (request in requests) {
                try {
                    activityRepository.writeActivityEntry(request)
                    written += 1
                } catch (retryError: CancellationException) {
                    throw retryError
                } catch (retryError: Exception) {
                    GarminLog.log("[GARMIN-IMPORT] activity write failed: $retryError")
                }
            }
        }
        if (written > 0) {
            // The same bookkeeping the folder importer does: the last imported
            // exercise type seeds the next manual entry's default.
            preferencesRepository.lastActivityExerciseType = requests.last().exerciseType
            GarminLog.log("[GARMIN-IMPORT] wrote $written watch activities")
        }
        return written
    }

    private suspend fun buildRequest(file: GarminDownloadedFile): ActivityWriteRequest? {
        // Indexed, not numbered: several files share the 65535 "unset" file
        // number, and identically-named entries cannot be told apart.
        val routeImport = RouteFileParser.parseFile(
            file.bytes,
            fileName = "${file.entry.type.label}_${file.entry.fileIndex}.fit",
        )
        val units = ActivityEntryUnits.uniform(preferencesRepository.unitSystem)
        val state = initialActivityEntryState(
            clock = clock,
            repository = activityRepository,
            selectedActivityType = preferredActivityType(
                requireGpsRoute = routeImport.points.isNotEmpty(),
            ),
        ).withRouteImport(
            routeImport = routeImport,
            units = units,
            clock = clock,
        )
        return buildWriteRequest(state, units)
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
