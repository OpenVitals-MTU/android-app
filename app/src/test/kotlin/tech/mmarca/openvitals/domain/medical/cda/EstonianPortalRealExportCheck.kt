package tech.mmarca.openvitals.domain.medical.cda

import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assume.assumeTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.medical.FhirImportAnalyzer
import tech.mmarca.openvitals.domain.medical.MedicalImportSourceResult

/**
 * Checks run by hand on a developer's computer, all skipped in CI.
 *
 * - With OPENVITALS_EE_EXPORT naming an unzipped real export, the importer runs over it and
 *   prints counts only: no names, codes, values or dates. No personal file enters the repository.
 * - With OPENVITALS_EE_SAMPLE_OUT or OPENVITALS_CCDA_SAMPLE_OUT naming a file, the invented
 *   Estonian export or the invented standard document is written there for a check on a phone.
 */
class EstonianPortalRealExportCheck {

    @Test
    fun `a real export maps without pre-flight rejections`() {
        val folder = System.getenv("OPENVITALS_EE_EXPORT")?.let(::File)
        assumeTrue(folder != null && folder.isDirectory)
        val zip = zipOf(folder!!)
        val result = CdaImportSource.read { ByteArrayInputStream(zip) } as MedicalImportSourceResult.Entries
        val analysis = FhirImportAnalyzer.analyze(result.file, "export.zip")
        val resources = result.file.entries.map { it.resource }

        // Ids must not change between versions, or a re-import would duplicate every record.
        println("id fingerprint: " + tech.mmarca.openvitals.domain.medical.sha256Hex(result.file.entries.map { "${it.origin?.baseUri}|${it.type}/${it.resource["id"]}" }.sorted().joinToString("\n")))
        println("records by type: " + resources.groupingBy { it.string("resourceType").orEmpty() }.eachCount().toSortedMap())
        println("skipped documents: " + result.skipped.groupingBy { it.reason }.eachCount())
        println("sources: ${analysis.groups.size}, ready ${analysis.readyCount}")
        assertThat(analysis.groups.flatMap { it.rejected }).isEmpty()
        // Health Connect refuses a no-break space in a FHIR string.
        assertThat(resources.count { " " in it.toString() }).isEqualTo(0)
        println("rejected: " + analysis.groups.flatMap { it.rejected }.groupingBy { "${it.problem.reason} ${it.problem.field.orEmpty()}" }.eachCount())
        println("id notes: " + analysis.groups.flatMap { it.notes }.groupingBy { it::class.simpleName }.eachCount())
        println("kept PDFs offered: ${result.documents.size}, bytes ${result.documents.sumOf { it.size }}")
        val observations = resources.filter { it.string("resourceType") == "Observation" }
        println("observation values: " + observations.groupingBy { o -> o.keys.firstOrNull { it.startsWith("value") } ?: "none" }.eachCount())
        println("observation code systems: " + observations.groupingBy { it.codeSystem("code") }.eachCount())
        println("observations with range: ${observations.count { "referenceRange" in it }}, interpretation: ${observations.count { "interpretation" in it }}")
        val conditions = resources.filter { it.string("resourceType") == "Condition" }
        println("condition code systems: " + conditions.groupingBy { it.codeSystem("code") }.eachCount())
        println("condition verification: " + conditions.groupingBy { it.codeSystem("verificationStatus", code = true) }.eachCount())
        println("encounter classes: " + resources.filter { it.string("resourceType") == "Encounter" }.groupingBy { it["class"]?.jsonObject?.string("code") }.eachCount())
        val empty = folder.walkTopDown().filter { it.extension == "xml" }.mapNotNull { file ->
            val root = parseCda(file.readBytes()) ?: return@mapNotNull null
            val mapped = EstonianCdaMapper.map(root) as? CdaMapResult.Mapped ?: return@mapNotNull null
            val sections = root.at("component", "structuredBody")?.children("component")?.mapNotNull { it.child("section")?.child("code")?.attr("code") }
            (root.child("code")?.attr("code") + " " + sections).takeIf { mapped.entries.all { it.type in CdaSharedTypes } }
        }.toList()
        println("documents with no records of their own: " + empty.groupingBy { it }.eachCount())
        println("records with no date: " + resources.filter { r -> DateKeys.none { it in r } && r.string("resourceType") !in Undated }.groupingBy { it.string("resourceType") }.eachCount())
    }

    /** Writes the invented sample export for a check on a phone. Skipped unless OPENVITALS_EE_SAMPLE_OUT names a file. */
    @Test
    fun `the invented sample export is written for a device check`() {
        val target = System.getenv("OPENVITALS_EE_SAMPLE_OUT")?.let(::File)
        assumeTrue(target != null)
        target!!.writeBytes(EstonianCdaFixtures.zip(EstonianCdaFixtures.all))
    }

    /** Writes the invented C-CDA document for a check on a phone. Skipped unless OPENVITALS_CCDA_SAMPLE_OUT names a file. */
    @Test
    fun `the invented standard document is written for a device check`() {
        val target = System.getenv("OPENVITALS_CCDA_SAMPLE_OUT")?.let(::File)
        assumeTrue(target != null)
        target!!.writeText(CcdaFixtures.summary)
    }

    private fun zipOf(folder: File): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            folder.walkTopDown().filter { it.isFile && it.extension.lowercase() in setOf("xml", "pdf") }.forEach { file ->
                zip.putNextEntry(ZipEntry(file.relativeTo(folder).path))
                zip.write(file.readBytes())
                zip.closeEntry()
            }
        }
        return output.toByteArray()
    }

    private fun JsonObject.string(key: String): String? = this[key]?.jsonPrimitive?.content

    /** The system of a CodeableConcept's first coding, or its code with [code]. Never the code of a finding. */
    private fun JsonObject.codeSystem(key: String, code: Boolean = false): String? {
        val coding = this[key]?.jsonObject?.get("coding")?.jsonArray?.firstOrNull()?.jsonObject ?: return "text only"
        return if (code) coding.string("code") else coding.string("system")
    }

    private companion object {
        val DateKeys = listOf("recordedDate", "effectiveDateTime", "occurrenceDateTime", "authoredOn", "performedDateTime", "period", "birthDate")
        val Undated = setOf("Patient", "Practitioner", "Organization")
    }
}
