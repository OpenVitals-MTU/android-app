package tech.mmarca.openvitals.features.activity.maps

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class OfflineMapMetadataStoreTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `write and read preserves imported maps and active format`() {
        val mapsDirectory = temporaryFolder.newFolder("offline_maps")
        val metadataFile = File(mapsDirectory, "metadata.json")
        val store = OfflineMapMetadataStore(metadataFile, mapsDirectory)
        val first = mapPack(mapsDirectory, id = "city-a", importedAtMillis = 1_000L)
        val second = mapPack(mapsDirectory, id = "city-b", importedAtMillis = 2_000L)

        store.write(
            OfflineMapLibraryState(
                mapPacks = listOf(first, second),
                activeFormat = OfflineMapPackFormat.PMTILES,
            ),
        )

        val read = store.read()

        assertEquals(listOf(second.id, first.id), read.mapPacks.map { it.id })
        assertEquals(OfflineMapPackFormat.PMTILES, read.activeFormat)
    }

    @Test
    fun `read drops missing map files and clears active format`() {
        val mapsDirectory = temporaryFolder.newFolder("offline_maps")
        val metadataFile = File(mapsDirectory, "metadata.json")
        val store = OfflineMapMetadataStore(metadataFile, mapsDirectory)
        val pack = mapPack(mapsDirectory, id = "missing-city", importedAtMillis = 1_000L)
        store.write(OfflineMapLibraryState(mapPacks = listOf(pack), activeFormat = pack.format))

        pack.file.delete()

        val read = store.read()

        assertEquals(emptyList<OfflineMapPack>(), read.mapPacks)
        assertNull(read.activeFormat)
    }

    @Test
    fun `write and read preserves mapsforge format and map extension`() {
        val mapsDirectory = temporaryFolder.newFolder("offline_maps")
        val metadataFile = File(mapsDirectory, "metadata.json")
        val store = OfflineMapMetadataStore(metadataFile, mapsDirectory)
        val pack = mapPack(
            mapsDirectory = mapsDirectory,
            id = "estonia",
            importedAtMillis = 1_000L,
            format = OfflineMapPackFormat.MAPSFORGE,
        )
        store.write(OfflineMapLibraryState(mapPacks = listOf(pack), activeFormat = pack.format))

        val read = store.read()

        assertEquals(OfflineMapPackFormat.MAPSFORGE, read.mapPacks.single().format)
        assertEquals(File(mapsDirectory, "estonia.map").absolutePath, read.mapPacks.single().path)
        assertEquals(OfflineMapPackFormat.MAPSFORGE, read.activeFormat)
    }

    @Test
    fun `read migrates old active map id to active format`() {
        val mapsDirectory = temporaryFolder.newFolder("offline_maps")
        val metadataFile = File(mapsDirectory, "metadata.json")
        mapPack(
            mapsDirectory = mapsDirectory,
            id = "estonia",
            importedAtMillis = 1_000L,
            format = OfflineMapPackFormat.MAPSFORGE,
        )
        metadataFile.writeText(
            """
            {
              "activeMapId": "estonia",
              "packs": [
                {
                  "id": "estonia",
                  "displayName": "estonia",
                  "originalFileName": "estonia.map",
                  "format": "MAPSFORGE",
                  "sizeBytes": 3,
                  "importedAtMillis": 1000
                }
              ]
            }
            """.trimIndent(),
        )
        val store = OfflineMapMetadataStore(metadataFile, mapsDirectory)

        val read = store.read()

        assertEquals(OfflineMapPackFormat.MAPSFORGE, read.activeFormat)
    }

    private fun mapPack(
        mapsDirectory: File,
        id: String,
        importedAtMillis: Long,
        format: OfflineMapPackFormat = OfflineMapPackFormat.PMTILES,
    ): OfflineMapPack {
        val file = File(mapsDirectory, "$id${format.fileExtension}")
        file.writeBytes(byteArrayOf(1, 2, 3))
        return OfflineMapPack(
            id = id,
            displayName = id,
            originalFileName = "$id${format.fileExtension}",
            sizeBytes = file.length(),
            importedAtMillis = importedAtMillis,
            path = file.absolutePath,
            format = format,
        )
    }
}
