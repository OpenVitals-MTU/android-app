package tech.mmarca.openvitals.domain.medical.cda

import java.io.BufferedInputStream
import java.io.InputStream
import java.util.zip.ZipInputStream
import tech.mmarca.openvitals.domain.medical.FhirEntry
import tech.mmarca.openvitals.domain.medical.FhirFile
import tech.mmarca.openvitals.domain.medical.FhirFileParser
import tech.mmarca.openvitals.domain.medical.MedicalImportSource
import tech.mmarca.openvitals.domain.medical.MedicalImportSourceResult
import tech.mmarca.openvitals.domain.medical.MedicalSourceDocument
import tech.mmarca.openvitals.domain.medical.MedicalSourceSkip
import tech.mmarca.openvitals.domain.medical.MedicalSourceSkipReason
import tech.mmarca.openvitals.domain.medical.fhirId
import tech.mmarca.openvitals.domain.medical.readUpTo
import tech.mmarca.openvitals.domain.medical.sha256Hex

/**
 * CDA documents, alone or in a zip.
 *
 * - One XML file is one document, as a patient portal's "download my record" gives. The file
 *   can be kept as the original.
 * - A zip holds many, as the Estonian health portal (terviseportaal.ee) hands out: one XML per
 *   document, and a PDF of the same name that renders it. Each PDF can be kept as the original
 *   of its own document's records. Its text is not read: the XML holds the same document.
 * - An Estonian document goes to the Estonian mapper, any other to the standard one.
 * - A referral gives only its diagnoses, as provisional. Declarations and authorisations make
 *   no record, and the review says so; their PDFs are kept too, with no record linked.
 * - The versions of one document are read oldest first, so the newest wins.
 */
internal object CdaImportSource : MedicalImportSource {

    override fun read(open: () -> InputStream): MedicalImportSourceResult {
        val files = readFiles(open) ?: return MedicalImportSourceResult.TooLarge
        val documents = mutableListOf<Pair<String, CdaMapResult.Mapped>>()
        val notRecords = mutableListOf<Pair<String, CdaMapResult.Skipped>>()
        val skipped = mutableListOf<MedicalSourceSkip>()
        var sawCda = false
        for ((stem, xml) in files.xmls) {
            val root = parseCda(xml.bytes)
            if (root == null) {
                // A lone file that is not XML is not this source's; in a zip it is one bad document among others.
                if (files.zipped) skipped += MedicalSourceSkip(MedicalSourceSkipReason.UNREADABLE_DOCUMENT, xml.name)
                continue
            }
            if (!isClinicalDocument(root)) continue
            sawCda = true
            val contentHash = sha256Hex(xml.bytes.decodeToString())
            val result = if (EstonianCdaMapper.isEstonian(root)) EstonianCdaMapper.map(root, contentHash) else CcdaMapper.map(root, contentHash)
            when (result) {
                is CdaMapResult.Skipped -> {
                    skipped += MedicalSourceSkip(result.reason, listOf(result.title, xml.name).filter { it.isNotBlank() }.joinToString(": "))
                    if (result.reason == MedicalSourceSkipReason.NOT_A_RECORD) notRecords += stem to result
                }
                is CdaMapResult.Mapped -> documents += stem to result
            }
        }
        if (!sawCda) return MedicalImportSourceResult.NotSupported

        val ordered = documents.sortedWith(compareBy({ it.second.series }, { it.second.version }))
        // One record per clinic, type and id: the clinic's Patient and doctors repeat in every document.
        val latest = linkedMapOf<Triple<String?, String?, String?>, FhirEntry>()
        ordered.forEach { (_, document) ->
            document.entries.forEach { entry -> latest[Triple(entry.origin?.baseUri, entry.type, entry.resource.fhirId)] = entry }
        }
        if (latest.isEmpty()) {
            return if (skipped.isEmpty()) MedicalImportSourceResult.Empty else MedicalImportSourceResult.Entries(FhirFile(emptyList()), skipped)
        }
        val kept = when {
            // The picked XML file itself, for every record in it.
            !files.zipped -> files.xmls.values.map { MedicalSourceDocument(it.bytes, XmlMimeType, "xml") }
            files.pdfsFit -> ordered.mapNotNull { (stem, document) -> keptPdf(files.pdfs[stem], document) } +
                notRecords.mapNotNull { (stem, document) -> standalonePdf(files.pdfs[stem], document) }
            else -> emptyList()
        }
        return MedicalImportSourceResult.Entries(FhirFile(latest.values.toList()), skipped, kept)
    }

    /** The PDF of one document, linked to that document's own records. */
    private fun keptPdf(pdf: NamedFile?, document: CdaMapResult.Mapped): MedicalSourceDocument? {
        pdf ?: return null
        val links = document.entries
            .filter { it.type !in CdaSharedTypes }
            .mapNotNull { entry -> entry.type?.let { type -> entry.resource.fhirId?.let { type to it } } }
            .toSet()
        if (links.isEmpty()) return null
        return MedicalSourceDocument(pdf.bytes, PdfMimeType, "pdf", name = pdf.name, links = links)
    }

    /** The PDF of a declaration or an authorisation: no record comes from it, and the user may keep it anyway. */
    private fun standalonePdf(pdf: NamedFile?, document: CdaMapResult.Skipped): MedicalSourceDocument? {
        pdf ?: return null
        return MedicalSourceDocument(
            pdf.bytes, PdfMimeType, "pdf",
            name = pdf.name, links = emptySet(), keepWithoutRecords = true, sourceName = document.clinic,
        )
    }

    /** The picked file as one XML document, or the XML and PDF files of a zip. Null when the XML is too large to hold. */
    private fun readFiles(open: () -> InputStream): PickedFiles? = BufferedInputStream(open()).use { input ->
        input.mark(ZipSignatureBytes)
        val isZip = input.read() == 'P'.code && input.read() == 'K'.code
        input.reset()
        if (isZip) {
            readZip(input)
        } else {
            val bytes = input.readUpTo(FhirFileParser.MaxFileBytes) ?: return null
            PickedFiles(mapOf("" to NamedFile("", bytes)), emptyMap(), pdfsFit = true, zipped = false)
        }
    }

    /** The XML and PDF files by file stem, whatever the folders. */
    private fun readZip(input: InputStream): PickedFiles? {
        val xmls = linkedMapOf<String, NamedFile>()
        val pdfs = mutableMapOf<String, NamedFile>()
        var xmlBudget = FhirFileParser.MaxFileBytes
        var pdfBytes = 0L
        var pdfsFit = true
        ZipInputStream(input).let { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val name = entry.name.substringAfterLast('/')
                val stem = name.substringBeforeLast('.')
                when {
                    entry.isDirectory || name.startsWith(".") -> Unit
                    name.endsWith(".xml", ignoreCase = true) -> {
                        val content = zip.readUpTo(xmlBudget) ?: return null
                        xmlBudget -= content.size
                        xmls[stem] = NamedFile(name, content)
                    }
                    name.endsWith(".pdf", ignoreCase = true) && pdfsFit -> {
                        val content = zip.readUpTo((MaxPdfBytes - pdfBytes).toInt())
                        if (content == null) {
                            // Too many to keep in memory: none are offered, and the records still import.
                            pdfsFit = false
                            pdfs.clear()
                        } else {
                            pdfBytes += content.size
                            pdfs[stem] = NamedFile(name, content)
                        }
                    }
                }
                zip.closeEntry()
            }
        }
        return PickedFiles(xmls, pdfs, pdfsFit, zipped = true)
    }

    private class NamedFile(val name: String, val bytes: ByteArray)

    private class PickedFiles(val xmls: Map<String, NamedFile>, val pdfs: Map<String, NamedFile>, val pdfsFit: Boolean, val zipped: Boolean)

    private const val PdfMimeType = "application/pdf"
    private const val XmlMimeType = "application/xml"

    /** PDFs above this total are not offered for keeping. */
    private const val MaxPdfBytes = 64L * 1024 * 1024

    private const val ZipSignatureBytes = 2
}
