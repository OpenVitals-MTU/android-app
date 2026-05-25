package tech.mmarca.openvitals.features.manualentry

enum class ManualEntryWidgetId {
    HYDRATION,
}

val DefaultManualEntryWidgetIds: List<ManualEntryWidgetId> = listOf(
    ManualEntryWidgetId.HYDRATION,
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
