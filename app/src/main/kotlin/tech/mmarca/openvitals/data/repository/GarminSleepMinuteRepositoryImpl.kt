package tech.mmarca.openvitals.data.repository

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.Instant
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton
import tech.mmarca.openvitals.data.local.garmin.GarminSleepMinuteDao
import tech.mmarca.openvitals.data.local.garmin.GarminSleepMinuteEntity
import tech.mmarca.openvitals.data.repository.contract.GarminSleepMinuteRepository
import tech.mmarca.openvitals.domain.model.GarminSleepMinute
import tech.mmarca.openvitals.domain.model.SleepMinuteKind

@Singleton
class GarminSleepMinuteRepositoryImpl @Inject constructor(
    private val dao: GarminSleepMinuteDao,
) : GarminSleepMinuteRepository {

    override suspend fun upsert(minutes: List<GarminSleepMinute>) {
        if (minutes.isEmpty()) return
        dao.upsertMinutes(minutes.map { it.toEntity() })
    }

    override suspend fun minutesBetween(from: Instant, to: Instant): List<GarminSleepMinute> =
        dao.minutesBetween(from.toEpochMilli(), to.toEpochMilli()).mapNotNull { it.toMinute() }

    override suspend fun pruneBefore(before: Instant) {
        dao.pruneBefore(before.toEpochMilli())
    }

    private fun GarminSleepMinute.toEntity(): GarminSleepMinuteEntity = GarminSleepMinuteEntity(
        timeMillis = time.toEpochMilli(),
        kind = kind.storageName,
        heartRate = heartRate,
        movement = movement,
        activity = activity,
        offsetSeconds = zoneOffset.totalSeconds,
        features = features?.let { values ->
            val buffer = ByteBuffer.allocate(values.size * FloatBytes).order(ByteOrder.LITTLE_ENDIAN)
            values.forEach(buffer::putFloat)
            buffer.array()
        },
    )

    /** Null for a kind this build does not know; a newer build may have written it. */
    private fun GarminSleepMinuteEntity.toMinute(): GarminSleepMinute? {
        val kind = SleepMinuteKind.fromStorageName(kind) ?: return null
        val offset = if (offsetSeconds in -MaxOffsetSeconds..MaxOffsetSeconds) {
            ZoneOffset.ofTotalSeconds(offsetSeconds)
        } else {
            ZoneOffset.UTC
        }
        return GarminSleepMinute(
            time = Instant.ofEpochMilli(timeMillis),
            kind = kind,
            heartRate = heartRate,
            movement = movement,
            activity = activity,
            zoneOffset = offset,
            features = features?.let { bytes ->
                val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
                FloatArray(bytes.size / FloatBytes) { buffer.getFloat() }
            },
        )
    }

    private companion object {
        const val FloatBytes = 4
        const val MaxOffsetSeconds = 18 * 60 * 60
    }
}
