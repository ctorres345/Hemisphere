package io.github.ctorres345.hemisphere.feature.browser

import io.github.ctorres345.hemisphere.feature.browser.pane.PaneId
import io.github.ctorres345.hemisphere.feature.browser.pane.PaneIntent
import io.github.ctorres345.hemisphere.feature.browser.pane.PaneResult
import io.github.ctorres345.hemisphere.feature.browser.pane.PaneState

/**
 * Everything the Browser screen shows (architecture §6.1): the shell and both panes. Single shows the
 * left pane only; Dual comes with HEM-54.
 */
data class BrowserUiState(
    val shell: ShellState = ShellState(),
    val panes: Map<PaneId, PaneState> = emptyMap(),
) {
    /** The pane that takes input and system back. */
    val activePane: PaneState? get() = panes[shell.activePane]
}

/** Screen-wide state: which pane is active. Layout mode and the clipboard tray come later. */
data class ShellState(
    val activePane: PaneId = PaneId.Left,
)

/** Everything the user can do on the Browser screen. */
sealed interface BrowserIntent {
    /** An intent for one pane. */
    data class Pane(
        val pane: PaneId,
        val intent: PaneIntent,
    ) : BrowserIntent
}

/** Everything that can change [BrowserUiState]. */
sealed interface BrowserResult {
    /** A result for one pane, reduced by `reducePane`. */
    data class Pane(
        val pane: PaneId,
        val result: PaneResult,
    ) : BrowserResult
}
