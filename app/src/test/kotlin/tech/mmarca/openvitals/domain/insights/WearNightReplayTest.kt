package tech.mmarca.openvitals.domain.insights

import java.io.File
import java.time.ZoneId
import org.junit.Assume.assumeTrue
import org.junit.Test
import tech.mmarca.openvitals.devices.wearos.WearOsSleepMinuteMapping
import tech.mmarca.openvitals.wearlink.WearLinkProtocol

/**
 * Replays a night pulled off a real watch through the pipeline and prints
 * what the phone would have written: the wear state, the window, the
 * labels per hour and the stages. For a developer with a watch in hand;
 * skipped unless `-Dopenvitals.wearNightLines=<file of SM lines>` is set
 * (`tool/README.md` says how to get the lines off the watch).
 */
class WearNightReplayTest {

    private val path: String? = System.getProperty("openvitals.wearNightLines")?.takeIf { it.isNotBlank() }

    @Test
    fun `replay a recorded night and print the estimate`() {
        assumeTrue("set -Dopenvitals.wearNightLines to run", path != null)
        val rows = File(path!!).readLines().mapNotNull { WearLinkProtocol.parseSleepMinute(it) }.map(WearOsSleepMinuteMapping::toDomain)
        val grid = requireNotNull(WearMinuteGrid.of(rows)) { "no rows" }
        val config = WearSleepEstimator.Config()
        val wear = WearStateDetector.detect(grid, config.wear)
        val window = SptWindowDetector.detect(grid, wear, config.window)
        val labels = SleepWakeScorer.score(grid, wear, config.scorer)
        val night = WearSleepEstimator.estimate(rows, config)
        val zone = ZoneId.systemDefault()
        val report = StringBuilder()
        report.append("rows ${rows.size}, grid ${grid.size} minutes from ${grid.timeAt(0).atZone(zone)}\n")
        report.append("worn ${wear.wornMinutes}, not worn ${wear.notWornMinutes} ${wear.reasonCounts}\n")
        report.append("angle window ${SptWindowDetector.angleWindow(grid, wear, config.window)}, heart window ${SptWindowDetector.heartRateWindow(grid, wear, config.window)}\n")
        report.append("window ${window?.let { "${grid.timeAt(it.onset).atZone(zone).toLocalTime()} → ${grid.timeAt(it.end).atZone(zone).toLocalTime()} (${it.length} min, ${it.source})" } ?: "none"}\n")
        report.append("labels per hour (S sleep, W wake, G gap):\n")
        var index = 0
        while (index < grid.size) {
            val end = minOf(grid.size, index + 60)
            report.append("  ${grid.timeAt(index).atZone(zone).toLocalTime()} ${String(labels, index, end - index)}\n")
            index = end
        }
        if (night == null) {
            report.append("no night\n")
        } else {
            report.append("night: ${night.summary()}\n")
            for (span in night.session.stages) {
                report.append("  ${span.start.atZone(zone).toLocalTime()} → ${span.end.atZone(zone).toLocalTime()} ${span.stage}\n")
            }
        }
        println(report)
    }
}
