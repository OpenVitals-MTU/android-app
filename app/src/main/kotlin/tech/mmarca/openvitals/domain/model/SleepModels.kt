package tech.mmarca.openvitals.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

data class SleepData(
    val id: String,
    val startTime: Instant,
    val endTime: Instant,
    val durationMs: Long,
    val source: String,
    val title: String? = null,
    val notes: String? = null,
    val startZoneOffset: ZoneOffset? = null,
    val endZoneOffset: ZoneOffset? = null,
    val lastModifiedTime: Instant? = null,
    val clientRecordId: String? = null,
    val clientRecordVersion: Long? = null,
    val recordingMethod: Int? = null,
    val device: SleepDeviceData? = null,
    val stages: List<SleepStage> = emptyList(),
) {
    val durationHours: Double get() = durationMs / 3_600_000.0
}

data class SleepDeviceData(
    val type: Int,
    val manufacturer: String?,
    val model: String?,
)

data class SleepStage(
    val startTime: Instant,
    val endTime: Instant,
    val stageType: Int,
) {
    val durationMs: Long get() = endTime.toEpochMilli() - startTime.toEpochMilli()

    companion object {
        const val STAGE_AWAKE = 1
        const val STAGE_SLEEPING = 2
        const val STAGE_OUT_OF_BED = 3
        const val STAGE_LIGHT = 4
        const val STAGE_DEEP = 5
        const val STAGE_REM = 6
        const val STAGE_AWAKE_IN_BED = 7
    }
}

data class DailySleepDuration(
    val date: LocalDate,
    val durationMs: Long,
) {
    val durationHours: Double get() = durationMs / 3_600_000.0
}

data class SleepReadData(
    val sessions: List<SleepData> = emptyList(),
    val dailyAggregateDurations: List<DailySleepDuration> = emptyList(),
)

internal fun sleepDurationMsFromStages(
    stages: List<SleepStage>,
    fallbackDurationMs: Long,
): Long {
    if (stages.isEmpty()) return fallbackDurationMs.coerceAtLeast(0L)

    val sleepStageDurationMs = stages
        .filter { it.stageType.isSleepDurationStage() }
        .sumOf { it.durationMs.coerceAtLeast(0L) }

    return sleepStageDurationMs.takeIf { it > 0L } ?: fallbackDurationMs.coerceAtLeast(0L)
}

private fun Int.isSleepDurationStage(): Boolean = when (this) {
    SleepStage.STAGE_SLEEPING,
    SleepStage.STAGE_LIGHT,
    SleepStage.STAGE_DEEP,
    SleepStage.STAGE_REM -> true
    else -> false
}
