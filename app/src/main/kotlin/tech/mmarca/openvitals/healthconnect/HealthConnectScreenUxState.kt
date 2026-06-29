package tech.mmarca.openvitals.healthconnect

import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.ui.components.HealthConnectAccessGateMode
import tech.mmarca.openvitals.ui.components.resolveHealthConnectAccessGateMode

data class HealthConnectScreenUxState(
    val feature: HealthConnectFeature,
    val availability: HealthConnectAvailability = HealthConnectAvailability.AVAILABLE,
    val syncEnabled: Boolean = true,
    val grantedPermissions: Set<String> = emptySet(),
    val requiredPermissions: Set<String> = emptySet(),
    val accessGateMode: HealthConnectAccessGateMode? = null,
    val missingReadPermissions: Set<String> = emptySet(),
    val showContextualPermissionPrompt: Boolean = false,
    val contextualPromptPermissions: Set<String> = emptySet(),
    val isLoading: Boolean = false,
) {
    val syncPaused: Boolean
        get() = availability == HealthConnectAvailability.AVAILABLE && !syncEnabled
}

fun buildHealthConnectScreenUxState(
    feature: HealthConnectFeature,
    manager: HealthConnectManager,
    availability: HealthConnectAvailability,
    syncEnabled: Boolean,
    grantedPermissions: Set<String>,
    showDoubleCancelRecovery: Boolean,
    acknowledgedPermissions: Set<String> = emptySet(),
    isLoading: Boolean = false,
): HealthConnectScreenUxState {
    val required = feature.requiredReadPermissions(manager)
    val missing = required - grantedPermissions
    val gateRequired = when {
        feature == HealthConnectFeature.DASHBOARD -> emptySet()
        feature == HealthConnectFeature.MANUAL_ENTRY || feature == HealthConnectFeature.DATA_IMPORT -> required
        else -> emptySet()
    }
    val accessGateMode = resolveHealthConnectAccessGateMode(
        availability = availability,
        syncEnabled = syncEnabled,
        requiredPermissions = gateRequired,
        grantedPermissions = grantedPermissions,
        showDoubleCancelRecovery = showDoubleCancelRecovery,
    )
    val unacknowledgedMissing = missing - acknowledgedPermissions
    val showContextual = accessGateMode == null &&
        unacknowledgedMissing.isNotEmpty() &&
        feature != HealthConnectFeature.DASHBOARD &&
        feature != HealthConnectFeature.MANUAL_ENTRY &&
        feature != HealthConnectFeature.DATA_IMPORT

    return HealthConnectScreenUxState(
        feature = feature,
        availability = availability,
        syncEnabled = syncEnabled,
        grantedPermissions = grantedPermissions,
        requiredPermissions = required,
        accessGateMode = accessGateMode,
        missingReadPermissions = missing,
        showContextualPermissionPrompt = showContextual,
        contextualPromptPermissions = if (showContextual) unacknowledgedMissing else emptySet(),
        isLoading = isLoading,
    )
}
