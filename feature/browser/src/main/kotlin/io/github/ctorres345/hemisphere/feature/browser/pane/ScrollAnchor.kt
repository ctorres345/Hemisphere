package io.github.ctorres345.hemisphere.feature.browser.pane

import io.github.ctorres345.hemisphere.core.model.EntryKey
import io.github.ctorres345.hemisphere.core.model.FsPath

/**
 * Where the user was in a folder when they left it (architecture §6.1, §7): the row they opened or had
 * focus on, and the first visible row with how far it was scrolled. Rows are named by their list key
 * (`rowKeys`), which is unique in a listing and can be saved.
 */
data class ScrollAnchor(
    val focusedRow: String?,
    val firstVisibleRow: String?,
    val firstVisibleOffset: Int = 0,
)

/** Remembers [anchor] for [folder]; null forgets it. */
internal fun Map<EntryKey, ScrollAnchor>.with(
    folder: FsPath,
    anchor: ScrollAnchor?,
): Map<EntryKey, ScrollAnchor> = if (anchor == null) this - folder.key else this + (folder.key to anchor)

/**
 * Drops the anchor of every folder no longer on [backStack], so anchors never outnumber the capped
 * stack. A folder that is on the stack twice keeps its anchor while one copy is left.
 */
internal fun Map<EntryKey, ScrollAnchor>.keptFor(backStack: List<FsPath>): Map<EntryKey, ScrollAnchor> {
    if (isEmpty()) return this
    val onStack = backStack.mapTo(HashSet()) { it.key }
    return filterKeys { it in onStack }
}
