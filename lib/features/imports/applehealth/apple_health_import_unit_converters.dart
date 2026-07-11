/// Unit conversions for the Apple Health importer, ported 1:1 from the Kotlin
/// `AppleHealthImportUnitConverters.kt`. These are the correctness core — the
/// conversion factors are the spec.
library;

double? toMeters(double value, String? unit) {
  switch (unit?.toLowerCase()) {
    case 'm':
    case 'meter':
    case 'meters':
      return value;
    case 'km':
    case 'kilometer':
    case 'kilometers':
      return value * 1000.0;
    case 'cm':
    case 'centimeter':
    case 'centimeters':
      return value / 100.0;
    case 'mm':
    case 'millimeter':
    case 'millimeters':
      return value / 1000.0;
    case 'mi':
    case 'mile':
    case 'miles':
      return value * 1609.344;
    case 'yd':
    case 'yard':
    case 'yards':
      return value * 0.9144;
    case 'ft':
    case 'foot':
    case 'feet':
      return value * 0.3048;
    case 'in':
    case 'inch':
    case 'inches':
      return value * 0.0254;
    default:
      return null;
  }
}

double? toMetersPerSecond(double value, String? unit) {
  switch (unit?.toLowerCase()) {
    case 'm/s':
    case 'm/sec':
    case 'meter/second':
    case 'meters/second':
    case 'meters/sec':
      return value;
    case 'km/hr':
    case 'km/h':
    case 'kph':
    case 'kilometer/hour':
    case 'kilometers/hour':
      return value / 3.6;
    case 'mi/hr':
    case 'mi/h':
    case 'mph':
    case 'mile/hour':
    case 'miles/hour':
      return value * 0.44704;
    case 'ft/s':
    case 'ft/sec':
    case 'foot/second':
    case 'feet/second':
    case 'feet/sec':
      return value * 0.3048;
    default:
      return null;
  }
}

double? toKilograms(double value, String? unit) {
  switch (unit?.toLowerCase()) {
    case 'kg':
    case 'kilogram':
    case 'kilograms':
      return value;
    case 'g':
    case 'gram':
    case 'grams':
      return value / 1000.0;
    case 'lb':
    case 'lbs':
    case 'pound':
    case 'pounds':
      return value * 0.45359237;
    case 'oz':
    case 'ounce':
    case 'ounces':
      return value * 0.028349523125;
    case 'st':
    case 'stone':
    case 'stones':
      return value * 6.35029318;
    default:
      return null;
  }
}

double? toKilocalories(double value, String? unit) {
  switch (unit?.toLowerCase()) {
    case 'kcal':
    case 'cal':
    case 'calorie':
    case 'calories':
    case 'calories/hour':
    case 'calories/hr':
      return value;
    case 'kj':
    case 'kilojoule':
    case 'kilojoules':
      return value / 4.184;
    case 'j':
    case 'joule':
    case 'joules':
      return value / 4184.0;
    default:
      return null;
  }
}

double? toMilliliters(double value, String? unit) {
  switch (unit?.toLowerCase()) {
    case 'ml':
    case 'milliliter':
    case 'milliliters':
      return value;
    case 'l':
    case 'liter':
    case 'liters':
      return value * 1000.0;
    case 'fl_oz_us':
    case 'floz':
    case 'fl oz':
    case 'oz':
      return value * 29.5735295625;
    default:
      return null;
  }
}

double? toPercentage(double value, String? unit) {
  if (unit == '%') {
    return value <= 1.0 ? value * 100.0 : value;
  }
  return null;
}

double? toCelsius(double value, String? unit) {
  switch (unit) {
    case 'degC':
    case '°C':
      return value;
    case 'degF':
    case '°F':
      return (value - 32.0) * 5.0 / 9.0;
    default:
      return null;
  }
}

/// A mass converted to grams (the canonical unit for nutrition), or `null` when
/// the unit is unsupported. Mirrors the Kotlin `Double.toMass` (which returns a
/// Health Connect `Mass`); the Dart importer stores nutrient masses in grams.
double? toGrams(double value, String? unit) {
  switch (unit?.toLowerCase()) {
    case 'kg':
    case 'kilogram':
    case 'kilograms':
      return value * 1000.0;
    case 'g':
    case 'gram':
    case 'grams':
      return value;
    case 'mg':
    case 'milligram':
    case 'milligrams':
      return value / 1000.0;
    case 'mcg':
    case 'ug':
    case 'µg':
    case 'microgram':
    case 'micrograms':
      return value / 1000000.0;
    case 'oz':
    case 'ounce':
    case 'ounces':
      return value * 28.349523125;
    case 'lb':
    case 'lbs':
    case 'pound':
    case 'pounds':
      return value * 453.59237;
    default:
      return null;
  }
}

/// Blood glucose converted to millimoles per litre (the canonical unit), or
/// `null` when unsupported. Mirrors the Kotlin `Double.toBloodGlucose`.
double? toMillimolesPerLiter(double value, String? unit) {
  switch (unit?.toLowerCase()) {
    case 'mg/dl':
      return value / 18.0;
    case 'mmol/l':
      return value;
    default:
      return null;
  }
}
