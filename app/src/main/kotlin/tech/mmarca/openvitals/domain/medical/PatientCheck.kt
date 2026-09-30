package tech.mmarca.openvitals.domain.medical

import java.text.Normalizer
import kotlinx.serialization.json.JsonObject

/** A person as a Patient record names them. Identifiers are left out on purpose. */
data class PatientIdentity(
    val givenNames: List<String>,
    val familyNames: List<String>,
    val birthDate: String?,
) {
    val displayName: String get() = (givenNames + familyNames).joinToString(" ")

    companion object {
        /** The first `usual` or `official` name, else the first name. */
        fun of(patient: JsonObject): PatientIdentity {
            val names = patient.objects("name")
            val name = names.firstOrNull { it.string("use") in setOf("usual", "official") } ?: names.firstOrNull()
            val given = name?.strings("given").orEmpty()
            val family = listOfNotNull(name?.string("family"))
            val fromText = if (given.isEmpty() && family.isEmpty()) name?.string("text")?.split(' ').orEmpty() else emptyList()
            return PatientIdentity(
                givenNames = given.ifEmpty { fromText.dropLast(1) },
                familyNames = family.ifEmpty { fromText.takeLast(1) },
                birthDate = patient.string("birthDate"),
            )
        }
    }
}

sealed interface PatientCheckResult {
    /** A match, an empty store, or a file that names no patient. */
    data object Pass : PatientCheckResult

    /** The file names someone other than the records already here. */
    data class Mismatch(val file: List<PatientIdentity>, val store: List<PatientIdentity>) : PatientCheckResult

    /** The file names more than one person. */
    data class SeveralPeople(val file: List<PatientIdentity>) : PatientCheckResult

    /** Without access to personal details there is nothing to compare with. */
    data class CannotCompare(val file: List<PatientIdentity>) : PatientCheckResult
}

/**
 * Health Connect holds one person's records. Before an import, the people a file names
 * are compared with the Patient records already there, by name and birth date.
 *
 * Names compare without case or accents. A missing second given name or second family
 * name does not count as a difference, since providers spell names differently, and
 * neither does a missing name or birth date. Two
 * Patient records for the same person, as an OpenVitals export with several sources
 * holds, count as one person. Every result but [PatientCheckResult.Pass] asks the user.
 */
object PatientCheck {

    /** [store] is null when the app cannot read personal details. */
    fun check(file: List<JsonObject>, store: List<JsonObject>?): PatientCheckResult {
        val fileIds = distinctPeople(file.map(PatientIdentity::of))
        if (fileIds.isEmpty()) return PatientCheckResult.Pass
        if (fileIds.size > 1) return PatientCheckResult.SeveralPeople(fileIds)
        if (store == null) return PatientCheckResult.CannotCompare(fileIds)
        val storeIds = distinctPeople(store.map(PatientIdentity::of))
        if (storeIds.isEmpty()) return PatientCheckResult.Pass
        return if (storeIds.any { samePerson(it, fileIds.single()) }) {
            PatientCheckResult.Pass
        } else {
            PatientCheckResult.Mismatch(fileIds, storeIds)
        }
    }

    fun samePerson(a: PatientIdentity, b: PatientIdentity): Boolean {
        val dates = a.birthDate == null || b.birthDate == null || a.birthDate == b.birthDate
        return dates && namesMatch(a.givenNames, b.givenNames) && namesMatch(a.familyNames, b.familyNames)
    }

    /**
     * Equal when the shorter list's words all appear, in order, at the start of the longer
     * one. A missing name is unknown, not different, like a missing birth date.
     */
    private fun namesMatch(a: List<String>, b: List<String>): Boolean {
        val left = a.flatMap(::words)
        val right = b.flatMap(::words)
        if (left.isEmpty() || right.isEmpty()) return true
        val (short, long) = if (left.size <= right.size) left to right else right to left
        return long.take(short.size) == short
    }

    private fun distinctPeople(ids: List<PatientIdentity>): List<PatientIdentity> =
        ids.fold(mutableListOf()) { people, id ->
            if (people.none { samePerson(it, id) }) people += id
            people
        }

    private fun words(name: String): List<String> =
        Normalizer.normalize(name, Normalizer.Form.NFD).replace(CombiningMarks, "")
            .lowercase().split(NonLetters).filter { it.isNotEmpty() }

    private val CombiningMarks = Regex("\\p{Mn}+")
    private val NonLetters = Regex("[^\\p{L}\\p{N}]+")
}
