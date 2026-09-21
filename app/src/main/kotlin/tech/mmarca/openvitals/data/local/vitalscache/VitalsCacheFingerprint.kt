package tech.mmarca.openvitals.data.local.vitalscache

import java.time.ZoneId

/**
 * The cursor row that holds the cache's fingerprint, beside the change tokens it guards.
 * No metric uses this key, and the row needs no schema change.
 */
const val VitalsCacheFingerprintKey: String = "__fingerprint__"

/**
 * What a cached day depends on besides the records themselves: which reads are granted,
 * the history grant included, and the zone whose midnight cut the days. When it changes the
 * cached days are wrong, not stale: a day cut at another midnight, or a history read that
 * stopped at 30 days.
 */
fun vitalsCacheFingerprint(granted: Set<String>, zone: ZoneId): String {
    val reads = granted.filter { it.startsWith("android.permission.health.READ_") }.sorted()
    return "v1|${zone.id}|${reads.joinToString(",").hashCode()}"
}

/** True when the stored fingerprint is the current one. A cache with no fingerprint yet is taken as it is. */
suspend fun VitalsDailyCacheDao.matchesFingerprint(granted: Set<String>, zone: ZoneId): Boolean {
    val stored = cursor(VitalsCacheFingerprintKey)?.changesToken ?: return true
    return stored == vitalsCacheFingerprint(granted, zone)
}
