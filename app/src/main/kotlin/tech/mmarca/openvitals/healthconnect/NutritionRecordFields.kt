package tech.mmarca.openvitals.healthconnect

import androidx.health.connect.client.records.MealType
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Mass
import java.time.Instant
import java.time.ZoneOffset
import tech.mmarca.openvitals.domain.model.NutritionNutrient
import tech.mmarca.openvitals.domain.model.NutritionNutrientUnit

// Each nutrient's Health Connect field, both ways. The nutrition form and the CSV importer
// write through the same mapping, so a nutrient cannot land in one field from one and
// another from the other.

/** A nutrition record carrying [nutrientValues]: kcal for energy, grams for the rest. */
internal fun nutritionRecordOf(
    startTime: Instant,
    startZoneOffset: ZoneOffset?,
    endTime: Instant,
    endZoneOffset: ZoneOffset?,
    metadata: Metadata,
    nutrientValues: Map<NutritionNutrient, Double>,
    name: String?,
    mealType: Int = MealType.MEAL_TYPE_UNKNOWN,
): NutritionRecord = NutritionRecord(
    startTime = startTime,
    startZoneOffset = startZoneOffset,
    endTime = endTime,
    endZoneOffset = endZoneOffset,
    metadata = metadata,
    energy = nutrientValues.energy(NutritionNutrient.ENERGY),
    energyFromFat = nutrientValues.energy(NutritionNutrient.ENERGY_FROM_FAT),
    biotin = nutrientValues.mass(NutritionNutrient.BIOTIN),
    caffeine = nutrientValues.mass(NutritionNutrient.CAFFEINE),
    calcium = nutrientValues.mass(NutritionNutrient.CALCIUM),
    chloride = nutrientValues.mass(NutritionNutrient.CHLORIDE),
    cholesterol = nutrientValues.mass(NutritionNutrient.CHOLESTEROL),
    chromium = nutrientValues.mass(NutritionNutrient.CHROMIUM),
    copper = nutrientValues.mass(NutritionNutrient.COPPER),
    dietaryFiber = nutrientValues.mass(NutritionNutrient.DIETARY_FIBER),
    folate = nutrientValues.mass(NutritionNutrient.FOLATE),
    folicAcid = nutrientValues.mass(NutritionNutrient.FOLIC_ACID),
    iodine = nutrientValues.mass(NutritionNutrient.IODINE),
    iron = nutrientValues.mass(NutritionNutrient.IRON),
    magnesium = nutrientValues.mass(NutritionNutrient.MAGNESIUM),
    manganese = nutrientValues.mass(NutritionNutrient.MANGANESE),
    molybdenum = nutrientValues.mass(NutritionNutrient.MOLYBDENUM),
    monounsaturatedFat = nutrientValues.mass(NutritionNutrient.MONOUNSATURATED_FAT),
    niacin = nutrientValues.mass(NutritionNutrient.NIACIN),
    pantothenicAcid = nutrientValues.mass(NutritionNutrient.PANTOTHENIC_ACID),
    phosphorus = nutrientValues.mass(NutritionNutrient.PHOSPHORUS),
    polyunsaturatedFat = nutrientValues.mass(NutritionNutrient.POLYUNSATURATED_FAT),
    potassium = nutrientValues.mass(NutritionNutrient.POTASSIUM),
    protein = nutrientValues.mass(NutritionNutrient.PROTEIN),
    riboflavin = nutrientValues.mass(NutritionNutrient.RIBOFLAVIN),
    saturatedFat = nutrientValues.mass(NutritionNutrient.SATURATED_FAT),
    selenium = nutrientValues.mass(NutritionNutrient.SELENIUM),
    sodium = nutrientValues.mass(NutritionNutrient.SODIUM),
    sugar = nutrientValues.mass(NutritionNutrient.SUGAR),
    thiamin = nutrientValues.mass(NutritionNutrient.THIAMIN),
    totalCarbohydrate = nutrientValues.mass(NutritionNutrient.TOTAL_CARBOHYDRATE),
    totalFat = nutrientValues.mass(NutritionNutrient.TOTAL_FAT),
    transFat = nutrientValues.mass(NutritionNutrient.TRANS_FAT),
    unsaturatedFat = nutrientValues.mass(NutritionNutrient.UNSATURATED_FAT),
    vitaminA = nutrientValues.mass(NutritionNutrient.VITAMIN_A),
    vitaminB12 = nutrientValues.mass(NutritionNutrient.VITAMIN_B12),
    vitaminB6 = nutrientValues.mass(NutritionNutrient.VITAMIN_B6),
    vitaminC = nutrientValues.mass(NutritionNutrient.VITAMIN_C),
    vitaminD = nutrientValues.mass(NutritionNutrient.VITAMIN_D),
    vitaminE = nutrientValues.mass(NutritionNutrient.VITAMIN_E),
    vitaminK = nutrientValues.mass(NutritionNutrient.VITAMIN_K),
    zinc = nutrientValues.mass(NutritionNutrient.ZINC),
    name = name,
    mealType = mealType,
)

/** The record's nutrients above zero: kcal for energy, grams for the rest. */
internal fun NutritionRecord.nutritionNutrientValues(): Map<NutritionNutrient, Double> = buildMap {
    putIfPositive(NutritionNutrient.ENERGY, energy?.inKilocalories)
    putIfPositive(NutritionNutrient.ENERGY_FROM_FAT, energyFromFat?.inKilocalories)
    putIfPositive(NutritionNutrient.BIOTIN, biotin?.inGrams)
    putIfPositive(NutritionNutrient.CAFFEINE, caffeine?.inGrams)
    putIfPositive(NutritionNutrient.CALCIUM, calcium?.inGrams)
    putIfPositive(NutritionNutrient.CHLORIDE, chloride?.inGrams)
    putIfPositive(NutritionNutrient.CHOLESTEROL, cholesterol?.inGrams)
    putIfPositive(NutritionNutrient.CHROMIUM, chromium?.inGrams)
    putIfPositive(NutritionNutrient.COPPER, copper?.inGrams)
    putIfPositive(NutritionNutrient.DIETARY_FIBER, dietaryFiber?.inGrams)
    putIfPositive(NutritionNutrient.FOLATE, folate?.inGrams)
    putIfPositive(NutritionNutrient.FOLIC_ACID, folicAcid?.inGrams)
    putIfPositive(NutritionNutrient.IODINE, iodine?.inGrams)
    putIfPositive(NutritionNutrient.IRON, iron?.inGrams)
    putIfPositive(NutritionNutrient.MAGNESIUM, magnesium?.inGrams)
    putIfPositive(NutritionNutrient.MANGANESE, manganese?.inGrams)
    putIfPositive(NutritionNutrient.MOLYBDENUM, molybdenum?.inGrams)
    putIfPositive(NutritionNutrient.MONOUNSATURATED_FAT, monounsaturatedFat?.inGrams)
    putIfPositive(NutritionNutrient.NIACIN, niacin?.inGrams)
    putIfPositive(NutritionNutrient.PANTOTHENIC_ACID, pantothenicAcid?.inGrams)
    putIfPositive(NutritionNutrient.PHOSPHORUS, phosphorus?.inGrams)
    putIfPositive(NutritionNutrient.POLYUNSATURATED_FAT, polyunsaturatedFat?.inGrams)
    putIfPositive(NutritionNutrient.POTASSIUM, potassium?.inGrams)
    putIfPositive(NutritionNutrient.PROTEIN, protein?.inGrams)
    putIfPositive(NutritionNutrient.RIBOFLAVIN, riboflavin?.inGrams)
    putIfPositive(NutritionNutrient.SATURATED_FAT, saturatedFat?.inGrams)
    putIfPositive(NutritionNutrient.SELENIUM, selenium?.inGrams)
    putIfPositive(NutritionNutrient.SODIUM, sodium?.inGrams)
    putIfPositive(NutritionNutrient.SUGAR, sugar?.inGrams)
    putIfPositive(NutritionNutrient.THIAMIN, thiamin?.inGrams)
    putIfPositive(NutritionNutrient.TOTAL_CARBOHYDRATE, totalCarbohydrate?.inGrams)
    putIfPositive(NutritionNutrient.TOTAL_FAT, totalFat?.inGrams)
    putIfPositive(NutritionNutrient.TRANS_FAT, transFat?.inGrams)
    putIfPositive(NutritionNutrient.UNSATURATED_FAT, unsaturatedFat?.inGrams)
    putIfPositive(NutritionNutrient.VITAMIN_A, vitaminA?.inGrams)
    putIfPositive(NutritionNutrient.VITAMIN_B12, vitaminB12?.inGrams)
    putIfPositive(NutritionNutrient.VITAMIN_B6, vitaminB6?.inGrams)
    putIfPositive(NutritionNutrient.VITAMIN_C, vitaminC?.inGrams)
    putIfPositive(NutritionNutrient.VITAMIN_D, vitaminD?.inGrams)
    putIfPositive(NutritionNutrient.VITAMIN_E, vitaminE?.inGrams)
    putIfPositive(NutritionNutrient.VITAMIN_K, vitaminK?.inGrams)
    putIfPositive(NutritionNutrient.ZINC, zinc?.inGrams)
}

private fun MutableMap<NutritionNutrient, Double>.putIfPositive(
    nutrient: NutritionNutrient,
    value: Double?,
) {
    if (value != null && value > 0.0) {
        put(nutrient, value)
    }
}

private fun Map<NutritionNutrient, Double>.energy(nutrient: NutritionNutrient): Energy? {
    require(nutrient.unit == NutritionNutrientUnit.ENERGY_KCAL) {
        "${nutrient.name} is not a Health Connect energy nutrient."
    }
    return this[nutrient]?.let(Energy::kilocalories)
}

private fun Map<NutritionNutrient, Double>.mass(nutrient: NutritionNutrient): Mass? {
    require(nutrient.unit != NutritionNutrientUnit.ENERGY_KCAL) {
        "${nutrient.name} is not a Health Connect mass nutrient."
    }
    return this[nutrient]?.let(Mass::grams)
}
