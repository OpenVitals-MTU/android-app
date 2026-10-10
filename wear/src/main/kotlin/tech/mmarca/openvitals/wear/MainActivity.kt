package tech.mmarca.openvitals.wear

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.wear.compose.material3.MaterialTheme

/**
 * The one screen: what the watch is recording, whether the phone can reach
 * it, and which phones may. Asks for the permissions the link and the
 * recorder need; each part starts on its own once granted. The work itself
 * lives in [WearAppService], so closing this screen changes nothing.
 */
class MainActivity : ComponentActivity() {

    private val store by lazy { MetricStore(this) }
    private val trust by lazy { WearTrustStore(this) }

    private var permissionsVersion by mutableStateOf(0)

    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        permissionsVersion++
        WearAppService.startIfPermitted(this)
        // The background grant is asked on its own, and only after the foreground one.
        requestBackgroundHeartRateIfNeeded()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestMissingPermissions()

        setContent {
            MaterialTheme {
                WatchStatusScreen(
                    permissionsVersion = permissionsVersion,
                    hasHeartRate = { WearPermissions.hasHeartRate(this) },
                    hasBluetooth = { WearPermissions.hasBluetooth(this) },
                    readLatest = { store.latest(WearMetrics.HEART_RATE) },
                    readCount = { store.count(WearMetrics.HEART_RATE) },
                    readMinuteCount = { store.count(WearMetrics.SLEEP_MINUTES) },
                    ppgLogAvailable = PpgRawLogger(this).isAvailable,
                    onTogglePpgLog = { WearAppService.togglePpgLog(this) },
                    onGrant = ::requestMissingPermissions,
                    onAllow = { pending ->
                        trust.trust(pending.address, pending.token, pending.name)
                        WearPhoneRequests.cancel(this)
                    },
                    onBlock = { pending ->
                        trust.block(pending.address)
                        WearPhoneRequests.cancel(this)
                    },
                    onForget = { address -> trust.forget(address) },
                    onOpenBluetoothSettings = ::openBluetoothSettings,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Also catches a grant made in the system settings.
        permissionsVersion++
        WearAppService.startIfPermitted(this)
    }

    private fun openBluetoothSettings() {
        // Not every watch has a Bluetooth settings activity; the system settings are the fallback.
        runCatching { startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }
            .onFailure { runCatching { startActivity(Intent(Settings.ACTION_SETTINGS)) } }
    }

    private fun requestMissingPermissions() {
        val wanted = buildList {
            add(WearPermissions.heartRate)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                add(android.Manifest.permission.BLUETOOTH_CONNECT)
            }
            // The link's ongoing notification and the phone requests; the service runs without it, just unseen.
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                add(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        val missing = wanted.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isEmpty()) {
            requestBackgroundHeartRateIfNeeded()
        } else {
            requestPermissions.launch(missing.toTypedArray())
        }
    }

    private fun requestBackgroundHeartRateIfNeeded() {
        val background = WearPermissions.heartRateInBackground ?: return
        if (!WearPermissions.hasHeartRate(this) || WearPermissions.hasHeartRateInBackground(this)) return
        requestPermissions.launch(arrayOf(background))
    }
}
