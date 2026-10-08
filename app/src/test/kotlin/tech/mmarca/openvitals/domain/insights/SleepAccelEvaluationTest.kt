package tech.mmarca.openvitals.domain.insights

import java.io.File
import java.time.Duration
import org.json.JSONObject
import org.junit.Assume.assumeTrue
import org.junit.Test
import tech.mmarca.openvitals.devices.wearos.WearLinkProtocol
import tech.mmarca.openvitals.devices.wearos.WearOsSleepMinuteMapping
import tech.mmarca.openvitals.domain.model.WearSleepMinute

/**
 * The Wear OS sleep pipeline against polysomnography: PhysioNet's
 * sleep-accel nights, converted by `tool/sleep_accel_fixture/build.py` into
 * the rows the watch would have recorded, with a label per minute. The
 * fixture is local and gitignored, so this test runs only when
 * `-Dopenvitals.sleepAccelFixture=<path>` points at it and is skipped
 * otherwise; the numbers it prints go into the commit message of every
 * threshold change, since CI never sees them.
 *
 * Gates (per-minute, pooled over subjects, inside the scored range):
 * accuracy ≥ 0.85, wake specificity ≥ 0.60, sleep sensitivity ≥ 0.85,
 * onset and offset MAE ≤ 30 min, mean |TST error| ≤ 40 min. Walch 2019
 * reports 80% accuracy at 30-s epochs on this data with motion, heart rate
 * and a clock proxy; per-minute scoring is an easier problem. When a run
 * misses, tune on the fixture; do not lower the gate.
 */
class SleepAccelEvaluationTest {

    private val fixturePath: String? = System.getProperty("openvitals.sleepAccelFixture")?.takeIf { it.isNotBlank() }

    private class Subject(val id: String, val rows: List<WearSleepMinute>, val labels: String)

    /**
     * The recordings run for days around the scored night; the phone only
     * ever sees one night's window, so each subject is cut to the scored
     * range plus [NIGHT_MARGIN_MINUTES] either side, the way the 18:00 to
     * 14:00 window surrounds a real night.
     */
    private fun subjects(): List<Subject> {
        val root = JSONObject(File(fixturePath!!).readText())
        val array = root.getJSONArray("subjects")
        return List(array.length()) { index ->
            val subject = array.getJSONObject(index)
            val lines = subject.getJSONArray("lines")
            val labels = subject.getString("labels")
            val rows = List(lines.length()) { line ->
                val parsed = requireNotNull(WearLinkProtocol.parseSleepMinute(lines.getString(line))) { "unparsable line $line of ${subject.getString("id")}" }
                WearOsSleepMinuteMapping.toDomain(parsed)
            }
            val scored = labels.indices.filter { labels[it] != '-' }
            // By time, not by row: the rows before the scored night can be days older.
            val from = rows[scored.first()].time.minusSeconds(60L * NIGHT_MARGIN_MINUTES)
            val to = rows[scored.last()].time.plusSeconds(60L * NIGHT_MARGIN_MINUTES)
            val kept = rows.indices.filter { !rows[it].time.isBefore(from) && !rows[it].time.isAfter(to) }
            Subject(subject.getString("id"), kept.map { rows[it] }, kept.map { labels[it] }.joinToString(""))
        }
    }

    /** What the pipeline's stages said about a subject, for the report. */
    private fun diagnose(subject: Subject, config: WearSleepEstimator.Config): String {
        val grid = WearMinuteGrid.of(subject.rows) ?: return "empty"
        val wear = WearStateDetector.detect(grid, config.wear)
        val scored = subject.rows.indices.filter { subject.labels[it] != '-' }
        val reasons = scored.mapNotNull { grid.indexOf(subject.rows[it].time)?.let { at -> wear.reasons[at] } }.groupingBy { it }.eachCount()
        val angle = SptWindowDetector.angleWindow(grid, wear, config.window)
        val heart = SptWindowDetector.heartRateWindow(grid, wear, config.window)
        val window = SptWindowDetector.detect(grid, wear, config.window)
        val labels = SleepWakeScorer.score(grid, wear, config.scorer)
        val sleepInWindow = window?.let { w -> (w.onset until w.end).count { labels[it] == 'S' } }
        val first = scored.firstOrNull { subject.labels[it] != 'W' }?.let { grid.indexOf(subject.rows[it].time) }
        val scoredAt = grid.indexOf(subject.rows[scored.first()].time) to grid.indexOf(subject.rows[scored.last()].time)
        return "scored ${scoredAt.first}..${scoredAt.second} first sleep $first, not worn in scored $reasons, " +
            "angle ${angle?.let { "${it.first}..${it.last}" } ?: "none"}, heart ${heart?.let { "${it.first}..${it.last}" } ?: "none"}, " +
            "window ${window?.let { "${it.onset}..${it.end} ${it.source}" } ?: "none"}, S in window $sleepInWindow"
    }

    /** Per-minute tallies, pooled. */
    private class Tally {
        var sleepTrue = 0
        var sleepMissed = 0
        var wakeTrue = 0
        var wakeMissed = 0
        var stageAgree = 0
        var stageTotal = 0
        val onsetErrors = ArrayList<Long>()
        val offsetErrors = ArrayList<Long>()
        val tstErrors = ArrayList<Long>()
        var noNight = 0

        val sensitivity get() = sleepTrue.toDouble() / maxOf(1, sleepTrue + sleepMissed)
        val specificity get() = wakeTrue.toDouble() / maxOf(1, wakeTrue + wakeMissed)
        val accuracy get() = (sleepTrue + wakeTrue).toDouble() / maxOf(1, sleepTrue + sleepMissed + wakeTrue + wakeMissed)
        val stageAccuracy get() = stageAgree.toDouble() / maxOf(1, stageTotal)
        fun mae(errors: List<Long>) = if (errors.isEmpty()) Double.NaN else errors.sumOf { kotlin.math.abs(it) }.toDouble() / errors.size
    }

    /** One pipeline configuration scored over every subject. */
    private fun evaluate(subjects: List<Subject>, config: WearSleepEstimator.Config, perSubject: StringBuilder?): Tally {
        val tally = Tally()
        for (subject in subjects) {
            val night = WearSleepEstimator.estimate(subject.rows, config)
            val scored = subject.rows.indices.filter { subject.labels[it] != '-' }
            if (scored.isEmpty()) continue
            val firstSleep = scored.firstOrNull { subject.labels[it] != 'W' }
            val lastSleep = scored.lastOrNull { subject.labels[it] != 'W' }
            val psgSleep = scored.count { subject.labels[it] != 'W' }
            if (night == null) {
                tally.noNight++
                scored.forEach { if (subject.labels[it] == 'W') tally.wakeTrue++ else tally.sleepMissed++ }
                perSubject?.append(String.format("  %-6s no night (PSG sleep %d min): %s%n", subject.id, psgSleep, diagnose(subject, config)))
                continue
            }
            val stageOf = HashMap<Long, EstimatedStage>()
            for (span in night.session.stages) {
                var at = span.start
                while (at.isBefore(span.end)) {
                    stageOf[at.epochSecond / 60] = span.stage
                    at = at.plusSeconds(60)
                }
            }
            var estimatedSleep = 0
            for (index in scored) {
                val minute = subject.rows[index].time.epochSecond / 60
                val stage = stageOf[minute]
                val asleep = stage == EstimatedStage.LIGHT || stage == EstimatedStage.DEEP || stage == EstimatedStage.REM
                if (asleep) estimatedSleep++
                val label = subject.labels[index]
                when {
                    label == 'W' && !asleep -> tally.wakeTrue++
                    label == 'W' -> tally.wakeMissed++
                    asleep -> tally.sleepTrue++
                    else -> tally.sleepMissed++
                }
                if (label != 'W' && asleep) {
                    tally.stageTotal++
                    val agree = (label == 'R' && stage == EstimatedStage.REM) || (label != 'R' && stage != EstimatedStage.REM)
                    if (agree) tally.stageAgree++
                }
            }
            if (firstSleep != null && lastSleep != null) {
                val onsetError = Duration.between(subject.rows[firstSleep].time, night.session.onset).toMinutes()
                val offsetError = Duration.between(subject.rows[lastSleep].time.plusSeconds(60), night.session.end).toMinutes()
                tally.onsetErrors += onsetError
                tally.offsetErrors += offsetError
                tally.tstErrors += (estimatedSleep - psgSleep).toLong()
                perSubject?.append(
                    String.format(
                        "  %-6s onset %+5d  offset %+5d  tst %+5d  worn %d  not worn %d  window %dm %s%n",
                        subject.id, onsetError, offsetError, estimatedSleep - psgSleep, night.wornMinutes, night.notWornMinutes,
                        night.window.length, night.window.source,
                    ),
                )
                if (perSubject != null && (kotlin.math.abs(onsetError) > 60 || kotlin.math.abs(offsetError) > 60)) {
                    perSubject.append("         ${diagnose(subject, config)}\n")
                }
            }
        }
        return tally
    }

    private fun Tally.summary(): String = String.format(
        "acc %.3f  spec %.3f  sens %.3f  onset MAE %5.1f  offset MAE %5.1f  TST MAE %5.1f  REM agree %.3f  no night %d",
        accuracy, specificity, sensitivity, mae(onsetErrors), mae(offsetErrors), mae(tstErrors), stageAccuracy, noNight,
    )

    @Test
    fun `the pipeline scores PhysioNet nights within the gates`() {
        assumeTrue("set -Dopenvitals.sleepAccelFixture to run", fixturePath != null)
        val subjects = subjects()
        val perSubject = StringBuilder()
        val tally = evaluate(subjects, config, perSubject)
        val report = StringBuilder()
        report.append("sleep-accel, ${subjects.size} subjects\n")
        report.append(String.format("  %-34s %s%n", "default", tally.summary()))
        for ((name, variant) in VARIANTS) {
            report.append(String.format("  %-34s %s%n", name, evaluate(subjects, variant, null).summary()))
        }
        report.append(perSubject)
        println(report)
        val failures = buildList {
            if (tally.accuracy < 0.85) add("accuracy ${"%.3f".format(tally.accuracy)} < 0.85")
            if (tally.specificity < 0.60) add("wake specificity ${"%.3f".format(tally.specificity)} < 0.60")
            if (tally.sensitivity < 0.85) add("sleep sensitivity ${"%.3f".format(tally.sensitivity)} < 0.85")
            if (tally.mae(tally.onsetErrors) > 30) add("onset MAE ${"%.1f".format(tally.mae(tally.onsetErrors))} > 30")
            if (tally.mae(tally.offsetErrors) > 30) add("offset MAE ${"%.1f".format(tally.mae(tally.offsetErrors))} > 30")
            if (tally.mae(tally.tstErrors) > 40) add("TST MAE ${"%.1f".format(tally.mae(tally.tstErrors))} > 40")
        }
        if (failures.isNotEmpty() && GATES_ENFORCED) throw AssertionError("gates missed: $failures\n$report")
    }

    @Test
    fun `two table hours before a PhysioNet night do not start the night early`() {
        assumeTrue("set -Dopenvitals.sleepAccelFixture to run", fixturePath != null)
        val subject = subjects().first()
        val firstReal = subject.rows.first().time
        val table = (120 downTo 1).map { WearSleepFixtures.table(firstReal.minusSeconds(60L * it)) }

        val night = WearSleepEstimator.estimate(table + subject.rows, config)

        if (night != null) {
            assert(!night.session.onset.isBefore(firstReal)) { "onset ${night.session.onset} before the first real row $firstReal" }
        }
    }

    private companion object {
        /** Minutes kept either side of the scored night, standing in for the phone's night window. */
        const val NIGHT_MARGIN_MINUTES = 240

        /** Every tuning knob at its default: the cut recording is about as long as the phone's night window. */
        val config = WearSleepEstimator.Config()

        /** The ablation table: one knob at a time against the default, so a change is argued with numbers. */
        val VARIANTS: List<Pair<String, WearSleepEstimator.Config>> = run {
            val window = config.window
            val scorer = config.scorer
            listOf(
                "angle floor 1.0, max 2.0" to config.copy(window = window.copy(angleThresholdMinDeg = 1.0f, angleThresholdMaxDeg = 2.0f)),
                "angle floor 2.5, max 4.0" to config.copy(window = window.copy(angleThresholdMinDeg = 2.5f, angleThresholdMaxDeg = 4.0f)),
                "angle block 20, merge 90" to config.copy(window = window.copy(angleBlockMinMinutes = 20, angleGapMergeMinutes = 90)),
                "HR window on" to config.copy(window = window.copy(useHeartRateWindow = true)),
                "scorer: movement only" to config.copy(scorer = scorer.copy(heartRateVolatilityWeight = 0f, angleMovingWeight = 0f)),
                "scorer: movement + HR" to config.copy(scorer = scorer.copy(angleMovingWeight = 0f)),
                "scorer: movement + angle" to config.copy(scorer = scorer.copy(heartRateVolatilityWeight = 0f)),
                "scorer: HR 1.0, angle 0.5" to config.copy(scorer = scorer.copy(heartRateVolatilityWeight = 1.0f, angleMovingWeight = 0.5f)),
                "scorer: Webster 1 pass" to config.copy(scorer = scorer.copy(websterPasses = 1)),
                "scorer: wake score 1.2" to config.copy(scorer = scorer.copy(wakeScore = 1.2f)),
                "stages: no LIDS" to config.copy(stages = config.stages.copy(lidsWeight = 0f)),
                "stages: LIDS 0.6" to config.copy(stages = config.stages.copy(lidsWeight = 0.6f)),
            )
        }

        /** The report is printed either way; the gates hold the default configuration to its evaluated numbers. */
        const val GATES_ENFORCED = true
    }
}
