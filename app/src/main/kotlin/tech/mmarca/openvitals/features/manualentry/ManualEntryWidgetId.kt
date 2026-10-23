package tech.mmarca.openvitals.features.manualentry

import tech.mmarca.openvitals.features.manualentry.*
import tech.mmarca.openvitals.features.manualentry.activity.*
import tech.mmarca.openvitals.features.manualentry.activity.recording.*
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.*
import tech.mmarca.openvitals.features.manualentry.body.*
import tech.mmarca.openvitals.features.manualentry.hydration.*
import tech.mmarca.openvitals.features.manualentry.mindfulness.*
import tech.mmarca.openvitals.features.manualentry.vitals.*



enum class ManualEntryWidgetId {
    HYDRATION,
    FOOD,
    NUTRITION,
    ACTIVITY,
    MINDFULNESS,
    BODY_COMPOSITION,
    HEIGHT,
    BLOOD_PRESSURE,
    SPO2,
    RESPIRATORY_RATE,
    BODY_TEMPERATURE,
    HRV,
    BLOOD_GLUCOSE,
    VO2_MAX,
    CYCLE,
    WORKOUT_PLANS,
}

val DefaultManualEntryWidgetIds: List<ManualEntryWidgetId> = listOf(
    ManualEntryWidgetId.HYDRATION,
    ManualEntryWidgetId.FOOD,
    ManualEntryWidgetId.NUTRITION,
    ManualEntryWidgetId.ACTIVITY,
    ManualEntryWidgetId.WORKOUT_PLANS,
    ManualEntryWidgetId.MINDFULNESS,
    ManualEntryWidgetId.BODY_COMPOSITION,
    ManualEntryWidgetId.HEIGHT,
    ManualEntryWidgetId.BLOOD_PRESSURE,
    ManualEntryWidgetId.SPO2,
    ManualEntryWidgetId.RESPIRATORY_RATE,
    ManualEntryWidgetId.BODY_TEMPERATURE,
    ManualEntryWidgetId.HRV,
    ManualEntryWidgetId.BLOOD_GLUCOSE,
    ManualEntryWidgetId.VO2_MAX,
    ManualEntryWidgetId.CYCLE,
)

fun customizableManualEntryWidgetIds(widgetIds: List<ManualEntryWidgetId>): List<ManualEntryWidgetId> =
    widgetIds.distinct()

/**
 * Tiles that were renamed, so a saved order keeps them. The carbs tile became one for every
 * nutrient, and the weight and body fat tiles one for everything a body analysis scale shows.
 */
private val storedWidgetIdRenames: Map<String, String> = mapOf(
    "CARBS" to ManualEntryWidgetId.NUTRITION.name,
    "WEIGHT" to ManualEntryWidgetId.BODY_COMPOSITION.name,
    "BODY_FAT" to ManualEntryWidgetId.BODY_COMPOSITION.name,
)

fun manualEntryWidgetIdsFromStored(storedIds: List<String>?): List<ManualEntryWidgetId> {
    if (storedIds == null) return DefaultManualEntryWidgetIds
    if (storedIds.isEmpty()) return emptyList()

    val parsedIds = storedIds
        .mapNotNull { storedId ->
            runCatching { ManualEntryWidgetId.valueOf(storedWidgetIdRenames[storedId] ?: storedId) }.getOrNull()
        }
        .let(::customizableManualEntryWidgetIds)

    return parsedIds.ifEmpty { DefaultManualEntryWidgetIds }
}
