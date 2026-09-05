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
 * Imports the activity FIT files a sync pulled, down the same path a folder
 * import uses, so the two cannot differ. Never throws except cancellation:
 * per-file failures are tolerated, and the raw bytes stay in [GarminFileStore].
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
            // The batch is atomic; retry file by file so only the bad one fails.
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
            // The last imported type seeds the next manual entry's default.
            preferencesRepository.lastActivityExerciseType = requests.last().exerciseType
            GarminLog.log("[GARMIN-IMPORT] wrote $written watch activities")
        }
        return written
    }

    private suspend fun buildRequest(file: GarminDownloadedFile): ActivityWriteRequest? {
        // Indexed, not numbered: several files share the 65535 "unset" number.
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
