package tech.mmarca.openvitals.data.repository.contract

import kotlinx.coroutines.flow.Flow
import tech.mmarca.openvitals.domain.preferences.CaffeinePreferences

/** The caffeine clearance model: half-life, the sleep threshold and the factors behind them. */
interface CaffeineModelPreferences {

    fun caffeinePreferences(): CaffeinePreferences

    /** Emits on every change, starting with the current value. */
    val caffeinePreferencesFlow: Flow<CaffeinePreferences>

    /** Stores the model after normalizing it; read back for the stored value. */
    fun setCaffeinePreferences(preferences: CaffeinePreferences)
}
