package tech.mmarca.openvitals.features.activity.maps

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.mapsforge.map.reader.MapFile
import tech.mmarca.openvitals.core.performance.DispatcherProvider

@Singleton
class OfflineMapRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dispatchers: DispatcherProvider,
) {
    private val mapsDirectory = File(context.filesDir, MapsDirectoryName)
    private val metadataStore = OfflineMapMetadataStore(
        metadataFile = File(mapsDirectory, MetadataFileName),
        mapsDirectory = mapsDirectory,
    )
    private val lock = Any()
    private val _state = MutableStateFlow(metadataStore.read())
    val state: StateFlow<OfflineMapLibraryState> = _state.asStateFlow()

    fun refresh() {
        synchronized(lock) {
            _state.value = metadataStore.read()
        }
    }

    suspend fun importMap(
        uri: Uri,
        onProgress: suspend (bytesCopied: Long, totalBytes: Long) -> Unit = { _, _ -> },
    ): OfflineMapPack = withContext(dispatchers.io) {
        mapsDirectory.mkdirs()
        val originalFileName = queryDisplayName(uri)
            ?.takeIf { it.isNotBlank() }
            ?: "offline-map-${System.currentTimeMillis()}.pmtiles"
        val format = OfflineMapPackFormat.fromFileName(originalFileName)
        require(format != null) {
            "Only .pmtiles, .map, and .maps offline map packs are supported."
        }
        val originalExtension = format.extensionForFileName(originalFileName) ?: format.fileExtension

        val id = mapIdFor(originalFileName, originalExtension)
        val totalBytes = querySize(uri).coerceAtLeast(0L)
        val tempFile = File(mapsDirectory, "$id${format.fileExtension}.tmp")
        val finalFile = File(mapsDirectory, "$id${format.fileExtension}")
        var finalFileRecorded = false

        try {
            tempFile.delete()
            finalFile.delete()
            val copiedBytes = copyUriToFile(
                uri = uri,
                destination = tempFile,
                totalBytes = totalBytes,
                onProgress = onProgress,
            )
            require(copiedBytes > 0L) { "The selected offline map pack is empty." }
            validateImportedMap(tempFile, format)
            if (!tempFile.renameTo(finalFile)) {
                tempFile.copyTo(finalFile, overwrite = true)
                tempFile.delete()
            }

            val pack = OfflineMapPack(
                id = id,
                displayName = originalFileName.removeSuffixIgnoreCase(originalExtension),
                originalFileName = originalFileName,
                sizeBytes = finalFile.length().takeIf { it > 0L } ?: copiedBytes,
                importedAtMillis = System.currentTimeMillis(),
                path = finalFile.absolutePath,
                format = format,
            )
            synchronized(lock) {
                val current = metadataStore.read()
                val updated = current.copy(
                    mapPacks = (current.mapPacks.filterNot { it.id == pack.id } + pack)
                        .sortedByDescending { it.importedAtMillis },
                    activeFormat = current.activeFormat
                        ?.takeIf { activeFormat ->
                            current.mapPacks.any { it.format == activeFormat } || pack.format == activeFormat
                        }
                        ?: pack.format,
                )
                metadataStore.write(updated)
                _state.value = updated
                finalFileRecorded = true
            }
            pack
        } catch (error: Throwable) {
            tempFile.delete()
            if (!finalFileRecorded) {
                finalFile.delete()
            }
            throw error
        }
    }

    suspend fun deleteMap(id: String) = withContext(dispatchers.io) {
        synchronized(lock) {
            val current = metadataStore.read()
            val deletedPack = current.mapPacks.firstOrNull { it.id == id }
            val remaining = current.mapPacks.filterNot { it.id == id }
            deletedPack?.file?.delete()
            val activeFormat = current.activeFormat
                ?.takeIf { format -> remaining.any { it.format == format } }
                ?: remaining.firstOrNull()?.format
            val updated = OfflineMapLibraryState(
                mapPacks = remaining,
                activeFormat = activeFormat,
            )
            metadataStore.write(updated)
            _state.value = updated
        }
    }

    fun setActiveFormat(format: OfflineMapPackFormat?) {
        synchronized(lock) {
            val current = metadataStore.read()
            val activeFormat = format
                ?.takeIf { candidate -> current.mapPacks.any { it.format == candidate } }
            val updated = current.copy(activeFormat = activeFormat)
            metadataStore.write(updated)
            _state.value = updated
        }
    }

    fun activeMapPacks(): List<OfflineMapPack> =
        state.value.activeMapPacks

    fun mapPacks(): List<OfflineMapPack> =
        state.value.mapPacks

    private suspend fun copyUriToFile(
        uri: Uri,
        destination: File,
        totalBytes: Long,
        onProgress: suspend (bytesCopied: Long, totalBytes: Long) -> Unit,
    ): Long {
        val resolver = context.contentResolver
        var bytesCopied = 0L
        resolver.openInputStream(uri)?.use { input ->
            destination.outputStream().use { output ->
                val buffer = ByteArray(CopyBufferSize)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    output.write(buffer, 0, read)
                    bytesCopied += read
                    onProgress(bytesCopied, totalBytes)
                }
            }
        } ?: error("Unable to open selected offline map pack.")
        return bytesCopied
    }

    private fun queryDisplayName(uri: Uri): String? =
        queryOpenableColumn(uri, OpenableColumns.DISPLAY_NAME) { cursor, index ->
            cursor.getString(index)
        }

    private fun querySize(uri: Uri): Long =
        queryOpenableColumn(uri, OpenableColumns.SIZE) { cursor, index ->
            cursor.getLong(index)
        } ?: 0L

    private fun <T> queryOpenableColumn(
        uri: Uri,
        column: String,
        read: (android.database.Cursor, Int) -> T,
    ): T? =
        context.contentResolver.query(uri, arrayOf(column), null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val index = cursor.getColumnIndex(column).takeIf { it >= 0 } ?: return@use null
            read(cursor, index)
        }

    private fun validateImportedMap(file: File, format: OfflineMapPackFormat) {
        when (format) {
            OfflineMapPackFormat.PMTILES -> Unit
            OfflineMapPackFormat.MAPSFORGE -> {
                runCatching { MapFile(file).close() }
                    .getOrElse { error ->
                        throw IllegalArgumentException(
                            "The selected Mapsforge map pack is invalid or unsupported.",
                            error,
                        )
                    }
            }
        }
    }

    private fun mapIdFor(originalFileName: String, fileExtension: String): String {
        val base = originalFileName
            .removeSuffixIgnoreCase(fileExtension)
            .lowercase(Locale.US)
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .ifBlank { "offline-map" }
        return "$base-${UUID.randomUUID().toString().take(8)}"
    }

    private fun String.removeSuffixIgnoreCase(suffix: String): String =
        if (endsWith(suffix, ignoreCase = true)) {
            dropLast(suffix.length)
        } else {
            this
        }

    private companion object {
        const val MapsDirectoryName = "offline_maps"
        const val MetadataFileName = "metadata.json"
        const val CopyBufferSize = 128 * 1024
    }
}
