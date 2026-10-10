package tech.mmarca.openvitals.wear

import tech.mmarca.openvitals.wearlink.WearLinkProtocol

/**
 * One series the watch records for the phone, and everything [MetricStore]
 * needs to keep it: a stable key, how a value becomes the row's text and
 * back, its time, what a second value at the same time does, and how long
 * rows stay.
 *
 * The row text is the value's line in the `:wearlink` protocol, so the
 * format is defined and tested once, in that module, and what the store
 * holds is exactly what the phone receives. A new metric is one more
 * definition in [WearMetrics] plus its line in `:wearlink`; the store, its
 * pruning and its tests do not change.
 */
class WearMetric<T>(
    /** The table key. Never reused or renamed: it is how the rows are found. */
    val key: String,
    val encode: (T) -> String,
    /** Null for a row this build cannot read; such a row is skipped, never fatal. */
    val decode: (String) -> T?,
    val timeOf: (T) -> Long,
    val onSameTime: OnSameTime,
    val retentionMillis: Long = WEEK_MILLIS,
) {
    /** What a value does when the store already holds one at its time. */
    enum class OnSameTime {
        /** The first value stays: a sample is final once taken. */
        KEEP,

        /** The new value replaces it: a row that is rewritten as its period fills in. */
        REPLACE,
    }

    override fun toString(): String = "WearMetric($key)"

    companion object {
        /** A week: long enough to miss a few days of syncing, small enough to stay bounded. */
        const val WEEK_MILLIS: Long = 7L * 24 * 60 * 60 * 1000
    }
}

/** Every series the watch records. */
object WearMetrics {

    /** One heart rate sample every ten seconds at most; see `HeartRateRecorder`. */
    val HEART_RATE = WearMetric(
        key = WearLinkProtocol.CAP_HEART_RATE,
        encode = { s: WearLinkProtocol.HeartRateSample -> WearLinkProtocol.formatSample(s.epochMillis, s.bpm) },
        decode = WearLinkProtocol::parseSample,
        timeOf = { it.epochMillis },
        onSameTime = WearMetric.OnSameTime.KEEP,
    )

    /**
     * One row per clock minute, the sleep pipeline's input; see
     * `docs/engineering/sleep-minute-features.md`. A minute is written again
     * while late readings for it arrive, so the newest row wins.
     */
    val SLEEP_MINUTES = WearMetric(
        key = WearLinkProtocol.CAP_SLEEP_MINUTES,
        encode = WearLinkProtocol::formatSleepMinute,
        decode = WearLinkProtocol::parseSleepMinute,
        timeOf = { it.epochMillis },
        onSameTime = WearMetric.OnSameTime.REPLACE,
    )

    val ALL: List<WearMetric<*>> = listOf(HEART_RATE, SLEEP_MINUTES)
}
