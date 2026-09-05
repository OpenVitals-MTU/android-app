package tech.mmarca.openvitals.domain.report

import java.time.Duration
import java.time.Instant

/** A duplicated record can shift by a moment on re-import; this window absorbs that. */
internal const val DuplicateReadingWindowSeconds = 30L

/**
 * Collapses duplicated records: the same [key] within
 * [DuplicateReadingWindowSeconds] is the same reading recorded twice.
 * Returns the survivors sorted by [time].
 */
internal fun <T> dedupeReadings(
    entries: List<T>,
    time: (T) -> Instant,
    key: (T) -> Any,
): List<T> {
    val lastKeptAt = mutableMapOf<Any, Instant>()
    return entries.sortedBy(time).filter { entry ->
        val entryKey = key(entry)
        val previous = lastKeptAt[entryKey]
        val isDuplicate = previous != null &&
            Duration.between(previous, time(entry)).seconds <= DuplicateReadingWindowSeconds
        if (!isDuplicate) lastKeptAt[entryKey] = time(entry)
        !isDuplicate
    }
}
