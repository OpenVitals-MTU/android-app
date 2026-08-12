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
 * Receives weather from a companion weather app — Breezy Weather in practice —
 * over Gadgetbridge's generic-weather broadcast contract.
 *
 * OpenVitals makes no network requests, so this broadcast is THE weather
 * source: the user points their weather app's "send to Gadgetbridge /
 * broadcast" feature at this app's package name, and every refresh lands
 * here. Both the plain-JSON and gzipped extras are accepted, since senders
 * vary.
 *
 * Exported deliberately: an external app must be able to reach it, and the
 * payload is public weather for a rough location — nothing a hostile sender
 * could do with it beyond showing wrong weather, which the freshness cap in
 * [WeatherStore] already bounds.
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
            // The gzipped form carries an ARRAY of locations; the plain form a
            // single object. Either way only the primary location matters —
            // the watch shows one place.
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
        /**
         * Gadgetbridge's action, verbatim — what Breezy Weather and friends
         * send today — plus a native alias so a future sender can address
         * this app without borrowing another project's namespace.
         */
        val ACCEPTED_ACTIONS = setOf(
            "nodomain.freeyourgadget.gadgetbridge.ACTION_GENERIC_WEATHER",
            "tech.mmarca.openvitals.ACTION_GENERIC_WEATHER",
        )
        const val EXTRA_WEATHER_JSON = "WeatherJson"
        const val EXTRA_WEATHER_GZ = "WeatherGz"
    }
}
