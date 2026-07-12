import 'package:flutter/material.dart';

import '../../../core/presentation/measurement_input.dart';
import '../../../core/presentation/unit_formatter.dart';
import '../../../domain/model/caffeine_models.dart';
import '../../../domain/model/nutrition_models.dart';
import '../../../l10n/app_localizations.dart';
import '../../nutrition/nutrition_formatting.dart';
import '../application/hydration_entry_view_model.dart';
import 'manual_entry_timestamp_fields.dart';

/// Formats a double as a compact string (trailing zeros trimmed). Used for
/// nutrient amounts, which carry no unit-system conversion.
String trimDecimals(double value) {
  var text = value.toStringAsFixed(2);
  if (text.contains('.')) {
    text = text.replaceAll(RegExp(r'0+$'), '').replaceAll(RegExp(r'\.$'), '');
  }
  return text;
}

/// Human label for a drink category. Port of the Kotlin category string map.
String caffeineCategoryLabel(
  CaffeineSourceCategory category,
  AppLocalizations l10n,
) =>
    switch (category) {
      CaffeineSourceCategory.water => l10n.hydrationCatalogSectionWater,
      CaffeineSourceCategory.coffee => l10n.hydrationCatalogSectionCoffees,
      CaffeineSourceCategory.tea => l10n.hydrationCatalogSectionTeas,
      CaffeineSourceCategory.energyDrink =>
        l10n.hydrationCatalogSectionEnergyDrinks,
      CaffeineSourceCategory.soda =>
        l10n.hydrationCatalogSectionCarbonatedSoftDrinks,
      CaffeineSourceCategory.chocolate =>
        l10n.hydrationCatalogSectionChocolateDrinks,
      CaffeineSourceCategory.supplement ||
      CaffeineSourceCategory.other =>
        l10n.hydrationCatalogSectionOtherDrinks,
    };

/// The per-nutrient amount field label, keyed off its unit. Port of the Kotlin
/// `nutrientAmountLabel`.
String nutrientAmountLabel(NutritionNutrient nutrient, AppLocalizations l10n) =>
    nutrient.unit == NutritionNutrientUnit.energyKcal
        ? l10n.hydrationCustomDrinkAmountKcal
        : l10n.hydrationCustomDrinkAmountGrams;

/// The amount + time chosen when logging a saved drink.
class SavedDrinkEntryResult {
  const SavedDrinkEntryResult({required this.amountMilliliters, this.entryTime});

  final double amountMilliliters;
  final DateTime? entryTime;
}

/// Port of the Kotlin `HydrationSavedDrinkEntryDialog`: pick how much of a saved
/// drink was consumed, and when. Returns null when dismissed.
Future<SavedDrinkEntryResult?> showSavedDrinkEntryDialog(
  BuildContext context,
  CustomHydrationDrink drink,
  UnitFormatter formatter,
) {
  return showDialog<SavedDrinkEntryResult>(
    context: context,
    builder: (context) =>
        _SavedDrinkEntryDialog(drink: drink, formatter: formatter),
  );
}

class _SavedDrinkEntryDialog extends StatefulWidget {
  const _SavedDrinkEntryDialog({required this.drink, required this.formatter});

  final CustomHydrationDrink drink;
  final UnitFormatter formatter;

  @override
  State<_SavedDrinkEntryDialog> createState() => _SavedDrinkEntryDialogState();
}

class _SavedDrinkEntryDialogState extends State<_SavedDrinkEntryDialog> {
  /// Seeded in the user's own unit, not raw millilitres.
  late final TextEditingController _amount = TextEditingController(
    text: widget.formatter.millilitersToVolumeInput(
      widget.drink.volumeMilliliters,
    ),
  );
  DateTime _time = DateTime.now();
  bool _invalid = false;

  @override
  void dispose() {
    _amount.dispose();
    super.dispose();
  }

  void _submit() {
    final amount = widget.formatter.volumeInputToMilliliters(_amount.text);
    if (amount == null || !isValidHydrationContainerMilliliters(amount)) {
      setState(() => _invalid = true);
      return;
    }
    Navigator.of(context)
        .pop(SavedDrinkEntryResult(amountMilliliters: amount, entryTime: _time));
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    return AlertDialog(
      title: Text(widget.drink.name),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          TextField(
            controller: _amount,
            autofocus: true,
            keyboardType: const TextInputType.numberWithOptions(decimal: true),
            decoration: InputDecoration(
              border: const OutlineInputBorder(),
              labelText:
                  l10n.hydrationDrinkAmountLabel(widget.formatter.volumeInputUnit),
              errorText: _invalid
                  ? l10n.hydrationDrinkInvalidAmountRange(
                      widget.formatter
                          .millilitersBoundLabel(kMinHydrationContainerMilliliters),
                      widget.formatter
                          .millilitersBoundLabel(kMaxHydrationContainerMilliliters),
                    )
                  : null,
            ),
            onChanged: (_) {
              if (_invalid) setState(() => _invalid = false);
            },
          ),
          const SizedBox(height: 12),
          ManualEntryTimestampFields(
            timestamp: _time,
            enabled: true,
            onChanged: (value) => setState(() => _time = value),
          ),
        ],
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(context).pop(),
          child: Text(l10n.actionCancel),
        ),
        FilledButton(onPressed: _submit, child: Text(l10n.actionSave)),
      ],
    );
  }
}

/// Port of the Kotlin `HydrationCustomDrinkDialog`: create or edit a saved drink
/// — name, volume, category, hydration impact and nutrient values. Returns null
/// when dismissed.
Future<CustomHydrationDrinkInput?> showCustomDrinkDialog(
  BuildContext context,
  UnitFormatter formatter, {
  CustomHydrationDrink? existing,
}) {
  return showDialog<CustomHydrationDrinkInput>(
    context: context,
    builder: (context) =>
        _CustomDrinkDialog(existing: existing, formatter: formatter),
  );
}

class _CustomDrinkDialog extends StatefulWidget {
  const _CustomDrinkDialog({required this.formatter, this.existing});

  final UnitFormatter formatter;
  final CustomHydrationDrink? existing;

  @override
  State<_CustomDrinkDialog> createState() => _CustomDrinkDialogState();
}

class _CustomDrinkDialogState extends State<_CustomDrinkDialog> {
  late final TextEditingController _name =
      TextEditingController(text: widget.existing?.name ?? '');
  /// Seeded in the user's own unit, not raw millilitres.
  late final TextEditingController _volume = TextEditingController(
    text: widget.formatter
        .millilitersToVolumeInput(widget.existing?.volumeMilliliters),
  );
  late final TextEditingController _percent = TextEditingController(
    text: hydrationImpactPercentText(
      widget.existing?.hydrationMultiplier ?? kFullHydrationImpactMultiplier,
    ),
  );

  late CaffeineSourceCategory? _category = widget.existing?.category;
  late HydrationImpactOption _impact = hydrationImpactOptionForMultiplier(
    widget.existing?.hydrationMultiplier ?? kFullHydrationImpactMultiplier,
  );
  late final Map<NutritionNutrient, TextEditingController> _nutrients = {
    for (final entry in (widget.existing?.nutrientValues ?? const {}).entries)
      entry.key: TextEditingController(text: trimDecimals(entry.value)),
  };

  String? _error;

  @override
  void dispose() {
    _name.dispose();
    _volume.dispose();
    _percent.dispose();
    for (final controller in _nutrients.values) {
      controller.dispose();
    }
    super.dispose();
  }

  Future<void> _addNutrient() async {
    final remaining = [
      for (final nutrient in NutritionNutrient.values)
        if (!_nutrients.containsKey(nutrient)) nutrient,
    ];
    if (remaining.isEmpty) return;
    final picked = await showDialog<NutritionNutrient>(
      context: context,
      builder: (context) {
        final l10n = AppLocalizations.of(context);
        return SimpleDialog(
          title: Text(l10n.hydrationCustomDrinkAddNutrient),
          children: [
            for (final nutrient in remaining)
              SimpleDialogOption(
                onPressed: () => Navigator.of(context).pop(nutrient),
                child: Text(nutrientTitle(nutrient, l10n)),
              ),
          ],
        );
      },
    );
    if (picked == null || !mounted) return;
    setState(() => _nutrients[picked] = TextEditingController());
  }

  /// Mirrors the Kotlin validation order so the same input yields the same
  /// rejection: name, volume, impact multiplier, then nutrient values.
  void _submit() {
    final l10n = AppLocalizations.of(context);
    final name = _name.text.trim();
    final volume = widget.formatter.volumeInputToMilliliters(_volume.text);
    if (name.isEmpty ||
        volume == null ||
        !isValidHydrationContainerMilliliters(volume)) {
      setState(() => _error = l10n.hydrationCustomDrinkInvalid);
      return;
    }
    final multiplier = hydrationImpactMultiplier(_impact, _percent.text);
    if (multiplier == null) {
      setState(() => _error = l10n.hydrationImpactInvalidPercent);
      return;
    }
    final values = <NutritionNutrient, double>{};
    for (final entry in _nutrients.entries) {
      final raw = entry.value.text.trim();
      if (raw.isEmpty) continue;
      // Nutrient amounts are unitless (grams / kcal), so no conversion here.
      final value = parseDecimalInput(raw);
      if (value == null || !isValidCustomDrinkNutrientValue(value)) {
        setState(() => _error = l10n.hydrationCustomDrinkInvalid);
        return;
      }
      values[entry.key] = value;
    }
    Navigator.of(context).pop(
      CustomHydrationDrinkInput(
        name: name,
        volumeMilliliters: volume,
        hydrationMultiplier: multiplier,
        category: _category,
        nutrientValues: values,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    return AlertDialog(
      title: Text(widget.existing == null
          ? l10n.hydrationNewDrinkTitle
          : l10n.hydrationEditDrinkTitle),
      content: SizedBox(
        width: 380,
        child: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              TextField(
                controller: _name,
                decoration: InputDecoration(
                  border: const OutlineInputBorder(),
                  labelText: l10n.hydrationCustomDrinkName,
                ),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: _volume,
                keyboardType:
                    const TextInputType.numberWithOptions(decimal: true),
                decoration: InputDecoration(
                  border: const OutlineInputBorder(),
                  labelText:
                      l10n.hydrationDrinkAmountLabel(widget.formatter.volumeInputUnit),
                ),
              ),
              const SizedBox(height: 12),
              DropdownButtonFormField<CaffeineSourceCategory?>(
                initialValue: _category,
                // The longest label ("Carbonated soft drinks") is wider than the
                // field, so let the button fill it and ellipsize instead of
                // overflowing the dialog.
                isExpanded: true,
                decoration: InputDecoration(
                  border: const OutlineInputBorder(),
                  labelText: l10n.hydrationCustomDrinkCategory,
                ),
                items: [
                  DropdownMenuItem<CaffeineSourceCategory?>(
                    child: Text(
                      l10n.hydrationCustomDrinkNoCategory,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                  for (final category in CaffeineSourceCategory.values)
                    DropdownMenuItem<CaffeineSourceCategory?>(
                      value: category,
                      child: Text(
                        caffeineCategoryLabel(category, l10n),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                ],
                onChanged: (value) => setState(() => _category = value),
              ),
              const SizedBox(height: 16),
              Align(
                alignment: Alignment.centerLeft,
                child: Text(
                  l10n.hydrationCustomDrinkHydrationImpact,
                  style: theme.textTheme.labelLarge,
                ),
              ),
              const SizedBox(height: 8),
              RadioGroup<HydrationImpactOption>(
                groupValue: _impact,
                onChanged: (value) => setState(() => _impact = value!),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    for (final option in HydrationImpactOption.values)
                      RadioListTile<HydrationImpactOption>(
                        value: option,
                        contentPadding: EdgeInsets.zero,
                        title: Text(_impactLabel(option, l10n)),
                        subtitle: Text(
                          _impactBody(option, l10n),
                          style: theme.textTheme.bodySmall,
                        ),
                      ),
                  ],
                ),
              ),
              if (_impact == HydrationImpactOption.partial)
                Padding(
                  padding: const EdgeInsets.only(top: 4),
                  child: TextField(
                    controller: _percent,
                    keyboardType: TextInputType.number,
                    decoration: InputDecoration(
                      border: const OutlineInputBorder(),
                      labelText: l10n.hydrationImpactPercentLabel,
                    ),
                  ),
                ),
              const SizedBox(height: 16),
              Row(
                children: [
                  Expanded(
                    child: Text(
                      l10n.hydrationCustomDrinkNutrients,
                      style: theme.textTheme.labelLarge,
                    ),
                  ),
                  TextButton.icon(
                    onPressed: _addNutrient,
                    icon: const Icon(Icons.add, size: 18),
                    label: Text(l10n.hydrationCustomDrinkAddNutrient),
                  ),
                ],
              ),
              if (_nutrients.isEmpty)
                Align(
                  alignment: Alignment.centerLeft,
                  child: Text(
                    l10n.hydrationCustomDrinkLiquidOnly,
                    style: theme.textTheme.bodySmall?.copyWith(
                      color: theme.colorScheme.onSurfaceVariant,
                    ),
                  ),
                ),
              for (final nutrient in _nutrients.keys.toList())
                Padding(
                  padding: const EdgeInsets.only(top: 8),
                  child: Row(
                    children: [
                      Expanded(
                        child: TextField(
                          controller: _nutrients[nutrient],
                          keyboardType: const TextInputType.numberWithOptions(
                            decimal: true,
                          ),
                          decoration: InputDecoration(
                            border: const OutlineInputBorder(),
                            labelText:
                                '${nutrientTitle(nutrient, l10n)} · '
                                '${nutrientAmountLabel(nutrient, l10n)}',
                          ),
                        ),
                      ),
                      IconButton(
                        tooltip: l10n.actionDelete,
                        icon: const Icon(Icons.close),
                        onPressed: () => setState(() {
                          _nutrients.remove(nutrient)?.dispose();
                        }),
                      ),
                    ],
                  ),
                ),
              if (_error != null)
                Padding(
                  padding: const EdgeInsets.only(top: 12),
                  child: Text(
                    _error!,
                    style: theme.textTheme.bodySmall
                        ?.copyWith(color: theme.colorScheme.error),
                  ),
                ),
            ],
          ),
        ),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(context).pop(),
          child: Text(l10n.actionCancel),
        ),
        FilledButton(onPressed: _submit, child: Text(l10n.actionSave)),
      ],
    );
  }

  String _impactLabel(HydrationImpactOption option, AppLocalizations l10n) =>
      switch (option) {
        HydrationImpactOption.full => l10n.hydrationImpactCountsFully,
        HydrationImpactOption.partial => l10n.hydrationImpactCountsPartially,
        HydrationImpactOption.none => l10n.hydrationImpactDoesNotCount,
      };

  String _impactBody(HydrationImpactOption option, AppLocalizations l10n) =>
      switch (option) {
        HydrationImpactOption.full => l10n.hydrationImpactCountsFullyBody,
        HydrationImpactOption.partial => l10n.hydrationImpactCountsPartiallyBody,
        HydrationImpactOption.none => l10n.hydrationImpactDoesNotCountBody,
      };
}
