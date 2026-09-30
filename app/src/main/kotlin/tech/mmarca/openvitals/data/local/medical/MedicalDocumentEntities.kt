package tech.mmarca.openvitals.data.local.medical

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A file kept from an import. [storedName] is the file under `files/medical_documents`.
 * [sha256] keeps one copy of the same file. The records themselves stay in Health Connect.
 */
@Entity(tableName = "medical_documents", indices = [Index(value = ["sha256"], unique = true)])
data class MedicalDocumentEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "file_name") val fileName: String,
    @ColumnInfo(name = "mime_type") val mimeType: String,
    @ColumnInfo(name = "size_bytes") val sizeBytes: Long,
    val sha256: String,
    @ColumnInfo(name = "imported_at_millis") val importedAtMillis: Long,
    @ColumnInfo(name = "source_name") val sourceName: String?,
    @ColumnInfo(name = "stored_name") val storedName: String,
)

/** One Health Connect record that came from a kept document. */
@Entity(
    tableName = "medical_document_records",
    primaryKeys = ["document_id", "data_source_id", "resource_type", "resource_id"],
    indices = [Index(value = ["data_source_id", "resource_type", "resource_id"])],
)
data class MedicalDocumentRecordEntity(
    @ColumnInfo(name = "document_id") val documentId: String,
    @ColumnInfo(name = "data_source_id") val dataSourceId: String,
    @ColumnInfo(name = "resource_type") val resourceType: String,
    @ColumnInfo(name = "resource_id") val resourceId: String,
)

/** A document with how many records still link to it. */
data class MedicalDocumentWithCount(
    @Embedded val document: MedicalDocumentEntity,
    @ColumnInfo(name = "record_count") val recordCount: Int,
)
