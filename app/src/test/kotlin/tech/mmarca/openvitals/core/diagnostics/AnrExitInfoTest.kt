package tech.mmarca.openvitals.core.diagnostics

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AnrExitInfoTest {
    private val trace = """
        ----- pid 4242 at 2026-09-18 19:51:02 -----
        Cmd line: tech.mmarca.openvitals

        DALVIK THREADS (3):
        "Signal Catcher" daemon prio=10 tid=6 Runnable
          at java.lang.Object.wait(Native method)

        "DefaultDispatcher-worker-2" daemon prio=5 tid=31 Waiting
          at kotlinx.coroutines.scheduling.CoroutineScheduler${'$'}Worker.park(CoroutineScheduler.kt:855)

        "main" prio=5 tid=1 Waiting
          at kotlinx.coroutines.BlockingCoroutine.joinBlocking(Builders.kt:121)
          at tech.mmarca.openvitals.features.homewidgets.UpdatingHomeWidgetReceiver.onUpdate(HomeReadinessWidgets.kt:151)

        "RenderThread" daemon prio=7 tid=22 Native
          at android.os.MessageQueue.nativePollOnce(Native method)
    """.trimIndent()

    @Test
    fun `keeps main and the coroutine workers, main first`() {
        val trimmed = trimAnrTrace(trace)

        assertThat(trimmed).startsWith("\"main\"")
        assertThat(trimmed).contains("UpdatingHomeWidgetReceiver.onUpdate")
        assertThat(trimmed).contains("DefaultDispatcher-worker-2")
        assertThat(trimmed).doesNotContain("RenderThread")
        assertThat(trimmed).doesNotContain("Signal Catcher")
    }

    @Test
    fun `a long trace is cut and says so`() {
        val trimmed = trimAnrTrace(trace, maxChars = 40)

        assertThat(trimmed).endsWith("[trace truncated]")
        assertThat(trimmed.length).isAtMost(40 + "\n[trace truncated]".length)
    }

    @Test
    fun `a record without a trace says so`() {
        val record = formatAnrRecord(
            at = "2026-09-18T19:51:02Z",
            processName = "tech.mmarca.openvitals",
            description = "Broadcast of Intent { act=android.appwidget.action.APPWIDGET_UPDATE }",
            importance = 100,
            trace = null,
        )

        assertThat(record).contains("APPWIDGET_UPDATE")
        assertThat(record).contains("No trace was kept.")
    }
}
