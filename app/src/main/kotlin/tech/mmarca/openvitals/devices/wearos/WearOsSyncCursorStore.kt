package tech.mmarca.openvitals.devices.wearos

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

/**
 * How far each Wear OS watch's heart rate has been pulled: the time of the
 * newest sample written to Health Connect, per device id. The next sync asks
 * the watch for what is newer. A re-pull is harmless: the import's record ids
 * are deterministic, so an upsert follows.
 */
@Singleton
class WearOsSyncCursorStore(private val prefs: SharedPreferences) {

    @Inject
    constructor(@ApplicationContext context: Context) : this(
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE),
    )

    /** The cursor, or [Instant.EPOCH] for a watch never synced. */
    fun heartRateCursor(deviceId: String): Instant =
        Instant.ofEpochMilli(prefs.getLong(key(deviceId), 0L))

    fun setHeartRateCursor(deviceId: String, at: Instant) {
        prefs.edit { putLong(key(deviceId), at.toEpochMilli()) }
    }

    fun clear(deviceId: String) {
        prefs.edit { remove(key(deviceId)) }
    }

    private fun key(deviceId: String) = "hr_cursor_$deviceId"

    private companion object {
        const val PREFS_FILE = "wearos_sync_cursors"
    }
}
