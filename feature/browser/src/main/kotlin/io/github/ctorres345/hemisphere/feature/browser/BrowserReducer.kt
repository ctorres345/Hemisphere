package io.github.ctorres345.hemisphere.feature.browser

import io.github.ctorres345.hemisphere.feature.browser.layout.PaneLayout
import io.github.ctorres345.hemisphere.feature.browser.pane.reducePane

/** The only place [BrowserUiState] changes. Pure: no I/O, no coroutines, no clock. */
fun reduce(
    state: BrowserUiState,
    result: BrowserResult,
): BrowserUiState =
    when (result) {
        is BrowserResult.Pane -> {
            val pane = state.panes[result.pane] ?: return state
            state.copy(panes = state.panes + (result.pane to reducePane(pane, result.result)))
        }

        is BrowserResult.LayoutChanged -> {
            state.copy(shell = state.shell.copy(layout = result.layout))
        }

        // Single shows only the active pane: there is no other pane to tap.
        is BrowserResult.Activated -> {
            if (state.shell.layout != PaneLayout.Dual) return state
            state.copy(shell = state.shell.copy(activePane = result.pane))
        }
    }
