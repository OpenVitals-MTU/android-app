package tech.mmarca.openvitals.features.manualentry.nutrition

import android.content.res.Resources
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.model.NutritionNutrient
import tech.mmarca.openvitals.domain.model.NutritionNutrientUnit
import tech.mmarca.openvitals.features.nutrition.titleRes
import tech.mmarca.openvitals.ui.components.OpenVitalsOutlinedButton
import tech.mmarca.openvitals.ui.components.OpenVitalsSurface
import tech.mmarca.openvitals.ui.components.OpenVitalsTextButton
import tech.mmarca.openvitals.ui.theme.Spacing

// The nutrient rows of the drink and food forms. The strings keep their
// hydration_ names: they were translated before foods shared them.

/** Health Connect refuses a larger value per nutrient. */
internal const val MaxNutrientInputValue = 10000.0

private val NutrientRowGap = 6.dp
private val NutrientButtonIconSize = 18.dp
private val NutrientChooserMaxHeight = 420.dp
private val NutrientChoiceBorderWidth = 1.dp

internal fun isValidNutrientInputValue(value: Double): Boolean =
    value > 0.0 &&
        value <= MaxNutrientInputValue &&
        value.isFinite()

/** One nutrient of a food or drink being edited. The text is what the user typed. */
internal data class NutrientInputRow(
    val nutrient: NutritionNutrient,
    val amountText: String = "",
)

/** Orders nutrients by their localized title, so the rows and the chooser read alphabetically. */
internal fun nutrientTitleComparator(resources: Resources): Comparator<NutritionNutrient> =
    Comparator { first, second ->
        resources.getString(first.titleRes()).compareTo(
            other = resources.getString(second.titleRes()),
            ignoreCase = true,
        )
    }

internal fun Map<NutritionNutrient, Double>.toNutrientInputRows(
    comparator: Comparator<NutritionNutrient>,
): List<NutrientInputRow> =
    entries
        .sortedWith { first, second -> comparator.compare(first.key, second.key) }
        .map { (nutrient, value) -> NutrientInputRow(nutrient = nutrient, amountText = value.toString()) }

internal fun List<NutrientInputRow>.sortedByTitle(
    comparator: Comparator<NutritionNutrient>,
): List<NutrientInputRow> =
    sortedWith { first, second -> comparator.compare(first.nutrient, second.nutrient) }

/** A blank row is skipped. Null when a filled row does not parse or is out of range. */
internal fun List<NutrientInputRow>.parsedNutrientValues(): Map<NutritionNutrient, Double>? {
    val filled = filter { it.amountText.isNotBlank() }
    val values = filled.mapNotNull { row ->
        row.amountText.toNutrientInputValueOrNull()?.let { row.nutrient to it }
    }
    return values.toMap().takeIf { it.size == filled.size }
}

internal fun String.toNutrientInputValueOrNull(): Double? =
    replace(',', '.').toDoubleOrNull()?.takeIf(::isValidNutrientInputValue)

@Composable
internal fun NutrientAmountRow(
    row: NutrientInputRow,
    onAmountChanged: (String) -> Unit,
    onRemove: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(row.nutrient.titleRes()),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.action_delete),
                )
            }
        }
        OutlinedTextField(
            value = row.amountText,
            onValueChange = onAmountChanged,
            label = { Text(nutrientAmountLabel(row.nutrient)) },
            isError = row.amountText.isNotBlank() && row.amountText.toNutrientInputValueOrNull() == null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("nutrient_amount_${row.nutrient.name}"),
        )
    }
}

@Composable
internal fun AddNutrientButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OpenVitalsOutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
    ) {
        Icon(
            imageVector = Icons.Outlined.Add,
            contentDescription = null,
            modifier = Modifier.size(NutrientButtonIconSize),
        )
        Text(
            text = stringResource(R.string.hydration_custom_drink_add_nutrient),
            modifier = Modifier.padding(start = NutrientRowGap),
        )
    }
}

@Composable
internal fun NutrientChooserDialog(
    availableNutrients: List<NutritionNutrient>,
    onDismiss: () -> Unit,
    onSelectNutrient: (NutritionNutrient) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.hydration_custom_drink_add_nutrient))
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = NutrientChooserMaxHeight)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(NutrientRowGap),
            ) {
                availableNutrients.forEach { nutrient ->
                    NutrientChoiceRow(
                        nutrient = nutrient,
                        onClick = { onSelectNutrient(nutrient) },
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            OpenVitalsTextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
private fun NutrientChoiceRow(
    nutrient: NutritionNutrient,
    onClick: () -> Unit,
) {
    OpenVitalsSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurface,
        contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.md),
        border = BorderStroke(NutrientChoiceBorderWidth, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Text(
            text = stringResource(nutrient.titleRes()),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun nutrientAmountLabel(nutrient: NutritionNutrient): String =
    when (nutrient.unit) {
        NutritionNutrientUnit.ENERGY_KCAL -> stringResource(R.string.hydration_custom_drink_amount_kcal)
        NutritionNutrientUnit.MASS_GRAMS,
        NutritionNutrientUnit.MASS_ADAPTIVE -> stringResource(R.string.hydration_custom_drink_amount_grams)
    }
