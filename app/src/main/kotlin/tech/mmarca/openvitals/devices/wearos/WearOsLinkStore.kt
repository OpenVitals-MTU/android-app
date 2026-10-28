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
 * each added watch (made on first use, kept until the watch is removed),
 * the Classic address a watch once answered a hello from (the surest way to
 * find its bond again), and, in memory, the latest snapshot per device for
 * the screen. Both are keyed by the address the watch was added under: the
 * bond's own address changes when the watch is paired again, and a new
 * token would have the watch ask the wearer again.
 */
@Singleton
class WearOsLinkStore(private val prefs: SharedPreferences) {

    @Inject
    constructor(@ApplicationContext context: Context) : this(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE))

    private val snapshotsFlow = MutableStateFlow<Map<String, WearOsLinkSnapshot>>(emptyMap())

    /** The latest snapshot per device id, as the screen shows it. */
    val snapshots: StateFlow<Map<String, WearOsLinkSnapshot>> = snapshotsFlow

    /**
     * The token for the watch added under [registeredAddress], reached through
     * the bond at [bondAddress]; created on first use. Tokens used to be kept
     * per bond address: one found there is adopted, so the watch that allowed
     * it is not asked again.
     */
    @Synchronized
    fun tokenFor(registeredAddress: String?, bondAddress: String): String {
        val key = tokenKey(registeredAddress?.takeIf { it.isNotBlank() } ?: bondAddress)
        prefs.getString(key, null)?.takeIf(WearLinkToken::isWellFormed)?.let { return it }
        val legacyKey = legacyTokenKey(bondAddress)
        val token = prefs.getString(legacyKey, null)?.takeIf(WearLinkToken::isWellFormed) ?: WearLinkToken.generate()
        prefs.edit {
            putString(key, token)
            remove(legacyKey)
        }
        return token
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

    /** Forgets a removed watch: adding it again presents a new token, which the watch asks the wearer about. */
    fun clear(deviceId: String, registeredAddress: String?) {
        registeredAddress?.let {
            prefs.edit {
                remove(linkKey(it))
                remove(tokenKey(it))
            }
        }
        snapshotsFlow.update { it - deviceId }
    }

    private fun tokenKey(registeredAddress: String) = "watch_token_${registeredAddress.uppercase()}"

    private fun legacyTokenKey(bondAddress: String) = "token_${bondAddress.uppercase()}"

    private fun linkKey(registeredAddress: String) = "link_address_${registeredAddress.uppercase()}"

    private companion object {
        const val PREFS = "wearos_link"
    }
}
