package tech.mmarca.openvitals.devices.xiaomi

import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.ParcelUuid
import androidx.core.content.IntentCompat
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Where the system delivers the scale's broadcasts, with the app open or
 * not. It only unpacks them: the work, and the time it may take inside a
 * broadcast, belong to [ScaleWeighInIngest].
 */
@AndroidEntryPoint
class ScaleScanReceiver : BroadcastReceiver() {

    @Inject lateinit var ingest: ScaleWeighInIngest

    override fun onReceive(context: Context, intent: Intent) {
        // A scan the system gave up on carries an error and no results. The listener re-arms on its own schedule.
        val results = IntentCompat.getParcelableArrayListExtra(
            intent,
            BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT,
            ScanResult::class.java,
        )
        if (results.isNullOrEmpty()) return
        val adverts = results.mapNotNull { result ->
            val serviceData = result.scanRecord?.getServiceData(ServiceUuid) ?: return@mapNotNull null
            ScaleAdvert(address = result.device.address, serviceData = serviceData)
        }
        if (adverts.isEmpty()) return
        val pendingResult = goAsync()
        ingest.onAdverts(adverts) { pendingResult.finish() }
    }

    private companion object {
        val ServiceUuid: ParcelUuid = ParcelUuid.fromString(S400Beacon.SERVICE_UUID)
    }
}
