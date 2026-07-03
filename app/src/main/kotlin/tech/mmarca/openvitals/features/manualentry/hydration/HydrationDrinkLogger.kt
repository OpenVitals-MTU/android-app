package tech.mmarca.openvitals.features.manualentry.hydration

import java.time.Instant
import tech.mmarca.openvitals.data.repository.contract.HydrationRepository
import tech.mmarca.openvitals.data.repository.contract.NutritionRepository
import tech.mmarca.openvitals.domain.model.CustomHydrationDrink
import tech.mmarca.openvitals.domain.model.HydrationWriteRequest
import tech.mmarca.openvitals.domain.model.NutritionNutrient
import tech.mmarca.openvitals.domain.model.NutritionWriteRequest

internal data class HydrationDrinkLogSuccess(
    val effectiveLiters: Double,
    val entryTime: Instant,
    val wroteHydration: Boolean,
    val wroteNutrition: Boolean,
    val notice: HydrationEntryNotice?,
)

internal sealed interface HydrationDrinkLogOutcome {
    data class Success(val value: HydrationDrinkLogSuccess) : HydrationDrinkLogOutcome
    data class Invalid(val error: HydrationEntryError) : HydrationDrinkLogOutcome
}

internal suspend fun logCustomHydrationDrinkEntry(
    repository: HydrationRepository,
    nutritionRepository: NutritionRepository,
    drink: CustomHydrationDrink,
    amountMilliliters: Double = drink.volumeMilliliters,
    requestedEntryTime: Instant? = null,
    canWriteHydration: Boolean? = null,
    canWriteNutrition: Boolean? = null,
): HydrationDrinkLogOutcome {
    if (!drink.isValidCustomHydrationDrink()) {
        return HydrationDrinkLogOutcome.Invalid(HydrationEntryError.INVALID_CUSTOM_DRINK)
    }
    if (!isValidHydrationContainerMilliliters(amountMilliliters)) {
        return HydrationDrinkLogOutcome.Invalid(HydrationEntryError.INVALID_AMOUNT)
    }
    val portionMultiplier = amountMilliliters / drink.volumeMilliliters
    return writeHydrationAndNutritionEntry(
        repository = repository,
        nutritionRepository = nutritionRepository,
        rawLiters = amountMilliliters / MillilitersPerLiter,
        hydrationMultiplier = drink.hydrationMultiplier,
        nutritionName = drink.name,
        nutrientValues = drink.nutrientValues.mapValues { (_, value) -> value * portionMultiplier },
        requestedEntryTime = requestedEntryTime,
        canWriteHydration = canWriteHydration ?: repository.hasHydrationWritePermission(),
        canWriteNutrition = canWriteNutrition ?: nutritionRepository.hasNutritionWritePermission(),
    )
}

internal suspend fun writeHydrationAndNutritionEntry(
    repository: HydrationRepository,
    nutritionRepository: NutritionRepository,
    rawLiters: Double,
    hydrationMultiplier: Double,
    nutritionName: String?,
    nutrientValues: Map<NutritionNutrient, Double>,
    requestedEntryTime: Instant? = null,
    fallbackEntryTime: Instant? = null,
    editRecordId: String? = null,
    canWriteHydration: Boolean? = null,
    canWriteNutrition: Boolean? = null,
): HydrationDrinkLogOutcome {
    if (!isValidCustomDrinkHydrationMultiplier(hydrationMultiplier)) {
        return HydrationDrinkLogOutcome.Invalid(HydrationEntryError.INVALID_CUSTOM_DRINK)
    }

    val effectiveLiters = rawLiters * hydrationMultiplier
    val writesHydration = effectiveLiters > 0.0
    val writesNutrition = nutrientValues.isNotEmpty()
    val hasHydrationPermission = canWriteHydration ?: repository.hasHydrationWritePermission()
    val hasNutritionPermission = canWriteNutrition ?: nutritionRepository.hasNutritionWritePermission()
    if (editRecordId != null && !writesHydration) {
        return HydrationDrinkLogOutcome.Invalid(HydrationEntryError.INVALID_AMOUNT)
    }
    if (writesHydration && !hasHydrationPermission) {
        return HydrationDrinkLogOutcome.Invalid(HydrationEntryError.MISSING_WRITE_PERMISSION)
    }
    if (writesNutrition && !hasNutritionPermission) {
        return HydrationDrinkLogOutcome.Invalid(HydrationEntryError.MISSING_NUTRITION_WRITE_PERMISSION)
    }
    if (writesHydration && effectiveLiters > MaxHydrationContainerMilliliters / MillilitersPerLiter) {
        return HydrationDrinkLogOutcome.Invalid(HydrationEntryError.INVALID_AMOUNT)
    }
    if (!writesHydration && !writesNutrition) {
        return HydrationDrinkLogOutcome.Invalid(HydrationEntryError.INVALID_CUSTOM_DRINK)
    }

    val now = Instant.now()
    val entryTime = requestedEntryTime?.coerceAtMost(now)
        ?: fallbackEntryTime?.coerceAtMost(now)
        ?: now
    if (editRecordId == null) {
        val hydrationClientRecordId = if (writesHydration) {
            repository.writeHydrationEntry(
                HydrationWriteRequest(
                    time = entryTime,
                    volumeLiters = effectiveLiters,
                )
            )
        } else {
            null
        }
        if (writesNutrition) {
            nutritionRepository.writeNutritionEntry(
                NutritionWriteRequest(
                    time = entryTime,
                    nutrientValues = nutrientValues,
                    name = nutritionName,
                    associatedHydrationClientRecordId = hydrationClientRecordId,
                )
            )
        }
    } else {
        repository.updateHydrationEntry(
            editRecordId,
            HydrationWriteRequest(
                time = entryTime,
                volumeLiters = effectiveLiters,
            )
        )
    }

    return HydrationDrinkLogOutcome.Success(
        HydrationDrinkLogSuccess(
            effectiveLiters = effectiveLiters,
            entryTime = entryTime,
            wroteHydration = writesHydration,
            wroteNutrition = writesNutrition,
            notice = if (!writesHydration && writesNutrition) {
                HydrationEntryNotice.NON_HYDRATING_DRINK_SAVED
            } else {
                null
            },
        )
    )
}

internal fun CustomHydrationDrink.isValidCustomHydrationDrink(): Boolean =
    id.isNotBlank() &&
        name.isNotBlank() &&
        isValidHydrationContainerMilliliters(volumeMilliliters) &&
        isValidCustomDrinkHydrationMultiplier(hydrationMultiplier) &&
        nutrientValues.values.all(::isValidCustomDrinkNutrientValue)
