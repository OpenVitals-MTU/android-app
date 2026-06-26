package tech.mmarca.openvitals.features.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.ui.components.OpenVitalsButton
import tech.mmarca.openvitals.ui.components.OpenVitalsCard

@Composable
fun DashboardHealthConnectPromoCard(
    availability: HealthConnectAvailability,
    syncEnabled: Boolean,
    onPrimaryAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (titleRes, bodyRes, actionRes) = when {
        availability == HealthConnectAvailability.NEEDS_PLAY_STORE ||
            availability == HealthConnectAvailability.NEEDS_PROVIDER_UPDATE ->
            Triple(
                R.string.dashboard_health_connect_promo_title,
                R.string.dashboard_health_connect_promo_body,
                R.string.dashboard_health_connect_install_action,
            )
        !syncEnabled ->
            Triple(
                R.string.settings_health_connect_sync_title,
                R.string.dashboard_health_connect_sync_paused_body,
                R.string.settings_health_connect_manage_access,
            )
        else ->
            Triple(
                R.string.dashboard_health_connect_promo_title,
                R.string.dashboard_health_connect_promo_body,
                R.string.dashboard_health_connect_promo_action,
            )
    }

    OpenVitalsCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(bodyRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            OpenVitalsButton(
                onClick = onPrimaryAction,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(actionRes))
            }
        }
    }
}
