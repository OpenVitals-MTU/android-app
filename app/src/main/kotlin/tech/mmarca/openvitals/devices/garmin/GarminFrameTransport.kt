package tech.mmarca.openvitals.devices.garmin

/** A transport must send each logical frame atomically, without interleaving chunks. */
interface GarminFrameTransport {
    /** Sizes writes to a negotiated MTU. */
    fun onMtuChanged(mtu: Int)

    suspend fun sendFrame(frame: ByteArray)
}
