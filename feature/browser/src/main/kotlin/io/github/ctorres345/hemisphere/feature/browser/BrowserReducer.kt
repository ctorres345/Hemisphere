package io.github.ctorres345.hemisphere.feature.browser

/** The only place [BrowserUiState] changes. Pure: no I/O, no coroutines, no clock. */
fun reduce(
    state: BrowserUiState,
    result: BrowserResult,
): BrowserUiState =
    when (result) {
        is BrowserResult.FolderOpened -> {
            state.copy(
                folder = result.folder,
                // Keep the rows on a retry of the same folder, so the list does not flash empty.
                entries = if (result.folder == state.folder) state.entries else emptyList(),
                loading = true,
                error = null,
            )
        }

        is BrowserResult.ListingChanged -> {
            state.copy(
                entries = result.listing.entries,
                loading = !result.listing.complete,
                error = result.listing.error,
            )
        }
    }
