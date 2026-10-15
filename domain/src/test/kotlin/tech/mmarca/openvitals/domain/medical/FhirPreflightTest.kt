package tech.mmarca.openvitals.domain.medical

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Each case mirrors a refusal seen on the test phone in the step 1.0 spike. */
class FhirPreflightTest {

    private fun check(json: String): FhirPreflightProblem? {
        val resource = FhirTestFiles.json(json)
        return FhirPreflight.check(PreparedResource(resource.resourceType!!, resource.fhirId ?: "", resource))
    }

    @Test
    fun `a clean record passes`() {
        assertThat(check("""{"resourceType":"Immunization","id":"i1","status":"completed","vaccineCode":{"text":"Tdap"}}"""))
            .isNull()
    }

    @Test
    fun `an invalid id, contained resources and a refused type are held back`() {
        assertThat(check("""{"resourceType":"Condition","id":"bad id!"}""")?.reason).isEqualTo(FhirRejection.INVALID_ID)
        assertThat(check("""{"resourceType":"Condition","id":"c","contained":[{"resourceType":"Patient"}]}""")?.reason)
            .isEqualTo(FhirRejection.CONTAINED)
        assertThat(check("""{"resourceType":"DiagnosticReport","id":"r"}""")?.reason).isEqualTo(FhirRejection.UNSUPPORTED_TYPE)
    }

    @Test
    fun `an Observation needs a category or a LOINC code`() {
        val textOnly = """{"resourceType":"Observation","id":"o","status":"final","code":{"text":"Something"}}"""
        val loinc = """{"resourceType":"Observation","id":"o","status":"final",""" +
            """"code":{"coding":[{"system":"http://loinc.org","code":"29463-7"}]}}"""
        val lab = """{"resourceType":"Observation","id":"o","status":"final","code":{"text":"Glucose"},""" +
            """"category":[{"coding":[{"code":"laboratory"}]}]}"""

        assertThat(check(textOnly)?.reason).isEqualTo(FhirRejection.UNCLASSIFIABLE_OBSERVATION)
        assertThat(check(loinc)).isNull()
        assertThat(check(lab)).isNull()
    }

    @Test
    fun `an empty object or array is held back with where it is`() {
        assertThat(check("""{"resourceType":"Condition","id":"c","bodySite":[]}"""))
            .isEqualTo(FhirPreflightProblem(FhirRejection.EMPTY_VALUE, "bodySite"))
        assertThat(check("""{"resourceType":"AllergyIntolerance","id":"a","reaction":[{"manifestation":[{}]}]}"""))
            .isEqualTo(FhirPreflightProblem(FhirRejection.EMPTY_VALUE, "reaction.manifestation"))
    }
}
