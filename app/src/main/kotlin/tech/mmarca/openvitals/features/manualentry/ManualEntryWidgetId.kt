package tech.mmarca.openvitals.features.manualentry

enum class ManualEntryWidgetId {
    HYDRATION,
    WEIGHT,
    HEIGHT,
    BODY_FAT,
}

val DefaultManualEntryWidgetIds: List<ManualEntryWidgetId> = listOf(
    ManualEntryWidgetId.HYDRATION,
    ManualEntryWidgetId.WEIGHT,
    ManualEntryWidgetId.HEIGHT,
    ManualEntryWidgetId.BODY_FAT,
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
