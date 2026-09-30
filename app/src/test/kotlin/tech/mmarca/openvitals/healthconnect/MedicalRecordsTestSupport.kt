package tech.mmarca.openvitals.healthconnect

import android.util.Log
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.TestDispatcher
import tech.mmarca.openvitals.core.performance.DispatcherProvider

/** Shared set-up for the medical records tests: logging, a reader support, record JSON. */
internal object MedicalRecordsTestSupport {
    fun mockLog() {
        HealthConnectRateLimitBackoff.resetForTest()
        mockkStatic(Log::class)
        every { Log.d(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>(), any()) } returns 0
        every { Log.e(any(), any<String>(), any()) } returns 0
    }

    fun unmockLog() = unmockkStatic(Log::class)

    fun dispatchers(dispatcher: TestDispatcher): DispatcherProvider =
        object : DispatcherProvider {
            override val main = dispatcher
            override val io = dispatcher
            override val default = dispatcher
        }

    /** The medical reader reaches Health Connect through its own client, so this one is never called. */
    fun support(dispatchers: DispatcherProvider): HealthConnectReaderSupport {
        val diagnostics = mockk<HealthConnectDiagnostics>()
        every { diagnostics.summary() } returns "test"
        return HealthConnectReaderSupport(
            clientProvider = { error("The medical reader must use its MedicalRecordsClient") },
            diagnostics = diagnostics,
            rateLimitMessage = { "rate limited" },
            dispatchers = dispatchers,
        )
    }

    fun reader(client: FakeMedicalRecordsClient, dispatchers: DispatcherProvider, available: () -> Boolean = { true }) =
        MedicalRecordsHealthReader(support(dispatchers), client, FakeMedicalRecordsClient.OWN_PACKAGE, available)

    fun immunization(id: String, vaccine: String = "Tetanus"): String =
        """{"resourceType":"Immunization","id":"$id","status":"completed",""" +
            """"vaccineCode":{"text":"$vaccine"},"patient":{"reference":"Patient/self"},""" +
            """"occurrenceDateTime":"2021-05-01"}"""

    fun condition(id: String): String =
        """{"resourceType":"Condition","id":"$id","code":{"text":"Asthma"},"subject":{"reference":"Patient/self"}}"""
}
