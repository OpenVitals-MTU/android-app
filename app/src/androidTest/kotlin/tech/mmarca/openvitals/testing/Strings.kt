package tech.mmarca.openvitals.testing

import androidx.annotation.StringRes
import androidx.test.platform.app.InstrumentationRegistry

/** A string resource resolved as the app resolves it, so the assertion pins what the screen shows, not the English of the day. */
fun string(@StringRes id: Int): String =
    InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

fun string(@StringRes id: Int, vararg formatArgs: Any): String =
    InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *formatArgs)
