package tech.mmarca.openvitals.data.repository

import java.time.LocalDate
import tech.mmarca.openvitals.domain.model.HeartRateDayAggregate

/** Assumed size of a day whose sample count is missing: a full day at 1 Hz. */
private const val UnknownDaySamples = 86_400L

/**
 * Groups the [stale] days into raw reads, newest first. One read covers
 * neighbouring stale days only, so no cached day is read again. A read holds at
 * most [readSamples]. The plan stops before [loadSamples], but always keeps the
 * newest stale day.
 */
internal fun planRawDayReads(
    days: List<HeartRateDayAggregate>,
    stale: Set<LocalDate>,
    readSamples: Long,
    loadSamples: Long,
): List<List<HeartRateDayAggregate>> {
    val reads = mutableListOf<List<HeartRateDayAggregate>>()
    var read = mutableListOf<HeartRateDayAggregate>()
    var readTotal = 0L
    var loadTotal = 0L
    fun closeRead() {
        if (read.isEmpty()) return
        reads += read
        read = mutableListOf()
        readTotal = 0L
    }
    for (day in days.sortedByDescending { it.summary.date }) {
        if (day.summary.date !in stale) {
            closeRead()
            continue
        }
        val samples = day.sampleCount.takeIf { it > 0L } ?: UnknownDaySamples
        if (loadTotal > 0L && loadTotal + samples > loadSamples) break
        if (readTotal > 0L && readTotal + samples > readSamples) closeRead()
        read += day
        readTotal += samples
        loadTotal += samples
    }
    closeRead()
    return reads
}
