package tech.mmarca.openvitals.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import java.time.LocalDate
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.BodyMeasurementType
import tech.mmarca.openvitals.domain.model.VitalsMeasurementType
import tech.mmarca.openvitals.features.activity.ActiveCaloriesScreen
import tech.mmarca.openvitals.features.activity.ActivityMetric
import tech.mmarca.openvitals.features.activity.ActivityOverviewViewModel
import tech.mmarca.openvitals.features.activity.ActivityViewModel
import tech.mmarca.openvitals.features.activity.ActivitiesScreen
import tech.mmarca.openvitals.features.activity.ActivitiesViewModel
import tech.mmarca.openvitals.features.activity.CaloriesOutScreen
import tech.mmarca.openvitals.features.activity.CaloriesScreen
import tech.mmarca.openvitals.features.activity.CaloriesViewModel
import tech.mmarca.openvitals.features.activity.CardioLoadDetailScreen
import tech.mmarca.openvitals.features.activity.DistanceScreen
import tech.mmarca.openvitals.features.activity.ElevationScreen
import tech.mmarca.openvitals.features.activity.FloorsScreen
import tech.mmarca.openvitals.features.activity.StepsScreen
import tech.mmarca.openvitals.features.activity.WheelchairPushesScreen
import tech.mmarca.openvitals.features.body.BmiScreen
import tech.mmarca.openvitals.features.body.BmrScreen
import tech.mmarca.openvitals.features.body.BodyFatScreen
import tech.mmarca.openvitals.features.body.BodyMetric
import tech.mmarca.openvitals.features.body.BodyScreen
import tech.mmarca.openvitals.features.body.BodyViewModel
import tech.mmarca.openvitals.features.body.BodyWaterMassScreen
import tech.mmarca.openvitals.features.body.BoneMassScreen
import tech.mmarca.openvitals.features.body.HeightScreen
import tech.mmarca.openvitals.features.body.LeanMassScreen
import tech.mmarca.openvitals.features.body.WeightScreen
import tech.mmarca.openvitals.features.bodyenergy.BodyEnergyDetailsScreen
import tech.mmarca.openvitals.features.bodyenergy.BodyEnergyViewModel
import tech.mmarca.openvitals.features.caffeine.CaffeineScreen
import tech.mmarca.openvitals.features.caffeine.CaffeineViewModel
import tech.mmarca.openvitals.features.cycle.CycleScreen
import tech.mmarca.openvitals.features.cycle.CycleViewModel
import tech.mmarca.openvitals.features.dashboard.DashboardWidgetId
import tech.mmarca.openvitals.features.heart.AverageHeartRateScreen
import tech.mmarca.openvitals.features.heart.BloodGlucoseScreen
import tech.mmarca.openvitals.features.heart.BloodPressureScreen
import tech.mmarca.openvitals.features.heart.BodyTemperatureScreen
import tech.mmarca.openvitals.features.heart.HeartMetric
import tech.mmarca.openvitals.features.heart.HeartViewModel
import tech.mmarca.openvitals.features.heart.HrvScreen
import tech.mmarca.openvitals.features.heart.RespiratoryRateScreen
import tech.mmarca.openvitals.features.heart.RestingHeartRateScreen
import tech.mmarca.openvitals.features.heart.SkinTemperatureScreen
import tech.mmarca.openvitals.features.heart.SpO2Screen
import tech.mmarca.openvitals.features.heart.Vo2MaxScreen
import tech.mmarca.openvitals.features.hydration.HydrationScreen
import tech.mmarca.openvitals.features.hydration.HydrationViewModel
import tech.mmarca.openvitals.features.mindfulness.MindfulnessScreen
import tech.mmarca.openvitals.features.mindfulness.MindfulnessViewModel
import tech.mmarca.openvitals.features.nutrition.CaloriesInScreen
import tech.mmarca.openvitals.features.nutrition.CarbsScreen
import tech.mmarca.openvitals.features.nutrition.FatScreen
import tech.mmarca.openvitals.features.nutrition.NutritionMetric
import tech.mmarca.openvitals.features.nutrition.NutritionScreen
import tech.mmarca.openvitals.features.nutrition.NutritionViewModel
import tech.mmarca.openvitals.features.nutrition.ProteinScreen
import tech.mmarca.openvitals.features.sleep.SleepScreen
import tech.mmarca.openvitals.features.sleep.SleepViewModel

@Composable
internal fun MetricRouteContent(
    metricId: DashboardWidgetId?,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onOpenMetric: (DashboardWidgetId) -> Unit,
    onOpenCardioLoad: () -> Unit,
    onOpenActivity: (String) -> Unit,
    onEditActivity: (String) -> Unit,
    onOpenSleepSession: (String) -> Unit,
    onOpenSleepScore: () -> Unit,
    onOpenSleepEfficiency: () -> Unit,
    onEditHydrationEntry: (String) -> Unit,
    onEditMindfulnessSession: (String) -> Unit,
    onEditBodyMeasurement: (BodyMeasurementType, String) -> Unit,
    onEditVitalsMeasurement: (VitalsMeasurementType, String) -> Unit,
    onSectionEditStateChanged: (Boolean, () -> Unit) -> Unit = { _, _ -> },
) {
    if (metricId?.isCaloriesDetailMetric() == true) {
        val caloriesViewModel = hiltViewModel<CaloriesViewModel>()
        CaloriesScreen(
            viewModel = caloriesViewModel,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            onSectionEditStateChanged = onSectionEditStateChanged,
        )
        return
    }

    if (metricId?.isNutritionDetailMetric() == true) {
        val nutritionMetric = metricId.toNutritionMetricOrNull()
        if (nutritionMetric != null) {
            val nutritionViewModel = hiltViewModel<NutritionViewModel>()
            NutritionMetricRouteScreen(
                metric = nutritionMetric,
                viewModel = nutritionViewModel,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                onSectionEditStateChanged = onSectionEditStateChanged,
            )
            return
        }
    }

    if (metricId?.isBodyDetailMetric() == true) {
        val bodyViewModel = hiltViewModel<BodyViewModel>()
        BodyScreen(
            viewModel = bodyViewModel,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            onEditBodyMeasurement = onEditBodyMeasurement,
            onSectionEditStateChanged = onSectionEditStateChanged,
        )
        return
    }

    metricId?.toActivityMetricOrNull()?.let { activityMetric ->
        val activityViewModel = hiltViewModel<ActivityViewModel>()
        ActivityMetricRouteScreen(
            metric = activityMetric,
            viewModel = activityViewModel,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            onSectionEditStateChanged = onSectionEditStateChanged,
        )
        return
    }

    metricId?.toHeartMetricOrNull()?.let { heartMetric ->
        val heartViewModel = hiltViewModel<HeartViewModel>()
        HeartMetricRouteScreen(
            metric = heartMetric,
            viewModel = heartViewModel,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            onEditVitalsMeasurement = onEditVitalsMeasurement,
            onSectionEditStateChanged = onSectionEditStateChanged,
        )
        return
    }

    metricId?.toBodyMetricOrNull()?.let { bodyMetric ->
        val bodyViewModel = hiltViewModel<BodyViewModel>()
        BodyMetricRouteScreen(
            metric = bodyMetric,
            viewModel = bodyViewModel,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            onEditBodyMeasurement = onEditBodyMeasurement,
            onSectionEditStateChanged = onSectionEditStateChanged,
        )
        return
    }

    when (metricId) {
        DashboardWidgetId.WORKOUT -> {
            val activitiesViewModel = hiltViewModel<ActivitiesViewModel>()
            ActivitiesScreen(
                viewModel = activitiesViewModel,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                onOpenActivity = onOpenActivity,
                onEditActivity = onEditActivity,
                onOpenCardioLoad = onOpenCardioLoad,
                onOpenSteps = { onOpenMetric(DashboardWidgetId.STEPS) },
                onOpenDistance = { onOpenMetric(DashboardWidgetId.DISTANCE) },
                onOpenEnergyBurned = { onOpenMetric(DashboardWidgetId.CALORIES_OUT) },
                onOpenHrv = { onOpenMetric(DashboardWidgetId.HRV) },
                onSectionEditStateChanged = onSectionEditStateChanged,
            )
        }
        DashboardWidgetId.SLEEP -> {
            val sleepViewModel = hiltViewModel<SleepViewModel>()
            SleepScreen(
                viewModel = sleepViewModel,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                onOpenSleepSession = onOpenSleepSession,
                onOpenSleepScore = onOpenSleepScore,
                onOpenSleepEfficiency = onOpenSleepEfficiency,
                onSectionEditStateChanged = onSectionEditStateChanged,
            )
        }
        DashboardWidgetId.BODY_ENERGY -> {
            val bodyEnergyViewModel = hiltViewModel<BodyEnergyViewModel>()
            BodyEnergyDetailsScreen(
                viewModel = bodyEnergyViewModel,
                selectedDate = LocalDate.now(),
            )
        }
        DashboardWidgetId.HYDRATION -> {
            val hydrationViewModel = hiltViewModel<HydrationViewModel>()
            HydrationScreen(
                viewModel = hydrationViewModel,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                onEditHydrationEntry = onEditHydrationEntry,
                onSectionEditStateChanged = onSectionEditStateChanged,
            )
        }
        DashboardWidgetId.CAFFEINE -> {
            val caffeineViewModel = hiltViewModel<CaffeineViewModel>()
            CaffeineScreen(
                viewModel = caffeineViewModel,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                onSectionEditStateChanged = onSectionEditStateChanged,
            )
        }
        DashboardWidgetId.MINDFULNESS -> {
            val mindfulnessViewModel = hiltViewModel<MindfulnessViewModel>()
            MindfulnessScreen(
                viewModel = mindfulnessViewModel,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                onEditMindfulnessSession = onEditMindfulnessSession,
            )
        }
        DashboardWidgetId.CYCLE -> {
            val cycleViewModel = hiltViewModel<CycleViewModel>()
            CycleScreen(
                viewModel = cycleViewModel,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
            )
        }
        DashboardWidgetId.WEEKLY_CARDIO_LOAD,
        DashboardWidgetId.CARDIO_LOAD -> {
            val activityOverviewViewModel = hiltViewModel<ActivityOverviewViewModel>()
            CardioLoadDetailScreen(
                viewModel = activityOverviewViewModel,
                unitFormatter = unitFormatter,
            )
        }
        else -> {
            Text(
                text = stringResource(R.string.unknown_error),
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Composable
private fun ActivityMetricRouteScreen(
    metric: ActivityMetric,
    viewModel: ActivityViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onSectionEditStateChanged: (Boolean, () -> Unit) -> Unit,
) {
    when (metric) {
        ActivityMetric.STEPS -> StepsScreen(
            viewModel,
            unitFormatter,
            dateTimeFormatterProvider,
            onSectionEditStateChanged,
        )
        ActivityMetric.DISTANCE -> DistanceScreen(
            viewModel,
            unitFormatter,
            dateTimeFormatterProvider,
            onSectionEditStateChanged,
        )
        ActivityMetric.CALORIES_BURNED -> CaloriesOutScreen(
            viewModel,
            unitFormatter,
            dateTimeFormatterProvider,
            onSectionEditStateChanged,
        )
        ActivityMetric.ACTIVE_CALORIES -> ActiveCaloriesScreen(
            viewModel,
            unitFormatter,
            dateTimeFormatterProvider,
            onSectionEditStateChanged,
        )
        ActivityMetric.FLOORS -> FloorsScreen(
            viewModel,
            unitFormatter,
            dateTimeFormatterProvider,
            onSectionEditStateChanged,
        )
        ActivityMetric.ELEVATION -> ElevationScreen(
            viewModel,
            unitFormatter,
            dateTimeFormatterProvider,
            onSectionEditStateChanged,
        )
        ActivityMetric.WHEELCHAIR_PUSHES -> WheelchairPushesScreen(
            viewModel,
            unitFormatter,
            dateTimeFormatterProvider,
            onSectionEditStateChanged,
        )
    }
}

@Composable
private fun HeartMetricRouteScreen(
    metric: HeartMetric,
    viewModel: HeartViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onEditVitalsMeasurement: (VitalsMeasurementType, String) -> Unit,
    onSectionEditStateChanged: (Boolean, () -> Unit) -> Unit,
) {
    when (metric) {
        HeartMetric.AVERAGE_HEART_RATE -> AverageHeartRateScreen(
            viewModel,
            unitFormatter,
            dateTimeFormatterProvider,
            onSectionEditStateChanged,
        )
        HeartMetric.RESTING_HEART_RATE -> RestingHeartRateScreen(
            viewModel,
            unitFormatter,
            dateTimeFormatterProvider,
            onSectionEditStateChanged,
        )
        HeartMetric.HRV -> HrvScreen(viewModel, unitFormatter, dateTimeFormatterProvider, onSectionEditStateChanged)
        HeartMetric.BLOOD_PRESSURE -> BloodPressureScreen(
            viewModel,
            unitFormatter,
            dateTimeFormatterProvider,
            onEditVitalsMeasurement,
            onSectionEditStateChanged,
        )
        HeartMetric.SPO2 -> SpO2Screen(
            viewModel,
            unitFormatter,
            dateTimeFormatterProvider,
            onEditVitalsMeasurement,
            onSectionEditStateChanged,
        )
        HeartMetric.VO2_MAX -> Vo2MaxScreen(viewModel, unitFormatter, dateTimeFormatterProvider, onSectionEditStateChanged)
        HeartMetric.RESPIRATORY_RATE -> RespiratoryRateScreen(
            viewModel,
            unitFormatter,
            dateTimeFormatterProvider,
            onEditVitalsMeasurement,
            onSectionEditStateChanged,
        )
        HeartMetric.BODY_TEMPERATURE -> BodyTemperatureScreen(
            viewModel,
            unitFormatter,
            dateTimeFormatterProvider,
            onEditVitalsMeasurement,
            onSectionEditStateChanged,
        )
        HeartMetric.BLOOD_GLUCOSE -> BloodGlucoseScreen(
            viewModel,
            unitFormatter,
            dateTimeFormatterProvider,
            onSectionEditStateChanged,
        )
        HeartMetric.SKIN_TEMPERATURE -> SkinTemperatureScreen(
            viewModel,
            unitFormatter,
            dateTimeFormatterProvider,
            onSectionEditStateChanged,
        )
    }
}

@Composable
private fun NutritionMetricRouteScreen(
    metric: NutritionMetric,
    viewModel: NutritionViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onSectionEditStateChanged: (Boolean, () -> Unit) -> Unit,
) {
    when (metric) {
        NutritionMetric.CALORIES_IN -> CaloriesInScreen(
            viewModel,
            unitFormatter,
            dateTimeFormatterProvider,
            onSectionEditStateChanged,
        )
        NutritionMetric.PROTEIN -> ProteinScreen(
            viewModel,
            unitFormatter,
            dateTimeFormatterProvider,
            onSectionEditStateChanged,
        )
        NutritionMetric.CARBS -> CarbsScreen(
            viewModel,
            unitFormatter,
            dateTimeFormatterProvider,
            onSectionEditStateChanged,
        )
        NutritionMetric.FAT -> FatScreen(
            viewModel,
            unitFormatter,
            dateTimeFormatterProvider,
            onSectionEditStateChanged,
        )
    }
}

@Composable
private fun BodyMetricRouteScreen(
    metric: BodyMetric,
    viewModel: BodyViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onEditBodyMeasurement: (BodyMeasurementType, String) -> Unit,
    onSectionEditStateChanged: (Boolean, () -> Unit) -> Unit,
) {
    when (metric) {
        BodyMetric.WEIGHT -> WeightScreen(
            viewModel,
            unitFormatter,
            dateTimeFormatterProvider,
            onEditBodyMeasurement,
            onSectionEditStateChanged,
        )
        BodyMetric.HEIGHT -> HeightScreen(
            viewModel,
            unitFormatter,
            dateTimeFormatterProvider,
            onEditBodyMeasurement,
            onSectionEditStateChanged,
        )
        BodyMetric.BMI -> BmiScreen(
            viewModel,
            unitFormatter,
            dateTimeFormatterProvider,
            onEditBodyMeasurement,
            onSectionEditStateChanged,
        )
        BodyMetric.BODY_FAT -> BodyFatScreen(
            viewModel,
            unitFormatter,
            dateTimeFormatterProvider,
            onEditBodyMeasurement,
            onSectionEditStateChanged,
        )
        BodyMetric.LEAN_MASS -> LeanMassScreen(
            viewModel,
            unitFormatter,
            dateTimeFormatterProvider,
            onSectionEditStateChanged,
        )
        BodyMetric.BMR -> BmrScreen(viewModel, unitFormatter, dateTimeFormatterProvider, onSectionEditStateChanged)
        BodyMetric.BONE_MASS -> BoneMassScreen(
            viewModel,
            unitFormatter,
            dateTimeFormatterProvider,
            onSectionEditStateChanged,
        )
        BodyMetric.BODY_WATER_MASS -> BodyWaterMassScreen(
            viewModel,
            unitFormatter,
            dateTimeFormatterProvider,
            onSectionEditStateChanged,
        )
    }
}

internal fun String.toDashboardWidgetIdOrNull(): DashboardWidgetId? =
    runCatching { DashboardWidgetId.valueOf(this) }.getOrNull()

internal fun String.toBodyMeasurementTypeOrNull(): BodyMeasurementType? =
    runCatching { BodyMeasurementType.valueOf(this) }.getOrNull()

internal fun String.toVitalsMeasurementTypeOrNull(): VitalsMeasurementType? =
    runCatching { VitalsMeasurementType.valueOf(this) }.getOrNull()

internal fun DashboardWidgetId.isHeartVitalsMetric(): Boolean =
    toHeartMetricOrNull() != null

private fun DashboardWidgetId.isCaloriesDetailMetric(): Boolean =
    this == DashboardWidgetId.CALORIES_OUT ||
        this == DashboardWidgetId.ACTIVE_CALORIES ||
        this == DashboardWidgetId.BMR

private fun DashboardWidgetId.isNutritionDetailMetric(): Boolean =
    this == DashboardWidgetId.CALORIES_IN ||
        this == DashboardWidgetId.PROTEIN ||
        this == DashboardWidgetId.CARBS ||
        this == DashboardWidgetId.FAT

private fun DashboardWidgetId.isBodyDetailMetric(): Boolean =
        this == DashboardWidgetId.WEIGHT ||
        this == DashboardWidgetId.HEIGHT ||
        this == DashboardWidgetId.BMI ||
        this == DashboardWidgetId.FFMI ||
        this == DashboardWidgetId.BODY_FAT ||
        this == DashboardWidgetId.LEAN_MASS ||
        this == DashboardWidgetId.BONE_MASS ||
        this == DashboardWidgetId.BODY_WATER_MASS

private fun DashboardWidgetId.toActivityMetricOrNull(): ActivityMetric? =
    when (this) {
        DashboardWidgetId.STEPS -> ActivityMetric.STEPS
        DashboardWidgetId.DISTANCE -> ActivityMetric.DISTANCE
        DashboardWidgetId.CALORIES_OUT -> ActivityMetric.CALORIES_BURNED
        DashboardWidgetId.ACTIVE_CALORIES -> ActivityMetric.ACTIVE_CALORIES
        DashboardWidgetId.FLOORS -> ActivityMetric.FLOORS
        DashboardWidgetId.ELEVATION -> ActivityMetric.ELEVATION
        DashboardWidgetId.WHEELCHAIR_PUSHES -> ActivityMetric.WHEELCHAIR_PUSHES
        else -> null
    }

private fun DashboardWidgetId.toHeartMetricOrNull(): HeartMetric? =
    when (this) {
        DashboardWidgetId.AVG_HEART_RATE -> HeartMetric.AVERAGE_HEART_RATE
        DashboardWidgetId.RESTING_HEART_RATE -> HeartMetric.RESTING_HEART_RATE
        DashboardWidgetId.HRV -> HeartMetric.HRV
        DashboardWidgetId.BLOOD_PRESSURE -> HeartMetric.BLOOD_PRESSURE
        DashboardWidgetId.SPO2 -> HeartMetric.SPO2
        DashboardWidgetId.VO2_MAX -> HeartMetric.VO2_MAX
        DashboardWidgetId.RESPIRATORY_RATE -> HeartMetric.RESPIRATORY_RATE
        DashboardWidgetId.BODY_TEMPERATURE -> HeartMetric.BODY_TEMPERATURE
        DashboardWidgetId.BLOOD_GLUCOSE -> HeartMetric.BLOOD_GLUCOSE
        DashboardWidgetId.SKIN_TEMPERATURE -> HeartMetric.SKIN_TEMPERATURE
        else -> null
    }

private fun DashboardWidgetId.toNutritionMetricOrNull(): NutritionMetric? =
    when (this) {
        DashboardWidgetId.CALORIES_IN -> NutritionMetric.CALORIES_IN
        DashboardWidgetId.PROTEIN -> NutritionMetric.PROTEIN
        DashboardWidgetId.CARBS -> NutritionMetric.CARBS
        DashboardWidgetId.FAT -> NutritionMetric.FAT
        else -> null
    }

private fun DashboardWidgetId.toBodyMetricOrNull(): BodyMetric? =
    when (this) {
        DashboardWidgetId.WEIGHT -> BodyMetric.WEIGHT
        DashboardWidgetId.HEIGHT -> BodyMetric.HEIGHT
        DashboardWidgetId.BMI -> BodyMetric.BMI
        DashboardWidgetId.FFMI -> BodyMetric.BMI
        DashboardWidgetId.BODY_FAT -> BodyMetric.BODY_FAT
        DashboardWidgetId.LEAN_MASS -> BodyMetric.LEAN_MASS
        DashboardWidgetId.BMR -> BodyMetric.BMR
        DashboardWidgetId.BONE_MASS -> BodyMetric.BONE_MASS
        DashboardWidgetId.BODY_WATER_MASS -> BodyMetric.BODY_WATER_MASS
        else -> null
    }

internal fun HeartMetric.toDashboardWidgetId(): DashboardWidgetId =
    when (this) {
        HeartMetric.AVERAGE_HEART_RATE -> DashboardWidgetId.AVG_HEART_RATE
        HeartMetric.RESTING_HEART_RATE -> DashboardWidgetId.RESTING_HEART_RATE
        HeartMetric.HRV -> DashboardWidgetId.HRV
        HeartMetric.BLOOD_PRESSURE -> DashboardWidgetId.BLOOD_PRESSURE
        HeartMetric.SPO2 -> DashboardWidgetId.SPO2
        HeartMetric.VO2_MAX -> DashboardWidgetId.VO2_MAX
        HeartMetric.RESPIRATORY_RATE -> DashboardWidgetId.RESPIRATORY_RATE
        HeartMetric.BODY_TEMPERATURE -> DashboardWidgetId.BODY_TEMPERATURE
        HeartMetric.BLOOD_GLUCOSE -> DashboardWidgetId.BLOOD_GLUCOSE
        HeartMetric.SKIN_TEMPERATURE -> DashboardWidgetId.SKIN_TEMPERATURE
    }
