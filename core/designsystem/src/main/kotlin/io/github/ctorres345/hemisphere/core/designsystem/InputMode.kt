package io.github.ctorres345.hemisphere.core.designsystem

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp

/**
 * The last input the user used (docs/design-system.md §3, "Input-aware UI"). Touch hides the focus
 * ring and the button hints and uses touch density; Gamepad shows them and uses compact density.
 * `core:input` tracks the real input and provides [LocalInputMode].
 */
enum class InputMode {
    Touch,
    Gamepad,
    ;

    /** The list row height for this mode: Default (48dp) for touch, Compact (40dp) for gamepad (design §7). */
    val rowHeight: Dp
        get() =
            when (this) {
                Touch -> HemisphereRowHeight.default
                Gamepad -> HemisphereRowHeight.compact
            }
}

// Like the theme colors, the input mode is ambient UI state every component reads (architecture §2 puts it here).

/**
 * The current [InputMode]. It changes only when the user switches between touch and a gamepad, so a
 * static local is cheap. Touch until `core:input` says otherwise.
 */
@Suppress("ComposeCompositionLocalUsage")
val LocalInputMode = staticCompositionLocalOf { InputMode.Touch }
