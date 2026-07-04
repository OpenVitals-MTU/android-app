package tech.mmarca.openvitals.data.repository

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import tech.mmarca.openvitals.domain.insights.BodyEnergyBucketState
import tech.mmarca.openvitals.domain.insights.BodyEnergyCalibrationMode
import tech.mmarca.openvitals.domain.insights.BodyEnergyConfidence
import tech.mmarca.openvitals.domain.insights.BodyEnergyInputSummary
import tech.mmarca.openvitals.domain.insights.BodyEnergyPrimaryInfluence
import tech.mmarca.openvitals.domain.insights.BodyEnergyTimeline
import tech.mmarca.openvitals.domain.insights.BodyEnergyTimelineBucketMinutes
import tech.mmarca.openvitals.domain.insights.BodyEnergyTimelinePoint

@Singleton
class BodyEnergyTimelineCacheStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("body_energy_timeline_cache", Context.MODE_PRIVATE)

    fun load(date: LocalDate, signature: String): BodyEnergyTimeline? {
        val encoded = prefs.getString(cacheKey(date, signature), null) ?: return null
        return encoded.toTimelineOrNull(signature)
    }

    fun save(timeline: BodyEnergyTimeline) {
        if (timeline.signature.isBlank()) return
        prefs.edit {
            putString(cacheKey(timeline.date, timeline.signature), timeline.toPreferenceString())
        }
    }

    fun loadBaseline(date: LocalDate, signature: String): BodyEnergyBaselineCacheEntry? {
        val encoded = prefs.getString(baselineCacheKey(date, signature), null) ?: return null
        return encoded.toBaselineOrNull()
    }

    fun saveBaseline(date: LocalDate, signature: String, baseline: BodyEnergyBaselineCacheEntry) {
        if (signature.isBlank()) return
        prefs.edit {
            putString(baselineCacheKey(date, signature), baseline.toPreferenceString())
        }
    }

    private fun cacheKey(date: LocalDate, signature: String): String =
        "${date}|${signature.hashCode()}"

    private fun baselineCacheKey(date: LocalDate, signature: String): String =
        "baseline|${date}|${signature.hashCode()}"
}

data class BodyEnergyBaselineCacheEntry(
    val baselineRestingHeartRateBpm: Long?,
    val observedMaxHeartRateBpm: Long?,
    val hrvBaselineRmssdMs: Double?,
    val respiratoryRateBaseline: Double?,
    val generatedAt: Instant = Instant.now(),
)

private fun BodyEnergyTimeline.toPreferenceString(): String {
    val header = listOf(
        date.toString(),
        startScore,
        currentScore,
        charged,
        drained,
        confidence.name,
        generatedAt.toEpochMilli(),
        confidenceReason.escapeCacheField(),
        inputSummary.algorithmVersion,
        inputSummary.bucketMinutes,
        inputSummary.heartRateSampleCount,
        inputSummary.hrvSampleCount,
        inputSummary.sleepSessionCount,
        inputSummary.workoutCount,
        inputSummary.respiratorySampleCount,
        inputSummary.hasRestingHeartRate,
        inputSummary.hasBaselineRestingHeartRate,
        inputSummary.hasObservedMaxHeartRate,
        inputSummary.hasHrvBaseline,
        inputSummary.hasRespiratoryBaseline,
        inputSummary.previousEndScore.cacheValue(),
        inputSummary.calibrationMode.name,
    ).joinToString("|")
    val pointsValue = points.joinToString(";") { point ->
        listOf(
            point.time.toEpochMilli(),
            point.score,
            "%.4f".format(java.util.Locale.US, point.delta),
            point.state.name,
            point.confidence.name,
            "%.4f".format(java.util.Locale.US, point.charge),
            "%.4f".format(java.util.Locale.US, point.intensityDrain),
            "%.4f".format(java.util.Locale.US, point.stressDrain),
            "%.4f".format(java.util.Locale.US, point.recoveryDebtDrain),
            point.primaryInfluence.name,
        ).joinToString(",")
    }
    return "$header\n$pointsValue"
}

private fun String.toTimelineOrNull(signature: String): BodyEnergyTimeline? =
    runCatching {
        val lines = split("\n", limit = 2)
        val header = lines[0].split("|")
        val points = lines.getOrNull(1)
            .orEmpty()
            .split(";")
            .filter { it.isNotBlank() }
            .mapNotNull { encoded ->
                val parts = encoded.split(",")
                if (parts.size < 5) return@mapNotNull null
                val state = BodyEnergyBucketState.valueOf(parts[3])
                val delta = parts[2].toDouble()
                BodyEnergyTimelinePoint(
                    time = Instant.ofEpochMilli(parts[0].toLong()),
                    score = parts[1].toInt(),
                    delta = delta,
                    state = state,
                    confidence = BodyEnergyConfidence.valueOf(parts[4]),
                    charge = parts.getOrNull(5)?.toDoubleOrNull() ?: delta.coerceAtLeast(0.0),
                    intensityDrain = parts.getOrNull(6)?.toDoubleOrNull() ?: 0.0,
                    stressDrain = parts.getOrNull(7)?.toDoubleOrNull() ?: 0.0,
                    recoveryDebtDrain = parts.getOrNull(8)?.toDoubleOrNull() ?: 0.0,
                    primaryInfluence = parts.getOrNull(9)
                        ?.let { runCatching { BodyEnergyPrimaryInfluence.valueOf(it) }.getOrNull() }
                        ?: state.legacyPrimaryInfluence(delta),
                )
            }
        BodyEnergyTimeline(
            date = LocalDate.parse(header[0]),
            startScore = header[1].toInt(),
            currentScore = header[2].toInt(),
            charged = header[3].toInt(),
            drained = header[4].toInt(),
            confidence = BodyEnergyConfidence.valueOf(header[5]),
            generatedAt = Instant.ofEpochMilli(header[6].toLong()),
            confidenceReason = header.getOrNull(7).orEmpty().unescapeCacheField(),
            inputSummary = header.toInputSummary(),
            points = points,
            signature = signature,
        )
    }.getOrNull()

private fun List<String>.toInputSummary(): BodyEnergyInputSummary =
    BodyEnergyInputSummary(
        algorithmVersion = getOrNull(8)?.toIntOrNull() ?: 1,
        bucketMinutes = getOrNull(9)?.toLongOrNull() ?: BodyEnergyTimelineBucketMinutes,
        heartRateSampleCount = getOrNull(10)?.toIntOrNull() ?: 0,
        hrvSampleCount = getOrNull(11)?.toIntOrNull() ?: 0,
        sleepSessionCount = getOrNull(12)?.toIntOrNull() ?: 0,
        workoutCount = getOrNull(13)?.toIntOrNull() ?: 0,
        respiratorySampleCount = getOrNull(14)?.toIntOrNull() ?: 0,
        hasRestingHeartRate = getOrNull(15)?.toBoolean() ?: false,
        hasBaselineRestingHeartRate = getOrNull(16)?.toBoolean() ?: false,
        hasObservedMaxHeartRate = getOrNull(17)?.toBoolean() ?: false,
        hasHrvBaseline = getOrNull(18)?.toBoolean() ?: false,
        hasRespiratoryBaseline = getOrNull(19)?.toBoolean() ?: false,
        previousEndScore = getOrNull(20).toIntOrNullCache(),
        calibrationMode = getOrNull(21)
            ?.let { runCatching { BodyEnergyCalibrationMode.valueOf(it) }.getOrNull() }
            ?: BodyEnergyCalibrationMode.AUTOMATIC,
    )

private fun BodyEnergyBucketState.legacyPrimaryInfluence(delta: Double): BodyEnergyPrimaryInfluence =
    when {
        this == BodyEnergyBucketState.UNMEASURABLE -> BodyEnergyPrimaryInfluence.NO_DATA
        delta > 0.0 && this == BodyEnergyBucketState.SLEEP -> BodyEnergyPrimaryInfluence.SLEEP_RECOVERY
        delta > 0.0 -> BodyEnergyPrimaryInfluence.QUIET_REST
        this == BodyEnergyBucketState.ACTIVITY -> BodyEnergyPrimaryInfluence.EXERTION
        this == BodyEnergyBucketState.STRESS -> BodyEnergyPrimaryInfluence.ELEVATED_HEART_RATE
        else -> BodyEnergyPrimaryInfluence.STEADY
    }

private fun BodyEnergyBaselineCacheEntry.toPreferenceString(): String =
    listOf(
        baselineRestingHeartRateBpm.cacheValue(),
        observedMaxHeartRateBpm.cacheValue(),
        hrvBaselineRmssdMs.cacheValue(),
        respiratoryRateBaseline.cacheValue(),
        generatedAt.toEpochMilli().toString(),
    ).joinToString("|")

private fun String.toBaselineOrNull(): BodyEnergyBaselineCacheEntry? =
    runCatching {
        val parts = split("|")
        BodyEnergyBaselineCacheEntry(
            baselineRestingHeartRateBpm = parts.getOrNull(0).toLongOrNullCache(),
            observedMaxHeartRateBpm = parts.getOrNull(1).toLongOrNullCache(),
            hrvBaselineRmssdMs = parts.getOrNull(2).toDoubleOrNullCache(),
            respiratoryRateBaseline = parts.getOrNull(3).toDoubleOrNullCache(),
            generatedAt = Instant.ofEpochMilli(parts.getOrNull(4)?.toLong() ?: 0L),
        )
    }.getOrNull()

private fun Long?.cacheValue(): String = this?.toString().orEmpty()

private fun Int?.cacheValue(): String = this?.toString().orEmpty()

private fun Double?.cacheValue(): String = this?.toString().orEmpty()

private fun String?.toLongOrNullCache(): Long? =
    takeUnless { it.isNullOrBlank() }?.toLongOrNull()

private fun String?.toDoubleOrNullCache(): Double? =
    takeUnless { it.isNullOrBlank() }?.toDoubleOrNull()

private fun String?.toIntOrNullCache(): Int? =
    takeUnless { it.isNullOrBlank() }?.toIntOrNull()

private fun String.escapeCacheField(): String =
    replace("\\", "\\\\")
        .replace("|", "\\p")
        .replace("\n", "\\n")

private fun String.unescapeCacheField(): String =
    replace("\\n", "\n")
        .replace("\\p", "|")
        .replace("\\\\", "\\")
