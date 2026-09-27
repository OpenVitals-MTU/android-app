package tech.mmarca.openvitals.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import tech.mmarca.openvitals.domain.cycle.CycleSymptom

/** A pregnancy test result. Health Connect has no record type for it. */
enum class HcgTestResult {
    NEGATIVE,
    POSITIVE,
    FAINT_UNCERTAIN,
}

/** Why a morning temperature should not count toward a thermal shift. */
enum class BbtDisturbance {
    FEVER,
    ALCOHOL,
    POOR_SLEEP,
    TIME_SHIFT,
    LATE_MEASUREMENT,
    STRESS,
    MEDICATION,
}

/** Vulvar sensation, the half of a cervical fluid observation Health Connect does not hold. */
enum class CervicalSensation {
    DRY,
    DAMP,
    WET,
    SLIPPERY,
}

/**
 * The subjective half of a day's cycle observations. Health Connect has no
 * record type for any of these fields, so they live in Room. Bleeding, tests
 * and temperatures stay in Health Connect.
 */
data class CycleJournalEntry(
    val date: LocalDate,
    /** The user said there was no bleeding. Distinct from nothing recorded. */
    val bleedingNone: Boolean = false,
    val painLevel: Int? = null,
    val moodLevel: Int? = null,
    val energyLevel: Int? = null,
    val symptoms: Set<CycleSymptom> = emptySet(),
    val notes: String = "",
    val hcgTest: HcgTestResult? = null,
    val bbtDisturbances: Set<BbtDisturbance> = emptySet(),
    val cervicalSensation: CervicalSensation? = null,
    val updatedAt: Instant = Instant.EPOCH,
) {
    init {
        require(painLevel == null || painLevel in TRACKING_SCALE) { "pain out of scale" }
        require(moodLevel == null || moodLevel in TRACKING_SCALE) { "mood out of scale" }
        require(energyLevel == null || energyLevel in TRACKING_SCALE) { "energy out of scale" }
    }

    /** Whether anything at all was recorded. An empty entry is deleted, not stored. */
    val hasObservations: Boolean
        get() = bleedingNone || painLevel != null || moodLevel != null || energyLevel != null ||
            symptoms.isNotEmpty() || notes.isNotBlank() || hcgTest != null ||
            bbtDisturbances.isNotEmpty() || cervicalSensation != null

    companion object {
        val TRACKING_SCALE = 1..5
    }
}

/** What a cycle reminder may say on a screen that is not locked. */
enum class CycleReminderVisibility {
    /** Neutral copy that reveals nothing. The default. */
    CONCEALED,
    DESCRIPTIVE,
    CUSTOM,
}

/** The three cycle reminders. Everything is off by default. */
data class CycleReminderConfig(
    val enabled: Boolean = false,
    val dailyCheckInEnabled: Boolean = false,
    val dailyCheckInTime: LocalTime = DefaultCheckInTime,
    val periodWindowEnabled: Boolean = false,
    val periodWindowLeadDays: Int = 2,
    val lateCycleEnabled: Boolean = false,
    val lateCycleGraceDays: Int = 1,
    val visibility: CycleReminderVisibility = CycleReminderVisibility.CONCEALED,
    val customTitle: String = "",
    val customBody: String = "",
) {
    fun normalized(): CycleReminderConfig = copy(
        periodWindowLeadDays = periodWindowLeadDays.coerceIn(LeadDaysRange),
        lateCycleGraceDays = lateCycleGraceDays.coerceIn(GraceDaysRange),
        customTitle = customTitle.trim().take(MaxCustomLength),
        customBody = customBody.trim().take(MaxCustomLength),
    )

    companion object {
        val DefaultCheckInTime: LocalTime = LocalTime.of(21, 0)
        val LeadDaysRange = 1..3
        val GraceDaysRange = 0..7
        const val MaxCustomLength = 120
    }
}
