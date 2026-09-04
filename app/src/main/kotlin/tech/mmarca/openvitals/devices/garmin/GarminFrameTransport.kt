package tech.mmarca.openvitals.devices.garmin

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The common GFDI frame seam shared by Garmin's V1 and V2 BLE transports.
 *
 * [sendFrame] is atomic at the logical-frame level. A frame may take several
 * characteristic writes at MTU 23, but no other GFDI frame may place a chunk
 * between them or the watch's streaming COBS decoder will see one corrupt
 * packet assembled from two callers.
 */
interface GarminFrameTransport {
    suspend fun sendFrame(frame: ByteArray)
}

/**
 * Garmin's direct V1 GFDI transport: COBS frames are written and received
 * without the V2 multi-link handle byte.
 */
class GarminV1Transport(
    private val write: suspend (ByteArray) -> Unit,
    private val onFrame: (GarminGfdiFrame) -> Unit,
    private val onLog: ((String) -> Unit)? = null,
) : GarminFrameTransport {
    private var maxWriteSize = 20
    private val decoder = GarminCobsDecoder()
    private val sendMutex = Mutex()

    fun onMtuChanged(mtu: Int) {
        maxWriteSize = (mtu.coerceIn(23, 515) - 3).coerceAtMost(512)
    }

    override suspend fun sendFrame(frame: ByteArray) = sendMutex.withLock {
        val encoded = GarminCobs.encode(frame)
        var offset = 0
        while (offset < encoded.size) {
            val end = (offset + maxWriteSize).coerceAtMost(encoded.size)
            write(encoded.copyOfRange(offset, end))
            offset = end
        }
    }

    fun handleInbound(packet: ByteArray) {
        decoder.addBytes(packet)
        var raw = decoder.pull()
        while (raw != null) {
            try {
                onFrame(GarminGfdiFrame.parse(raw))
            } catch (error: GarminGfdiFrameException) {
                onLog?.invoke("[GARMIN-V1] dropped bad frame: ${error.message}")
            }
            raw = decoder.pull()
        }
    }
}
