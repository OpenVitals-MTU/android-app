package tech.mmarca.openvitals.features.imports.applehealth

import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Mass
import androidx.health.connect.client.units.kilocalories

internal class NutritionValues {
    var biotin: Mass? = null
    var caffeine: Mass? = null
    var calcium: Mass? = null
    var energy: Energy? = null
    var energyFromFat: Energy? = null
    var cholesterol: Mass? = null
    var chromium: Mass? = null
    var copper: Mass? = null
    var dietaryFiber: Mass? = null
    var folate: Mass? = null
    var iodine: Mass? = null
    var iron: Mass? = null
    var magnesium: Mass? = null
    var manganese: Mass? = null
    var molybdenum: Mass? = null
    var monounsaturatedFat: Mass? = null
    var niacin: Mass? = null
    var pantothenicAcid: Mass? = null
    var phosphorus: Mass? = null
    var polyunsaturatedFat: Mass? = null
    var potassium: Mass? = null
    var protein: Mass? = null
    var riboflavin: Mass? = null
    var saturatedFat: Mass? = null
    var selenium: Mass? = null
    var sodium: Mass? = null
    var sugar: Mass? = null
    var thiamin: Mass? = null
    var totalCarbohydrate: Mass? = null
    var totalFat: Mass? = null
    var transFat: Mass? = null
    var vitaminA: Mass? = null
    var vitaminB12: Mass? = null
    var vitaminB6: Mass? = null
    var vitaminC: Mass? = null
    var vitaminD: Mass? = null
    var vitaminE: Mass? = null
    var vitaminK: Mass? = null
    var zinc: Mass? = null
    var hasAny: Boolean = false

    fun apply(type: String, value: Double, unit: String?): Boolean {
        val applied = when (type) {
            AppleDietaryEnergyConsumed -> value.toKilocalories(unit)?.let { energy = it.kilocalories } != null
            AppleDietaryFatTotal -> value.toMass(unit)?.let { totalFat = it } != null
            AppleDietaryFatSaturated -> value.toMass(unit)?.let { saturatedFat = it } != null
            AppleDietaryFatTrans -> value.toMass(unit)?.let { transFat = it } != null
            AppleDietaryFatMonounsaturated -> value.toMass(unit)?.let { monounsaturatedFat = it } != null
            AppleDietaryFatPolyunsaturated -> value.toMass(unit)?.let { polyunsaturatedFat = it } != null
            AppleDietaryCholesterol -> value.toMass(unit)?.let { cholesterol = it } != null
            AppleDietarySodium -> value.toMass(unit)?.let { sodium = it } != null
            AppleDietaryCarbohydrates -> value.toMass(unit)?.let { totalCarbohydrate = it } != null
            AppleDietaryFiber -> value.toMass(unit)?.let { dietaryFiber = it } != null
            AppleDietarySugar -> value.toMass(unit)?.let { sugar = it } != null
            AppleDietaryProtein -> value.toMass(unit)?.let { protein = it } != null
            AppleDietaryCaffeine -> value.toMass(unit)?.let { caffeine = it } != null
            AppleDietaryCalcium -> value.toMass(unit)?.let { calcium = it } != null
            AppleDietaryIron -> value.toMass(unit)?.let { iron = it } != null
            AppleDietaryThiamin -> value.toMass(unit)?.let { thiamin = it } != null
            AppleDietaryRiboflavin -> value.toMass(unit)?.let { riboflavin = it } != null
            AppleDietaryNiacin -> value.toMass(unit)?.let { niacin = it } != null
            AppleDietaryFolate -> value.toMass(unit)?.let { folate = it } != null
            AppleDietaryBiotin -> value.toMass(unit)?.let { biotin = it } != null
            AppleDietaryPantothenicAcid -> value.toMass(unit)?.let { pantothenicAcid = it } != null
            AppleDietaryPhosphorus -> value.toMass(unit)?.let { phosphorus = it } != null
            AppleDietaryIodine -> value.toMass(unit)?.let { iodine = it } != null
            AppleDietaryMagnesium -> value.toMass(unit)?.let { magnesium = it } != null
            AppleDietaryZinc -> value.toMass(unit)?.let { zinc = it } != null
            AppleDietarySelenium -> value.toMass(unit)?.let { selenium = it } != null
            AppleDietaryCopper -> value.toMass(unit)?.let { copper = it } != null
            AppleDietaryManganese -> value.toMass(unit)?.let { manganese = it } != null
            AppleDietaryChromium -> value.toMass(unit)?.let { chromium = it } != null
            AppleDietaryMolybdenum -> value.toMass(unit)?.let { molybdenum = it } != null
            AppleDietaryPotassium -> value.toMass(unit)?.let { potassium = it } != null
            AppleDietaryVitaminA -> value.toMass(unit)?.let { vitaminA = it } != null
            AppleDietaryVitaminB6 -> value.toMass(unit)?.let { vitaminB6 = it } != null
            AppleDietaryVitaminB12 -> value.toMass(unit)?.let { vitaminB12 = it } != null
            AppleDietaryVitaminC -> value.toMass(unit)?.let { vitaminC = it } != null
            AppleDietaryVitaminD -> value.toMass(unit)?.let { vitaminD = it } != null
            AppleDietaryVitaminE -> value.toMass(unit)?.let { vitaminE = it } != null
            AppleDietaryVitaminK -> value.toMass(unit)?.let { vitaminK = it } != null
            else -> false
        }
        if (applied) hasAny = true
        return applied
    }
}
