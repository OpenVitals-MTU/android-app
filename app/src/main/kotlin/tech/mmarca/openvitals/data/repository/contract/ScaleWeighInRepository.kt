package tech.mmarca.openvitals.data.repository.contract

import java.time.Instant
import kotlinx.coroutines.flow.Flow
import tech.mmarca.openvitals.domain.model.ScaleReading
import tech.mmarca.openvitals.domain.model.ScaleWeighIn

/**
 * The weigh-ins a scale broadcast, held in Room as the scale measured them.
 * Health Connect cannot own them: it has no type for body impedance, and a
 * weigh-in heard while the app is closed has to wait here until a write is
 * possible. Thin over the DAO: no composition, no Health Connect.
 */
interface ScaleWeighInRepository {

    /**
     * Folds [reading] into the weigh-in `(scaleTimestamp, profile)`, creating
     * it at [time] if it is new. Returns the weigh-in when the reading added
     * something, null when it only repeated what was known.
     */
    suspend fun merge(
        scaleTimestamp: Long,
        profile: Int,
        reading: ScaleReading,
        time: Instant,
        now: Instant,
    ): ScaleWeighIn?

    /** Weigh-ins whose current state has not reached Health Connect, oldest first. */
    suspend fun pending(): List<ScaleWeighIn>

    /** Records that [weighIn], as it stands, reached Health Connect. */
    suspend fun markWritten(weighIn: ScaleWeighIn)

    /** The most recent weigh-in with a weight. */
    val latest: Flow<ScaleWeighIn?>

    /** How many weigh-ins are waiting for Health Connect. */
    val pendingCount: Flow<Int>

    /** Forgets one weigh-in. Its Health Connect records are the caller's to remove. */
    suspend fun delete(scaleTimestamp: Long, profile: Int)

    /**
     * Forgets the weigh-ins older than [before] that never got a weight. The
     * scale sends the weight within seconds of the rest, so one that is still
     * missing was not heard and will not come.
     */
    suspend fun dropWithoutWeight(before: Instant)
}
