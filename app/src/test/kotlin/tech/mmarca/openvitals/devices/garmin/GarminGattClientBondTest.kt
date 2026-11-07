package tech.mmarca.openvitals.devices.garmin

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

/** The bond is the Garmin link's only authentication: the GFDI handshake echoes what it is sent. */
class GarminGattClientBondTest {

    // Not "address": inside the adapter mock that name is the adapter's own getAddress().
    private val watchAddress = "AA:BB:CC:DD:EE:FF"
    private val device = mockk<BluetoothDevice>()
    private val adapter = mockk<BluetoothAdapter> {
        every { isEnabled } returns true
        every { getRemoteDevice(watchAddress) } returns device
    }
    private val context = mockk<Context> {
        every { getSystemService(Context.BLUETOOTH_SERVICE) } returns
            mockk<BluetoothManager> { every { this@mockk.adapter } returns this@GarminGattClientBondTest.adapter }
    }

    @Test
    fun `a watch that is not bonded is never connected to`() = runTest {
        for (state in listOf(BluetoothDevice.BOND_NONE, BluetoothDevice.BOND_BONDING)) {
            every { device.bondState } returns state

            val error = runCatching { GarminGattClient(context, watchAddress).connect(onFrame = {}) }
                .exceptionOrNull()

            assertTrue("got $error", error is GarminGattClientException)
            assertTrue("got ${error?.message}", error?.message.orEmpty().contains("no longer paired"))
        }
        verify(exactly = 0) { device.connectGatt(any(), any(), any(), any<Int>()) }
    }
}
