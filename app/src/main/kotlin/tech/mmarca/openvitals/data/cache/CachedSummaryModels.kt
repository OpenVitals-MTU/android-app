package tech.mmarca.openvitals.data.cache

import java.time.LocalDate
import tech.mmarca.openvitals.domain.model.RefreshMode

data class CachedSummaryKey(
    val surface: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val metricSet: String,
    val permissionFingerprint: String,
    val configHash: String,
    val schemaVersion: Int,
)

data class CachedSummaryEntry(
    val key: CachedSummaryKey,
    val payloadJson: String,
    val writtenAtMillis: Long,
)

enum class CachedSummaryFreshness {
    FRESH,
    STALE,
    EXPIRED,
    MISS,
}

data class CachedSummaryRead(
    val entry: CachedSummaryEntry?,
    val freshness: CachedSummaryFreshness,
) {
    val isUsable: Boolean
        get() = freshness == CachedSummaryFreshness.FRESH || freshness == CachedSummaryFreshness.STALE
}

internal data class CachedSummaryPolicy(
    val freshMillis: Long,
    val staleUsableMillis: Long,
)

internal fun summaryPolicyFor(
    referenceDate: LocalDate,
    today: LocalDate,
): CachedSummaryPolicy {
    val ageDays = java.time.temporal.ChronoUnit.DAYS.between(referenceDate, today)
    return when {
        ageDays <= 0L -> CachedSummaryPolicy(
            freshMillis = 5.minutes,
            staleUsableMillis = 24.hours,
        )
        ageDays <= 14L -> CachedSummaryPolicy(
            freshMillis = 6.hours,
            staleUsableMillis = 14.days,
        )
        else -> CachedSummaryPolicy(
            freshMillis = 7.days,
            staleUsableMillis = 90.days,
        )
    }
}

internal fun RefreshMode.allowsPersistentCache(): Boolean = this != RefreshMode.FORCE

private val Int.minutes: Long get() = this * 60_000L
private val Int.hours: Long get() = this * 60 * 60_000L
private val Int.days: Long get() = this * 24 * 60 * 60_000L
