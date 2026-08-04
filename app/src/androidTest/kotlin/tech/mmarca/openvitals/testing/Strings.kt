package tech.mmarca.openvitals.testing

import androidx.annotation.StringRes
import androidx.test.platform.app.InstrumentationRegistry

/**
 * A string resource, resolved the way the app under test would resolve it.
 *
 * Instrumentation tests assert on rendered text, and the obvious way to write
 * that is to type the English in. It makes the test fail for two reasons that
 * have nothing to do with the behaviour: someone rewording a label, and the
 * device being in any other locale. Reading the resource means the assertion
 * pins what the screen *shows a user*, not what it said in English on the day
 * the test was written.
 */
fun string(@StringRes id: Int): String =
    InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

fun string(@StringRes id: Int, vararg formatArgs: Any): String =
    InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *formatArgs)
