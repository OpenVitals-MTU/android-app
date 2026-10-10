package tech.mmarca.openvitals.wear

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import tech.mmarca.openvitals.wearlink.WearLinkProtocol

/** The status screen: recording, link, and the phones that may connect. */
@Composable
internal fun WatchStatusScreen(
    permissionsVersion: Int,
    hasHeartRate: () -> Boolean,
    hasBluetooth: () -> Boolean,
    readLatest: () -> WearLinkProtocol.HeartRateSample?,
    readCount: () -> Long,
    readMinuteCount: () -> Long,
    ppgLogAvailable: Boolean,
    onTogglePpgLog: () -> Unit,
    onGrant: () -> Unit,
    onAllow: (PendingPhone) -> Unit,
    onBlock: (PendingPhone) -> Unit,
    onForget: (String) -> Unit,
    onOpenBluetoothSettings: () -> Unit,
) {
    var latest by remember { mutableStateOf<WearLinkProtocol.HeartRateSample?>(null) }
    var count by remember { mutableStateOf(0L) }
    var minuteCount by remember { mutableStateOf(0L) }
    val link by WearLinkState.state.collectAsState()
    val heartRateGranted = remember(permissionsVersion) { hasHeartRate() }
    val bluetoothGranted = remember(permissionsVersion) { hasBluetooth() }

    // The stores are written by the service; the screen reads them every few seconds.
    LaunchedEffect(Unit) {
        while (true) {
            latest = readLatest()
            count = readCount()
            minuteCount = readMinuteCount()
            WearLinkState.expirePending(System.currentTimeMillis())
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
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.open_vitals_launcher_prod),
            contentDescription = null,
            modifier = Modifier.size(40.dp),
        )
        val sample = latest
        Line(
            text = when {
                !heartRateGranted -> stringResource(R.string.status_heart_rate_not_granted)
                sample == null -> stringResource(R.string.status_heart_rate_waiting)
                else -> stringResource(R.string.status_heart_rate_bpm, sample.bpm)
            },
            title = true,
        )
        Line(stringResource(R.string.status_samples_stored, count))
        Line(stringResource(R.string.status_minutes_stored, minuteCount))
        Line(
            stringResource(
                when (link.bedtimeHold) {
                    BedtimeHold.OFF -> R.string.status_bedtime_off
                    BedtimeHold.HOLDING -> R.string.status_bedtime_holding
                    BedtimeHold.PAUSED_OFF_WRIST -> R.string.status_bedtime_paused_off_wrist
                    BedtimeHold.PAUSED_CHARGING -> R.string.status_bedtime_paused_charging
                },
            ),
        )
        Line(
            text = when {
                !bluetoothGranted -> stringResource(R.string.status_link_not_granted)
                !link.bluetoothOn -> stringResource(R.string.status_link_bluetooth_off)
                link.listening -> stringResource(R.string.status_link_listening)
                else -> stringResource(R.string.status_link_starting)
            },
        )
        if (!heartRateGranted || !bluetoothGranted) {
            Button(onClick = onGrant, modifier = Modifier.padding(top = 8.dp)) {
                Text(stringResource(R.string.action_grant))
            }
        }
        PhoneCards(link, onAllow, onBlock, onForget, onOpenBluetoothSettings)
        if (link.ppgLogging) Line(stringResource(R.string.status_ppg_logging))
    }
}

@Composable
internal fun Line(text: String, title: Boolean = false) {
    Text(
        text = text,
        style = if (title) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 4.dp),
    )
}
