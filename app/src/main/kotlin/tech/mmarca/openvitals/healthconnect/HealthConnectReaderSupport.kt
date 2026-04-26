package tech.mmarca.openvitals.healthconnect

import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

internal class HealthConnectReaderSupport(
    private val clientProvider: () -> HealthConnectClient,
    private val diagnostics: HealthConnectDiagnostics,
) {
    fun client(): HealthConnectClient = clientProvider()

    fun diagnosticsSummary(): String = diagnostics.summary()

    suspend fun <T> withLogging(
        operation: String,
        fallback: T,
        block: suspend () -> T,
    ): T = try {
        Log.d(TAG, "Starting $operation ${diagnosticsSummary()}")
        block().also {
            Log.d(TAG, "Finished $operation successfully")
        }
    } catch (t: Throwable) {
        Log.e(TAG, "Failed $operation ${diagnosticsSummary()}", t)
        fallback
    }

    suspend fun <T> withNullableLogging(
        operation: String,
        block: suspend () -> T?,
    ): T? = try {
        Log.d(TAG, "Starting $operation ${diagnosticsSummary()}")
        block().also {
            Log.d(TAG, "Finished $operation successfully")
        }
    } catch (t: Throwable) {
        Log.e(TAG, "Failed $operation ${diagnosticsSummary()}", t)
        null
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
    }
}
