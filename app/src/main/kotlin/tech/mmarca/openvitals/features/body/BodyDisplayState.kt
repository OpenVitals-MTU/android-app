package tech.mmarca.openvitals.features.body

import androidx.compose.runtime.Immutable
import tech.mmarca.openvitals.core.period.DatePeriod
import java.time.LocalDate

@Immutable
data class BodyDisplayState(
    val selectedPeriod: DatePeriod = DatePeriod(LocalDate.now(), LocalDate.now()),
    val summary: BodySummaryDisplay = BodySummaryDisplay(),
)

@Immutable
data class BodySummaryDisplay(
    val heightCm: Double? = null,
    val leanMassKg: Double? = null,
    val bmrKcal: Double? = null,
    val boneMassKg: Double? = null,
    val bodyWaterMassKg: Double? = null,
    val latestWeightKg: Double? = null,
    val previousLatestWeightKg: Double? = null,
    val firstWeightKg: Double? = null,
    val weightChangeKg: Double? = null,
    val latestBodyFatPercent: Double? = null,
    val previousLatestBodyFatPercent: Double? = null,
    val bmi: Double? = null,
    val ffmi: Double? = null,
    val adjustedFfmi: Double? = null,
    val latestHeightCm: Double? = null,
    val previousLatestHeightCm: Double? = null,
    val latestLeanMassKg: Double? = null,
    val previousLatestLeanMassKg: Double? = null,
    val latestBmrKcal: Double? = null,
    val previousLatestBmrKcal: Double? = null,
    val latestBoneMassKg: Double? = null,
    val previousLatestBoneMassKg: Double? = null,
    val latestBodyWaterMassKg: Double? = null,
    val previousLatestBodyWaterMassKg: Double? = null,
    val previousBmi: Double? = null,
)

internal val BodyUiState.summary: BodySummaryDisplay
    get() = display.summary
