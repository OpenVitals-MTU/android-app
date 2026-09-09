package tech.mmarca.openvitals.devices.garmin

import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.devices.weather.WeatherSnapshot

/** What a session's owner provides for the watch's asks. A missing hook refuses that ask. */
data class GarminSessionHooks(
    /** The weather to serve, or null when none is fresh: the ask is acked and left unanswered. */
    val weatherProvider: (() -> WeatherSnapshot?)? = null,
    /** Last-known phone position. Without it the watch never fetches weather. */
    val locationProvider: (() -> GarminPhoneLocation?)? = null,
    /** GPS ephemeris for the watch. Null refuses the ask. */
    val agpsSource: GarminAgpsSource? = null,
    /**
     * Calendar events in the asked window, or null when sync is off. Off
     * still answers with none, or the watch re-asks forever.
     */
    val calendarProvider: (
        (beginEpochSeconds: Long, endEpochSeconds: Long) -> List<GarminCalendarEvent>?
    )? = null,
    /** The watch asked the phone to ring, or to stop. Ringing is the owner's job. */
    val onFindPhone: ((durationSeconds: Int) -> Unit)? = null,
    val onFindPhoneCancel: (() -> Unit)? = null,
    /** Whether the phone's app is in the foreground, read at handshake time. */
    val hostForeground: (() -> Boolean)? = null,
    /** The watch still needs the pair-flow trio. OR'd with a capability flag the watch shows briefly. */
    val setupWizardPending: Boolean = false,
    /** The trio went out; the owner clears its pending flag. */
    val onSetupWizardCompleted: (() -> Unit)? = null,
    /** Called after the capabilities exchange, when the watch accepts requests. */
    val onHandshakeReady: (() -> Unit)? = null,
)

/**
 * Answers the watch's asks that are not file sync: time, weather,
 * find-my-phone, and the protobuf services (HTTP proxy, calendar, location,
 * OAuth). Owned by [GarminSession] and sends on its link.
 */
internal class GarminSessionResponders(
    private val scope: CoroutineScope,
    private val send: suspend (ByteArray) -> Unit,
    private val protobuf: GarminProtobufTransport,
    private val hooks: GarminSessionHooks,
) {

    /**
     * Answers the watch's HTTP-proxy fetches (weather, ephemeris).
     * The first interceptor to claim a URL wins.
     */
    private val http: GarminHttpProxy? by lazy {
        val interceptors = buildList {
            hooks.weatherProvider?.let { add(GarminWeatherInterceptor(it)) }
            hooks.agpsSource?.let { add(GarminAgpsInterceptor(it)) }
            // OAuth last: it claims by path alone, across every domain.
            if (isNotEmpty()) add(GarminOauthInterceptor())
        }
        interceptors.takeIf { it.isNotEmpty() }?.let { GarminHttpProxy(it) }
    }

    private val coreLocation: GarminCoreLocation? by lazy {
        hooks.locationProvider?.let { GarminCoreLocation(it) }
    }

    /** Always present: an unanswered calendar ask is re-sent forever. */
    private val calendar = GarminCalendarResponder(hooks.calendarProvider)

    /** Weather records waiting for the watch to accept the definitions. Sent together, it dropped them. */
    private var pendingWeatherData: ByteArray? = null

    /** A protobuf request from the watch. Replies are launched: this runs inside frame handling. */
    fun handleServiceRequest(requestId: Int, payload: ByteArray) {
        val httpReply = http?.handle(payload)
            ?: calendar.handle(payload)
            ?: GarminServiceResponders.handle(payload)
        // Guarded: the scope has no exception handler, so a failed write
        // here would otherwise take the process down.
        if (httpReply != null) {
            scope.launch {
                runCatching { protobuf.respond(requestId, httpReply) }
                    .onFailure { GarminLog.log("[GARMIN-PB] reply #$requestId failed: $it") }
            }
            return
        }
        val coreReply = coreLocation?.handle(payload) ?: return
        scope.launch {
            runCatching {
                protobuf.respond(requestId, coreReply.payload)
                coreReply.followUp?.let { followUp ->
                    protobuf.request(followUp, label = "location update", timeout = 5.seconds)
                }
            }.onFailure {
                GarminLog.log("[GARMIN-PB] location reply #$requestId failed: $it")
            }
        }
    }

    suspend fun handleCurrentTime(message: GarminCurrentTimeRequest) {
        // This app is the watch's only clock source: reply with time, zone and DST.
        GarminLog.log("[GARMIN-SYNC] watch asked for the time")
        send(buildCurrentTimeResponse(referenceId = message.referenceId))
    }

    suspend fun handleWeatherRequest(message: GarminWeatherRequest) {
        val weather = hooks.weatherProvider?.invoke()
        if (weather == null) {
            GarminLog.log("[GARMIN-WEATHER] watch asked; nothing fresh to serve")
            return
        }
        GarminLog.log(
            "[GARMIN-WEATHER] watch asked (format=${message.format}, " +
                "${message.hoursOfForecast}h); sending " +
                "${weather.hourly.size}h/${weather.daily.size}d " +
                "for \"${weather.location}\"",
        )
        sendWeatherDefinitions(weather)
    }

    /** Pushes weather after the capabilities exchange, when the watch has the glance. */
    suspend fun pushWeatherIfSupported(capabilities: Set<GarminCapability>) {
        // Only watches with the capability have a weather glance; others NAK.
        if (GarminCapability.WEATHER_CONDITIONS !in capabilities) return
        val weather = hooks.weatherProvider?.invoke() ?: return
        GarminLog.log(
            "[GARMIN-WEATHER] pushing ${weather.hourly.size}h/" +
                "${weather.daily.size}d for \"${weather.location}\"",
        )
        sendWeatherDefinitions(weather)
    }

    /** The watch accepted the weather definitions: the records may follow. */
    suspend fun onFitDefinitionAccepted() {
        val data = pendingWeatherData ?: return
        pendingWeatherData = null
        GarminLog.log("[GARMIN-WEATHER] definitions accepted; sending records")
        send(GarminGfdiFrame.build(GarminMessageId.FIT_DATA, data))
    }

    fun handleFindPhone(message: GarminFindMyPhoneRequest) {
        GarminLog.log("[GARMIN-SYNC] find-my-phone for ${message.durationSeconds}s")
        hooks.onFindPhone?.invoke(message.durationSeconds)
    }

    fun handleFindPhoneCancel() {
        GarminLog.log("[GARMIN-SYNC] find-my-phone cancelled from the watch")
        hooks.onFindPhoneCancel?.invoke()
    }

    /** Definitions first; the records wait for the watch's status on them. */
    private suspend fun sendWeatherDefinitions(weather: WeatherSnapshot) {
        pendingWeatherData = GarminFitWeather.dataPayload(weather)
        send(
            GarminGfdiFrame.build(GarminMessageId.FIT_DEFINITION, GarminFitWeather.definitionPayload()),
        )
    }
}
