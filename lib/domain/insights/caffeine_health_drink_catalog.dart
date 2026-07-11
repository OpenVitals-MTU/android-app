import '../model/caffeine_models.dart';
import '../model/nutrition_models.dart';
import 'beverage_nutrition_defaults.dart';

/// Faithful port of `CaffeineHealthDrinkCatalog` from the Kotlin app.
///
/// Static matching metadata derived from CaffeineHealth GPL-3.0
/// consumable_items.json. OpenVitals uses it as seed/matching metadata for
/// beverages; Health Connect remains the source of truth for logged nutrition
/// records.
class CaffeineHealthDrinkCatalog {
  CaffeineHealthDrinkCatalog._();

  static const String _beveragePresetIdPrefix = 'caffeinehealth-';

  static final List<CaffeineCatalogItem> items = <CaffeineCatalogItem>[

        CaffeineCatalogItem(
            id: "drip-coffee",
            name: "Drip coffee",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 95.0,
            defaultServingMilliliters: 240.0,
            aliases: ["coffee", "brewed coffee", "filter coffee", "regular coffee"],
        ),
        CaffeineCatalogItem(
            id: "espresso",
            name: "Espresso",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 77.0,
            defaultServingMilliliters: 44.3,
            aliases: ["single espresso", "double espresso", "espresso shot"],
        ),
        CaffeineCatalogItem(
            id: "instant-coffee",
            name: "Instant Coffee",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 57.0,
            defaultServingMilliliters: 240.0,
        ),
        CaffeineCatalogItem(
            id: "tea-black",
            name: "Tea (Black)",
            category: CaffeineSourceCategory.tea,
            typicalCaffeineMg: 48.0,
            defaultServingMilliliters: 240.0,
            aliases: ["black tea"],
        ),
        CaffeineCatalogItem(
            id: "costa-espresso",
            name: "Costa Espresso",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 100.0,
            defaultServingMilliliters: 30.0,
        ),
        CaffeineCatalogItem(
            id: "americano",
            name: "Americano",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 77.0,
            defaultServingMilliliters: 30.0,
            aliases: ["cafe americano", "caffe americano"],
        ),
        CaffeineCatalogItem(
            id: "monster-energy-ultra",
            name: "Monster Energy Ultra",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 150.0,
            defaultServingMilliliters: 473.0,
            aliases: ["monster ultra"],
        ),
        CaffeineCatalogItem(
            id: "decaf-instant-coffee",
            name: "Decaf instant coffee",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 2.4,
            defaultServingMilliliters: 240.0,
        ),
        CaffeineCatalogItem(
            id: "coca-cola-zero",
            name: "Coca-Cola Zero",
            category: CaffeineSourceCategory.soda,
            typicalCaffeineMg: 32.0,
            defaultServingMilliliters: 333.0,
            aliases: ["coke zero"],
        ),
        CaffeineCatalogItem(
            id: "cappuccino",
            name: "Cappuccino",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 77.0,
            defaultServingMilliliters: 200.0,
            aliases: ["capuccino"],
        ),
        CaffeineCatalogItem(
            id: "pepsi",
            name: "Pepsi",
            category: CaffeineSourceCategory.soda,
            typicalCaffeineMg: 38.0205,
            defaultServingMilliliters: 355.0,
        ),
        CaffeineCatalogItem(
            id: "nescafe-classic",
            name: "Nescaf\u00e9 Classic",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 75.0,
            defaultServingMilliliters: 240.0,
        ),
        CaffeineCatalogItem(
            id: "red-bull",
            name: "Red Bull",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 80.0,
            defaultServingMilliliters: 250.0,
            aliases: ["redbull", "red bull energy"],
        ),
        CaffeineCatalogItem(
            id: "caff-latte",
            name: "Caff\u00e8 Latte",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 77.0,
            defaultServingMilliliters: 250.0,
            aliases: ["latte", "caffe latte", "cafe latte"],
        ),
        CaffeineCatalogItem(
            id: "monster-energy-zero-sugar",
            name: "Monster Energy Zero Sugar",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 160.0,
            defaultServingMilliliters: 473.0,
        ),
        CaffeineCatalogItem(
            id: "coca-cola",
            name: "Coca-Cola",
            category: CaffeineSourceCategory.soda,
            typicalCaffeineMg: 32.0,
            defaultServingMilliliters: 333.0,
            aliases: ["coke", "classic coke"],
        ),
        CaffeineCatalogItem(
            id: "tea-green",
            name: "Tea (Green)",
            category: CaffeineSourceCategory.tea,
            typicalCaffeineMg: 36.0,
            defaultServingMilliliters: 240.0,
            aliases: ["green tea"],
        ),
        CaffeineCatalogItem(
            id: "green-tea",
            name: "Green Tea",
            category: CaffeineSourceCategory.tea,
            typicalCaffeineMg: 34.0,
            defaultServingMilliliters: 200.0,
            aliases: ["tea green"],
        ),
        CaffeineCatalogItem(
            id: "dr-pepper",
            name: "Dr. Pepper",
            category: CaffeineSourceCategory.soda,
            typicalCaffeineMg: 41.9965,
            defaultServingMilliliters: 355.0,
            aliases: ["dr pepper"],
        ),
        CaffeineCatalogItem(
            id: "diet-coke",
            name: "Diet Coke",
            category: CaffeineSourceCategory.soda,
            typicalCaffeineMg: 46.01,
            defaultServingMilliliters: 355.0,
            aliases: ["coke diet"],
        ),
        CaffeineCatalogItem(
            id: "red-bull-zero",
            name: "Red Bull Zero",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 80.0,
            defaultServingMilliliters: 250.0,
        ),
        CaffeineCatalogItem(
            id: "starbucks-caffe-latte",
            name: "Starbucks Caffe Latte",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 66.0,
            defaultServingMilliliters: 354.0,
        ),
        CaffeineCatalogItem(
            id: "monster-energy",
            name: "Monster Energy",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 163.0,
            defaultServingMilliliters: 473.0,
            aliases: ["monster"],
        ),
        CaffeineCatalogItem(
            id: "matcha-tea",
            name: "Matcha Tea",
            category: CaffeineSourceCategory.tea,
            typicalCaffeineMg: 64.0,
            defaultServingMilliliters: 240.0,
            aliases: ["matcha"],
        ),
        CaffeineCatalogItem(
            id: "red-bull-blue-edition",
            name: "Red Bull Blue Edition",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 80.0,
            defaultServingMilliliters: 250.0,
        ),
        CaffeineCatalogItem(
            id: "nescafe-gold",
            name: "Nescaf\u00e9 Gold",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 44.0,
            defaultServingMilliliters: 240.0,
        ),
        CaffeineCatalogItem(
            id: "pepsi-max",
            name: "Pepsi Max",
            category: CaffeineSourceCategory.soda,
            typicalCaffeineMg: 45.8305,
            defaultServingMilliliters: 355.0,
        ),
        CaffeineCatalogItem(
            id: "celsius-sparkling-orange",
            name: "Celsius Sparkling Orange",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 200.0,
            defaultServingMilliliters: 355.0,
        ),
        CaffeineCatalogItem(
            id: "monster-energy-ultra-sunrise",
            name: "Monster Energy Ultra Sunrise",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 155.0,
            defaultServingMilliliters: 473.0,
        ),
        CaffeineCatalogItem(
            id: "nescafe-dolce-gusto-espresso-intenso",
            name: "Nescaf\u00e9 Dolce Gusto - Espresso Intenso",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 115.0,
            defaultServingMilliliters: 240.0,
        ),
        CaffeineCatalogItem(
            id: "costa-latte",
            name: "Costa Latte",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 100.0,
            defaultServingMilliliters: 354.0,
        ),
        CaffeineCatalogItem(
            id: "mountain-dew",
            name: "Mountain Dew",
            category: CaffeineSourceCategory.soda,
            typicalCaffeineMg: 53.99,
            defaultServingMilliliters: 355.0,
            aliases: ["mtn dew"],
        ),
        CaffeineCatalogItem(
            id: "costa-cappuccino",
            name: "Costa Cappuccino",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 200.0,
            defaultServingMilliliters: 354.0,
        ),
        CaffeineCatalogItem(
            id: "mccafe-latte",
            name: "McCaf\u00e9 Latte",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 71.0,
            defaultServingMilliliters: 354.0,
        ),
        CaffeineCatalogItem(
            id: "fuze-tea",
            name: "Fuze Tea",
            category: CaffeineSourceCategory.tea,
            typicalCaffeineMg: 11.999,
            defaultServingMilliliters: 355.0,
        ),
        CaffeineCatalogItem(
            id: "k-cup",
            name: "K-Cup",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 80.0,
            defaultServingMilliliters: 240.0,
        ),
        CaffeineCatalogItem(
            id: "c4-energy-drink",
            name: "C4 Energy drink",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 200.0,
            defaultServingMilliliters: 473.0,
        ),
        CaffeineCatalogItem(
            id: "monster-energy-ultra-peachy-keen",
            name: "Monster Energy Ultra Peachy Keen",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 150.0,
            defaultServingMilliliters: 473.0,
        ),
        CaffeineCatalogItem(
            id: "red-bull-sugarfree",
            name: "Red Bull Sugarfree",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 80.0,
            defaultServingMilliliters: 250.0,
        ),
        CaffeineCatalogItem(
            id: "caffe-nero-mocha-frappe-latte",
            name: "Caff\u00e9 Nero Mocha Frappe Latte",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 170.0,
            defaultServingMilliliters: 350.0,
        ),
        CaffeineCatalogItem(
            id: "dunkin-macchiato",
            name: "Dunkin' Macchiato",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 237.0,
            defaultServingMilliliters: 295.0,
        ),
        CaffeineCatalogItem(
            id: "monster-energy-mango-loco",
            name: "Monster Energy Mango Loco",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 150.0,
            defaultServingMilliliters: 473.0,
        ),
        CaffeineCatalogItem(
            id: "monster-energy-ultra-rosa",
            name: "Monster Energy Ultra Rosa",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 150.0,
            defaultServingMilliliters: 473.0,
        ),
        CaffeineCatalogItem(
            id: "starbucks-espresso",
            name: "Starbucks Espresso",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 33.0,
            defaultServingMilliliters: 33.0,
        ),
        CaffeineCatalogItem(
            id: "red-bull-coconut-edition",
            name: "Red Bull Coconut Edition",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 80.0,
            defaultServingMilliliters: 250.0,
        ),
        CaffeineCatalogItem(
            id: "cappuccino-double",
            name: "Cappuccino - Double",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 143.0,
            defaultServingMilliliters: 300.0,
        ),
        CaffeineCatalogItem(
            id: "bang-energy-drink",
            name: "Bang Energy Drink",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 300.0,
            defaultServingMilliliters: 500.0,
        ),
        CaffeineCatalogItem(
            id: "dunkin-iced-latte",
            name: "Dunkin' Iced Latte",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 166.0,
            defaultServingMilliliters: 709.0,
        ),
        CaffeineCatalogItem(
            id: "nescafe-dolce-gusto-espresso",
            name: "Nescaf\u00e9 Dolce Gusto - Espresso",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 80.0,
            defaultServingMilliliters: 240.0,
        ),
        CaffeineCatalogItem(
            id: "monster-energy-ultra-fiesta",
            name: "Monster Energy Ultra Fiesta",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 150.0,
            defaultServingMilliliters: 473.0,
        ),
        CaffeineCatalogItem(
            id: "dunkin-hot-chocolate",
            name: "Dunkin' Hot Chocolate",
            category: CaffeineSourceCategory.chocolate,
            typicalCaffeineMg: 13.0,
            defaultServingMilliliters: 414.0,
        ),
        CaffeineCatalogItem(
            id: "caffe-nero-americano",
            name: "Caff\u00e9 Nero Americano",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 160.0,
            defaultServingMilliliters: 350.0,
        ),
        CaffeineCatalogItem(
            id: "nescafe-dolce-gusto-cafe-au-lait",
            name: "Nescaf\u00e9 Dolce Gusto - Caf\u00e9 Au Lait",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 92.0,
            defaultServingMilliliters: 240.0,
        ),
        CaffeineCatalogItem(
            id: "pepsi-diet-cherry",
            name: "Pepsi Diet Cherry",
            category: CaffeineSourceCategory.soda,
            typicalCaffeineMg: 40.5,
            defaultServingMilliliters: 354.0,
        ),
        CaffeineCatalogItem(
            id: "tea-jasmine",
            name: "Tea (Jasmine)",
            category: CaffeineSourceCategory.tea,
            typicalCaffeineMg: 25.0,
            defaultServingMilliliters: 240.0,
        ),
        CaffeineCatalogItem(
            id: "faygo-cola",
            name: "Faygo Cola",
            category: CaffeineSourceCategory.soda,
            typicalCaffeineMg: 41.7,
            defaultServingMilliliters: 354.0,
        ),
        CaffeineCatalogItem(
            id: "red-bull-red-edition",
            name: "Red Bull Red Edition",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 80.0,
            defaultServingMilliliters: 250.0,
        ),
        CaffeineCatalogItem(
            id: "starbucks-cafe-mocha",
            name: "Starbucks Caf\u00e9 Mocha",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 66.0,
            defaultServingMilliliters: 354.0,
        ),
        CaffeineCatalogItem(
            id: "dunkin-iced-tea",
            name: "Dunkin' Iced Tea",
            category: CaffeineSourceCategory.tea,
            typicalCaffeineMg: 67.0,
            defaultServingMilliliters: 709.0,
        ),
        CaffeineCatalogItem(
            id: "hot-cocoa",
            name: "Hot Cocoa",
            category: CaffeineSourceCategory.chocolate,
            typicalCaffeineMg: 7.5,
            defaultServingMilliliters: 250.0,
            aliases: ["cocoa"],
        ),
        CaffeineCatalogItem(
            id: "monster-energy-pipeline-punch",
            name: "Monster Energy Pipeline Punch",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 160.0,
            defaultServingMilliliters: 473.0,
        ),
        CaffeineCatalogItem(
            id: "lipton-iced-tea",
            name: "Lipton Iced Tea",
            category: CaffeineSourceCategory.tea,
            typicalCaffeineMg: 15.02,
            defaultServingMilliliters: 355.0,
        ),
        CaffeineCatalogItem(
            id: "monster-energy-rehab",
            name: "Monster Energy Rehab",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 160.0,
            defaultServingMilliliters: 473.0,
        ),
        CaffeineCatalogItem(
            id: "red-bull-peach-edition",
            name: "Red Bull Peach Edition",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 80.0,
            defaultServingMilliliters: 250.0,
        ),
        CaffeineCatalogItem(
            id: "monster-energy-reserve",
            name: "Monster Energy Reserve",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 160.0,
            defaultServingMilliliters: 473.0,
        ),
        CaffeineCatalogItem(
            id: "red-bull-summer-edition",
            name: "Red Bull Summer Edition",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 80.0,
            defaultServingMilliliters: 250.0,
        ),
        CaffeineCatalogItem(
            id: "monster-energy-ultra-gold",
            name: "Monster Energy Ultra Gold",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 150.0,
            defaultServingMilliliters: 473.0,
        ),
        CaffeineCatalogItem(
            id: "monster-energy-juice",
            name: "Monster Energy Juice",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 160.0,
            defaultServingMilliliters: 473.0,
        ),
        CaffeineCatalogItem(
            id: "celsius-sparkling-grapefruit",
            name: "Celsius Sparkling Grapefruit",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 200.0,
            defaultServingMilliliters: 355.0,
        ),
        CaffeineCatalogItem(
            id: "dark-chocolate-70-85",
            name: "Dark Chocolate (70-85%)",
            category: CaffeineSourceCategory.chocolate,
            typicalCaffeineMg: 8.0,
            aliases: ["dark chocolate"],
        ),
        CaffeineCatalogItem(
            id: "mccafe-americano",
            name: "McCaf\u00e9 Americano",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 71.0,
            defaultServingMilliliters: 354.0,
        ),
        CaffeineCatalogItem(
            id: "starbucks-caramel-macchiato",
            name: "Starbucks Caramel Macchiato",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 66.0,
            defaultServingMilliliters: 354.0,
        ),
        CaffeineCatalogItem(
            id: "v8-energy-strawberry-banana",
            name: "V8+ Energy Strawberry Banana",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 80.0,
            defaultServingMilliliters: 237.0,
        ),
        CaffeineCatalogItem(
            id: "starbucks-cappuccino",
            name: "Starbucks Cappuccino",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 66.0,
            defaultServingMilliliters: 354.0,
        ),
        CaffeineCatalogItem(
            id: "hell-energy-drink",
            name: "Hell Energy Drink",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 80.0,
            defaultServingMilliliters: 250.0,
        ),
        CaffeineCatalogItem(
            id: "monster-energy-mega-drink",
            name: "Monster Energy Mega Drink",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 240.0,
            defaultServingMilliliters: 710.0,
        ),
        CaffeineCatalogItem(
            id: "monster-energy-ultra-blue",
            name: "Monster Energy Ultra Blue",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 150.0,
            defaultServingMilliliters: 473.0,
        ),
        CaffeineCatalogItem(
            id: "nescafe-dolce-gusto-cafe-au-lait-intenso",
            name: "Nescaf\u00e9 Dolce Gusto - Caf\u00e9 Au Lait Intenso",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 102.0,
            defaultServingMilliliters: 240.0,
        ),
        CaffeineCatalogItem(
            id: "nescafe-dolce-gusto-caffe-grande-intenso",
            name: "Nescaf\u00e9 Dolce Gusto - Caffe Grande Intenso",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 130.0,
            defaultServingMilliliters: 240.0,
        ),
        CaffeineCatalogItem(
            id: "nestea",
            name: "Nestea",
            category: CaffeineSourceCategory.tea,
            typicalCaffeineMg: 16.33,
            defaultServingMilliliters: 355.0,
        ),
        CaffeineCatalogItem(
            id: "pepsi-cherry",
            name: "Pepsi Cherry",
            category: CaffeineSourceCategory.soda,
            typicalCaffeineMg: 39.7,
            defaultServingMilliliters: 354.0,
        ),
        CaffeineCatalogItem(
            id: "red-bull-tropical-edition",
            name: "Red Bull Tropical Edition",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 80.0,
            defaultServingMilliliters: 250.0,
        ),
        CaffeineCatalogItem(
            id: "costa-americano",
            name: "Costa Americano",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 200.0,
            defaultServingMilliliters: 354.0,
        ),
        CaffeineCatalogItem(
            id: "monster-energy-assault",
            name: "Monster Energy Assault",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 160.0,
            defaultServingMilliliters: 473.0,
        ),
        CaffeineCatalogItem(
            id: "monster-energy-nitro",
            name: "Monster Energy Nitro",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 160.0,
            defaultServingMilliliters: 473.0,
        ),
        CaffeineCatalogItem(
            id: "monster-energy-ultra-violet",
            name: "Monster Energy Ultra Violet",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 140.0,
            defaultServingMilliliters: 473.0,
        ),
        CaffeineCatalogItem(
            id: "monster-energy-ultra-watermelon",
            name: "Monster Energy Ultra Watermelon",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 150.0,
            defaultServingMilliliters: 473.0,
        ),
        CaffeineCatalogItem(
            id: "starbucks-flat-white",
            name: "Starbucks Flat White",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 66.0,
            defaultServingMilliliters: 236.0,
        ),
        CaffeineCatalogItem(
            id: "monster-energy-import",
            name: "Monster Energy Import",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 179.0,
            defaultServingMilliliters: 550.0,
        ),
        CaffeineCatalogItem(
            id: "monster-energy-ultra-paradise",
            name: "Monster Energy Ultra Paradise",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 140.0,
            defaultServingMilliliters: 473.0,
        ),
        CaffeineCatalogItem(
            id: "tea-herbal",
            name: "Tea (Herbal)",
            category: CaffeineSourceCategory.tea,
            typicalCaffeineMg: 0.0,
            defaultServingMilliliters: 240.0,
        ),
        CaffeineCatalogItem(
            id: "v8-energy-sparkling-lemon-lime",
            name: "V8+ Energy Sparkling Lemon Lime",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 80.0,
            defaultServingMilliliters: 355.0,
        ),
        CaffeineCatalogItem(
            id: "burn",
            name: "Burn",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 113.6,
            defaultServingMilliliters: 355.0,
        ),
        CaffeineCatalogItem(
            id: "caffe-nero-espresso-macchiato",
            name: "Caff\u00e9 Nero Espresso Macchiato",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 80.0,
            defaultServingMilliliters: 350.0,
        ),
        CaffeineCatalogItem(
            id: "celsius-peach-mango-green-tea",
            name: "Celsius Peach Mango Green Tea",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 200.0,
            defaultServingMilliliters: 355.0,
        ),
        CaffeineCatalogItem(
            id: "coca-cola-cherry",
            name: "Coca-Cola Cherry",
            category: CaffeineSourceCategory.soda,
            typicalCaffeineMg: 34.4,
            defaultServingMilliliters: 354.0,
        ),
        CaffeineCatalogItem(
            id: "coca-cola-diet-cherry",
            name: "Coca-Cola Diet Cherry",
            category: CaffeineSourceCategory.soda,
            typicalCaffeineMg: 35.0,
            defaultServingMilliliters: 354.0,
        ),
        CaffeineCatalogItem(
            id: "costa-flat-white",
            name: "Costa Flat White",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 241.0,
            defaultServingMilliliters: 354.0,
        ),
        CaffeineCatalogItem(
            id: "coca-cola-light",
            name: "Coca-Cola Light",
            category: CaffeineSourceCategory.soda,
            typicalCaffeineMg: 46.01,
            defaultServingMilliliters: 355.0,
        ),
        CaffeineCatalogItem(
            id: "dunkin-latte",
            name: "Dunkin' Latte",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 166.0,
            defaultServingMilliliters: 414.0,
        ),
        CaffeineCatalogItem(
            id: "mccafe-mocha",
            name: "McCaf\u00e9 Mocha",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 86.0,
            defaultServingMilliliters: 354.0,
        ),
        CaffeineCatalogItem(
            id: "red-bull-green-edition",
            name: "Red Bull Green Edition",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 80.0,
            defaultServingMilliliters: 250.0,
        ),
        CaffeineCatalogItem(
            id: "28-black-energy-drink",
            name: "28 Black Energy Drink",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 125.0,
            defaultServingMilliliters: 250.0,
        ),
        CaffeineCatalogItem(
            id: "caffe-nero-caffe-latte",
            name: "Caff\u00e9 Nero Caff\u00e9 Latte",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 160.0,
            defaultServingMilliliters: 350.0,
        ),
        CaffeineCatalogItem(
            id: "carabao-energy-drink",
            name: "Carabao Energy Drink",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 106.0,
            defaultServingMilliliters: 330.0,
        ),
        CaffeineCatalogItem(
            id: "dark-chocolate-60-69",
            name: "Dark Chocolate (60-69%)",
            category: CaffeineSourceCategory.chocolate,
            typicalCaffeineMg: 8.6,
        ),
        CaffeineCatalogItem(
            id: "dunkin-matcha-latte",
            name: "Dunkin' Matcha Latte",
            category: CaffeineSourceCategory.tea,
            typicalCaffeineMg: 90.0,
            defaultServingMilliliters: 414.0,
        ),
        CaffeineCatalogItem(
            id: "dunkin-vanilla-chai",
            name: "Dunkin' Vanilla Chai",
            category: CaffeineSourceCategory.tea,
            typicalCaffeineMg: 80.0,
            defaultServingMilliliters: 414.0,
        ),
        CaffeineCatalogItem(
            id: "monster-energy-java",
            name: "Monster Energy Java",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 200.0,
            defaultServingMilliliters: 444.0,
        ),
        CaffeineCatalogItem(
            id: "nescafe-3-in-1-instant-coffee",
            name: "Nescaf\u00e9 3 in 1 Instant Coffee",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 50.0,
            defaultServingMilliliters: 240.0,
        ),
        CaffeineCatalogItem(
            id: "nescafe-dolce-gusto-capuccino",
            name: "Nescaf\u00e9 Dolce Gusto - Capuccino",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 107.0,
            defaultServingMilliliters: 240.0,
        ),
        CaffeineCatalogItem(
            id: "v8-energy-diet-strawberry-lemonade",
            name: "V8+ Energy Diet Strawberry Lemonade",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 80.0,
            defaultServingMilliliters: 237.0,
        ),
        CaffeineCatalogItem(
            id: "caffe-nero-frappe-latte",
            name: "Caff\u00e9 Nero Frappe Latte",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 160.0,
            defaultServingMilliliters: 350.0,
        ),
        CaffeineCatalogItem(
            id: "caffe-nero-iced-latte",
            name: "Caff\u00e9 Nero Iced Latte",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 160.0,
            defaultServingMilliliters: 350.0,
        ),
        CaffeineCatalogItem(
            id: "caffe-nero-mocha",
            name: "Caff\u00e9 Nero Mocha",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 170.0,
            defaultServingMilliliters: 350.0,
        ),
        CaffeineCatalogItem(
            id: "dark-chocolate-45-59",
            name: "Dark Chocolate (45-59%)",
            category: CaffeineSourceCategory.chocolate,
            typicalCaffeineMg: 4.3,
        ),
        CaffeineCatalogItem(
            id: "mccafe-caramel-macchiato",
            name: "McCaf\u00e9 Caramel Macchiato",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 71.0,
            defaultServingMilliliters: 354.0,
        ),
        CaffeineCatalogItem(
            id: "prime-energy-drink",
            name: "Prime Energy Drink",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 200.0,
            defaultServingMilliliters: 473.0,
        ),
        CaffeineCatalogItem(
            id: "starbucks-white-mocha",
            name: "Starbucks White Mocha",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 66.0,
            defaultServingMilliliters: 354.0,
        ),
        CaffeineCatalogItem(
            id: "swiss-miss-hot-chocolate",
            name: "Swiss Miss Hot Chocolate",
            category: CaffeineSourceCategory.chocolate,
            typicalCaffeineMg: 6.0,
            defaultServingMilliliters: 240.0,
        ),
        CaffeineCatalogItem(
            id: "v8-energy-peach-mango",
            name: "V8+ Energy Peach Mango",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 80.0,
            defaultServingMilliliters: 237.0,
        ),
        CaffeineCatalogItem(
            id: "caffe-nero-espresso",
            name: "Caff\u00e9 Nero Espresso",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 80.0,
            defaultServingMilliliters: 350.0,
        ),
        CaffeineCatalogItem(
            id: "dunkin-iced-macchiato",
            name: "Dunkin' Iced Macchiato",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 284.0,
            defaultServingMilliliters: 709.0,
        ),
        CaffeineCatalogItem(
            id: "nescafe-dolce-gusto-latte-macchiato",
            name: "Nescaf\u00e9 Dolce Gusto - Latte Macchiato",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 85.0,
            defaultServingMilliliters: 240.0,
        ),
        CaffeineCatalogItem(
            id: "shasta-cola",
            name: "Shasta Cola",
            category: CaffeineSourceCategory.soda,
            typicalCaffeineMg: 42.9,
            defaultServingMilliliters: 354.0,
        ),
        CaffeineCatalogItem(
            id: "v8-energy-orange-pineapple",
            name: "V8+ Energy Orange Pineapple",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 80.0,
            defaultServingMilliliters: 237.0,
        ),
        CaffeineCatalogItem(
            id: "3d-energy-drink",
            name: "3D Energy",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 200.0,
            defaultServingMilliliters: 473.0,
        ),
        CaffeineCatalogItem(
            id: "celsius-sparkling-watermelon",
            name: "Celsius Sparkling Watermelon",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 200.0,
            defaultServingMilliliters: 355.0,
        ),
        CaffeineCatalogItem(
            id: "dunkin-espresso",
            name: "Dunkin' Espresso",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 118.0,
            defaultServingMilliliters: 295.0,
        ),
        CaffeineCatalogItem(
            id: "monster-energy-java-300",
            name: "Monster Energy Java 300",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 300.0,
            defaultServingMilliliters: 444.0,
        ),
        CaffeineCatalogItem(
            id: "monster-energy-ultra-black",
            name: "Monster Energy Ultra Black",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 150.0,
            defaultServingMilliliters: 473.0,
        ),
        CaffeineCatalogItem(
            id: "monster-energy-ultra-red",
            name: "Monster Energy Ultra Red",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 140.0,
            defaultServingMilliliters: 473.0,
        ),
        CaffeineCatalogItem(
            id: "mountain-dew-code-red",
            name: "Mountain Dew Code Red",
            category: CaffeineSourceCategory.soda,
            typicalCaffeineMg: 54.03,
            defaultServingMilliliters: 355.0,
        ),
        CaffeineCatalogItem(
            id: "nescafe-dolce-gusto-lungo-intenso",
            name: "Nescaf\u00e9 Dolce Gusto - Lungo Intenso",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 147.0,
            defaultServingMilliliters: 240.0,
        ),
        CaffeineCatalogItem(
            id: "v8-energy-tropical-green",
            name: "V8+ Energy Tropical Green",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 80.0,
            defaultServingMilliliters: 237.0,
        ),
        CaffeineCatalogItem(
            id: "big-shock",
            name: "Big Shock!",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 114.6,
            defaultServingMilliliters: 355.0,
        ),
        CaffeineCatalogItem(
            id: "blue-tokai-attikan-estate",
            name: "Blue Tokai Attikan Estate",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 60.0,
            defaultServingMilliliters: 35.0,
        ),
        CaffeineCatalogItem(
            id: "blue-tokai-dhak-blend",
            name: "Blue Tokai Dhak Blend",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 60.0,
            defaultServingMilliliters: 35.0,
        ),
        CaffeineCatalogItem(
            id: "blue-tokai-silver-oak-blend",
            name: "Blue Tokai Silver Oak Blend",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 60.0,
            defaultServingMilliliters: 35.0,
        ),
        CaffeineCatalogItem(
            id: "blue-tokai-vienna-roast",
            name: "Blue Tokai Vienna Roast",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 60.0,
            defaultServingMilliliters: 35.0,
        ),
        CaffeineCatalogItem(
            id: "caffeine-powder",
            name: "Caffeine powder",
            category: CaffeineSourceCategory.supplement,
            typicalCaffeineMg: 1000.0,
            aliases: ["caffeine powder"],
        ),
        CaffeineCatalogItem(
            id: "celsius-inferno-punch",
            name: "Celsius Inferno Punch",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 300.0,
            defaultServingMilliliters: 473.0,
        ),
        CaffeineCatalogItem(
            id: "celsius-tart-cherry-lime",
            name: "Celsius Tart Cherry Lime",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 100.0,
            defaultServingMilliliters: 355.0,
        ),
        CaffeineCatalogItem(
            id: "dunkin-americano",
            name: "Dunkin' Americano",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 237.0,
            defaultServingMilliliters: 295.0,
        ),
        CaffeineCatalogItem(
            id: "dunkin-hot-tea",
            name: "Dunkin' Hot Tea",
            category: CaffeineSourceCategory.tea,
            typicalCaffeineMg: 90.0,
            defaultServingMilliliters: 414.0,
        ),
        CaffeineCatalogItem(
            id: "excedrin",
            name: "Excedrin",
            category: CaffeineSourceCategory.supplement,
            typicalCaffeineMg: 65.0,
        ),
        CaffeineCatalogItem(
            id: "irish-tea",
            name: "Irish Tea",
            category: CaffeineSourceCategory.tea,
            typicalCaffeineMg: 50.0,
            defaultServingMilliliters: 240.0,
        ),
        CaffeineCatalogItem(
            id: "kopi-peng",
            name: "Kopi Peng",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 95.0,
            defaultServingMilliliters: 240.0,
        ),
        CaffeineCatalogItem(
            id: "mccafe-cappuccino",
            name: "McCaf\u00e9 Cappuccino",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 71.0,
            defaultServingMilliliters: 354.0,
        ),
        CaffeineCatalogItem(
            id: "mccafe-caramel-cappuccino",
            name: "McCaf\u00e9 Caramel Cappuccino",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 71.0,
            defaultServingMilliliters: 354.0,
        ),
        CaffeineCatalogItem(
            id: "merrild-red",
            name: "Merrild Red",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 96.0,
            defaultServingMilliliters: 240.0,
        ),
        CaffeineCatalogItem(
            id: "merrild-special",
            name: "Merrild Special",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 95.0,
            defaultServingMilliliters: 240.0,
        ),
        CaffeineCatalogItem(
            id: "nescafe-dolce-gusto-capuccino-skinny",
            name: "Nescaf\u00e9 Dolce Gusto - Capuccino Skinny",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 90.0,
            defaultServingMilliliters: 240.0,
        ),
        CaffeineCatalogItem(
            id: "nescafe-dolce-gusto-lungo",
            name: "Nescaf\u00e9 Dolce Gusto - Lungo",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 89.0,
            defaultServingMilliliters: 240.0,
        ),
        CaffeineCatalogItem(
            id: "nespresso-classic-pod-110-ml",
            name: "Nespresso Classic Pod (110 ml)",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 95.0,
            defaultServingMilliliters: 240.0,
        ),
        CaffeineCatalogItem(
            id: "nespresso-classic-pod-25-ml",
            name: "Nespresso Classic Pod (25 ml)",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 60.0,
            defaultServingMilliliters: 25.0,
        ),
        CaffeineCatalogItem(
            id: "nespresso-classic-pod-40-ml",
            name: "Nespresso Classic Pod (40 ml)",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 80.0,
            defaultServingMilliliters: 40.0,
        ),
        CaffeineCatalogItem(
            id: "nocco-bcaa",
            name: "Nocco BCAA",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 180.0,
            defaultServingMilliliters: 330.0,
        ),
        CaffeineCatalogItem(
            id: "nocco-focus",
            name: "Nocco Focus",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 180.0,
            defaultServingMilliliters: 330.0,
        ),
        CaffeineCatalogItem(
            id: "paulaner-spezi",
            name: "Paulaner Spezi",
            category: CaffeineSourceCategory.soda,
            typicalCaffeineMg: 33.0,
            defaultServingMilliliters: 500.0,
        ),
        CaffeineCatalogItem(
            id: "peter-larsen-kaffe-fairtrade",
            name: "Peter Larsen Kaffe Fairtrade",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 95.0,
            defaultServingMilliliters: 240.0,
        ),
        CaffeineCatalogItem(
            id: "peter-larsen-kaffe-original-72",
            name: "Peter Larsen Kaffe Original 72",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 95.0,
            defaultServingMilliliters: 240.0,
        ),
        CaffeineCatalogItem(
            id: "red-bull-winter-edition",
            name: "Red Bull Winter Edition",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 80.0,
            defaultServingMilliliters: 250.0,
        ),
        CaffeineCatalogItem(
            id: "reign",
            name: "Reign (US)",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 300.0,
            defaultServingMilliliters: 473.0,
        ),
        CaffeineCatalogItem(
            id: "reign-uk",
            name: "Reign (UK)",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 200.0,
            defaultServingMilliliters: 500.0,
        ),
        CaffeineCatalogItem(
            id: "starbucks-iced-caff-latte",
            name: "Starbucks Iced Caff\u00e8 Latte",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 75.0,
            defaultServingMilliliters: 355.0,
        ),
        CaffeineCatalogItem(
            id: "turkish-coffee",
            name: "Turkish Coffee",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 50.7,
            defaultServingMilliliters: 60.0,
        ),
        CaffeineCatalogItem(
            id: "vivarin-caffeine-pill",
            name: "Vivarin",
            category: CaffeineSourceCategory.supplement,
            typicalCaffeineMg: 200.0,
        ),
        CaffeineCatalogItem(
            id: "yerba-mat-tea",
            name: "Yerba Mat\u00e9 (Tea)",
            category: CaffeineSourceCategory.tea,
            typicalCaffeineMg: 40.0,
            defaultServingMilliliters: 240.0,
        ),
        CaffeineCatalogItem(
            id: "5-hour-energy-extra-strength",
            name: "5 Hour Energy Extra Strength",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 230.0,
            defaultServingMilliliters: 57.0,
        ),
        CaffeineCatalogItem(
            id: "atkins-iced-coffee-protein-shake",
            name: "Atkins Iced Coffee Protein Shake",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 70.0,
            defaultServingMilliliters: 240.0,
        ),
        CaffeineCatalogItem(
            id: "caffe-nero-cappuccino",
            name: "Caff\u00e9 Nero Cappuccino",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 160.0,
            defaultServingMilliliters: 350.0,
        ),
        CaffeineCatalogItem(
            id: "caffe-nero-espresso-con-panna",
            name: "Caff\u00e9 Nero Espresso Con Panna",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 80.0,
            defaultServingMilliliters: 350.0,
        ),
        CaffeineCatalogItem(
            id: "celsius-blood-orange-lemonade",
            name: "Celsius Blood Orange Lemonade",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 100.0,
            defaultServingMilliliters: 355.0,
        ),
        CaffeineCatalogItem(
            id: "celsius-jackfruit",
            name: "Celsius Jackfruit",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 300.0,
            defaultServingMilliliters: 473.0,
        ),
        CaffeineCatalogItem(
            id: "cheerwine",
            name: "Cheerwine",
            category: CaffeineSourceCategory.soda,
            typicalCaffeineMg: 47.5,
            defaultServingMilliliters: 354.0,
        ),
        CaffeineCatalogItem(
            id: "cheerwine-diet",
            name: "Cheerwine Diet",
            category: CaffeineSourceCategory.soda,
            typicalCaffeineMg: 48.1,
            defaultServingMilliliters: 354.0,
        ),
        CaffeineCatalogItem(
            id: "coca-cola-diet-cherry-1",
            name: "Coca-Cola Diet Cherry",
            category: CaffeineSourceCategory.soda,
            typicalCaffeineMg: 35.0,
            defaultServingMilliliters: 354.0,
        ),
        CaffeineCatalogItem(
            id: "crystal-light",
            name: "Crystal Light",
            category: CaffeineSourceCategory.soda,
            typicalCaffeineMg: 30.0,
            defaultServingMilliliters: 240.0,
        ),
        CaffeineCatalogItem(
            id: "dunkin-cappuccino",
            name: "Dunkin' Cappuccino",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 166.0,
            defaultServingMilliliters: 414.0,
        ),
        CaffeineCatalogItem(
            id: "dunkin-dunkaccino",
            name: "Dunkin' Dunkaccino",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 85.0,
            defaultServingMilliliters: 414.0,
        ),
        CaffeineCatalogItem(
            id: "eternal-energy",
            name: "Eternal Energy",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 190.0,
            defaultServingMilliliters: 59.0,
        ),
        CaffeineCatalogItem(
            id: "fast-twitch",
            name: "Fast Twitch",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 200.0,
            defaultServingMilliliters: 355.0,
        ),
        CaffeineCatalogItem(
            id: "full-throttle",
            name: "Full Throttle",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 160.0,
            defaultServingMilliliters: 473.0,
        ),
        CaffeineCatalogItem(
            id: "gamersups",
            name: "Gamersups",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 100.0,
            defaultServingMilliliters: 240.0,
        ),
        CaffeineCatalogItem(
            id: "generic-caffeine-pill",
            name: "Generic Caffeine Pill",
            category: CaffeineSourceCategory.supplement,
            typicalCaffeineMg: 200.0,
            aliases: ["caffeine pill", "caffeine tablet"],
        ),
        CaffeineCatalogItem(
            id: "ghost",
            name: "Ghost",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 200.0,
            defaultServingMilliliters: 473.0,
        ),
        CaffeineCatalogItem(
            id: "gorgie",
            name: "Gorgie",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 120.0,
            defaultServingMilliliters: 355.0,
        ),
        CaffeineCatalogItem(
            id: "gorilla-mind-energy-drink",
            name: "Gorilla Mind Energy Drink",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 200.0,
            defaultServingMilliliters: 355.0,
        ),
        CaffeineCatalogItem(
            id: "gorilla-mode-preworkout",
            name: "Gorilla Mode Pre-workout",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 350.0,
            defaultServingMilliliters: 240.0,
        ),
        CaffeineCatalogItem(
            id: "guayak-yerba-mate",
            name: "Guayak\u00ed Yerba Mate",
            category: CaffeineSourceCategory.tea,
            typicalCaffeineMg: 150.0,
            defaultServingMilliliters: 458.0,
        ),
        CaffeineCatalogItem(
            id: "hell-ice-coffee",
            name: "Hell Ice Coffee",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 38.0,
            defaultServingMilliliters: 240.0,
        ),
        CaffeineCatalogItem(
            id: "jet-alert-caffeine-pill",
            name: "Jet Alert Caffeine Pill",
            category: CaffeineSourceCategory.supplement,
            typicalCaffeineMg: 100.0,
        ),
        CaffeineCatalogItem(
            id: "jocko-go",
            name: "Jocko GO",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 95.0,
            defaultServingMilliliters: 355.0,
        ),
        CaffeineCatalogItem(
            id: "kirkland-breakfast-blend",
            name: "Kirkland Breakfast Blend",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 98.0,
            defaultServingMilliliters: 240.0,
        ),
        CaffeineCatalogItem(
            id: "kofola",
            name: "Kofola",
            category: CaffeineSourceCategory.soda,
            typicalCaffeineMg: 53.25,
            defaultServingMilliliters: 355.0,
        ),
        CaffeineCatalogItem(
            id: "lucky-energy",
            name: "Lucky Energy",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 80.0,
            defaultServingMilliliters: 250.0,
        ),
        CaffeineCatalogItem(
            id: "madmonq",
            name: "MADMONQ",
            category: CaffeineSourceCategory.supplement,
            typicalCaffeineMg: 155.0,
        ),
        CaffeineCatalogItem(
            id: "mio-energy",
            name: "Mio Energy",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 60.0,
            defaultServingMilliliters: 240.0,
        ),
        CaffeineCatalogItem(
            id: "monster-energy-mule",
            name: "Monster Energy Mule",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 160.0,
            defaultServingMilliliters: 473.0,
        ),
        CaffeineCatalogItem(
            id: "mountain-dew-kickstart",
            name: "Mountain Dew Kickstart",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 90.0,
            defaultServingMilliliters: 473.0,
        ),
        CaffeineCatalogItem(
            id: "nescafe-ice-java",
            name: "Nescaf\u00e9 Ice Java",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 60.0,
            defaultServingMilliliters: 15.0,
        ),
        CaffeineCatalogItem(
            id: "nespresso-vertuo-pod-150-ml",
            name: "Nespresso Vertuo Pod (150 ml)",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 150.0,
            defaultServingMilliliters: 150.0,
        ),
        CaffeineCatalogItem(
            id: "nespresso-vertuo-pod-230-ml",
            name: "Nespresso Vertuo Pod (230 ml)",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 177.0,
            defaultServingMilliliters: 240.0,
        ),
        CaffeineCatalogItem(
            id: "nespresso-vertuo-pod-230-ml-20-caffeine",
            name: "Nespresso Vertuo Pod (230 ml, +20% Caffeine)",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 200.0,
            defaultServingMilliliters: 230.0,
        ),
        CaffeineCatalogItem(
            id: "nespresso-vertuo-pod-25-ml",
            name: "Nespresso Vertuo Pod (25 ml)",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 60.0,
            defaultServingMilliliters: 25.0,
        ),
        CaffeineCatalogItem(
            id: "nespresso-vertuo-pod-40-ml",
            name: "Nespresso Vertuo Pod (40 ml)",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 60.0,
            defaultServingMilliliters: 40.0,
        ),
        CaffeineCatalogItem(
            id: "nodoz-caffeine-pill",
            name: "NoDoz Caffeine Pill",
            category: CaffeineSourceCategory.supplement,
            typicalCaffeineMg: 200.0,
        ),
        CaffeineCatalogItem(
            id: "oca-energy-drink",
            name: "Oca",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 120.0,
            defaultServingMilliliters: 355.0,
        ),
        CaffeineCatalogItem(
            id: "old-town-white-coffee",
            name: "Old Town White coffee",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 60.0,
            defaultServingMilliliters: 240.0,
        ),
        CaffeineCatalogItem(
            id: "rip-it-citrus-x",
            name: "Rip It Citrus X",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 160.0,
            defaultServingMilliliters: 473.0,
        ),
        CaffeineCatalogItem(
            id: "semtex",
            name: "Semtex",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 113.6,
            defaultServingMilliliters: 355.0,
        ),
        CaffeineCatalogItem(
            id: "solo-energy-drink",
            name: "Solo Energy Drink",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 32.0,
            defaultServingMilliliters: 100.0,
        ),
        CaffeineCatalogItem(
            id: "suntory-boss-iced-long-black",
            name: "Suntory Boss Iced Long Black",
            category: CaffeineSourceCategory.coffee,
            typicalCaffeineMg: 140.0,
            defaultServingMilliliters: 237.0,
        ),
        CaffeineCatalogItem(
            id: "true-north-home",
            name: "True North Home",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 120.0,
            defaultServingMilliliters: 355.0,
        ),
        CaffeineCatalogItem(
            id: "twist-energy-drink",
            name: "Twist Energy Drink",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 80.0,
            defaultServingMilliliters: 250.0,
        ),
        CaffeineCatalogItem(
            id: "v8-energy-black-cherry",
            name: "V8+ Energy Black Cherry",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 80.0,
            defaultServingMilliliters: 237.0,
        ),
        CaffeineCatalogItem(
            id: "v8-energy-diet-cranberry-raspberry",
            name: "V8+ Energy Diet Cranberry Raspberry",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 80.0,
            defaultServingMilliliters: 237.0,
        ),
        CaffeineCatalogItem(
            id: "v8-energy-honeycrisp-apple-berry",
            name: "V8+ Energy Honeycrisp Apple Berry",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 80.0,
            defaultServingMilliliters: 237.0,
        ),
        CaffeineCatalogItem(
            id: "v8-energy-pomegranate-blueberry",
            name: "V8+ Energy Pomegranate Blueberry",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 80.0,
            defaultServingMilliliters: 237.0,
        ),
        CaffeineCatalogItem(
            id: "v8-energy-sparkling-black-cherry",
            name: "V8+ Energy Sparkling Black Cherry",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 80.0,
            defaultServingMilliliters: 355.0,
        ),
        CaffeineCatalogItem(
            id: "v8-energy-sparkling-orange-pineapple",
            name: "V8+ Energy Sparkling Orange Pineapple",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 80.0,
            defaultServingMilliliters: 355.0,
        ),
        CaffeineCatalogItem(
            id: "v8-energy-sparkling-strawberry-kiwi",
            name: "V8+ Energy Sparkling Strawberry Kiwi",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 80.0,
            defaultServingMilliliters: 355.0,
        ),
        CaffeineCatalogItem(
            id: "wooooo-energy-drink",
            name: "Wooooo! Energy Drink",
            category: CaffeineSourceCategory.energyDrink,
            typicalCaffeineMg: 200.0,
            defaultServingMilliliters: 473.0,
        ),
  ];

  static final List<_NormalizedItem> _normalizedItems = items.map((item) {
    final seen = <String>{};
    final aliases = <String>[];
    for (final raw in <String>[item.name, item.id.replaceAll('-', ' '), ...item.aliases]) {
      final normalized = _normalize(raw);
      if (normalized.isNotEmpty && seen.add(normalized)) {
        aliases.add(normalized);
      }
    }
    _mergeSortByDescendingLength(aliases);
    return _NormalizedItem(item: item, aliases: aliases);
  }).toList();

  static final Map<String, CaffeineCatalogItem> _itemsById = <String, CaffeineCatalogItem>{
    for (final item in items) item.id: item,
  };

  static List<CustomHydrationDrink> beveragePresets() => items
      .where((item) => item.defaultServingMilliliters != null)
      .where((item) => item.category != CaffeineSourceCategory.supplement)
      .map(
        (item) => CustomHydrationDrink(
          id: '$_beveragePresetIdPrefix${item.id}',
          name: item.name,
          volumeMilliliters: item.defaultServingMilliliters ?? 240.0,
          hydrationMultiplier: 1.0,
          nutrientValues: BeverageNutritionDefaults.nutrientValuesFor(item),
          category: item.category,
          isPreloaded: true,
        ),
      )
      .toList();

  static CaffeineCatalogItem? beveragePresetItem(String presetId) {
    if (!presetId.startsWith(_beveragePresetIdPrefix)) return null;
    return _itemsById[presetId.substring(_beveragePresetIdPrefix.length)];
  }

  static CaffeineCatalogMatch? match(CaffeineEntry entry) => matchName(entry.name);

  static CaffeineCatalogMatch? matchName(String? name) {
    final normalized = _normalize(name ?? '');
    if (normalized.isEmpty) return null;

    for (final candidate in _normalizedItems) {
      String? exact;
      for (final alias in candidate.aliases) {
        if (alias == normalized) {
          exact = alias;
          break;
        }
      }
      if (exact != null) {
        return CaffeineCatalogMatch(
          item: candidate.item,
          confidence: CaffeineCatalogMatchConfidence.exact,
          matchedText: exact,
        );
      }
    }

    _NormalizedItem? bestCandidate;
    String? bestAlias;
    for (final candidate in _normalizedItems) {
      for (final alias in candidate.aliases) {
        if (alias.length >= 3 && _phraseContains(normalized, alias)) {
          if (bestAlias == null || alias.length > bestAlias.length) {
            bestAlias = alias;
            bestCandidate = candidate;
          }
        }
      }
    }
    if (bestCandidate != null && bestAlias != null) {
      final originalAliases =
          bestCandidate.item.aliases.map(_normalize).toList();
      return CaffeineCatalogMatch(
        item: bestCandidate.item,
        confidence: originalAliases.contains(bestAlias)
            ? CaffeineCatalogMatchConfidence.alias
            : CaffeineCatalogMatchConfidence.contains,
        matchedText: bestAlias,
      );
    }

    return null;
  }

  static CaffeineSourceCategory categoryFor(CaffeineEntry entry) =>
      match(entry)?.item.category ?? inferGenericCategory(entry.name);

  static CaffeineSourceCategory inferGenericCategory(String? name) {
    final normalized = _normalize(name ?? '');
    if (_containsAny(normalized, [
      'espresso',
      'coffee',
      'latte',
      'cappuccino',
      'americano',
      'cold brew',
      'mocha',
      'macchiato',
      'frappe',
      'nespresso',
    ])) {
      return CaffeineSourceCategory.coffee;
    }
    if (_containsAny(normalized, ['tea', 'matcha', 'chai', 'yerba', 'mate'])) {
      return CaffeineSourceCategory.tea;
    }
    if (_containsAny(normalized, [
      'energy',
      'red bull',
      'redbull',
      'monster',
      'celsius',
      'rockstar',
      'bang',
      'c4',
      'pre workout',
      'preworkout',
    ])) {
      return CaffeineSourceCategory.energyDrink;
    }
    if (_containsAny(normalized, [
      'cola',
      'soda',
      'pepsi',
      'coke',
      'dr pepper',
      'mountain dew',
      'mtn dew',
    ])) {
      return CaffeineSourceCategory.soda;
    }
    if (_containsAny(normalized, ['chocolate', 'cocoa', 'cacao'])) {
      return CaffeineSourceCategory.chocolate;
    }
    if (_containsAny(normalized, [
      'pill',
      'tablet',
      'capsule',
      'powder',
      'supplement',
      'excedrin',
      'nodoz',
    ])) {
      return CaffeineSourceCategory.supplement;
    }
    return CaffeineSourceCategory.other;
  }

  static bool _phraseContains(String value, String phrase) =>
      ' $value '.contains(' $phrase ');

  static bool _containsAny(String value, List<String> needles) =>
      needles.any((needle) => _phraseContains(value, _normalize(needle)));

  static String _normalize(String value) {
    final stripped = _stripDiacritics(value.toLowerCase());
    return stripped.replaceAll(RegExp(r'[^a-z0-9]+'), ' ').trim();
  }

  /// Equivalent of Kotlin's `Normalizer.normalize(..., NFD)` followed by
  /// stripping combining marks: removes Latin diacritics down to the base
  /// letter so accented catalog names (Nescafé, Caffè, Guayakí)
  /// match plain-ASCII input.
  static String _stripDiacritics(String value) {
    final buffer = StringBuffer();
    for (final rune in value.runes) {
      buffer.write(_diacriticMap[rune] ?? String.fromCharCode(rune));
    }
    return buffer.toString();
  }

  static const Map<int, String> _diacriticMap = <int, String>{
    0x00E0: 'a', 0x00E1: 'a', 0x00E2: 'a', 0x00E3: 'a', 0x00E4: 'a',
    0x00E5: 'a', 0x0101: 'a', 0x0103: 'a', 0x0105: 'a',
    0x00E7: 'c', 0x0107: 'c', 0x0109: 'c', 0x010B: 'c', 0x010D: 'c',
    0x00E8: 'e', 0x00E9: 'e', 0x00EA: 'e', 0x00EB: 'e', 0x0113: 'e',
    0x0115: 'e', 0x0117: 'e', 0x0119: 'e', 0x011B: 'e',
    0x011F: 'g', 0x0121: 'g', 0x0123: 'g',
    0x00EC: 'i', 0x00ED: 'i', 0x00EE: 'i', 0x00EF: 'i', 0x0129: 'i',
    0x012B: 'i', 0x012D: 'i', 0x012F: 'i', 0x0131: 'i',
    0x0144: 'n', 0x0146: 'n', 0x0148: 'n', 0x00F1: 'n',
    0x00F2: 'o', 0x00F3: 'o', 0x00F4: 'o', 0x00F5: 'o', 0x00F6: 'o',
    0x00F8: 'o', 0x014D: 'o', 0x014F: 'o', 0x0151: 'o',
    0x015B: 's', 0x015D: 's', 0x015F: 's', 0x0161: 's',
    0x0163: 't', 0x0165: 't',
    0x00F9: 'u', 0x00FA: 'u', 0x00FB: 'u', 0x00FC: 'u', 0x0169: 'u',
    0x016B: 'u', 0x016D: 'u', 0x016F: 'u', 0x0171: 'u', 0x0173: 'u',
    0x00FD: 'y', 0x00FF: 'y',
    0x017A: 'z', 0x017C: 'z', 0x017E: 'z',
  };

  static void _mergeSortByDescendingLength(List<String> list) {
    if (list.length < 2) return;
    final buffer = List<String>.of(list);
    _mergeSort(list, buffer, 0, list.length);
  }

  // Stable merge sort by descending string length (mirrors Kotlin
  // `sortedByDescending(String::length)`).
  static void _mergeSort(
    List<String> list,
    List<String> buffer,
    int start,
    int end,
  ) {
    if (end - start < 2) return;
    final middle = start + (end - start) ~/ 2;
    _mergeSort(list, buffer, start, middle);
    _mergeSort(list, buffer, middle, end);
    var left = start;
    var right = middle;
    var index = start;
    while (left < middle && right < end) {
      if (list[left].length >= list[right].length) {
        buffer[index++] = list[left++];
      } else {
        buffer[index++] = list[right++];
      }
    }
    while (left < middle) {
      buffer[index++] = list[left++];
    }
    while (right < end) {
      buffer[index++] = list[right++];
    }
    for (var i = start; i < end; i++) {
      list[i] = buffer[i];
    }
  }
}

class _NormalizedItem {
  const _NormalizedItem({required this.item, required this.aliases});

  final CaffeineCatalogItem item;
  final List<String> aliases;
}

