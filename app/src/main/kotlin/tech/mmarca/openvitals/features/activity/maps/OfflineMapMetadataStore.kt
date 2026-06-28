package tech.mmarca.openvitals.features.activity.maps

import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

internal class OfflineMapMetadataStore(
    private val metadataFile: File,
    private val mapsDirectory: File,
) {
    fun read(): OfflineMapLibraryState {
        val root = runCatching {
            Json.parseToJsonElement(metadataFile.readText()).jsonObject
        }.getOrNull() ?: return OfflineMapLibraryState()

        val packs = root[PacksKey]
            ?.let { element -> runCatching { element.jsonArray }.getOrNull() }
            ?.mapNotNull { element -> runCatching { element.jsonObject.toMapPack() }.getOrNull() }
            ?.filter { pack -> pack.file.exists() }
            .orEmpty()
            .sortedByDescending { it.importedAtMillis }

        val activeFormat = root.string(ActiveFormatKey)
            ?.let { value -> runCatching { OfflineMapPackFormat.valueOf(value) }.getOrNull() }
            ?.takeIf { format -> packs.any { it.format == format } }
            ?: root.string(ActiveMapIdKey)
                ?.let { activeId -> packs.firstOrNull { it.id == activeId }?.format }

        return OfflineMapLibraryState(
            mapPacks = packs,
            activeFormat = activeFormat,
        )
    }

    fun write(state: OfflineMapLibraryState) {
        mapsDirectory.mkdirs()
        val normalizedActiveFormat = state.activeFormat
            ?.takeIf { format -> state.mapPacks.any { it.format == format } }
        val json = buildJsonObject {
            if (normalizedActiveFormat != null) {
                put(ActiveFormatKey, normalizedActiveFormat.name)
            }
            put(PacksKey, state.mapPacks.toJsonArray())
        }.toString()

        val tempFile = File(metadataFile.parentFile ?: mapsDirectory, "${metadataFile.name}.tmp")
        tempFile.writeText(json)
        if (!tempFile.renameTo(metadataFile)) {
            tempFile.copyTo(metadataFile, overwrite = true)
            tempFile.delete()
        }
    }

    private fun JsonObject.toMapPack(): OfflineMapPack? {
        val id = string(IdKey)?.takeIf { it.isNotBlank() } ?: return null
        val displayName = string(DisplayNameKey)?.takeIf { it.isNotBlank() } ?: return null
        val savedOriginalFileName = string(OriginalFileNameKey)?.takeIf { it.isNotBlank() }
        val format = string(FormatKey)
            ?.let { value -> runCatching { OfflineMapPackFormat.valueOf(value) }.getOrNull() }
            ?: savedOriginalFileName?.let(OfflineMapPackFormat::fromFileName)
            ?: OfflineMapPackFormat.PMTILES
        val originalFileName = savedOriginalFileName ?: "$displayName${format.fileExtension}"
        val sizeBytes = long(SizeBytesKey)?.takeIf { it >= 0L } ?: 0L
        val importedAtMillis = long(ImportedAtMillisKey)?.takeIf { it > 0L } ?: 0L
        return OfflineMapPack(
            id = id,
            displayName = displayName,
            originalFileName = originalFileName,
            sizeBytes = sizeBytes,
            importedAtMillis = importedAtMillis,
            path = File(mapsDirectory, "$id${format.fileExtension}").absolutePath,
            format = format,
        )
    }

    private fun List<OfflineMapPack>.toJsonArray(): JsonArray =
        buildJsonArray {
            forEach { pack ->
                add(
                    buildJsonObject {
                        put(IdKey, pack.id)
                        put(DisplayNameKey, pack.displayName)
                        put(OriginalFileNameKey, pack.originalFileName)
                        put(FormatKey, pack.format.name)
                        put(SizeBytesKey, pack.sizeBytes)
                        put(ImportedAtMillisKey, pack.importedAtMillis)
                    },
                )
            }
        }

    private fun JsonObject.string(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNull

    private fun JsonObject.long(key: String): Long? =
        this[key]?.jsonPrimitive?.longOrNull

    private companion object {
        const val ActiveFormatKey = "activeFormat"
        const val ActiveMapIdKey = "activeMapId"
        const val PacksKey = "packs"
        const val IdKey = "id"
        const val DisplayNameKey = "displayName"
        const val OriginalFileNameKey = "originalFileName"
        const val FormatKey = "format"
        const val SizeBytesKey = "sizeBytes"
        const val ImportedAtMillisKey = "importedAtMillis"
    }
}
