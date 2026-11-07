package tech.mmarca.openvitals.data.repository

import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import tech.mmarca.openvitals.data.local.scale.ScaleWeighInDao
import tech.mmarca.openvitals.data.local.scale.ScaleWeighInEntity
import tech.mmarca.openvitals.data.repository.contract.ScaleWeighInRepository
import tech.mmarca.openvitals.domain.model.ScaleReading
import tech.mmarca.openvitals.domain.model.ScaleWeighIn

@Singleton
class ScaleWeighInRepositoryImpl @Inject constructor(
    private val dao: ScaleWeighInDao,
) : ScaleWeighInRepository {

    // Read, merge, write: two broadcasts of one weigh-in must not interleave.
    private val mergeLock = Mutex()

    override suspend fun merge(
        scaleTimestamp: Long,
        profile: Int,
        reading: ScaleReading,
        time: Instant,
        now: Instant,
    ): ScaleWeighIn? = mergeLock.withLock {
        val existing = dao.find(scaleTimestamp, profile)?.toWeighIn()
        val merged = if (existing == null) {
            ScaleWeighIn(
                scaleTimestamp = scaleTimestamp,
                profile = profile,
                time = time,
                reading = reading,
                updatedMillis = now.toEpochMilli(),
            )
        } else {
            existing.mergedWith(reading, now.toEpochMilli())
        }
        if (merged === existing) return@withLock null
        dao.upsert(merged.toEntity())
        merged
    }

    override suspend fun pending(): List<ScaleWeighIn> = dao.pending().map { it.toWeighIn() }

    override suspend fun markWritten(weighIn: ScaleWeighIn) {
        dao.markWritten(weighIn.scaleTimestamp, weighIn.profile, weighIn.updatedMillis)
    }

    override val latest: Flow<ScaleWeighIn?> = dao.latest().map { it?.toWeighIn() }

    override val pendingCount: Flow<Int> = dao.pendingCount()

    override suspend fun delete(scaleTimestamp: Long, profile: Int) {
        dao.delete(scaleTimestamp, profile)
    }

    override suspend fun dropWithoutWeight(before: Instant) {
        dao.deleteWithoutWeightBefore(before.toEpochMilli())
    }

    private fun ScaleWeighInEntity.toWeighIn(): ScaleWeighIn =
        ScaleWeighIn(
            scaleTimestamp = scaleTimestamp,
            profile = profile,
            time = Instant.ofEpochMilli(timeMillis),
            reading = ScaleReading(
                weightKg = weightKg,
                heartRateBpm = heartRateBpm,
                impedanceLowOhm = impedanceLowOhm,
                impedanceHighOhm = impedanceHighOhm,
            ),
            updatedMillis = updatedMillis,
            writtenMillis = writtenMillis,
        )

    private fun ScaleWeighIn.toEntity(): ScaleWeighInEntity =
        ScaleWeighInEntity(
            scaleTimestamp = scaleTimestamp,
            profile = profile,
            timeMillis = time.toEpochMilli(),
            weightKg = reading.weightKg,
            heartRateBpm = reading.heartRateBpm,
            impedanceLowOhm = reading.impedanceLowOhm,
            impedanceHighOhm = reading.impedanceHighOhm,
            updatedMillis = updatedMillis,
            writtenMillis = writtenMillis,
        )
}
