package io.github.ctorres345.hemisphere.core.input

import io.github.ctorres345.hemisphere.core.designsystem.InputMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tracks the last input kind used: last event wins (docs/design-system.md §3). Feed it every input event
 * with [onTouch] or [onGamepad]; [mode] changes only on the first event of the other kind.
 */
class InputModeTracker(
    initial: InputMode = InputMode.Touch,
) {
    private val state = MutableStateFlow(initial)

    /** The current mode. Touch hides focus rings; Gamepad shows them. */
    val mode: StateFlow<InputMode> = state.asStateFlow()

    /** A touch (or pointer) event happened. */
    fun onTouch() = onInput(InputMode.Touch)

    /** A gamepad key or axis event happened. */
    fun onGamepad() = onInput(InputMode.Gamepad)

    /** An event of kind [mode] happened. Same kind as now: nothing changes. */
    fun onInput(mode: InputMode) {
        state.value = mode
    }
}
