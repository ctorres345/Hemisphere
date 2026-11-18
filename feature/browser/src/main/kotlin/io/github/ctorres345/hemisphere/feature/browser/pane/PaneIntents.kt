package io.github.ctorres345.hemisphere.feature.browser.pane

import io.github.ctorres345.hemisphere.core.domain.files.FolderLock
import io.github.ctorres345.hemisphere.core.model.Entry
import io.github.ctorres345.hemisphere.core.model.FsPath
import io.github.ctorres345.hemisphere.core.model.Listing

/** What the user can do in one pane. Wrapped in `BrowserIntent.Pane` (architecture §6.1). */
sealed interface PaneIntent {
    /**
     * Open [entry] from the open folder. A folder opens; a file does nothing yet (HEM-68).
     * [leaving] is where the user was in the folder they leave, restored when they come back.
     */
    data class OpenEntry(
        val entry: Entry,
        val leaving: ScrollAnchor?,
    ) : PaneIntent

    /** Go back to the previous folder in this pane's history. */
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

    /** A new emission of [folder]'s listing. Dropped if [folder] is no longer open. */
    data class ListingChanged(
        val folder: FsPath,
        val listing: Listing,
    ) : PaneResult
}
