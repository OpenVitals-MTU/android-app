package tech.mmarca.openvitals.ui.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner

/** An icon button a screen puts in the app bar. */
@Immutable
data class AppBarAction(
    val icon: ImageVector,
    @param:StringRes val contentDescription: Int,
    /** For a toggle that is on. Null takes the ordinary app bar tint. */
    val tint: Color? = null,
    val onClick: () -> Unit,
)

/**
 * What one screen wants in the app bar while it is on screen.
 *
 * The app bar renders this without knowing which screen sent it. A screen that
 * declares nothing keeps the title its route carries and shows no actions.
 */
@Immutable
data class ScreenAppBar(
    /** Replaces the title the route carries, for a screen that titles itself. */
    val title: String? = null,
    val actions: List<AppBarAction> = emptyList(),
    /** Repaints the chrome, for a screen that takes over the display's colours. */
    val containerColor: Color? = null,
    /** The screen wants the whole display: no app bar at all. */
    val hidesAppBar: Boolean = false,
)

/**
 * What each screen has declared, by the destination it belongs to.
 *
 * Keyed by destination because both screens are composed during a navigation
 * animation. The app bar asks for the destination it is showing, so nothing of
 * the screen being left can appear on the one arriving.
 */
@Stable
class AppBarState {
    private val declarations = mutableStateMapOf<Any, MutableState<ScreenAppBar>>()

    /** What the screen at [destination] declared, or null if it declared nothing. */
    fun of(destination: Any?): ScreenAppBar? = destination?.let { declarations[it]?.value }

    internal fun declare(destination: Any, appBar: ScreenAppBar) {
        val slot = declarations[destination]
        if (slot == null) {
            declarations[destination] = mutableStateOf(appBar)
        } else {
            // An equal value is not a change, so a screen that recomposes without
            // changing its chrome does not recompose the app bar.
            slot.value = appBar
        }
    }

    internal fun withdraw(destination: Any) {
        declarations.remove(destination)
    }
}

val LocalAppBarState = staticCompositionLocalOf { AppBarState() }

/**
 * Declares [appBar] while this screen is on screen, and withdraws it when the
 * screen leaves. Nothing has to be cleared by hand.
 *
 * Build [appBar] inside `remember` when it holds a lambda: two lambdas built
 * from the same code are not equal, so an unremembered one recomposes the app
 * bar on every pass.
 */
@Composable
fun DeclareAppBar(appBar: ScreenAppBar) {
    val state = LocalAppBarState.current
    // Inside a navigation graph this owner is the destination's own back stack entry.
    val destination = LocalViewModelStoreOwner.current ?: return
    SideEffect { state.declare(destination, appBar) }
    DisposableEffect(state, destination) {
        onDispose { state.withdraw(destination) }
    }
}
