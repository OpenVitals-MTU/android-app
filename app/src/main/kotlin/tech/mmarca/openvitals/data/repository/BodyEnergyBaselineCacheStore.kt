package tech.mmarca.openvitals.data.repository

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SharedPreferences cache for the 28-day Body Energy baselines, keyed by
 * date and signature. The day timelines moved to Room; the baselines are
 * five numbers a day and stayed.
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

    /** One-shot removal of the retired timeline entries, matched narrowly by key shape. Flag-guarded. */
    open fun purgeLegacyTimelineEntries() {
        if (prefs.getBoolean(PurgedFlagKey, false)) return
        val stale = prefs.all.keys.filter { LegacyTimelineKey.matches(it) }
        prefs.edit {
            stale.forEach { remove(it) }
            putBoolean(PurgedFlagKey, true)
        }
    }

    /** Drops every cached baseline. The legacy-purge flag survives. */
    open fun clearBaselines() {
        val cached = prefs.all.keys.filter { it.startsWith(BaselineKeyPrefix) }
        if (cached.isEmpty()) return
        prefs.edit {
            cached.forEach { remove(it) }
        }
    }

    private fun baselineCacheKey(date: LocalDate, signature: String): String =
        "$BaselineKeyPrefix$date|${signature.hashCode()}"

    private companion object {
        const val PrefsFile = "body_energy_timeline_cache"
        const val BaselineKeyPrefix = "baseline|"
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
