package io.github.ctorres345.hemisphere.core.designsystem

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp

/**
 * The last input the user used (docs/design-system.md §3, "Input-aware UI"). Touch hides the focus
 * ring and the button hints and uses touch density; Gamepad shows them and uses compact density.
 * Keyboard (a hardware keyboard's navigation keys) shows the focus ring only, at touch density.
 * `core:input` tracks the real input; the app provides [LocalInputMode].
 */
enum class InputMode {
    Touch,
    Gamepad,
    Keyboard,
    ;

    /** True when focus is drawn: the focus ring and the focused row's fill (gamepad and keyboard). */
    val showsFocus: Boolean
        get() = this != Touch

    /** True when the button-hint footer shows (gamepad only). */
    val showsHints: Boolean
        get() = this == Gamepad

    /** The list row height for this mode: Compact (40dp) for gamepad, else Default (48dp) (design §7). */
    val rowHeight: Dp
        get() =
            when (this) {
                Touch, Keyboard -> HemisphereRowHeight.default
                Gamepad -> HemisphereRowHeight.compact
            }
}

// Like the theme colors, the input mode is ambient UI state every component reads (architecture §2 puts it here).

/**
 * The current [InputMode]. It changes only when the user switches between touch, a gamepad and a
 * keyboard, so a static local is cheap. Touch until the app provides the tracked mode.
 */
@Suppress("ComposeCompositionLocalUsage")
val LocalInputMode = staticCompositionLocalOf { InputMode.Touch }
