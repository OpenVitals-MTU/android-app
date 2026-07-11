/// HealthKit type identifiers used by the Apple Health export importer, ported
/// 1:1 from the Kotlin `AppleHealthImportTypes.kt`.
///
/// These are the correctness core of the importer: the HKQuantityType /
/// HKCategoryType → domain-record mapping and unit conversions key off them.
library;

const String appleStepCount = 'HKQuantityTypeIdentifierStepCount';
const String appleDistanceWalkingRunning =
    'HKQuantityTypeIdentifierDistanceWalkingRunning';
const String appleDistanceCycling = 'HKQuantityTypeIdentifierDistanceCycling';
const String appleDistanceSwimming = 'HKQuantityTypeIdentifierDistanceSwimming';
const String appleDistanceWheelchair =
    'HKQuantityTypeIdentifierDistanceWheelchair';
const String appleWalkingSpeed = 'HKQuantityTypeIdentifierWalkingSpeed';
const String appleActiveEnergyBurned =
    'HKQuantityTypeIdentifierActiveEnergyBurned';
const String appleBasalEnergyBurned =
    'HKQuantityTypeIdentifierBasalEnergyBurned';
const String appleFlightsClimbed = 'HKQuantityTypeIdentifierFlightsClimbed';
const String appleElevationAscended =
    'HKQuantityTypeIdentifierElevationAscended';
const String applePushCount = 'HKQuantityTypeIdentifierPushCount';
const String appleHeartRate = 'HKQuantityTypeIdentifierHeartRate';
const String appleRestingHeartRate = 'HKQuantityTypeIdentifierRestingHeartRate';
const String appleHeartRateVariabilitySdnn =
    'HKQuantityTypeIdentifierHeartRateVariabilitySDNN';
const String appleBodyMass = 'HKQuantityTypeIdentifierBodyMass';
const String appleHeight = 'HKQuantityTypeIdentifierHeight';
const String appleBodyFatPercentage =
    'HKQuantityTypeIdentifierBodyFatPercentage';
const String appleLeanBodyMass = 'HKQuantityTypeIdentifierLeanBodyMass';
const String appleBoneMass = 'HKQuantityTypeIdentifierBoneMass';
const String appleBodyWaterMass = 'HKQuantityTypeIdentifierBodyWaterMass';
const String appleDietaryWater = 'HKQuantityTypeIdentifierDietaryWater';
const String appleOxygenSaturation = 'HKQuantityTypeIdentifierOxygenSaturation';
const String appleRespiratoryRate = 'HKQuantityTypeIdentifierRespiratoryRate';
const String appleBodyTemperature = 'HKQuantityTypeIdentifierBodyTemperature';
const String appleBloodGlucose = 'HKQuantityTypeIdentifierBloodGlucose';
const String appleVo2Max = 'HKQuantityTypeIdentifierVO2Max';
const String appleBasalBodyTemperature =
    'HKQuantityTypeIdentifierBasalBodyTemperature';
const String appleBloodPressureSystolic =
    'HKQuantityTypeIdentifierBloodPressureSystolic';
const String appleBloodPressureDiastolic =
    'HKQuantityTypeIdentifierBloodPressureDiastolic';
const String appleBloodPressureCorrelation =
    'HKCorrelationTypeIdentifierBloodPressure';
const String appleSleepAnalysis = 'HKCategoryTypeIdentifierSleepAnalysis';
const String appleSleepInBed = 'HKCategoryValueSleepAnalysisInBed';
const String appleSleepAsleep = 'HKCategoryValueSleepAnalysisAsleep';
const String appleSleepAsleepUnspecified =
    'HKCategoryValueSleepAnalysisAsleepUnspecified';
const String appleSleepAsleepCore = 'HKCategoryValueSleepAnalysisAsleepCore';
const String appleSleepAsleepDeep = 'HKCategoryValueSleepAnalysisAsleepDeep';
const String appleSleepAsleepRem = 'HKCategoryValueSleepAnalysisAsleepREM';
const String appleSleepAwake = 'HKCategoryValueSleepAnalysisAwake';
const String appleMindfulSession = 'HKCategoryTypeIdentifierMindfulSession';
const String appleMenstrualFlow = 'HKCategoryTypeIdentifierMenstrualFlow';
const String appleOvulationTest = 'HKCategoryTypeIdentifierOvulationTestResult';
const String appleCervicalMucus =
    'HKCategoryTypeIdentifierCervicalMucusQuality';
const String appleIntermenstrualBleeding =
    'HKCategoryTypeIdentifierIntermenstrualBleeding';
const String appleSexualActivity = 'HKCategoryTypeIdentifierSexualActivity';

const String appleDietaryEnergyConsumed =
    'HKQuantityTypeIdentifierDietaryEnergyConsumed';
const String appleDietaryFatTotal = 'HKQuantityTypeIdentifierDietaryFatTotal';
const String appleDietaryFatSaturated =
    'HKQuantityTypeIdentifierDietaryFatSaturated';
const String appleDietaryFatTrans = 'HKQuantityTypeIdentifierDietaryFatTrans';
const String appleDietaryFatMonounsaturated =
    'HKQuantityTypeIdentifierDietaryFatMonounsaturated';
const String appleDietaryFatPolyunsaturated =
    'HKQuantityTypeIdentifierDietaryFatPolyunsaturated';
const String appleDietaryCholesterol =
    'HKQuantityTypeIdentifierDietaryCholesterol';
const String appleDietarySodium = 'HKQuantityTypeIdentifierDietarySodium';
const String appleDietaryCarbohydrates =
    'HKQuantityTypeIdentifierDietaryCarbohydrates';
const String appleDietaryFiber = 'HKQuantityTypeIdentifierDietaryFiber';
const String appleDietarySugar = 'HKQuantityTypeIdentifierDietarySugar';
const String appleDietaryProtein = 'HKQuantityTypeIdentifierDietaryProtein';
const String appleDietaryCaffeine = 'HKQuantityTypeIdentifierDietaryCaffeine';
const String appleDietaryCalcium = 'HKQuantityTypeIdentifierDietaryCalcium';
const String appleDietaryIron = 'HKQuantityTypeIdentifierDietaryIron';
const String appleDietaryThiamin = 'HKQuantityTypeIdentifierDietaryThiamin';
const String appleDietaryRiboflavin =
    'HKQuantityTypeIdentifierDietaryRiboflavin';
const String appleDietaryNiacin = 'HKQuantityTypeIdentifierDietaryNiacin';
const String appleDietaryFolate = 'HKQuantityTypeIdentifierDietaryFolate';
const String appleDietaryBiotin = 'HKQuantityTypeIdentifierDietaryBiotin';
const String appleDietaryPantothenicAcid =
    'HKQuantityTypeIdentifierDietaryPantothenicAcid';
const String appleDietaryPhosphorus =
    'HKQuantityTypeIdentifierDietaryPhosphorus';
const String appleDietaryIodine = 'HKQuantityTypeIdentifierDietaryIodine';
const String appleDietaryMagnesium = 'HKQuantityTypeIdentifierDietaryMagnesium';
const String appleDietaryZinc = 'HKQuantityTypeIdentifierDietaryZinc';
const String appleDietarySelenium = 'HKQuantityTypeIdentifierDietarySelenium';
const String appleDietaryCopper = 'HKQuantityTypeIdentifierDietaryCopper';
const String appleDietaryManganese = 'HKQuantityTypeIdentifierDietaryManganese';
const String appleDietaryChromium = 'HKQuantityTypeIdentifierDietaryChromium';
const String appleDietaryMolybdenum =
    'HKQuantityTypeIdentifierDietaryMolybdenum';
const String appleDietaryPotassium = 'HKQuantityTypeIdentifierDietaryPotassium';
const String appleDietaryVitaminA = 'HKQuantityTypeIdentifierDietaryVitaminA';
const String appleDietaryVitaminB6 = 'HKQuantityTypeIdentifierDietaryVitaminB6';
const String appleDietaryVitaminB12 =
    'HKQuantityTypeIdentifierDietaryVitaminB12';
const String appleDietaryVitaminC = 'HKQuantityTypeIdentifierDietaryVitaminC';
const String appleDietaryVitaminD = 'HKQuantityTypeIdentifierDietaryVitaminD';
const String appleDietaryVitaminE = 'HKQuantityTypeIdentifierDietaryVitaminE';
const String appleDietaryVitaminK = 'HKQuantityTypeIdentifierDietaryVitaminK';
const String appleNutritionSyntheticType = 'AppleHealthNutritionGroup';

const Set<String> appleDistanceTypes = {
  appleDistanceWalkingRunning,
  appleDistanceCycling,
  appleDistanceSwimming,
  appleDistanceWheelchair,
};

const Set<String> appleAdditiveOverlapSensitiveTypes = {
  appleDistanceWalkingRunning,
  appleDistanceCycling,
  appleDistanceSwimming,
  appleDistanceWheelchair,
  appleStepCount,
  appleActiveEnergyBurned,
};

const Set<String> appleNutritionTypes = {
  appleDietaryEnergyConsumed,
  appleDietaryFatTotal,
  appleDietaryFatSaturated,
  appleDietaryFatTrans,
  appleDietaryFatMonounsaturated,
  appleDietaryFatPolyunsaturated,
  appleDietaryCholesterol,
  appleDietarySodium,
  appleDietaryCarbohydrates,
  appleDietaryFiber,
  appleDietarySugar,
  appleDietaryProtein,
  appleDietaryCaffeine,
  appleDietaryCalcium,
  appleDietaryIron,
  appleDietaryThiamin,
  appleDietaryRiboflavin,
  appleDietaryNiacin,
  appleDietaryFolate,
  appleDietaryBiotin,
  appleDietaryPantothenicAcid,
  appleDietaryPhosphorus,
  appleDietaryIodine,
  appleDietaryMagnesium,
  appleDietaryZinc,
  appleDietarySelenium,
  appleDietaryCopper,
  appleDietaryManganese,
  appleDietaryChromium,
  appleDietaryMolybdenum,
  appleDietaryPotassium,
  appleDietaryVitaminA,
  appleDietaryVitaminB6,
  appleDietaryVitaminB12,
  appleDietaryVitaminC,
  appleDietaryVitaminD,
  appleDietaryVitaminE,
  appleDietaryVitaminK,
};

const Set<String> appleCycleCategoryTypes = {
  appleMenstrualFlow,
  appleOvulationTest,
  appleCervicalMucus,
  appleIntermenstrualBleeding,
  appleSexualActivity,
};

const int maxWorkoutOverlapCandidates = 10000;
