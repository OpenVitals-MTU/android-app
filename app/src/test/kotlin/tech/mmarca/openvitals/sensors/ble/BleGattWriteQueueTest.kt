package tech.mmarca.openvitals.sensors.ble

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BleGattWriteQueueTest {

    private val started = mutableListOf<String>()
    private val refused = mutableSetOf<String>()
    private val queue = BleGattWriteQueue<String> { item ->
        if (item in refused) false else started.add(item)
    }

    @Test
    fun `only one write is in flight at a time`() {
        queue.enqueue(listOf("power", "speed-cadence"))

        // The old loop fired both at once and Android refused the second.
        assertEquals(listOf("power"), started)
    }

    @Test
    fun `the next write starts when the stack reports the last one done`() {
        queue.enqueue(listOf("power", "speed-cadence"))

        assertEquals("power", queue.onWriteFinished())
        assertEquals(listOf("power", "speed-cadence"), started)
        assertEquals("speed-cadence", queue.onWriteFinished())
        assertNull(queue.onWriteFinished())
    }

    @Test
    fun `a refused write is skipped, because no callback will come for it`() {
        refused += "power"

        queue.enqueue(listOf("power", "speed-cadence"))

        assertEquals(listOf("speed-cadence"), started)
        assertEquals("speed-cadence", queue.onWriteFinished())
    }

    @Test
    fun `items added while a write is in flight wait their turn`() {
        queue.enqueue(listOf("power"))
        queue.enqueue(listOf("heart-rate"))

        assertEquals(listOf("power"), started)
        queue.onWriteFinished()
        assertEquals(listOf("power", "heart-rate"), started)
    }

    @Test
    fun `clear drops what is pending and in flight`() {
        queue.enqueue(listOf("power", "speed-cadence"))

        queue.clear()

        assertNull(queue.onWriteFinished())
        assertEquals(listOf("power"), started)
    }
}
