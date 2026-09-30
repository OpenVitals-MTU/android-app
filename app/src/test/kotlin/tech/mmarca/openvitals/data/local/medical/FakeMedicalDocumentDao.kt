package tech.mmarca.openvitals.data.local.medical

/** The document index in memory, with the unique hash and the link key Room enforces. */
class FakeMedicalDocumentDao : MedicalDocumentDao {
    val documents = mutableListOf<MedicalDocumentEntity>()
    val links = linkedSetOf<MedicalDocumentRecordEntity>()

    override suspend fun byHash(sha256: String) = documents.firstOrNull { it.sha256 == sha256 }

    override suspend fun byId(id: String) = documents.firstOrNull { it.id == id }

    override suspend fun insert(document: MedicalDocumentEntity) {
        require(documents.none { it.id == document.id || it.sha256 == document.sha256 }) { "UNIQUE constraint failed" }
        documents += document
    }

    override suspend fun insertLinks(links: List<MedicalDocumentRecordEntity>) {
        this.links += links
    }

    override suspend fun documentsWithCounts() = documents.sortedByDescending { it.importedAtMillis }
        .map { document -> MedicalDocumentWithCount(document, links.count { it.documentId == document.id }) }

    override suspend fun totalBytes() = documents.sumOf { it.sizeBytes }

    override suspend fun documentFor(dataSourceId: String, resourceType: String, resourceId: String) =
        links.filter { it.dataSourceId == dataSourceId && it.resourceType == resourceType && it.resourceId == resourceId }
            .mapNotNull { link -> documents.firstOrNull { it.id == link.documentId } }
            .maxByOrNull { it.importedAtMillis }

    override suspend fun recordCount(documentId: String) = links.count { it.documentId == documentId }

    override suspend fun allLinks() = links.toList()

    override suspend fun deleteLinks(links: List<MedicalDocumentRecordEntity>) {
        this.links -= links.toSet()
    }

    override suspend fun deleteLinksOf(documentId: String) {
        links.removeAll { it.documentId == documentId }
    }

    override suspend fun deleteDocument(id: String) {
        documents.removeAll { it.id == id }
    }

    override suspend fun deleteAllLinks() = links.clear()

    override suspend fun deleteAllDocuments() = documents.clear()
}
