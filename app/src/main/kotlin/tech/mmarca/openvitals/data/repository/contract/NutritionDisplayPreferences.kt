package tech.mmarca.openvitals.data.repository.contract

import kotlinx.coroutines.flow.Flow
import tech.mmarca.openvitals.domain.preferences.NutritionAverageBasis

/** Whether nutrition averages count every day in the period or only the days with an entry. */
interface NutritionDisplayPreferences {

    var nutritionAverageBasis: NutritionAverageBasis

    /** Emits on every change, starting with the current value. */
    val nutritionAverageBasisFlow: Flow<NutritionAverageBasis>
}
