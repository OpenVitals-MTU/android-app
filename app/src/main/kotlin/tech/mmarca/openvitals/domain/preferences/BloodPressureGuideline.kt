package tech.mmarca.openvitals.domain.preferences

/** Which published guideline names the blood pressure categories. Stored by name. */
enum class BloodPressureGuideline {
    /**
     * United States. Hypertension starts at 130/80. Whelton et al. 2018,
     * doi:10.1161/HYP.0000000000000065. The 2025 update
     * (doi:10.1161/HYP.0000000000000249) keeps the four categories.
     */
    ACC_AHA_2017,

    /**
     * Europe, graded. Hypertension starts at 140/90. Mancia et al. 2023,
     * doi:10.1097/HJH.0000000000003480.
     */
    ESH_2023,

    /**
     * Europe, three categories. Hypertension starts at 140/90. McEvoy et al.
     * 2024, doi:10.1093/eurheartj/ehae178.
     */
    ESC_2024,

    /**
     * International. Hypertension starts at 140/90. Unger et al. 2020,
     * doi:10.1161/HYPERTENSIONAHA.120.15026.
     */
    ISH_2020,
}
