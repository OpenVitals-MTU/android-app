package tech.mmarca.openvitals.testing

import android.animation.ValueAnimator
import androidx.test.platform.app.InstrumentationRegistry
import java.io.FileInputStream
import org.junit.rules.ExternalResource

/**
 * Turns the device's reduce-motion switch on and off for a test.
 *
 * OpenVitals reads `ValueAnimator.areAnimatorsEnabled()`, which is the system
 * animator scale — there is no app-level override to fake. So a test about
 * reduce-motion behaviour has to move the real setting, and put it back
 * afterwards whether it passed or not.
 *
 * The setting reaches the process asynchronously, so [motion] waits for
 * `areAnimatorsEnabled` to actually agree before returning. Composing before
 * that lands would read the previous value and the test would assert against
 * the wrong branch — quietly, and only sometimes.
 */
class AnimatorScaleRule : ExternalResource() {

    private var original: String? = null

    override fun before() {
        original = readSetting()
    }

    override fun after() {
        original?.let { write(it) }
    }

    /** Sets the animator scale so that [enabled] is what the app will observe. */
    fun motion(enabled: Boolean) {
        write(if (enabled) "1" else "0")
        val deadline = System.currentTimeMillis() + SettleTimeoutMillis
        while (ValueAnimator.areAnimatorsEnabled() != enabled) {
            check(System.currentTimeMillis() < deadline) {
                "animator_duration_scale did not reach ${if (enabled) "on" else "off"} in " +
                    "${SettleTimeoutMillis}ms; areAnimatorsEnabled is still " +
                    ValueAnimator.areAnimatorsEnabled()
            }
            Thread.sleep(25)
        }
    }

    private fun readSetting(): String =
        shell("settings get global animator_duration_scale")
            .trim()
            .takeIf { it.isNotEmpty() && it != "null" }
            ?: "1"

    private fun write(value: String) {
        shell("settings put global animator_duration_scale $value")
    }

    private fun shell(command: String): String =
        InstrumentationRegistry.getInstrumentation()
            .uiAutomation
            .executeShellCommand(command)
            .let { descriptor ->
                FileInputStream(descriptor.fileDescriptor).use { it.readBytes().decodeToString() }
            }

    private companion object {
        const val SettleTimeoutMillis = 5_000L
    }
}
