package tech.mmarca.openvitals.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext
import tech.mmarca.openvitals.core.performance.DefaultDispatcherProvider
import tech.mmarca.openvitals.core.performance.DispatcherProvider
import tech.mmarca.openvitals.data.local.medical.MedicalDocumentDao
import tech.mmarca.openvitals.data.local.medical.MedicalDocumentEntity
import tech.mmarca.openvitals.data.local.medical.MedicalDocumentRecordEntity
import tech.mmarca.openvitals.data.repository.contract.MedicalDocumentsRepository
import tech.mmarca.openvitals.domain.model.MedicalDocument
import tech.mmarca.openvitals.domain.model.MedicalRecordRef

/** Kept files live under the app's private files, never the cache, which export staging prunes. */
internal const val MedicalDocumentsDirectory = "medical_documents"

@Singleton
class MedicalDocumentsRepositoryImpl(
    private val directory: File,
    private val dao: MedicalDocumentDao,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider,
    private val now: () -> Instant = Instant::now,
) : MedicalDocumentsRepository {

    @Inject
    constructor(@ApplicationContext context: Context, dao: MedicalDocumentDao) :
        this(File(context.filesDir, MedicalDocumentsDirectory), dao)

    override suspend fun keep(
        fileName: String,
        mimeType: String,
        extension: String,
        bytes: ByteArray,
        sourceName: String?,
        refs: List<MedicalRecordRef>,
    ): MedicalDocument = withContext(dispatchers.io) {
        val hash = sha256(bytes)
        val document = dao.byHash(hash) ?: run {
            val id = UUID.randomUUID().toString()
            val entity = MedicalDocumentEntity(
                id = id,
                fileName = fileName,
                mimeType = mimeType,
                sizeBytes = bytes.size.toLong(),
                sha256 = hash,
                importedAtMillis = now().toEpochMilli(),
                sourceName = sourceName,
                storedName = "$id.${extension.filter { it.isLetterOrDigit() }.ifEmpty { "bin" }}",
            )
            val file = File(directory.apply { mkdirs() }, entity.storedName)
            file.writeBytes(bytes)
            // A row without its file would list a document that cannot open.
            runCatching { dao.insert(entity) }.onFailure { file.delete() }.getOrThrow()
            entity
        }
        dao.insertLinks(refs.map { MedicalDocumentRecordEntity(document.id, it.dataSourceId, it.resourceType, it.resourceId) })
        document.toDomain(dao.recordCount(document.id))
    }

    override suspend fun documents(): List<MedicalDocument> = withContext(dispatchers.io) {
        dao.documentsWithCounts().mapNotNull { row ->
            if (File(directory, row.document.storedName).exists()) {
                row.document.toDomain(row.recordCount)
            } else {
                dao.delete(row.document.id)
                null
            }
        }
    }

    override suspend fun totalBytes(): Long = withContext(dispatchers.io) { dao.totalBytes() }

    override suspend fun file(id: String): File? = withContext(dispatchers.io) {
        dao.byId(id)?.let { File(directory, it.storedName) }?.takeIf { it.exists() }
    }

    override suspend fun documentFor(ref: MedicalRecordRef): MedicalDocument? = withContext(dispatchers.io) {
        dao.documentFor(ref.dataSourceId, ref.resourceType, ref.resourceId)?.let { it.toDomain(dao.recordCount(it.id)) }
    }

    override suspend fun linkedRefs(): List<MedicalRecordRef> = withContext(dispatchers.io) {
        dao.allLinks().map { MedicalRecordRef(it.dataSourceId, it.resourceType, it.resourceId) }.distinct()
    }

    override suspend fun dropLinks(refs: Collection<MedicalRecordRef>) = withContext(dispatchers.io) {
        if (refs.isEmpty()) return@withContext
        val gone = refs.toSet()
        dao.deleteLinks(dao.allLinks().filter { MedicalRecordRef(it.dataSourceId, it.resourceType, it.resourceId) in gone })
    }

    override suspend fun delete(id: String) = withContext(dispatchers.io) {
        dao.byId(id)?.let { File(directory, it.storedName).delete() }
        dao.delete(id)
    }

    override suspend fun deleteAll() = withContext(dispatchers.io) {
        directory.listFiles()?.forEach { it.delete() }
        dao.deleteAll()
    }

    private fun MedicalDocumentEntity.toDomain(recordCount: Int) = MedicalDocument(
        id = id,
        fileName = fileName,
        mimeType = mimeType,
        sizeBytes = sizeBytes,
        importedAt = Instant.ofEpochMilli(importedAtMillis),
        sourceName = sourceName,
        recordCount = recordCount,
    )

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}
