package tech.mmarca.openvitals.devices.wearos

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import tech.mmarca.openvitals.wearlink.WearLinkToken

/** The last thing the link learned about one registered watch. */
data class WearOsLinkSnapshot(
    val status: WearOsAppStatus,
    val watchName: String? = null,
    val bondLost: Boolean = false,
    val checkedAt: Instant,
)

/**
 * What the phone keeps about its watches' links: the token it presents to
 * each watch (one per Classic address, made on first use and never changed
 * unless the watch is forgotten), the Classic address a watch once answered
 * a hello from (the surest way to find its bond again), and, in memory,
 * the latest snapshot per device for the screen.
 */
@Singleton
class WearOsLinkStore(private val prefs: SharedPreferences) {

    @Inject
    constructor(@ApplicationContext context: Context) : this(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE))

    private val snapshotsFlow = MutableStateFlow<Map<String, WearOsLinkSnapshot>>(emptyMap())

    /** The latest snapshot per device id, as the screen shows it. */
    val snapshots: StateFlow<Map<String, WearOsLinkSnapshot>> = snapshotsFlow

    /** The token for the watch at [classicAddress], created on first use. */
    @Synchronized
    fun tokenFor(classicAddress: String): String {
        val key = tokenKey(classicAddress)
        prefs.getString(key, null)?.takeIf(WearLinkToken::isWellFormed)?.let { return it }
        val token = WearLinkToken.generate()
        prefs.edit { putString(key, token) }
        return token
    }

    /** Drops the token for [classicAddress]: the next hello is a fresh request on the watch. */
    fun forgetToken(classicAddress: String) {
        prefs.edit { remove(tokenKey(classicAddress)) }
    }

    /**
     * The Classic address the watch registered under [registeredAddress]
     * (the scan address) last answered a hello from, or null. The registry
     * keys devices by that scan address, so the port can look it up.
     */
    fun linkAddress(registeredAddress: String?): String? =
        registeredAddress?.let { prefs.getString(linkKey(it), null) }

    fun setLinkAddress(registeredAddress: String?, classicAddress: String) {
        if (registeredAddress.isNullOrBlank()) return
        prefs.edit { putString(linkKey(registeredAddress), classicAddress.uppercase()) }
    }

    fun record(deviceId: String, snapshot: WearOsLinkSnapshot) {
        snapshotsFlow.update { it + (deviceId to snapshot) }
    }

    fun clear(deviceId: String, registeredAddress: String?) {
        registeredAddress?.let { prefs.edit { remove(linkKey(it)) } }
        snapshotsFlow.update { it - deviceId }
    }

    private fun tokenKey(classicAddress: String) = "token_${classicAddress.uppercase()}"

    private fun linkKey(registeredAddress: String) = "link_address_${registeredAddress.uppercase()}"

    private companion object {
        const val PREFS = "wearos_link"
    }
}
