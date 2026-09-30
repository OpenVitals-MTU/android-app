package tech.mmarca.openvitals.domain.model

/** A Health Connect medical records category. Each one has its own read permission. */
enum class MedicalCategory(val block: MedicalCategoryBlock) {
    VACCINES(MedicalCategoryBlock.CARE),
    ALLERGIES(MedicalCategoryBlock.CARE),
    CONDITIONS(MedicalCategoryBlock.CARE),
    MEDICATIONS(MedicalCategoryBlock.CARE),
    LAB_RESULTS(MedicalCategoryBlock.CARE),
    PROCEDURES(MedicalCategoryBlock.CARE),
    VISITS(MedicalCategoryBlock.CARE),
    VITAL_SIGNS(MedicalCategoryBlock.CARE),
    PREGNANCY(MedicalCategoryBlock.SENSITIVE),
    SOCIAL_HISTORY(MedicalCategoryBlock.SENSITIVE),
    PERSONAL_DETAILS(MedicalCategoryBlock.SENSITIVE),
    PRACTITIONER_DETAILS(MedicalCategoryBlock.SENSITIVE),
}

/** How the records home groups categories. Presentation only. */
enum class MedicalCategoryBlock { CARE, SENSITIVE }
