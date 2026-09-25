package tech.mmarca.openvitals.features.manualentry.food

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.LunchDining
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.time.Instant
import kotlin.math.round
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.core.presentation.resolve
import tech.mmarca.openvitals.domain.model.CustomFood
import tech.mmarca.openvitals.domain.model.FoodCategory
import tech.mmarca.openvitals.domain.model.NutritionNutrient
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.features.manualentry.ManualEntryWritePermissionCallout
import tech.mmarca.openvitals.ui.components.AccentIconChip
import tech.mmarca.openvitals.ui.components.OpenVitalsButton
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.OpenVitalsSurface
import tech.mmarca.openvitals.ui.theme.LayoutMetrics
import tech.mmarca.openvitals.ui.theme.NutritionColor
import tech.mmarca.openvitals.ui.theme.Spacing

/** A search field appears once the list is long enough to need one. */
private const val FoodCatalogSearchThreshold = 8
private val FoodTrackerIconSize = 40.dp
private val FoodTrackerGlyphSize = 20.dp
private val FoodActionIconSize = 18.dp
private val FoodRowGap = 6.dp
private val FoodRowBorderWidth = 1.dp

@Composable
internal fun FoodTrackerCard(
    state: FoodEntryUiState,
    unitFormatter: UnitFormatter,
    onSaveCustomFood: (CustomFoodInput, String?) -> Unit,
    onLogFood: (CustomFood, Double, Instant) -> Unit,
    onDeleteCustomFood: (CustomFood) -> Unit,
    onRequestWritePermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val canInteract = !state.isSavingEntry && !state.isCheckingPermission
    var addingNewFood by remember { mutableStateOf(false) }
    var editingFood by remember { mutableStateOf<CustomFood?>(null) }
    var loggingFood by remember { mutableStateOf<CustomFood?>(null) }

    OpenVitalsCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("food_entry_tracker"),
    ) {
        Column(
            modifier = Modifier.padding(LayoutMetrics.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            FoodTrackerHeader()

            if (!state.canWrite && !state.isCheckingPermission) {
                ManualEntryWritePermissionCallout(
                    body = stringResource(R.string.food_tracker_permission_needed),
                    onGrant = onRequestWritePermission,
                )
            }

            Text(
                text = stringResource(R.string.food_today_energy, unitFormatter.energy(state.todayEnergyKcal).text),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            FoodCatalog(
                foods = state.foodOptions,
                unitFormatter = unitFormatter,
                canLog = state.canWrite && canInteract,
                canEdit = canInteract,
                onSelectFood = { loggingFood = it },
                onEditFood = { editingFood = it },
                onDeleteFood = onDeleteCustomFood,
            )

            OpenVitalsButton(
                onClick = { addingNewFood = true },
                enabled = canInteract,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("food_new_food"),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                    modifier = Modifier.size(FoodActionIconSize),
                )
                Text(
                    text = stringResource(R.string.food_new_food_action),
                    modifier = Modifier.padding(start = FoodRowGap),
                )
            }

            state.entryError?.let { entryError ->
                Text(
                    text = foodEntryErrorText(entryError, state.writeError),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }

    if (addingNewFood) {
        FoodDialog(
            titleRes = R.string.food_new_food_title,
            unitFormatter = unitFormatter,
            initialName = "",
            initialAmountGrams = null,
            onDismiss = { addingNewFood = false },
            onSave = { input ->
                addingNewFood = false
                onSaveCustomFood(input, null)
            },
        )
    }
    editingFood?.let { food ->
        FoodDialog(
            titleRes = R.string.food_edit_food_title,
            unitFormatter = unitFormatter,
            initialName = food.name,
            initialAmountGrams = food.amountGrams,
            initialCategory = food.category,
            initialNutrientValues = food.nutrientValues,
            onDismiss = { editingFood = null },
            onSave = { input ->
                editingFood = null
                onSaveCustomFood(input, food.id)
            },
        )
    }
    loggingFood?.let { food ->
        FoodLogDialog(
            food = food,
            unitFormatter = unitFormatter,
            enabled = !state.isSavingEntry,
            onDismiss = { loggingFood = null },
            onSave = { amountGrams, entryTime ->
                loggingFood = null
                onLogFood(food, amountGrams, entryTime)
            },
        )
    }
}

@Composable
private fun FoodTrackerHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AccentIconChip(
            icon = Icons.Outlined.LunchDining,
            color = NutritionColor,
            size = FoodTrackerIconSize,
            iconSize = FoodTrackerGlyphSize,
        )
        Column(
            modifier = Modifier
                .padding(horizontal = Spacing.md)
                .weight(1f),
        ) {
            Text(
                text = stringResource(R.string.food_tracker_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.food_tracker_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** The saved foods, grouped by category. A tap logs one; the menu edits or deletes it. */
@Composable
private fun FoodCatalog(
    foods: List<CustomFood>,
    unitFormatter: UnitFormatter,
    canLog: Boolean,
    canEdit: Boolean,
    onSelectFood: (CustomFood) -> Unit,
    onEditFood: (CustomFood) -> Unit,
    onDeleteFood: (CustomFood) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val sections = remember(foods, query) { groupFoodCatalog(foods, query) }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        if (foods.isEmpty()) {
            Text(
                text = stringResource(R.string.food_catalog_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (foods.size >= FoodCatalogSearchThreshold) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text(stringResource(R.string.food_catalog_search)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("food_catalog_search"),
            )
        }
        sections.forEach { section ->
            Text(
                text = stringResource(section.category.sectionTitleRes()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.xs),
            )
            section.foods.forEach { food ->
                FoodCatalogRow(
                    food = food,
                    unitFormatter = unitFormatter,
                    canLog = canLog,
                    canEdit = canEdit,
                    onClick = { onSelectFood(food) },
                    onEdit = { onEditFood(food) },
                    onDelete = { onDeleteFood(food) },
                )
            }
        }
    }
}

@Composable
private fun FoodCatalogRow(
    food: CustomFood,
    unitFormatter: UnitFormatter,
    canLog: Boolean,
    canEdit: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    OpenVitalsSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = canLog, onClick = onClick)
            .testTag("food_row_${food.id}"),
        shape = MaterialTheme.shapes.medium,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurface,
        contentPadding = PaddingValues(start = Spacing.md, top = Spacing.xs, end = Spacing.xs, bottom = Spacing.xs),
        border = BorderStroke(FoodRowBorderWidth, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = food.name,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = foodRowSummary(food, unitFormatter),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }, enabled = canEdit) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = stringResource(R.string.cd_food_actions),
                    )
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_edit)) },
                        onClick = {
                            menuOpen = false
                            onEdit()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_delete)) },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}

/** "120 g · 105 kcal", or the amount alone for a food without energy. */
private fun foodRowSummary(food: CustomFood, unitFormatter: UnitFormatter): String {
    val amount = foodAmountLabel(food.amountGrams, unitFormatter)
    val energy = food.nutrientValues[NutritionNutrient.ENERGY]?.let { unitFormatter.energy(it).text }
    return if (energy == null) amount else "$amount · $energy"
}

internal fun foodAmountLabel(grams: Double, unitFormatter: UnitFormatter): String =
    when (unitFormatter.unitSystem()) {
        UnitSystem.METRIC -> "${unitFormatter.decimal(grams, if (grams == round(grams)) 0 else 1)} g"
        UnitSystem.IMPERIAL -> "${unitFormatter.decimal(grams / GramsPerOunce, 1)} oz"
    }

@StringRes
internal fun FoodCategory.titleRes(): Int =
    when (this) {
        FoodCategory.FRUIT -> R.string.food_catalog_section_fruit
        FoodCategory.VEGETABLE -> R.string.food_catalog_section_vegetables
        FoodCategory.GRAIN -> R.string.food_catalog_section_grains
        FoodCategory.PROTEIN -> R.string.food_catalog_section_protein
        FoodCategory.DAIRY -> R.string.food_catalog_section_dairy
        FoodCategory.SNACK -> R.string.food_catalog_section_snacks
        FoodCategory.MEAL -> R.string.food_catalog_section_meals
        FoodCategory.DESSERT -> R.string.food_catalog_section_desserts
        FoodCategory.OTHER -> R.string.food_catalog_section_other
    }

@StringRes
private fun FoodCategory?.sectionTitleRes(): Int =
    this?.titleRes() ?: R.string.food_catalog_saved_outside

@Composable
internal fun foodEntryErrorText(
    error: FoodEntryError,
    writeError: ScreenError?,
): String = when (error) {
    FoodEntryError.INVALID_AMOUNT -> stringResource(R.string.food_invalid_amount)
    FoodEntryError.INVALID_FOOD -> stringResource(R.string.food_invalid)
    FoodEntryError.MISSING_WRITE_PERMISSION -> stringResource(R.string.food_tracker_permission_needed)
    FoodEntryError.WRITE_FAILED -> stringResource(
        R.string.food_write_failed,
        writeError.resolve() ?: stringResource(R.string.unknown_error),
    )
}
