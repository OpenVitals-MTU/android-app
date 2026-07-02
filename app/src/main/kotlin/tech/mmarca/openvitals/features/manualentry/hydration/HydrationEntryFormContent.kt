package tech.mmarca.openvitals.features.manualentry.hydration

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.abs
import kotlin.math.roundToInt
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.core.presentation.resolve
import tech.mmarca.openvitals.domain.model.CustomHydrationDrink
import tech.mmarca.openvitals.domain.model.NutritionNutrient
import tech.mmarca.openvitals.domain.model.NutritionNutrientUnit
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.features.manualentry.ManualEntryTimestampFields
import tech.mmarca.openvitals.features.nutrition.titleRes
import tech.mmarca.openvitals.ui.components.OpenVitalsButton
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.OpenVitalsOutlinedButton
import tech.mmarca.openvitals.ui.components.OpenVitalsSurface
import tech.mmarca.openvitals.ui.components.OpenVitalsTextButton
import tech.mmarca.openvitals.ui.theme.HydrationColor

private const val MillilitersPerFluidOunce = 29.5735295625
private const val FullHydrationImpactMultiplier = 1.0
private const val DefaultPartialHydrationImpactPercent = 50
private const val HydrationSavedDrinkGridColumns = 2
private const val HydrationSavedDrinkEditWiggleDegrees = 0.45f
private val HydrationSavedDrinkGridSpacing = 8.dp

@Composable
internal fun HydrationTrackerCard(
    state: HydrationEntryUiState,
    unitFormatter: UnitFormatter,
    onAddSelectedEntry: () -> Unit,
    onSaveCustomDrink: (CustomHydrationDrinkInput, String?) -> Unit,
    onAddSavedCustomDrinkEntry: (CustomHydrationDrink) -> Unit,
    onDeleteCustomDrink: (CustomHydrationDrink) -> Unit,
    onMoveCustomDrinkToTarget: (String, String) -> Unit,
    onEntryTimeChanged: (java.time.Instant) -> Unit,
    onRequestWritePermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val canLogEntry = state.canWriteHydration && !state.isSavingEntry && !state.isCheckingPermission
    val canEditSavedDrinks = !state.isSavingEntry && !state.isCheckingPermission
    var addingNewDrink by remember { mutableStateOf(false) }
    var editingSavedDrink by remember { mutableStateOf<CustomHydrationDrink?>(null) }
    var isEditingSavedDrinks by remember { mutableStateOf(false) }
    OpenVitalsCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("hydration_entry_tracker"),

    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocalDrink,
                    contentDescription = null,
                    tint = HydrationColor,
                    modifier = Modifier.size(22.dp),
                )
                Column(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .weight(1f),
                ) {
                    Text(
                        text = stringResource(R.string.hydration_tracker_title),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = stringResource(
                            if (state.canWriteHydration) {
                                if (state.canWriteNutrition) {
                                    R.string.hydration_tracker_subtitle
                                } else {
                                    R.string.hydration_nutrition_permission_needed
                                }
                            } else {
                                R.string.hydration_tracker_permission_needed
                            }
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if ((!state.canWriteHydration || !state.canWriteNutrition) && !state.isCheckingPermission) {
                    OpenVitalsOutlinedButton(onClick = onRequestWritePermission) {
                        Text(stringResource(R.string.action_grant))
                    }
                }
            }

            HydrationTodayCounter(
                liters = state.todayHydrationLiters,
                dailyGoalLiters = state.dailyGoalLiters,
                unitFormatter = unitFormatter,
            )

            val entryNotice = state.entryNotice
            AnimatedVisibility(visible = entryNotice != null) {
                if (entryNotice != null) {
                    HydrationEntryNoticeCallout(
                        notice = entryNotice,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            if (state.isEditMode) {
                ManualEntryTimestampFields(
                    timestamp = state.editTime,
                    enabled = !state.isSavingEntry,
                    onTimestampChanged = onEntryTimeChanged,
                    modifier = Modifier.fillMaxWidth(),
                )

                OpenVitalsButton(
                    onClick = onAddSelectedEntry,
                    enabled = canLogEntry,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = stringResource(R.string.action_save),
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            } else {
                HydrationCustomDrinkCarousel(
                    drinks = state.customDrinkOptions,
                    unitFormatter = unitFormatter,
                    enabled = canLogEntry,
                    isEditingDrinks = isEditingSavedDrinks,
                    onToggleEdit = { isEditingSavedDrinks = !isEditingSavedDrinks },
                    onSelectDrink = onAddSavedCustomDrinkEntry,
                    onEditDrink = { editingSavedDrink = it },
                    onDeleteDrink = onDeleteCustomDrink,
                    onMoveDrinkToTarget = onMoveCustomDrinkToTarget,
                )

                OpenVitalsButton(
                    onClick = { addingNewDrink = true },
                    enabled = canEditSavedDrinks,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = stringResource(R.string.hydration_new_drink_action),
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }

            if (addingNewDrink) {
                HydrationCustomDrinkDialog(
                    titleRes = R.string.hydration_new_drink_title,
                    unitFormatter = unitFormatter,
                    initialName = "",
                    initialMilliliters = null,
                    initialHydrationMultiplier = FullHydrationImpactMultiplier,
                    onDismiss = { addingNewDrink = false },
                    onSave = { input ->
                        addingNewDrink = false
                        onSaveCustomDrink(input, null)
                    },
                )
            }
            editingSavedDrink?.let { drink ->
                HydrationCustomDrinkDialog(
                    titleRes = R.string.hydration_edit_drink_title,
                    unitFormatter = unitFormatter,
                    initialName = drink.name,
                    initialMilliliters = drink.volumeMilliliters,
                    initialHydrationMultiplier = drink.hydrationMultiplier,
                    initialNutrientValues = drink.nutrientValues,
                    onDismiss = { editingSavedDrink = null },
                    onSave = { input ->
                        editingSavedDrink = null
                        onSaveCustomDrink(input, drink.id)
                    },
                )
            }

            state.entryError?.let { entryError ->
                Text(
                    text = hydrationEntryErrorText(entryError, state.writeError),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun HydrationEntryNoticeCallout(
    notice: HydrationEntryNotice,
    modifier: Modifier = Modifier,
) {
    val textRes = when (notice) {
        HydrationEntryNotice.NON_HYDRATING_DRINK_SAVED ->
            R.string.hydration_non_hydrating_drink_saved_hint
    }
    Row(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.small,
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = stringResource(textRes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.weight(1f),
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun HydrationTodayCounter(
    liters: Double,
    dailyGoalLiters: Double,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
) {
    val todayHydration = unitFormatter.hydration(liters)
    val dailyGoal = unitFormatter.hydration(dailyGoalLiters)
    val targetProgress = if (dailyGoalLiters > 0.0) {
        (liters / dailyGoalLiters).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }
    val progress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 650),
        label = "HydrationTodayGoalProgress",
    )
    val trackColor = MaterialTheme.colorScheme.outlineVariant
    val progressColor = HydrationColor.copy(alpha = 0.86f)
    val strokeWidth = with(LocalDensity.current) { 5.dp.toPx() }
    val progressStroke = remember(strokeWidth) {
        Stroke(width = strokeWidth, cap = StrokeCap.Round)
    }
    OpenVitalsSurface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurface,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "${todayHydration.text} / ${dailyGoal.text}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End,
            )
            LinearWavyProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp),
                color = progressColor,
                trackColor = trackColor,
                stroke = progressStroke,
                trackStroke = progressStroke,
                wavelength = 34.dp,
                waveSpeed = 34.dp,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun HydrationCustomDrinkCarousel(
    drinks: List<CustomHydrationDrink>,
    unitFormatter: UnitFormatter,
    enabled: Boolean,
    isEditingDrinks: Boolean,
    onToggleEdit: () -> Unit,
    onSelectDrink: (CustomHydrationDrink) -> Unit,
    onEditDrink: (CustomHydrationDrink) -> Unit,
    onDeleteDrink: (CustomHydrationDrink) -> Unit,
    onMoveDrinkToTarget: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (drinks.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.hydration_custom_drinks_title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            val editLabel = stringResource(
                if (isEditingDrinks) {
                    R.string.cd_done_editing_saved_drinks
                } else {
                    R.string.cd_edit_saved_drinks
                }
            )
            IconButton(onClick = onToggleEdit) {
                Icon(
                    imageVector = if (isEditingDrinks) Icons.Outlined.Check else Icons.Outlined.Edit,
                    contentDescription = editLabel,
                )
            }
        }

        HydrationSavedDrinkGrid(
            drinks = drinks,
            unitFormatter = unitFormatter,
            enabled = enabled,
            isEditingDrinks = isEditingDrinks,
            onSelectDrink = onSelectDrink,
            onEditDrink = onEditDrink,
            onDeleteDrink = onDeleteDrink,
            onMoveDrinkToTarget = onMoveDrinkToTarget,
        )
    }
}

@Composable
private fun HydrationSavedDrinkGrid(
    drinks: List<CustomHydrationDrink>,
    unitFormatter: UnitFormatter,
    enabled: Boolean,
    isEditingDrinks: Boolean,
    onSelectDrink: (CustomHydrationDrink) -> Unit,
    onEditDrink: (CustomHydrationDrink) -> Unit,
    onDeleteDrink: (CustomHydrationDrink) -> Unit,
    onMoveDrinkToTarget: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lazyGridState = rememberLazyGridState()
    val reorderableState = rememberReorderableLazyGridState(lazyGridState) { from, to ->
        val fromId = from.key as? String
        val toId = to.key as? String
        if (fromId != null && toId != null && fromId != toId) {
            onMoveDrinkToTarget(fromId, toId)
        }
    }
    val rowCount = (drinks.size + HydrationSavedDrinkGridColumns - 1) / HydrationSavedDrinkGridColumns

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val tileWidth = (
            maxWidth - HydrationSavedDrinkGridSpacing * (HydrationSavedDrinkGridColumns - 1)
            ).coerceAtLeast(0.dp) / HydrationSavedDrinkGridColumns
        val gridHeight = tileWidth * rowCount +
            HydrationSavedDrinkGridSpacing * (rowCount - 1).coerceAtLeast(0)

        LazyVerticalGrid(
            columns = GridCells.Fixed(HydrationSavedDrinkGridColumns),
            state = lazyGridState,
            userScrollEnabled = false,
            modifier = Modifier
                .fillMaxWidth()
                .height(gridHeight)
                .animateContentSize(),
            horizontalArrangement = Arrangement.spacedBy(HydrationSavedDrinkGridSpacing),
            verticalArrangement = Arrangement.spacedBy(HydrationSavedDrinkGridSpacing),
        ) {
            itemsIndexed(
                items = drinks,
                key = { _, drink -> drink.id },
            ) { _, drink ->
                ReorderableItem(
                    state = reorderableState,
                    key = drink.id,
                    enabled = isEditingDrinks,
                ) { isDragging ->
                    HydrationSavedDrinkTile(
                        drink = drink,
                        unitFormatter = unitFormatter,
                        enabled = enabled,
                        isEditingDrinks = isEditingDrinks,
                        isDragging = isDragging,
                        onSelectDrink = { onSelectDrink(drink) },
                        onEditDrink = { onEditDrink(drink) },
                        onDeleteDrink = { onDeleteDrink(drink) },
                        dragHandleModifier = Modifier.longPressDraggableHandle(enabled = isEditingDrinks),
                        modifier = Modifier.aspectRatio(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun HydrationSavedDrinkTile(
    drink: CustomHydrationDrink,
    unitFormatter: UnitFormatter,
    enabled: Boolean,
    isEditingDrinks: Boolean,
    isDragging: Boolean,
    onSelectDrink: () -> Unit,
    onEditDrink: () -> Unit,
    onDeleteDrink: () -> Unit,
    dragHandleModifier: Modifier,
    modifier: Modifier = Modifier,
) {
    val wiggleRotation = if (isEditingDrinks) {
        val wiggleTransition = rememberInfiniteTransition(label = "HydrationSavedDrinkWiggle")
        val rotation by wiggleTransition.animateFloat(
            initialValue = -HydrationSavedDrinkEditWiggleDegrees,
            targetValue = HydrationSavedDrinkEditWiggleDegrees,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 140,
                    delayMillis = (drink.id.hashCode().mod(4)) * 35,
                    easing = LinearEasing,
                ),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "HydrationSavedDrinkWiggleRotation",
        )
        rotation
    } else {
        0f
    }

    Box(
        modifier = modifier
            .zIndex(if (isDragging) 1f else 0f)
            .graphicsLayer {
                rotationZ = if (isEditingDrinks && !isDragging) wiggleRotation else 0f
                scaleX = if (isDragging) 1.02f else 1f
                scaleY = if (isDragging) 1.02f else 1f
                shadowElevation = if (isDragging) 12.dp.toPx() else 0f
            }
            .then(dragHandleModifier),
    ) {
        OpenVitalsSurface(
            modifier = Modifier
                .fillMaxSize()
                .combinedClickable(
                    enabled = enabled && !isEditingDrinks,
                    role = Role.Button,
                    onClick = onSelectDrink,
                ),
            shape = MaterialTheme.shapes.medium,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocalDrink,
                    contentDescription = null,
                    tint = HydrationColor,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = drink.name,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                )
                Text(
                    text = hydrationSavedDrinkAmountImpactLabel(drink, unitFormatter),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 2,
                )
                Text(
                    text = if (drink.nutrientValues.isEmpty()) {
                        stringResource(R.string.hydration_custom_drink_liquid_only)
                    } else {
                        stringResource(
                            R.string.hydration_custom_drink_nutrient_count,
                            drink.nutrientValues.size,
                        )
                    },
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        if (isEditingDrinks) {
            HydrationDrinkEditControl(
                icon = Icons.Outlined.Edit,
                contentDescription = stringResource(R.string.cd_edit_drink),
                onClick = onEditDrink,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp),
            )
            HydrationDrinkEditControl(
                icon = Icons.Outlined.Delete,
                contentDescription = stringResource(R.string.cd_delete_drink),
                onClick = onDeleteDrink,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp),
            )
        }
    }
}

@Composable
private fun HydrationDrinkEditControl(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(36.dp)
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                shape = CircleShape,
            )
            .clickable(onClickLabel = contentDescription, onClick = onClick),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
internal fun HydrationCustomDrinkDialog(
    titleRes: Int,
    unitFormatter: UnitFormatter,
    initialName: String,
    initialMilliliters: Double?,
    initialHydrationMultiplier: Double = FullHydrationImpactMultiplier,
    initialNutrientValues: Map<NutritionNutrient, Double> = emptyMap(),
    onDismiss: () -> Unit,
    onSave: (CustomHydrationDrinkInput) -> Unit,
) {
    var nameText by remember(initialName) { mutableStateOf(initialName) }
    var amountText by remember(initialMilliliters, unitFormatter.unitSystem()) {
        mutableStateOf(hydrationInputAmountText(initialMilliliters, unitFormatter))
    }
    var hydrationImpactOption by remember(initialHydrationMultiplier) {
        mutableStateOf(hydrationImpactOptionForMultiplier(initialHydrationMultiplier))
    }
    var hydrationImpactPercentText by remember(initialHydrationMultiplier) {
        mutableStateOf(hydrationImpactPercentText(initialHydrationMultiplier))
    }
    val resources = LocalContext.current.resources
    val nutrientComparator = Comparator<NutritionNutrient> { first, second ->
        resources.getString(first.titleRes()).compareTo(
            other = resources.getString(second.titleRes()),
            ignoreCase = true,
        )
    }
    var nutrientRows by remember(initialNutrientValues) {
        mutableStateOf(
            initialNutrientValues.entries.sortedWith { first, second ->
                nutrientComparator.compare(first.key, second.key)
            }.map { (nutrient, value) ->
                HydrationNutrientInputRow(
                    nutrient = nutrient,
                    amountText = value.toString(),
                )
            }
        )
    }
    var nutrientChooserOpen by remember { mutableStateOf(false) }
    val amountMilliliters = hydrationInputMilliliters(amountText, unitFormatter.unitSystem())
    val isAmountValid = amountMilliliters?.let(::isValidHydrationContainerMilliliters) == true
    val nutrientValues = nutrientRows.mapNotNull { row ->
        val value = row.amountText.replace(',', '.').toDoubleOrNull()
            ?.takeIf(::isValidCustomDrinkNutrientValue)
            ?: return@mapNotNull null
        row.nutrient to value
    }.toMap()
    val selectedNutrients = nutrientRows.map { it.nutrient }.toSet()
    val availableNutrients = NutritionNutrient.entries
        .filter { it !in selectedNutrients }
        .sortedWith(nutrientComparator)
    val areNutrientsValid = nutrientValues.size == nutrientRows.size
    val canAddNutrient = availableNutrients.isNotEmpty()
    val hydrationMultiplier = hydrationImpactMultiplier(
        option = hydrationImpactOption,
        percentText = hydrationImpactPercentText,
    )
    val isHydrationImpactValid = hydrationMultiplier != null
    val isFormValid = nameText.trim().isNotBlank() && isAmountValid && areNutrientsValid && isHydrationImpactValid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(titleRes))
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text(stringResource(R.string.hydration_custom_drink_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = {
                        Text(
                            stringResource(
                                R.string.hydration_drink_amount_label,
                                hydrationInputUnitLabel(unitFormatter.unitSystem()),
                            )
                        )
                    },
                    isError = amountText.isNotBlank() && !isAmountValid,
                    supportingText = if (amountText.isNotBlank() && !isAmountValid) {
                        {
                            Text(hydrationInputInvalidAmountText(unitFormatter))
                        }
                    } else {
                        null
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                HydrationImpactSelector(
                    selectedOption = hydrationImpactOption,
                    partialPercentText = hydrationImpactPercentText,
                    isPartialPercentValid = isHydrationImpactValid,
                    onOptionSelected = { option ->
                        hydrationImpactOption = option
                        if (option == HydrationImpactOption.PARTIAL &&
                            hydrationImpactMultiplier(option, hydrationImpactPercentText) == null
                        ) {
                            hydrationImpactPercentText = DefaultPartialHydrationImpactPercent.toString()
                        }
                    },
                    onPartialPercentChanged = { text ->
                        hydrationImpactPercentText = text
                    },
                )
                Text(
                    text = stringResource(R.string.hydration_custom_drink_nutrients),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (nutrientRows.isEmpty()) {
                    Text(
                        text = stringResource(R.string.hydration_custom_drink_liquid_only),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                nutrientRows.forEachIndexed { index, row ->
                    HydrationCustomDrinkNutrientRow(
                        row = row,
                        onAmountChanged = { text ->
                            nutrientRows = nutrientRows.mapIndexed { rowIndex, existing ->
                                if (rowIndex == index) existing.copy(amountText = text) else existing
                            }
                        },
                        onRemove = {
                            nutrientRows = nutrientRows.filterIndexed { rowIndex, _ -> rowIndex != index }
                        },
                    )
                }
                HydrationNutrientPicker(
                    enabled = canAddNutrient,
                    onClick = { nutrientChooserOpen = true },
                )
            }
        },
        confirmButton = {
            OpenVitalsTextButton(
                onClick = {
                    amountMilliliters?.takeIf(::isValidHydrationContainerMilliliters)?.let { milliliters ->
                        val impactMultiplier = hydrationMultiplier ?: return@let
                        onSave(
                            CustomHydrationDrinkInput(
                                name = nameText,
                                volumeMilliliters = milliliters,
                                hydrationMultiplier = impactMultiplier,
                                nutrientValues = nutrientValues,
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
        HydrationNutrientChooserDialog(
            availableNutrients = availableNutrients,
            onDismiss = { nutrientChooserOpen = false },
            onSelectNutrient = { nutrient ->
                nutrientRows = (nutrientRows + HydrationNutrientInputRow(nutrient))
                    .sortedWith { first, second ->
                        nutrientComparator.compare(first.nutrient, second.nutrient)
                    }
                nutrientChooserOpen = false
            },
        )
    }
}

@Composable
private fun HydrationNutrientPicker(
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
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = stringResource(R.string.hydration_custom_drink_add_nutrient),
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

@Composable
private fun HydrationNutrientChooserDialog(
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
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                availableNutrients.forEach { nutrient ->
                    HydrationNutrientChoiceRow(
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
private fun HydrationNutrientChoiceRow(
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
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Text(
            text = stringResource(nutrient.titleRes()),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun HydrationImpactSelector(
    selectedOption: HydrationImpactOption,
    partialPercentText: String,
    isPartialPercentValid: Boolean,
    onOptionSelected: (HydrationImpactOption) -> Unit,
    onPartialPercentChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.hydration_custom_drink_hydration_impact),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HydrationImpactOption.entries.forEach { option ->
                HydrationImpactChoiceRow(
                    option = option,
                    selected = selectedOption == option,
                    onClick = { onOptionSelected(option) },
                )
            }
        }
        if (selectedOption == HydrationImpactOption.PARTIAL) {
            OutlinedTextField(
                value = partialPercentText,
                onValueChange = onPartialPercentChanged,
                label = { Text(stringResource(R.string.hydration_impact_percent_label)) },
                isError = partialPercentText.isNotBlank() && !isPartialPercentValid,
                supportingText = if (partialPercentText.isNotBlank() && !isPartialPercentValid) {
                    {
                        Text(stringResource(R.string.hydration_impact_invalid_percent))
                    }
                } else {
                    null
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun HydrationImpactChoiceRow(
    option: HydrationImpactOption,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val supportingTextColor = if (selected) {
        contentColor.copy(alpha = 0.82f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    OpenVitalsSurface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        containerColor = containerColor,
        contentColor = contentColor,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        border = BorderStroke(1.dp, borderColor),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Column(
                modifier = Modifier
                    .padding(start = 10.dp)
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(option.labelRes()),
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    text = stringResource(option.bodyRes()),
                    style = MaterialTheme.typography.bodySmall,
                    color = supportingTextColor,
                )
            }
        }
    }
}

private fun HydrationImpactOption.labelRes(): Int =
    when (this) {
        HydrationImpactOption.FULL -> R.string.hydration_impact_counts_fully
        HydrationImpactOption.PARTIAL -> R.string.hydration_impact_counts_partially
        HydrationImpactOption.NONE -> R.string.hydration_impact_does_not_count
    }

private fun HydrationImpactOption.bodyRes(): Int =
    when (this) {
        HydrationImpactOption.FULL -> R.string.hydration_impact_counts_fully_body
        HydrationImpactOption.PARTIAL -> R.string.hydration_impact_counts_partially_body
        HydrationImpactOption.NONE -> R.string.hydration_impact_does_not_count_body
    }

@Composable
private fun HydrationCustomDrinkNutrientRow(
    row: HydrationNutrientInputRow,
    onAmountChanged: (String) -> Unit,
    onRemove: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
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
            isError = row.amountText.isNotBlank() &&
                row.amountText.replace(',', '.').toDoubleOrNull()
                    ?.let(::isValidCustomDrinkNutrientValue) != true,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private data class HydrationNutrientInputRow(
    val nutrient: NutritionNutrient,
    val amountText: String = "",
)

private enum class HydrationImpactOption {
    FULL,
    PARTIAL,
    NONE,
}

@Composable
private fun nutrientAmountLabel(nutrient: NutritionNutrient): String =
    when (nutrient.unit) {
        NutritionNutrientUnit.ENERGY_KCAL -> stringResource(R.string.hydration_custom_drink_amount_kcal)
        NutritionNutrientUnit.MASS_GRAMS,
        NutritionNutrientUnit.MASS_ADAPTIVE -> stringResource(R.string.hydration_custom_drink_amount_grams)
    }

@Composable
internal fun hydrationEntryErrorText(
    error: HydrationEntryError,
    writeError: ScreenError?,
): String = when (error) {
    HydrationEntryError.INVALID_AMOUNT -> stringResource(R.string.hydration_invalid_amount)
    HydrationEntryError.INVALID_CUSTOM_DRINK -> stringResource(R.string.hydration_custom_drink_invalid)
    HydrationEntryError.MISSING_WRITE_PERMISSION -> stringResource(R.string.hydration_tracker_permission_needed)
    HydrationEntryError.MISSING_NUTRITION_WRITE_PERMISSION -> stringResource(
        R.string.hydration_nutrition_permission_needed
    )
    HydrationEntryError.WRITE_FAILED -> stringResource(
        R.string.hydration_write_failed,
        writeError.resolve() ?: stringResource(R.string.unknown_error),
    )
}

internal fun hydrationAmountLabel(liters: Double, unitFormatter: UnitFormatter): String =
    if (unitFormatter.unitSystem() == UnitSystem.METRIC && liters < 1.0) {
        "${unitFormatter.count((liters * 1000.0).roundToInt())} ml"
    } else {
        unitFormatter.hydration(liters).text
    }

@Composable
private fun hydrationSavedDrinkAmountImpactLabel(
    drink: CustomHydrationDrink,
    unitFormatter: UnitFormatter,
): String {
    val amount = hydrationAmountLabel(drink.volumeLiters, unitFormatter)
    return when {
        drink.hydrationMultiplier == 0.0 -> stringResource(
            R.string.hydration_saved_drink_amount_no_hydration,
            amount,
        )
        abs(drink.hydrationMultiplier - FullHydrationImpactMultiplier) > 0.0001 -> stringResource(
            R.string.hydration_saved_drink_amount_partial_hydration,
            amount,
            (drink.hydrationMultiplier * 100.0).roundToInt(),
        )
        else -> amount
    }
}

private fun hydrationImpactOptionForMultiplier(multiplier: Double): HydrationImpactOption =
    when {
        multiplier <= 0.0 -> HydrationImpactOption.NONE
        abs(multiplier - FullHydrationImpactMultiplier) < 0.0001 -> HydrationImpactOption.FULL
        else -> HydrationImpactOption.PARTIAL
    }

private fun hydrationImpactPercentText(multiplier: Double): String =
    if (multiplier > 0.0 && multiplier < FullHydrationImpactMultiplier) {
        (multiplier * 100.0).roundToInt().coerceIn(1, 99).toString()
    } else {
        DefaultPartialHydrationImpactPercent.toString()
    }

private fun hydrationImpactMultiplier(
    option: HydrationImpactOption,
    percentText: String,
): Double? = when (option) {
    HydrationImpactOption.FULL -> FullHydrationImpactMultiplier
    HydrationImpactOption.NONE -> 0.0
    HydrationImpactOption.PARTIAL -> percentText.trim()
        .replace(',', '.')
        .toDoubleOrNull()
        ?.takeIf { it > 0.0 && it < 100.0 }
        ?.let { it / 100.0 }
}

internal fun hydrationInputMilliliters(
    input: String,
    unitSystem: UnitSystem,
): Double? {
    val value = input.trim().replace(',', '.').toDoubleOrNull() ?: return null
    return when (unitSystem) {
        UnitSystem.METRIC -> value
        UnitSystem.IMPERIAL -> value * MillilitersPerFluidOunce
    }
}

internal fun hydrationInputAmountText(
    milliliters: Double?,
    unitFormatter: UnitFormatter,
): String {
    if (milliliters == null) return ""
    return when (unitFormatter.unitSystem()) {
        UnitSystem.METRIC -> unitFormatter.count(milliliters.roundToInt())
        UnitSystem.IMPERIAL -> unitFormatter.decimal(milliliters / MillilitersPerFluidOunce, 1)
    }
}

internal fun hydrationInputUnitLabel(unitSystem: UnitSystem): String =
    when (unitSystem) {
        UnitSystem.METRIC -> "ml"
        UnitSystem.IMPERIAL -> "fl oz"
    }

@Composable
private fun hydrationInputInvalidAmountText(unitFormatter: UnitFormatter): String {
    val unitSystem = unitFormatter.unitSystem()
    val unitLabel = hydrationInputUnitLabel(unitSystem)
    val minimum = when (unitSystem) {
        UnitSystem.METRIC -> unitFormatter.count(MinHydrationContainerMilliliters.roundToInt())
        UnitSystem.IMPERIAL -> unitFormatter.decimal(
            MinHydrationContainerMilliliters / MillilitersPerFluidOunce,
            2,
        )
    }
    val maximum = when (unitSystem) {
        UnitSystem.METRIC -> unitFormatter.count(MaxHydrationContainerMilliliters.roundToInt())
        UnitSystem.IMPERIAL -> unitFormatter.count(
            (MaxHydrationContainerMilliliters / MillilitersPerFluidOunce).roundToInt()
        )
    }
    return stringResource(
        R.string.hydration_drink_invalid_amount_range,
        "$minimum $unitLabel",
        "$maximum $unitLabel",
    )
}
