package tech.mmarca.openvitals.features.body

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.ui.theme.BodyFatColor
import tech.mmarca.openvitals.ui.theme.WeightColor
import kotlin.math.abs

@Composable
internal fun BodyCompositionCard(
    bmi: Double?,
    bodyFatPercent: Double?,
    leanMassKg: Double?,
    bmrKcal: Double?,
    boneMassKg: Double?,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                bmi?.let {
                    CompositionStat(
                        label = "BMI",
                        value = unitFormatter.decimal(it, 1),
                        modifier = Modifier.weight(1f),
                    )
                }
                bodyFatPercent?.let {
                    CompositionStat(
                        label = "Body fat",
                        value = unitFormatter.percent(it).text,
                        color = BodyFatColor,
                        modifier = Modifier.weight(1f),
                    )
                }
                leanMassKg?.let {
                    CompositionStat(
                        label = "Lean mass",
                        value = unitFormatter.bodyMass(it).text,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (bmrKcal != null || boneMassKg != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    bmrKcal?.let {
                        CompositionStat(
                            label = "BMR",
                            value = unitFormatter.energy(it).text,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    boneMassKg?.let {
                        CompositionStat(
                            label = "Bone mass",
                            value = unitFormatter.bodyMass(it, decimals = 2).text,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (bmrKcal != null && boneMassKg != null) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
internal fun WeightSummaryCard(
    latestKg: Double?,
    changeKg: Double?,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "Latest",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = latestKg?.let { unitFormatter.weight(it).text } ?: "-",
                    style = MaterialTheme.typography.headlineSmall,
                    color = WeightColor,
                )
            }
            if (changeKg != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Change",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val sign = if (changeKg >= 0) "+" else ""
                    val change = unitFormatter.weight(abs(changeKg))
                    Text(
                        text = "$sign${change.text}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = if (changeKg < 0) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun CompositionStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = color,
        )
    }
}
