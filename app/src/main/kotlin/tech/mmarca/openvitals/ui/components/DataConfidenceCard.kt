package tech.mmarca.openvitals.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.insights.DataConfidence
import tech.mmarca.openvitals.core.insights.DataConfidenceLevel
import tech.mmarca.openvitals.core.insights.DataConfidenceWarning
import tech.mmarca.openvitals.core.insights.DataSourceConsistency
import tech.mmarca.openvitals.core.insights.DataValueKind

@Composable
fun DataConfidenceCard(
    confidence: DataConfidence,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.medium
    val levelColor = when (confidence.level) {
        DataConfidenceLevel.HIGH -> accentColor
        DataConfidenceLevel.MEDIUM -> MaterialTheme.colorScheme.tertiary
        DataConfidenceLevel.LOW -> MaterialTheme.colorScheme.error
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = levelColor.copy(alpha = 0.35f),
                shape = shape,
            ),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Verified,
                    contentDescription = null,
                    tint = levelColor,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.data_confidence_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = dataConfidenceLevelText(confidence.level),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = levelColor,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(
                    R.string.data_confidence_coverage,
                    confidence.trackedDays,
                    confidence.expectedDays,
                    confidence.coveragePercent,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.data_confidence_samples, confidence.sampleCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = dataSourceText(confidence),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = dataValueKindText(confidence.valueKind),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            confidence.warnings.take(3).forEach { warning ->
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "- ${dataWarningText(warning)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun dataConfidenceLevelText(level: DataConfidenceLevel): String =
    stringResource(
        when (level) {
            DataConfidenceLevel.HIGH -> R.string.data_confidence_high
            DataConfidenceLevel.MEDIUM -> R.string.data_confidence_medium
            DataConfidenceLevel.LOW -> R.string.data_confidence_low
        }
    )

@Composable
private fun dataSourceText(confidence: DataConfidence): String =
    when (confidence.sourceConsistency) {
        DataSourceConsistency.NOT_AVAILABLE -> stringResource(R.string.data_confidence_source_unavailable)
        DataSourceConsistency.SINGLE_SOURCE -> stringResource(
            R.string.data_confidence_source_single,
            displaySourceName(confidence.sources.first()),
        )
        DataSourceConsistency.MIXED_SOURCES -> stringResource(
            R.string.data_confidence_source_mixed,
            confidence.sources.take(3).joinToString(", ") { displaySourceName(it) },
        )
    }

@Composable
private fun dataValueKindText(kind: DataValueKind): String =
    stringResource(
        when (kind) {
            DataValueKind.MEASURED -> R.string.data_confidence_kind_measured
            DataValueKind.AGGREGATED -> R.string.data_confidence_kind_aggregated
            DataValueKind.CALCULATED -> R.string.data_confidence_kind_calculated
            DataValueKind.ESTIMATED -> R.string.data_confidence_kind_estimated
            DataValueKind.MIXED -> R.string.data_confidence_kind_mixed
        }
    )

@Composable
private fun dataWarningText(warning: DataConfidenceWarning): String =
    stringResource(
        when (warning) {
            DataConfidenceWarning.LOW_COVERAGE -> R.string.data_confidence_warning_low_coverage
            DataConfidenceWarning.SPARSE_DATA -> R.string.data_confidence_warning_sparse
            DataConfidenceWarning.MIXED_SOURCES -> R.string.data_confidence_warning_mixed_sources
            DataConfidenceWarning.MANUAL_ENTRIES -> R.string.data_confidence_warning_manual
            DataConfidenceWarning.CALCULATED_VALUE -> R.string.data_confidence_warning_calculated
            DataConfidenceWarning.NO_SOURCE_DETAILS -> R.string.data_confidence_warning_no_sources
        }
    )

private fun displaySourceName(packageName: String): String = when {
    packageName.contains("samsung") -> "Samsung Health"
    packageName.contains("fitbit") -> "Fitbit"
    packageName.contains("opentracks") -> "OpenTracks"
    packageName.contains("strava") -> "Strava"
    packageName.contains("garmin") -> "Garmin"
    packageName.contains("polar") -> "Polar"
    packageName.contains("google.android.apps.fitness") -> "Google Fit"
    else -> packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
}
