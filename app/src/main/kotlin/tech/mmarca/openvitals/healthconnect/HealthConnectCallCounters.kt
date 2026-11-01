package tech.mmarca.openvitals.healthconnect

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/** How one kind of Health Connect call fared since the process started. Counts only. */
internal data class HealthConnectCallCount(
    val started: Long = 0L,
    val succeeded: Long = 0L,
    val failed: Long = 0L,
    val rateLimited: Long = 0L,
    val skippedForBackoff: Long = 0L,
) {
    operator fun minus(other: HealthConnectCallCount): HealthConnectCallCount = HealthConnectCallCount(
        started = started - other.started,
        succeeded = succeeded - other.succeeded,
        failed = failed - other.failed,
        rateLimited = rateLimited - other.rateLimited,
        skippedForBackoff = skippedForBackoff - other.skippedForBackoff,
    )

    operator fun plus(other: HealthConnectCallCount): HealthConnectCallCount = HealthConnectCallCount(
        started = started + other.started,
        succeeded = succeeded + other.succeeded,
        failed = failed + other.failed,
        rateLimited = rateLimited + other.rateLimited,
        skippedForBackoff = skippedForBackoff + other.skippedForBackoff,
    )

    val isEmpty: Boolean
        get() = started == 0L && succeeded == 0L && failed == 0L && rateLimited == 0L && skippedForBackoff == 0L

    fun format(): String =
        "started=$started ok=$succeeded failed=$failed rateLimited=$rateLimited skipped=$skippedForBackoff"
}

/** The counters at one moment, by operation. */
internal class HealthConnectCallSnapshot(val counts: Map<String, HealthConnectCallCount>) {
    val total: HealthConnectCallCount
        get() = counts.values.fold(HealthConnectCallCount()) { sum, count -> sum + count }

    /** One line per operation, busiest first, with the total on top. Structure only: names and counts. */
    fun lines(): List<String> {
        if (counts.isEmpty()) return listOf("none")
        val perOperation = counts.entries
            .filter { !it.value.isEmpty }
            .sortedWith(compareByDescending<Map.Entry<String, HealthConnectCallCount>> { it.value.started }.thenBy { it.key })
            .map { (operation, count) -> "$operation: ${count.format()}" }
        return listOf("total: ${total.format()}") + perOperation
    }

    fun oneLine(): String = "total ${total.format()}"
}

/**
 * Process-wide tally of Health Connect calls, by operation. The key is the
 * operation name before any bracket, so a range or an id never lands in a
 * report: `readRawHeartRateSamples[2026-..]` counts as `readRawHeartRateSamples`.
 *
 * A bug report from a phone we do not have says "rate limited" and nothing
 * else. These numbers, in the report email and the diagnostics export, say
 * how many calls the load made and which ones were refused.
 */
internal object HealthConnectCallCounters {
    private class Cell {
        val started = AtomicLong()
        val succeeded = AtomicLong()
        val failed = AtomicLong()
        val rateLimited = AtomicLong()
        val skippedForBackoff = AtomicLong()

        fun toCount() = HealthConnectCallCount(
            started = started.get(),
            succeeded = succeeded.get(),
            failed = failed.get(),
            rateLimited = rateLimited.get(),
            skippedForBackoff = skippedForBackoff.get(),
        )
    }

    private val cells = ConcurrentHashMap<String, Cell>()

    fun started(operation: String) { cell(operation).started.incrementAndGet() }
    fun succeeded(operation: String) { cell(operation).succeeded.incrementAndGet() }
    fun failed(operation: String) { cell(operation).failed.incrementAndGet() }
    fun rateLimited(operation: String) { cell(operation).rateLimited.incrementAndGet() }
    fun skippedForBackoff(operation: String) { cell(operation).skippedForBackoff.incrementAndGet() }

    fun snapshot(): HealthConnectCallSnapshot =
        HealthConnectCallSnapshot(cells.mapValues { (_, cell) -> cell.toCount() })

    /** What happened since [earlier]. Operations first seen after it count whole. */
    fun deltaSince(earlier: HealthConnectCallSnapshot): HealthConnectCallSnapshot {
        val now = snapshot().counts
        return HealthConnectCallSnapshot(
            now.mapValues { (operation, count) ->
                earlier.counts[operation]?.let { count - it } ?: count
            }.filterValues { !it.isEmpty },
        )
    }

    fun summaryLines(): List<String> = snapshot().lines()

    internal fun resetForTest() {
        cells.clear()
    }

    private fun cell(operation: String): Cell =
        cells.getOrPut(operation.substringBefore('[')) { Cell() }
}
