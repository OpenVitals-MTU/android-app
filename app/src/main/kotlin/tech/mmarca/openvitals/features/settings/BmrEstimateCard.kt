package tech.mmarca.openvitals.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.insights.BmrInput
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.OpenVitalsTonalButton
import tech.mmarca.openvitals.ui.theme.LayoutMetrics
import tech.mmarca.openvitals.ui.theme.Spacing

private val LeadingIconOpticalOffset = 2.dp
private val LeadingIconSize = 20.dp

/**
 * The opt-in basal metabolic rate estimate, under Body profile. The switch
 * applies at once; what the estimate still needs, or today's value, sits
 * under it so the user sees why nothing is written yet.
 */
@Composable
internal fun BmrEstimateCard(
    enabled: Boolean,
    todayKcal: Int?,
    missingInputs: Set<BmrInput>,
    writePermissionMissing: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onGrantPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OpenVitalsCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(LayoutMetrics.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Outlined.LocalFireDepartment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(top = LeadingIconOpticalOffset)
                        .size(LeadingIconSize),
                )
                Column(
                    modifier = Modifier
                        .padding(start = Spacing.md)
                        .weight(1f),
                ) {
                    Text(
                        text = stringResource(R.string.settings_bmr_estimate_title),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = stringResource(R.string.settings_bmr_estimate_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Spacing.xs),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.settings_bmr_estimate_toggle),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange,
                    modifier = Modifier.padding(start = Spacing.md),
                )
            }

            BmrEstimateStatus(todayKcal = todayKcal, missingInputs = missingInputs)

            if (enabled && writePermissionMissing) {
                Text(
                    text = stringResource(R.string.settings_bmr_estimate_permission_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                OpenVitalsTonalButton(onClick = onGrantPermission) {
                    Text(stringResource(R.string.action_grant_permission))
                }
            }
        }
    }
}

@Composable
private fun BmrEstimateStatus(todayKcal: Int?, missingInputs: Set<BmrInput>) {
    val text = if (missingInputs.isEmpty() && todayKcal != null) {
        stringResource(R.string.settings_bmr_estimate_today, todayKcal)
    } else {
        // map is inline, so the composable call inside is allowed; joinToString's lambda is not.
        val labels = BmrInput.entries
            .filter { it in missingInputs }
            .map { stringResource(it.labelRes) }
            .joinToString(separator = ", ")
        stringResource(R.string.settings_bmr_estimate_missing, labels)
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private val BmrInput.labelRes: Int
    get() = when (this) {
        BmrInput.SEX -> R.string.settings_body_profile_sex
        BmrInput.BIRTH_YEAR -> R.string.body_energy_calibration_birth_year
        BmrInput.WEIGHT -> R.string.settings_body_profile_weight
        BmrInput.HEIGHT -> R.string.settings_body_profile_height
    }
