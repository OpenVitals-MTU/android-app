package tech.mmarca.openvitals.data.source.sync

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import java.io.File
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import tech.mmarca.openvitals.data.repository.TestDispatcherProvider
import tech.mmarca.openvitals.features.devicesync.protocol.SyncReport
import tech.mmarca.openvitals.features.devicesync.protocol.SyncTypeSummary
import tech.mmarca.openvitals.features.devicesync.protocol.buildSyncReportText
import tech.mmarca.openvitals.features.devicesync.store.DeviceSyncReportStore

/**
 * Port of the Flutter `device_sync_report_store_test.dart` suite. The Dart
 * store takes a directory resolver; the Kotlin one derives its directory from
 * `Context.filesDir`, so the temp directory is injected through a mocked
 * context instead.
 */
class DeviceSyncReportStoreTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val at: Instant = Instant.parse("2026-01-20T14:30:00Z")

    @Test
    fun `renders the summary and per-type lines for a completed sync`() {
        val report = SyncReport(
            completed = true,
            peerDeviceName = "Galaxy S23",
            negotiatedTypes = listOf("WeightRecord"),
            itemsSent = 10,
            itemsReceived = 8,
            imported = 6,
            duplicateSkipped = 2,
            typeSummaries = listOf(
                SyncTypeSummary(
                    recordType = "WeightRecord",
                    received = 8,
                    imported = 6,
                    duplicateSkipped = 2,
                ),
            ),
        )

        val text = buildSyncReportText(report, generatedAt = at)

        assertTrue(text.contains("Status: completed"))
        assertTrue(text.contains("Peer: Galaxy S23"))
        assertTrue(text.contains("Imported: 6"))
        assertTrue(text.contains("Already had (skipped): 2"))
        assertTrue(text.contains("WeightRecord: received 8, imported 6, skipped 2"))
        assertTrue(text.contains("2026-01-20T14:30"))
    }

    @Test
    fun `renders the abort reason for an aborted sync`() {
        val report = SyncReport(
            completed = false,
            peerDeviceName = "unknown",
            negotiatedTypes = emptyList(),
            itemsSent = 0,
            itemsReceived = 0,
            imported = 0,
            duplicateSkipped = 0,
            typeSummaries = emptyList(),
            abortReason = "pairing code did not match",
        )

        val text = buildSyncReportText(report, generatedAt = at)

        assertTrue(text.contains("Status: aborted"))
        assertTrue(text.contains("Reason: pairing code did not match"))
        assertTrue(text.contains("(none)"))
    }

    @Test
    fun `round-trips a report through a file`() = runTest {
        val filesDir: File = temporaryFolder.newFolder("files")
        val context = mockk<Context> {
            every { this@mockk.filesDir } returns filesDir
        }
        val store = DeviceSyncReportStore(context, TestDispatcherProvider)

        assertEquals("", store.readReport())
        store.writeReport("hello report")
        assertEquals("hello report", store.readReport())
    }
}
