package tech.mmarca.openvitals.features.imports.applehealth

import androidx.health.connect.client.records.MealType
import androidx.health.connect.client.records.NutritionRecord

internal fun AppleHealthImportConverter.convertNutrition(
    records: List<AppleRecord>,
    trackConsumedRecords: Boolean = true,
): List<ConvertedAppleRecord> {
    val nutritionRecords = records.filter { it.type in AppleNutritionTypes && it.type != AppleDietaryWater }
    if (nutritionRecords.isEmpty()) return emptyList()

    val grouped = nutritionRecords.groupBy { record ->
        listOf(
            record.sourceName.orEmpty(),
            record.startDate?.instant?.toString().orEmpty(),
            record.endDate?.instant?.toString().orEmpty(),
            record.metadata["HKMetadataKeyFoodType"].orEmpty(),
        ).joinToString("|")
    }

    return grouped.values.mapNotNull { group ->
        val start = group.mapNotNull { it.startDate }.minByOrNull { it.instant }
        val end = group.mapNotNull { it.endDate ?: it.startDate }.maxByOrNull { it.instant }
        if (start == null) {
            group.forEach { invalid(it, "Nutrition record is missing startDate.") }
            return@mapNotNull null
        }
        val interval = interval(start, end ?: start)
        val nutrients = NutritionValues()
        group.forEach { record ->
            val value = record.numericValue
            val applied = value != null && nutrients.apply(record.type, value, record.unit)
            if (applied) {
                if (trackConsumedRecords) {
                    consumedRecordFingerprints += record.sourceFingerprint
                }
            } else {
                invalid(record, "Nutrition value is missing or has an unsupported unit.")
            }
        }
        if (!nutrients.hasAny) return@mapNotNull null
        val fingerprint = buildStableClientRecordId("nutrition", group.map { it.stableParts() })
        markConverted(AppleNutritionSyntheticType)
        ConvertedAppleRecord(
            appleType = AppleNutritionSyntheticType,
            targetType = "NutritionRecord",
            fingerprint = fingerprint,
            recordType = NutritionRecord::class,
            record = NutritionRecord(
                startTime = interval.start.instant,
                startZoneOffset = interval.start.offset,
                endTime = interval.end.instant,
                endZoneOffset = interval.end.offset,
                metadata = appleMetadata("NutritionRecord", fingerprint),
                biotin = nutrients.biotin,
                caffeine = nutrients.caffeine,
                calcium = nutrients.calcium,
                energy = nutrients.energy,
                energyFromFat = nutrients.energyFromFat,
                cholesterol = nutrients.cholesterol,
                chromium = nutrients.chromium,
                copper = nutrients.copper,
                dietaryFiber = nutrients.dietaryFiber,
                folate = nutrients.folate,
                iodine = nutrients.iodine,
                iron = nutrients.iron,
                magnesium = nutrients.magnesium,
                manganese = nutrients.manganese,
                molybdenum = nutrients.molybdenum,
                monounsaturatedFat = nutrients.monounsaturatedFat,
                niacin = nutrients.niacin,
                pantothenicAcid = nutrients.pantothenicAcid,
                phosphorus = nutrients.phosphorus,
                polyunsaturatedFat = nutrients.polyunsaturatedFat,
                potassium = nutrients.potassium,
                protein = nutrients.protein,
                riboflavin = nutrients.riboflavin,
                saturatedFat = nutrients.saturatedFat,
                selenium = nutrients.selenium,
                sodium = nutrients.sodium,
                sugar = nutrients.sugar,
                thiamin = nutrients.thiamin,
                totalCarbohydrate = nutrients.totalCarbohydrate,
                totalFat = nutrients.totalFat,
                transFat = nutrients.transFat,
                vitaminA = nutrients.vitaminA,
                vitaminB12 = nutrients.vitaminB12,
                vitaminB6 = nutrients.vitaminB6,
                vitaminC = nutrients.vitaminC,
                vitaminD = nutrients.vitaminD,
                vitaminE = nutrients.vitaminE,
                vitaminK = nutrients.vitaminK,
                zinc = nutrients.zinc,
                name = group.firstNotNullOfOrNull { it.metadata["HKMetadataKeyFoodType"] },
                mealType = MealType.MEAL_TYPE_UNKNOWN,
            ),
            sourceTimeRange = AppleImportTimeRange(interval.start.instant, interval.end.instant),
            unit = null,
            value = "nutrients=${group.size}",
        )
    }
}
