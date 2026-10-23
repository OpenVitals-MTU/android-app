package tech.mmarca.openvitals.data.repository

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import tech.mmarca.openvitals.data.repository.contract.CsvImportPreferences
import tech.mmarca.openvitals.domain.model.BpRecordValues

/** In a preference file of its own: [PreferencesRepository] is at its size ceiling. */
@Singleton
class CsvImportPreferencesRepository @Inject constructor(
    @ApplicationContext context: Context,
) : CsvImportPreferences {
    private val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

    override var defaultBodyPosition: Int
        get() = prefs.getInt(KEY_DEFAULT_BODY_POSITION, BpRecordValues.BODY_POSITION_UNKNOWN)
        set(value) = prefs.edit { putInt(KEY_DEFAULT_BODY_POSITION, value) }

    override var defaultCuffLocation: Int
        get() = prefs.getInt(KEY_DEFAULT_CUFF_LOCATION, BpRecordValues.MEASUREMENT_LOCATION_UNKNOWN)
        set(value) = prefs.edit { putInt(KEY_DEFAULT_CUFF_LOCATION, value) }

    private companion object {
        const val PREFS_FILE = "openvitals_csv_import_preferences"
        const val KEY_DEFAULT_BODY_POSITION = "default_body_position"
        const val KEY_DEFAULT_CUFF_LOCATION = "default_cuff_location"
    }
}
