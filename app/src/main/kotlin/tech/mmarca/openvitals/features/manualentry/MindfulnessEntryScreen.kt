package tech.mmarca.openvitals.features.manualentry

import android.media.MediaPlayer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.coroutines.resume
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.data.model.MindfulnessBackgroundSound
import tech.mmarca.openvitals.data.model.MindfulnessBellSound
import tech.mmarca.openvitals.ui.theme.MindfulnessColor

@Composable
fun MindfulnessEntryScreen(
    viewModel: MindfulnessEntryViewModel,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val requestWritePermissions = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) {
        viewModel.refreshPermission()
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshPermission()
    }
    MindfulnessBellEffect(state.bellEvent)
    MindfulnessBackgroundPreviewEffect(
        event = state.backgroundEvent,
        isTimerRunning = state.isTimerRunning,
    )
    MindfulnessBackgroundEffect(
        sound = state.backgroundSound,
        isPlaying = state.isTimerRunning,
    )

    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
        item {
            MindfulnessTimerCard(
                state = state,
                onDurationChanged = viewModel::updateDurationMinutes,
                onIntervalEnabledChanged = viewModel::updateIntervalEnabled,
                onIntervalChanged = viewModel::updateIntervalMinutes,
                onBellSoundChanged = viewModel::updateBellSound,
                onBackgroundSoundChanged = viewModel::updateBackgroundSound,
                onStartTimer = viewModel::startTimer,
                onStopTimer = viewModel::stopTimer,
                onResumeTimer = viewModel::resumeTimer,
                onSaveTimerSession = viewModel::saveTimerSession,
                onDiscardTimer = viewModel::discardTimer,
                onRequestWritePermission = {
                    requestWritePermissions.launch(state.writePermissions)
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        item {
            MindfulnessManualEntryCard(
                state = state,
                onMinutesChanged = viewModel::updateManualMinutes,
                onAddEntry = viewModel::addManualEntry,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun MindfulnessTimerCard(
    state: MindfulnessEntryUiState,
    onDurationChanged: (String) -> Unit,
    onIntervalEnabledChanged: (Boolean) -> Unit,
    onIntervalChanged: (String) -> Unit,
    onBellSoundChanged: (MindfulnessBellSound) -> Unit,
    onBackgroundSoundChanged: (MindfulnessBackgroundSound) -> Unit,
    onStartTimer: () -> Unit,
    onStopTimer: () -> Unit,
    onResumeTimer: () -> Unit,
    onSaveTimerSession: () -> Unit,
    onDiscardTimer: () -> Unit,
    onRequestWritePermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val canEditTimer = !state.isTimerRunning && !state.isTimerPaused && !state.timerCompleted && !state.isSavingEntry
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MindfulnessEntryHeader(
                state = state,
                onRequestWritePermission = onRequestWritePermission,
            )

            MindfulnessTimerDial(
                state = state,
                canEditTimer = canEditTimer,
                durationMinutesText = state.durationMinutesText,
                onDurationChanged = onDurationChanged,
                modifier = Modifier.fillMaxWidth(),
            )

            IntervalBellRow(
                intervalEnabled = state.intervalEnabled,
                intervalMinutesText = state.intervalMinutesText,
                enabled = canEditTimer,
                onIntervalEnabledChanged = onIntervalEnabledChanged,
                onIntervalChanged = onIntervalChanged,
            )

            BellSoundSelector(
                selectedSound = state.bellSound,
                enabled = canEditTimer,
                onBellSoundChanged = onBellSoundChanged,
            )

            BackgroundSoundSelector(
                selectedSound = state.backgroundSound,
                enabled = canEditTimer,
                onBackgroundSoundChanged = onBackgroundSoundChanged,
            )

            TimerActions(
                state = state,
                onStartTimer = onStartTimer,
                onStopTimer = onStopTimer,
                onResumeTimer = onResumeTimer,
                onSaveTimerSession = onSaveTimerSession,
                onDiscardTimer = onDiscardTimer,
            )

            state.entryError?.let { entryError ->
                Text(
                    text = mindfulnessEntryErrorText(entryError, state.writeErrorMessage),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun MindfulnessEntryHeader(
    state: MindfulnessEntryUiState,
    onRequestWritePermission: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.SelfImprovement,
            contentDescription = null,
            tint = MindfulnessColor,
            modifier = Modifier.size(22.dp),
        )
        Column(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .weight(1f),
        ) {
            Text(
                text = stringResource(R.string.mindfulness_entry_timer_title),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(
                    when {
                        !state.mindfulnessAvailable -> R.string.mindfulness_entry_unavailable
                        state.canWrite -> R.string.mindfulness_entry_subtitle
                        else -> R.string.mindfulness_entry_permission_needed
                    }
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!state.canWrite && state.mindfulnessAvailable && !state.isCheckingPermission) {
            OutlinedButton(onClick = onRequestWritePermission) {
                Text(stringResource(R.string.action_grant))
            }
        }
    }
}

@Composable
private fun BellSoundSelector(
    selectedSound: MindfulnessBellSound,
    enabled: Boolean,
    onBellSoundChanged: (MindfulnessBellSound) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.mindfulness_entry_bell_sound),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(start = 2.dp, end = 24.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(MindfulnessBellSound.entries, key = { it.name }) { sound ->
                FilterChip(
                    selected = sound == selectedSound,
                    onClick = { onBellSoundChanged(sound) },
                    enabled = enabled,
                    label = { Text(stringResource(sound.labelRes())) },
                )
            }
        }
    }
}

@Composable
private fun IntervalBellRow(
    intervalEnabled: Boolean,
    intervalMinutesText: String,
    enabled: Boolean,
    onIntervalEnabledChanged: (Boolean) -> Unit,
    onIntervalChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Checkbox(
            checked = intervalEnabled,
            onCheckedChange = onIntervalEnabledChanged,
            enabled = enabled,
        )
        Text(
            text = stringResource(R.string.mindfulness_entry_interval_bell),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
            value = intervalMinutesText,
            onValueChange = onIntervalChanged,
            enabled = enabled && intervalEnabled,
            singleLine = true,
            label = { Text(stringResource(R.string.mindfulness_entry_interval_minutes)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun BackgroundSoundSelector(
    selectedSound: MindfulnessBackgroundSound,
    enabled: Boolean,
    onBackgroundSoundChanged: (MindfulnessBackgroundSound) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.mindfulness_entry_background_sound),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(start = 2.dp, end = 24.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(MindfulnessBackgroundSound.entries, key = { it.name }) { sound ->
                FilterChip(
                    selected = sound == selectedSound,
                    onClick = { onBackgroundSoundChanged(sound) },
                    enabled = enabled,
                    label = { Text(stringResource(sound.labelRes())) },
                )
            }
        }
    }
}

@Composable
private fun MindfulnessTimerDial(
    state: MindfulnessEntryUiState,
    canEditTimer: Boolean,
    durationMinutesText: String,
    onDurationChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val completedProgress = if (state.totalSeconds > 0) {
        1f - (state.remainingSeconds.toFloat() / state.totalSeconds.toFloat())
    } else {
        0f
    }.coerceIn(0f, 1f)
    val progress by animateFloatAsState(
        targetValue = completedProgress,
        animationSpec = tween(durationMillis = 450),
        label = "MindfulnessTimerProgress",
    )
    val pulseTransition = rememberInfiniteTransition(label = "MindfulnessTimerPulse")
    val runningPulse by pulseTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (state.isTimerRunning) 1f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "MindfulnessTimerPulseAlpha",
    )
    val wigglePhase by pulseTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (state.isTimerRunning) 1f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "MindfulnessTimerWigglePhase",
    )
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val progressColor = MaterialTheme.colorScheme.primary
    val pulseColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f * runningPulse)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(232.dp)
                .aspectRatio(1f),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 14.dp.toPx()
                val inset = strokeWidth / 2f
                val arcSize = size.copy(
                    width = size.width - strokeWidth,
                    height = size.height - strokeWidth,
                )
                if (state.isTimerRunning) {
                    val path = Path()
                    val steps = 96
                    val fullCircle = (PI * 2.0).toFloat()
                    val phase = wigglePhase * fullCircle
                    val baseRadius = (size.minDimension / 2f) * (0.88f + runningPulse * 0.04f)
                    val amplitude = 5.dp.toPx()
                    for (step in 0..steps) {
                        val angle = step.toFloat() / steps.toFloat() * fullCircle
                        val radius = baseRadius +
                            sin(angle * 5f + phase) * amplitude +
                            sin(angle * 3f - phase * 0.7f) * amplitude * 0.45f
                        val x = center.x + cos(angle) * radius
                        val y = center.y + sin(angle) * radius
                        if (step == 0) {
                            path.moveTo(x, y)
                        } else {
                            path.lineTo(x, y)
                        }
                    }
                    path.close()
                    drawPath(path = path, color = pulseColor)
                }
                drawCircle(
                    color = trackColor,
                    radius = (size.minDimension - strokeWidth) / 2f,
                    style = Stroke(width = strokeWidth),
                )
                drawArc(
                    color = progressColor,
                    startAngle = -90f,
                    sweepAngle = progress * 360f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }
            if (canEditTimer) {
                DurationCenterField(
                    value = durationMinutesText,
                    onValueChange = onDurationChanged,
                )
            } else {
                Text(
                    text = formattedTimer(state.remainingSeconds),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun DurationCenterField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = { nextValue ->
                onValueChange(nextValue.filter(Char::isDigit).take(4))
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.headlineLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(112.dp),
        )
        Text(
            text = stringResource(R.string.mindfulness_entry_minutes),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun TimerActions(
    state: MindfulnessEntryUiState,
    onStartTimer: () -> Unit,
    onStopTimer: () -> Unit,
    onResumeTimer: () -> Unit,
    onSaveTimerSession: () -> Unit,
    onDiscardTimer: () -> Unit,
) {
    when {
        state.isTimerRunning -> {
            OutlinedButton(
                onClick = onStopTimer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.mindfulness_entry_stop_timer))
            }
        }
        state.isTimerPaused -> {
            Button(
                onClick = onResumeTimer,
                enabled = !state.isSavingEntry,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.mindfulness_entry_resume_timer))
            }
            Button(
                onClick = onSaveTimerSession,
                enabled = state.canWrite && !state.isSavingEntry,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.mindfulness_entry_save_session))
            }
            OutlinedButton(
                onClick = onDiscardTimer,
                enabled = !state.isSavingEntry,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.mindfulness_entry_discard_timer))
            }
        }
        state.timerCompleted -> {
            Text(
                text = stringResource(R.string.mindfulness_entry_completed),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = onSaveTimerSession,
                enabled = state.canWrite && !state.isSavingEntry,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.mindfulness_entry_save_session))
            }
            OutlinedButton(
                onClick = onDiscardTimer,
                enabled = !state.isSavingEntry,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.mindfulness_entry_discard_timer))
            }
        }
        else -> {
            Button(
                onClick = onStartTimer,
                enabled = !state.isSavingEntry,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.mindfulness_entry_start_timer))
            }
        }
    }
}

@Composable
private fun MindfulnessManualEntryCard(
    state: MindfulnessEntryUiState,
    onMinutesChanged: (String) -> Unit,
    onAddEntry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val enabled = state.canWrite && !state.isSavingEntry && !state.isCheckingPermission
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.mindfulness_entry_manual_title),
                style = MaterialTheme.typography.titleSmall,
            )
            OutlinedTextField(
                value = state.manualMinutesText,
                onValueChange = onMinutesChanged,
                enabled = !state.isSavingEntry,
                singleLine = true,
                label = { Text(stringResource(R.string.mindfulness_entry_minutes)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = onAddEntry,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(R.string.mindfulness_entry_add_minutes),
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun MindfulnessBellEffect(event: MindfulnessBellEvent?) {
    val context = LocalContext.current
    LaunchedEffect(event?.id) {
        val sound = event?.sound ?: return@LaunchedEffect
        val player = runCatching { MediaPlayer.create(context, sound.rawRes()) }.getOrNull()
            ?: return@LaunchedEffect
        var released = false
        fun releasePlayer() {
            if (!released) {
                player.release()
                released = true
            }
        }
        try {
            if (event.previewMillis != null) {
                runCatching { player.start() }
                delay(event.previewMillis)
            } else {
                suspendCancellableCoroutine { continuation ->
                    player.setOnCompletionListener {
                        if (continuation.isActive) continuation.resume(Unit)
                    }
                    player.setOnErrorListener { _, _, _ ->
                        if (continuation.isActive) continuation.resume(Unit)
                        true
                    }
                    continuation.invokeOnCancellation { releasePlayer() }
                    runCatching { player.start() }.onFailure {
                        if (continuation.isActive) continuation.resume(Unit)
                    }
                }
            }
        } finally {
            releasePlayer()
        }
    }
}

@Composable
private fun MindfulnessBackgroundPreviewEffect(
    event: MindfulnessBackgroundEvent?,
    isTimerRunning: Boolean,
) {
    val context = LocalContext.current
    LaunchedEffect(event?.id, isTimerRunning) {
        val backgroundEvent = event ?: return@LaunchedEffect
        if (isTimerRunning) return@LaunchedEffect
        val rawRes = backgroundEvent.sound.rawResOrNull() ?: return@LaunchedEffect
        val player = runCatching { MediaPlayer.create(context, rawRes) }.getOrNull()
            ?: return@LaunchedEffect
        var released = false
        fun releasePlayer() {
            if (!released) {
                player.release()
                released = true
            }
        }
        try {
            player.setVolume(0.38f, 0.38f)
            runCatching { player.start() }
            delay(backgroundEvent.previewMillis)
        } finally {
            releasePlayer()
        }
    }
}

@Composable
private fun MindfulnessBackgroundEffect(
    sound: MindfulnessBackgroundSound,
    isPlaying: Boolean,
) {
    val context = LocalContext.current
    DisposableEffect(sound, isPlaying) {
        val player = if (isPlaying) {
            sound.rawResOrNull()?.let { rawRes ->
                runCatching {
                    MediaPlayer.create(context, rawRes)?.apply {
                        isLooping = true
                        setVolume(0.38f, 0.38f)
                        start()
                    }
                }.getOrNull()
            }
        } else {
            null
        }

        onDispose {
            player?.release()
        }
    }
}

@Composable
private fun mindfulnessEntryErrorText(
    error: MindfulnessEntryError,
    message: String?,
): String = when (error) {
    MindfulnessEntryError.INVALID_TIMER -> stringResource(R.string.mindfulness_entry_invalid_timer)
    MindfulnessEntryError.INVALID_MANUAL_ENTRY -> stringResource(R.string.mindfulness_entry_invalid_manual)
    MindfulnessEntryError.TIMER_TOO_SHORT -> stringResource(R.string.mindfulness_entry_timer_too_short)
    MindfulnessEntryError.MISSING_WRITE_PERMISSION -> stringResource(R.string.mindfulness_entry_permission_needed)
    MindfulnessEntryError.UNAVAILABLE -> stringResource(R.string.mindfulness_entry_unavailable)
    MindfulnessEntryError.WRITE_FAILED -> stringResource(
        R.string.mindfulness_entry_write_failed,
        message ?: stringResource(R.string.unknown_error),
    )
}

private fun MindfulnessBellSound.labelRes(): Int = when (this) {
    MindfulnessBellSound.STRUCK -> R.string.mindfulness_bell_struck
    MindfulnessBellSound.RUBBED -> R.string.mindfulness_bell_rubbed
    MindfulnessBellSound.BRIGHT -> R.string.mindfulness_bell_bright
    MindfulnessBellSound.TEMPLE -> R.string.mindfulness_bell_temple
    MindfulnessBellSound.HARMONY -> R.string.mindfulness_bell_harmony
}

private fun MindfulnessBellSound.rawRes(): Int = when (this) {
    MindfulnessBellSound.STRUCK -> R.raw.bowl_struck
    MindfulnessBellSound.RUBBED -> R.raw.bowl_rubbed
    MindfulnessBellSound.BRIGHT -> R.raw.bowl_bright
    MindfulnessBellSound.TEMPLE -> R.raw.bowl_temple
    MindfulnessBellSound.HARMONY -> R.raw.bowl_harmony
}

private fun MindfulnessBackgroundSound.labelRes(): Int = when (this) {
    MindfulnessBackgroundSound.NONE -> R.string.mindfulness_background_none
    MindfulnessBackgroundSound.BOWL -> R.string.mindfulness_background_bowl
    MindfulnessBackgroundSound.MEDITATION -> R.string.mindfulness_background_meditation
    MindfulnessBackgroundSound.CHIMES -> R.string.mindfulness_background_chimes
    MindfulnessBackgroundSound.DREAMSCAPE -> R.string.mindfulness_background_dreamscape
}

private fun MindfulnessBackgroundSound.rawResOrNull(): Int? = when (this) {
    MindfulnessBackgroundSound.NONE -> null
    MindfulnessBackgroundSound.BOWL -> R.raw.bowl_rubbed
    MindfulnessBackgroundSound.MEDITATION -> R.raw.ambient_meditation
    MindfulnessBackgroundSound.CHIMES -> R.raw.ambient_chimes
    MindfulnessBackgroundSound.DREAMSCAPE -> R.raw.ambient_dreamscape
}

private fun formattedTimer(seconds: Int): String {
    val clampedSeconds = seconds.coerceAtLeast(0)
    val minutes = clampedSeconds / 60
    val remainingSeconds = clampedSeconds % 60
    return "%02d:%02d".format(minutes, remainingSeconds)
}
