package tech.mmarca.openvitals.features.watches

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.OpenVitalsCardStyle
import tech.mmarca.openvitals.ui.theme.Spacing

/**
 * Watch notification forwarding: the master switch, the two gates and the
 * per-app blocklist. A blocklist, so a new messaging app is not silent.
 */
@Composable
fun WatchNotificationAppsScreen(viewModel: WatchNotificationAppsViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Resolving every app's label is a round trip per app, so it waits for this screen.
    LaunchedEffect(Unit) { viewModel.loadApps() }

    // Access is granted on a system screen with no callback; re-read on resume.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    if (state.showDisclosure) {
        DisclosureDialog(
            onAccept = viewModel::acceptDisclosure,
            onDecline = viewModel::declineDisclosure,
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        item(key = "master") {
            MasterSwitchCard(
                state = state,
                onToggle = viewModel::setEnabled,
                onOpenAccessSettings = viewModel::openAccessSettings,
            )
        }

        if (state.active) {
            item(key = "intro") {
                Text(
                    text = stringResource(R.string.settings_watch_notifications_apps_intro),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                )
            }
            when {
                state.loadingApps -> item(key = "loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Spacing.xxxl),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                state.apps.isEmpty() -> item(key = "empty") {
                    Text(
                        text = stringResource(R.string.settings_watch_notifications_apps_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(Spacing.lg),
                    )
                }
                else -> items(state.apps, key = { it.packageName }) { app ->
                    AppRow(
                        app = app,
                        onSendsChanged = { sends ->
                            // Inverted: the switch reads "sends", the stored state is the blocklist.
                            viewModel.setBlocked(app.packageName, blocked = !sends)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun MasterSwitchCard(
    state: WatchNotificationsUiState,
    onToggle: (Boolean) -> Unit,
    onOpenAccessSettings: () -> Unit,
) {
    OpenVitalsCard {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.NotificationsActive,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(Spacing.lg))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_watch_notifications_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.settings_watch_notifications_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.width(Spacing.lg))
                Switch(
                    checked = state.active,
                    onCheckedChange = if (state.loading) null else onToggle,
                    enabled = !state.loading,
                )
            }

            if (!state.accessGranted && !state.loading) {
                Spacer(modifier = Modifier.padding(top = Spacing.md))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Spacer(modifier = Modifier.width(Spacing.lg))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_watch_notifications_grant),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = stringResource(
                                R.string.settings_watch_notifications_grant_body,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    FilledTonalButton(onClick = onOpenAccessSettings) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                            contentDescription = null,
                        )
                    }
                }
            }

            if (state.active) {
                Spacer(modifier = Modifier.padding(top = Spacing.sm))
                Text(
                    text = if (state.blockedCount == 0) {
                        stringResource(R.string.settings_watch_notifications_apps_none)
                    } else {
                        pluralStringResource(
                            R.plurals.settings_watch_notifications_apps_blocked,
                            state.blockedCount,
                            state.blockedCount,
                        )
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AppRow(
    app: WatchNotificationApp,
    onSendsChanged: (Boolean) -> Unit,
) {
    OpenVitalsCard(style = OpenVitalsCardStyle.Neutral) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.width(Spacing.lg))
            Switch(
                checked = !app.blocked,
                onCheckedChange = onSendsChanged,
            )
        }
    }
}

/** The prominent disclosure Google Play requires before notification access. Dismissing declines. */
@Composable
private fun DisclosureDialog(
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDecline,
        icon = { Icon(imageVector = Icons.Outlined.PrivacyTip, contentDescription = null) },
        title = {
            Text(stringResource(R.string.settings_watch_notifications_disclosure_title))
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.settings_watch_notifications_disclosure_body))
            }
        },
        confirmButton = {
            FilledTonalButton(onClick = onAccept) {
                Text(stringResource(R.string.settings_watch_notifications_disclosure_accept))
            }
        },
        dismissButton = {
            TextButton(onClick = onDecline) {
                Text(stringResource(R.string.settings_watch_notifications_disclosure_decline))
            }
        },
    )
}
