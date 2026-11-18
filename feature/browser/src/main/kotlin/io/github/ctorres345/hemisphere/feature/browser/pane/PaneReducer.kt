package io.github.ctorres345.hemisphere.feature.browser.pane

import io.github.ctorres345.hemisphere.core.model.Listing
import io.github.ctorres345.hemisphere.feature.browser.selection.prunedTo
import io.github.ctorres345.hemisphere.feature.browser.selection.reduceSelection

/**
 * The one reducer for a pane, shared by both panes (architecture §6.1). Pure: no I/O, no coroutines,
 * no clock. Every result applies to the active tab.
 */
fun reducePane(
    pane: PaneState,
    result: PaneResult,
): PaneState =
    pane.copy(
        tabs =
            pane.tabs.mapIndexed { i, tab ->
                if (i ==
                    pane.activeTab
                ) {
                    reduceTab(tab, result)
                } else {
                    tab
                }
            },
    )

private fun reduceTab(
    tab: TabState,
    result: PaneResult,
): TabState =
    when (result) {
        is PaneResult.Opened -> {
            val backStack = tab.backStack.pushed(result.folder)
            TabState(
                backStack = backStack,
                anchors = tab.anchors.with(tab.folder, result.leaving).keptFor(backStack),
            )
        }

        PaneResult.WentBack -> {
            if (!tab.canGoBack) {
                tab
            } else {
                val backStack = tab.backStack.popped()
                TabState(backStack = backStack, anchors = tab.anchors.keptFor(backStack))
            }
        }

        is PaneResult.ListingStarted -> {
            when {
                result.folder != tab.folder -> {
                    tab
                }

                // Android keeps it private: nothing to read, so the listing is done and empty, and no row
                // stays selected.
                result.lock.locked -> {
                    tab.copy(
                        lock = result.lock,
                        listing = Listing(emptyList(), complete = true),
                        selection = emptySet(),
                        selectionAnchor = null,
                    )
                }

                // A retry keeps the rows already shown, so the list does not flash empty.
                else -> {
                    tab.copy(lock = result.lock, listing = tab.listing.copy(complete = false, error = null))
                }
            }
        }

        is PaneResult.ListingChanged -> {
            // A late emission from a folder already left never wins (templates/screen.md).
            if (result.folder != tab.folder) tab else tab.prunedTo(result.listing).copy(listing = result.listing)
        }

        is PaneResult.Selection -> {
            reduceSelection(tab, result.result)
        }
    }
