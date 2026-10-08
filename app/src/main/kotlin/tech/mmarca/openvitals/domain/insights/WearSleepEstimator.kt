package tech.mmarca.openvitals.domain.insights

import tech.mmarca.openvitals.domain.model.SleepMinute
import tech.mmarca.openvitals.domain.model.SleepMinuteKind
import tech.mmarca.openvitals.domain.model.WearSleepMinute

/**
 * The sleep pipeline for the rows the OpenVitals Wear OS app records:
 * the grid, the wear state, then the night. A minute the watch was not
 * worn for is a gap to everything downstream and can never be sleep; a
 * recording with too few worn minutes yields no night at all. Pure and
 * deterministic: the same rows always give the same night.
 */
object WearSleepEstimator {

    data class Config(
        val wear: WearStateDetector.Config = WearStateDetector.Config(),
        val stages: SleepEstimatorConfig = SleepEstimatorConfig(),
        /** Fewer worn minutes than this and there is nothing to estimate. */
        val minWornMinutes: Int = 180,
    )

    /** One estimated night with what the wear state said about the rows. */
    data class Night(
        val session: EstimatedSleepSession,
        val wornMinutes: Int,
        val notWornMinutes: Int,
        val notWornReasons: Map<NotWornReason, Int>,
    ) {
        /** One line for the log. */
        fun summary(): String {
            val reasons = notWornReasons.entries.sortedByDescending { it.value }.joinToString(", ") { "${it.key} ${it.value}" }
            return "${session.onset} → ${session.end}, sleep=${session.sleepMinutes}m awake=${session.awakeMinutes}m " +
                "unknown=${session.unknownMinutes}m, worn=$wornMinutes, not worn=$notWornMinutes" +
                if (reasons.isEmpty()) "" else " ($reasons)"
        }
    }

    fun estimate(rows: List<WearSleepMinute>, config: Config = Config()): Night? {
        val grid = WearMinuteGrid.of(rows) ?: return null
        val wear = WearStateDetector.detect(grid, config.wear)
        if (wear.wornMinutes < config.minWornMinutes) return null
        val minutes = ArrayList<SleepMinute>(grid.size)
        for (index in 0 until grid.size) {
            if (!grid.present[index]) continue
            val kind = when {
                !wear.isWorn(index) -> SleepMinuteKind.UNMEASURABLE
                grid.kinds[index] == SleepMinuteKind.AWAKE -> SleepMinuteKind.AWAKE
                else -> SleepMinuteKind.RAW
            }
            val bpm = grid.heartRate[index]
            minutes += SleepMinute(grid.timeAt(index), kind, grid.movement[index], if (bpm.isNaN()) null else bpm)
        }
        val session = SleepStageEstimator.estimate(minutes, config.stages) ?: return null
        return Night(session, wear.wornMinutes, wear.notWornMinutes, wear.reasonCounts)
    }
}
