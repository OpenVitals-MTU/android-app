package tech.mmarca.openvitals.features.imports.applehealth

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import java.io.File
import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** A full copy of someone's health export must not outlive an analysis they walked away from. */
class AppleHealthImportStagingStoreTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val now: Instant = Instant.parse("2026-09-20T12:00:00Z")
    private val maxAge: Duration = Duration.ofDays(1)

    private fun context(): Context = mockk { every { filesDir } returns folder.root }

    private fun stagedFile(copiedAt: Instant): File {
        val directory = File(folder.root, "apple_health_import").apply { mkdirs() }
        File(directory, "staged_export.properties").apply { writeText("x") }.setLastModified(copiedAt.toEpochMilli())
        return File(directory, "staged_export.bin").apply {
            writeText("a health export")
            setLastModified(copiedAt.toEpochMilli())
        }
    }

    @Test
    fun `a copy older than a day is removed`() {
        val file = stagedFile(copiedAt = now.minus(Duration.ofHours(25)))

        assertTrue(AppleHealthImportStagingStore.clearIfOlderThan(context(), maxAge, now))

        assertFalse(file.exists())
        assertFalse(file.parentFile!!.exists())
    }

    @Test
    fun `a copy from this morning is kept for the import that may follow`() {
        val file = stagedFile(copiedAt = now.minus(Duration.ofHours(3)))

        assertFalse(AppleHealthImportStagingStore.clearIfOlderThan(context(), maxAge, now))

        assertTrue(file.exists())
    }

    @Test
    fun `nothing staged means nothing to do`() {
        assertFalse(AppleHealthImportStagingStore.clearIfOlderThan(context(), maxAge, now))
    }
}
