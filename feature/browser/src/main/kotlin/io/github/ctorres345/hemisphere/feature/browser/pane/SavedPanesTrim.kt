package io.github.ctorres345.hemisphere.feature.browser.pane

// How `SavedPanes.trimmed` cuts an oversized state, one step at a time.

/** The panes with one anchor removed: the oldest of the tab holding the most, or null when none is left. */
internal fun Map<PaneId, PaneState>.withoutOldestAnchor(): Map<PaneId, PaneState>? =
    replacingLargestTab({ it.anchors.byLevel(it.backStack).size }) { tab ->
        val (level, _) = tab.anchors.byLevel(tab.backStack).first()
        tab.copy(anchors = tab.anchors.keptFor(tab.backStack) - tab.backStack[level].key)
    }

/** The panes with the bottom level of the deepest stack removed, or null when every stack is one folder. */
internal fun Map<PaneId, PaneState>.withoutOldestLevel(): Map<PaneId, PaneState>? =
    replacingLargestTab({ it.backStack.size - 1 }) { tab ->
        val stack = tab.backStack.drop(1)
        tab.copy(backStack = stack, anchors = tab.anchors.keptFor(stack))
    }

/** Replaces the tab with the largest positive [measure] by [change], or null when none is positive. */
private fun Map<PaneId, PaneState>.replacingLargestTab(
    measure: (TabState) -> Int,
    change: (TabState) -> TabState,
): Map<PaneId, PaneState>? =
    entries
        .flatMap { (id, pane) -> pane.tabs.indices.map { id to it } }
        .filter { (id, index) -> measure(getValue(id).tabs[index]) > 0 }
        .maxByOrNull { (id, index) -> measure(getValue(id).tabs[index]) }
        ?.let { (id, index) ->
            val pane = getValue(id)
            this + (id to pane.copy(tabs = pane.tabs.toMutableList().apply { this[index] = change(this[index]) }))
        }
