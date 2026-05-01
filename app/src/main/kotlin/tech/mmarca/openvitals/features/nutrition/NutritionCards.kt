package tech.mmarca.openvitals.features.nutrition

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.ui.components.MetricCard
import tech.mmarca.openvitals.ui.theme.NutritionColor
import kotlin.math.roundToInt

private val proteinColor = Color(0xFF7E57C2)
private val carbsColor = Color(0xFF26A69A)
private val fatColor = Color(0xFFFFB300)

@Composable
internal fun NutritionSummary(
    state: NutritionUiState,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
) {
    val energy = unitFormatter.energy(state.totalEnergyKcal)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MetricCard(
            title = stringResource(R.string.metric_calories_in),
            value = energy.value,
            unit = energy.unit,
            icon = Icons.Outlined.Restaurant,
            accentColor = NutritionColor,
            subtitle = stringResource(R.string.summary_entries, unitFormatter.count(state.entries.size)),
            modifier = Modifier.weight(1f),
        )
        MetricCard(
            title = stringResource(R.string.metric_protein),
            value = unitFormatter.count(state.totalProteinGrams.roundToInt()),
            unit = stringResource(R.string.unit_grams),
            icon = Icons.Outlined.Restaurant,
            accentColor = proteinColor,
            subtitle = stringResource(R.string.summary_across_selected_period),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
internal fun MacroSummaryCard(
    proteinGrams: Double,
    carbsGrams: Double,
    fatGrams: Double,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
) {
    val total = proteinGrams + carbsGrams + fatGrams

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.metric_macros),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp),
            ) {
                MacroSegment(if (total > 0.0) proteinGrams / total else 1.0 / 3.0, proteinColor)
                MacroSegment(if (total > 0.0) carbsGrams / total else 1.0 / 3.0, carbsColor)
                MacroSegment(if (total > 0.0) fatGrams / total else 1.0 / 3.0, fatColor)
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                MacroLegend(stringResource(R.string.metric_protein), proteinGrams, proteinColor, unitFormatter)
                MacroLegend(stringResource(R.string.metric_carbs), carbsGrams, carbsColor, unitFormatter)
                MacroLegend(stringResource(R.string.metric_fat), fatGrams, fatColor, unitFormatter)
            }
        }
    }
}

@Composable
private fun RowScope.MacroSegment(fraction: Double, color: Color) {
    Box(
        modifier = Modifier
            .weight(fraction.toFloat().coerceAtLeast(0.01f))
            .height(16.dp),
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(16.dp)) {
            drawRoundRect(
                color = color,
                cornerRadius = CornerRadius(6.dp.toPx()),
            )
        }
    }
}

@Composable
private fun MacroLegend(
    label: String,
    grams: Double,
    color: Color,
    unitFormatter: UnitFormatter,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "${unitFormatter.count(grams.roundToInt())} ${stringResource(R.string.unit_grams)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
