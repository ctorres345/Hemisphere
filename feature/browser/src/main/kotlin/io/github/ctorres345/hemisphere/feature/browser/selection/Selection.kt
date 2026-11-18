package io.github.ctorres345.hemisphere.feature.browser.selection

import io.github.ctorres345.hemisphere.core.model.Entry
import io.github.ctorres345.hemisphere.core.model.EntryKey
import io.github.ctorres345.hemisphere.feature.browser.pane.PaneIntent
import io.github.ctorres345.hemisphere.feature.browser.pane.TabState

/**
 * What the user can do with the selected rows, in the order the action bar, the Dual header and a row's
 * menu show them, Delete last as the prototype's action bar. Each only sends an intent until HEM-59, 60 and 61.
 */
enum class SelectionAction { Copy, Move, Rename, More, Delete }

/** What can change one tab's selection. Wrapped in `PaneResult.Selection`. */
sealed interface SelectionResult {
    /**
     * A row was long-pressed. With nothing selected it starts the selection; otherwise it adds every
     * row between the anchor and this one, in listing order, and becomes the next range's anchor.
     */
    data class LongPressed(
        val key: EntryKey,
    ) : SelectionResult

    /** A row was tapped while selecting: it joins or leaves the selection. */
    data class Toggled(
        val key: EntryKey,
    ) : SelectionResult

    data object SelectedAll : SelectionResult

    /** Clear, the header's close button, or Back. */
    data object Cleared : SelectionResult
}

/**
 * What the selection header and action bar show: "[count] selected", plus [fileBytes] when every
 * selected row is a file of known size (folder sizes are not counted in the MVP). Rename takes one item.
 */
data class SelectionSummary(
    val count: Int,
    val fileBytes: Long?,
) {
    val canRename: Boolean get() = count == 1

    /** Whether [action] can run on this selection. */
    fun allows(action: SelectionAction): Boolean = count > 0 && (action != SelectionAction.Rename || canRename)
}

/** The key that [entry], a row of the open folder, is selected by. */
fun TabState.keyOf(entry: Entry): EntryKey = folder.child(entry.name, entry.ref).key

/** True while any row is selected: taps toggle rows and Back clears the selection. */
val TabState.selecting: Boolean get() = selection.isNotEmpty()

/** The header and action bar for this tab's selection. */
fun TabState.selectionSummary(): SelectionSummary {
    val selected = listing.entries.filter { keyOf(it) in selection }
    val sizes = selected.map { if (it.kind == Entry.Kind.File) it.size else null }
    // While a refresh has not read every selected row yet, a sum would leave some out.
    val known = selection.isNotEmpty() && selected.size == selection.size && sizes.all { it != null }
    val bytes = if (known) sizes.sumOf { it ?: 0L } else null
    return SelectionSummary(count = selection.size, fileBytes = bytes)
}

/**
 * Whether system Back stays in the app: it clears the selection first, else goes up a folder. At a
 * root with nothing selected the system takes it.
 */
val TabState.handlesBack: Boolean get() = selecting || canGoBack

/**
 * The selection change [intent] makes on this tab, or null when it is not a selection intent. While
 * selecting, a tap toggles the row and Back clears; a press from a folder no longer open is dropped.
 */
fun TabState.selectionResultFor(intent: PaneIntent): SelectionResult? =
    when {
        intent is PaneIntent.LongPress && intent.from == folder -> {
            SelectionResult.LongPressed(keyOf(intent.entry))
        }

        intent is PaneIntent.OpenEntry && selecting -> {
            if (intent.from == folder) SelectionResult.Toggled(keyOf(intent.entry)) else null
        }

        intent is PaneIntent.SelectAll -> {
            SelectionResult.SelectedAll
        }

        intent is PaneIntent.ClearSelection || (intent is PaneIntent.Back && selecting) -> {
            SelectionResult.Cleared
        }

        else -> {
            null
        }
    }
