package tech.mmarca.openvitals.data.repository

import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import tech.mmarca.openvitals.data.local.garmin.GarminSleepMinuteDao
import tech.mmarca.openvitals.data.local.garmin.GarminSleepMinuteEntity
import tech.mmarca.openvitals.domain.model.GarminSleepMinute
import tech.mmarca.openvitals.domain.model.SleepMinuteKind

class GarminSleepMinuteRepositoryImplTest {

    /** In-memory stand-in: the repo has no Room runtime in unit tests. */
    private class FakeDao : GarminSleepMinuteDao {
        val rows = sortedMapOf<Long, GarminSleepMinuteEntity>()

        override suspend fun upsertMinutes(minutes: List<GarminSleepMinuteEntity>) {
            minutes.forEach { rows[it.timeMillis] = it }
        }

        override suspend fun minutesBetween(fromMillis: Long, toMillis: Long): List<GarminSleepMinuteEntity> =
            rows.subMap(fromMillis, toMillis).values.toList()

        override suspend fun pruneBefore(beforeMillis: Long) {
            rows.headMap(beforeMillis).clear()
        }

        override suspend fun count(): Long = rows.size.toLong()
    }

    private val dao = FakeDao()
    private val repository = GarminSleepMinuteRepositoryImpl(dao)
    private val at: Instant = Instant.parse("2024-03-04T22:00:00Z")
    private val zone: ZoneOffset = ZoneOffset.ofHours(2)

    private fun raw(time: Instant) = GarminSleepMinute(
        time = time,
        kind = SleepMinuteKind.RAW,
        heartRate = 51.5,
        movement = 2.0,
        activity = -0.25,
        zoneOffset = zone,
        features = FloatArray(10) { it * 0.5f },
    )

    @Test
    fun `round-trips every field including the feature blob`() = runTest {
        repository.upsert(listOf(raw(at)))

        val stored = repository.minutesBetween(at, at.plusSeconds(60)).single()

        assertEquals(at, stored.time)
        assertEquals(SleepMinuteKind.RAW, stored.kind)
        assertEquals(51.5, stored.heartRate!!, 0.0)
        assertEquals(2.0, stored.movement!!, 0.0)
        assertEquals(-0.25, stored.activity!!, 0.0)
        assertEquals(zone, stored.zoneOffset)
        assertArrayEquals(FloatArray(10) { it * 0.5f }, stored.features!!, 0f)
    }

    @Test
    fun `a minute without features stores nulls`() = runTest {
        val awake = GarminSleepMinute(at, SleepMinuteKind.AWAKE, null, null, null, zone, null)
        repository.upsert(listOf(awake))

        val stored = repository.minutesBetween(at, at.plusSeconds(60)).single()

        assertEquals(SleepMinuteKind.AWAKE, stored.kind)
        assertNull(stored.heartRate)
        assertNull(stored.features)
    }

    @Test
    fun `the minute is the key, so a re-import replaces`() = runTest {
        repository.upsert(listOf(raw(at)))
        repository.upsert(listOf(raw(at).copy(heartRate = 60.0)))

        val stored = repository.minutesBetween(at, at.plusSeconds(60))

        assertEquals(1, stored.size)
        assertEquals(60.0, stored.single().heartRate!!, 0.0)
    }

    @Test
    fun `prune drops only the old minutes`() = runTest {
        repository.upsert(listOf(raw(at), raw(at.plusSeconds(60)), raw(at.plusSeconds(120))))

        repository.pruneBefore(at.plusSeconds(60))

        assertEquals(listOf(at.plusSeconds(60), at.plusSeconds(120)), repository.minutesBetween(at, at.plusSeconds(180)).map { it.time })
    }

    @Test
    fun `an unknown kind from a newer build is skipped`() = runTest {
        dao.upsertMinutes(listOf(GarminSleepMinuteEntity(at.toEpochMilli(), "dreaming", null, null, null, 0, null)))

        assertEquals(emptyList<GarminSleepMinute>(), repository.minutesBetween(at, at.plusSeconds(60)))
    }
}
