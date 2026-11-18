package io.github.ctorres345.hemisphere.feature.browser

import io.github.ctorres345.hemisphere.feature.browser.layout.BrowserWindow
import io.github.ctorres345.hemisphere.feature.browser.layout.PaneLayout
import io.github.ctorres345.hemisphere.feature.browser.pane.PaneId
import io.github.ctorres345.hemisphere.feature.browser.pane.PaneIntent
import io.github.ctorres345.hemisphere.feature.browser.pane.PaneResult
import io.github.ctorres345.hemisphere.feature.browser.pane.PaneState

/**
 * Everything the Browser screen shows (architecture §6.1): the shell and both panes. Single shows the
 * active pane only; Dual shows both, side by side.
 */
data class BrowserUiState(
    val shell: ShellState = ShellState(),
    val panes: Map<PaneId, PaneState> = emptyMap(),
) {
    /** The pane that takes input and system back. */
    val activePane: PaneState? get() = panes[shell.activePane]

    /** The panes on screen, left to right: both in Dual, the active one in Single. */
    val shownPanes: List<PaneId>
        get() = if (shell.layout == PaneLayout.Dual) PaneId.entries else listOf(shell.activePane)
}

/** Screen-wide state: which pane is active and the layout shown. The clipboard tray comes later. */
data class ShellState(
    val activePane: PaneId = PaneId.Left,
    val layout: PaneLayout = PaneLayout.Single,
)

/** Everything the user can do on the Browser screen. */
sealed interface BrowserIntent {
    /** An intent for one pane. */
    data class Pane(
        val pane: PaneId,
        val intent: PaneIntent,
    ) : BrowserIntent

    /** The screen measured its window: on start, on rotation and on resize. */
    data class WindowChanged(
        val window: BrowserWindow,
    ) : BrowserIntent

    /** The user tapped [pane]; in Dual it becomes the active pane. */
    data class Activate(
        val pane: PaneId,
    ) : BrowserIntent

    /** The toolbar toggle: pin the other layout for this orientation. */
    data object ToggleLayout : BrowserIntent
}

/** Everything that can change [BrowserUiState]. */
sealed interface BrowserResult {
    /** A result for one pane, reduced by `reducePane`. */
    data class Pane(
        val pane: PaneId,
        val result: PaneResult,
    ) : BrowserResult

    /** The layout shown changed. Both panes keep their state; Single shows the active one. */
    data class LayoutChanged(
        val layout: PaneLayout,
    ) : BrowserResult

    /** [pane] is now the active pane. */
    data class Activated(
        val pane: PaneId,
    ) : BrowserResult
}
