package tech.mmarca.openvitals.features.devicesync.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import java.io.IOException
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

/** A socket that failed to connect still holds its RFCOMM channel until it is closed. */
class RfcommClientTest {

    private val phone = "AA:BB:CC:DD:EE:FF"
    private val socket = mockk<BluetoothSocket> { every { close() } just runs }
    private val device = mockk<BluetoothDevice> {
        every { createRfcommSocketToServiceRecord(SyncBluetooth.APP_UUID) } returns socket
    }
    private val adapter = mockk<BluetoothAdapter> {
        every { getRemoteDevice(phone) } returns device
        every { isDiscovering } returns false
    }

    @Test
    fun `a failed connect closes its socket`() {
        every { socket.connect() } throws IOException("read failed, socket might closed or timeout")

        assertThrows(IOException::class.java) { RfcommClient(adapter).connect(phone) }

        verify(exactly = 1) { socket.close() }
    }

    @Test
    fun `a connected socket is handed over open`() {
        every { socket.connect() } just runs

        assertSame(socket, RfcommClient(adapter).connect(phone))

        verify(exactly = 0) { socket.close() }
    }
}
