package tech.mmarca.openvitals.features.settings

import java.time.Instant

/**
 * One app contributing to Health Connect. A diagnostic: a WearOS watch
 * shows up as its vendor app's package.
 */
data class HealthConnectSource(
    val packageName: String,
    val recordCount: Int,
    val lastSeen: Instant,
    val metrics: Set<String>,
) {
    /** A friendly name for the well-known contributors, else the raw package. */
    val displayName: String
        get() = FRIENDLY_NAMES[packageName] ?: packageName

    companion object {
        const val UNKNOWN_PACKAGE = "unknown"

        private val FRIENDLY_NAMES = mapOf(
            "tech.mmarca.openvitals" to "OpenVitals (this app)",
            "com.sec.android.app.shealth" to "Samsung Health",
            "com.google.android.apps.healthdata" to "Health Connect",
            "com.google.android.apps.fitness" to "Google Fit",
            "com.fitbit.FitbitMobile" to "Fitbit",
            "com.garmin.android.apps.connectmobile" to "Garmin Connect",
            UNKNOWN_PACKAGE to "Unknown source",
        )
    }
}

/** Folds (source, time) observations per metric into a list, most recent first. */
fun aggregateHealthConnectSources(
    byMetric: Map<String, List<Pair<String, Instant>>>,
): List<HealthConnectSource> {
    data class Acc(var count: Int, var lastSeen: Instant, val metrics: MutableSet<String>)

    val acc = mutableMapOf<String, Acc>()
    for ((metric, observations) in byMetric) {
        for ((source, time) in observations) {
            val key = source.trim().ifEmpty { HealthConnectSource.UNKNOWN_PACKAGE }
            val existing = acc[key]
            if (existing == null) {
                acc[key] = Acc(count = 1, lastSeen = time, metrics = mutableSetOf(metric))
            } else {
                existing.count += 1
                if (time.isAfter(existing.lastSeen)) existing.lastSeen = time
                existing.metrics += metric
            }
        }
    }
    return acc.map { (packageName, value) ->
        HealthConnectSource(
            packageName = packageName,
            recordCount = value.count,
            lastSeen = value.lastSeen,
            metrics = value.metrics,
        )
    }.sortedByDescending { it.lastSeen }
}
