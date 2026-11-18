package io.github.ctorres345.hemisphere.feature.browser.pane

import io.github.ctorres345.hemisphere.core.domain.files.FolderLock
import io.github.ctorres345.hemisphere.core.model.Entry
import io.github.ctorres345.hemisphere.core.model.FsPath
import io.github.ctorres345.hemisphere.core.model.Listing
import io.github.ctorres345.hemisphere.feature.browser.selection.SelectionAction
import io.github.ctorres345.hemisphere.feature.browser.selection.SelectionResult

/** What the user can do in one pane. Wrapped in `BrowserIntent.Pane` (architecture §6.1). */
sealed interface PaneIntent {
    /**
     * A tap on [entry], a row of [from]'s listing. While selecting it toggles the row; otherwise a
     * folder opens and a file does nothing yet (HEM-68). Dropped unless [from] is still the open
     * folder. [leaving] is where the user was in the folder they leave, restored when they come back.
     */
    data class OpenEntry(
        val entry: Entry,
        val from: FsPath,
        val leaving: ScrollAnchor?,
    ) : PaneIntent

    /** A long-press on [entry], a row of [from]'s listing: starts the selection, or selects a range. */
    data class LongPress(
        val entry: Entry,
        val from: FsPath,
    ) : PaneIntent

    data object SelectAll : PaneIntent

    data object ClearSelection : PaneIntent

    /**
     * [action] on the selection, from the action bar, or on [entry] alone, from its row's menu. The
     * actions come with HEM-59, 60 and 61; until then this does nothing.
     */
    data class Act(
        val action: SelectionAction,
        val entry: Entry? = null,
    ) : PaneIntent

    /** Clear the selection if there is one, else go back to the previous folder in this pane's history. */
    data object Back : PaneIntent

    /** List the open folder again after an error. */
    data object Retry : PaneIntent
}

/** What can change one pane. Wrapped in `BrowserResult.Pane` and reduced by [reducePane]. */
sealed interface PaneResult {
    /** [folder] was opened from the open folder, where the user was at [leaving]. */
    data class Opened(
        val folder: FsPath,
        val leaving: ScrollAnchor?,
    ) : PaneResult

    /** The top folder was left for the one below it. */
    data object WentBack : PaneResult

    /** Started reading [folder]; [lock] says what Android keeps private there. */
    data class ListingStarted(
        val folder: FsPath,
        val lock: FolderLock,
    ) : PaneResult

    /** The open folder's selection changed. */
    data class Selection(
        val result: SelectionResult,
    ) : PaneResult

    /**
     * A new emission of [folder]'s listing. Dropped if [folder] is no longer open. Selected rows it no
     * longer lists leave the selection.
     */
    data class ListingChanged(
        val folder: FsPath,
        val listing: Listing,
    ) : PaneResult
}
