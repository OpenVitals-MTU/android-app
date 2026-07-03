# Preloaded Beverage Nutrition Reference

> **Status:** Current implemented reference data.
> **Audience:** Users and contributors.
> **Implementation:** `features/manualentry/hydration`, `features/nutrition`.
> **Navigation:** beverage logging from hydration entry routes.
> **Related:** [Feature map](feature-map.md), [Beverage logging and caffeine](beverage-logging-and-caffeine.md), [Nutrition](nutrition.md).

Research date: 2026-07-02

This note covers the beverage presets imported from `CaffeineHealthDrinkCatalog`
plus the OpenVitals water seed. The app currently exposes 215 preloaded beverage
presets with ordinary drink serving sizes: 1 water drink, 83 coffee drinks,
91 energy drinks, 16 tea drinks, 21 carbonated soft drinks, and 3 chocolate drinks.

The catalog itself stores name, category, default serving volume, and caffeine. It
does not store a complete Nutrition Facts panel. The table below is therefore a
reference for the beverage families represented by the preloaded catalog, not a
claim that every branded SKU in the catalog has one universal label. Branded products
change by market, package size, recipe, and date; use the linked sources or package
label when an exact entry needs to be logged.

## Nutrition Table

Values are per common serving unless noted. Ranges mean the preloaded catalog contains
multiple branded items in that family.

| Preloaded beverage type | Catalog examples | Serving basis | kcal | Fat g | Sat fat g | Carb g | Sugar g | Fiber g | Protein g | Sodium mg | Potassium mg | Calcium mg | Caffeine mg | Notes and source |
|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|
| Water | Water | 100 ml | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | OpenVitals local seed: no nutrients, 100 percent hydration impact, displayed in metric or imperial units through the app unit formatter. |
| Brewed black coffee | Drip coffee, K-Cup, Kirkland Breakfast Blend, Merrild, Peter Larsen | 240 ml | 2 | 0.05 | 0 | 0 | 0 | 0 | 0.3 | 5 | 116 | 5 | 95 | USDA/MyFoodData brewed coffee reports 2 kcal per 237 g and near-zero macros; catalog caffeine is 95 mg for drip coffee. Source: https://tools.myfooddata.com/nutrition-facts/171890/wt3 |
| Espresso and coffee pods | Espresso, Americano base, Nespresso pods, Costa Espresso | 60 ml double espresso reference | 5 | 0.1 | 0 | 1.0 | 0 | 0 | 0.1 | 8 | 69 | 1 | 127 | USDA/MyFoodData espresso lists 127.2 mg caffeine per 60 g. Catalog single-shot presets vary from 33 to 100 mg. Source: https://tools.myfooddata.com/nutrition-comparison/2710378/wt1 |
| Instant coffee prepared with water | Instant Coffee, Nescafe Classic, Nescafe Gold, decaf instant | 240 ml | 2-4 | 0 | 0 | 0-1 | 0 | 0 | 0 | 0-5 | 80-120 | 0-5 | 2-75 | Prepared instant coffee is nutritionally close to brewed coffee; caffeine depends heavily on powder amount and decaf status. Source: https://tools.myfooddata.com/nutrition-facts/171893/wt1 |
| Milk espresso drinks | Latte, flat white, macchiato, cafe au lait, iced latte | 12 fl oz / 354 ml | 100-185 | 0-6 | 0-3.5 | 14-16 | 12-15 | 0 | 7-10 | 100-150 | 350-450 | 250-350 | 66-160 | Mostly milk nutrition plus espresso. Starbucks-style tall latte references range from 100 kcal nonfat to 150 kcal 2 percent milk; protein-milk variants are higher protein. Sources: https://www.starbucks.com/menu/product/407/hot/nutrition and https://www.mynetdiary.com/food/calories-in-caffe-latte-tall-by-starbucks-cup-10995816-0.html |
| Cappuccino | Cappuccino, Caffe Nero Cappuccino, Costa Cappuccino, Dunkin Cappuccino | 12 fl oz / 354 ml | 90-135 | 3-8 | 2-4.5 | 9-10 | 9 | 0 | 6.5-7 | 80-120 | 250-350 | 200-260 | 66-200 | Milk volume and shop recipe drive calories more than espresso. Sources: https://www.starbucks.com/menu/product/409/hot/nutrition and https://www.getmistapp.com/starbucks-calorie-calculator/tall-cappuccino |
| Mocha, dunkaccino, sweet flavored coffee | Cafe mocha, white mocha, Dunkaccino, mocha frappe | 12-14 fl oz | 195-330 | 8-13 | 5-8 | 34-59 | 28-46 | 0-2 | 2-12 | 150-320 | 250+ | 40-350 | 66-170 | These behave nutritionally more like dessert drinks: milk plus sweet sauce or cocoa. Sources: https://www.starbucks.com/menu/product/408/hot/nutrition and https://www.dunkindonuts.com/content/dam/dd/pdf/nutrition.pdf |
| Coffee protein shake | Atkins Iced Coffee Protein Shake | 11 fl oz / 325 ml | 160-170 | 9 | 1.5 | 6-7 | 1 | 3-5 | 15 | 240 | -- | 350 | ~70-95 | Protein/fiber are intentional formula features, unlike normal coffee. Sources: https://shop.atkins.com/products/cafe-au-lait-iced-coffee-shake and https://hannaford.com/groceries/health-beauty/nutrition-weight-management/protein-meal-replacement-shakes/atkins-iced-coffee-mocha-latte-15g-protein-shakes-4-pk-11-oz-btls.html |
| Unsweetened black tea | Tea (Black), Irish Tea, Dunkin Hot Tea | 240 ml | 2 | 0 | 0 | 0.7 | 0 | 0 | 0 | 0-7 | 50-90 | 0 | 48-90 | USDA black tea is near-zero calories; caffeine depends on tea mass, brew time, and serving size. Source: https://tools.myfooddata.com/nutrition-facts/173227/wt1 |
| Unsweetened green, jasmine, and matcha tea | Tea (Green), Green Tea, Tea (Jasmine), Matcha Tea | 240 ml | 0-5 | 0 | 0 | 0-1 | 0 | 0 | 0-0.5 | 0-17 | 45-90 | 0-5 | 25-64 | Brewed green tea is near-zero calories. Matcha has more caffeine and tiny leaf-derived solids because the leaf is consumed. Source: https://tools.myfooddata.com/nutrition-facts/171911/wt1 |
| Herbal tea | Tea (Herbal) | 240 ml | 0-2 | 0 | 0 | 0-0.5 | 0 | 0 | 0 | 0-2 | 20 | 0-5 | 0 | Herbal teas are caffeine-free unless blended with tea, yerba mate, guarana, or added caffeine. Source: https://tools.myfooddata.com/nutrition-facts/174156/wt1 |
| Sweetened or bottled iced tea | Lipton Iced Tea, Fuze Tea, Nestea, Dunkin Iced Tea | 12-16.9 fl oz | 68-130 | 0 | 0 | 17-33 | 17-29 | 0 | 0 | 90-180 | -- | -- | 12-67 | Bottled/sweetened tea is mostly sugar water plus tea extract. Lipton Lemon lists 100 kcal, 25 g carbs/sugar, and 21 mg caffeine per 16.9 fl oz bottle. Sources: https://www.liptonicedtea.com/en-us/our-iced-teas/black-iced-tea/iced-tea-lemon and https://www.pepsicoproductfacts.com/Home/product?gtin=00012000018633 |
| Ready-to-drink yerba mate | Guayaki Yerba Mate, Yerba Mate tea | 15.5 fl oz / 458 ml | 15-120 | 0 | 0 | 1-31 | 0-28 | 0 | 0-1 | 0-15 | -- | -- | 40-150 | Unsweetened yerba mate is light; canned sweetened products are closer to sweet tea. Sources: https://yerbamadre.com/products/enlightenmint and https://www.ewg.org/foodscores/products/632432757773-GuayakiYerbaMateEnlightenMint/ |
| Hot cocoa and chocolate drinks | Hot Cocoa, Swiss Miss Hot Chocolate, Dunkin Hot Chocolate | 240-414 ml | 70-330 | 1.5-11 | 1.5-9 | 11-59 | 11-46 | 0-2 | 1-3 | 170-320 | 250 | 40+ | 6-25+ | Cocoa naturally contributes small caffeine/theobromine; cafe hot chocolates are much higher calorie and sugar than packet mixes. Sources: https://www.swissmiss.com/indulgent-collection/double-chocolate-hot-cocoa-mix, https://www.kdpproductfacts.com/product/a0e3h000003LKHQAA4/swiss-miss-hot-cocoa-milk-chocolate-hot-cocoa-mix-kcup-pod-us, and https://www.starbucks.com/menu/product/471/hot/nutrition |
| Regular cola and caffeinated soda | Coca-Cola, Pepsi, Cheerwine, Kofola, Faygo Cola, Shasta Cola | 12 fl oz / 355 ml | 140-150 | 0 | 0 | 39-43 | 39-43 | 0 | 0 | 40-85 | 0-15 | 0-7 | 32-54 | Full-sugar sodas are mostly sugar and carbonated water. Coca-Cola US lists 150 kcal, 39 g carbs/sugar, 85 mg sodium, and 34 mg caffeine per 355 ml bottle. Sources: https://www.coca-cola.com/us/en/brands/coca-cola/products/original and https://tools.myfooddata.com/nutrition-facts/174852/wt3 |
| Diet or zero-sugar cola | Diet Coke, Coke Zero, Pepsi Max, Red Bull Zero-like soda rows | 12 fl oz / 355 ml | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 25-40 | 0 | 0 | 32-46 | Nutrition is mostly sodium/preservatives/acids/sweeteners; caffeine is still present unless caffeine-free. Sources: https://www.coca-cola.com/us/en/brands/diet-coke/products and https://www.coca-cola.com/us/en/brands/coca-cola/products/zero |
| Citrus or pepper caffeinated soft drinks | Mountain Dew, Mountain Dew Code Red, Dr Pepper, Paulaner Spezi | 12 fl oz / 355 ml | 140-170 | 0 | 0 | 38-46 | 38-46 | 0 | 0 | 45-85 | -- | -- | 42-54 | Similar macro profile to regular cola, but caffeine and sodium vary by brand. Catalog caffeine is generally 42-54 mg. Source baseline: https://tools.myfooddata.com/nutrition-facts/174852/wt3 |
| Full-sugar energy drink | Red Bull, Monster Energy, Full Throttle, Burn, Hell, Rip It | 250-473 ml | 110-230 | 0 | 0 | 27-58 | 27-54 | 0 | 0-1 | 105-370 | -- | -- | 80-200 | Red Bull 250 ml lists 80 mg caffeine and 27 g sugar. Monster-style 16 fl oz products often list 160 mg caffeine and 54 g sugar. Sources: https://www.redbull.com/int-en/energydrink/products/red-bull-energy-drink-ingredients-list and https://www.jerrysfoods.com/store/jerrys-food/products/22799306-monster-energy-original-16-oz |
| Sugar-free or low-calorie energy drink | Red Bull Sugarfree, Monster Ultra, C4, Celsius, Bang, Reign, NOCCO | 355-500 ml | 0-15 | 0 | 0 | 0-2 | 0 | 0 | 0 | 0-85 | 0-85 | 0-5 | 80-300 | Calories and sugar are low, but caffeine may be high. Bang-style drinks can reach 300 mg; C4 lists 200 mg. Sources: https://www.bangenergy.com/en-us/, https://cellucor.com/products/c4-original-carbonated, and https://www.heb.com/product-detail/c4-performance-zero-sugar-energy-drink-orange-slice/4131034 |
| Energy shots and concentrated energy | 5 Hour Energy Extra Strength, Eternal Energy | 57-59 ml | 0-4 | 0 | 0 | 0 | 0 | 0 | 0 | 0-15 | -- | -- | 190-230 | Very high caffeine density; nutrition label may be Supplement Facts rather than Nutrition Facts. Sources: https://5hourenergy.com/blogs/the-feed/5-hour-energy-caffeine-facts and https://www.amazon.com/5-Hour-Energy-Strength-Dietary-Supplement/dp/B007W86RUA |
| Juice, tea, or vegetable energy drink | V8+ Energy, Celsius tea flavors, Monster Juice/Rehab | 237-473 ml | 50-230 | 0 | 0 | 10-56 | 10-50 | 0-1 | 0-1 | 40-240 | -- | -- | 80-200 | These add fruit/vegetable juice or tea bases, so sugar and calories vary more than zero-sugar energy drinks. V8 Energy lists 80 mg caffeine and fruit/vegetable juice content. Source: https://www.campbells.com/v8/products/v8-energy/peach-mango/ |
| Pre-workout or caffeinated drink mix | Gorilla Mode Pre-workout, Gamersups, Mio Energy, C4 drink mix | prepared serving | 0-20 | 0 | 0 | 0-5 | 0 | 0 | 0 | 0-40 | -- | -- | 60-350 | Many are dietary supplements, not ordinary beverages; ingredients can include beta-alanine, amino acids, niacin, or other actives. Source example: https://cellucor.com/pages/c4-energy-nutrition-facts |

## Catalog Mapping Implications

- The runtime beverage catalog is Room-backed. `CaffeineHealthDrinkCatalog` is seed
  and matching metadata, while user edits/deletes/category moves persist in the
  local beverage table.
- OpenVitals should keep Health Connect as the source of truth for logged nutrient
  amounts. The preloaded catalog can safely provide defaults, but user edits should
  override defaults before write.
- Preloaded default nutrients are limited to Health Connect `NutritionRecord` mirrors:
  energy, total fat, saturated fat, total carbohydrate, sugar, dietary fiber, protein,
  sodium, potassium, calcium, and caffeine.
- The current UI categories map cleanly to nutrition families:
  - `COFFEE`: brewed coffee, espresso/pods, milk espresso drinks, sweet coffee drinks,
    coffee protein shakes.
  - `TEA`: unsweetened tea, sweetened iced tea, matcha/chai latte, yerba mate.
  - `CHOCOLATE`: hot cocoa and cafe hot chocolate.
  - `CARBONATED_SOFT_DRINK`: cola, diet/zero cola, citrus/pepper caffeinated soft drinks.
  - `ENERGY_DRINK`: full-sugar energy drinks, sugar-free energy drinks, energy shots,
    juice/tea energy drinks, caffeinated mixes.
  - `OTHER`: fallback only when a preset cannot be classified from the catalog metadata.

## Source Notes

- USDA FoodData Central is the preferred public-domain baseline for generic foods:
  https://fdc.nal.usda.gov/
- MyFoodData pages are used as readable USDA/FDC mirrors for several generic rows:
  brewed coffee, espresso, black tea, green tea, herbal tea, cola, energy drink, and
  hot cocoa.
- Manufacturer pages are used for branded rows where formulas are product-specific:
  Coca-Cola, Diet Coke, Red Bull, Starbucks, Dunkin, Lipton, Atkins, Swiss Miss,
  Yerba Madre/Guayaki, V8, C4, Bang, and 5-hour Energy.
