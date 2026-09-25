# Food Logging

> **Status:** Current implemented behavior.
> **Audience:** Users and contributors.
> **Implementation:** `features/manualentry/food`, `features/manualentry/nutrition`, `data/local/food`, `data/repository/FoodRepository.kt`.
> **Navigation:** `Screen.FoodEntry`; entry widget `ManualEntryWidgetId.FOOD`.
> **Related:** [Feature map](feature-map.md), [Nutrition](nutrition.md), [Beverage logging and caffeine](beverage-logging-and-caffeine.md), [Manual entry of metrics](manual-entry-metrics.md).

Food logging works like beverage logging: a catalog the user keeps, and a Health Connect record for each portion eaten.

## The Food Catalog

There are no preloaded foods. The user makes each one with **New food**:

- a name,
- an amount in grams (or ounces, under imperial units),
- a category, or none,
- one or more nutrients, for that amount.

Any nutrient Health Connect supports can be added: energy, the macros, fibre, sugar, the fat types, vitamins, minerals, and caffeine. A food needs at least one nutrient; a portion with none would have nothing to write.

The catalog lives in Room, in `foods` and `food_nutrients`. Deleting a food hides it; the row stays. Foods without a category are listed first, then each category in order. A search field appears once the list is long.

## Logging A Portion

Tapping a food asks for the amount eaten, with the food's own amount preselected, and when it was eaten. The nutrients are scaled by amount eaten over the food's amount and written as one Health Connect nutrition record named after the food. Its client record id starts with `openvitals_food_` and carries the food's id.

The beverage history and the frequent-drinks list skip records with that prefix, so a food never appears as a drink, even one with caffeine or a drink's name. The nutrition screens list it under meals; calories in, protein, carbohydrates and fat count it. The caffeine screen counts a caffeinated food as a caffeine source.

A logged portion can be deleted from the nutrition screens. It cannot be edited after the fact.

## Permission

Writing needs Health Connect nutrition write permission. The entry screen asks for it when it is missing. OpenVitals keeps the catalog only; each logged portion is stored in Health Connect.
