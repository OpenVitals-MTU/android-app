package tech.mmarca.openvitals.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

data class CaffeineEntry(
    val id: String,
    val startTime: Instant,
    val endTime: Instant,
    val caffeineMg: Double,
    val name: String?,
    val source: String,
    val mealType: Int,
    val clientRecordId: String? = null,
    val isOpenVitalsEntry: Boolean = false,
)

data class CaffeinePeriodData(
    val entries: List<CaffeineEntry>,
)

data class CaffeinePoint(
    val time: Instant,
    val valueMg: Double,
)

data class CaffeineEntryInsight(
    val entry: CaffeineEntry,
    val currentContributionMg: Double,
    val peakTime: Instant,
    val peakMg: Double,
    val contributionPoints: List<CaffeinePoint>,
    val inferredCategory: CaffeineSourceCategory,
    val catalogMatch: CaffeineCatalogMatch? = null,
)

data class CaffeineDailyStat(
    val date: LocalDate,
    val totalMg: Double,
    val bedtimeMg: Double,
    val safeForSleep: Boolean,
)

data class CaffeineDistributionSlice(
    val label: String,
    val valueMg: Double,
)

data class CaffeineTimeBucket(
    val bucket: CaffeineTimeOfDayBucket,
    val valueMg: Double,
)

data class CaffeineInsights(
    val currentMg: Double = 0.0,
    val todayTotalMg: Double = 0.0,
    val periodTotalMg: Double = 0.0,
    val periodAverageMg: Double = 0.0,
    val loggedDays: Int = 0,
    val peakDay: CaffeineDailyStat? = null,
    val safeNights: Int = 0,
    val totalNights: Int = 0,
    val safeSleepStreak: Int = 0,
    val bedtimeMg: Double = 0.0,
    val sleepThresholdMg: Int = 0,
    val bedtime: LocalTime = LocalTime.MIDNIGHT,
    val timeToThresholdMinutes: Long? = null,
    val curvePoints: List<CaffeinePoint> = emptyList(),
    val dailyStats: List<CaffeineDailyStat> = emptyList(),
    val entryInsights: List<CaffeineEntryInsight> = emptyList(),
    val sourceTotals: List<CaffeineDistributionSlice> = emptyList(),
    val itemTotals: List<CaffeineDistributionSlice> = emptyList(),
    val categoryTotals: List<CaffeineDistributionSlice> = emptyList(),
    val timeBuckets: List<CaffeineTimeBucket> = emptyList(),
)

enum class CaffeineSourceCategory {
    COFFEE,
    TEA,
    ENERGY_DRINK,
    SODA,
    CHOCOLATE,
    SUPPLEMENT,
    OTHER,
}

enum class CaffeineTimeOfDayBucket {
    MORNING,
    AFTERNOON,
    EVENING,
    NIGHT,
}

data class CaffeineCatalogItem(
    val id: String,
    val name: String,
    val category: CaffeineSourceCategory,
    val typicalCaffeineMg: Double,
    val defaultServingMilliliters: Double? = null,
    val aliases: List<String> = emptyList(),
)

data class CaffeineCatalogMatch(
    val item: CaffeineCatalogItem,
    val confidence: CaffeineCatalogMatchConfidence,
    val matchedText: String,
)

enum class CaffeineCatalogMatchConfidence {
    EXACT,
    ALIAS,
    CONTAINS,
}

val CaffeineSourceCategory.displayLabel: String
    get() = when (this) {
        CaffeineSourceCategory.COFFEE -> "Coffee"
        CaffeineSourceCategory.TEA -> "Tea"
        CaffeineSourceCategory.ENERGY_DRINK -> "Energy drink"
        CaffeineSourceCategory.SODA -> "Soda"
        CaffeineSourceCategory.CHOCOLATE -> "Chocolate"
        CaffeineSourceCategory.SUPPLEMENT -> "Supplement"
        CaffeineSourceCategory.OTHER -> "Other"
    }
