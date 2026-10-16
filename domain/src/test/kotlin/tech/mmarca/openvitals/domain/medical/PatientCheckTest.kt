package tech.mmarca.openvitals.domain.medical

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.JsonObject
import org.junit.Test

/** OpenVitals holds one person's records. A file for someone else must stop and ask. */
class PatientCheckTest {

    private fun patient(given: String, family: String, birthDate: String? = "1978-11-03"): JsonObject {
        val givenNames = given.split(' ').joinToString(",") { "\"$it\"" }
        val date = birthDate?.let { ""","birthDate":"$it"""" } ?: ""
        return FhirTestFiles.json("""{"resourceType":"Patient","name":[{"family":"$family","given":[$givenNames]}]$date}""")
    }

    private val maria = patient("María José", "García López")

    @Test
    fun `the same person passes despite case, accents and a missing second name`() {
        val result = PatientCheck.check(listOf(patient("MARIA", "Garcia")), store = listOf(maria))

        assertThat(result).isEqualTo(PatientCheckResult.Pass)
    }

    @Test
    fun `a first import into an empty store passes`() {
        assertThat(PatientCheck.check(listOf(maria), store = emptyList())).isEqualTo(PatientCheckResult.Pass)
    }

    @Test
    fun `a file that names no patient passes`() {
        assertThat(PatientCheck.check(emptyList(), store = listOf(maria))).isEqualTo(PatientCheckResult.Pass)
    }

    @Test
    fun `someone else stops the import with both identities`() {
        val child = patient("Lucía", "García López", birthDate = "2015-04-20")

        val result = PatientCheck.check(listOf(child), store = listOf(maria))

        assertThat(result).isInstanceOf(PatientCheckResult.Mismatch::class.java)
        result as PatientCheckResult.Mismatch
        assertThat(result.file.single().displayName).isEqualTo("Lucía García López")
        assertThat(result.store.single().birthDate).isEqualTo("1978-11-03")
    }

    @Test
    fun `a different birth date is a different person`() {
        val result = PatientCheck.check(listOf(patient("María José", "García López", "1978-11-04")), store = listOf(maria))

        assertThat(result).isInstanceOf(PatientCheckResult.Mismatch::class.java)
    }

    @Test
    fun `a file that names two people stops the import`() {
        val result = PatientCheck.check(listOf(maria, patient("Pedro", "Sánchez")), store = listOf(maria))

        assertThat(result).isInstanceOf(PatientCheckResult.SeveralPeople::class.java)
    }

    @Test
    fun `two records for one person, as an export with two sources holds, count once`() {
        val result = PatientCheck.check(listOf(maria, patient("María", "García")), store = listOf(maria))

        assertThat(result).isEqualTo(PatientCheckResult.Pass)
    }

    @Test
    fun `without access to personal details the user confirms`() {
        val result = PatientCheck.check(listOf(maria), store = null)

        assertThat(result).isInstanceOf(PatientCheckResult.CannotCompare::class.java)
    }

    @Test
    fun `a missing birth date is unknown, not different`() {
        val result = PatientCheck.check(listOf(patient("María José", "García López", birthDate = null)), store = listOf(maria))

        assertThat(result).isEqualTo(PatientCheckResult.Pass)
    }

    @Test
    fun `a name given only as text is split into given and family`() {
        val identity = PatientIdentity.of(FhirTestFiles.json("""{"resourceType":"Patient","name":[{"text":"Alex Doe"}]}"""))

        assertThat(identity.givenNames).containsExactly("Alex")
        assertThat(identity.familyNames).containsExactly("Doe")
    }
}
