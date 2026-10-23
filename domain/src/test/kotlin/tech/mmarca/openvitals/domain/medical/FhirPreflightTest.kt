package tech.mmarca.openvitals.domain.medical

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import tech.mmarca.openvitals.domain.model.MedicalCategory

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
    fun `an Observation needs a category or a LOINC code Health Connect files it by`() {
        fun observation(fields: String) = """{"resourceType":"Observation","id":"o","status":"final",$fields}"""
        val category = """"category":[{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/observation-category","code":"laboratory"}]}]"""

        assertThat(check(observation(""""code":{"text":"Something"}"""))?.reason).isEqualTo(FhirRejection.UNCLASSIFIABLE_OBSERVATION)
        // Body weight is on the platform's vital signs list.
        assertThat(check(observation(""""code":{"coding":[{"system":"http://loinc.org","code":"29463-7"}]}"""))).isNull()
        // Glucose is not on any list, so without a category Health Connect cannot file it.
        assertThat(check(observation(""""code":{"coding":[{"system":"http://loinc.org","code":"2345-7"}]}"""))?.reason)
            .isEqualTo(FhirRejection.UNCLASSIFIABLE_OBSERVATION)
        assertThat(check(observation(""""code":{"text":"Glucose"},$category"""))).isNull()
        // The platform reads category codes under their code system only.
        assertThat(check(observation(""""code":{"text":"Glucose"},"category":[{"coding":[{"code":"laboratory"}]}]"""))?.reason)
            .isEqualTo(FhirRejection.UNCLASSIFIABLE_OBSERVATION)
    }

    @Test
    fun `an Observation lands where the platform files it, pregnancy first`() {
        fun observation(loinc: String, category: String) = FhirTestFiles.json(
            """{"resourceType":"Observation","id":"o","status":"final",
               "code":{"coding":[{"system":"http://loinc.org","code":"$loinc"}]},
               "category":[{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/observation-category","code":"$category"}]}]}""",
        )

        assertThat(FhirResourceTypes.likelyCategory(observation("82810-3", "laboratory"))).isEqualTo(MedicalCategory.PREGNANCY)
        assertThat(FhirResourceTypes.likelyCategory(observation("72166-2", "vital-signs"))).isEqualTo(MedicalCategory.SOCIAL_HISTORY)
        assertThat(FhirResourceTypes.likelyCategory(observation("8867-4", "laboratory"))).isEqualTo(MedicalCategory.VITAL_SIGNS)
        assertThat(FhirResourceTypes.likelyCategory(observation("2345-7", "laboratory"))).isEqualTo(MedicalCategory.LAB_RESULTS)
    }

    @Test
    fun `an empty object or array is held back with where it is`() {
        assertThat(check("""{"resourceType":"Condition","id":"c","bodySite":[]}"""))
            .isEqualTo(FhirPreflightProblem(FhirRejection.EMPTY_VALUE, "bodySite"))
        assertThat(check("""{"resourceType":"AllergyIntolerance","id":"a","reaction":[{"manifestation":[{}]}]}"""))
            .isEqualTo(FhirPreflightProblem(FhirRejection.EMPTY_VALUE, "reaction.manifestation"))
    }
}
