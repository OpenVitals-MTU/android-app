package tech.mmarca.openvitals.data.cache

import android.util.Log
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.core.performance.DefaultDispatcherProvider
import tech.mmarca.openvitals.core.performance.DispatcherProvider
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.domain.model.RefreshMode

class CachedPeriodRepositoryLoader(
    private val cacheStore: MetricSummaryCacheStore?,
    private val appScope: CoroutineScope?,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider,
    private val tag: String,
) {
    suspend fun <T> load(
        key: CachedSummaryKey,
        refreshMode: RefreshMode,
        referenceDate: LocalDate = key.endDate,
        decode: (String) -> T,
        encode: (T) -> String,
        loadFresh: suspend () -> T,
    ): T {
        val store = cacheStore
        if (store != null) {
            val cached = store.read(
                key = key,
                referenceDate = referenceDate,
                refreshMode = refreshMode,
            )
            if (cached.isUsable) {
                runCatching { decode(checkNotNull(cached.entry).payloadJson) }
                    .onSuccess { value ->
                        if (cached.freshness == CachedSummaryFreshness.STALE) {
                            refreshInBackground(key, encode, loadFresh)
                        }
                        Log.d(tag, "period cacheHit surface=${key.surface} freshness=${cached.freshness}")
                        return value
                    }
                    .onFailure {
                        store.invalidate(key)
                    }
            }
        }

        val fresh = loadFresh()
        store?.write(key, encode(fresh))
        return fresh
    }

    private fun <T> refreshInBackground(
        key: CachedSummaryKey,
        encode: (T) -> String,
        loadFresh: suspend () -> T,
    ) {
        val store = cacheStore ?: return
        val scope = appScope ?: return
        scope.launch(dispatchers.io) {
            runCatching {
                val fresh = loadFresh()
                store.write(key, encode(fresh))
            }.onFailure { error ->
                Log.w(tag, "Background period cache refresh failed surface=${key.surface}", error)
            }
        }
    }
}

fun periodSummaryKey(
    surface: String,
    query: PeriodLoadQuery,
    metricSet: String,
    permissionFingerprint: String,
    schemaVersion: Int,
    extraConfig: List<String> = emptyList(),
): CachedSummaryKey =
    CachedSummaryKey(
        surface = surface,
        startDate = query.windows.current.start,
        endDate = query.windows.current.end,
        metricSet = metricSet,
        permissionFingerprint = permissionFingerprint,
        configHash = periodConfigParts(query, extraConfig).joinToString(separator = "|"),
        schemaVersion = schemaVersion,
    )

private fun periodConfigParts(
    query: PeriodLoadQuery,
    extraConfig: List<String>,
): List<String> =
    buildList {
        add("range=${query.range.name}")
        add("anchor=${query.anchorDate}")
        add("selected=${query.selectedDate}")
        add("today=${query.today}")
        add("baselineDays=${query.baselineDays}")
        add("weekMode=${query.weekPeriodMode.name}")
        add("current=${query.windows.current.cachePart()}")
        add("previous=${query.windows.previous.cachePart()}")
        add("baseline=${query.windows.baseline.cachePart()}")
        addAll(extraConfig)
    }

private fun DatePeriod.cachePart(): String = "$start..$end"
