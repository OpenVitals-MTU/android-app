package tech.mmarca.openvitals.data.local.medical

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface MedicalDocumentDao {
    @Query("SELECT * FROM medical_documents WHERE sha256 = :sha256 LIMIT 1")
    suspend fun byHash(sha256: String): MedicalDocumentEntity?

    @Query("SELECT * FROM medical_documents WHERE id = :id LIMIT 1")
    suspend fun byId(id: String): MedicalDocumentEntity?

    @Insert
    suspend fun insert(document: MedicalDocumentEntity)

    /** A link that already exists stays as it is. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLinks(links: List<MedicalDocumentRecordEntity>)

    @Query(
        """
        SELECT d.*, COUNT(r.resource_id) AS record_count FROM medical_documents d
        LEFT JOIN medical_document_records r ON r.document_id = d.id
        GROUP BY d.id ORDER BY d.imported_at_millis DESC
        """,
    )
    suspend fun documentsWithCounts(): List<MedicalDocumentWithCount>

    @Query("SELECT COALESCE(SUM(size_bytes), 0) FROM medical_documents")
    suspend fun totalBytes(): Long

    @Query(
        """
        SELECT d.* FROM medical_documents d JOIN medical_document_records r ON r.document_id = d.id
        WHERE r.data_source_id = :dataSourceId AND r.resource_type = :resourceType AND r.resource_id = :resourceId
        ORDER BY d.imported_at_millis DESC LIMIT 1
        """,
    )
    suspend fun documentFor(dataSourceId: String, resourceType: String, resourceId: String): MedicalDocumentEntity?

    @Query("SELECT COUNT(*) FROM medical_document_records WHERE document_id = :documentId")
    suspend fun recordCount(documentId: String): Int

    @Query("SELECT * FROM medical_document_records")
    suspend fun allLinks(): List<MedicalDocumentRecordEntity>

    @Delete
    suspend fun deleteLinks(links: List<MedicalDocumentRecordEntity>)

    @Query("DELETE FROM medical_document_records WHERE document_id = :documentId")
    suspend fun deleteLinksOf(documentId: String)

    @Query("DELETE FROM medical_documents WHERE id = :id")
    suspend fun deleteDocument(id: String)

    @Transaction
    suspend fun delete(id: String) {
        deleteLinksOf(id)
        deleteDocument(id)
    }

    @Query("DELETE FROM medical_document_records")
    suspend fun deleteAllLinks()

    @Query("DELETE FROM medical_documents")
    suspend fun deleteAllDocuments()

    @Transaction
    suspend fun deleteAll() {
        deleteAllLinks()
        deleteAllDocuments()
    }
}
