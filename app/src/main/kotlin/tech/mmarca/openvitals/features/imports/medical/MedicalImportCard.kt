package tech.mmarca.openvitals.features.imports.medical

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MedicalInformation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.OpenVitalsOutlinedButton
import tech.mmarca.openvitals.ui.theme.Spacing

/** The Import & export entry to the medical records import, next to CSV and Apple Health. */
@Composable
internal fun MedicalImportCard(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    OpenVitalsCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Outlined.MedicalInformation,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(top = Spacing.xs)
                        .size(Spacing.xl),
                )
                Column(modifier = Modifier.padding(start = Spacing.md).weight(1f)) {
                    Text(text = stringResource(R.string.medical_import_title), style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = stringResource(R.string.medical_import_card_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Spacing.xs),
                    )
                }
            }
            OpenVitalsOutlinedButton(
                onClick = onOpen,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.md),
            ) { Text(stringResource(R.string.medical_import_choose_file)) }
        }
    }
}

/** Under the Apple Health card: the export also holds clinical records, which the medical import reads. */
@Composable
internal fun AppleClinicalRecordsCard(count: Int, onImport: () -> Unit, modifier: Modifier = Modifier) {
    OpenVitalsCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Outlined.MedicalInformation,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(top = Spacing.xs)
                        .size(Spacing.xl),
                )
                Column(modifier = Modifier.padding(start = Spacing.md).weight(1f)) {
                    Text(
                        text = pluralStringResource(R.plurals.medical_import_apple_found, count, count),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = stringResource(R.string.medical_import_apple_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Spacing.xs),
                    )
                }
            }
            OpenVitalsOutlinedButton(
                onClick = onImport,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.md),
            ) { Text(stringResource(R.string.medical_import_apple_action)) }
        }
    }
}
