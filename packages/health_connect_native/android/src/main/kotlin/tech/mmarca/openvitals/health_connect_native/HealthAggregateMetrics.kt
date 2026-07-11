package tech.mmarca.openvitals.health_connect_native

import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.CyclingPedalingCadenceRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ElevationGainedRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.MindfulnessSessionRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.PowerRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsCadenceRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.WheelchairPushesRecord

/**
 * Maps canonical `"RecordType.metric"` strings (e.g. `"Steps.count"`,
 * `"HeartRate.bpmAvg"`) to Health Connect [AggregateMetric]s plus a canonical
 * unit extractor. Extractors capture the concretely-typed metric so the
 * [AggregationResult] getter resolves the correct return type.
 *
 * Canonical units: meters, kilograms, kilocalories, liters, meters/second,
 * watts, bpm, count, grams (nutrients), milliseconds (durations).
 */
internal object HealthAggregateMetrics {

    /** A metric to request together with how to read it back as a [Double]. */
    class Spec(
        val metric: AggregateMetric<*>,
        val extract: (AggregationResult) -> Double?,
    )

    private val specs: Map<String, Spec> = buildMap {
        // Activity counts / totals.
        put("Steps.count", Spec(StepsRecord.COUNT_TOTAL) { it[StepsRecord.COUNT_TOTAL]?.toDouble() })
        put("Distance.distance", Spec(DistanceRecord.DISTANCE_TOTAL) { it[DistanceRecord.DISTANCE_TOTAL]?.inMeters })
        put(
            "ActiveCaloriesBurned.energy",
            Spec(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL) {
                it[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories
            },
        )
        put(
            "TotalCaloriesBurned.energy",
            Spec(TotalCaloriesBurnedRecord.ENERGY_TOTAL) { it[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories },
        )
        put(
            "FloorsClimbed.floors",
            Spec(FloorsClimbedRecord.FLOORS_CLIMBED_TOTAL) { it[FloorsClimbedRecord.FLOORS_CLIMBED_TOTAL] },
        )
        put(
            "ElevationGained.elevation",
            Spec(ElevationGainedRecord.ELEVATION_GAINED_TOTAL) { it[ElevationGainedRecord.ELEVATION_GAINED_TOTAL]?.inMeters },
        )
        put(
            "WheelchairPushes.count",
            Spec(WheelchairPushesRecord.COUNT_TOTAL) { it[WheelchairPushesRecord.COUNT_TOTAL]?.toDouble() },
        )

        // Heart rate.
        put("HeartRate.bpmAvg", Spec(HeartRateRecord.BPM_AVG) { it[HeartRateRecord.BPM_AVG]?.toDouble() })
        put("HeartRate.bpmMin", Spec(HeartRateRecord.BPM_MIN) { it[HeartRateRecord.BPM_MIN]?.toDouble() })
        put("HeartRate.bpmMax", Spec(HeartRateRecord.BPM_MAX) { it[HeartRateRecord.BPM_MAX]?.toDouble() })
        put("RestingHeartRate.bpmAvg", Spec(RestingHeartRateRecord.BPM_AVG) { it[RestingHeartRateRecord.BPM_AVG]?.toDouble() })
        put("RestingHeartRate.bpmMin", Spec(RestingHeartRateRecord.BPM_MIN) { it[RestingHeartRateRecord.BPM_MIN]?.toDouble() })
        put("RestingHeartRate.bpmMax", Spec(RestingHeartRateRecord.BPM_MAX) { it[RestingHeartRateRecord.BPM_MAX]?.toDouble() })

        // Hydration.
        put("Hydration.volume", Spec(HydrationRecord.VOLUME_TOTAL) { it[HydrationRecord.VOLUME_TOTAL]?.inLiters })

        // Body measurements.
        put("Weight.weightAvg", Spec(WeightRecord.WEIGHT_AVG) { it[WeightRecord.WEIGHT_AVG]?.inKilograms })
        put("Weight.weightMin", Spec(WeightRecord.WEIGHT_MIN) { it[WeightRecord.WEIGHT_MIN]?.inKilograms })
        put("Weight.weightMax", Spec(WeightRecord.WEIGHT_MAX) { it[WeightRecord.WEIGHT_MAX]?.inKilograms })
        put("Height.heightAvg", Spec(HeightRecord.HEIGHT_AVG) { it[HeightRecord.HEIGHT_AVG]?.inMeters })
        put("Height.heightMin", Spec(HeightRecord.HEIGHT_MIN) { it[HeightRecord.HEIGHT_MIN]?.inMeters })
        put("Height.heightMax", Spec(HeightRecord.HEIGHT_MAX) { it[HeightRecord.HEIGHT_MAX]?.inMeters })

        // Exercise sensor series.
        put("Speed.speedAvg", Spec(SpeedRecord.SPEED_AVG) { it[SpeedRecord.SPEED_AVG]?.inMetersPerSecond })
        put("Speed.speedMin", Spec(SpeedRecord.SPEED_MIN) { it[SpeedRecord.SPEED_MIN]?.inMetersPerSecond })
        put("Speed.speedMax", Spec(SpeedRecord.SPEED_MAX) { it[SpeedRecord.SPEED_MAX]?.inMetersPerSecond })
        put("Power.powerAvg", Spec(PowerRecord.POWER_AVG) { it[PowerRecord.POWER_AVG]?.inWatts })
        put("Power.powerMin", Spec(PowerRecord.POWER_MIN) { it[PowerRecord.POWER_MIN]?.inWatts })
        put("Power.powerMax", Spec(PowerRecord.POWER_MAX) { it[PowerRecord.POWER_MAX]?.inWatts })
        put("StepsCadence.rateAvg", Spec(StepsCadenceRecord.RATE_AVG) { it[StepsCadenceRecord.RATE_AVG] })
        put("StepsCadence.rateMin", Spec(StepsCadenceRecord.RATE_MIN) { it[StepsCadenceRecord.RATE_MIN] })
        put("StepsCadence.rateMax", Spec(StepsCadenceRecord.RATE_MAX) { it[StepsCadenceRecord.RATE_MAX] })
        put("CyclingPedalingCadence.rpmAvg", Spec(CyclingPedalingCadenceRecord.RPM_AVG) { it[CyclingPedalingCadenceRecord.RPM_AVG] })
        put("CyclingPedalingCadence.rpmMin", Spec(CyclingPedalingCadenceRecord.RPM_MIN) { it[CyclingPedalingCadenceRecord.RPM_MIN] })
        put("CyclingPedalingCadence.rpmMax", Spec(CyclingPedalingCadenceRecord.RPM_MAX) { it[CyclingPedalingCadenceRecord.RPM_MAX] })

        // Durations (milliseconds).
        put(
            "Sleep.duration",
            Spec(SleepSessionRecord.SLEEP_DURATION_TOTAL) {
                it[SleepSessionRecord.SLEEP_DURATION_TOTAL]?.toMillis()?.toDouble()
            },
        )
        put(
            "MindfulnessSession.duration",
            Spec(MindfulnessSessionRecord.MINDFULNESS_DURATION_TOTAL) {
                it[MindfulnessSessionRecord.MINDFULNESS_DURATION_TOTAL]?.toMillis()?.toDouble()
            },
        )

        // Nutrition totals (energy in kcal, everything else in grams).
        put("Nutrition.energy", Spec(NutritionRecord.ENERGY_TOTAL) { it[NutritionRecord.ENERGY_TOTAL]?.inKilocalories })
        put("Nutrition.protein", Spec(NutritionRecord.PROTEIN_TOTAL) { it[NutritionRecord.PROTEIN_TOTAL]?.inGrams })
        put(
            "Nutrition.totalCarbohydrate",
            Spec(NutritionRecord.TOTAL_CARBOHYDRATE_TOTAL) { it[NutritionRecord.TOTAL_CARBOHYDRATE_TOTAL]?.inGrams },
        )
        put("Nutrition.totalFat", Spec(NutritionRecord.TOTAL_FAT_TOTAL) { it[NutritionRecord.TOTAL_FAT_TOTAL]?.inGrams })
        put(
            "Nutrition.dietaryFiber",
            Spec(NutritionRecord.DIETARY_FIBER_TOTAL) { it[NutritionRecord.DIETARY_FIBER_TOTAL]?.inGrams },
        )
        put("Nutrition.sugar", Spec(NutritionRecord.SUGAR_TOTAL) { it[NutritionRecord.SUGAR_TOTAL]?.inGrams })
        put(
            "Nutrition.saturatedFat",
            Spec(NutritionRecord.SATURATED_FAT_TOTAL) { it[NutritionRecord.SATURATED_FAT_TOTAL]?.inGrams },
        )
        put("Nutrition.sodium", Spec(NutritionRecord.SODIUM_TOTAL) { it[NutritionRecord.SODIUM_TOTAL]?.inGrams })
        put("Nutrition.potassium", Spec(NutritionRecord.POTASSIUM_TOTAL) { it[NutritionRecord.POTASSIUM_TOTAL]?.inGrams })
        put("Nutrition.calcium", Spec(NutritionRecord.CALCIUM_TOTAL) { it[NutritionRecord.CALCIUM_TOTAL]?.inGrams })
        put("Nutrition.caffeine", Spec(NutritionRecord.CAFFEINE_TOTAL) { it[NutritionRecord.CAFFEINE_TOTAL]?.inGrams })
        put("Nutrition.cholesterol", Spec(NutritionRecord.CHOLESTEROL_TOTAL) { it[NutritionRecord.CHOLESTEROL_TOTAL]?.inGrams })
        put("Nutrition.iron", Spec(NutritionRecord.IRON_TOTAL) { it[NutritionRecord.IRON_TOTAL]?.inGrams })
    }

    /** The [Spec] for a metric key, or null when unknown. */
    fun specFor(key: String): Spec? = specs[key]
}
