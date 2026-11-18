package io.github.ctorres345.hemisphere.feature.browser.selection

import io.github.ctorres345.hemisphere.core.model.Listing
import io.github.ctorres345.hemisphere.feature.browser.pane.TabState

/** Pure: no I/O, no coroutines, no clock. A key the open folder does not list is ignored. */
fun reduceSelection(
    tab: TabState,
    result: SelectionResult,
): TabState {
    val listed = tab.listing.entries.map(tab::keyOf)
    return when (result) {
        is SelectionResult.LongPressed -> {
            if (result.key !in listed) return tab
            val from = listed.indexOf(tab.selectionAnchor)
            val to = listed.indexOf(result.key)
            val added = if (tab.selecting && from >= 0) listed.between(from, to) else listOf(result.key)
            tab.copy(selection = tab.selection + added, selectionAnchor = result.key)
        }

        is SelectionResult.Toggled -> {
            when (result.key) {
                !in listed -> tab
                in tab.selection -> tab.copy(selection = tab.selection - result.key).withoutStaleAnchor()
                else -> tab.copy(selection = tab.selection + result.key, selectionAnchor = result.key)
            }
        }

        SelectionResult.SelectedAll -> {
            // Rows a refresh has not read yet stay selected.
            tab.copy(selection = tab.selection + listed)
        }

        SelectionResult.Cleared -> {
            tab.copy(selection = emptySet(), selectionAnchor = null)
        }
    }
}

/**
 * Drops selected keys [listing] no longer lists, such as a file deleted outside the app. Only a
 * complete listing without an error counts: a refresh emits the rows read so far, and a row not read
 * yet is not gone.
 */
fun TabState.prunedTo(listing: Listing): TabState {
    if (!selecting || !listing.complete || listing.error != null) return this
    val listed = listing.entries.mapTo(HashSet(), ::keyOf)
    return copy(selection = selection.filterTo(HashSet()) { it in listed }).withoutStaleAnchor()
}

/** The rows from [a] to [b], both included, whichever comes first. */
private fun <T> List<T>.between(
    a: Int,
    b: Int,
): List<T> = subList(minOf(a, b), maxOf(a, b) + 1)

/** The anchor only names a selected row; with nothing selected there is none. */
private fun TabState.withoutStaleAnchor(): TabState =
    if (selectionAnchor != null && selectionAnchor !in selection) copy(selectionAnchor = null) else this
