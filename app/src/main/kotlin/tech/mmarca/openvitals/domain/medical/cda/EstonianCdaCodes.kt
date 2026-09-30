package tech.mmarca.openvitals.domain.medical.cda

import tech.mmarca.openvitals.domain.medical.MedicalSourceSkipReason

/** The codes and ids of Estonian CDA documents (the national health information system's profiles). */
internal object EstonianCdaCodes {
    /** The root of every Estonian document's templateId. */
    const val TemplateRoot = "1.3.6.1.4.1.28284.6.1.1"

    /** The business registry: an organisation's id root. The extension is its registry code. */
    const val BusinessRegistryRoot = "1.3.6.1.4.1.28284.4"

    /** The registry of healthcare professionals: a doctor's or nurse's id root. */
    const val HealthcareProfessionalRoot = "1.3.6.1.4.1.28284.6.2.4.9"

    // Section codes.
    const val Visit = "AMBS"
    const val Diagnoses = "DGN"
    const val Dental = "DENTDISE"
    const val Drugs = "DRUG"
    const val Labs = "ANA"
    const val Immunisation = "IMM"
    const val Radiology = "RG_PROC"
    const val Pathology = "PAT_PROC"

    /** "Teostatud uuringud": studies done, such as imaging, each with its description and finding. */
    const val Studies = "PROC"

    /** The observation type of a diagnosis. */
    const val DiagnosisType = "DGN"

    /** "Diagnoosi statistiline liik": its code 3 marks a provisional diagnosis. */
    private const val DiagnosisKindList = "1.3.6.1.4.1.28284.6.2.1.1.2"
    private const val ProvisionalDiagnosis = "3"

    /** "Visiidi tüüp": its code 31 is a phone consultation. */
    private const val VisitTypeList = "1.3.6.1.4.1.28284.6.2.1.32."
    private const val PhoneConsultation = "31"

    /** The observation type lists: codes such as ANA and DGN say what an observation holds, not what was measured. */
    private val ObservationTypeLists = listOf("1.3.6.1.4.1.28284.6.2.2.5.", "1.3.6.1.4.1.28284.6.2.2.12.")

    /** A document type left out, with the reason; null for one that is imported. */
    fun skipReason(documentCode: String?): MedicalSourceSkipReason? = when {
        documentCode == null -> null
        documentCode.startsWith("18.") || documentCode == "9" -> MedicalSourceSkipReason.NOT_A_RECORD
        else -> null
    }

    /** A referral (63.x): only its diagnoses are read, and they are provisional. */
    fun isReferral(documentCode: String?): Boolean = documentCode?.startsWith("63.") == true

    fun isObservationType(codeSystem: String?): Boolean = codeSystem != null && ObservationTypeLists.any { codeSystem.startsWith(it) }

    fun isProvisional(kindCode: String?, kindSystem: String?): Boolean = kindSystem == DiagnosisKindList && kindCode == ProvisionalDiagnosis

    fun isPhoneConsultation(code: String?, system: String?): Boolean = system?.startsWith(VisitTypeList) == true && code == PhoneConsultation
}
