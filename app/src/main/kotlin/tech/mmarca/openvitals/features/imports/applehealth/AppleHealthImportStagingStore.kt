package tech.mmarca.openvitals.features.imports.applehealth

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.Properties

internal data class AppleHealthStagedExport(
    val file: File,
    val bytes: Long,
    val reused: Boolean,
)

internal object AppleHealthImportStagingStore {
    fun stage(
        context: Context,
        sourceUri: Uri,
        fingerprint: AppleHealthExportFingerprint,
    ): AppleHealthStagedExport {
        val file = stagedExportFile(context)
        val metadata = metadataFile(context)
        if (file.exists() && metadata.matches(sourceUri, fingerprint, file.length())) {
            return AppleHealthStagedExport(
                file = file,
                bytes = file.length(),
                reused = true,
            )
        }

        file.parentFile?.mkdirs()
        val tempFile = File(file.parentFile, "${file.name}.tmp")
        tempFile.delete()
        val bytesCopied = context.contentResolver.openInputStream(sourceUri)
            ?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            ?: throw IllegalArgumentException("Unable to open Apple Health export.")
        if (!tempFile.renameTo(file)) {
            tempFile.copyTo(file, overwrite = true)
            tempFile.delete()
        }
        metadata.write(sourceUri, fingerprint, bytesCopied)
        return AppleHealthStagedExport(
            file = file,
            bytes = bytesCopied,
            reused = false,
        )
    }

    fun clear(context: Context) {
        stagedExportFile(context).delete()
        metadataFile(context).delete()
    }

    private fun File.matches(
        sourceUri: Uri,
        fingerprint: AppleHealthExportFingerprint,
        fileBytes: Long,
    ): Boolean {
        if (!exists() || fileBytes <= 0L) return false
        val properties = runCatching {
            Properties().apply {
                inputStream().use(::load)
            }
        }.getOrNull() ?: return false
        return properties.getProperty(KeySourceUri) == sourceUri.toString() &&
            properties.getProperty(KeyDisplayName).orEmpty().ifBlank { null } == fingerprint.displayName &&
            properties.getProperty(KeySize).orEmpty().ifBlank { null } == fingerprint.size?.toString() &&
            properties.getProperty(KeyBytesCopied)?.toLongOrNull() == fileBytes
    }

    private fun File.write(
        sourceUri: Uri,
        fingerprint: AppleHealthExportFingerprint,
        bytesCopied: Long,
    ) {
        val properties = Properties().apply {
            setProperty(KeySourceUri, sourceUri.toString())
            setProperty(KeyDisplayName, fingerprint.displayName.orEmpty())
            setProperty(KeySize, fingerprint.size?.toString().orEmpty())
            setProperty(KeyBytesCopied, bytesCopied.toString())
        }
        outputStream().use { output ->
            properties.store(output, "OpenVitals Apple Health staged export")
        }
    }

    private fun stagedExportFile(context: Context): File =
        File(importDirectory(context), StagedExportFileName)

    private fun metadataFile(context: Context): File =
        File(importDirectory(context), MetadataFileName)

    private fun importDirectory(context: Context): File =
        File(context.filesDir, ImportDirectoryName)

    private const val ImportDirectoryName = "apple_health_import"
    private const val StagedExportFileName = "staged_export.bin"
    private const val MetadataFileName = "staged_export.properties"
    private const val KeySourceUri = "sourceUri"
    private const val KeyDisplayName = "displayName"
    private const val KeySize = "size"
    private const val KeyBytesCopied = "bytesCopied"
}
