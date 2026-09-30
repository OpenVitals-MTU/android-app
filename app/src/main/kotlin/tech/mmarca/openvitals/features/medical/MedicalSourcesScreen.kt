package tech.mmarca.openvitals.features.medical

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.resolve
import tech.mmarca.openvitals.healthconnect.HealthConnectFeature
import tech.mmarca.openvitals.ui.components.EmptyState
import tech.mmarca.openvitals.ui.components.FullScreenLoading
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.OpenVitalsTextButton
import tech.mmarca.openvitals.ui.components.ScreenErrorContent
import tech.mmarca.openvitals.ui.components.WithHealthConnectFeatureScreen
import tech.mmarca.openvitals.ui.theme.LayoutMetrics
import tech.mmarca.openvitals.ui.theme.Spacing

/** The sources this app added. Deleting one removes it and all its records from Health Connect, after a confirmation. */
@Composable
fun MedicalSourcesScreen(viewModel: MedicalSourcesViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var pending by remember { mutableStateOf<MedicalSourceRow?>(null) }
    DeleteErrorToast(state, onShown = viewModel::consumeDeleteError)
    pending?.let { row ->
        DeleteSourceDialog(
            row = row,
            onDismiss = { pending = null },
            onConfirm = {
                pending = null
                viewModel.delete(row.source.id)
            },
        )
    }
    WithHealthConnectFeatureScreen(feature = HealthConnectFeature.MEDICAL_RECORDS_BROWSE) {
        val error = state.error
        when {
            state.isLoading && state.rows.isEmpty() -> FullScreenLoading()
            error != null -> ScreenErrorContent(error)
            state.rows.isEmpty() -> EmptyState(message = stringResource(R.string.medical_sources_empty), icon = Icons.Outlined.FolderOpen)
            else -> LazyColumn(contentPadding = PaddingValues(vertical = Spacing.sm)) {
                item {
                    Text(
                        text = stringResource(R.string.medical_sources_intro),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = LayoutMetrics.screenGutter, vertical = Spacing.sm),
                    )
                }
                items(state.rows, key = { it.source.id }) { row ->
                    SourceCard(row, onDelete = { pending = row })
                }
            }
        }
    }
}

@Composable
private fun SourceCard(row: MedicalSourceRow, onDelete: () -> Unit) {
    val count = row.recordCount
    OpenVitalsCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = LayoutMetrics.screenGutter, vertical = Spacing.xs),
    ) {
        Row(modifier = Modifier.padding(start = Spacing.lg, top = Spacing.sm, bottom = Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = row.source.displayName, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = listOfNotNull(
                        count?.let { pluralStringResource(R.plurals.medical_records_count, it, it) },
                        "FHIR ${row.source.fhirVersion}",
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = row.source.fhirBaseUri,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.medical_sources_delete_action, row.source.displayName),
                )
            }
        }
    }
}

/** Names the source and, when known, how many records go with it. There is no undo. */
@Composable
private fun DeleteSourceDialog(row: MedicalSourceRow, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val count = row.recordCount
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.medical_sources_delete_title, row.source.displayName)) },
        text = {
            Text(
                when (count) {
                    null -> stringResource(R.string.medical_sources_delete_body_unknown)
                    0 -> stringResource(R.string.medical_sources_delete_body_empty)
                    else -> pluralStringResource(R.plurals.medical_sources_delete_body, count, count)
                },
            )
        },
        confirmButton = { OpenVitalsTextButton(onClick = onConfirm) { Text(stringResource(R.string.action_delete)) } },
        dismissButton = { OpenVitalsTextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun DeleteErrorToast(state: MedicalSourcesUiState, onShown: () -> Unit) {
    val context = LocalContext.current
    val failed = stringResource(R.string.medical_sources_delete_failed)
    val reason = state.deleteError.resolve()
    LaunchedEffect(state.deleteError) {
        if (state.deleteError != null) {
            Toast.makeText(context, listOfNotNull(failed, reason).joinToString(" "), Toast.LENGTH_LONG).show()
            onShown()
        }
    }
}
