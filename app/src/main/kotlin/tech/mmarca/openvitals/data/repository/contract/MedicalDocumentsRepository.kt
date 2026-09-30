package tech.mmarca.openvitals.data.repository.contract

import java.io.File
import tech.mmarca.openvitals.domain.model.MedicalDocument
import tech.mmarca.openvitals.domain.model.MedicalRecordRef

/**
 * Files kept from imports, in the app's private storage, with a Room index. A document and
 * its records are separate: deleting one never deletes the other.
 */
interface MedicalDocumentsRepository {
    /**
     * Keeps [bytes] and links the copy to [refs]. The same file kept twice, by content, keeps one
     * copy and gains the new links.
     */
    suspend fun keep(
        fileName: String,
        mimeType: String,
        extension: String,
        bytes: ByteArray,
        sourceName: String?,
        refs: List<MedicalRecordRef>,
    ): MedicalDocument

    /** Newest first. A row whose file has gone is dropped. */
    suspend fun documents(): List<MedicalDocument>

    /** The space all kept files take. */
    suspend fun totalBytes(): Long

    /** The kept file, to open, save or share. Null when it is gone. */
    suspend fun file(id: String): File?

    /** The newest document a record came from, or null. */
    suspend fun documentFor(ref: MedicalRecordRef): MedicalDocument?

    suspend fun linkedRefs(): List<MedicalRecordRef>

    /** Drops links to records that no longer exist. The documents stay. */
    suspend fun dropLinks(refs: Collection<MedicalRecordRef>)

    /** Deletes the file and its links. Its records stay in Health Connect. */
    suspend fun delete(id: String)

    suspend fun deleteAll()
}
