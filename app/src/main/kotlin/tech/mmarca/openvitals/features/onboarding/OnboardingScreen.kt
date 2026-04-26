package tech.mmarca.openvitals.features.onboarding

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.data.model.HealthConnectAvailability
import tech.mmarca.openvitals.ui.components.FullScreenLoading

private const val HC_PACKAGE = "com.google.android.apps.healthdata"
private const val PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=$HC_PACKAGE"

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onOnboardingComplete: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val permissionCategories = viewModel.permissionCategories
    val requiredCategory = permissionCategories.firstOrNull { it.required }

    val requestPermissions = rememberLauncherForActivityResult(
        contract = viewModel.onboardingPermissions.let {
            androidx.health.connect.client.PermissionController
                .createRequestPermissionResultContract()
        }
    ) { granted ->
        viewModel.onPermissionsResult(granted)
    }

    if (state.isCheckingPermissions) {
        FullScreenLoading()
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(32.dp))

        // App icon placeholder
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Favorite,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(40.dp),
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "OpenVitals",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        Text(
            text = "Your health data, on your device",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )

        Spacer(Modifier.height(32.dp))

        when (state.availability) {
            HealthConnectAvailability.NOT_SUPPORTED -> {
                UnavailableMessage()
                return@Column
            }

            HealthConnectAvailability.NEEDS_PROVIDER_UPDATE -> {
                NeedsUpdateMessage(
                    onInstall = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_STORE_URL))
                        )
                    }
                )
                return@Column
            }

            HealthConnectAvailability.AVAILABLE -> Unit
        }

        // Privacy card
        FeatureCard(
            icon = Icons.Outlined.Lock,
            title = "Privacy first",
            body = "No account required. Data stays on your device. " +
                    "No cloud upload, no analytics, no ads.",
        )

        Spacer(Modifier.height(12.dp))

        // On-device card
        FeatureCard(
            icon = Icons.Outlined.PhoneAndroid,
            title = "Powered by Health Connect",
            body = "Reads from Android's secure on-device health store. " +
                    "Works with all data imported into Health Connect."
        )

        Spacer(Modifier.height(24.dp))

        // Permissions section
        Text(
            text = "PERMISSIONS",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
        )

        Button(
            onClick = {
                if (state.phase1Granted) onOnboardingComplete()
                else requiredCategory?.let { requestPermissions.launch(it.permissions) }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = state.phase1Granted || requiredCategory != null,
        ) {
            Text(if (state.phase1Granted) "Get started" else "Grant only core permissions")
        }

        if (!state.phase2Granted || !state.phase3Granted) {
            FilledTonalButton(
                onClick = { requestPermissions.launch(viewModel.onboardingPermissions) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                Text("Grant all dashboard permissions")
            }
        }

        if (!state.phase1Granted) {
            Text(
                text = "Core permissions are required to show your dashboard.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        Spacer(Modifier.height(24.dp))

        permissionCategories.forEach { category ->
            PermissionCategoryRow(
                category = category,
                grantedPermissions = state.grantedPermissions,
                cycleTrackingEnabled = state.cycleTrackingEnabled,
                onGrant = {
                    if (category.available) {
                        if (category.optIn) {
                            viewModel.enableCycleTracking()
                        }
                        requestPermissions.launch(category.permissions)
                    }
                },
            )
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun FeatureCard(icon: ImageVector, title: String, body: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun PermissionCategoryRow(
    category: OnboardingPermissionCategory,
    grantedPermissions: Set<String>,
    cycleTrackingEnabled: Boolean,
    onGrant: () -> Unit,
) {
    val grantedCount = category.permissions.count { it in grantedPermissions }
    val optInEnabled = !category.optIn || cycleTrackingEnabled
    val granted = category.available && optInEnabled && grantedCount == category.permissions.size
    val partial = category.available && optInEnabled && grantedCount > 0 && !granted
    val status = when {
        !category.available -> "Not supported"
        granted -> "Granted"
        partial -> "$grantedCount/${category.permissions.size} granted"
        category.optIn && !cycleTrackingEnabled -> "Off"
        category.required -> "Required"
        else -> "Optional"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (granted)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else
                MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (category.required) "${category.title} (required)" else category.title,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (granted)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = category.unavailableReason?.takeIf { !category.available } ?: category.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            if (granted) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = "Granted",
                    tint = MaterialTheme.colorScheme.primary,
                )
            } else if (!category.available) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = "Not supported",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                FilledTonalButton(onClick = onGrant) {
                    Text(
                        when {
                            category.optIn && !cycleTrackingEnabled -> "Enable"
                            partial -> "Review"
                            else -> "Grant"
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun UnavailableMessage() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Text(
            text = "Health Connect is not supported on this device.",
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

@Composable
private fun NeedsUpdateMessage(onInstall: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            ),
        ) {
            Text(
                text = "Health Connect needs to be installed or updated to use this app.",
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onInstall, modifier = Modifier.fillMaxWidth()) {
            Text("Install Health Connect")
        }
    }
}
