package tech.mmarca.openvitals.features.reports

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.theme.Spacing

/**
 * Adds the medical records section to the report. It uses the medical access already given and
 * asks for none: a report's request must never mix medical and fitness permissions.
 */
@Composable
internal fun ReportMedicalOption(checked: Boolean, onToggle: () -> Unit) {
    OpenVitalsCard(modifier = Modifier.fillMaxWidth().padding(top = Spacing.md)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle() })
                .padding(Spacing.lg),
        ) {
            Checkbox(checked = checked, onCheckedChange = null)
            Column(modifier = Modifier.weight(1f).padding(start = Spacing.xs)) {
                Text(text = stringResource(R.string.medical_records_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(R.string.medical_report_option_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
