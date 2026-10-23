package tech.mmarca.openvitals.domain.medical

import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * A zip of FHIR files, as CommonHealth exports: each JSON or NDJSON file in it, whatever the
 * folders, is read as [FhirFileImportSource] reads one picked file. A file that is not FHIR is
 * left out and named, with what it holds instead ([MedicalFileInspector]), so the rest still
 * import. A zip with no FHIR file at all is not this source's.
 */
object FhirZipImportSource : MedicalImportSource {

    override fun read(open: () -> InputStream): MedicalImportSourceResult {
        val files = mutableListOf<FhirFile>()
        val read = linkedMapOf<String, ByteArray>()
        val skipped = mutableListOf<MedicalSourceSkip>()
        var budget = FhirFileParser.MaxFileBytes
        ZipInputStream(BufferedInputStream(open())).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val path = entry.name.replace('\\', '/')
                when {
                    entry.isDirectory || isHidden(path) -> Unit
                    !isFhirText(path) -> skipped += MedicalSourceSkip(MedicalSourceSkipReason.UNREADABLE, path)
                    else -> {
                        val bytes = zip.readUpTo(budget) ?: return MedicalImportSourceResult.TooLarge
                        budget -= bytes.size
                        when (val parsed = FhirFileParser.parse(bytes.decodeFhirText())) {
                            is FhirParseResult.Parsed -> {
                                files += parsed.file.withDstu2Converted()
                                read[path] = bytes
                            }
                            FhirParseResult.NotFhir ->
                                skipped += MedicalSourceSkip(MedicalSourceSkipReason.UNREADABLE, "$path: ${MedicalFileInspector.describe(bytes)}")
                        }
                    }
                }
                zip.closeEntry()
            }
        }
        if (files.isEmpty()) return MedicalImportSourceResult.NotSupported
        val merged = FhirFile(
            entries = files.flatMap { it.entries },
            isOpenVitalsExport = files.any { it.isOpenVitalsExport },
            bundleProfiles = files.flatMap { it.bundleProfiles }.distinct(),
        )
        val kept = MedicalSourceDocument(zipOf(read), "application/zip", "zip", nameSuffix = "-fhir")
        return MedicalImportSourceResult.Entries(merged, skipped, documents = listOf(kept))
    }

    /**
     * A Bundle's DSTU2 profile speaks for its own records only, so they are converted here,
     * before the files merge; the analyzer would read the profile as every file's. A type with
     * no mapping stays as it is, as the analyzer leaves it.
     */
    private fun FhirFile.withDstu2Converted(): FhirFile {
        if (bundleProfiles.none(Dstu2ToR4::profileNamesDstu2)) return this
        return copy(
            entries = entries.map { entry -> Dstu2ToR4.convert(entry.resource)?.let { entry.copy(resource = it) } ?: entry },
            bundleProfiles = bundleProfiles.filterNot(Dstu2ToR4::profileNamesDstu2),
        )
    }

    /** macOS adds a `__MACOSX` folder and dot files to the zips it makes; they hold no records. */
    private fun isHidden(path: String): Boolean =
        path.startsWith("__MACOSX/") || path.substringAfterLast('/').startsWith(".")

    private fun isFhirText(path: String): Boolean {
        val name = path.substringAfterLast('/')
        return '.' !in name || name.substringAfterLast('.').lowercase() in FhirExtensions
    }

    /** Exporters label FHIR loosely, as the picker's own types do; text is tried too. */
    private val FhirExtensions = setOf("json", "ndjson", "jsonl", "fhir", "txt")
}

/** [files] packed as one zip, by path, in their order. */
fun zipOf(files: Map<String, ByteArray>): ByteArray {
    val output = ByteArrayOutputStream()
    ZipOutputStream(output).use { zip ->
        files.forEach { (path, bytes) ->
            zip.putNextEntry(ZipEntry(path))
            zip.write(bytes)
            zip.closeEntry()
        }
    }
    return output.toByteArray()
}
