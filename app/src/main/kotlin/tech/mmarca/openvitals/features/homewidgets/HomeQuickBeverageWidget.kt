package tech.mmarca.openvitals.features.homewidgets

import android.annotation.SuppressLint
import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.core.content.edit
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.AppWidgetId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.data.repository.contract.HydrationRepository
import tech.mmarca.openvitals.data.repository.contract.NutritionRepository
import tech.mmarca.openvitals.domain.model.CustomHydrationDrink
import tech.mmarca.openvitals.features.hydration.reminders.HydrationReminderController
import tech.mmarca.openvitals.features.manualentry.hydration.HydrationDrinkLogOutcome
import tech.mmarca.openvitals.features.manualentry.hydration.HydrationEntryError
import tech.mmarca.openvitals.features.manualentry.hydration.hydrationAmountLabel
import tech.mmarca.openvitals.features.manualentry.hydration.isValidCustomHydrationDrink
import tech.mmarca.openvitals.features.manualentry.hydration.logCustomHydrationDrinkEntry
import tech.mmarca.openvitals.navigation.Screen

class HomeQuickBeverageWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<Preferences> = HomeQuickBeverageWidgetState.definition

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(
            DpSize(180.dp, 88.dp),
            DpSize(240.dp, 110.dp),
            DpSize(320.dp, 140.dp),
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val preferences = currentState<Preferences>()
            val snapshot = preferences.toQuickBeverageSnapshot(context)
                ?: HomeQuickBeverageSnapshot(
                    drinkId = "",
                    title = context.getString(R.string.home_quick_beverage_widget_config_title),
                    amount = "--",
                    subtitle = context.getString(R.string.home_quick_beverage_widget_not_configured),
                    route = Screen.HydrationEntry.route,
                )
            HomeQuickBeverageWidgetContent(snapshot = snapshot)
        }
    }

    override suspend fun onDelete(context: Context, glanceId: GlanceId) {
        homeQuickBeverageWidgetSelection(context).clearDrink(glanceId)
        super.onDelete(context, glanceId)
    }
}

class HomeQuickBeverageWidgetReceiver : UpdatingHomeWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HomeQuickBeverageWidget()

    override suspend fun refreshWidget(context: Context, appWidgetId: Int) {
        refreshHomeQuickBeverageWidget(context, appWidgetId)
    }
}

class HomeQuickBeverageLogAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val drinkId = parameters[QuickBeverageDrinkIdParameterKey]
            ?: homeQuickBeverageWidgetSelection(context).drinkIdFor(glanceId)
            ?: getAppWidgetState(context, HomeQuickBeverageWidgetState.definition, glanceId)[HomeQuickBeverageWidgetState.drinkIdKey]
            ?: return
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            HomeQuickBeverageWidgetEntryPoint::class.java,
        )
        val repository = entryPoint.hydrationRepository()
        val drink = repository.customHydrationDrinks()
            .firstOrNull { it.id == drinkId && it.isValidCustomHydrationDrink() }
        if (drink == null) {
            updateQuickBeverageWidgetStatus(
                context = context,
                glanceId = glanceId,
                drinkId = drinkId,
                subtitle = context.getString(R.string.home_quick_beverage_widget_not_configured),
            )
            return
        }

        runCatching {
            repository.setLastCustomHydrationAmountMilliliters(drink.volumeMilliliters)
            logCustomHydrationDrinkEntry(
                repository = repository,
                nutritionRepository = entryPoint.nutritionRepository(),
                drink = drink,
                canWriteHydration = repository.hasHydrationWritePermission(),
                canWriteNutrition = entryPoint.nutritionRepository().hasNutritionWritePermission(),
            )
        }.onSuccess { outcome ->
            when (outcome) {
                is HydrationDrinkLogOutcome.Invalid -> {
                    updateQuickBeverageWidgetStatus(
                        context = context,
                        glanceId = glanceId,
                        drinkId = drinkId,
                        subtitle = outcome.error.quickBeverageWidgetMessage(context),
                    )
                }
                is HydrationDrinkLogOutcome.Success -> {
                    if (outcome.value.effectiveLiters > 0.0) {
                        runCatching { entryPoint.hydrationReminderController().hideReminderNotification() }
                    }
                    updateQuickBeverageWidgetStatus(
                        context = context,
                        glanceId = glanceId,
                        drinkId = drinkId,
                        subtitle = context.getString(
                            if (outcome.value.wroteHydration) {
                                R.string.home_quick_beverage_widget_saved
                            } else {
                                R.string.home_quick_beverage_widget_saved_nutrition
                            }
                        ),
                    )
                }
            }
        }.onFailure { throwable ->
            Log.e(HomeWidgetLogTag, "Quick beverage widget log failed", throwable)
            updateQuickBeverageWidgetStatus(
                context = context,
                glanceId = glanceId,
                drinkId = drinkId,
                subtitle = context.getString(R.string.home_metric_widget_update_failed),
            )
        }
    }
}

object HomeQuickBeverageWidgetState {
    const val drinkIdOptionKey = "quick_beverage_drink_id"
    val drinkIdKey = stringPreferencesKey("quick_beverage_drink_id")
    val titleKey = stringPreferencesKey("quick_beverage_title")
    val amountKey = stringPreferencesKey("quick_beverage_amount")
    val subtitleKey = stringPreferencesKey("quick_beverage_subtitle")
    val routeKey = stringPreferencesKey("quick_beverage_route")
    val definition = androidx.glance.state.PreferencesGlanceStateDefinition
}

class HomeQuickBeverageWidgetSelection(private val context: Context) {
    private val appWidgetManager = AppWidgetManager.getInstance(context.applicationContext)
    private val preferences = context.applicationContext.getSharedPreferences(
        "home_quick_beverage_widgets",
        Context.MODE_PRIVATE,
    )

    fun drinkIdFor(glanceId: GlanceId): String? =
        glanceId.appWidgetIdOrNull()
            ?.let(::drinkIdForAppWidgetId)

    private fun drinkIdForAppWidgetId(appWidgetId: Int): String? {
        val optionDrinkId = appWidgetManager.getAppWidgetOptions(appWidgetId)
            ?.getString(HomeQuickBeverageWidgetState.drinkIdOptionKey)
            ?.takeIf(String::isNotBlank)
        val sharedDrinkId = preferences.getString(drinkKey(appWidgetId), null)
            ?.takeIf(String::isNotBlank)
            ?.also { drinkId -> updateDrinkOptions(appWidgetId, drinkId) }
        val pendingDrinkId = if (optionDrinkId == null && sharedDrinkId == null) {
            adoptPendingDrink(appWidgetId)
        } else {
            null
        }
        return optionDrinkId ?: sharedDrinkId ?: pendingDrinkId
    }

    @SuppressLint("ApplySharedPref")
    fun setDrink(appWidgetId: Int, drinkId: String) {
        preferences.edit(commit = true) {
            putString(drinkKey(appWidgetId), drinkId)
            putString(PendingDrinkIdKey, drinkId)
            putLong(PendingDrinkTimestampKey, System.currentTimeMillis())
        }
    }

    fun clearDrink(glanceId: GlanceId) {
        val appWidgetId = glanceId.appWidgetIdOrNull() ?: return
        preferences.edit {
            remove(drinkKey(appWidgetId))
        }
        val options = Bundle(appWidgetManager.getAppWidgetOptions(appWidgetId))
        options.remove(HomeQuickBeverageWidgetState.drinkIdOptionKey)
        appWidgetManager.updateAppWidgetOptions(appWidgetId, options)
    }

    private fun drinkKey(appWidgetId: Int): String = "drink_id_$appWidgetId"

    private fun adoptPendingDrink(appWidgetId: Int): String? {
        val timestamp = preferences.getLong(PendingDrinkTimestampKey, 0L)
        if (System.currentTimeMillis() - timestamp > PendingDrinkTtlMillis) return null
        val drinkId = preferences.getString(PendingDrinkIdKey, null)?.takeIf(String::isNotBlank) ?: return null

        preferences.edit {
            putString(drinkKey(appWidgetId), drinkId)
        }
        updateDrinkOptions(appWidgetId, drinkId)

        return drinkId
    }

    private fun updateDrinkOptions(appWidgetId: Int, drinkId: String) {
        val options = Bundle(appWidgetManager.getAppWidgetOptions(appWidgetId))
        options.putString(HomeQuickBeverageWidgetState.drinkIdOptionKey, drinkId)
        appWidgetManager.updateAppWidgetOptions(appWidgetId, options)
    }

    private companion object {
        const val PendingDrinkIdKey = "pending_drink_id"
        const val PendingDrinkTimestampKey = "pending_drink_timestamp"
        const val PendingDrinkTtlMillis = 2 * 60 * 1_000L
    }
}

fun homeQuickBeverageWidgetSelection(context: Context): HomeQuickBeverageWidgetSelection =
    HomeQuickBeverageWidgetSelection(context)

data class HomeQuickBeverageSnapshot(
    val drinkId: String,
    val title: String,
    val amount: String,
    val subtitle: String,
    val route: String,
)

suspend fun refreshHomeQuickBeverageWidget(
    context: Context,
    appWidgetId: Int,
    drinkId: String? = null,
) {
    if (!hasAppWidgetInfo(context, appWidgetId)) return

    val glanceId = glanceAppWidgetId(appWidgetId)
    val stateDrinkId = getAppWidgetState(context, HomeQuickBeverageWidgetState.definition, glanceId)[HomeQuickBeverageWidgetState.drinkIdKey]
    val resolvedDrinkId = drinkId
        ?: homeQuickBeverageWidgetSelection(context).drinkIdFor(glanceId)
        ?: stateDrinkId

    if (resolvedDrinkId == null) {
        HomeQuickBeverageWidget().update(context, glanceId)
        return
    }

    val snapshot = loadQuickBeverageSnapshot(context, resolvedDrinkId)
    writeQuickBeverageWidgetSnapshot(context, glanceId, snapshot)
    HomeQuickBeverageWidget().update(context, glanceId)
}

internal suspend fun writeQuickBeverageWidgetSnapshot(
    context: Context,
    glanceId: GlanceId,
    snapshot: HomeQuickBeverageSnapshot,
) {
    updateAppWidgetState(
        context = context,
        glanceId = glanceId,
    ) { preferences ->
        preferences[HomeQuickBeverageWidgetState.drinkIdKey] = snapshot.drinkId
        preferences[HomeQuickBeverageWidgetState.titleKey] = snapshot.title
        preferences[HomeQuickBeverageWidgetState.amountKey] = snapshot.amount
        preferences[HomeQuickBeverageWidgetState.subtitleKey] = snapshot.subtitle
        preferences[HomeQuickBeverageWidgetState.routeKey] = snapshot.route
    }
}

internal fun Preferences.toQuickBeverageSnapshot(context: Context): HomeQuickBeverageSnapshot? {
    val drinkId = this[HomeQuickBeverageWidgetState.drinkIdKey].orEmpty()
    val title = this[HomeQuickBeverageWidgetState.titleKey] ?: return null
    return HomeQuickBeverageSnapshot(
        drinkId = drinkId,
        title = title,
        amount = this[HomeQuickBeverageWidgetState.amountKey] ?: "--",
        subtitle = this[HomeQuickBeverageWidgetState.subtitleKey].orEmpty(),
        route = this[HomeQuickBeverageWidgetState.routeKey] ?: Screen.HydrationEntry.route,
    )
}

@SuppressLint("RestrictedApi")
private fun GlanceId.appWidgetIdOrNull(): Int? = (this as? AppWidgetId)?.appWidgetId

@Composable
private fun HomeQuickBeverageWidgetContent(snapshot: HomeQuickBeverageSnapshot) {
    val context = LocalContext.current
    val logAction = if (snapshot.drinkId.isBlank()) {
        actionStartActivity(openMetricIntent(context, snapshot.route))
    } else {
        actionRunCallback<HomeQuickBeverageLogAction>(
            actionParametersOf(QuickBeverageDrinkIdParameterKey to snapshot.drinkId)
        )
    }
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(WidgetBackground))
            .padding(16.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Column(
            modifier = GlanceModifier
                .defaultWeight()
                .clickable(logAction),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Text(
                text = snapshot.title,
                maxLines = 1,
                style = TextStyle(
                    color = ColorProvider(WidgetMutedText),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
            Text(
                text = snapshot.amount,
                maxLines = 1,
                style = TextStyle(
                    color = ColorProvider(WidgetPrimaryText),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            if (snapshot.subtitle.isNotBlank()) {
                Text(
                    text = snapshot.subtitle,
                    maxLines = 1,
                    style = TextStyle(
                        color = ColorProvider(WidgetMutedText),
                        fontSize = 12.sp,
                    ),
                )
            }
        }
        Spacer(modifier = GlanceModifier.width(10.dp))
        Column(
            modifier = GlanceModifier
                .width(54.dp)
                .height(64.dp)
                .background(ColorProvider(WidgetActionBackground))
                .clickable(actionStartActivity(openMetricIntent(context, snapshot.route)))
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Text(
                text = context.getString(R.string.action_edit),
                maxLines = 1,
                style = TextStyle(
                    color = ColorProvider(WidgetPrimaryText),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
    }
}

internal suspend fun loadQuickBeverageSnapshot(
    context: Context,
    drinkId: String,
    subtitleOverride: String? = null,
): HomeQuickBeverageSnapshot {
    val entryPoint = EntryPointAccessors.fromApplication(
        context.applicationContext,
        HomeQuickBeverageWidgetEntryPoint::class.java,
    )
    val drink = entryPoint.hydrationRepository()
        .customHydrationDrinks()
        .firstOrNull { it.id == drinkId && it.isValidCustomHydrationDrink() }
    val route = Screen.HydrationEntryLogDrink.createRoute(drinkId)
    return if (drink == null) {
        HomeQuickBeverageSnapshot(
            drinkId = drinkId,
            title = context.getString(R.string.home_quick_beverage_widget_config_title),
            amount = "--",
            subtitle = context.getString(R.string.home_quick_beverage_widget_not_configured),
            route = Screen.HydrationEntry.route,
        )
    } else {
        HomeQuickBeverageSnapshot(
            drinkId = drink.id,
            title = drink.name,
            amount = hydrationAmountLabel(drink.volumeLiters, entryPoint.unitFormatter()),
            subtitle = subtitleOverride ?: context.getString(R.string.home_quick_beverage_widget_tap_to_log),
            route = route,
        )
    }
}

private suspend fun updateQuickBeverageWidgetStatus(
    context: Context,
    glanceId: GlanceId,
    drinkId: String,
    subtitle: String,
) {
    val snapshot = loadQuickBeverageSnapshot(context, drinkId, subtitle)
    writeQuickBeverageWidgetSnapshot(context, glanceId, snapshot)
    HomeQuickBeverageWidget().update(context, glanceId)
}

private fun HydrationEntryError.quickBeverageWidgetMessage(context: Context): String =
    when (this) {
        HydrationEntryError.MISSING_WRITE_PERMISSION,
        HydrationEntryError.MISSING_NUTRITION_WRITE_PERMISSION -> context.getString(R.string.home_metric_widget_permission_needed)
        HydrationEntryError.INVALID_AMOUNT,
        HydrationEntryError.INVALID_CUSTOM_DRINK,
        HydrationEntryError.WRITE_FAILED -> context.getString(R.string.home_metric_widget_update_failed)
    }

private val QuickBeverageDrinkIdParameterKey =
    ActionParameters.Key<String>("quick_beverage_drink_id")

private val WidgetActionBackground = Color(0xFF20313A)

@EntryPoint
@InstallIn(SingletonComponent::class)
interface HomeQuickBeverageWidgetEntryPoint {
    fun hydrationRepository(): HydrationRepository
    fun nutritionRepository(): NutritionRepository
    fun unitFormatter(): UnitFormatter
    fun hydrationReminderController(): HydrationReminderController
}
