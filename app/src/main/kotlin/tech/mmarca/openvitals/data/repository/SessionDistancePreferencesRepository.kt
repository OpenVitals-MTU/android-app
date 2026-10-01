package tech.mmarca.openvitals.data.repository

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import tech.mmarca.openvitals.data.repository.contract.SessionDistancePreferences

/** In a preference file of its own: [PreferencesRepository] is at its size ceiling. */
@Singleton
class SessionDistancePreferencesRepository @Inject constructor(
    @ApplicationContext context: Context,
) : SessionDistancePreferences {
    private val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

    override var preferRouteDistance: Boolean
        get() = prefs.getBoolean(KEY_PREFER_ROUTE_DISTANCE, false)
        set(value) = prefs.edit { putBoolean(KEY_PREFER_ROUTE_DISTANCE, value) }

    private companion object {
        const val PREFS_FILE = "openvitals_session_distance_preferences"
        const val KEY_PREFER_ROUTE_DISTANCE = "prefer_route_distance"
    }
}
