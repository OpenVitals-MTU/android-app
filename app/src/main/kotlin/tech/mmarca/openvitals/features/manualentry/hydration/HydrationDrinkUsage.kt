package tech.mmarca.openvitals.features.manualentry.hydration

import java.time.Instant
import java.util.Locale
import tech.mmarca.openvitals.domain.model.CustomHydrationDrink
import tech.mmarca.openvitals.domain.model.HydrationEntry
import tech.mmarca.openvitals.domain.model.NutritionEntry

internal const val FrequentHydrationDrinkLimit = 6
internal const val FrequentHydrationDrinkLookbackDays = 90L

private const val OpenVitalsHydrationClientRecordPrefix = "openvitals_hydration_"
private const val OpenVitalsHydrationDrinkClientRecordMarker = "_drink_"
private const val OpenVitalsStandaloneNutritionPrefix = "openvitals_nutrition_"
private const val OpenVitalsPairedHydrationNutritionPrefix = "openvitals_hydration_nutrition_"

private data class HydrationDrinkUsageScore(
    val count: Int,
    val latestTime: Instant,
)

internal fun frequentHydrationDrinkOptions(
    drinks: List<CustomHydrationDrink>,
    hydrationEntries: List<HydrationEntry>,
    nutritionEntries: List<NutritionEntry>,
): List<CustomHydrationDrink> {
    if (drinks.isEmpty()) return emptyList()
    val drinkById = drinks.associateBy { drink -> drink.id }
    val drinkOrder = drinks.mapIndexed { index, drink -> drink.id to index }.toMap()
    val drinkIdByName = drinks.fold(mutableMapOf<String, String>()) { nameMap, drink ->
        nameMap.apply {
            putIfAbsent(drink.name.normalizedHydrationDrinkName(), drink.id)
        }
    }
    val scores = mutableMapOf<String, HydrationDrinkUsageScore>()
    val countedHydrationClientRecordIds = mutableSetOf<String>()

    fun increment(drinkId: String, time: Instant) {
        if (drinkId !in drinkById) return
        val current = scores[drinkId]
        scores[drinkId] = HydrationDrinkUsageScore(
            count = (current?.count ?: 0) + 1,
            latestTime = maxOf(current?.latestTime ?: Instant.EPOCH, time),
        )
    }

    hydrationEntries.forEach { entry ->
        if (!entry.isOpenVitalsHydrationEntry()) return@forEach
        val clientRecordId = entry.clientRecordId ?: return@forEach
        val drinkId = clientRecordId.hydrationDrinkIdFromClientRecordId() ?: return@forEach
        increment(drinkId, entry.startTime)
        countedHydrationClientRecordIds += clientRecordId
    }

    nutritionEntries.forEach { entry ->
        if (!entry.isOpenVitalsNutritionEntry()) return@forEach
        val pairedHydrationClientRecordId = entry.clientRecordId?.pairedHydrationClientRecordIdOrNull()
        val pairedDrinkId = pairedHydrationClientRecordId?.hydrationDrinkIdFromClientRecordId()
        if (pairedDrinkId != null) {
            if (pairedHydrationClientRecordId !in countedHydrationClientRecordIds) {
                increment(pairedDrinkId, entry.time)
            }
            return@forEach
        }
        val drinkId = entry.name
            ?.normalizedHydrationDrinkName()
            ?.takeIf { it.isNotBlank() }
            ?.let(drinkIdByName::get)
            ?: return@forEach
        increment(drinkId, entry.time)
    }

    return scores.keys
        .sortedWith(
            compareByDescending<String> { drinkId -> scores.getValue(drinkId).count }
                .thenByDescending { drinkId -> scores.getValue(drinkId).latestTime }
                .thenBy { drinkId -> drinkOrder[drinkId] ?: Int.MAX_VALUE }
        )
        .take(FrequentHydrationDrinkLimit)
        .mapNotNull(drinkById::get)
}

private fun HydrationEntry.isOpenVitalsHydrationEntry(): Boolean =
    isOpenVitalsEntry || clientRecordId?.startsWith(OpenVitalsHydrationClientRecordPrefix) == true

private fun NutritionEntry.isOpenVitalsNutritionEntry(): Boolean =
    isOpenVitalsEntry ||
        clientRecordId?.startsWith(OpenVitalsStandaloneNutritionPrefix) == true ||
        clientRecordId?.startsWith(OpenVitalsPairedHydrationNutritionPrefix) == true

private fun String.hydrationDrinkIdFromClientRecordId(): String? {
    if (!startsWith(OpenVitalsHydrationClientRecordPrefix)) return null
    val markerStart = indexOf(OpenVitalsHydrationDrinkClientRecordMarker)
        .takeIf { it >= 0 }
        ?: return null
    val drinkIdStart = markerStart + OpenVitalsHydrationDrinkClientRecordMarker.length
    val drinkIdEnd = indexOf('_', startIndex = drinkIdStart)
        .takeIf { it > drinkIdStart }
        ?: return null
    return substring(drinkIdStart, drinkIdEnd).takeIf { it.isNotBlank() }
}

private fun String.pairedHydrationClientRecordIdOrNull(): String? =
    takeIf { it.startsWith(OpenVitalsPairedHydrationNutritionPrefix) }
        ?.removePrefix(OpenVitalsPairedHydrationNutritionPrefix)

private fun String.normalizedHydrationDrinkName(): String =
    trim().lowercase(Locale.ROOT)
