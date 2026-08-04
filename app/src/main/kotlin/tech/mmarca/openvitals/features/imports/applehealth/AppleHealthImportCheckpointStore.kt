package tech.mmarca.openvitals.features.imports.applehealth

import android.content.Context
import java.io.File
import java.util.Properties

data class AppleHealthImportCheckpoint(
    val sourceKey: String,
    val selectedCategories: Set<AppleHealthImportCategory>,
    val committedSelectedRecords: Int,
    val importedRecords: Int,
    val duplicateSkippedRecords: Int,
    val failedRecords: Int,
    val typeStats: Map<String, AppleHealthImportCheckpointTypeStats>,
)

data class AppleHealthImportCheckpointTypeStats(
    val imported: Int,
    val duplicateSkipped: Int,
    val failed: Int,
)

internal object AppleHealthImportCheckpointStore {
    fun load(
        context: Context,
        sourceKey: String,
        selectedCategories: Set<AppleHealthImportCategory>,
    ): AppleHealthImportCheckpoint? {
        val file = checkpointFile(context)
        if (!file.exists()) return null
        val properties = runCatching {
            Properties().apply {
                file.inputStream().use(::load)
            }
        }.getOrNull() ?: return null
        if (properties.getProperty(KeySourceKey) != sourceKey) return null
        val savedCategories = properties.getProperty(KeySelectedCategories)
            ?.split(',')
            ?.filter { it.isNotBlank() }
            ?.mapNotNull { value -> runCatching { AppleHealthImportCategory.valueOf(value) }.getOrNull() }
            ?.toSet()
            ?: emptySet()
        if (savedCategories != selectedCategories) return null
        val committed = properties.getProperty(KeyCommittedSelectedRecords)?.toIntOrNull() ?: return null
        return AppleHealthImportCheckpoint(
            sourceKey = sourceKey,
            selectedCategories = selectedCategories,
            committedSelectedRecords = committed.coerceAtLeast(0),
            importedRecords = properties.getProperty(KeyImportedRecords)?.toIntOrNull()?.coerceAtLeast(0) ?: 0,
            duplicateSkippedRecords = properties.getProperty(KeyDuplicateSkippedRecords)?.toIntOrNull()?.coerceAtLeast(0) ?: 0,
            failedRecords = properties.getProperty(KeyFailedRecords)?.toIntOrNull()?.coerceAtLeast(0) ?: 0,
            typeStats = properties.getProperty(KeyTypeStats).orEmpty().decodeTypeStats(),
        )
    }

    fun save(
        context: Context,
        checkpoint: AppleHealthImportCheckpoint,
    ) {
        val file = checkpointFile(context)
        file.parentFile?.mkdirs()
        val tempFile = File(file.parentFile, "${file.name}.tmp")
        val properties = Properties().apply {
            setProperty(KeySourceKey, checkpoint.sourceKey)
            setProperty(KeySelectedCategories, checkpoint.selectedCategories.toCategoryProperty())
            setProperty(KeyCommittedSelectedRecords, checkpoint.committedSelectedRecords.toString())
            setProperty(KeyImportedRecords, checkpoint.importedRecords.toString())
            setProperty(KeyDuplicateSkippedRecords, checkpoint.duplicateSkippedRecords.toString())
            setProperty(KeyFailedRecords, checkpoint.failedRecords.toString())
            setProperty(KeyTypeStats, checkpoint.typeStats.encodeTypeStats())
        }
        tempFile.outputStream().use { output ->
            properties.store(output, "OpenVitals Apple Health import checkpoint")
        }
        if (!tempFile.renameTo(file)) {
            tempFile.copyTo(file, overwrite = true)
            tempFile.delete()
        }
    }

    fun clear(context: Context) {
        checkpointFile(context).delete()
    }

    fun sourceKey(
        uri: android.net.Uri,
        fingerprint: AppleHealthExportFingerprint,
    ): String =
        listOf(
            uri.toString(),
            fingerprint.displayName.orEmpty(),
            fingerprint.size?.toString().orEmpty(),
        ).joinToString(separator = "|")

    private fun checkpointFile(context: Context): File =
        File(importDirectory(context), CheckpointFileName)

    private fun importDirectory(context: Context): File =
        File(context.filesDir, ImportDirectoryName)

    private fun Set<AppleHealthImportCategory>.toCategoryProperty(): String =
        map { it.name }.sorted().joinToString(",")

    private fun Map<String, AppleHealthImportCheckpointTypeStats>.encodeTypeStats(): String =
        entries.joinToString("\n") { (appleType, stats) ->
            listOf(
                appleType.escapeField(),
                stats.imported.toString(),
                stats.duplicateSkipped.toString(),
                stats.failed.toString(),
            ).joinToString("\t")
        }

    private fun String.decodeTypeStats(): Map<String, AppleHealthImportCheckpointTypeStats> =
        lineSequence()
            .mapNotNull { line ->
                val parts = line.split('\t')
                if (parts.size != 4) return@mapNotNull null
                val imported = parts[1].toIntOrNull() ?: return@mapNotNull null
                val duplicates = parts[2].toIntOrNull() ?: return@mapNotNull null
                val failed = parts[3].toIntOrNull() ?: return@mapNotNull null
                parts[0].unescapeField() to AppleHealthImportCheckpointTypeStats(
                    imported = imported.coerceAtLeast(0),
                    duplicateSkipped = duplicates.coerceAtLeast(0),
                    failed = failed.coerceAtLeast(0),
                )
            }
            .toMap()

    private fun String.escapeField(): String =
        replace("\\", "\\\\")
            .replace("\t", "\\t")
            .replace("\n", "\\n")

    private fun String.unescapeField(): String {
        val result = StringBuilder(length)
        var index = 0
        while (index < length) {
            val c = this[index]
            if (c == '\\' && index + 1 < length) {
                when (val next = this[index + 1]) {
                    '\\' -> result.append('\\')
                    't' -> result.append('\t')
                    'n' -> result.append('\n')
                    else -> {
                        result.append(c)
                        result.append(next)
                    }
                }
                index += 2
            } else {
                result.append(c)
                index++
            }
        }
        return result.toString()
    }

    private const val ImportDirectoryName = "apple_health_import"
    private const val CheckpointFileName = "checkpoint.properties"
    private const val KeySourceKey = "sourceKey"
    private const val KeySelectedCategories = "selectedCategories"
    private const val KeyCommittedSelectedRecords = "committedSelectedRecords"
    private const val KeyImportedRecords = "importedRecords"
    private const val KeyDuplicateSkippedRecords = "duplicateSkippedRecords"
    private const val KeyFailedRecords = "failedRecords"
    private const val KeyTypeStats = "typeStats"
}
