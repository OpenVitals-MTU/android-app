package tech.mmarca.openvitals.features.watches

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.geo.GeoCoordinateError
import tech.mmarca.openvitals.core.geo.GeoCoordinateParseResult
import tech.mmarca.openvitals.devices.garmin.GarminSendFileResult
import tech.mmarca.openvitals.devices.garmin.GarminSendStage
import tech.mmarca.openvitals.devices.garmin.GarminUploadRefusal
import tech.mmarca.openvitals.domain.model.BleSensorDevice
import tech.mmarca.openvitals.ui.components.OpenVitalsButton
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.OpenVitalsTextButton
import tech.mmarca.openvitals.ui.theme.Spacing

/**
 * Types or receives a position and sends it to the watch's saved locations.
 * Nothing is sent until the user taps Send, also when another app shared the
 * position.
 */
@Composable
fun WatchSendPointScreen(
    viewModel: WatchSendPointViewModel,
    onOpenWatches: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        if (state.watches.isEmpty()) {
            NoWatchCard(onOpenWatches)
        } else {
            SendPointForm(state, viewModel)
        }
    }
}

@Composable
private fun SendPointForm(state: WatchSendPointUiState, viewModel: WatchSendPointViewModel) {
    OpenVitalsCard {
        Column(
            modifier = Modifier.padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            WatchChoice(state, onSelect = viewModel::selectWatch)

            if (state.sharedUnreadable) {
                Text(
                    text = stringResource(R.string.settings_watch_point_shared_unreadable),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::updateName,
                enabled = !state.isSending,
                singleLine = true,
                label = { Text(stringResource(R.string.settings_watch_point_name_label)) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            // Text, not Decimal: the field takes letters, a minus and a degree sign.
            OutlinedTextField(
                value = state.coordinates,
                onValueChange = viewModel::updateCoordinates,
                enabled = !state.isSending,
                singleLine = true,
                isError = state.parsed is GeoCoordinateParseResult.Invalid,
                label = { Text(stringResource(R.string.settings_watch_point_coordinates_label)) },
                supportingText = { CoordinatesSupportingText(state.parsed) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    autoCorrectEnabled = false,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            OpenVitalsButton(
                onClick = viewModel::send,
                enabled = state.canSend,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(SendIconSize),
                        strokeWidth = SendProgressStroke,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Place,
                        contentDescription = null,
                        modifier = Modifier.size(SendIconSize),
                    )
                }
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(stringResource(R.string.settings_watch_point_send))
            }

            SendStatus(state)
        }
    }
}

@Composable
private fun NoWatchCard(onOpenWatches: () -> Unit) {
    OpenVitalsCard {
        Column(
            modifier = Modifier.padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(
                text = stringResource(R.string.settings_watch_point_no_watch),
                style = MaterialTheme.typography.bodyMedium,
            )
            OpenVitalsTextButton(onClick = onOpenWatches) {
                Text(stringResource(R.string.settings_watch_point_open_watches))
            }
        }
    }
}

/** One watch is named. Several are a choice. */
@Composable
private fun WatchChoice(state: WatchSendPointUiState, onSelect: (String) -> Unit) {
    Text(
        text = stringResource(R.string.settings_watch_point_watch_label),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (state.watches.size == 1) {
        Text(text = state.watches.single().displayName, style = MaterialTheme.typography.bodyLarge)
        return
    }
    state.watches.forEach { watch -> WatchOption(watch, state, onSelect) }
}

@Composable
private fun WatchOption(watch: BleSensorDevice, state: WatchSendPointUiState, onSelect: (String) -> Unit) {
    val selected = watch.id == state.selectedDeviceId
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                enabled = !state.isSending,
                role = Role.RadioButton,
                onClick = { onSelect(watch.id) },
            ),
    ) {
        // The row is the target; a second one on the button would announce twice.
        RadioButton(selected = selected, onClick = null, enabled = !state.isSending)
        Spacer(modifier = Modifier.width(Spacing.sm))
        Text(text = watch.displayName, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun CoordinatesSupportingText(parsed: GeoCoordinateParseResult?) {
    val text = when (parsed) {
        null -> stringResource(R.string.settings_watch_point_coordinates_hint)
        is GeoCoordinateParseResult.Parsed -> stringResource(
            R.string.settings_watch_point_preview,
            coordinateText(parsed.latitude),
            coordinateText(parsed.longitude),
        )
        is GeoCoordinateParseResult.Invalid -> stringResource(coordinateErrorText(parsed.reason))
    }
    Text(text)
}

/** Progress while sending, then how it ended. Inline: this screen has no snackbar host. */
@Composable
private fun SendStatus(state: WatchSendPointUiState) {
    val stage = state.sendingStage
    val result = state.result
    val (text, isError) = when {
        state.isSending -> sendStageText(stage) to false
        result != null -> sendResultText(result) to (result != GarminSendFileResult.Sent)
        state.radioBusy -> R.string.settings_watch_point_failed_busy to false
        else -> return
    }
    Text(
        text = stringResource(text),
        style = MaterialTheme.typography.bodySmall,
        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
    )
}

private fun coordinateText(degrees: Double): String =
    String.format(java.util.Locale.getDefault(), "%.5f", degrees)

@StringRes
private fun sendStageText(stage: GarminSendStage?): Int = when (stage) {
    GarminSendStage.SENDING -> R.string.settings_watch_point_stage_sending
    GarminSendStage.CONNECTING, null -> R.string.settings_watch_point_stage_connecting
}

@StringRes
internal fun coordinateErrorText(reason: GeoCoordinateError): Int = when (reason) {
    GeoCoordinateError.EMPTY,
    GeoCoordinateError.UNREADABLE -> R.string.settings_watch_point_error_unreadable
    GeoCoordinateError.LATITUDE_RANGE -> R.string.settings_watch_point_error_latitude
    GeoCoordinateError.LONGITUDE_RANGE -> R.string.settings_watch_point_error_longitude
    GeoCoordinateError.MINUTES_RANGE -> R.string.settings_watch_point_error_minutes
    GeoCoordinateError.HEMISPHERE_CONFLICT -> R.string.settings_watch_point_error_hemisphere
}

@StringRes
internal fun sendResultText(result: GarminSendFileResult): Int = when (result) {
    GarminSendFileResult.Sent -> R.string.settings_watch_point_sent
    is GarminSendFileResult.Refused -> refusalText(result.reason)
    GarminSendFileResult.NoAnswer,
    GarminSendFileResult.HandshakeTimeout -> R.string.settings_watch_point_failed_no_answer
    is GarminSendFileResult.Busy -> R.string.settings_watch_point_failed_busy
    GarminSendFileResult.SyncRunning -> R.string.settings_watch_point_failed_sync_running
    GarminSendFileResult.RecordingActive -> R.string.settings_watch_point_failed_recording
    GarminSendFileResult.Unreachable -> R.string.settings_watch_point_failed_unreachable
    GarminSendFileResult.LinkLost -> R.string.settings_watch_point_failed_link_lost
}

@StringRes
private fun refusalText(reason: GarminUploadRefusal): Int = when (reason) {
    GarminUploadRefusal.DUPLICATE -> R.string.settings_watch_point_failed_duplicate
    GarminUploadRefusal.NO_SPACE,
    GarminUploadRefusal.NO_SLOTS -> R.string.settings_watch_point_failed_no_space
    GarminUploadRefusal.UNSUPPORTED -> R.string.settings_watch_point_failed_unsupported
    GarminUploadRefusal.NOT_READY,
    GarminUploadRefusal.SYNC_PAUSED -> R.string.settings_watch_point_failed_not_ready
    GarminUploadRefusal.NOT_WRITEABLE,
    GarminUploadRefusal.TRANSFER_ABORTED,
    GarminUploadRefusal.CRC_MISMATCH,
    GarminUploadRefusal.OFFSET_MISMATCH,
    GarminUploadRefusal.RESEND_LIMIT,
    GarminUploadRefusal.INVALID_REPLY -> R.string.settings_watch_point_failed_rejected
}

/** The button's leading icon and its spinner share a size. */
private val SendIconSize = 18.dp

/** The spinner's stroke, as on the watch screen. */
private val SendProgressStroke = 2.dp
