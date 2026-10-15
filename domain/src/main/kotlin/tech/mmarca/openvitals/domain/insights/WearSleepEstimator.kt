package tech.mmarca.openvitals.domain.insights

import tech.mmarca.openvitals.domain.model.SleepMinute
import tech.mmarca.openvitals.domain.model.SleepMinuteKind
import tech.mmarca.openvitals.domain.model.WearSleepMinute

/**
 * The sleep pipeline for the rows the OpenVitals Wear OS app records, in
 * the order the actigraphy literature settled on: the grid, the wear state
 * (a minute the watch was not worn for is a gap to everything after it and
 * can never be sleep), the night's window, sleep or wake per minute inside
 * it, then the stages. Pure and deterministic: the same rows always give
 * the same night.
 */
object WearSleepEstimator {

    data class Config(
        val wear: WearStateDetector.Config = WearStateDetector.Config(),
        val window: SptWindowDetector.Config = SptWindowDetector.Config(),
        val scorer: SleepWakeScorer.Config = SleepWakeScorer.Config(),
        /** LIDS joins the staging: the Wear OS count is about a fifth of Garmin's, hence the scale. */
        val stages: SleepEstimatorConfig = SleepEstimatorConfig(lidsWeight = 0.3f, lidsActivityScale = 0.2f),
    )

    /** One estimated night with what the wear state and the window said about the rows. */
    data class Night(
        val session: EstimatedSleepSession,
        val window: SptWindowDetector.SptWindow,
        val wornMinutes: Int,
        val notWornMinutes: Int,
        val notWornReasons: Map<NotWornReason, Int>,
    ) {
        /** One line for the log. */
        fun summary(): String {
            val reasons = notWornReasons.entries.sortedByDescending { it.value }.joinToString(", ") { "${it.key} ${it.value}" }
            return "${session.onset} → ${session.end}, sleep=${session.sleepMinutes}m awake=${session.awakeMinutes}m " +
                "unknown=${session.unknownMinutes}m, window ${window.length}m by ${window.source}, " +
                "worn=$wornMinutes, not worn=$notWornMinutes" + if (reasons.isEmpty()) "" else " ($reasons)"
        }
    }

    fun estimate(rows: List<WearSleepMinute>, config: Config = Config()): Night? {
        val grid = WearMinuteGrid.of(rows) ?: return null
        val wear = WearStateDetector.detect(grid, config.wear)
        if (wear.wornMinutes < config.window.minWornMinutes) return null
        val window = SptWindowDetector.detect(grid, wear, config.window) ?: return null
        val labels = SleepWakeScorer.score(grid, wear, config.scorer)
        val minutes = List(grid.size) { index ->
            val kind = when {
                !wear.isWorn(index) -> SleepMinuteKind.UNMEASURABLE
                grid.kinds[index] == SleepMinuteKind.AWAKE -> SleepMinuteKind.AWAKE
                else -> SleepMinuteKind.RAW
            }
            val bpm = grid.heartRate[index]
            SleepMinute(grid.timeAt(index), kind, grid.movement[index], if (bpm.isNaN()) null else bpm)
        }
        val session = SleepStageEstimator.estimateWindow(minutes, labels, window.onset, window.end, config.stages) ?: return null
        return Night(session, window, wear.wornMinutes, wear.notWornMinutes, wear.reasonCounts)
    }
}
