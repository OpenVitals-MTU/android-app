package tech.mmarca.openvitals.domain.model

/**
 * The unit a nutrition label prints a nutrient in. What is stored stays grams (kcal for
 * energy); [decimalShift] moves the decimal point between the two. The food form types in
 * it, the CSV importer reads in it by default, and the health report prints in it.
 */
enum class NutrientLabelUnit(val decimalShift: Int, val symbol: String) {
    KCAL(0, "kcal"),
    GRAM(0, "g"),
    MILLIGRAM(3, "mg"),
    MICROGRAM(6, "µg"),
}

/** The unit nutrition labels print this nutrient in: µg for vitamin D, mg for sodium, g for protein. */
val NutritionNutrient.labelUnit: NutrientLabelUnit
    get() = when (this) {
        NutritionNutrient.ENERGY,
        NutritionNutrient.ENERGY_FROM_FAT -> NutrientLabelUnit.KCAL
        NutritionNutrient.PROTEIN,
        NutritionNutrient.TOTAL_CARBOHYDRATE,
        NutritionNutrient.TOTAL_FAT,
        NutritionNutrient.DIETARY_FIBER,
        NutritionNutrient.SUGAR,
        NutritionNutrient.MONOUNSATURATED_FAT,
        NutritionNutrient.POLYUNSATURATED_FAT,
        NutritionNutrient.SATURATED_FAT,
        NutritionNutrient.TRANS_FAT,
        NutritionNutrient.UNSATURATED_FAT -> NutrientLabelUnit.GRAM
        NutritionNutrient.CHOLESTEROL,
        NutritionNutrient.NIACIN,
        NutritionNutrient.PANTOTHENIC_ACID,
        NutritionNutrient.RIBOFLAVIN,
        NutritionNutrient.THIAMIN,
        NutritionNutrient.VITAMIN_B6,
        NutritionNutrient.VITAMIN_C,
        NutritionNutrient.VITAMIN_E,
        NutritionNutrient.CALCIUM,
        NutritionNutrient.CHLORIDE,
        NutritionNutrient.COPPER,
        NutritionNutrient.IRON,
        NutritionNutrient.MAGNESIUM,
        NutritionNutrient.MANGANESE,
        NutritionNutrient.PHOSPHORUS,
        NutritionNutrient.POTASSIUM,
        NutritionNutrient.SODIUM,
        NutritionNutrient.ZINC,
        NutritionNutrient.CAFFEINE -> NutrientLabelUnit.MILLIGRAM
        NutritionNutrient.BIOTIN,
        NutritionNutrient.FOLATE,
        NutritionNutrient.FOLIC_ACID,
        NutritionNutrient.VITAMIN_A,
        NutritionNutrient.VITAMIN_B12,
        NutritionNutrient.VITAMIN_D,
        NutritionNutrient.VITAMIN_K,
        NutritionNutrient.CHROMIUM,
        NutritionNutrient.IODINE,
        NutritionNutrient.MOLYBDENUM,
        NutritionNutrient.SELENIUM -> NutrientLabelUnit.MICROGRAM
    }
