package tech.mmarca.openvitals.devices.weather

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONObject

/**
 * The last weather report a companion app broadcast, and when it arrived.
 *
 * One snapshot, no history: the watch only ever wants "the weather now", and
 * a companion app re-broadcasts on its own refresh schedule. [freshSnapshot]
 * is what the Garmin link serves — stale weather is worse than none, because
 * the watch renders it with full confidence and no timestamp.
 */
@Singleton
class WeatherStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

    fun save(snapshot: WeatherSnapshot, receivedAt: Instant = Instant.now()) {
        prefs.edit {
            putString(KEY_SNAPSHOT, snapshot.toJson().toString())
            putLong(KEY_RECEIVED_AT, receivedAt.toEpochMilli())
        }
    }

    fun snapshot(): WeatherSnapshot? =
        prefs.getString(KEY_SNAPSHOT, null)?.let { stored ->
            runCatching { WeatherSnapshot.fromJson(JSONObject(stored)) }.getOrNull()
        }

    fun receivedAt(): Instant? =
        prefs.getLong(KEY_RECEIVED_AT, 0L).takeIf { it > 0L }?.let(Instant::ofEpochMilli)

    /** The snapshot if it is recent enough to show on a wrist, else null. */
    fun freshSnapshot(now: Instant = Instant.now()): WeatherSnapshot? {
        val received = receivedAt() ?: return null
        if (Duration.between(received, now) > MAX_AGE) return null
        return snapshot()
    }

    private companion object {
        const val PREFS_FILE = "weather_store"
        const val KEY_SNAPSHOT = "snapshot"
        const val KEY_RECEIVED_AT = "received_at"

        /**
         * Companion apps refresh every 30m-2h; anything older than this is a
         * report the app itself would no longer show.
         */
        val MAX_AGE: Duration = Duration.ofHours(6)
    }
}
