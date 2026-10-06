package tech.mmarca.openvitals.data.repository.contract

import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import tech.mmarca.openvitals.domain.model.ScaleReading
import tech.mmarca.openvitals.domain.model.ScaleWeighIn

/** In-memory weigh-ins, with the merge the Room repository does. */
class FakeScaleWeighInRepository : ScaleWeighInRepository {

    private val rows = MutableStateFlow<Map<Pair<Long, Int>, ScaleWeighIn>>(emptyMap())

    val all: List<ScaleWeighIn> get() = rows.value.values.sortedBy { it.time }

    override suspend fun merge(
        scaleTimestamp: Long,
        profile: Int,
        reading: ScaleReading,
        time: Instant,
        now: Instant,
    ): ScaleWeighIn? {
        val key = scaleTimestamp to profile
        val existing = rows.value[key]
        val merged = existing?.mergedWith(reading, now.toEpochMilli())
            ?: ScaleWeighIn(scaleTimestamp, profile, time, reading, updatedMillis = now.toEpochMilli())
        if (merged === existing) return null
        rows.value += key to merged
        return merged
    }

    override suspend fun pending(): List<ScaleWeighIn> = all.filter { it.isPending }

    override suspend fun markWritten(weighIn: ScaleWeighIn) {
        val key = weighIn.scaleTimestamp to weighIn.profile
        val current = rows.value[key] ?: return
        rows.value += key to current.copy(writtenMillis = weighIn.updatedMillis)
    }

    override val latest: Flow<ScaleWeighIn?> =
        rows.map { byKey -> byKey.values.filter { it.isWritable }.maxByOrNull { it.time } }

    override val pendingCount: Flow<Int> = rows.map { byKey -> byKey.values.count { it.isPending } }

    override suspend fun delete(scaleTimestamp: Long, profile: Int) {
        rows.value -= scaleTimestamp to profile
    }

    override suspend fun dropWithoutWeight(before: Instant) {
        rows.value = rows.value.filterValues { it.isWritable || !it.time.isBefore(before) }
    }
}
