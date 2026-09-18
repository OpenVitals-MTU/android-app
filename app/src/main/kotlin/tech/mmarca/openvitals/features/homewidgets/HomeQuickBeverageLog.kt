package tech.mmarca.openvitals.features.homewidgets

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import tech.mmarca.openvitals.data.repository.contract.HydrationRepository
import tech.mmarca.openvitals.data.repository.contract.NutritionRepository
import tech.mmarca.openvitals.domain.model.CustomHydrationDrink
import tech.mmarca.openvitals.features.manualentry.hydration.HydrationDrinkLogOutcome
import tech.mmarca.openvitals.features.manualentry.hydration.HydrationDrinkLogSuccess
import tech.mmarca.openvitals.features.manualentry.hydration.HydrationEntryError
import tech.mmarca.openvitals.features.manualentry.hydration.isValidCustomHydrationDrink
import tech.mmarca.openvitals.features.manualentry.hydration.logCustomHydrationDrinkEntry

/** What the tile says after a tap. */
internal enum class QuickBeverageStatus {
    NOT_CONFIGURED,
    SAVED,
    SAVED_NUTRITION,
    PERMISSION_NEEDED,
    FAILED,
    TAP_TO_LOG,
}

/** What one tap needs. Plain functions, so a JVM test can drive it. */
internal class QuickBeverageLogDeps(
    val hydrationRepository: HydrationRepository,
    val nutritionRepository: NutritionRepository,
    /** Runs after a successful write, for the reminder. */
    val onLogged: (HydrationDrinkLogSuccess) -> Unit,
    /** Redraws the tile. The drink is null when it no longer exists. */
    val showStatus: suspend (drink: CustomHydrationDrink?, status: QuickBeverageStatus) -> Unit,
)

/**
 * Runs one tap on [scope] and waits at most [budgetMillis] for the write.
 * The broadcast ends then; the write and the label revert finish on [scope].
 * The budget only stops the wait. Cancelling the write could split a
 * hydration and nutrition pair.
 */
internal suspend fun runQuickBeverageTap(
    drinkId: String,
    deps: QuickBeverageLogDeps,
    scope: CoroutineScope,
    budgetMillis: Long = QuickBeverageBroadcastBudgetMillis,
    revertDelayMillis: Long = SavedConfirmationDurationMillis,
) {
    val settled = CompletableDeferred<Unit>()
    scope.launch {
        val loggedDrink = try {
            logQuickBeverage(drinkId, deps)
        } finally {
            settled.complete(Unit)
        }
        if (loggedDrink != null) {
            // Briefly confirm the tap, then revert to the normal widget text.
            delay(revertDelayMillis)
            deps.showStatus(loggedDrink, QuickBeverageStatus.TAP_TO_LOG)
        }
    }
    withTimeoutOrNull(budgetMillis) { settled.await() }
}

/** Writes first; every tile update comes after. Returns the drink when it was logged. */
internal suspend fun logQuickBeverage(
    drinkId: String,
    deps: QuickBeverageLogDeps,
): CustomHydrationDrink? {
    val repository = deps.hydrationRepository
    var found: CustomHydrationDrink? = null
    try {
        val drink = repository.customHydrationDrinks()
            .firstOrNull { it.id == drinkId && it.isValidCustomHydrationDrink() }
        found = drink
        if (drink == null) {
            deps.showStatus(null, QuickBeverageStatus.NOT_CONFIGURED)
            return null
        }
        val outcome = logCustomHydrationDrinkEntry(
            repository = repository,
            nutritionRepository = deps.nutritionRepository,
            drink = drink,
            canWriteHydration = repository.hasHydrationWritePermission(),
            canWriteNutrition = deps.nutritionRepository.hasNutritionWritePermission(),
        )
        return when (outcome) {
            is HydrationDrinkLogOutcome.Invalid -> {
                deps.showStatus(drink, outcome.error.quickBeverageStatus())
                null
            }
            is HydrationDrinkLogOutcome.Success -> {
                // The write has landed. Nothing below may turn it into a failure.
                runCatching {
                    repository.setLastCustomHydrationAmountMilliliters(drink.volumeMilliliters)
                    repository.recordRecentHydrationAmountMilliliters(drink.volumeMilliliters)
                    deps.onLogged(outcome.value)
                }.onFailure { Log.w(HomeWidgetLogTag, "Quick beverage follow-up failed", it) }
                deps.showStatus(
                    drink,
                    if (outcome.value.wroteHydration) QuickBeverageStatus.SAVED else QuickBeverageStatus.SAVED_NUTRITION,
                )
                drink
            }
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (throwable: Throwable) {
        Log.e(HomeWidgetLogTag, "Quick beverage widget log failed", throwable)
        runCatching { deps.showStatus(found, QuickBeverageStatus.FAILED) }
        return null
    }
}

private fun HydrationEntryError.quickBeverageStatus(): QuickBeverageStatus =
    when (this) {
        HydrationEntryError.MISSING_WRITE_PERMISSION,
        HydrationEntryError.MISSING_NUTRITION_WRITE_PERMISSION -> QuickBeverageStatus.PERMISSION_NEEDED
        HydrationEntryError.INVALID_AMOUNT,
        HydrationEntryError.INVALID_CUSTOM_DRINK,
        HydrationEntryError.WRITE_FAILED -> QuickBeverageStatus.FAILED
    }

internal const val SavedConfirmationDurationMillis = 1_200L

// Under the 10 s foreground broadcast limit, with room for a cold start.
internal const val QuickBeverageBroadcastBudgetMillis = 8_000L
