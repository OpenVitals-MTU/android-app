package tech.mmarca.openvitals.domain.preferences

/** Which published guideline names the blood pressure categories. Stored by name. */
enum class BloodPressureGuideline {
    /** United States. Hypertension starts at 130/80. */
    ACC_AHA_2017,

    /** Europe, graded. Hypertension starts at 140/90. */
    ESH_2023,

    /** Europe, three categories. Hypertension starts at 140/90. */
    ESC_2024,

    /** International. Hypertension starts at 140/90. */
    ISH_2020,
}
