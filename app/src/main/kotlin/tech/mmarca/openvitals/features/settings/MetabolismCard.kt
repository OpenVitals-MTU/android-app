package tech.mmarca.openvitals.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.preferences.CaffeinePreferences
import tech.mmarca.openvitals.features.caffeine.MetabolismEditor
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.OpenVitalsTonalButton

/**
 * The nine physiological caffeine-clearance factors, edited as part of the
 * Body profile section. Saves through the same CaffeinePreferences store as
 * the caffeine model card under Nutrition.
 */
@Composable
internal fun MetabolismCard(
    preferences: CaffeinePreferences,
    onSave: (CaffeinePreferences) -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by remember(preferences) { mutableStateOf(preferences) }
    OpenVitalsCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.MonitorHeart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = stringResource(R.string.settings_metabolism_title),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
            Text(
                text = stringResource(R.string.settings_metabolism_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            MetabolismEditor(
                preferences = draft,
                onChange = { draft = it },
                modifier = Modifier.padding(top = 8.dp),
            )
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) {
                OpenVitalsTonalButton(onClick = { onSave(draft.copy(profileCompleted = true)) }) {
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }
}
