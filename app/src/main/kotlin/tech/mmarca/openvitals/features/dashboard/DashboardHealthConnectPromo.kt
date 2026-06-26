package tech.mmarca.openvitals.features.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(R.drawable.health_connect_logo),
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    contentScale = ContentScale.Fit,
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                ) {
                    Text(
                        text = stringResource(titleRes),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = stringResource(bodyRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            OpenVitalsButton(
                onClick = onPrimaryAction,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) {
                Text(stringResource(actionRes))
            }
        }
    }
}
