package tech.mmarca.openvitals.features.nutrition

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import tech.mmarca.openvitals.domain.model.NutritionEntry

class NutritionEntryEditingTest {

    @Test fun `a typed entry and an old carbs entry edit in the nutrition form`() {
        assertEquals(NutritionEntryEditKind.TYPED, entry("openvitals_manual_nutrition_1_u").editKind())
        assertEquals(
            NutritionEntryEditKind.TYPED,
            entry("openvitals_nutrition_1_u", name = "OpenVitals carbs").editKind(),
        )
    }

    @Test fun `a drink's nutrition edits as the drink`() {
        assertEquals(
            NutritionEntryEditKind.DRINK,
            entry("openvitals_hydration_nutrition_openvitals_hydration_1_u").editKind(),
        )
    }

    @Test fun `a food, a water-less drink, another app's record or one without ids is not editable`() {
        assertNull(entry("openvitals_food_1_banana_u", name = "Banana").editKind())
        assertNull(entry("openvitals_nutrition_1_u", name = "Espresso").editKind())
        assertNull(entry("openvitals_hydration_nutrition_").editKind())
        assertNull(entry("openvitals_manual_nutrition_1_u", isOpenVitals = false).editKind())
        assertNull(entry("openvitals_manual_nutrition_1_u", id = "").editKind())
        assertNull(entry(null).editKind())
    }

    private fun entry(
        clientRecordId: String?,
        name: String = "OpenVitals nutrition",
        isOpenVitals: Boolean = true,
        id: String = "record-1",
    ) = NutritionEntry(
        time = Instant.EPOCH,
        mealType = 0,
        name = name,
        energyKcal = 100.0,
        proteinGrams = null,
        carbsGrams = null,
        fatGrams = null,
        fiberGrams = null,
        sugarGrams = null,
        source = "test",
        id = id,
        clientRecordId = clientRecordId,
        isOpenVitalsEntry = isOpenVitals,
    )
}
