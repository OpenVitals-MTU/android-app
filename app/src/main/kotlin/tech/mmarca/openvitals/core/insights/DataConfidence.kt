package tech.mmarca.openvitals.core.insights

import tech.mmarca.openvitals.core.period.DatePeriod
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

enum class DataConfidenceLevel {
    HIGH,
    MEDIUM,
    LOW,
}

enum class DataSourceConsistency {
    NOT_AVAILABLE,
    SINGLE_SOURCE,
    MIXED_SOURCES,
}

enum class DataValueKind {
    MEASURED,
    AGGREGATED,
    CALCULATED,
    ESTIMATED,
    MIXED,
}

enum class DataConfidenceWarning {
    LOW_COVERAGE,
    SPARSE_DATA,
    MIXED_SOURCES,
    MANUAL_ENTRIES,
    CALCULATED_VALUE,
    NO_SOURCE_DETAILS,
}

data class DataConfidence(
    val level: DataConfidenceLevel,
    val expectedDays: Int,
    val trackedDays: Int,
    val sampleCount: Int,
    val coveragePercent: Int,
    val sources: List<String>,
    val sourceConsistency: DataSourceConsistency,
    val valueKind: DataValueKind,
    val manualEntryCount: Int,
    val warnings: List<DataConfidenceWarning>,
)

fun dataConfidence(
    period: DatePeriod,
    trackedDates: Collection<LocalDate>,
    sampleCount: Int,
    sources: Collection<String> = emptyList(),
    valueKind: DataValueKind = DataValueKind.MEASURED,
    manualEntryCount: Int = 0,
    minSamplesForTrend: Int = 3,
): DataConfidence {
    val expectedDays = ChronoUnit.DAYS.between(period.start, period.end).toInt().plus(1).coerceAtLeast(1)
    val trackedDays = trackedDates
        .asSequence()
        .filter { date -> !date.isBefore(period.start) && !date.isAfter(period.end) }
        .distinct()
        .count()
    val coveragePercent = ((trackedDays.toDouble() / expectedDays.toDouble()) * 100.0).roundToInt()
    val normalizedSources = sources
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .sorted()
    val sourceConsistency = when (normalizedSources.size) {
        0 -> DataSourceConsistency.NOT_AVAILABLE
        1 -> DataSourceConsistency.SINGLE_SOURCE
        else -> DataSourceConsistency.MIXED_SOURCES
    }
    val sparseData = sampleCount in 1 until minSamplesForTrend ||
        (expectedDays > 1 && trackedDays in 1 until minOf(minSamplesForTrend, expectedDays))
    val lowCoverage = expectedDays > 1 && coveragePercent < 60
    val warnings = buildList {
        if (lowCoverage) add(DataConfidenceWarning.LOW_COVERAGE)
        if (sparseData) add(DataConfidenceWarning.SPARSE_DATA)
        if (sourceConsistency == DataSourceConsistency.MIXED_SOURCES) add(DataConfidenceWarning.MIXED_SOURCES)
        if (manualEntryCount > 0) add(DataConfidenceWarning.MANUAL_ENTRIES)
        if (valueKind == DataValueKind.CALCULATED || valueKind == DataValueKind.ESTIMATED) {
            add(DataConfidenceWarning.CALCULATED_VALUE)
        }
        if (sourceConsistency == DataSourceConsistency.NOT_AVAILABLE) add(DataConfidenceWarning.NO_SOURCE_DETAILS)
    }
    val level = when {
        sampleCount <= 0 -> DataConfidenceLevel.LOW
        expectedDays > 1 && coveragePercent < 25 -> DataConfidenceLevel.LOW
        sparseData -> DataConfidenceLevel.LOW
        warnings.isNotEmpty() -> DataConfidenceLevel.MEDIUM
        else -> DataConfidenceLevel.HIGH
    }

    return DataConfidence(
        level = level,
        expectedDays = expectedDays,
        trackedDays = trackedDays,
        sampleCount = sampleCount.coerceAtLeast(0),
        coveragePercent = coveragePercent,
        sources = normalizedSources,
        sourceConsistency = sourceConsistency,
        valueKind = valueKind,
        manualEntryCount = manualEntryCount.coerceAtLeast(0),
        warnings = warnings,
    )
}
