package tech.mmarca.openvitals.features.manualentry.activity.recording

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.Duration
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.model.CoMapsNavigationSnapshot
import tech.mmarca.openvitals.domain.model.CoMapsNavigationState
import tech.mmarca.openvitals.domain.model.CoMapsTurnKind
import tech.mmarca.openvitals.domain.model.coMapsNavigationDirection
import tech.mmarca.openvitals.domain.model.coMapsReadableDirection
import tech.mmarca.openvitals.domain.model.coMapsTurnKindForDirection
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.OpenVitalsIconButton
import tech.mmarca.openvitals.ui.components.OpenVitalsOutlinedButton
import tech.mmarca.openvitals.ui.theme.Emphasis

/**
 * CoMaps plans and navigates; OpenVitals records. Everything here READS what
 * CoMaps is already doing — and every state it can be in, including the four
 * that say "no guidance", is an ordinary one: the recording never depends on
 * any of them, so none of them shouts.
 */

/**
 * CoMaps' own guidance green, which the overlay borrows so a turn instruction
 * reads as CoMaps' voice rather than as one of the app's own metrics.
 */
private val CoMapsGuidanceGreen = Color(0xFF4F7F50)
private val CoMapsGuidanceGreenDark = Color(0xFF3E6A43)

/**
 * How far to rotate the forward arrow for each turn. The icon points right at
 * 0 degrees, so straight ahead is a quarter turn back.
 */
internal fun coMapsTurnRotationDegrees(kind: CoMapsTurnKind): Float = when (kind) {
    CoMapsTurnKind.STRAIGHT -> -90f
    CoMapsTurnKind.RIGHT -> 0f
    CoMapsTurnKind.SLIGHT_RIGHT -> -45f
    CoMapsTurnKind.SHARP_RIGHT -> 35f
    CoMapsTurnKind.LEFT -> 180f
    CoMapsTurnKind.SLIGHT_LEFT -> -135f
    CoMapsTurnKind.SHARP_LEFT -> 145f
    CoMapsTurnKind.U_TURN -> 90f
    CoMapsTurnKind.ROUNDABOUT -> 45f
    CoMapsTurnKind.FINISH -> 0f
    CoMapsTurnKind.UNKNOWN -> -90f
}

/**
 * One live reading turned into the strings the overlay prints — every
 * fallback chosen, every list joined, here rather than in a layout.
 *
 * A snapshot is mostly holes: CoMaps sends what it has, and what it has
 * depends on the route, the mode and the moment. Deciding *which* hole a
 * field falls back into is the whole job.
 */
internal data class CoMapsGuidanceDisplay(
    val turnKind: CoMapsTurnKind,
    val turnDistance: String,
    val primaryStreet: String,
    val overlaySecondary: String,
    val overlayFooter: String,
)

@Composable
internal fun buildCoMapsGuidanceDisplay(snapshot: CoMapsNavigationSnapshot): CoMapsGuidanceDisplay {
    val direction = coMapsNavigationDirection(snapshot)
    val readableDirection = coMapsReadableDirection(direction)

    // The one distance the overlay shows, in the order the runner cares
    // about it: the turn ahead first, then the destination, then whatever
    // stop is next.
    val overlayDistance = firstNonEmpty(
        snapshot.distanceToTurn,
        snapshot.distanceToTarget,
        snapshot.distanceToNextStop,
    )
    val primaryStreet = firstNonEmpty(
        snapshot.nextStreet,
        snapshot.currentStreet,
        snapshot.sessionState,
    )

    // The percentage is CoMaps' own progress along its own route, and it
    // arrives as a fraction of a percent. Nobody reads decimals mid-run.
    val completion = snapshot.completionPercent?.let { percent ->
        stringResource(R.string.recording_comaps_completion, percent.toInt())
    }.orEmpty()
    val nextStopTime = snapshot.timeToNextStopSeconds?.let { seconds ->
        formatRecordingElapsed(Duration.ofSeconds(seconds.coerceAtLeast(0).toLong()))
    }.orEmpty()
    val totalTime = snapshot.totalTimeSeconds?.let { seconds ->
        formatRecordingElapsed(Duration.ofSeconds(seconds.coerceAtLeast(0).toLong()))
    }.orEmpty()

    return CoMapsGuidanceDisplay(
        turnKind = coMapsTurnKindForDirection(direction),
        turnDistance = overlayDistance.ifEmpty { "--" },
        primaryStreet = primaryStreet,
        overlaySecondary = joinPresent(
            readableDirection,
            // The current street is only worth a second line when it is not
            // already the headline.
            snapshot.currentStreet.takeIf { it != primaryStreet }.orEmpty(),
            snapshot.distanceToTarget
                .takeIf { it.isNotEmpty() && it != overlayDistance }
                ?.let { stringResource(R.string.recording_comaps_destination_with_distance, it) }
                .orEmpty(),
        ),
        overlayFooter = joinPresent(
            completion,
            nextStopTime
                .takeIf { it.isNotEmpty() }
                ?.let { stringResource(R.string.recording_comaps_next_stop_with_time, it) }
                .orEmpty(),
            totalTime
                .takeIf { it.isNotEmpty() }
                ?.let { stringResource(R.string.recording_comaps_route_time_with_duration, it) }
                .orEmpty(),
        ),
    )
}

private fun firstNonEmpty(vararg candidates: String): String =
    candidates.firstOrNull { it.isNotEmpty() }.orEmpty()

private fun joinPresent(vararg parts: String): String =
    parts.filter { it.isNotEmpty() }.joinToString(" - ")

/**
 * The whole live-guidance surface of the recording screen, dispatching on
 * what CoMaps can currently tell us.
 *
 * [CoMapsNavigationState.Disabled] renders NOTHING — the user never asked for
 * this, and a panel explaining that a feature they did not switch on is off
 * would be the loudest thing on the screen.
 */
@Composable
internal fun CoMapsGuidancePanel(
    state: CoMapsNavigationState,
    onRequestPermission: () -> Unit,
    onPlanInCoMaps: (() -> Unit)?,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
    startGateHint: Boolean = false,
) {
    when (state) {
        CoMapsNavigationState.Disabled -> Unit
        is CoMapsNavigationState.Active ->
            // The compact turn strip, not a full breakdown: mid-run the only
            // question is "which way do I turn?". The rest is kept with the
            // activity and read afterwards.
            CoMapsMapGuidanceOverlay(
                snapshot = state.snapshot,
                onDismiss = onDismiss,
                modifier = modifier,
            )
        else -> CoMapsNavigationContextCard(
            state = state,
            onRequestPermission = onRequestPermission,
            onPlanInCoMaps = onPlanInCoMaps,
            onDismiss = onDismiss,
            startGateHint = startGateHint,
            modifier = modifier,
        )
    }
}

/**
 * The titled card that carries the "no guidance right now" answers. Each one
 * says, in its own words, that the recording carries on — because it does.
 */
@Composable
private fun CoMapsNavigationContextCard(
    state: CoMapsNavigationState,
    onRequestPermission: () -> Unit,
    onPlanInCoMaps: (() -> Unit)?,
    onDismiss: (() -> Unit)?,
    startGateHint: Boolean,
    modifier: Modifier = Modifier,
) {
    // Two of these the user can act on, and each gets its own button: the
    // permission, and "CoMaps is here but guiding nobody" — which is not a
    // fact about their phone, it is an invitation to go set a route. The
    // remaining states (no app, no provider, a failed query) are facts, and
    // get none.
    val message = when (state) {
        CoMapsNavigationState.PermissionMissing ->
            stringResource(R.string.recording_comaps_permission_missing)
        CoMapsNavigationState.AppUnavailable ->
            stringResource(R.string.recording_comaps_app_unavailable)
        CoMapsNavigationState.ProviderUnavailable ->
            stringResource(R.string.recording_comaps_provider_unavailable)
        CoMapsNavigationState.NotNavigating ->
            stringResource(R.string.recording_comaps_not_navigating)
        is CoMapsNavigationState.Error ->
            stringResource(R.string.recording_comaps_error)
        else -> ""
    }
    // Offered only for "here but guiding nobody" — the state the button
    // answers. There is nothing to plan in an app that is not installed.
    val planAction = onPlanInCoMaps.takeIf { state == CoMapsNavigationState.NotNavigating }

    OpenVitalsCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Map,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = stringResource(R.string.recording_comaps_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                )
                if (onDismiss != null) {
                    OpenVitalsIconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.action_close),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // Start is gated on this card being answered: the hint says what
            // Start is waiting for, where the user would otherwise wonder why
            // nothing happened.
            if (startGateHint) {
                Text(
                    text = stringResource(R.string.recording_comaps_start_gate_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (state == CoMapsNavigationState.PermissionMissing) {
                OpenVitalsOutlinedButton(
                    onClick = onRequestPermission,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.recording_comaps_permission_action))
                }
            }
            // Reachable mid-recording on purpose. Deciding to follow a route
            // is not something a user only does before pressing Start.
            if (planAction != null) {
                OpenVitalsOutlinedButton(
                    onClick = planAction,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Map,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = stringResource(R.string.recording_comaps_plan_action),
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
        }
    }
}

/** The turn, big enough to read at arm's length on a bike, in CoMaps' green. */
@Composable
internal fun CoMapsMapGuidanceOverlay(
    snapshot: CoMapsNavigationSnapshot,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
) {
    val display = buildCoMapsGuidanceDisplay(snapshot)

    Surface(
        color = CoMapsGuidanceGreen,
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxHeight()
                    .width(112.dp)
                    .background(CoMapsGuidanceGreenDark)
                    .padding(horizontal = 10.dp, vertical = 12.dp),
            ) {
                CoMapsTurnArrow(kind = display.turnKind)
                Text(
                    text = display.turnDistance,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = display.primaryStreet,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (onDismiss != null) {
                        OpenVitalsIconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = stringResource(R.string.action_close),
                                tint = Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
                if (display.overlaySecondary.isNotEmpty()) {
                    Text(
                        text = display.overlaySecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = Emphasis.strong),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                if (display.overlayFooter.isNotEmpty()) {
                    Text(
                        text = display.overlayFooter,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}

/**
 * One arrow, rotated to the turn it stands for — except the last "turn" of a
 * route, which is not a turn at all but an arrival, and gets a flag.
 */
@Composable
private fun CoMapsTurnArrow(kind: CoMapsTurnKind) {
    if (kind == CoMapsTurnKind.FINISH) {
        Icon(
            imageVector = Icons.Outlined.Flag,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(52.dp),
        )
    } else {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .size(52.dp)
                .rotate(coMapsTurnRotationDegrees(kind)),
        )
    }
}
