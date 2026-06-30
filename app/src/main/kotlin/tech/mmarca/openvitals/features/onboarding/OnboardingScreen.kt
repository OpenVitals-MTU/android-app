package tech.mmarca.openvitals.features.onboarding

import tech.mmarca.openvitals.ui.components.OpenVitalsCard

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.core.net.toUri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.domain.model.PermissionGrantMode
import tech.mmarca.openvitals.features.bodyenergy.BodyEnergyCalibrationCard
import tech.mmarca.openvitals.healthconnect.openHealthConnectPermissionSettings
import tech.mmarca.openvitals.ui.components.AppLanguageDropdown
import tech.mmarca.openvitals.ui.components.OpenVitalsButton
import tech.mmarca.openvitals.ui.components.OpenVitalsTonalButton
import tech.mmarca.openvitals.ui.components.FullScreenLoading

private const val HC_PACKAGE = "com.google.android.apps.healthdata"
private const val PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=$HC_PACKAGE"

private enum class OnboardingStep {
    PERMISSIONS,
    BODY_ENERGY,
}

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onOnboardingComplete: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var onboardingStep by rememberSaveable { mutableStateOf(OnboardingStep.PERMISSIONS) }
    val unableToOpenPermissions = stringResource(R.string.onboarding_unable_open_permissions)
    val permissionCategories = viewModel.permissionCategories
    val availablePermissionCategories = permissionCategories.filter { it.available }
    val minimumOnboardingPermissions = viewModel.minimumOnboardingPermissions
    val onboardingPermissions = viewModel.onboardingPermissions
    val manualPermissions = availablePermissionCategories
        .flatMap { it.manualPermissions }
        .toSet()
    val missingMinimumPermissions = minimumOnboardingPermissions - state.grantedPermissions
    val minimumPermissionsGranted = missingMinimumPermissions.isEmpty()
    val missingOnboardingPermissions = onboardingPermissions - state.grantedPermissions
    val missingRequestablePermissions = missingOnboardingPermissions - manualPermissions
    val missingOptionalPermissions = missingRequestablePermissions - minimumOnboardingPermissions
    val missingManualPermissions = missingOnboardingPermissions.intersect(manualPermissions)
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
        contract = androidx.health.connect.client.PermissionController
            .createRequestPermissionResultContract()
    ) { granted ->
        viewModel.onPermissionsResult(granted)
    }

    if (state.isCheckingPermissions) {
        FullScreenLoading()
        return
    }

    fun completeOnboarding() {
        viewModel.completeOnboarding()
        onOnboardingComplete()
    }

    if (onboardingStep == OnboardingStep.BODY_ENERGY) {
        BodyEnergyCalibrationOnboardingStep(
            calibration = state.bodyEnergyCalibration,
            onSave = { calibration ->
                viewModel.saveBodyEnergyCalibration(calibration)
                completeOnboarding()
            },
            onUseAutomatic = {
                viewModel.useAutomaticBodyEnergyCalibration()
                completeOnboarding()
            },
            onSkip = ::completeOnboarding,
        )
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

        Image(
            painter = painterResource(R.drawable.open_vitals_logo_wide),
            contentDescription = null,
            modifier = Modifier
                .width(152.dp)
                .height(104.dp),
            contentScale = ContentScale.Fit,
        )

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
                            Intent(Intent.ACTION_VIEW, PLAY_STORE_URL.toUri())
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

        FeatureCard(
            icon = Icons.Outlined.Info,
            title = stringResource(R.string.health_disclaimer_title),
            body = stringResource(R.string.health_disclaimer_body),
        )

        Spacer(Modifier.height(12.dp))

        // On-device card
        FeatureCard(
            icon = Icons.Outlined.PhoneAndroid,
            title = stringResource(R.string.onboarding_health_connect_title),
            body = stringResource(R.string.onboarding_health_connect_body),
            leadingContent = {
                Image(
                    painter = painterResource(R.drawable.health_connect_logo),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    contentScale = ContentScale.Fit,
                )
            },
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

        OpenVitalsButton(
            onClick = {
                if (minimumPermissionsGranted) {
                    onboardingStep = OnboardingStep.BODY_ENERGY
                } else if (missingMinimumPermissions.isNotEmpty()) {
                    requestPermissions.launch(missingMinimumPermissions)
                } else if (missingManualPermissions.isNotEmpty()) {
                    openManualPermissionSettings()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = minimumPermissionsGranted ||
                missingMinimumPermissions.isNotEmpty() ||
                missingManualPermissions.isNotEmpty(),
        ) {
            Text(
                when {
                    minimumPermissionsGranted -> stringResource(R.string.action_continue)
                    missingMinimumPermissions.isNotEmpty() -> stringResource(R.string.onboarding_grant_all)
                    else -> stringResource(R.string.onboarding_open_required_permissions)
                }
            )
        }

        if (missingOptionalPermissions.isNotEmpty() && minimumPermissionsGranted) {
            OpenVitalsTonalButton(
                onClick = { requestPermissions.launch(missingOptionalPermissions) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                Text(stringResource(R.string.onboarding_grant_remaining))
            }
        }

        if (!minimumPermissionsGranted) {
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
                onGrant = {
                    if (category.available) {
                        val missingPermissions = category.permissions - state.grantedPermissions
                        val requestablePermissions = missingPermissions - category.manualPermissions
                        val manualPermissions = missingPermissions.intersect(category.manualPermissions)
                        when {
                            requestablePermissions.isNotEmpty() -> {
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
private fun BodyEnergyCalibrationOnboardingStep(
    calibration: tech.mmarca.openvitals.domain.preferences.BodyEnergyCalibration,
    onSave: (tech.mmarca.openvitals.domain.preferences.BodyEnergyCalibration) -> Unit,
    onUseAutomatic: () -> Unit,
    onSkip: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(32.dp))
        Image(
            painter = painterResource(R.drawable.open_vitals_logo_wide),
            contentDescription = null,
            modifier = Modifier
                .width(152.dp)
                .height(104.dp),
            contentScale = ContentScale.Fit,
        )
        Spacer(Modifier.height(24.dp))
        BodyEnergyCalibrationCard(
            calibration = calibration,
            showSkipAction = true,
            onSave = onSave,
            onUseAutomatic = onUseAutomatic,
            onSkip = onSkip,
        )
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun FeatureCard(
    icon: ImageVector,
    title: String,
    body: String,
    leadingContent: (@Composable () -> Unit)? = null,
) {
                OpenVitalsCard(
        modifier = Modifier.fillMaxWidth(),

    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            if (leadingContent != null) {
                leadingContent()
                Spacer(Modifier.width(12.dp))
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(12.dp))
            }
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
    onGrant: () -> Unit,
) {
    val grantedCount = category.permissions.count { it in grantedPermissions }
    val granted = category.available && grantedCount == category.permissions.size
    val partial = category.available && grantedCount > 0 && !granted
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
        isManualGrant -> stringResource(R.string.onboarding_status_manual)
        category.required -> stringResource(R.string.onboarding_status_required)
        else -> stringResource(R.string.onboarding_status_optional)
    }
    val categoryTitle = stringResource(category.titleRes)
    val description = if (!category.available && unavailableReasonRes != null) {
        stringResource(unavailableReasonRes)
    } else if (category.manualPermissions.isNotEmpty() && missingManualCount > 0) {
        stringResource(
            R.string.onboarding_category_additional_data_access_manual_note,
            stringResource(category.descriptionRes),
        )
    } else {
        stringResource(category.descriptionRes)
    }

    OpenVitalsCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = if (granted) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = categoryTitle,
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
                }
                if (granted) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = stringResource(R.string.onboarding_status_granted),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                } else if (!category.available) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = stringResource(R.string.onboarding_status_not_supported),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!granted && category.available) {
                OpenVitalsTonalButton(
                    onClick = onGrant,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(
                        when {
                            isManualGrant -> stringResource(R.string.action_open)
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
    OpenVitalsCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.errorContainer,
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
    OpenVitalsCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.errorContainer,
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
        OpenVitalsCard(
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ) {
            Text(
                text = stringResource(R.string.onboarding_health_connect_update),
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
        Spacer(Modifier.height(16.dp))
        OpenVitalsButton(onClick = onInstall, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.onboarding_install_health_connect))
        }
    }
}
