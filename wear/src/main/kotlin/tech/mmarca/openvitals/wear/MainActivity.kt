package tech.mmarca.openvitals.wear

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import kotlinx.coroutines.delay

/**
 * The one screen: what the watch is recording and whether the phone can
 * reach it. Asks for the permissions the link and the recorder need; each
 * part starts on its own once granted. The work itself lives in
 * [WearAppService], so closing this screen changes nothing.
 */
class MainActivity : ComponentActivity() {

    private val store by lazy { HeartRateStore(this) }
    private val minuteStore by lazy { SleepMinuteStore(this) }

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
                    readLatest = { store.latest() },
                    readCount = { store.count() },
                    readMinuteCount = { minuteStore.count() },
                    ppgLogAvailable = PpgRawLogger(this).isAvailable,
                    readPpgLogging = { WearAppService.ppgLogging },
                    onTogglePpgLog = { WearAppService.togglePpgLog(this) },
                    onGrant = ::requestMissingPermissions,
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

    private fun requestMissingPermissions() {
        val wanted = buildList {
            add(WearPermissions.heartRate)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                add(android.Manifest.permission.BLUETOOTH_CONNECT)
            }
            // The link's ongoing notification; the service runs without it, just unseen.
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

@Composable
private fun WatchStatusScreen(
    permissionsVersion: Int,
    hasHeartRate: () -> Boolean,
    hasBluetooth: () -> Boolean,
    readLatest: () -> WearLinkProtocol.HeartRateSample?,
    readCount: () -> Long,
    readMinuteCount: () -> Long,
    ppgLogAvailable: Boolean,
    readPpgLogging: () -> Boolean,
    onTogglePpgLog: () -> Unit,
    onGrant: () -> Unit,
) {
    var latest by remember { mutableStateOf<WearLinkProtocol.HeartRateSample?>(null) }
    var count by remember { mutableStateOf(0L) }
    var minuteCount by remember { mutableStateOf(0L) }
    var ppgLogging by remember { mutableStateOf(false) }
    val heartRateGranted = remember(permissionsVersion) { hasHeartRate() }
    val bluetoothGranted = remember(permissionsVersion) { hasBluetooth() }

    // The store is written by the service; the screen reads it every few seconds.
    LaunchedEffect(Unit) {
        while (true) {
            latest = readLatest()
            count = readCount()
            minuteCount = readMinuteCount()
            ppgLogging = readPpgLogging()
            delay(3_000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            // The raw PPG spike, debuggable builds only: a long press starts or stops the log.
            .pointerInput(ppgLogAvailable) {
                if (ppgLogAvailable) detectTapGestures(onLongPress = { onTogglePpgLog() })
            }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.open_vitals_launcher_prod),
            contentDescription = null,
            modifier = Modifier.size(40.dp),
        )
        val sample = latest
        Text(
            text = if (!heartRateGranted) {
                stringResource(R.string.status_heart_rate_not_granted)
            } else if (sample == null) {
                stringResource(R.string.status_heart_rate_waiting)
            } else {
                stringResource(R.string.status_heart_rate_bpm, sample.bpm)
            },
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = stringResource(R.string.status_samples_stored, count),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.status_minutes_stored, minuteCount),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
        )
        Text(
            text = if (bluetoothGranted) {
                stringResource(R.string.status_link_listening)
            } else {
                stringResource(R.string.status_link_not_granted)
            },
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
        if (!heartRateGranted || !bluetoothGranted) {
            Button(onClick = onGrant, modifier = Modifier.padding(top = 8.dp)) {
                Text(stringResource(R.string.action_grant))
            }
        }
        if (ppgLogging) {
            Text(
                text = stringResource(R.string.status_ppg_logging),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
