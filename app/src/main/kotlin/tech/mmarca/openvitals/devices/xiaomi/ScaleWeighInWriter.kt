package tech.mmarca.openvitals.devices.xiaomi

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import tech.mmarca.openvitals.core.presentation.isPermissionFailure
import tech.mmarca.openvitals.data.repository.AppleHealthImportRepository
import tech.mmarca.openvitals.data.repository.contract.BodyProfilePreferences
import tech.mmarca.openvitals.data.repository.contract.BodyRepository
import tech.mmarca.openvitals.data.repository.contract.HealthRepository
import tech.mmarca.openvitals.data.repository.contract.ScaleWeighInRepository
import tech.mmarca.openvitals.domain.insights.BodyCompositionEstimate
import tech.mmarca.openvitals.domain.insights.BodyCompositionInput
import tech.mmarca.openvitals.domain.insights.bodyComposition
import tech.mmarca.openvitals.domain.insights.bodyCompositionMissingInputs
import tech.mmarca.openvitals.domain.model.ScaleWeighIn
import tech.mmarca.openvitals.domain.preferences.BodyProfile
import tech.mmarca.openvitals.features.imports.applehealth.isDuplicateClientRecordFailure
import tech.mmarca.openvitals.healthconnect.HealthConnectManager
import tech.mmarca.openvitals.healthconnect.HealthConnectSyncDisabledException

/**
 * Takes the weigh-ins Room holds to Health Connect, through the shared
 * insert door. It runs wherever a weigh-in is heard, the background
 * included, where nothing can ask for a permission: a weigh-in that cannot
 * be written stays pending, the reason is kept for the Scales screen, and
 * every later pass tries again.
 */
@Singleton
class ScaleWeighInWriter @Inject constructor(
    private val weighIns: ScaleWeighInRepository,
    private val importRepository: AppleHealthImportRepository,
    private val healthRepository: HealthRepository,
    private val bodyProfilePreferences: BodyProfilePreferences,
    private val bodyRepository: BodyRepository,
    private val hc: HealthConnectManager,
    private val store: XiaomiScaleStore,
) {
    // A frame, the worker and the screen can all ask at once. One pass at a time.
    private val writing = Mutex()

    /** Writes every pending weigh-in. Never throws, bar cancellation: the outcome is in the store. */
    suspend fun writePending() = writing.withLock {
        try {
            val pending = weighIns.pending()
            if (pending.isEmpty()) {
                store.setWriteFailure(null)
                return@withLock
            }
            val granted = healthRepository.grantedPermissions()
            // The other records hang off the weight, so without it nothing is worth writing.
            if (ScaleRecordKind.WEIGHT.writePermission !in granted) {
                store.setWriteFailure(ScaleWriteFailure.PERMISSION)
                return@withLock
            }
            val profile = bodyProfile()
            for (weighIn in pending) {
                write(weighIn, compositionOf(weighIn, profile), granted)
            }
            store.setWriteFailure(null)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            store.setWriteFailure(
                when {
                    error.isPermissionFailure() -> ScaleWriteFailure.PERMISSION
                    error is HealthConnectSyncDisabledException -> ScaleWriteFailure.SYNC_PAUSED
                    else -> ScaleWriteFailure.OTHER
                },
            )
        }
    }

    /** The estimate [weighIn] gives for the person in the Body profile, for display. */
    suspend fun compositionOf(weighIn: ScaleWeighIn): BodyCompositionEstimate? =
        compositionOf(weighIn, bodyProfile())

    /** What the Body profile still lacks for an estimate. Empty means a weigh-in with an impedance gets one. */
    suspend fun missingProfileInputs(): Set<BodyCompositionInput> = bodyProfile().bodyCompositionMissingInputs()

    /** Removes a weigh-in everywhere: its Health Connect records, then its row. */
    suspend fun delete(weighIn: ScaleWeighIn) = writing.withLock {
        val granted = healthRepository.grantedPermissions()
        for (kind in ScaleRecordKind.entries) {
            // A delete needs the same grant as a write, and an ungranted kind was never written.
            if (kind.writePermission !in granted) continue
            importRepository.deleteImportedRecordsByClientIds(
                kind.recordType,
                listOf(scaleRecordId(kind, weighIn.scaleTimestamp, weighIn.profile)),
            )
        }
        weighIns.delete(weighIn.scaleTimestamp, weighIn.profile)
        // The scale repeats a weigh-in for a while after it. It must not come back.
        store.setDeletedScaleTimestamp(weighIn.scaleTimestamp)
    }

    private suspend fun write(
        weighIn: ScaleWeighIn,
        composition: BodyCompositionEstimate?,
        granted: Set<String>,
    ) {
        val records = scaleWeighInRecords(weighIn, composition, granted)
        // Not cut short once started: an insert without its stamp would be written twice.
        withContext(NonCancellable) {
            try {
                importRepository.insertImportedRecords(records)
            } catch (error: Exception) {
                // This state is already there: an earlier pass wrote it and died before stamping it.
                if (!error.isDuplicateClientRecordFailure()) throw error
            }
            weighIns.markWritten(weighIn)
        }
    }

    private fun compositionOf(weighIn: ScaleWeighIn, profile: BodyProfile): BodyCompositionEstimate? {
        val weightKg = weighIn.reading.weightKg ?: return null
        val resistanceOhm = weighIn.reading.impedanceLowOhm ?: return null
        return profile.bodyComposition(weightKg, resistanceOhm)
    }

    /**
     * Sex and age only the profile knows. The height may be a newer Health
     * Connect record, but in the background Health Connect shows this app its
     * own records only, so there the stored profile is the whole answer.
     */
    private suspend fun bodyProfile(): BodyProfile {
        val declared = bodyProfilePreferences.bodyProfile()
        return if (hc.readsOtherAppsDataNow()) bodyRepository.resolveBodyProfile(declared) else declared
    }
}
