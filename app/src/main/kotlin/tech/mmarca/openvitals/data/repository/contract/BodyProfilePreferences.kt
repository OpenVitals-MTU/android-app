package tech.mmarca.openvitals.data.repository.contract

import kotlinx.coroutines.flow.Flow
import tech.mmarca.openvitals.domain.preferences.BodyProfile

/** The user's age, size and heart rates. Calculations read it; Settings writes it. */
interface BodyProfilePreferences {

    fun bodyProfile(): BodyProfile

    val bodyProfileFlow: Flow<BodyProfile>

    /** Stores the profile after normalizing it, so a bad value never reaches a calculation. */
    fun setBodyProfile(profile: BodyProfile)
}
