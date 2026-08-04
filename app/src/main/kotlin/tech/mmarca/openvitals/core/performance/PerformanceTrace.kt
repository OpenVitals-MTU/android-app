package tech.mmarca.openvitals.core.performance

import android.util.Log

object PerformanceTrace {
    private const val TAG = "OpenVitalsPerf"

    suspend fun <T> timed(
        name: String,
        attributes: Map<String, Any?> = emptyMap(),
        block: suspend () -> T,
    ): T {
        val startedAt = System.currentTimeMillis()
        return try {
            block()
        } finally {
            val durationMs = System.currentTimeMillis() - startedAt
            val suffix = attributes
                .filterValues { it != null }
                .entries
                .joinToString(separator = " ") { (key, value) -> "$key=$value" }
            Log.d(TAG, "$name durationMs=$durationMs${suffix.takeIf { it.isNotBlank() }?.let { " $it" }.orEmpty()}")
        }
    }
}
