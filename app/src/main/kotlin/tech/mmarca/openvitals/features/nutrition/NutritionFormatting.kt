package tech.mmarca.openvitals.features.nutrition

import androidx.compose.ui.graphics.Color
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.NutritionNutrient
import tech.mmarca.openvitals.domain.model.NutritionNutrientGroup
import tech.mmarca.openvitals.domain.model.NutritionNutrientUnit
import tech.mmarca.openvitals.ui.components.PeriodChartValue
import tech.mmarca.openvitals.ui.theme.NutritionColor
import kotlin.math.roundToInt

private val proteinMetricColor = Color(0xFF7E57C2)
private val carbsMetricColor = Color(0xFF26A69A)
private val fatMetricColor = Color(0xFFFFB300)

internal data class NutritionSeriesUiModel(
    val nutrient: NutritionNutrient,
    val titleRes: Int,
    val total: DisplayValue,
    val totalValue: Double,
    val values: List<PeriodChartValue>,
    val color: Color,
    val valueDisplayFormatter: (Double) -> DisplayValue,
) {
    val hasTrackedValues: Boolean = values.any { it.value > 0.0 }
}

internal fun NutritionNutrientSeries.toUiModel(unitFormatter: UnitFormatter): NutritionSeriesUiModel =
    NutritionSeriesUiModel(
        nutrient = nutrient,
        titleRes = nutrient.titleRes(),
        total = nutrient.displayValue(totalValue, unitFormatter),
        totalValue = totalValue,
        values = values.map { PeriodChartValue(date = it.date, value = it.value) },
        color = nutrient.color(),
        valueDisplayFormatter = { nutrient.displayValue(it, unitFormatter) },
    )

internal fun NutritionMetricDisplay.toUiModel(unitFormatter: UnitFormatter): NutritionSeriesUiModel =
    NutritionSeriesUiModel(
        nutrient = nutrient,
        titleRes = nutrient.titleRes(),
        total = nutrient.displayValue(totalValue, unitFormatter),
        totalValue = totalValue,
        values = values.map { PeriodChartValue(date = it.date, value = it.value) },
        color = nutrient.color(),
        valueDisplayFormatter = { nutrient.displayValue(it, unitFormatter) },
    )

internal fun NutritionNutrient.displayValue(
    value: Double,
    unitFormatter: UnitFormatter,
): DisplayValue =
    when (unit) {
        NutritionNutrientUnit.ENERGY_KCAL -> unitFormatter.energy(value)
        NutritionNutrientUnit.MASS_GRAMS -> DisplayValue(unitFormatter.count(value.roundToInt()), "g")
        NutritionNutrientUnit.MASS_ADAPTIVE -> adaptiveMassDisplay(value, unitFormatter)
    }

private fun adaptiveMassDisplay(
    grams: Double,
    unitFormatter: UnitFormatter,
): DisplayValue {
    val milligrams = grams * 1_000.0
    val micrograms = grams * 1_000_000.0
    return when {
        grams >= 1.0 -> DisplayValue(unitFormatter.decimal(grams, if (grams < 10.0) 1 else 0), "g")
        milligrams >= 1.0 -> DisplayValue(unitFormatter.decimal(milligrams, if (milligrams < 10.0) 1 else 0), "mg")
        else -> DisplayValue(unitFormatter.decimal(micrograms, if (micrograms < 10.0) 1 else 0), "mcg")
    }
}

internal fun NutritionNutrient.color(): Color =
    when (group) {
        NutritionNutrientGroup.OVERVIEW -> when (this) {
            NutritionNutrient.ENERGY -> NutritionColor
            NutritionNutrient.PROTEIN -> proteinMetricColor
            NutritionNutrient.TOTAL_CARBOHYDRATE -> carbsMetricColor
            NutritionNutrient.TOTAL_FAT -> fatMetricColor
            else -> NutritionColor
        }
        NutritionNutrientGroup.CARBOHYDRATES -> carbsMetricColor
        NutritionNutrientGroup.FATS -> fatMetricColor
        NutritionNutrientGroup.VITAMINS -> Color(0xFF5E7CE2)
        NutritionNutrientGroup.MINERALS -> Color(0xFF8D6E63)
        NutritionNutrientGroup.OTHER -> Color(0xFF00897B)
    }

internal fun NutritionNutrientGroup.titleRes(): Int =
    when (this) {
        NutritionNutrientGroup.OVERVIEW -> R.string.screen_nutrition
        NutritionNutrientGroup.CARBOHYDRATES -> R.string.section_carbohydrates
        NutritionNutrientGroup.FATS -> R.string.section_fats
        NutritionNutrientGroup.VITAMINS -> R.string.section_vitamins
        NutritionNutrientGroup.MINERALS -> R.string.section_minerals
        NutritionNutrientGroup.OTHER -> R.string.section_other_nutrients
    }

internal fun NutritionNutrient.titleRes(): Int =
    when (this) {
        NutritionNutrient.ENERGY -> R.string.metric_calories_in
        NutritionNutrient.PROTEIN -> R.string.metric_protein
        NutritionNutrient.TOTAL_CARBOHYDRATE -> R.string.metric_carbs
        NutritionNutrient.TOTAL_FAT -> R.string.metric_fat
        NutritionNutrient.DIETARY_FIBER -> R.string.metric_dietary_fiber
        NutritionNutrient.SUGAR -> R.string.metric_sugar
        NutritionNutrient.ENERGY_FROM_FAT -> R.string.metric_energy_from_fat
        NutritionNutrient.MONOUNSATURATED_FAT -> R.string.metric_monounsaturated_fat
        NutritionNutrient.POLYUNSATURATED_FAT -> R.string.metric_polyunsaturated_fat
        NutritionNutrient.SATURATED_FAT -> R.string.metric_saturated_fat
        NutritionNutrient.TRANS_FAT -> R.string.metric_trans_fat
        NutritionNutrient.UNSATURATED_FAT -> R.string.metric_unsaturated_fat
        NutritionNutrient.CHOLESTEROL -> R.string.metric_cholesterol
        NutritionNutrient.BIOTIN -> R.string.metric_biotin
        NutritionNutrient.FOLATE -> R.string.metric_folate
        NutritionNutrient.FOLIC_ACID -> R.string.metric_folic_acid
        NutritionNutrient.NIACIN -> R.string.metric_niacin
        NutritionNutrient.PANTOTHENIC_ACID -> R.string.metric_pantothenic_acid
        NutritionNutrient.RIBOFLAVIN -> R.string.metric_riboflavin
        NutritionNutrient.THIAMIN -> R.string.metric_thiamin
        NutritionNutrient.VITAMIN_A -> R.string.metric_vitamin_a
        NutritionNutrient.VITAMIN_B12 -> R.string.metric_vitamin_b12
        NutritionNutrient.VITAMIN_B6 -> R.string.metric_vitamin_b6
        NutritionNutrient.VITAMIN_C -> R.string.metric_vitamin_c
        NutritionNutrient.VITAMIN_D -> R.string.metric_vitamin_d
        NutritionNutrient.VITAMIN_E -> R.string.metric_vitamin_e
        NutritionNutrient.VITAMIN_K -> R.string.metric_vitamin_k
        NutritionNutrient.CALCIUM -> R.string.metric_calcium
        NutritionNutrient.CHLORIDE -> R.string.metric_chloride
        NutritionNutrient.CHROMIUM -> R.string.metric_chromium
        NutritionNutrient.COPPER -> R.string.metric_copper
        NutritionNutrient.IODINE -> R.string.metric_iodine
        NutritionNutrient.IRON -> R.string.metric_iron
        NutritionNutrient.MAGNESIUM -> R.string.metric_magnesium
        NutritionNutrient.MANGANESE -> R.string.metric_manganese
        NutritionNutrient.MOLYBDENUM -> R.string.metric_molybdenum
        NutritionNutrient.PHOSPHORUS -> R.string.metric_phosphorus
        NutritionNutrient.POTASSIUM -> R.string.metric_potassium
        NutritionNutrient.SELENIUM -> R.string.metric_selenium
        NutritionNutrient.SODIUM -> R.string.metric_sodium
        NutritionNutrient.ZINC -> R.string.metric_zinc
        NutritionNutrient.CAFFEINE -> R.string.metric_caffeine
    }
