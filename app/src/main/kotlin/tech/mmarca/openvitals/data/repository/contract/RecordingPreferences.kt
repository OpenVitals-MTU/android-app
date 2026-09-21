package tech.mmarca.openvitals.data.repository.contract

import tech.mmarca.openvitals.domain.preferences.ActivityRecordingPreferences

/** What the workout form remembers: how to record, and which type to open on. */
interface RecordingPreferences {

    fun activityRecordingPreferences(): ActivityRecordingPreferences

    /** Stores the preferences after normalizing them. */
    fun setActivityRecordingPreferences(preferences: ActivityRecordingPreferences)

    /** The type of the last workout entered; the form opens on it. */
    var lastActivityExerciseType: Int?

    /** The type the user pinned; it wins over the last one. */
    var favoriteActivityExerciseType: Int?
}
