package tech.mmarca.openvitals.features.devicesync.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

/** Bridges the discovery broadcasts to callbacks. Registered only while a discovery runs. */
internal class BluetoothDiscoveryReceiver(
    private val onFound: (BluetoothDevice) -> Unit,
    private val onFinished: () -> Unit,
) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            BluetoothDevice.ACTION_FOUND -> {
                val device =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(
                            BluetoothDevice.EXTRA_DEVICE,
                            BluetoothDevice::class.java,
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                device?.let(onFound)
            }
            BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> onFinished()
        }
    }
}
