package tech.mmarca.openvitals.features.activity.maps

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import io.mockk.every
import io.mockk.mockk
import java.io.ByteArrayInputStream
import java.io.File
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import tech.mmarca.openvitals.core.performance.DispatcherProvider

/**
 * The import/delete half of the offline-map library.
 *
 * Ported from test/features/activity/maps/offline_map_import_controller_test.dart
 * (the cases that do not depend on Flutter's file-picker plumbing).
 */
class OfflineMapRepositoryTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `rejects a mapsforge pack that is not a valid map file`() = runTest {
        // Validation opens the file with `MapFile(...)`; garbage bytes must fail
        // the import and leave nothing behind.
        val filesDir = temporaryFolder.newFolder("files")
        val repository = repository(filesDir, "broken.map", ByteArray(4096) { 0xAB.toByte() })

        val failure = runCatching { repository.importMap(mockk<Uri>()) }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
        assertEquals(emptyList<OfflineMapPack>(), repository.state.value.mapPacks)
        // Failed imports must clean up their temp and final files.
        val mapsDirectory = File(filesDir, "offline_maps")
        val leftovers = mapsDirectory.listFiles()?.filterNot { it.name == "metadata.json" }.orEmpty()
        assertEquals(emptyList<File>(), leftovers)
    }

    @Test
    fun `deleteMap removes the file and its metadata entry`() = runTest {
        val filesDir = temporaryFolder.newFolder("files")
        val repository = repository(filesDir, "city.pmtiles", ByteArray(16) { 1 })

        val pack = repository.importMap(mockk<Uri>())
        assertTrue(File(pack.path).exists())

        repository.deleteMap(pack.id)

        assertFalse(File(pack.path).exists())
        assertEquals(emptyList<OfflineMapPack>(), repository.state.value.mapPacks)
    }

    private fun repository(
        filesDir: File,
        displayName: String,
        bytes: ByteArray,
    ): OfflineMapRepository {
        val resolver = mockk<ContentResolver>()
        every { resolver.openInputStream(any()) } answers { ByteArrayInputStream(bytes) }
        every { resolver.query(any(), any(), any(), any(), any()) } answers {
            val columns = arg<Array<String>>(1)
            openableColumnCursor(columns.single(), displayName, bytes.size.toLong())
        }
        val context = mockk<Context>()
        every { context.filesDir } returns filesDir
        every { context.contentResolver } returns resolver
        return OfflineMapRepository(context, UnconfinedDispatchers)
    }

    private fun openableColumnCursor(column: String, displayName: String, size: Long): Cursor =
        mockk<Cursor>().also { cursor ->
            every { cursor.moveToFirst() } returns true
            every { cursor.getColumnIndex(column) } returns 0
            every { cursor.getString(0) } returns displayName
            every { cursor.getLong(0) } returns size
            every { cursor.close() } returns Unit
            if (column == OpenableColumns.DISPLAY_NAME) {
                every { cursor.getLong(any()) } returns size
            }
        }

    private object UnconfinedDispatchers : DispatcherProvider {
        override val main: CoroutineContext = Dispatchers.Unconfined
        override val io: CoroutineContext = Dispatchers.Unconfined
        override val default: CoroutineContext = Dispatchers.Unconfined
    }
}
