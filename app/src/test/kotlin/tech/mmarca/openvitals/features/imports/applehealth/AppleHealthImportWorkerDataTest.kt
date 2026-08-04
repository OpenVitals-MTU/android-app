package tech.mmarca.openvitals.features.imports.applehealth

import android.content.Context
import android.net.Uri
import androidx.work.Data
import io.mockk.every
import io.mockk.mockk
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.features.imports.applehealth.AppleHealthImportWorker.Companion.toData

/**
 * Kotlin analogue of Flutter's `apple_health_import_background_test.dart` isolate-payload cases:
 * the worker's [Data] payloads must round-trip every counter the card and notification rely on.
 */
class AppleHealthImportWorkerDataTest {

    @Test
    fun `category set survives the input data round trip`() {
        val uri = mockk<Uri>()
        every { uri.toString() } returns "content://downloads/1"
        val selected = setOf(
            AppleHealthImportCategory.ACTIVITY,
            AppleHealthImportCategory.WORKOUTS,
        )

        val data = AppleHealthImportWorker.inputData(
            uri = uri,
            selectedCategories = selected,
            expectedSelectedRecords = 12,
            expectedParsedElements = 20,
        )

        assertEquals(selected, AppleHealthImportWorker.selectedCategoriesFromData(data))
        // Missing key decodes to everything selected, mirroring Flutter's decode(null).
        assertEquals(
            AllAppleHealthImportCategories,
            AppleHealthImportWorker.selectedCategoriesFromData(Data.EMPTY),
        )
    }

    @Test
    fun `progress survives the work data round trip`() {
        val progress = AppleHealthImportProgress(
            phase = AppleHealthImportPhase.WRITING,
            parsedRecords = 100,
            convertedRecords = 80,
            importedRecords = 40,
            notSelectedRecords = 5,
            expectedSelectedRecords = 75,
            expectedParsedElements = 200,
        )

        val decoded = AppleHealthImportWorker.progressFromData(progress.toData())

        assertEquals(AppleHealthImportPhase.WRITING, decoded?.phase)
        assertEquals(40, decoded?.importedRecords)
        assertEquals(75, decoded?.expectedSelectedRecords)
        // The scan denominator must survive the trip like every other counter,
        // or the card can never show the scan percent variant.
        assertEquals(200, decoded?.expectedParsedElements)
        assertEquals(progress.percent, decoded?.percent)
        assertNull(AppleHealthImportWorker.progressFromData(Data.EMPTY))
    }

    @Test
    fun `the result payload carries the counters and the store carries the report`() {
        val resultProgress = AppleHealthImportProgress(
            phase = AppleHealthImportPhase.COMPLETE,
            parsedRecords = 12,
            convertedRecords = 12,
            importedRecords = 9,
            duplicateSkippedRecords = 1,
            notSelectedRecords = 2,
            unsupportedElements = 3,
            expectedSelectedRecords = 12,
        )
        val payload = resultProgress.toData(reportPath = null, workoutRoutesIncomplete = true)

        val decoded = AppleHealthImportWorker.resultFromData(payload, "REPORT_FROM_STORE")

        assertEquals(9, decoded?.importedRecords)
        assertEquals(1, decoded?.duplicateSkippedRecords)
        assertEquals(true, decoded?.workoutRoutesIncomplete)
        assertEquals("REPORT_FROM_STORE", decoded?.shareableReportText)
    }

    @Test
    fun `the error payload carries the details and the permission flag`() {
        val data = AppleHealthImportWorker.errorData(
            reportContext(),
            IllegalStateException("boom"),
        )

        assertTrue(data.getString(AppleHealthImportWorker.KeyError).orEmpty().contains("boom"))
        assertFalse(data.getBoolean(AppleHealthImportWorker.KeyPermissionDenied, true))
    }

    @Test
    fun `a permission denial raises the flag the card acts on`() {
        val data = AppleHealthImportWorker.errorData(
            reportContext(),
            SecurityException("steps write"),
        )

        assertTrue(data.getBoolean(AppleHealthImportWorker.KeyPermissionDenied, false))
    }

    private fun reportContext(): Context {
        val filesDir = Files.createTempDirectory("apple-health-error-data").toFile()
        return mockk<Context>().also { context ->
            every { context.filesDir } returns filesDir
        }
    }
}
