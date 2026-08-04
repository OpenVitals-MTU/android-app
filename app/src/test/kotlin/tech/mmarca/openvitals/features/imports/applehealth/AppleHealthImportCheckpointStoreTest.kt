package tech.mmarca.openvitals.features.imports.applehealth

import android.content.Context
import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import java.io.File
import java.nio.file.Files
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AppleHealthImportCheckpointStoreTest {

    private val filesDir = Files.createTempDirectory("apple-health-checkpoint").toFile()
    private val context = mockk<Context>().also { context ->
        every { context.filesDir } returns filesDir
    }

    private val sourceKey = "content://downloads/1|export.zip|4096"
    private val categories = setOf(
        AppleHealthImportCategory.HEART,
        AppleHealthImportCategory.BODY,
    )
    private val checkpoint = AppleHealthImportCheckpoint(
        sourceKey = sourceKey,
        selectedCategories = categories,
        committedSelectedRecords = 300,
        importedRecords = 280,
        duplicateSkippedRecords = 15,
        failedRecords = 5,
        typeStats = mapOf(
            "HKQuantityTypeIdentifierHeartRate" to AppleHealthImportCheckpointTypeStats(
                imported = 280,
                duplicateSkipped = 15,
                failed = 5,
            ),
        ),
    )

    @After
    fun tearDown() {
        filesDir.deleteRecursively()
    }

    @Test
    fun `round-trips a checkpoint for the same source and categories`() {
        AppleHealthImportCheckpointStore.save(context, checkpoint)

        val loaded = AppleHealthImportCheckpointStore.load(context, sourceKey, categories)

        assertNotNull(loaded)
        assertEquals(300, loaded?.committedSelectedRecords)
        assertEquals(280, loaded?.importedRecords)
        assertEquals(15, loaded?.duplicateSkippedRecords)
        assertEquals(5, loaded?.failedRecords)
        assertEquals(
            AppleHealthImportCheckpointTypeStats(
                imported = 280,
                duplicateSkipped = 15,
                failed = 5,
            ),
            loaded?.typeStats?.get("HKQuantityTypeIdentifierHeartRate"),
        )
        assertEquals(categories, loaded?.selectedCategories)
    }

    @Test
    fun `is not reused when the source key differs`() {
        AppleHealthImportCheckpointStore.save(context, checkpoint)

        val loaded = AppleHealthImportCheckpointStore.load(
            context,
            "content://downloads/2|other.zip|4096",
            categories,
        )

        assertNull(loaded)
    }

    @Test
    fun `is not reused when the selected categories differ`() {
        AppleHealthImportCheckpointStore.save(context, checkpoint)

        assertNull(
            AppleHealthImportCheckpointStore.load(
                context,
                sourceKey,
                setOf(AppleHealthImportCategory.HEART),
            ),
        )
        assertNull(
            AppleHealthImportCheckpointStore.load(
                context,
                sourceKey,
                categories + AppleHealthImportCategory.SLEEP,
            ),
        )
    }

    @Test
    fun `load returns null when nothing was ever written`() {
        assertNull(AppleHealthImportCheckpointStore.load(context, sourceKey, categories))
    }

    @Test
    fun `clear removes the checkpoint`() {
        AppleHealthImportCheckpointStore.save(context, checkpoint)
        AppleHealthImportCheckpointStore.clear(context)

        assertNull(AppleHealthImportCheckpointStore.load(context, sourceKey, categories))
        assertFalse(File(filesDir, "apple_health_import/checkpoint.properties").exists())
    }

    @Test
    fun `source key is uri displayName and size joined with pipes`() {
        val uri = mockk<Uri>()
        every { uri.toString() } returns "content://downloads/1"

        val key = AppleHealthImportCheckpointStore.sourceKey(
            uri,
            AppleHealthExportFingerprint(displayName = "export.zip", size = 42L),
        )

        assertEquals("content://downloads/1|export.zip|42", key)
    }
}
