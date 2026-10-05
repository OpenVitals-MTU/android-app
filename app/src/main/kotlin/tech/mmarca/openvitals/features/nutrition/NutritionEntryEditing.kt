package tech.mmarca.openvitals.features.nutrition

import tech.mmarca.openvitals.domain.model.LegacyOpenVitalsCarbsEntryName
import tech.mmarca.openvitals.domain.model.NutritionEntry
import tech.mmarca.openvitals.domain.model.OpenVitalsManualNutritionClientRecordPrefix
import tech.mmarca.openvitals.features.manualentry.hydration.pairedHydrationClientRecordIdOrNull

/** Where an entry in the nutrition list is edited. */
enum class NutritionEntryEditKind {
    /** Typed into the nutrition form: the form opens on it. */
    TYPED,

    /** The nutrition half of a drink: the drink is edited, and its nutrients follow. */
    DRINK,
}

/**
 * Null when the entry cannot be edited here: another app wrote it, or it is a logged food,
 * or a drink logged without water. A food stays delete-only, so its catalog portion holds.
 */
fun NutritionEntry.editKind(): NutritionEntryEditKind? {
    if (!isOpenVitalsEntry || id.isBlank()) return null
    val clientRecordId = clientRecordId ?: return null
    return when {
        clientRecordId.startsWith(OpenVitalsManualNutritionClientRecordPrefix) -> NutritionEntryEditKind.TYPED
        name == LegacyOpenVitalsCarbsEntryName -> NutritionEntryEditKind.TYPED
        !clientRecordId.pairedHydrationClientRecordIdOrNull().isNullOrBlank() -> NutritionEntryEditKind.DRINK
        else -> null
    }
}

/** An edit the screen should open. Ids are Health Connect record ids. */
sealed interface NutritionEditTarget {
    data class TypedEntry(val nutritionRecordId: String) : NutritionEditTarget

    data class Drink(val hydrationRecordId: String) : NutritionEditTarget
}
