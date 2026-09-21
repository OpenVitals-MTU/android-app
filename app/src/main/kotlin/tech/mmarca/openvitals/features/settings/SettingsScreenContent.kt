package tech.mmarca.openvitals.features.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.BuildConfig
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.PermissionCallout
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.theme.LayoutMetrics

internal fun LazyListScope.settingsScreenContent(
    section: SettingsSection?,
    state: SettingsUiState,
    viewModel: SettingsViewModel,
    actions: SettingsScreenActions,
) {
    when (section) {
        null -> settingsRootCards(actions)
        SettingsSection.VITALS -> {
            item { SectionHeader(stringResource(section.titleRes)) }
            item {
                BloodPressureGuidelineCard(
                    selected = state.bloodPressureGuideline,
                    onSelect = viewModel::setBloodPressureGuideline,
                    modifier = Modifier.padding(horizontal = LayoutMetrics.screenGutter),
                )
            }
        }
        SettingsSection.HEALTH_CONNECT -> healthConnectCards(section, state, viewModel, actions)
        SettingsSection.DEBUG_DIAGNOSTICS -> {
            if (BuildConfig.OPENVITALS_DIAGNOSTICS) {
                item { SectionHeader(stringResource(section.titleRes)) }
                item {
                    DebugDiagnosticsCard(
                        onSaveLogs = actions.onSaveDebugLogs,
                        onShareLogs = actions.onShareDebugLogs,
                        modifier = Modifier.padding(horizontal = LayoutMetrics.screenGutter),
                    )
                }
                item { SettingsCardSpacer() }
                item {
                    ReminderTestCard(
                        onShowTestReminder = viewModel::showTestHydrationReminder,
                        modifier = Modifier.padding(horizontal = LayoutMetrics.screenGutter),
                    )
                }
                item { SettingsCardSpacer() }
                item {
                    HealthConnectSourcesCard(
                        sources = state.healthConnectSources,
                        modifier = Modifier.padding(horizontal = LayoutMetrics.screenGutter),
                    )
                }
            }
        }
        // These route to their own screens; the branches keep the `when` exhaustive.
        SettingsSection.DISPLAY,
        SettingsSection.ACTIVITIES,
        SettingsSection.SENSORS,
        SettingsSection.WATCHES,
        SettingsSection.NUTRITION,
        SettingsSection.BODY_PROFILE,
        SettingsSection.RECOVERY,
        SettingsSection.DATA_IMPORT,
        SettingsSection.DEVICE_SYNC,
        -> Unit
    }
}

private fun LazyListScope.settingsRootCards(actions: SettingsScreenActions) {
    SettingsSection.entries
        .filter { BuildConfig.OPENVITALS_DIAGNOSTICS || it != SettingsSection.DEBUG_DIAGNOSTICS }
        .forEach { settingsSection ->
            item {
                SettingsCategoryCard(
                    section = settingsSection,
                    onClick = { actions.onOpenSection(settingsSection) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }

    item { SectionHeader(stringResource(R.string.section_support)) }

    item {
        SupportOpenVitalsCard(
            onOpenIssues = actions.onOpenIssues,
            onOpenDiscussion = actions.onOpenDiscussion,
            onOpenSupport = actions.onOpenSupport,
            modifier = Modifier.padding(horizontal = LayoutMetrics.screenGutter),
        )
    }

    item { SectionHeader(stringResource(R.string.section_privacy)) }

    item {
        PrivacyInfoCard(
            onOpenPrivacyPolicy = actions.onOpenPrivacyPolicy,
            modifier = Modifier.padding(horizontal = LayoutMetrics.screenGutter),
        )
    }

    item {
        SettingsVersionText()
    }
}

private fun LazyListScope.healthConnectCards(
    section: SettingsSection,
    state: SettingsUiState,
    viewModel: SettingsViewModel,
    actions: SettingsScreenActions,
) {
    item { SectionHeader(stringResource(section.titleRes)) }
    item {
        HealthConnectSettingsCard(
            syncEnabled = state.healthConnectSyncEnabled,
            availability = state.availability,
            onSyncEnabledChange = viewModel::setHealthConnectSyncEnabled,
            onManageAccess = actions.onOpenManualPermissionSettings,
            modifier = Modifier.padding(horizontal = LayoutMetrics.screenGutter),
        )
    }
    item { SettingsCardSpacer() }
    item {
        MindfulnessIntegrationCard(
            enabled = state.healthConnectMindfulnessEnabled,
            onEnabledChange = viewModel::setHealthConnectMindfulnessEnabled,
            modifier = Modifier.padding(horizontal = LayoutMetrics.screenGutter),
        )
    }
    item { SettingsCardSpacer() }

    state.permissionCategories.forEach { category ->
        item {
            PermissionCategoryCard(
                category = category,
                grantedPermissions = state.grantedPermissions,
                availability = state.availability,
                onGrant = {
                    val missingPermissions = category.permissions - state.grantedPermissions
                    val requestablePermissions = missingPermissions - category.manualPermissions
                    val manualPermissions = missingPermissions.intersect(category.manualPermissions)
                    when {
                        requestablePermissions.isNotEmpty() ->
                            actions.onGrantPermissions(requestablePermissions)
                        manualPermissions.isNotEmpty() -> actions.onOpenManualPermissionSettings()
                    }
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
    }

    if (state.permissionCategories.isEmpty()) {
        item {
            OpenVitalsCard(
                modifier = Modifier.padding(horizontal = LayoutMetrics.screenGutter),
            ) {
                Text(
                    text = stringResource(R.string.settings_all_requestable_granted),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }

    if (state.missingManualVisiblePermissions.isNotEmpty()) {
        item {
            PermissionCallout(
                title = stringResource(R.string.settings_manual_permissions_title),
                body = stringResource(R.string.settings_manual_permissions_body),
                actionLabel = stringResource(R.string.settings_open_health_permissions),
                onGrant = actions.onOpenManualPermissionSettings,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }

    item { SettingsCardSpacer() }
    item {
        AppLockCard(
            enabled = state.appLockEnabled,
            onEnabledChange = viewModel::setAppLockEnabled,
            modifier = Modifier.padding(horizontal = LayoutMetrics.screenGutter),
        )
    }
    item { SettingsCardSpacer() }
}
