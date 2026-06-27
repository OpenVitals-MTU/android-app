package tech.mmarca.openvitals.healthconnect

import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

internal class HealthConnectReaderSupport(
    private val clientProvider: () -> HealthConnectClient,
    private val diagnostics: HealthConnectDiagnostics,
    private val rateLimitMessage: (Long) -> String,
    private val syncEnabled: () -> Boolean = { true },
) {
    private val readSemaphore = Semaphore(MaxConcurrentReads)

    fun client(): HealthConnectClient = clientProvider()

    fun diagnosticsSummary(): String = diagnostics.summary()

    suspend fun <T> withLogging(
        operation: String,
        fallback: T,
        block: suspend () -> T,
    ): T = withRateLimitRetry(operation, fallback, block)

    suspend fun <T> withNullableLogging(
        operation: String,
        block: suspend () -> T?,
    ): T? = withRateLimitRetry(operation, null, block)

    private suspend fun <T> withRateLimitRetry(
        operation: String,
        fallback: T,
        block: suspend () -> T,
    ): T {
        val safeOperation = operation.privacySafeOperationName()
        var hasRetriedRateLimit = false
        var result: Result<T>? = null

        while (result == null) {
            if (!syncEnabled()) {
                Log.d(TAG, "Skipping $safeOperation - Health Connect sync paused")
                return fallback
            }
            waitForActiveRateLimit(safeOperation)
            Log.d(TAG, "Starting $safeOperation ${diagnosticsSummary()}")

            try {
                result = readSemaphore.withPermit {
                    withContext(Dispatchers.IO) {
                        Result.success(block().also {
                            Log.d(TAG, "Finished $safeOperation successfully")
                        })
                    }
                }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                if (HealthConnectRateLimitBackoff.isRateLimitFailure(t)) {
                    val rateLimit = HealthConnectRateLimitBackoff.markRateLimited(t, rateLimitMessage)
                    Log.w(TAG, "Rate limited $safeOperation ${diagnosticsSummary()}", t)
                    if (!hasRetriedRateLimit) {
                        hasRetriedRateLimit = true
                        delay(rateLimit.retryAfterMillis)
                        continue
                    }
                } else {
                    Log.e(TAG, "Failed $safeOperation ${diagnosticsSummary()}", t)
                }
                result = Result.success(fallback)
            }
        }

        return result.getOrThrow()
    }

    private suspend fun waitForActiveRateLimit(operation: String) {
        try {
            HealthConnectRateLimitBackoff.throwIfActive(rateLimitMessage)
        } catch (rateLimit: HealthConnectRateLimitException) {
            Log.w(TAG, "Waiting to retry $operation after Health Connect rate limit")
            delay(rateLimit.retryAfterMillis)
        }
    }

    fun dayRange(date: LocalDate): Pair<Instant, Instant> {
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant()
        val end = if (date == LocalDate.now(zone)) {
            Instant.now()
        } else {
            date.plusDays(1).atStartOfDay(zone).toInstant()
        }
        return start to end
    }

    private companion object {
        private const val TAG = "HealthConnectManager"
        private const val MaxConcurrentReads = 2
    }
}

private fun String.privacySafeOperationName(): String =
    substringBefore('[')
