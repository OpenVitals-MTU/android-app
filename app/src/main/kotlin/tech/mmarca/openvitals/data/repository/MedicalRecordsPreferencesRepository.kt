package tech.mmarca.openvitals.data.repository

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import tech.mmarca.openvitals.data.repository.contract.MedicalRecordsPreferences

/** In a preference file of its own: [PreferencesRepository] is at its size ceiling. */
@Singleton
class MedicalRecordsPreferencesRepository @Inject constructor(
    @ApplicationContext context: Context,
) : MedicalRecordsPreferences {
    private val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

    override var firstPermissionRequestDone: Boolean
        get() = prefs.getBoolean(KEY_FIRST_REQUEST_DONE, false)
        set(value) = prefs.edit { putBoolean(KEY_FIRST_REQUEST_DONE, value) }

    private companion object {
        const val PREFS_FILE = "openvitals_medical_records_preferences"
        const val KEY_FIRST_REQUEST_DONE = "first_permission_request_done"
    }
}
