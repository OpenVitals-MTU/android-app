package tech.mmarca.openvitals.domain.medical.cda

/** CDA names a code system by OID; FHIR names it by URI. */
internal object CdaCodeSystems {
    const val Loinc = "http://loinc.org"
    const val Icd10 = "http://hl7.org/fhir/sid/icd-10"

    private val Systems = mapOf(
        "2.16.840.1.113883.6.1" to Loinc,
        "2.16.840.1.113883.6.96" to "http://snomed.info/sct",
        "2.16.840.1.113883.6.88" to "http://www.nlm.nih.gov/research/umls/rxnorm",
        "2.16.840.1.113883.12.292" to "http://hl7.org/fhir/sid/cvx",
        "2.16.840.1.113883.6.69" to "http://hl7.org/fhir/sid/ndc",
        "2.16.840.1.113883.6.73" to "http://www.whocc.no/atc",
        "2.16.840.1.113883.6.3" to Icd10,
        "2.16.840.1.113883.6.90" to "http://hl7.org/fhir/sid/icd-10-cm",
        "2.16.840.1.113883.6.103" to "http://hl7.org/fhir/sid/icd-9-cm",
        "2.16.840.1.113883.6.12" to "http://www.ama-assn.org/go/cpt",
        "2.16.840.1.113883.6.8" to "http://unitsofmeasure.org",
        "2.16.840.1.113883.5.4" to "http://terminology.hl7.org/CodeSystem/v3-ActCode",
        "2.16.840.1.113883.5.83" to "http://terminology.hl7.org/CodeSystem/v3-ObservationInterpretation",
        "2.16.840.1.113883.5.1" to "http://terminology.hl7.org/CodeSystem/v3-AdministrativeGender",
        "2.16.840.1.113883.5.112" to "http://terminology.hl7.org/CodeSystem/v3-RouteOfAdministration",
    )

    /** RHK-10, Estonia's ICD-10, in its versions, and the notifiable disease list that uses its codes. */
    private val Icd10Lists = listOf("1.3.6.1.4.1.28284.6.2.1.13.", "1.3.6.1.4.1.28284.6.2.1.117.")

    private val OidShape = Regex("[0-2](\\.(0|[1-9][0-9]*))+")

    /** The FHIR system for a code system OID: a known URI, else `urn:oid:`. Null for a value that is not an OID. */
    fun systemUri(oid: String): String? = when {
        !OidShape.matches(oid) -> null
        isIcd10List(oid) -> Icd10
        else -> Systems[oid] ?: "urn:oid:$oid"
    }

    fun isIcd10List(oid: String?): Boolean = oid != null && Icd10Lists.any { oid.startsWith(it) }
}
