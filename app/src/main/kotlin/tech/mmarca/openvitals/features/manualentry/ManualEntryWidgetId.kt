package tech.mmarca.openvitals.features.manualentry

enum class ManualEntryWidgetId {
    HYDRATION,
    MINDFULNESS,
    WEIGHT,
    HEIGHT,
    BODY_FAT,
    BLOOD_PRESSURE,
    SPO2,
    RESPIRATORY_RATE,
    BODY_TEMPERATURE,
}

val DefaultManualEntryWidgetIds: List<ManualEntryWidgetId> = listOf(
    ManualEntryWidgetId.HYDRATION,
    ManualEntryWidgetId.MINDFULNESS,
    ManualEntryWidgetId.WEIGHT,
    ManualEntryWidgetId.HEIGHT,
    ManualEntryWidgetId.BODY_FAT,
    ManualEntryWidgetId.BLOOD_PRESSURE,
    ManualEntryWidgetId.SPO2,
    ManualEntryWidgetId.RESPIRATORY_RATE,
    ManualEntryWidgetId.BODY_TEMPERATURE,
)

fun customizableManualEntryWidgetIds(widgetIds: List<ManualEntryWidgetId>): List<ManualEntryWidgetId> =
    widgetIds.distinct()

fun manualEntryWidgetIdsFromStored(storedIds: List<String>?): List<ManualEntryWidgetId> {
    if (storedIds == null) return DefaultManualEntryWidgetIds
    if (storedIds.isEmpty()) return emptyList()

    val parsedIds = storedIds
        .mapNotNull { storedId ->
            runCatching { ManualEntryWidgetId.valueOf(storedId) }.getOrNull()
        }
        .let(::customizableManualEntryWidgetIds)

    return parsedIds.ifEmpty { DefaultManualEntryWidgetIds }
}
