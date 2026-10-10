package tech.mmarca.openvitals.wear

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/** A phone the wearer has allowed. */
data class TrustedPhone(val address: String, val name: String, val trustedAtMillis: Long)

/** A phone that said hello with a token the watch does not hold yet. */
data class PendingPhone(val address: String, val token: String, val name: String, val sinceMillis: Long)

/** A known phone that presented a different token. */
data class RefusedPhone(val address: String, val name: String, val atMillis: Long)

/** What the status screen shows about the link. */
data class WearLinkUiState(
    val bluetoothOn: Boolean = true,
    val listening: Boolean = false,
    val activeClients: Int = 0,
    val trusted: List<TrustedPhone> = emptyList(),
    val pending: PendingPhone? = null,
    val refused: RefusedPhone? = null,
    /** Trusted phones no longer in the watch's bonded list. */
    val bondLost: List<TrustedPhone> = emptyList(),
    val ppgLogging: Boolean = false,
    /** Whether the sleep recorder holds the processor awake for bedtime. */
    val bedtimeHold: BedtimeHold = BedtimeHold.OFF,
)

/**
 * The link's state, written by the service and its helpers, read by the
 * status screen. One process-wide holder: the service and the activity
 * are in the same process and the screen only ever reads.
 */
object WearLinkState {

    private val flow = MutableStateFlow(WearLinkUiState())

    val state: StateFlow<WearLinkUiState> = flow

    fun update(change: (WearLinkUiState) -> WearLinkUiState) = flow.update(change)

    /** The pending request, unless it is older than [PENDING_TTL_MILLIS]. */
    fun pendingIfFresh(nowMillis: Long): PendingPhone? =
        flow.value.pending?.takeIf { nowMillis - it.sinceMillis <= PENDING_TTL_MILLIS }

    /** Drops a pending request that has expired as of [nowMillis]. */
    fun expirePending(nowMillis: Long) {
        flow.update { s -> if (s.pending != null && pendingIfFresh(nowMillis) == null) s.copy(pending = null) else s }
    }

    /** How long a request stays open for the wearer to answer. */
    const val PENDING_TTL_MILLIS: Long = 5 * 60 * 1000
}
