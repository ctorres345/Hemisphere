package io.github.ctorres345.hemisphere.feature.browser

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
    }
