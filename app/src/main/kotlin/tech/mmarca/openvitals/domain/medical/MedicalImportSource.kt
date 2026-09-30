package tech.mmarca.openvitals.domain.medical

import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * Turns a picked file into FHIR entries for the import wizard. Phase 1 reads FHIR files and
 * the clinical records in an Apple Health export. Each phase 2 parser (health cards, CDA,
 * PDF text, scans) is another source behind this seam; the wizard's analyze, confirm,
 * import and result steps stay the same. [read] may call [open] more than once.
 */
interface MedicalImportSource {
    fun read(open: () -> InputStream): MedicalImportSourceResult
}

sealed interface MedicalImportSourceResult {
    /**
     * [skipped] are records the source found but could not hand over, such as FHIR DSTU2.
     * [documents] are the files the user may choose to keep once the import has written records.
     */
    data class Entries(
        val file: FhirFile,
        val skipped: List<MedicalSourceSkip> = emptyList(),
        val documents: List<MedicalSourceDocument> = emptyList(),
    ) : MedicalImportSourceResult

    /** Not a format this source reads. */
    data object NotSupported : MedicalImportSourceResult

    /** Larger than the parser can hold. */
    data object TooLarge : MedicalImportSourceResult

    /** A format this source reads, with nothing in it to import. */
    data object Empty : MedicalImportSourceResult
}

/**
 * A file an import can keep: the bytes it read and their type. [nameSuffix] marks a copy
 * that is not the picked file itself, such as the clinical records packed from an Apple export.
 * [name] is the file's own name, for one of several files in the picked one. [links] are the
 * type and id of the records it holds; null means every record of the import.
 * [keepWithoutRecords] keeps a file that holds no record, such as a declaration of intent in an
 * Estonian export; [sourceName] then names where it came from.
 */
class MedicalSourceDocument(
    val bytes: ByteArray,
    val mimeType: String,
    val extension: String,
    val nameSuffix: String? = null,
    val name: String? = null,
    val links: Set<Pair<String, String>>? = null,
    val keepWithoutRecords: Boolean = false,
    val sourceName: String? = null,
) {
    val size: Long get() = bytes.size.toLong()

    /** The name the kept copy shows: its own, the picked file's, or the picked file's stem with [nameSuffix]. */
    fun fileName(original: String?): String {
        if (name != null) return name
        val stem = original?.substringAfterLast('/')?.substringBeforeLast('.')?.takeIf { it.isNotBlank() } ?: DefaultStem
        return if (nameSuffix == null && original != null) original.substringAfterLast('/') else "$stem${nameSuffix.orEmpty()}.$extension"
    }

    private companion object {
        const val DefaultStem = "medical-records"
    }
}

/** A record left out before any analysis. [detail] names it, such as a file path. */
data class MedicalSourceSkip(val reason: MedicalSourceSkipReason, val detail: String)

enum class MedicalSourceSkipReason {
    /** FHIR DSTU2 (1.0.2): Health Connect stores R4 and R4B only. */
    FHIR_DSTU2,
    UNSUPPORTED_FHIR_VERSION,

    /** An index entry whose file is not in the export. */
    FILE_MISSING,

    /** A file no index entry names, so its origin is unknown. */
    NOT_INDEXED,

    /** A file that is not FHIR JSON. */
    UNREADABLE,

    /** A document that is not well-formed XML. */
    UNREADABLE_DOCUMENT,

    /** A document that holds no health record, such as a declaration of intent. */
    NOT_A_RECORD,

    /** A SMART Health Card split over several QR codes, with parts missing from the file. */
    CARD_INCOMPLETE,
}

/** One resource, a Bundle, or NDJSON, up to [FhirFileParser.MaxFileBytes]. */
object FhirFileImportSource : MedicalImportSource {
    override fun read(open: () -> InputStream): MedicalImportSourceResult {
        val bytes = open().use { it.readUpTo(FhirFileParser.MaxFileBytes) } ?: return MedicalImportSourceResult.TooLarge
        return when (val parsed = FhirFileParser.parse(bytes.decodeFhirText())) {
            is FhirParseResult.Parsed -> MedicalImportSourceResult.Entries(parsed.file, documents = listOf(MedicalSourceDocument(bytes, JsonMimeType, "json")))
            FhirParseResult.NotFhir -> MedicalImportSourceResult.NotSupported
        }
    }
}

/** The whole stream, or null when it holds more than [maxBytes]. */
fun InputStream.readUpTo(maxBytes: Int): ByteArray? {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(BufferBytes)
    var total = 0
    while (true) {
        val read = read(buffer)
        if (read == -1) return output.toByteArray()
        total += read
        if (total > maxBytes) return null
        output.write(buffer, 0, read)
    }
}

/** UTF-8, without the byte order mark some exporters add and JSON does not allow. */
fun ByteArray.decodeFhirText(): String = decodeToString().removePrefix("\uFEFF")

private const val BufferBytes = 8 * 1024

private const val JsonMimeType = "application/json"
