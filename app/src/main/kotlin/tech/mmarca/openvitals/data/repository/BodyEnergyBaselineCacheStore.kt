package tech.mmarca.openvitals.data.repository

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SharedPreferences cache for the expensive 28-day Body Energy baselines, keyed
 * by date and a caller-supplied signature (permission fingerprint + algorithm
 * version).
 *
 * The day *timelines* used to live here too, encoded as one delimited string per
 * day. They now live in Room ([BodyEnergyTimelineStore]) because the chain has
 * to read a previous day's end score cheaply and a multi-day view needs a range
 * query — neither of which a prefs blob can do. The baselines stayed: they are
 * five numbers a day with working adjacent-day reuse, and moving them would add
 * a table for no benefit.
 */
@Singleton
open class BodyEnergyBaselineCacheStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PrefsFile, Context.MODE_PRIVATE)

    open fun loadBaseline(date: LocalDate, signature: String): BodyEnergyBaselineCacheEntry? =
        prefs.getString(baselineCacheKey(date, signature), null)?.toBaselineOrNull()

    open fun saveBaseline(
        date: LocalDate,
        signature: String,
        baseline: BodyEnergyBaselineCacheEntry,
    ) {
        if (signature.isBlank()) return
        prefs.edit {
            putString(baselineCacheKey(date, signature), baseline.toPreferenceString())
        }
    }

    /**
     * One-shot removal of the retired timeline entries.
     *
     * The prefs store never evicted anything, so an install that has run since
     * the feature shipped is carrying one ~15 KB string per (day × signature)
     * for timelines that now live in Room. Matched narrowly: `2026-07-26|-123`
     * is the only key shape the timeline half ever wrote, and the baseline keys
     * carry a `baseline|` prefix so they cannot match. Flag-guarded so it runs
     * once per install.
     */
    open fun purgeLegacyTimelineEntries() {
        if (prefs.getBoolean(PurgedFlagKey, false)) return
        val stale = prefs.all.keys.filter { LegacyTimelineKey.matches(it) }
        prefs.edit {
            stale.forEach { remove(it) }
            putBoolean(PurgedFlagKey, true)
        }
    }

    private fun baselineCacheKey(date: LocalDate, signature: String): String =
        "baseline|$date|${signature.hashCode()}"

    private companion object {
        const val PrefsFile = "body_energy_timeline_cache"
        const val PurgedFlagKey = "bodyEnergyPrefsTimelinePurged.v1"
        val LegacyTimelineKey = Regex("""^\d{4}-\d{2}-\d{2}\|-?\d+$""")
    }
}

/** Cached day-boundary baselines used to seed the next day's timeline. */
data class BodyEnergyBaselineCacheEntry(
    val baselineRestingHeartRateBpm: Long?,
    val observedMaxHeartRateBpm: Long?,
    val hrvBaselineRmssdMs: Double?,
    val respiratoryRateBaseline: Double?,
    val generatedAt: Instant = Instant.now(),
)

private fun BodyEnergyBaselineCacheEntry.toPreferenceString(): String =
    listOf(
        baselineRestingHeartRateBpm.cacheValue(),
        observedMaxHeartRateBpm.cacheValue(),
        hrvBaselineRmssdMs.cacheValue(),
        respiratoryRateBaseline.cacheValue(),
        generatedAt.toEpochMilli().toString(),
    ).joinToString("|")

private fun String.toBaselineOrNull(): BodyEnergyBaselineCacheEntry? =
    runCatching {
        val parts = split("|")
        BodyEnergyBaselineCacheEntry(
            baselineRestingHeartRateBpm = parts.getOrNull(0).toLongOrNullCache(),
            observedMaxHeartRateBpm = parts.getOrNull(1).toLongOrNullCache(),
            hrvBaselineRmssdMs = parts.getOrNull(2).toDoubleOrNullCache(),
            respiratoryRateBaseline = parts.getOrNull(3).toDoubleOrNullCache(),
            generatedAt = Instant.ofEpochMilli(parts.getOrNull(4)?.toLong() ?: 0L),
        )
    }.getOrNull()

private fun Long?.cacheValue(): String = this?.toString().orEmpty()

private fun Double?.cacheValue(): String = this?.toString().orEmpty()

private fun String?.toLongOrNullCache(): Long? =
    takeUnless { it.isNullOrBlank() }?.toLongOrNull()

private fun String?.toDoubleOrNullCache(): Double? =
    takeUnless { it.isNullOrBlank() }?.toDoubleOrNull()
