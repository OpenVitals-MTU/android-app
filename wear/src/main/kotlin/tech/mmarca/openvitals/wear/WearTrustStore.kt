package tech.mmarca.openvitals.wear

import android.content.Context
import android.content.SharedPreferences
import tech.mmarca.openvitals.wearlink.WearLinkTrustStore

/**
 * The phones the wearer has allowed, with the token each presents, and the
 * phones the wearer has blocked. Plain preferences: a few entries, keyed by
 * the phone's Bluetooth address. The server consults it on every hello;
 * Allow and Block, on the screen and on the request notification, write to it.
 */
class WearTrustStore(
    private val prefs: SharedPreferences,
    private val state: WearLinkState = WearLinkState,
    private val clock: () -> Long = System::currentTimeMillis,
) : WearLinkTrustStore {

    constructor(context: Context) : this(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE))

    /** Called when an unknown phone asks; the service turns it into a notification. */
    @Volatile
    var onPending: ((PendingPhone) -> Unit)? = null

    init {
        publish()
    }

    override fun tokenFor(peerAddress: String): String? = prefs.getString(tokenKey(peerAddress), null)

    override fun isBlocked(peerAddress: String): Boolean = peerAddress in blocked()

    override fun notePending(peerAddress: String, token: String, peerName: String) {
        val now = clock()
        val pending = PendingPhone(peerAddress, token, peerName, now, newKey = tokenFor(peerAddress) != null)
        // The same phone asking again keeps its first request; another phone replaces it.
        val current = state.pendingIfFresh(now)
        if (current != null && current.address == peerAddress && current.token == token) return
        state.update { it.copy(pending = pending) }
        onPending?.invoke(pending)
    }

    /** The wearer allowed [address] with [token], replacing any token it had. Clears a pending request for it. */
    fun trust(address: String, token: String, name: String) {
        prefs.edit()
            .putString(tokenKey(address), token)
            .putString(nameKey(address), name)
            .putLong(trustedAtKey(address), clock())
            .putStringSet(BLOCKED, (blocked() - address).toMutableSet())
            .apply()
        state.update { it.copy(pending = it.pending?.takeUnless { p -> p.address == address }) }
        publish()
    }

    fun block(address: String) {
        forgetEntry(address)
        prefs.edit().putStringSet(BLOCKED, (blocked() + address).toMutableSet()).apply()
        state.update { it.copy(pending = it.pending?.takeUnless { p -> p.address == address }) }
        publish()
    }

    fun unblock(address: String) {
        prefs.edit().putStringSet(BLOCKED, (blocked() - address).toMutableSet()).apply()
        publish()
    }

    /** Drops everything about [address]; its next hello is pending again. */
    fun forget(address: String) {
        forgetEntry(address)
        unblock(address)
    }

    fun trusted(): List<TrustedPhone> = prefs.all.keys
        .filter { it.startsWith(TOKEN) }
        .map { it.removePrefix(TOKEN) }
        .map { address ->
            TrustedPhone(address, prefs.getString(nameKey(address), null) ?: address, prefs.getLong(trustedAtKey(address), 0L))
        }
        .sortedBy { it.name }

    fun blocked(): Set<String> = prefs.getStringSet(BLOCKED, emptySet()).orEmpty()

    private fun forgetEntry(address: String) {
        prefs.edit().remove(tokenKey(address)).remove(nameKey(address)).remove(trustedAtKey(address)).apply()
    }

    private fun publish() {
        val trusted = trusted()
        state.update { it.copy(trusted = trusted) }
    }

    private fun tokenKey(address: String) = TOKEN + address.uppercase()
    private fun nameKey(address: String) = NAME + address.uppercase()
    private fun trustedAtKey(address: String) = TRUSTED_AT + address.uppercase()

    private companion object {
        const val PREFS = "wear_link_trust"
        const val TOKEN = "token_"
        const val NAME = "name_"
        const val TRUSTED_AT = "trusted_at_"
        const val BLOCKED = "blocked"
    }
}
