package tech.mmarca.openvitals.sensors.ble

/**
 * Runs GATT writes one at a time. Android allows one GATT operation in flight and refuses
 * the next one outright, so a loop of `writeDescriptor` calls subscribes only the first
 * characteristic. Not thread-safe: call it from the GATT callback thread only.
 *
 * [write] starts one write and says whether the stack took it.
 */
internal class BleGattWriteQueue<T>(private val write: (T) -> Boolean) {
    private val pending = ArrayDeque<T>()
    private var inFlight: T? = null

    fun enqueue(items: List<T>) {
        pending += items
        if (inFlight == null) startNext()
    }

    /** Call when the stack reports a write done. Returns the item it was for, then starts the next. */
    fun onWriteFinished(): T? {
        val finished = inFlight
        inFlight = null
        startNext()
        return finished
    }

    fun clear() {
        pending.clear()
        inFlight = null
    }

    private fun startNext() {
        while (true) {
            val item = pending.removeFirstOrNull() ?: return
            if (write(item)) {
                inFlight = item
                return
            }
            // Refused: no callback will come for it, so move on.
        }
    }
}
