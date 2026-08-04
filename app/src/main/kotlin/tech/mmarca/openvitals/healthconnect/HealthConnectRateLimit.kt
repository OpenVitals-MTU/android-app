package tech.mmarca.openvitals.healthconnect

import android.os.Build
import kotlin.math.ceil

internal class HealthConnectRateLimitException(
    val retryAfterMillis: Long,
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

internal object HealthConnectRateLimitBackoff {
    private const val DEFAULT_BACKOFF_MILLIS = 60_000L
    private val rateLimitMarkers = listOf(
        "rate limited",
        "quota has been exceeded",
    )

    @Volatile
    private var retryAfterEpochMillis: Long = 0L

    fun throwIfActive(
        messageForRetryAfter: (Long) -> String,
        nowMillis: Long = System.currentTimeMillis(),
    ) {
        val remainingMillis = remainingMillis(nowMillis)
        if (remainingMillis > 0L) {
            throw HealthConnectRateLimitException(
                retryAfterMillis = remainingMillis,
                message = messageForRetryAfter(remainingMillis),
            )
        }
    }

    fun markRateLimited(
        cause: Throwable,
        messageForRetryAfter: (Long) -> String,
        nowMillis: Long = System.currentTimeMillis(),
    ): HealthConnectRateLimitException {
        retryAfterEpochMillis = maxOf(retryAfterEpochMillis, nowMillis + DEFAULT_BACKOFF_MILLIS)
        val remainingMillis = remainingMillis(nowMillis)
        return HealthConnectRateLimitException(
            retryAfterMillis = remainingMillis,
            message = messageForRetryAfter(remainingMillis),
            cause = cause,
        )
    }

    fun isRateLimitFailure(t: Throwable): Boolean =
        generateSequence(t) { it.cause }.any { failure ->
            failure is HealthConnectRateLimitException ||
                failure.isPlatformRateLimitException() ||
                rateLimitMarkers.any { marker ->
                    failure.message?.contains(marker, ignoreCase = true) == true
                }
        }

    /**
     * androidx rewraps the platform quota error as a bare IllegalStateException
     * whose message is not guaranteed to carry the markers above, so on API 34+
     * the cause chain is also checked for the typed platform error code.
     */
    private fun Throwable.isPlatformRateLimitException(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return false
        return this is android.health.connect.HealthConnectException &&
            errorCode == android.health.connect.HealthConnectException.ERROR_RATE_LIMIT_EXCEEDED
    }

    internal fun resetForTest() {
        retryAfterEpochMillis = 0L
    }

    private fun remainingMillis(nowMillis: Long): Long =
        (retryAfterEpochMillis - nowMillis).coerceAtLeast(0L)
}

internal fun retryAfterMinutes(retryAfterMillis: Long): Long =
    ceil(retryAfterMillis.coerceAtLeast(1L) / 60_000.0).toLong().coerceAtLeast(1L)
