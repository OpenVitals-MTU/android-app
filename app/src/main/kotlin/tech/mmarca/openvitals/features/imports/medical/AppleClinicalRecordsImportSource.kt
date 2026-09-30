package tech.mmarca.openvitals.features.imports.medical

import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.FilterInputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import tech.mmarca.openvitals.domain.medical.Dstu2ToR4
import tech.mmarca.openvitals.domain.medical.FhirEntry
import tech.mmarca.openvitals.domain.medical.FhirFile
import tech.mmarca.openvitals.domain.medical.FhirFileImportSource
import tech.mmarca.openvitals.domain.medical.FhirFileParser
import tech.mmarca.openvitals.domain.medical.FhirOrigin
import tech.mmarca.openvitals.domain.medical.FhirParseResult
import tech.mmarca.openvitals.domain.medical.FhirSourceGrouper
import tech.mmarca.openvitals.domain.medical.FhirVersionDetector
import tech.mmarca.openvitals.domain.medical.MedicalImportSource
import tech.mmarca.openvitals.domain.medical.MedicalImportSourceResult
import tech.mmarca.openvitals.domain.medical.MedicalSourceDocument
import tech.mmarca.openvitals.domain.medical.MedicalSourceSkip
import tech.mmarca.openvitals.domain.medical.MedicalSourceSkipReason
import tech.mmarca.openvitals.domain.medical.cda.CdaImportSource
import tech.mmarca.openvitals.domain.medical.decodeFhirText
import tech.mmarca.openvitals.domain.medical.readUpTo
import tech.mmarca.openvitals.domain.medical.shc.CardFileType
import tech.mmarca.openvitals.features.imports.applehealth.AppleClinicalRecordIndexEntry
import tech.mmarca.openvitals.features.imports.applehealth.AppleCorrelation
import tech.mmarca.openvitals.features.imports.applehealth.AppleHealthImportParser
import tech.mmarca.openvitals.features.imports.applehealth.AppleHealthParseOptions
import tech.mmarca.openvitals.features.imports.applehealth.AppleHealthXmlEventConsumer
import tech.mmarca.openvitals.features.imports.applehealth.AppleRecord
import tech.mmarca.openvitals.features.imports.applehealth.AppleWorkout

/**
 * The clinical records in an Apple Health export zip.
 *
 * export.xml lists each record as a `ClinicalRecord` element with its provider, FHIR
 * version and file. One pass reads that index and the `clinical-records/` files, in
 * whatever order the zip holds them. Each record takes its provider as its origin, so two
 * hospitals never share a data source. R4 and R4B records continue as they are. Health
 * Connect refuses DSTU2, the version older iPhones export, so those records are converted
 * to R4 first ([Dstu2ToR4]); one of a type with no mapping is left out, and the review says why.
 */
internal object AppleClinicalRecordsImportSource : MedicalImportSource {

    override fun read(open: () -> InputStream): MedicalImportSourceResult {
        val index = mutableListOf<AppleClinicalRecordIndexEntry>()
        val files = linkedMapOf<String, String>()
        var budget = FhirFileParser.MaxFileBytes
        var sawExport = false
        ZipInputStream(BufferedInputStream(open())).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val path = clinicalPath(entry.name)
                when {
                    entry.isDirectory -> Unit
                    entry.name.substringAfterLast('/') == ExportXml -> {
                        sawExport = true
                        AppleHealthImportParser.parse(
                            input = BufferedInputStream(NonClosing(zip)),
                            consumer = IndexCollector(index),
                            options = AppleHealthParseOptions(parseRouteFiles = false, parseRecordDetails = false),
                        )
                    }
                    path != null && path.endsWith(".json") -> {
                        val bytes = NonClosing(zip).readUpTo(budget) ?: return MedicalImportSourceResult.TooLarge
                        budget -= bytes.size
                        files[path] = bytes.decodeFhirText()
                    }
                }
                zip.closeEntry()
            }
        }
        if (!sawExport) return MedicalImportSourceResult.NotSupported
        if (index.isEmpty()) return MedicalImportSourceResult.Empty
        return entries(index, files)
    }

    private fun entries(index: List<AppleClinicalRecordIndexEntry>, files: Map<String, String>): MedicalImportSourceResult {
        val entries = mutableListOf<FhirEntry>()
        val skipped = mutableListOf<MedicalSourceSkip>()
        val used = mutableSetOf<String>()
        index.forEach { record ->
            val path = record.resourceFilePath?.let(::clinicalPath)
            val label = path ?: record.type ?: "ClinicalRecord"
            val version = record.fhirVersion?.trim()
            val dstu2 = version == Dstu2ToR4.Version
            when {
                !dstu2 && version != FhirVersionDetector.R4 && version != FhirVersionDetector.R4B ->
                    skipped += MedicalSourceSkip(MedicalSourceSkipReason.UNSUPPORTED_FHIR_VERSION, "$label (${version ?: "no version"})")
                path == null || path !in files -> skipped += MedicalSourceSkip(MedicalSourceSkipReason.FILE_MISSING, label)
                else -> {
                    used += path
                    when (val parsed = FhirFileParser.parse(files.getValue(path))) {
                        is FhirParseResult.Parsed -> parsed.file.entries.forEach { entry ->
                            // A DSTU2 record is converted to R4 and joins the provider's R4 source. A type with no mapping is left out.
                            val resource = if (dstu2) Dstu2ToR4.convert(entry.resource) else entry.resource
                            if (resource == null) {
                                skipped += MedicalSourceSkip(MedicalSourceSkipReason.FHIR_DSTU2, "$label (${entry.type})")
                            } else {
                                entries += entry.copy(resource = resource, origin = origin(record, if (dstu2 || version == null) FhirVersionDetector.R4 else version))
                            }
                        }
                        FhirParseResult.NotFhir -> skipped += MedicalSourceSkip(MedicalSourceSkipReason.UNREADABLE, path)
                    }
                }
            }
        }
        (files.keys - used).forEach { path ->
            if (index.none { it.resourceFilePath?.let(::clinicalPath) == path }) {
                skipped += MedicalSourceSkip(MedicalSourceSkipReason.NOT_INDEXED, path)
            }
        }
        return MedicalImportSourceResult.Entries(FhirFile(entries), skipped, documents = listOf(clinicalZip(files)))
    }

    /** Only the clinical records files, packed as one zip: the rest of the export is not kept. */
    private fun clinicalZip(files: Map<String, String>): MedicalSourceDocument {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            files.forEach { (path, text) ->
                zip.putNextEntry(ZipEntry(path))
                zip.write(text.toByteArray())
                zip.closeEntry()
            }
        }
        return MedicalSourceDocument(output.toByteArray(), "application/zip", "zip", nameSuffix = "-clinical-records")
    }

    /** The provider as Apple names it. A provider with no URL gets one from its name. */
    private fun origin(record: AppleClinicalRecordIndexEntry, version: String): FhirOrigin {
        val name = record.sourceName?.takeIf { it.isNotBlank() } ?: DefaultProvider
        val baseUri = record.sourceUrl?.takeIf { it.isNotBlank() } ?: FhirSourceGrouper.importBaseUri(name)
        return FhirOrigin(baseUri = baseUri, displayName = name, fhirVersion = version)
    }

    /** `clinical-records/<file>` in lower case, from a zip entry or an index path. Null for anything else. */
    private fun clinicalPath(name: String): String? {
        val lower = name.replace('\\', '/').lowercase()
        val start = lower.indexOf(ClinicalRecordsDirectory)
        return if (start < 0) null else lower.substring(start)
    }

    /** Collects the index; everything else in export.xml is Health Connect data, which the Apple import handles. */
    private class IndexCollector(private val index: MutableList<AppleClinicalRecordIndexEntry>) : AppleHealthXmlEventConsumer {
        override fun shouldMaterializeRecord(type: String): Boolean = false
        override fun onParsedType(type: String) = Unit
        override fun onRecord(record: AppleRecord) = Unit
        override fun onWorkout(workout: AppleWorkout) = Unit
        override fun onCorrelation(correlation: AppleCorrelation) = Unit
        override fun onActivitySummary() = Unit
        override fun onClinicalRecord(entry: AppleClinicalRecordIndexEntry) {
            index += entry
        }
    }

    /** The zip entry stream, which the XML parser must not close: the next entry follows it. */
    private class NonClosing(input: InputStream) : FilterInputStream(input) {
        override fun close() = Unit
    }

    private const val ExportXml = "export.xml"
    private const val ClinicalRecordsDirectory = "clinical-records/"
    private const val DefaultProvider = "Apple Health"
}

/**
 * Sends a picked file to the source that reads it. A zip is an Apple Health export or a set of
 * CDA documents, as the Estonian health portal hands out. XML is a CDA document. An image or a
 * PDF can only hold a SMART Health Card. Other text is a card when it looks like one, and a
 * FHIR file otherwise.
 */
internal class PickedFileImportSource(private val cards: MedicalImportSource) : MedicalImportSource {
    override fun read(open: () -> InputStream): MedicalImportSourceResult {
        val header = open().use { it.readHeader(HeaderBytes) }
        val isZip = header.size >= 2 && header[0] == 'P'.code.toByte() && header[1] == 'K'.code.toByte()
        return when {
            isZip -> AppleClinicalRecordsImportSource.read(open).takeUnless { it == MedicalImportSourceResult.NotSupported }
                ?: CdaImportSource.read(open)
            CardFileType.isPictureOrPdf(header) -> cards.read(open)
            header.decodeToString().removePrefix("\uFEFF").trimStart().startsWith("<") -> CdaImportSource.read(open)
            else -> cards.read(open).takeUnless { it == MedicalImportSourceResult.NotSupported } ?: FhirFileImportSource.read(open)
        }
    }

    /** The first [size] bytes, or fewer in a shorter file. */
    private fun InputStream.readHeader(size: Int): ByteArray {
        val buffer = ByteArray(size)
        var filled = 0
        while (filled < size) {
            val read = read(buffer, filled, size - filled)
            if (read == -1) break
            filled += read
        }
        return buffer.copyOf(filled)
    }

    private companion object {
        /** Enough for every file signature the sources look for. */
        const val HeaderBytes = 16
    }
}
