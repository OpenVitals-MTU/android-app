package tech.mmarca.openvitals.devices.weather

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import java.io.ByteArrayInputStream
import java.util.zip.GZIPInputStream
import javax.inject.Inject
import org.json.JSONObject
import tech.mmarca.openvitals.devices.garmin.GarminLog

/**
 * Receives weather from a companion app (Breezy Weather) over Gadgetbridge's
 * generic-weather broadcast. This is the only weather source. Exported on
 * purpose: the payload is public weather, and freshness is capped.
 */
@AndroidEntryPoint
class GenericWeatherReceiver : BroadcastReceiver() {

    @Inject
    lateinit var store: WeatherStore

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in ACCEPTED_ACTIONS) return
        val bundle = intent.extras ?: return
        try {
            val json = bundle.getString(EXTRA_WEATHER_JSON)
                ?: bundle.getByteArray(EXTRA_WEATHER_GZ)?.let(::gunzip)
                ?: return
            // The gzipped form is an array of locations; only the primary matters.
            val primary = json.trimStart().let { trimmed ->
                if (trimmed.startsWith("[")) {
                    org.json.JSONArray(trimmed).optJSONObject(0) ?: return
                } else {
                    JSONObject(trimmed)
                }
            }
            val snapshot = WeatherSnapshot.fromJson(primary)
            store.save(snapshot)
            GarminLog.log(
                "[WEATHER] received ${snapshot.location.ifBlank { "(unnamed)" }} " +
                    "${snapshot.currentTempKelvin}K, ${snapshot.hourly.size}h/" +
                    "${snapshot.daily.size}d forecast",
            )
        } catch (error: Exception) {
            GarminLog.log("[WEATHER] broken weather broadcast: $error")
        }
    }

    private fun gunzip(compressed: ByteArray): String =
        GZIPInputStream(ByteArrayInputStream(compressed)).use { stream ->
            stream.readBytes().toString(Charsets.UTF_8)
        }

    companion object {
        /** Gadgetbridge's action, plus a native alias. */
        val ACCEPTED_ACTIONS = setOf(
            "nodomain.freeyourgadget.gadgetbridge.ACTION_GENERIC_WEATHER",
            "tech.mmarca.openvitals.ACTION_GENERIC_WEATHER",
        )
        const val EXTRA_WEATHER_JSON = "WeatherJson"
        const val EXTRA_WEATHER_GZ = "WeatherGz"
    }
}
