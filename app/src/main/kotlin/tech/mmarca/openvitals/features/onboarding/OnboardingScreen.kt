package tech.mmarca.openvitals.features.onboarding

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.data.model.HealthConnectAvailability
import tech.mmarca.openvitals.data.model.PermissionGrantMode
import tech.mmarca.openvitals.healthconnect.openHealthConnectPermissionSettings
import tech.mmarca.openvitals.ui.components.AppLanguageDropdown
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
    val unableToOpenPermissions = stringResource(R.string.onboarding_unable_open_permissions)
    val permissionCategories = viewModel.permissionCategories
    val requiredCategory = permissionCategories.firstOrNull { it.required }
    val openManualPermissionSettings = {
        if (!openHealthConnectPermissionSettings(context)) {
            Toast.makeText(
                context,
                unableToOpenPermissions,
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.checkState()
    }

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

        AppLanguageDropdown(
            selected = state.appLanguage,
            onSelect = viewModel::selectAppLanguage,
            modifier = Modifier.align(Alignment.End),
        )

        Spacer(Modifier.height(16.dp))

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
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        Text(
            text = stringResource(R.string.onboarding_tagline),
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

            HealthConnectAvailability.NEEDS_PLAY_STORE -> {
                NeedsPlayStoreMessage()
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
            title = stringResource(R.string.onboarding_privacy_title),
            body = stringResource(R.string.onboarding_privacy_body),
        )

        Spacer(Modifier.height(12.dp))

        // On-device card
        FeatureCard(
            icon = Icons.Outlined.PhoneAndroid,
            title = stringResource(R.string.onboarding_health_connect_title),
            body = stringResource(R.string.onboarding_health_connect_body),
        )

        Spacer(Modifier.height(24.dp))

        // Permissions section
        Text(
            text = stringResource(R.string.onboarding_permissions_header),
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
            Text(
                if (state.phase1Granted) {
                    stringResource(R.string.action_get_started)
                } else {
                    stringResource(R.string.onboarding_grant_core)
                }
            )
        }

        if (!state.phase2Granted || !state.phase3Granted) {
            FilledTonalButton(
                onClick = { requestPermissions.launch(viewModel.onboardingPermissions) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                Text(stringResource(R.string.onboarding_grant_all))
            }
        }

        if (!state.phase1Granted) {
            Text(
                text = stringResource(R.string.onboarding_core_required),
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
                        val missingPermissions = category.permissions - state.grantedPermissions
                        val requestablePermissions = missingPermissions - category.manualPermissions
                        val manualPermissions = missingPermissions.intersect(category.manualPermissions)
                        when {
                            requestablePermissions.isNotEmpty() -> {
                                if (category.optIn) {
                                    viewModel.enableCycleTracking()
                                }
                                requestPermissions.launch(requestablePermissions)
                            }
                            manualPermissions.isNotEmpty() -> openManualPermissionSettings()
                            category.grantMode == PermissionGrantMode.MANUAL -> openManualPermissionSettings()
                        }
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
    val missingPermissions = category.permissions - grantedPermissions
    val missingRequestableCount = (missingPermissions - category.manualPermissions).size
    val missingManualCount = missingPermissions.intersect(category.manualPermissions).size
    val isManualGrant = category.grantMode == PermissionGrantMode.MANUAL ||
        (missingRequestableCount == 0 && missingManualCount > 0)
    val unavailableReasonRes = category.unavailableReasonRes
    val status = when {
        !category.available -> stringResource(R.string.onboarding_status_not_supported)
        granted -> stringResource(R.string.onboarding_status_granted)
        partial -> stringResource(
            R.string.onboarding_status_partially_granted,
            grantedCount,
            category.permissions.size,
        )
        category.optIn && !cycleTrackingEnabled -> stringResource(R.string.onboarding_status_off)
        isManualGrant -> stringResource(R.string.onboarding_status_manual)
        category.required -> stringResource(R.string.onboarding_status_required)
        else -> stringResource(R.string.onboarding_status_optional)
    }
    val categoryTitle = stringResource(category.titleRes)

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
                    text = if (category.required) {
                        stringResource(R.string.onboarding_required_suffix, categoryTitle)
                    } else {
                        categoryTitle
                    },
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
                    text = if (!category.available && unavailableReasonRes != null) {
                        stringResource(unavailableReasonRes)
                    } else if (category.manualPermissions.isNotEmpty() && missingManualCount > 0) {
                        stringResource(
                            R.string.onboarding_category_additional_data_access_manual_note,
                            stringResource(category.descriptionRes),
                        )
                    } else {
                        stringResource(category.descriptionRes)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            if (granted) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = stringResource(R.string.onboarding_status_granted),
                    tint = MaterialTheme.colorScheme.primary,
                )
            } else if (!category.available) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = stringResource(R.string.onboarding_status_not_supported),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                FilledTonalButton(onClick = onGrant) {
                    Text(
                        when {
                            isManualGrant -> stringResource(R.string.action_open)
                            category.optIn && !cycleTrackingEnabled -> stringResource(R.string.action_enable)
                            partial -> stringResource(R.string.action_review)
                            else -> stringResource(R.string.action_grant)
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
            text = stringResource(R.string.onboarding_health_connect_not_supported),
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

@Composable
private fun NeedsPlayStoreMessage() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Text(
            text = stringResource(R.string.onboarding_health_connect_needs_play_store),
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
                text = stringResource(R.string.onboarding_health_connect_update),
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onInstall, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.onboarding_install_health_connect))
        }
    }
}
