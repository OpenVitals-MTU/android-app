package tech.mmarca.openvitals.features.imports.medical

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.theme.Spacing

/**
 * The choice to keep the imported file, or each document's PDF when [many]. It starts off on
 * every import and nothing remembers it. The card says what the copies cost in space and where they stay.
 */
@Composable
internal fun MedicalKeepDocumentCard(size: Long, alreadyUsed: Long, many: Boolean, keep: Boolean, onKeepChange: (Boolean) -> Unit) {
    val context = LocalContext.current
    OpenVitalsCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(if (many) R.string.medical_documents_keep_each else R.string.medical_documents_keep),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = stringResource(
                            if (many) R.string.medical_documents_keep_sizes_many else R.string.medical_documents_keep_sizes,
                            Formatter.formatShortFileSize(context, size),
                            Formatter.formatShortFileSize(context, alreadyUsed),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = keep, onCheckedChange = onKeepChange)
            }
            Text(
                text = stringResource(R.string.medical_documents_keep_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
