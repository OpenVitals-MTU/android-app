package tech.mmarca.openvitals.healthconnect

import android.os.Build
import kotlin.math.ceil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class HealthConnectRateLimitException(
    val retryAfterMillis: Long,
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

/**
 * The process-wide "Health Connect is refusing us" state.
 *
 * One deadline for every reader: a quota is per app, not per read. The deadline
 * escalates along [BackoffLadderMillis] when hits keep coming inside
 * [EscalationWindowMillis], because the standalone Health Connect app's
 * quota is a window, and a minute's wait followed by the same burst just
 * refills the refusal. A hit from a read that was already in flight when the
 * last one landed is the same burst, not a new one, and moves nothing.
 */
internal object HealthConnectRateLimitBackoff {
    /** Successive hits inside the window wait longer each time; the last step repeats. */
    private val BackoffLadderMillis = longArrayOf(60_000L, 120_000L, 300_000L, 900_000L)

    /** A hit this long after the previous one starts the ladder over. */
    private const val EscalationWindowMillis = 30L * 60_000L

    private val rateLimitMarkers = listOf(
        "rate limited",
        "quota has been exceeded",
    )

    private val lock = Any()
    private var retryAfterEpochMillis: Long = 0L
    private var lastHitAtEpochMillis: Long = 0L
    private var streak: Int = 0
    private var hasHit = false

    private val deadline = MutableStateFlow(0L)

    /** Epoch millis the backoff ends at, or 0 while none is armed. A screen counts down from it. */
    val deadlineEpochMillis: StateFlow<Long> = deadline

    /** True once this process has been refused at all. The pacer keeps a wider gap from then on. */
    val hasBeenRateLimited: Boolean
        get() = hasHit

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

    /**
     * Records a refusal. [readStartedAtMillis] is when the refused call was
     * issued: a call that was already out when the last hit was recorded
     * belongs to that hit and does not escalate.
     */
    fun markRateLimited(
        cause: Throwable,
        messageForRetryAfter: (Long) -> String,
        nowMillis: Long = System.currentTimeMillis(),
        readStartedAtMillis: Long = nowMillis,
    ): HealthConnectRateLimitException {
        val remainingMillis = synchronized(lock) {
            val sameBurst = hasHit &&
                readStartedAtMillis < lastHitAtEpochMillis &&
                retryAfterEpochMillis > nowMillis
            if (!sameBurst) {
                val withinWindow = hasHit &&
                    nowMillis - lastHitAtEpochMillis < EscalationWindowMillis
                streak = if (withinWindow) streak + 1 else 1
                val step = BackoffLadderMillis[minOf(streak, BackoffLadderMillis.size) - 1]
                retryAfterEpochMillis = maxOf(retryAfterEpochMillis, nowMillis + step)
                lastHitAtEpochMillis = nowMillis
                hasHit = true
                deadline.value = retryAfterEpochMillis
            }
            (retryAfterEpochMillis - nowMillis).coerceAtLeast(0L)
        }
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

    /** androidx rewraps the quota error as a bare IllegalStateException, so the cause chain is checked too. */
    private fun Throwable.isPlatformRateLimitException(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return false
        return this is android.health.connect.HealthConnectException &&
            errorCode == android.health.connect.HealthConnectException.ERROR_RATE_LIMIT_EXCEEDED
    }

    internal fun resetForTest() {
        synchronized(lock) {
            retryAfterEpochMillis = 0L
            lastHitAtEpochMillis = 0L
            streak = 0
            hasHit = false
            deadline.value = 0L
        }
    }

    /** How much longer the backoff runs, or 0. Callers decide whether to wait. */
    fun remainingMillis(nowMillis: Long = System.currentTimeMillis()): Long {
        val remaining = (retryAfterEpochMillis - nowMillis).coerceAtLeast(0L)
        // The flow clears itself the first time anyone looks after the deadline.
        if (remaining == 0L && deadline.value != 0L) deadline.value = 0L
        return remaining
    }
}

internal fun retryAfterMinutes(retryAfterMillis: Long): Long =
    ceil(retryAfterMillis.coerceAtLeast(1L) / 60_000.0).toLong().coerceAtLeast(1L)
