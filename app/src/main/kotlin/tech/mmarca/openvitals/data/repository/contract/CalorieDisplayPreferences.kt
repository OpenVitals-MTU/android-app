package tech.mmarca.openvitals.data.repository.contract

import kotlinx.coroutines.flow.Flow

/** Whether calorie figures come from OpenVitals' own estimate or only from what was recorded. */
interface CalorieDisplayPreferences {

    var showOpenVitalsCalculatedCalories: Boolean

    /** Emits on every change, starting with the current value. */
    val showOpenVitalsCalculatedCaloriesFlow: Flow<Boolean>
}
