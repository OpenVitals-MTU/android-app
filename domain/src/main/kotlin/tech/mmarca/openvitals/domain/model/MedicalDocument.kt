package tech.mmarca.openvitals.domain.model

import java.time.Instant

/**
 * A file an import kept because the user asked. [sourceName] is set when its records went to
 * one source. [recordCount] counts the records still linked to it.
 */
data class MedicalDocument(
    val id: String,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val importedAt: Instant,
    val sourceName: String?,
    val recordCount: Int,
)
