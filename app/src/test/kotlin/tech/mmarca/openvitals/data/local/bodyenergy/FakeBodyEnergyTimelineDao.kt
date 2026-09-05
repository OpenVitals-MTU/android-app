package tech.mmarca.openvitals.data.local.bodyenergy

import tech.mmarca.openvitals.data.local.vitalscache.VitalsSyncCursorEntity

/** In-memory stand-in for [BodyEnergyTimelineDao]; the `@Transaction` default methods run for real against it. */
class FakeBodyEnergyTimelineDao : BodyEnergyTimelineDao {
    private val days = linkedMapOf<Long, BodyEnergyDayEntity>()
    private val buckets = linkedMapOf<Pair<Long, Long>, BodyEnergyBucketEntity>()
    private val cursors = linkedMapOf<String, VitalsSyncCursorEntity>()

    override suspend fun day(epochDay: Long): BodyEnergyDayEntity? = days[epochDay]

    override suspend fun daysBetween(
        startEpochDay: Long,
        endEpochDay: Long,
    ): List<BodyEnergyDayEntity> =
        days.values.filter { it.epochDay in startEpochDay..endEpochDay }.sortedBy { it.epochDay }

    override suspend fun bucketsForDay(epochDay: Long): List<BodyEnergyBucketEntity> =
        buckets.values.filter { it.epochDay == epochDay }.sortedBy { it.timeMillis }

    override suspend fun countDays(): Int = days.size

    override suspend fun countBucketsForDay(epochDay: Long): Int =
        buckets.values.count { it.epochDay == epochDay }

    override suspend fun purgeBucketsBefore(epochDay: Long) {
        buckets.keys.filter { it.first < epochDay }.forEach(buckets::remove)
    }

    override suspend fun cursor(metric: String): VitalsSyncCursorEntity? = cursors[metric]

    override suspend fun insertDay(day: BodyEnergyDayEntity) {
        days[day.epochDay] = day
    }

    override suspend fun insertBuckets(buckets: List<BodyEnergyBucketEntity>) {
        buckets.forEach { this.buckets[it.epochDay to it.timeMillis] = it }
    }

    override suspend fun insertCursor(cursor: VitalsSyncCursorEntity) {
        cursors[cursor.metric] = cursor
    }

    override suspend fun deleteBucketsForDay(epochDay: Long) {
        buckets.keys.filter { it.first == epochDay }.forEach(buckets::remove)
    }

    override suspend fun deleteBucketsBetween(startEpochDay: Long, endEpochDay: Long) {
        buckets.keys.filter { it.first in startEpochDay..endEpochDay }.forEach(buckets::remove)
    }

    override suspend fun deleteDaysBetween(startEpochDay: Long, endEpochDay: Long) {
        days.keys.filter { it in startEpochDay..endEpochDay }.toList().forEach(days::remove)
    }

    override suspend fun deleteAllBuckets() {
        buckets.clear()
    }

    override suspend fun deleteAllDays() {
        days.clear()
    }

    override suspend fun deleteCursor(metric: String) {
        cursors.remove(metric)
    }
}
