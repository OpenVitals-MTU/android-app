package tech.mmarca.openvitals.devices.wearos

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

/**
 * How far each Wear OS watch has been pulled, per device id: the time of the
 * newest heart rate sample written to Health Connect, and the time of the
 * newest sleep minute an estimate was made from. The next sync asks the
 * watch for what is newer. A re-pull is harmless: the imports' record ids
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

    /** The sleep cursor, or [Instant.EPOCH] for a watch never synced. */
    fun sleepCursor(deviceId: String): Instant =
        Instant.ofEpochMilli(prefs.getLong(sleepKey(deviceId), 0L))

    fun setSleepCursor(deviceId: String, at: Instant) {
        prefs.edit { putLong(sleepKey(deviceId), at.toEpochMilli()) }
    }

    fun clear(deviceId: String) {
        prefs.edit {
            remove(key(deviceId))
            remove(sleepKey(deviceId))
        }
    }

    private fun key(deviceId: String) = "hr_cursor_$deviceId"

    private fun sleepKey(deviceId: String) = "sleep_cursor_$deviceId"

    private companion object {
        const val PREFS_FILE = "wearos_sync_cursors"
    }
}
