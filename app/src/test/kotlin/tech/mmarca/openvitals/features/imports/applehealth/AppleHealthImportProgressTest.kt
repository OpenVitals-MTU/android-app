package tech.mmarca.openvitals.features.imports.applehealth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppleHealthImportProgressTest {

    @Test
    fun `percent is unavailable until selected record total is known`() {
        val progress = AppleHealthImportProgress(
            phase = AppleHealthImportPhase.PARSING,
            convertedRecords = 4,
        )

        assertNull(progress.percent)
    }

    @Test
    fun `percent uses selected record total as denominator`() {
        val progress = AppleHealthImportProgress(
            phase = AppleHealthImportPhase.PARSING,
            convertedRecords = 6,
            notSelectedRecords = 2,
            expectedSelectedRecords = 8,
        )

        assertEquals(44, progress.percent)
    }

    @Test
    fun `percent does not count unselected records or generic skips as selected progress`() {
        val progress = AppleHealthImportProgress(
            phase = AppleHealthImportPhase.PARSING,
            convertedRecords = 10,
            notSelectedRecords = 6,
            skippedRecords = 3,
            expectedSelectedRecords = 8,
        )

        assertEquals(44, progress.percent)
    }

    @Test
    fun `percent reserves final steps for duplicate checks writing and report`() {
        val duplicateCheck = AppleHealthImportProgress(
            phase = AppleHealthImportPhase.CHECKING_DUPLICATES,
            convertedRecords = 8,
            expectedSelectedRecords = 8,
        )
        val writing = duplicateCheck.copy(phase = AppleHealthImportPhase.WRITING)
        val report = duplicateCheck.copy(phase = AppleHealthImportPhase.BUILDING_REPORT)
        val complete = duplicateCheck.copy(phase = AppleHealthImportPhase.COMPLETE)

        assertEquals(88, duplicateCheck.percent)
        assertEquals(92, writing.percent)
        assertEquals(98, report.percent)
        assertEquals(100, complete.percent)
    }
}
