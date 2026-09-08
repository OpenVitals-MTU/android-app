package tech.mmarca.openvitals.devices.garmin

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** A transport must send each logical frame atomically, without interleaving chunks. */
interface GarminFrameTransport {
    suspend fun sendFrame(frame: ByteArray)
}

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
