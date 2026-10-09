package tech.mmarca.openvitals.wearlink

/**
 * Runs [onTimeout] once [millis] after the last [reset] unless [cancel]led
 * first. A Bluetooth socket has no read timeout; closing it from this
 * thread is what unblocks a read that the peer never answers.
 */
class WearLinkWatchdog(
    millis: Long,
    private val onTimeout: () -> Unit,
) {
    private val lock = Object()
    private var deadline = System.currentTimeMillis() + millis
    private var cancelled = false
    private var fired = false

    private val thread = Thread({
        synchronized(lock) {
            while (!cancelled) {
                val remaining = deadline - System.currentTimeMillis()
                if (remaining <= 0) {
                    fired = true
                    break
                }
                lock.wait(remaining)
            }
        }
        if (fired) onTimeout()
    }, "WearLinkWatchdog").apply {
        isDaemon = true
        start()
    }

    /** True once the timeout ran. */
    val hasFired: Boolean
        get() = synchronized(lock) { fired }

    /** Pushes the deadline out to [millis] from now. */
    fun reset(millis: Long) {
        synchronized(lock) {
            deadline = System.currentTimeMillis() + millis
            lock.notifyAll()
        }
    }

    fun cancel() {
        synchronized(lock) {
            cancelled = true
            lock.notifyAll()
        }
    }
}
