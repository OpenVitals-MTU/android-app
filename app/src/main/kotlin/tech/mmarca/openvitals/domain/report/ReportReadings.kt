package tech.mmarca.openvitals.domain.report

import java.time.Duration
import java.time.Instant

/**
 * Two duplicated records usually share their instant exactly, but a re-import
 * or an echoing app can shift the copy by a moment — this window absorbs that
 * while staying far below how fast anyone takes two real measurements.
 */
internal const val DuplicateReadingWindowSeconds = 30L

/**
 * Collapses duplicated records: an entry whose [key] matches one kept within
 * the last [DuplicateReadingWindowSeconds] cannot be a second measurement — it
 * is the same reading recorded twice (an import run twice, a second app
 * echoing the data back). Entries with a different key, or further apart in
 * time, all stay: repeated back-to-back measurements are real. Returns the
 * survivors sorted by [time].
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
