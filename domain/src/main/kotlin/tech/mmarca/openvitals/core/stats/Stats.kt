package tech.mmarca.openvitals.core.stats

/**
 * The empty-list contract for aggregates. [averageOrNull] means "unknown";
 * [averageOrZero] means "the true total was zero". Never NaN, which slips
 * past every guard and renders literally.
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
