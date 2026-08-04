package tech.mmarca.openvitals.features.imports.applehealth

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.IOException
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
        val tempFile = stagedExportTempFile(context)
        tempFile.delete()
        val bytesCopied = try {
            context.contentResolver.openInputStream(sourceUri)
                ?.use { input ->
                    tempFile.outputStream().use { output ->
                        val copied = input.copyTo(output)
                        output.fd.sync()
                        copied
                    }
                }
                ?: throw IllegalArgumentException("Unable to open Apple Health export.")
        } catch (error: Exception) {
            tempFile.delete()
            throw error
        }
        fingerprint.expectedBytes?.let { expectedBytes ->
            if (bytesCopied != expectedBytes) {
                tempFile.delete()
                throw AppleHealthExportCopyException(expectedBytes, bytesCopied)
            }
        }
        if (!tempFile.renameTo(file)) {
            tempFile.inputStream().use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                    output.fd.sync()
                }
            }
            tempFile.delete()
        }
        metadata.write(sourceUri, fingerprint, bytesCopied)
        return AppleHealthStagedExport(
            file = file,
            bytes = bytesCopied,
            reused = false,
        )
    }

    fun clear(context: Context): Boolean {
        val files = listOf(
            stagedExportFile(context),
            stagedExportTempFile(context),
            metadataFile(context),
        )
        files.forEach { file ->
            if (file.exists()) file.delete()
        }
        importDirectory(context).delete()
        return files.none(File::exists)
    }

    private fun File.matches(
        sourceUri: Uri,
        fingerprint: AppleHealthExportFingerprint,
        fileBytes: Long,
    ): Boolean {
        if (!exists() || fileBytes <= 0L) return false
        if (fingerprint.expectedBytes?.let { expected -> expected != fileBytes } == true) return false
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

    private fun stagedExportTempFile(context: Context): File =
        File(importDirectory(context), "$StagedExportFileName.tmp")

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

internal class AppleHealthExportCopyException(
    expectedBytes: Long,
    copiedBytes: Long,
) : IOException(
    "Apple Health export copy was incomplete: Android reported $expectedBytes byte(s), but only " +
        "$copiedBytes byte(s) were copied into app storage. Download the ZIP fully to local storage and select it again.",
)

private val AppleHealthExportFingerprint.expectedBytes: Long?
    get() = size?.takeIf { it > 0L }
