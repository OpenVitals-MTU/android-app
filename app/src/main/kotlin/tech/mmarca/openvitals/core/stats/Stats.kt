package tech.mmarca.openvitals.core.stats

/**
 * The empty-list contract for aggregates, stated once instead of guessed at each
 * call site.
 *
 * There are exactly two answers, and which one a caller wants is a real decision:
 *
 *  - [averageOrNull] returns null for "no samples, so the average is unknown".
 *    Use it whenever the UI can omit a number it does not have.
 *  - [averageOrZero] returns 0.0 for "the true total over this window was zero".
 *    Use it only where zero is a real reading, not a stand-in for missing.
 *
 * The two answers deliberately NOT used:
 *
 *  - NaN, which is what the stdlib's `average()` returns on an empty list. NaN
 *    compares false against everything, so it slips straight past `<= 0` and
 *    range guards, `?.` does not short-circuit it because it is not null, and it
 *    reaches the formatter to render a literal "NaN" in a stat tile.
 *  - 0, which reads as a real measurement. "0 bpm" and "0 ms" are not what a day
 *    with no reading looks like, and several callers branch on null to say so.
 *
 * [minOrNull]/[maxOrNull] from the stdlib already return null on empty; the bug
 * they were papered over with was a `?: 0.0` fallback at the call site.
 */
fun Iterable<Double>.averageOrNull(): Double? {
    var sum = 0.0
    var count = 0
    for (value in this) {
        sum += value
        count++
    }
    return if (count == 0) null else sum / count
}

@JvmName("averageOrNullOfLong")
fun Iterable<Long>.averageOrNull(): Double? {
    var sum = 0.0
    var count = 0
    for (value in this) {
        sum += value
        count++
    }
    return if (count == 0) null else sum / count
}

@JvmName("averageOrNullOfInt")
fun Iterable<Int>.averageOrNull(): Double? {
    var sum = 0.0
    var count = 0
    for (value in this) {
        sum += value
        count++
    }
    return if (count == 0) null else sum / count
}

fun Iterable<Double>.averageOrZero(): Double = averageOrNull() ?: 0.0

@JvmName("averageOrZeroOfLong")
fun Iterable<Long>.averageOrZero(): Double = averageOrNull() ?: 0.0
