package tech.mmarca.openvitals.domain.medical

import com.google.common.truth.Truth.assertThat
import java.io.File
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * A check run by hand on a developer's computer, skipped in CI. OPENVITALS_FHIR_CORPUS names
 * one or more folders, split by `:`, of FHIR JSON and NDJSON files: the Synthea patients in the
 * repository's ignored `fhir/` folder, a SMART bulk export sample, the HL7 AU test data. Each
 * file goes through the wizard's own steps, one import per file, and a summary is printed.
 *
 * It fails on what the importer gets wrong on its own: a file that throws, a record left with an
 * id or a contained record the importer should have repaired, an R4 record taken for DSTU2. A
 * record held back for what Health Connect itself refuses, such as an Observation it cannot
 * classify, is only counted. What only Health Connect checks is not seen here.
 * The committed slices in `fhir/datasets` keep a few files of each corpus under CI.
 */
class FhirCorpusCheck {

    @Test
    fun `every file of the corpus imports without a rejection or a false conversion`() {
        val folders = System.getenv("OPENVITALS_FHIR_CORPUS")?.split(':')?.map(::File)?.filter { it.isDirectory }.orEmpty()
        assumeTrue(folders.isNotEmpty())
        val tally = Tally()
        folders.flatMap { folder -> folder.walkTopDown().filter { it.isFile && it.extension in setOf("json", "ndjson") }.sorted().toList() }
            .forEach { file -> tally.read(file) }
        tally.print()
        assertThat(tally.failures).isEmpty()
        assertThat(tally.rejected.keys.filter { key -> ImporterRepairs.any { it in key } }).isEmpty()
        assertThat(tally.converted).isEqualTo(0)
    }

    private class Tally {
        val outcomes = sortedMapOf<String, Int>()
        val failures = mutableListOf<String>()
        val rejected = sortedMapOf<String, Int>()
        val ready = sortedMapOf<String, Int>()
        val skippedTypes = sortedMapOf<String, Int>()
        val notes = sortedMapOf<String, Int>()
        val categories = sortedMapOf<String, Int>()
        val references = sortedMapOf<String, Int>()
        val patientChecks = sortedMapOf<String, Int>()
        val groupsPerFile = sortedMapOf<Int, Int>()
        val blankStrings = sortedMapOf<String, Int>()
        var converted = 0
        var noBreakSpacesInNarrative = 0
        var noBreakSpacesElsewhere = 0
        val rejectedExamples = mutableMapOf<String, String>()

        fun read(file: File) {
            val result = try {
                FhirFileImportSource.read { file.inputStream() }
            } catch (failure: Exception) {
                failures += "${file.name}: $failure"
                return
            }
            val entries = when (result) {
                is MedicalImportSourceResult.Entries -> result
                else -> return count(outcomes, result::class.simpleName.orEmpty())
            }
            count(outcomes, "read")
            val analysis = try {
                FhirImportAnalyzer.analyze(entries.file, file.name).also { MedicalImportPlanner.plan(it, emptyList(), emptyList()) }
            } catch (failure: Exception) {
                failures += "${file.name}: $failure"
                return
            }
            count(groupsPerFile, analysis.groups.size)
            count(patientChecks, PatientCheck.check(analysis.patients.map { it.json }, emptyList())::class.simpleName.orEmpty())
            for (group in analysis.groups) {
                val keys = group.ready.mapTo(mutableSetOf()) { "${it.type}/${it.id}" }
                group.ready.forEach { resource ->
                    count(ready, resource.type)
                    if (Dstu2ToR4.isConverted(resource.json)) converted++
                    // Health Connect refuses one in a coded text (seen on a phone); in the XHTML narrative it is not known.
                    if (NoBreakSpace in resource.json.without("text").toString()) noBreakSpacesElsewhere++
                    else if (NoBreakSpace in resource.json.toString()) noBreakSpacesInNarrative++
                    blankPaths(resource.json, resource.type).forEach { count(blankStrings, it) }
                    referencesOf(resource.json).forEach { count(references, shapeOf(it, keys)) }
                }
                group.rejected.forEach { rejection ->
                    val key = "${rejection.resource.type} ${rejection.problem.reason} ${rejection.problem.field.orEmpty()}".trim()
                    count(rejected, key)
                    rejectedExamples.putIfAbsent(key, file.name)
                }
                group.skippedTypes.forEach { (type, n) -> count(skippedTypes, type, n) }
                group.notes.forEach { count(notes, it::class.simpleName.orEmpty()) }
                group.likelyCategories.forEach { (category, n) -> count(categories, category?.name ?: "by LOINC only", n) }
            }
        }

        fun print() {
            println("files: $outcomes, sources per file: $groupsPerFile")
            println("failures: ${failures.size}" + failures.take(5).joinToString("") { "\n  $it" })
            println("ready by type: $ready")
            println("rejected: $rejected")
            rejectedExamples.forEach { (key, name) -> println("  first $key in $name") }
            println("types Health Connect does not store: $skippedTypes")
            println("id notes: $notes")
            println("likely categories: $categories")
            println("references: $references")
            println("patient check: $patientChecks")
            println("blank strings: $blankStrings")
            println("converted from DSTU2: $converted")
            println("records with a no-break space outside the narrative: $noBreakSpacesElsewhere, in the narrative only: $noBreakSpacesInNarrative")
        }

        private fun <K> count(map: MutableMap<K, Int>, key: K, n: Int = 1) {
            map[key] = (map[key] ?: 0) + n
        }

        /** Where a reference points, as a shape: resolved in the file, a relative one to a record not in it, and so on. */
        private fun shapeOf(reference: String, keys: Set<String>): String = when {
            reference in keys -> "resolved"
            reference.startsWith("#") -> "local"
            reference.startsWith("urn:", ignoreCase = true) -> "urn, unresolved"
            '?' in reference -> "conditional"
            reference.startsWith("http", ignoreCase = true) -> "absolute"
            RelativeReference.matches(reference) -> "relative, not in file"
            else -> "other"
        }

        private fun referencesOf(element: JsonElement): List<String> = when (element) {
            is JsonObject -> listOfNotNull(element.string("reference")) + element.values.flatMap(::referencesOf)
            is JsonArray -> element.flatMap(::referencesOf)
            else -> emptyList()
        }

        /** FHIR allows no empty or whitespace-only string. */
        private fun blankPaths(element: JsonElement, path: String): List<String> = when (element) {
            is JsonObject -> element.entries.flatMap { (key, value) -> blankPaths(value, "$path.$key") }
            is JsonArray -> element.flatMap { blankPaths(it, path) }
            is JsonPrimitive -> if (element.isString && element.content.isBlank()) listOf(path) else emptyList()
        }
    }

    private companion object {
        /** Rejections the importer repairs on its own, so one left over is its fault. */
        val ImporterRepairs = listOf(FhirRejection.INVALID_ID.name, FhirRejection.CONTAINED.name)
        const val NoBreakSpace = '\u00A0'
        val RelativeReference = Regex("[A-Z][A-Za-z]+/[A-Za-z0-9\\-.]{1,64}(/_history/[A-Za-z0-9\\-.]{1,64})?")
    }
}
