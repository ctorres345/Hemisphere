package io.github.ctorres345.hemisphere.feature.browser

import io.github.ctorres345.hemisphere.core.model.Entry
import io.github.ctorres345.hemisphere.core.model.FsError
import io.github.ctorres345.hemisphere.core.model.FsPath
import io.github.ctorres345.hemisphere.core.model.Listing

/**
 * Everything the Browser screen shows. The walking skeleton has one folder; panes, tabs, the back
 * stack and selection (architecture §6.1) come with the Browser ticket.
 */
data class BrowserUiState(
    val folder: FsPath? = null,
    val entries: List<Entry> = emptyList(),
    val loading: Boolean = true,
    val error: FsError? = null,
)

/** Everything the user can do on the Browser screen. */
sealed interface BrowserIntent {
    /** List the open folder again after an error. */
    data object Retry : BrowserIntent
}

/** Everything that can change [BrowserUiState]. */
sealed interface BrowserResult {
    /** Started listing [folder]. */
    data class FolderOpened(
        val folder: FsPath,
    ) : BrowserResult

    /** A new emission of the open folder's listing. */
    data class ListingChanged(
        val listing: Listing,
    ) : BrowserResult
}
