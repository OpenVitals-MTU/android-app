package tech.mmarca.openvitals.features.nutrition

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.records.MealType
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.NutritionEntry
import tech.mmarca.openvitals.ui.components.SourceChip
import tech.mmarca.openvitals.ui.theme.CaloriesColor
import tech.mmarca.openvitals.ui.theme.NutritionColor
import java.time.ZoneId
import kotlin.math.roundToInt

@Composable
internal fun NutritionEntryRow(
    entry: NutritionEntry,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    val start = entry.time.atZone(ZoneId.systemDefault())
    val dateFormatter = dateTimeFormatterProvider.mediumDate()
    val timeFormatter = dateTimeFormatterProvider.shortTime()

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Restaurant,
                contentDescription = null,
                tint = NutritionColor,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.name ?: mealTypeLabel(entry.mealType),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = "${dateFormatter.format(start)}  ·  ${timeFormatter.format(start)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = macroLine(entry, unitFormatter),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = entry.energyKcal?.let { unitFormatter.energy(it).text } ?: stringResource(R.string.message_no_kcal),
                    style = MaterialTheme.typography.labelLarge,
                    color = CaloriesColor,
                )
                Spacer(Modifier.height(4.dp))
                SourceChip(source = entry.source)
            }
        }
    }
}

@Composable
private fun macroLine(entry: NutritionEntry, unitFormatter: UnitFormatter): String {
    val protein = entry.proteinGrams?.let {
        stringResource(R.string.macro_protein_short, unitFormatter.count(it.roundToInt()))
    }
    val carbs = entry.carbsGrams?.let {
        stringResource(R.string.macro_carbs_short, unitFormatter.count(it.roundToInt()))
    }
    val fat = entry.fatGrams?.let {
        stringResource(R.string.macro_fat_short, unitFormatter.count(it.roundToInt()))
    }
    val fiber = entry.fiberGrams?.let {
        stringResource(R.string.macro_fiber, unitFormatter.count(it.roundToInt()))
    }
    val sugar = entry.sugarGrams?.let {
        stringResource(R.string.macro_sugar, unitFormatter.count(it.roundToInt()))
    }
    val parts = buildList {
        protein?.let(::add)
        carbs?.let(::add)
        fat?.let(::add)
        fiber?.let(::add)
        sugar?.let(::add)
    }
    return parts.ifEmpty { listOf(mealTypeLabel(entry.mealType)) }.joinToString(" · ")
}

@Composable
private fun mealTypeLabel(mealType: Int): String = stringResource(
    when (mealType) {
        MealType.MEAL_TYPE_BREAKFAST -> R.string.meal_breakfast
        MealType.MEAL_TYPE_LUNCH -> R.string.meal_lunch
        MealType.MEAL_TYPE_DINNER -> R.string.meal_dinner
        MealType.MEAL_TYPE_SNACK -> R.string.meal_snack
        else -> R.string.meal_generic
    }
)
