package tech.mmarca.openvitals.features.body

import androidx.compose.runtime.Immutable
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import tech.mmarca.openvitals.domain.insights.BaselineValue
import tech.mmarca.openvitals.domain.model.BodyFatEntry
import tech.mmarca.openvitals.domain.model.WeightEntry

/** One point of FFMI history: a body-fat reading, with the weight nearest it in time. */
@Immutable
data class FfmiReading(
    val time: Instant,
    val source: String,
    val ffmi: Double,
    val adjustedFfmi: Double,
)

/**
 * FFMI over time, one reading per body-fat measurement.
 *
 * Body fat is what makes FFMI, so only a body-fat reading makes a point, and it takes the weight
 * nearest it in time. Carrying an old body-fat percentage onto every later weigh-in would show fat
 * lost as lean mass lost. Height is the latest on record, as BMI uses it.
 */
internal fun ffmiReadings(
    bodyFatEntries: List<BodyFatEntry>,
    weightEntries: List<WeightEntry>,
    heightCm: Double?,
): List<FfmiReading> {
    if (weightEntries.isEmpty()) return emptyList()
    return bodyFatEntries.mapNotNull { bodyFat ->
        val weight = weightEntries.minBy { Duration.between(it.time, bodyFat.time).abs() }
        val ffmi = weight.weightKg.ffmiWith(heightCm, bodyFat.percent) ?: return@mapNotNull null
        val adjustedFfmi = ffmi.adjustedFfmiWith(heightCm) ?: return@mapNotNull null
        FfmiReading(
            time = bodyFat.time,
            source = bodyFat.source,
            ffmi = ffmi,
            adjustedFfmi = adjustedFfmi,
        )
    }.sortedBy { it.time }
}

internal fun FfmiReading.ffmiBaselineValue(): BaselineValue =
    BaselineValue(
        date = time.atZone(ZoneId.systemDefault()).toLocalDate(),
        value = adjustedFfmi,
    )
