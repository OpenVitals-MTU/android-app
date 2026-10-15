package tech.mmarca.openvitals.domain.insights

/** The per-minute sleep/wake labels the estimators exchange, and runs over them. */
internal object SleepLabels {
    const val Sleep = 'S'
    const val Wake = 'W'
    const val Gap = 'G'

    class LabelRun(val label: Char, val start: Int, val length: Int)

    fun runsOf(labels: CharArray, from: Int = 0, to: Int = labels.size): List<LabelRun> {
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
}

/**
 * Webster's rescoring rules (Webster 1982, as used with Cole-Kripke): the
 * first sleep minutes after a wake run are still wake, and a short sleep
 * run between long wake runs is wake. Passes repeat until stable: a wake
 * run that grew carries over again. Shared by every sleep/wake scorer.
 */
internal object WebsterRescoring {

    fun apply(labels: CharArray, passes: Int = DefaultPasses) {
        repeat(passes) {
            val carried = carryOver(labels)
            val bridged = bridge(labels)
            if (!carried && !bridged) return
        }
    }

    /** Rule 1: the first sleep minutes after a wake run are still wake. Returns whether anything changed. */
    fun carryOver(labels: CharArray): Boolean {
        var changed = false
        val runs = SleepLabels.runsOf(labels)
        for ((position, run) in runs.withIndex()) {
            if (run.label != SleepLabels.Wake) continue
            val next = runs.getOrNull(position + 1) ?: continue
            if (next.label != SleepLabels.Sleep) continue
            val toWake = when {
                run.length >= 15 -> 4
                run.length >= 10 -> 3
                run.length >= 4 -> 1
                else -> 0
            }
            for (index in next.start until next.start + minOf(toWake, next.length)) {
                labels[index] = SleepLabels.Wake
                changed = true
            }
        }
        return changed
    }

    /** Rules 2 and 3: a short sleep run between long wake runs is wake. Returns whether anything changed. */
    fun bridge(labels: CharArray): Boolean {
        var changed = false
        val rescanned = SleepLabels.runsOf(labels)
        for ((position, run) in rescanned.withIndex()) {
            if (run.label != SleepLabels.Sleep) continue
            val before = rescanned.getOrNull(position - 1) ?: continue
            val after = rescanned.getOrNull(position + 1) ?: continue
            if (before.label != SleepLabels.Wake || after.label != SleepLabels.Wake) continue
            val bounded = minOf(before.length, after.length)
            if ((run.length <= 6 && bounded >= 10) || (run.length <= 10 && bounded >= 20)) {
                for (index in run.start until run.start + run.length) labels[index] = SleepLabels.Wake
                changed = true
            }
        }
        return changed
    }

    /** The corpus constants were fitted with this, so it is not a single pass. */
    const val DefaultPasses = 3
}
