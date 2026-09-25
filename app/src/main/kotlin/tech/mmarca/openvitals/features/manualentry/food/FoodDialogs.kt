package tech.mmarca.openvitals.features.manualentry.food

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import java.time.Instant
import kotlin.math.roundToInt
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.CustomFood
import tech.mmarca.openvitals.domain.model.FoodCategory
import tech.mmarca.openvitals.domain.model.NutritionNutrient
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.features.manualentry.ManualEntryTimestampFields
import tech.mmarca.openvitals.features.manualentry.nutrition.AddNutrientButton
import tech.mmarca.openvitals.features.manualentry.nutrition.NutrientAmountRow
import tech.mmarca.openvitals.features.manualentry.nutrition.NutrientChooserDialog
import tech.mmarca.openvitals.features.manualentry.nutrition.NutrientInputRow
import tech.mmarca.openvitals.features.manualentry.nutrition.nutrientTitleComparator
import tech.mmarca.openvitals.features.manualentry.nutrition.parsedNutrientValues
import tech.mmarca.openvitals.features.manualentry.nutrition.sortedByTitle
import tech.mmarca.openvitals.features.manualentry.nutrition.toNutrientInputRows
import tech.mmarca.openvitals.ui.components.OpenVitalsOutlinedButton
import tech.mmarca.openvitals.ui.components.OpenVitalsTextButton
import tech.mmarca.openvitals.ui.theme.Spacing

private val FoodSelectorGap = 6.dp

/** Create or edit a food: name, the amount its nutrients describe, a category, and the nutrients. */
@Composable
internal fun FoodDialog(
    @StringRes titleRes: Int,
    unitFormatter: UnitFormatter,
    initialName: String,
    initialAmountGrams: Double?,
    initialCategory: FoodCategory? = null,
    initialNutrientValues: Map<NutritionNutrient, Double> = emptyMap(),
    onDismiss: () -> Unit,
    onSave: (CustomFoodInput) -> Unit,
) {
    val unitSystem = unitFormatter.unitSystem()
    var nameText by remember(initialName) { mutableStateOf(initialName) }
    var amountValue by remember(initialAmountGrams, unitSystem) {
        mutableStateOf(TextFieldValue(foodInputAmountText(initialAmountGrams, unitSystem)))
    }
    var selectedCategory by remember(initialCategory) { mutableStateOf(initialCategory) }
    val nutrientComparator = nutrientTitleComparator(LocalContext.current.resources)
    var nutrientRows by remember(initialNutrientValues) {
        mutableStateOf(initialNutrientValues.toNutrientInputRows(nutrientComparator))
    }
    var nutrientChooserOpen by remember { mutableStateOf(false) }
    val amountGrams = foodInputGrams(amountValue.text, unitSystem)
    val isAmountValid = amountGrams?.let(::isValidFoodAmountGrams) == true
    // A blank row is skipped on save; only unparsable text blocks the form.
    val nutrientValues = nutrientRows.parsedNutrientValues()
    val selectedNutrients = nutrientRows.map { it.nutrient }.toSet()
    val availableNutrients = NutritionNutrient.entries
        .filter { it !in selectedNutrients }
        .sortedWith(nutrientComparator)
    val isFormValid = nameText.isNotBlank() && isAmountValid && !nutrientValues.isNullOrEmpty()

    AlertDialog(
        modifier = Modifier.imePadding(),
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(titleRes))
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text(stringResource(R.string.food_name)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("food_name"),
                )
                FoodAmountField(
                    value = amountValue,
                    onValueChange = { amountValue = it },
                    unitFormatter = unitFormatter,
                    isValid = isAmountValid,
                    enabled = true,
                    modifier = Modifier.testTag("food_amount"),
                )
                FoodCategorySelector(
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it },
                )
                FoodNutrientFields(
                    rows = nutrientRows,
                    onRowsChanged = { nutrientRows = it },
                    canAdd = availableNutrients.isNotEmpty(),
                    onAdd = { nutrientChooserOpen = true },
                )
            }
        },
        confirmButton = {
            OpenVitalsTextButton(
                onClick = {
                    amountGrams?.takeIf(::isValidFoodAmountGrams)?.let { grams ->
                        val values = nutrientValues ?: return@let
                        onSave(
                            CustomFoodInput(
                                name = nameText,
                                amountGrams = grams,
                                category = selectedCategory,
                                nutrientValues = values,
                            )
                        )
                    }
                },
                enabled = isFormValid,
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            OpenVitalsTextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )

    if (nutrientChooserOpen) {
        NutrientChooserDialog(
            availableNutrients = availableNutrients,
            onDismiss = { nutrientChooserOpen = false },
            onSelectNutrient = { nutrient ->
                nutrientRows = (nutrientRows + NutrientInputRow(nutrient)).sortedByTitle(nutrientComparator)
                nutrientChooserOpen = false
            },
        )
    }
}

/** Log a portion of a saved food: how much, and when. */
@Composable
internal fun FoodLogDialog(
    food: CustomFood,
    unitFormatter: UnitFormatter,
    enabled: Boolean,
    onDismiss: () -> Unit,
    onSave: (Double, Instant) -> Unit,
) {
    val unitSystem = unitFormatter.unitSystem()
    // The food's own amount arrives selected, so the first keystroke replaces it.
    var amountValue by remember(food.id, unitSystem) {
        val text = foodInputAmountText(food.amountGrams, unitSystem)
        mutableStateOf(TextFieldValue(text, selection = TextRange(0, text.length)))
    }
    val amountFocusRequester = remember { FocusRequester() }
    LaunchedEffect(food.id) {
        runCatching { amountFocusRequester.requestFocus() }
    }
    var entryTime by remember(food.id) { mutableStateOf(Instant.now()) }
    val amountGrams = foodInputGrams(amountValue.text, unitSystem)
    val isAmountValid = amountGrams?.let(::isValidFoodAmountGrams) == true

    AlertDialog(
        modifier = Modifier.imePadding(),
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.food_log_food_title, food.name))
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                FoodAmountField(
                    value = amountValue,
                    onValueChange = { amountValue = it },
                    unitFormatter = unitFormatter,
                    isValid = isAmountValid,
                    enabled = enabled,
                    modifier = Modifier
                        .focusRequester(amountFocusRequester)
                        .testTag("food_log_amount"),
                )
                ManualEntryTimestampFields(
                    timestamp = entryTime,
                    enabled = enabled,
                    onTimestampChanged = { entryTime = it },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            OpenVitalsTextButton(
                onClick = {
                    amountGrams?.takeIf(::isValidFoodAmountGrams)?.let { grams -> onSave(grams, entryTime) }
                },
                enabled = enabled && isAmountValid,
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            OpenVitalsTextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
private fun FoodAmountField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    unitFormatter: UnitFormatter,
    isValid: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val showError = value.text.isNotBlank() && !isValid
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(stringResource(R.string.food_amount_label, foodInputUnitLabel(unitFormatter.unitSystem())))
        },
        isError = showError,
        supportingText = if (showError) {
            {
                Text(foodInvalidAmountText(unitFormatter))
            }
        } else {
            null
        },
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun FoodNutrientFields(
    rows: List<NutrientInputRow>,
    onRowsChanged: (List<NutrientInputRow>) -> Unit,
    canAdd: Boolean,
    onAdd: () -> Unit,
) {
    Text(
        text = stringResource(R.string.food_nutrients),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (rows.isEmpty()) {
        Text(
            text = stringResource(R.string.food_nutrients_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    rows.forEachIndexed { index, row ->
        NutrientAmountRow(
            row = row,
            onAmountChanged = { text ->
                onRowsChanged(
                    rows.mapIndexed { rowIndex, existing ->
                        if (rowIndex == index) existing.copy(amountText = text) else existing
                    }
                )
            },
            onRemove = {
                onRowsChanged(rows.filterIndexed { rowIndex, _ -> rowIndex != index })
            },
        )
    }
    AddNutrientButton(
        enabled = canAdd,
        onClick = onAdd,
    )
}

@Composable
private fun FoodCategorySelector(
    selectedCategory: FoodCategory?,
    onCategorySelected: (FoodCategory?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val options: List<FoodCategory?> = listOf(null) + FoodCategory.entries

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(FoodSelectorGap),
    ) {
        Text(
            text = stringResource(R.string.food_category),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(modifier = Modifier.fillMaxWidth()) {
            OpenVitalsOutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("food_category"),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(selectedCategory.optionTitleRes()),
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.padding(start = Spacing.sm),
                    )
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                options.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(stringResource(category.optionTitleRes())) },
                        onClick = {
                            onCategorySelected(category)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@StringRes
private fun FoodCategory?.optionTitleRes(): Int =
    this?.titleRes() ?: R.string.food_no_category

@Composable
internal fun foodInvalidAmountText(unitFormatter: UnitFormatter): String {
    val unitSystem = unitFormatter.unitSystem()
    val unitLabel = foodInputUnitLabel(unitSystem)
    val minimum = when (unitSystem) {
        UnitSystem.METRIC -> unitFormatter.count(MinFoodAmountGrams.roundToInt())
        UnitSystem.IMPERIAL -> unitFormatter.decimal(MinFoodAmountGrams / GramsPerOunce, 2)
    }
    val maximum = when (unitSystem) {
        UnitSystem.METRIC -> unitFormatter.count(MaxFoodAmountGrams.roundToInt())
        UnitSystem.IMPERIAL -> unitFormatter.count((MaxFoodAmountGrams / GramsPerOunce).roundToInt())
    }
    return stringResource(
        R.string.food_invalid_amount_range,
        "$minimum $unitLabel",
        "$maximum $unitLabel",
    )
}
