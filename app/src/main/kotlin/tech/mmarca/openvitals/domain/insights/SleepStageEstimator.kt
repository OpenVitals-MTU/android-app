package tech.mmarca.openvitals.domain.insights

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.sqrt
import tech.mmarca.openvitals.domain.model.SleepMinute
import tech.mmarca.openvitals.domain.model.SleepMinuteKind

/** A stage the estimator assigns to a minute. UNKNOWN is a not-worn gap inside the night. */
enum class EstimatedStage { AWAKE, LIGHT, DEEP, REM, UNKNOWN }

/** `[start, end)` spent at [stage]. */
data class EstimatedStageSpan(
    val start: Instant,
    val end: Instant,
    val stage: EstimatedStage,
)

/** One estimated night. [onset] is sleep onset, not bed time. */
data class EstimatedSleepSession(
    val onset: Instant,
    val end: Instant,
    /** True until 45 minutes of not sleeping have been seen after [end]. */
    val inProgress: Boolean,
    val stages: List<EstimatedStageSpan>,
    val sleepMinutes: Int,
    val awakeMinutes: Int,
    val unknownMinutes: Int,
    /** The deep and REM score thresholds after the sanity loop. 0.6 when it never fired. */
    val deepThreshold: Float,
    val remThreshold: Float,
)

/**
 * Every constant of the estimator. The defaults were fitted on three nights
 * of one wearer's Venu SQ; a test can tighten or loosen them.
 */
data class SleepEstimatorConfig(
    /** Activity assigned to a minute the watch itself called awake. */
    val awakeActivity: Float = 10f,
    /** Weighted activity at or above which a raw minute counts as awake. */
    val sleepThreshold: Float = 8f,
    /** A run of not sleeping this long splits candidate blocks and ends a night. */
    val maxBreakMinutes: Int = 45,
    /** Sleep onset is the first sleep run this long; the end is the last one. */
    val onsetRunMinutes: Int = 10,
    val endRunMinutes: Int = 10,
    val minWindowMinutes: Int = 180,
    val minSleepMinutes: Int = 150,
    val heartRateSmoothHalf: Int = 2,
    val baselineHalf: Int = 45,
    val deviationHalf: Int = 3,
    val movementHalf: Int = 2,
    /** A wake run this long resets the post-wake light-sleep rule. */
    val wakeRunMinutes: Int = 3,
    val remLatencyMinutes: Int = 60,
    val postWakeLightMinutes: Int = 10,
    val deviationScale: Float = 2f,
    val variabilityWeight: Float = 0.5f,
    val deepMovementPenalty: Float = 0.6f,
    val remMovementPenalty: Float = 0.4f,
    val deepPriorStart: Float = 0.5f,
    val deepPriorSlope: Float = -1.0f,
    val remPriorStart: Float = -0.6f,
    val remPriorSlope: Float = 1.2f,
    val deepThreshold: Float = 0.6f,
    val remThreshold: Float = 0.6f,
    val minSegmentMinutes: Int = 5,
    val bridgeMinutes: Int = 2,
    val deepShareMin: Float = 0.10f,
    val deepShareMax: Float = 0.25f,
    val remShareMin: Float = 0.15f,
    val remShareMax: Float = 0.30f,
    val adaptStep: Float = 0.1f,
    val adaptMaxIterations: Int = 8,
)

/**
 * Estimates one night of sleep from per-minute movement and heart rate, for
 * watches that leave staging to the vendor's servers. Sleep/wake follows
 * actigraphy practice (Cole-Kripke weights, Webster rescoring); stages
 * follow heart rate against its own slow baseline, its variability, and
 * time-of-night priors. Pure and deterministic: the same minutes always
 * give the same night, so a night can be re-estimated as files arrive.
 *
 * This is a heuristic without ground truth. Read the stages as "when heart
 * rate was low and steady versus high and variable", not as a sleep lab.
 */
object SleepStageEstimator {

    /**
     * The night a minute belongs to: minutes from 18:00 count toward the next
     * date, minutes before 14:00 toward their own. The afternoon belongs to none.
     */
    fun nightDateOf(time: Instant, zone: ZoneOffset): LocalDate? {
        val local = time.atOffset(zone).toLocalDateTime()
        return when {
            local.hour >= NightStartHour -> local.toLocalDate().plusDays(1)
            local.hour < NightEndHour -> local.toLocalDate()
            else -> null
        }
    }

    /** The night in [minutes], or null when no block qualifies. */
    fun estimate(
        minutes: List<SleepMinute>,
        config: SleepEstimatorConfig = SleepEstimatorConfig(),
    ): EstimatedSleepSession? {
        val grid = MinuteGrid.of(minutes) ?: return null
        val labels = scoreSleepWake(grid, config)
        val block = findBlocks(labels, config)
            .sortedWith(compareByDescending<SleepBlock> { it.sleepCount }.thenBy { it.onset })
            .firstOrNull() ?: return null
        val staged = classifyStages(grid, labels, block.onset, block.end, config)
        val stages = staged.stages
        val spans = mutableListOf<EstimatedStageSpan>()
        var runStart = 0
        for (index in 1..stages.size) {
            if (index == stages.size || stages[index] != stages[runStart]) {
                spans.add(
                    EstimatedStageSpan(
                        start = grid.timeAt(block.onset + runStart),
                        end = grid.timeAt(block.onset + index),
                        stage = stages[runStart],
                    ),
                )
                runStart = index
            }
        }
        return EstimatedSleepSession(
            onset = grid.timeAt(block.onset),
            end = grid.timeAt(block.end),
            inProgress = grid.size - block.end < config.maxBreakMinutes,
            stages = spans,
            sleepMinutes = stages.count { it == EstimatedStage.LIGHT || it == EstimatedStage.DEEP || it == EstimatedStage.REM },
            awakeMinutes = stages.count { it == EstimatedStage.AWAKE },
            unknownMinutes = stages.count { it == EstimatedStage.UNKNOWN },
            deepThreshold = staged.deepThreshold,
            remThreshold = staged.remThreshold,
        )
    }

    /** Sleep (S), wake (W) or gap (G) per minute, after Webster rescoring. */
    internal fun scoreSleepWake(grid: MinuteGrid, config: SleepEstimatorConfig): CharArray {
        val activity = grid.activity(config)
        val labels = CharArray(grid.size)
        for (index in 0 until grid.size) {
            labels[index] = when (grid.kinds[index]) {
                SleepMinuteKind.UNMEASURABLE -> Gap
                SleepMinuteKind.AWAKE -> Wake
                SleepMinuteKind.RAW -> {
                    var weighted = 0f
                    for (offset in ColeKripkeWeights.indices) {
                        val at = index + offset - ColeKripkeCenter
                        if (at in 0 until grid.size) weighted += ColeKripkeWeights[offset] * activity[at]
                    }
                    if (weighted < config.sleepThreshold) Sleep else Wake
                }
            }
        }
        // Passes repeat until stable: a wake run that grew carries over again.
        // The corpus constants were fitted with this, so it is not a single pass.
        repeat(WebsterPasses) {
            val carried = websterCarryOver(labels)
            val bridged = websterBridge(labels)
            if (!carried && !bridged) return labels
        }
        return labels
    }

    /** Webster rule 1: the first sleep minutes after a wake run are still wake. Returns whether anything changed. */
    private fun websterCarryOver(labels: CharArray): Boolean {
        var changed = false
        val runs = runsOf(labels)
        for ((position, run) in runs.withIndex()) {
            if (run.label != Wake) continue
            val next = runs.getOrNull(position + 1) ?: continue
            if (next.label != Sleep) continue
            val toWake = when {
                run.length >= 15 -> 4
                run.length >= 10 -> 3
                run.length >= 4 -> 1
                else -> 0
            }
            for (index in next.start until next.start + minOf(toWake, next.length)) {
                labels[index] = Wake
                changed = true
            }
        }
        return changed
    }

    /** Webster rules 2 and 3: a short sleep run between long wake runs is wake. Returns whether anything changed. */
    private fun websterBridge(labels: CharArray): Boolean {
        var changed = false
        val rescanned = runsOf(labels)
        for ((position, run) in rescanned.withIndex()) {
            if (run.label != Sleep) continue
            val before = rescanned.getOrNull(position - 1) ?: continue
            val after = rescanned.getOrNull(position + 1) ?: continue
            if (before.label != Wake || after.label != Wake) continue
            val bounded = minOf(before.length, after.length)
            if ((run.length <= 6 && bounded >= 10) || (run.length <= 10 && bounded >= 20)) {
                for (index in run.start until run.start + run.length) labels[index] = Wake
                changed = true
            }
        }
        return changed
    }

    /** Candidate sleep blocks: spans between long breaks that hold a real night. */
    internal fun findBlocks(labels: CharArray, config: SleepEstimatorConfig): List<SleepBlock> {
        val blocks = mutableListOf<SleepBlock>()
        var spanStart = 0
        var index = 0
        while (index <= labels.size) {
            // A break is a run of not sleeping (wake or gap mixed) at least maxBreak long.
            val breakStart = index
            while (index < labels.size && labels[index] != Sleep) index++
            val breakLength = index - breakStart
            if (breakLength >= config.maxBreakMinutes || index == labels.size) {
                blockOf(labels, spanStart, breakStart, config)?.let(blocks::add)
                spanStart = index
            }
            if (index == labels.size) break
            index++
        }
        return blocks
    }

    private fun blockOf(labels: CharArray, from: Int, to: Int, config: SleepEstimatorConfig): SleepBlock? {
        if (to <= from) return null
        val runs = runsOf(labels, from, to).filter { it.label == Sleep }
        val onsetRun = runs.firstOrNull { it.length >= config.onsetRunMinutes } ?: return null
        val endRun = runs.lastOrNull { it.length >= config.endRunMinutes } ?: return null
        val onset = onsetRun.start
        val end = endRun.start + endRun.length
        if (end - onset < config.minWindowMinutes) return null
        var sleepCount = 0
        for (index in onset until end) if (labels[index] == Sleep) sleepCount++
        if (sleepCount < config.minSleepMinutes) return null
        return SleepBlock(onset = onset, end = end, sleepCount = sleepCount)
    }

    /** Stages for the window `[onset, end)`, with the thresholds the sanity loop settled on. */
    internal fun classifyStages(
        grid: MinuteGrid,
        labels: CharArray,
        onset: Int,
        end: Int,
        config: SleepEstimatorConfig,
    ): StagedWindow {
        val length = end - onset
        val features = StageFeatures.of(grid, labels, onset, end, config)
        var deepThreshold = config.deepThreshold
        var remThreshold = config.remThreshold
        var stages = decide(features, labels, onset, length, deepThreshold, remThreshold, config)
        for (iteration in 0 until config.adaptMaxIterations) {
            val sleeping = stages.count { it == EstimatedStage.LIGHT || it == EstimatedStage.DEEP || it == EstimatedStage.REM }
            if (sleeping == 0) break
            val deepShare = stages.count { it == EstimatedStage.DEEP }.toFloat() / sleeping
            val remShare = stages.count { it == EstimatedStage.REM }.toFloat() / sleeping
            var moved = false
            if (deepShare < config.deepShareMin) {
                deepThreshold -= config.adaptStep
                moved = true
            } else if (deepShare > config.deepShareMax) {
                deepThreshold += config.adaptStep
                moved = true
            }
            if (remShare < config.remShareMin) {
                remThreshold -= config.adaptStep
                moved = true
            } else if (remShare > config.remShareMax) {
                remThreshold += config.adaptStep
                moved = true
            }
            if (!moved) break
            stages = decide(features, labels, onset, length, deepThreshold, remThreshold, config)
        }
        return StagedWindow(stages, deepThreshold, remThreshold)
    }

    private fun decide(
        features: StageFeatures,
        labels: CharArray,
        onset: Int,
        length: Int,
        deepThreshold: Float,
        remThreshold: Float,
        config: SleepEstimatorConfig,
    ): Array<EstimatedStage> {
        val stages = Array(length) { EstimatedStage.LIGHT }
        val variabilityScale = maxOf(features.variabilityMedian, 1f)
        for (index in 0 until length) {
            when (labels[onset + index]) {
                Gap -> {
                    stages[index] = EstimatedStage.UNKNOWN
                    continue
                }
                Wake -> {
                    stages[index] = EstimatedStage.AWAKE
                    continue
                }
            }
            val position = index.toFloat() / length
            val deviation = features.deviation[index] / config.deviationScale
            val variability = (features.variability[index] - features.variabilityMedian) / variabilityScale
            val moving = features.movement[index] > 0f
            var deep = -deviation - config.variabilityWeight * variability +
                (config.deepPriorStart + config.deepPriorSlope * position)
            if (moving) deep -= config.deepMovementPenalty
            var rem = deviation + config.variabilityWeight * variability +
                (config.remPriorStart + config.remPriorSlope * position)
            if (moving) rem -= config.remMovementPenalty
            if (index < config.remLatencyMinutes) rem = Float.NEGATIVE_INFINITY
            if (features.sinceWake[index] < config.postWakeLightMinutes) deep = Float.NEGATIVE_INFINITY
            stages[index] = when {
                deep > deepThreshold && deep >= rem -> EstimatedStage.DEEP
                rem > remThreshold -> EstimatedStage.REM
                else -> EstimatedStage.LIGHT
            }
        }
        smooth(stages, config)
        return stages
    }

    /** Short deep/REM runs become light; a light sliver between two runs of one stage joins them. */
    private fun smooth(stages: Array<EstimatedStage>, config: SleepEstimatorConfig) {
        for (run in stageRuns(stages)) {
            if ((run.stage == EstimatedStage.DEEP || run.stage == EstimatedStage.REM) &&
                run.length < config.minSegmentMinutes
            ) {
                for (index in run.start until run.start + run.length) stages[index] = EstimatedStage.LIGHT
            }
        }
        val runs = stageRuns(stages)
        for ((position, run) in runs.withIndex()) {
            if (run.stage != EstimatedStage.LIGHT || run.length > config.bridgeMinutes) continue
            val before = runs.getOrNull(position - 1) ?: continue
            val after = runs.getOrNull(position + 1) ?: continue
            if (before.stage == after.stage && (before.stage == EstimatedStage.DEEP || before.stage == EstimatedStage.REM)) {
                for (index in run.start until run.start + run.length) stages[index] = before.stage
            }
        }
    }

    private class StageRun(val stage: EstimatedStage, val start: Int, val length: Int)

    private fun stageRuns(stages: Array<EstimatedStage>): List<StageRun> {
        val runs = mutableListOf<StageRun>()
        var start = 0
        for (index in 1..stages.size) {
            if (index == stages.size || stages[index] != stages[start]) {
                runs.add(StageRun(stages[start], start, index - start))
                start = index
            }
        }
        return runs
    }

    internal class LabelRun(val label: Char, val start: Int, val length: Int)

    internal fun runsOf(labels: CharArray, from: Int = 0, to: Int = labels.size): List<LabelRun> {
        val runs = mutableListOf<LabelRun>()
        var start = from
        for (index in from + 1..to) {
            if (index == to || labels[index] != labels[start]) {
                runs.add(LabelRun(labels[start], start, index - start))
                start = index
            }
        }
        return runs
    }

    internal const val Sleep = 'S'
    internal const val Wake = 'W'
    internal const val Gap = 'G'

    private const val NightStartHour = 18
    private const val NightEndHour = 14
    private const val WebsterPasses = 3

    /** Cole-Kripke one-minute weights for t-4..t+2, scaled so the centre is 1. */
    private val ColeKripkeWeights = floatArrayOf(0.29f, 0.42f, 0.23f, 0.31f, 1.00f, 0.36f, 0.25f)
    private const val ColeKripkeCenter = 4
}

/** A candidate night: window indices into the grid and how many minutes scored as sleep. */
internal class SleepBlock(val onset: Int, val end: Int, val sleepCount: Int)

internal class StagedWindow(
    val stages: Array<EstimatedStage>,
    val deepThreshold: Float,
    val remThreshold: Float,
)

/**
 * The input on a one-minute grid: sorted, duplicates resolved (last wins),
 * missing minutes filled as not worn so a partial sync behaves like a gap.
 */
internal class MinuteGrid private constructor(
    private val startMinute: Long,
    val kinds: Array<SleepMinuteKind>,
    val movement: FloatArray,
    /** Beats per minute, or NaN when the minute carried none. */
    val heartRate: FloatArray,
) {
    val size: Int get() = kinds.size

    fun timeAt(index: Int): Instant = Instant.ofEpochSecond((startMinute + index) * SecondsPerMinute)

    /** Movement for scoring: the watch's own awake verdict counts as strong movement. */
    fun activity(config: SleepEstimatorConfig): FloatArray = FloatArray(size) { index ->
        when (kinds[index]) {
            SleepMinuteKind.RAW -> movement[index]
            SleepMinuteKind.AWAKE -> config.awakeActivity
            SleepMinuteKind.UNMEASURABLE -> 0f
        }
    }

    companion object {
        fun of(minutes: List<SleepMinute>): MinuteGrid? {
            if (minutes.isEmpty()) return null
            val byMinute = LinkedHashMap<Long, SleepMinute>()
            for (minute in minutes.sortedBy { it.time }) {
                byMinute[minute.time.epochSecond / SecondsPerMinute] = minute
            }
            val first = byMinute.keys.min()
            val last = byMinute.keys.max()
            val size = (last - first + 1).toInt()
            val kinds = Array(size) { SleepMinuteKind.UNMEASURABLE }
            val movement = FloatArray(size)
            val heartRate = FloatArray(size) { Float.NaN }
            for ((slot, minute) in byMinute) {
                val index = (slot - first).toInt()
                kinds[index] = minute.kind
                if (minute.kind == SleepMinuteKind.RAW) {
                    movement[index] = maxOf(minute.movement, 0f).takeIf { it.isFinite() } ?: 0f
                    val bpm = minute.heartRate
                    if (bpm != null && bpm.isFinite() && bpm > 0f) heartRate[index] = bpm
                }
            }
            return MinuteGrid(first, kinds, movement, heartRate)
        }

        private const val SecondsPerMinute = 60L
    }
}

/** Per-minute stage features over one window. Indices are window-local. */
private class StageFeatures(
    val deviation: FloatArray,
    val variability: FloatArray,
    val variabilityMedian: Float,
    val movement: FloatArray,
    val sinceWake: IntArray,
) {
    companion object {
        fun of(
            grid: MinuteGrid,
            labels: CharArray,
            onset: Int,
            end: Int,
            config: SleepEstimatorConfig,
        ): StageFeatures {
            val length = end - onset
            val heartRate = FloatArray(length) { grid.heartRate[onset + it] }
            val sleeping = BooleanArray(length) { labels[onset + it] == SleepStageEstimator.Sleep }

            val smoothed = FloatArray(length) { index ->
                medianOf(heartRate, index - config.heartRateSmoothHalf, index + config.heartRateSmoothHalf)
            }
            // The baseline follows the night: heart rate keeps falling for an hour after onset.
            val deviation = FloatArray(length) { index ->
                val base = medianOf(smoothed, index - config.baselineHalf, index + config.baselineHalf, sleeping)
                if (smoothed[index].isNaN() || base.isNaN()) 0f else smoothed[index] - base
            }

            val rawVariability = FloatArray(length) { index ->
                standardDeviationOf(heartRate, index - config.deviationHalf, index + config.deviationHalf)
            }
            val sleepingVariability = FloatArray(length)
            var count = 0
            for (index in 0 until length) {
                if (sleeping[index] && !rawVariability[index].isNaN()) sleepingVariability[count++] = rawVariability[index]
            }
            val variabilityMedian = if (count == 0) 1f else medianOf(sleepingVariability, 0, count - 1)
            val variability = FloatArray(length) { index ->
                if (rawVariability[index].isNaN()) variabilityMedian else rawVariability[index]
            }

            val activity = grid.activity(config)
            val movement = FloatArray(length) { index ->
                var sum = 0f
                for (at in index - config.movementHalf..index + config.movementHalf) {
                    if (at in 0 until length) sum += activity[onset + at]
                }
                sum
            }

            // Minutes since the last wake or gap run of at least wakeRunMinutes ended.
            val sinceWake = IntArray(length) { Int.MAX_VALUE }
            var runLength = 0
            var lastWakeEnd = -1
            for (index in 0 until length) {
                if (sleeping[index]) {
                    if (runLength >= config.wakeRunMinutes) lastWakeEnd = index
                    runLength = 0
                    if (lastWakeEnd >= 0) sinceWake[index] = index - lastWakeEnd
                } else {
                    runLength++
                }
            }

            return StageFeatures(deviation, variability, variabilityMedian, movement, sinceWake)
        }

        /** Median of the finite values in `[from, to]`, optionally only where [mask] is true. NaN if none. */
        private fun medianOf(values: FloatArray, from: Int, to: Int, mask: BooleanArray? = null): Float {
            val low = maxOf(from, 0)
            val high = minOf(to, values.size - 1)
            if (high < low) return Float.NaN
            val picked = FloatArray(high - low + 1)
            var count = 0
            for (index in low..high) {
                if (values[index].isNaN()) continue
                if (mask != null && !mask[index]) continue
                picked[count++] = values[index]
            }
            if (count == 0) return Float.NaN
            picked.sort(0, count)
            return if (count % 2 == 1) picked[count / 2] else (picked[count / 2 - 1] + picked[count / 2]) / 2f
        }

        /** Population standard deviation over `[from, to]`, or NaN with fewer than four samples. */
        private fun standardDeviationOf(values: FloatArray, from: Int, to: Int): Float {
            val low = maxOf(from, 0)
            val high = minOf(to, values.size - 1)
            var count = 0
            var sum = 0.0
            for (index in low..high) {
                if (values[index].isNaN()) continue
                count++
                sum += values[index]
            }
            if (count < MinVariabilitySamples) return Float.NaN
            val mean = sum / count
            var squares = 0.0
            for (index in low..high) {
                if (values[index].isNaN()) continue
                val delta = values[index] - mean
                squares += delta * delta
            }
            return sqrt(squares / count).toFloat()
        }

        private const val MinVariabilitySamples = 4
    }
}
