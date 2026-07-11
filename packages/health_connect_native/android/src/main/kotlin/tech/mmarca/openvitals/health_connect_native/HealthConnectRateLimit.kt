package tech.mmarca.openvitals.health_connect_native

import kotlin.math.ceil

/**
 * Ported from the native OpenVitals app (`healthconnect/HealthConnectRateLimit.kt`).
 *
 * Health Connect enforces per-app read/write quotas. When exceeded it throws with
 * a "rate limited" / "quota has been exceeded" message. This process-global
 * backoff records a cool-off window so that concurrent/subsequent reads wait it
 * out instead of hammering the quota further.
 */
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
        rateLimitMarkers.any { marker ->
          failure.message?.contains(marker, ignoreCase = true) == true
        }
    }

  private fun remainingMillis(nowMillis: Long): Long =
    (retryAfterEpochMillis - nowMillis).coerceAtLeast(0L)
}

internal fun retryAfterMinutes(retryAfterMillis: Long): Long =
  ceil(retryAfterMillis.coerceAtLeast(1L) / 60_000.0).toLong().coerceAtLeast(1L)
